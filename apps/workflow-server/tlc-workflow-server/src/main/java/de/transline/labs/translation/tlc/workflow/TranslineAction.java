package de.transline.labs.translation.tlc.workflow;

import com.coremedia.cap.common.Blob;
import com.coremedia.cap.common.RelativeTimeLimit;
import com.coremedia.cap.content.Content;
import com.coremedia.cap.content.ContentObject;
import com.coremedia.cap.multisite.ContentObjectSiteAspect;
import com.coremedia.cap.multisite.Site;
import com.coremedia.cap.multisite.SitesService;
import com.coremedia.cap.struct.Struct;
import com.coremedia.cap.translate.xliff.XliffImportResultCode;
import com.coremedia.cap.util.StructUtil;
import com.coremedia.cap.workflow.Process;
import com.coremedia.cap.workflow.Task;
import com.coremedia.cap.workflow.plugin.ActionResult;
import de.transline.labs.translation.tlc.facade.TLCConfigProperty;
import de.transline.labs.translation.tlc.facade.TLCExchangeFacade;
import de.transline.labs.translation.tlc.facade.TLCFacadeCommunicationException;
import de.transline.labs.translation.tlc.facade.TLCFacadeConfigException;
import de.transline.labs.translation.tlc.facade.TLCFacadeException;
import de.transline.labs.translation.tlc.facade.TLCFacadeFileTypeConfigException;
import de.transline.labs.translation.tlc.facade.TLCFacadeIOException;
import com.coremedia.rest.validation.Severity;
import com.coremedia.workflow.common.util.SpringAwareLongAction;
import com.google.common.annotations.VisibleForTesting;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;
import edu.umd.cs.findbugs.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static de.transline.labs.translation.tlc.facade.DefaultTLCExchangeFacadeSessionProvider.defaultFactory;
import static java.util.Objects.requireNonNull;

/**
 * Abstract workflow {@link com.coremedia.cap.workflow.plugin.LongAction} that opens a connection to Transline and
 * handles errors during action execution.
 *
 * <p>Concrete subclasses must implement
 * <ul>
 *   <li>{@link #doExtractParameters(Task)} to extract a value of type {@code <P>} from workflow variables that
 *   is passed as parameter to method {@link #doExecuteTranslineAction(Object, Consumer, TLCExchangeFacade, Map)}.
 *
 *   <li>{@link #doExecuteTranslineAction(Object, Consumer, TLCExchangeFacade, Map)} to execute the action. This method
 *   receives a parameter of type {@code <P>} that was returned by {@link #doExtractParameters(Task)} and sets the
 *   result value of type {@code <R>} at its consumer argument. The result value will then be passed to
 *   {@link #doStoreResult(Task, Object)}, which by default stores the value in the workflow variable that is
 *   configured in the workflow definition with attribute {@code resultVariable} at the {@code Action} element.
 *   Subclasses may also overwrite {@link #doStoreResult(Task, Object)} to store complex results in different
 *   workflow variables.
 * </ul>
 *
 * <p>Methods {@link #doExtractParameters(Task)}, {@link #doExecuteTranslineAction(Object, Consumer, TLCExchangeFacade, Map)}
 * and {@link #doStoreResult(Task, Object)} are called from {@link com.coremedia.cap.workflow.plugin.LongAction}
 * methods {@link com.coremedia.cap.workflow.plugin.LongAction#extractParameters(Task) extractParameters},
 * {@link com.coremedia.cap.workflow.plugin.LongAction#execute(Object) execute} and
 * {@link com.coremedia.cap.workflow.plugin.LongAction#storeResult(Task, Object) storeResult}, respectively,
 * and the constraints documented for those methods apply as well.
 *
 * @param <P> the type of the parameter passed to {@link #doExecuteTranslineAction(Object, Consumer, TLCExchangeFacade, Map)}
 *            that was previously returned by {@link #doExtractParameters(Task)}
 * @param <R> the type of the result value that {@link #doExecuteTranslineAction(Object, Consumer, TLCExchangeFacade, Map)}
 *           passes to its consumer argument and that is then passed as parameter to {@link #doStoreResult(Task, Object)}
 */
abstract class TranslineAction<P, R> extends SpringAwareLongAction {
  private static final Logger LOG = LoggerFactory.getLogger(TranslineAction.class);

  private static final long serialVersionUID = -7130959823193680910L;

  private static final String LOCAL_SETTINGS = "localSettings";
  private static final String LINKED_SETTINGS = "linkedSettings";
  private static final String CMSETTINGS_SETTINGS = "settings";

  /**
   * Name of the config parameter in {@link #getTlcSettings(Site)} to control how many times communication
   * errors should be retried automatically. Defaults to {@link #DEFAULT_RETRY_COMMUNICATION_ERRORS} if unset.
   */
  private static final String CONFIG_RETRY_COMMUNICATION_ERRORS = "retryCommunicationErrors";
  private static final int DEFAULT_RETRY_COMMUNICATION_ERRORS = 5;

  private static final MimeType MIME_TYPE_JSON = mimeType("application/json");
  private static final Gson contentObjectReturnsIdGson = new GsonBuilder()
          .enableComplexMapKeySerialization()
          .registerTypeAdapter(Content.class, new ContentObjectSerializer())
          .create();

  private String skipVariable;
  private String masterContentObjectsVariable;
  private String remainingAutomaticRetriesVariable;
  private String issuesVariable;
  private String retryDelayTimerVariable;


  /**
   * Minimum delay between retrying communication with Transline. Firing too many update requests on the external system
   * could be considered a DoS attack.
   */
  private static final int MIN_RETRY_DELAY_SECS = 60; // one minute

  /**
   * If the value is accidentally set to a very big delay and the workflow process picks this value, you will have
   * to wait very long until it checks again for an update.
   * Changing this accidentally got also a lot more likely, since times can be change in the content repository directly.
   */
  private static final int MAX_RETRY_DELAY_SECS = 86400; // one day

  /**
   * Fallback delay between retrying communication with Transline for illegal values.
   */
  private static final int FALLBACK_RETRY_COMMUNICATION_DELAY_SECS = 900;

  // --- construct and configure ----------------------------------------------------------------------

  TranslineAction(boolean rethrowResultException) {
    super(rethrowResultException);
  }

  /**
   * Sets the name of the boolean process variable which controls whether this action should be skipped.
   *
   * @param skipVariable variable name
   */
  @SuppressWarnings("unused") // set from workflow definition
  public void setSkipVariable(String skipVariable) {
    this.skipVariable = skipVariable;
  }


  /**
   * Return the name of the process variable containing the source contents objects.
   *
   * @return the name of the process variable
   */
  String getMasterContentObjectsVariable() {
    return masterContentObjectsVariable;
  }

  /**
   * Set the name of the process variable containing the source contents objects.
   *
   * @param masterContentObjectsVariable the name of the process variable
   */
  @SuppressWarnings("unused") // set from workflow definition
  public void setMasterContentObjectsVariable(String masterContentObjectsVariable) {
    this.masterContentObjectsVariable = masterContentObjectsVariable;
  }

  /**
   * Sets the name of the Integer process variable that holds the number of remaining automatic retries after
   * errors that should be retried without user intervention.
   *
   * @param remainingAutomaticRetriesVariable integer workflow variable name
   */
  @SuppressWarnings("unused") // set from workflow definition
  public void setRemainingAutomaticRetriesVariable(String remainingAutomaticRetriesVariable) {
    this.remainingAutomaticRetriesVariable = remainingAutomaticRetriesVariable;
  }

  /**
   * Sets the name of the blob process variable to store a JSON blob with errors that happened when interacting
   * with the translation service, or null if no such errors occurred. Studio's TaskErrorValidator
   * can then display these errors as task issues. The JSON data structure is a serialized map from severity to
   * map of error code to list of affected contents, i.e. {@code Map<Severity, Map<String, List<Content>>>}.
   *
   * @param issuesVariable blob workflow variable name
   */
  @SuppressWarnings({"unused", "WeakerAccess"}) // set from workflow definition
  public void setIssuesVariable(String issuesVariable) {
    this.issuesVariable = requireNonNull(issuesVariable);
  }

  /**
   * The name of the Timer variable that will be initialized with value from the corresponding Spring properties or content
   * setting.
   * @param retryDelayTimerVariable the name of the Timer variable
   */
  public void setRetryDelayTimerVariable(String retryDelayTimerVariable) {
    this.retryDelayTimerVariable = retryDelayTimerVariable;
  }

  // --- LongAction interface ----------------------------------------------------------------------

  @Override
  public final Parameters<P> extractParameters(Task task) {
    Process process = task.getContainingProcess();

    if (skipVariable != null && process.getBoolean(skipVariable)) {
      return null;
    }

    List<ContentObject> masterContentObjects = process.getLinksAndVersions(getMasterContentObjectsVariable());

    if (retryDelayTimerVariable != null) {
      Site masterSite = getMasterSite(masterContentObjects);
      Map<String, Object> settings = getTlcSettings(masterSite);
      Integer delaySeconds = ensureRetryDelayConfig(settings, retryDelayTimerVariable);
      process.set(retryDelayTimerVariable, new RelativeTimeLimit(delaySeconds));
    }

    Integer i = process.getInteger(remainingAutomaticRetriesVariable);
    int remainingAutomaticRetries = i != null ? i : 0;
    P extendedParameters = doExtractParameters(task);
    return new Parameters<>(extendedParameters, masterContentObjects, remainingAutomaticRetries);
  }

  private static Integer ensureRetryDelayConfig(Map<String, Object> config, String key) {
    Object value = config.get(key);
    if (value == null) {
      LOG.warn("\"{}\" value must not be null. Falling back to {}.", key, FALLBACK_RETRY_COMMUNICATION_DELAY_SECS);
      return FALLBACK_RETRY_COMMUNICATION_DELAY_SECS;
    }
    int retryDelayInSec = Integer.parseInt(String.valueOf(value));
    if (retryDelayInSec < MIN_RETRY_DELAY_SECS) {
      LOG.warn("\"{}\" must not be smaller than {} seconds, but is {}. Falling back to minimum.", key, MIN_RETRY_DELAY_SECS, retryDelayInSec);
      retryDelayInSec = MIN_RETRY_DELAY_SECS;
    } else if (retryDelayInSec > MAX_RETRY_DELAY_SECS) {
      LOG.warn("\"{}\" must not be bigger than {} seconds, but is {}. Falling back to maximum.", key, MAX_RETRY_DELAY_SECS, retryDelayInSec);
      retryDelayInSec = MAX_RETRY_DELAY_SECS;
    }
    return retryDelayInSec;
  }

  @Override
  protected final Result<R> doExecute(Object params) {
    if (params == null) {
      // skip
      return null;
    }

    @SuppressWarnings("unchecked" /* per interface contract: params is the return value of #extractParameters */)
    Parameters<P> parameters = (Parameters<P>) params;

    Result<R> result = new Result<>();
    // maps error codes to affected contents; list of contents may be empty for some errors */
    Map<String, List<Content>> issues = new HashMap<>();

    Site masterSite = getMasterSite(parameters.masterContentObjects);
    try  {
      TLCExchangeFacade tlcSession = openSession(masterSite);

      // call subclass implementation and store the result as result.extendedResult
      Consumer<R> resultConsumer = r -> result.extendedResult = Optional.of(r);
      doExecuteTranslineAction(parameters.extendedParameters, resultConsumer, tlcSession, issues);

    } catch (TLCFacadeCommunicationException e) {
      // automatically retry communication errors until configured maximum of retries has been reached
      // but do not retry automatically if #doExecuteTranslineAction returned additional issues
      //noinspection ConstantConditions - false positive; issues is not always empty, it can be modified by #doExecuteTranslineAction
      if (issues.isEmpty()) {
        result.remainingAutomaticRetries = parameters.remainingAutomaticRetries > 0
                ? parameters.remainingAutomaticRetries - 1
                : maxAutomaticRetries(masterSite);
      }
      if (result.remainingAutomaticRetries > 0) {
        LOG.info("{}: Failed to connect to TLC ({}). Will retry {} time(s)", getName(),
                TranslineWorkflowErrorCodes.TRANSLINE_COMMUNICATION_ERROR, result.remainingAutomaticRetries, e);
      } else {
        LOG.warn("{}: Failed to connect to TLC ({}).", getName(), TranslineWorkflowErrorCodes.TRANSLINE_COMMUNICATION_ERROR, e);
      }
      issues.put(TranslineWorkflowErrorCodes.TRANSLINE_COMMUNICATION_ERROR, Collections.emptyList());

    } catch (TLCFacadeIOException e) {
      LOG.warn("{}: Local I/O error ({}).", getName(), TranslineWorkflowErrorCodes.LOCAL_IO_ERROR, e);
      issues.put(TranslineWorkflowErrorCodes.LOCAL_IO_ERROR, Collections.emptyList());
    } catch (TLCFacadeFileTypeConfigException e) {
      LOG.warn("{}: Communication failed because of unsupported configured file type ({})", getName(), TranslineWorkflowErrorCodes.SETTINGS_FILE_TYPE_ERROR, e);
      issues.put(TranslineWorkflowErrorCodes.SETTINGS_FILE_TYPE_ERROR, Collections.emptyList());
    } catch (TLCFacadeConfigException e) {
      LOG.warn("{}: Communication failed because of invalid/missing settings ({})", getName(), TranslineWorkflowErrorCodes.SETTINGS_ERROR, e);
      issues.put(TranslineWorkflowErrorCodes.SETTINGS_ERROR, Collections.emptyList());
    } catch (TLCFacadeException e) {
      LOG.warn("{}: Unknown error occurred ({})", getName(), TranslineWorkflowErrorCodes.UNKNOWN_ERROR, e);
      issues.put(TranslineWorkflowErrorCodes.UNKNOWN_ERROR, Collections.emptyList());
    } catch (TranslineWorkflowException e) {
      LOG.warn("{}: " + e.getMessage() + "({})", getName(), e.getErrorCode(), e);
      issues.put(e.getErrorCode(), Collections.emptyList());
    }

    result.issues = issuesAsJsonBlob(issues);
    return result;
  }

  @Override
  public final ActionResult storeResult(Task task, Object result) {
    checkNotAborted(task);
    if (result instanceof Exception) {
      return storeResultException(task, (Exception) result);
    }
    if (result == null) {
      // skip
      return ActionResult.SUCCESSFUL;
    }

    @SuppressWarnings("unchecked" /* per interface contract: result is the return value of #doExecute */)
    Result<R> r = (Result<R>) result;

    Process process = task.getContainingProcess();
    process.set(remainingAutomaticRetriesVariable, r.remainingAutomaticRetries);
    process.set(issuesVariable, r.issues);

    Object resultValue = r.extendedResult
            .map(extendedResult -> doStoreResult(task, extendedResult))
            .orElse(null);
    return super.storeResult(task, resultValue);
  }

  // --- Methods to be implemented / overridden by concrete subclass -------------------------------

  /**
   * Extract parameters from the {@link Task} and return them as a single object of type {@code <T>}.
   * The result will be passed as argument to method {@link #doExecuteTranslineAction(Object, Consumer, TLCExchangeFacade, Map)}.
   *
   * <p>This method is called from {@link com.coremedia.cap.workflow.plugin.LongAction#extractParameters(Task)} and
   * the constraints documented for that method apply here as well.
   *
   * @param task the task in which the action should be executed
   * @return the parameters for the actual computation or null if no parameters are needed
   */
  abstract P doExtractParameters(Task task);

  /**
   * Executes the action and optionally sets a result value at the given {@code resultConsumer}.
   *
   * <p>The result value will be passed as argument to method {@link #doStoreResult(Task, Object)} by
   * the caller. If the consumer is called multiple times, all but the last call will be ignored. If the consumer
   * isn't called, then method {@link #doStoreResult(Task, Object)} won't be called and workflow variables won't be
   * changed as result of this action.
   *
   * <p>If this method throws a {@link TLCFacadeException} or {@link TranslineWorkflowException}, then method
   * {@link #doStoreResult(Task, Object)} will still be called afterwards if a result has been set at the
   * consumer. In case of other exceptions, no results are stored and exceptions are propagated and
   * may lead to task escalation, depending on the value of the {@code rethrowResultException} constructor
   * parameter.
   *
   * <p>This method is called from {@link com.coremedia.cap.workflow.plugin.LongAction#execute(Object)} and
   * the constraints documented for that method apply here as well.
   *
   * @param params parameters returned by {@link #doExtractParameters(Task)}
   * @param resultConsumer consumer that takes the result of the execution
   * @param facade the facade to communicate with Transline
   * @param issues map to add issues to that occurred during action execution and will be stored in the workflow
   *               variable set with {@link #setIssuesVariable(String)}. The workflow can display
   *               these issues to the end-user, who may trigger a retry, for example.
   * @throws TLCFacadeException if an error was raised by the given facade
   * @throws TranslineWorkflowException if some other error occurred
   */
  abstract void doExecuteTranslineAction(P params,
                                          Consumer<? super R> resultConsumer,
                                          TLCExchangeFacade facade,
                                          Map<String, List<Content>> issues);

  /**
   * Receives the result from {@link #doExecuteTranslineAction(Object, Consumer, TLCExchangeFacade, Map)} if that
   * method passed a result to its consumer argument. This method may store the result in workflow variables.
   * It may also return a value that the caller then stores in the result workflow variable, which is configured in the
   * workflow definition with attribute {@code resultVariable} at the {@code Action} element.
   *
   * <p>This method is called from {@link com.coremedia.cap.workflow.plugin.LongAction#storeResult(Task, Object)} and
   * the constraints documented for that method apply here as well.
   *
   * <p>The default implementation just returns the unchanged value of the {@code result} argument. Subclasses
   * must override this method for complex result values that cannot or should not be stored in the
   * {@code resultVariable} as is.
   *
   * @param task the task in which the action should be executed
   * @param result result value that was passed to the consumer in {@link #doExecuteTranslineAction}
   * @return value to store in the {@code resultVariable} or null to store nothing in that variable
   */
  @Nullable
  Object doStoreResult(Task task, R result) {
    return result;
  }

  // --- Helper methods for subclasses ----------------------------------------

  SitesService getSitesService() {
    return getSpringContext().getBean(SitesService.class);
  }

  static String parseSubmissionId(String submissionId, String wfTaskId) {
    if (submissionId == null || submissionId.isEmpty()) {
      throw new TranslineWorkflowException(TranslineWorkflowErrorCodes.ILLEGAL_SUBMISSION_ID_ERROR, "Transline submission id not set", wfTaskId);
    }
    try {
      return submissionId;
    } catch (NumberFormatException e) {
      throw new TranslineWorkflowException(TranslineWorkflowErrorCodes.ILLEGAL_SUBMISSION_ID_ERROR, "Transline submission id malformed. Long value expected",
              (Object[]) new Object[]{wfTaskId, submissionId});
    }
  }

  static MimeType mimeType(String mimeTypeString) {
    try {
      return new MimeType(mimeTypeString);
    } catch (MimeTypeParseException e) {
      throw new IllegalArgumentException("Cannot parse mime-type: '" + mimeTypeString + "'.", e);
    }
  }

  // --- Internal -------------------------------------------------------------

  private Site getMasterSite(Collection<? extends ContentObject> masterContents) {
    SitesService sitesService = getSitesService();
    return masterContents.stream()
      .map(sitesService::getSiteAspect)
      .map(ContentObjectSiteAspect::getSite)
      .filter(Objects::nonNull)
      .findAny()
      .orElseThrow(() -> new IllegalStateException("No master site found"));
  }

  private int maxAutomaticRetries(Site masterSite) {
    Map<String, Object> tlcSettings = getTlcSettings(masterSite);
    Object value = tlcSettings.get(CONFIG_RETRY_COMMUNICATION_ERRORS);
    if (value != null) {
      try {
        return Integer.parseInt(String.valueOf(value));
      } catch (NumberFormatException e) {
        LOG.warn("Ignoring setting '{}'. Not an integer: {}", CONFIG_RETRY_COMMUNICATION_ERRORS, value);
      }
    }
    return DEFAULT_RETRY_COMMUNICATION_ERRORS;
  }

  @VisibleForTesting
  Map<String, Object> getTlcSettings(Site site) {
    Content siteIndicator = site.getSiteIndicator();

    @SuppressWarnings("unchecked")
    Map<String, Object> defaultSettings = new HashMap<String,Object>(getSpringContext().getBean("tlcConfigurationProperties", Map.class));

    Map<String, Object> siteIndicatorSettings = getTlcSettings(siteIndicator);
    if (!siteIndicatorSettings.isEmpty()) {
      defaultSettings.putAll(siteIndicatorSettings);
      return Collections.unmodifiableMap(defaultSettings);
    }

    Content siteRootDocument = site.getSiteRootDocument();
    Map<String, Object> rootDocumentSettings = getTlcSettings(siteRootDocument);
    if (!rootDocumentSettings.isEmpty()) {
      defaultSettings.putAll(rootDocumentSettings);
      return Collections.unmodifiableMap(defaultSettings);
    }

    return defaultSettings;
  }

  private static Map<String, Object> getTlcSettings(Content content) {
    Struct localSettings = getStruct(content, LOCAL_SETTINGS);
    Struct struct = StructUtil.mergeStructList(
            localSettings,
            content.getLinks(LINKED_SETTINGS)
                    .stream()
                    .map(link -> getStruct(link, CMSETTINGS_SETTINGS))
                    .collect(Collectors.toList())
    );
    if (struct != null) {
      Object value = struct.get(TLCConfigProperty.KEY_TRANSLINE_ROOT);
      if (value instanceof Struct) {
        return ((Struct) value).toNestedMaps();
      }
    }

    return Collections.emptyMap();
  }

  @Nullable
  private static Struct getStruct(Content content, String name) {
    if (content != null && content.isInProduction()) {
      return content.getStruct(name);
    }
    return null;
  }

  @VisibleForTesting
  TLCExchangeFacade openSession(Site site) {
    return defaultFactory().openSession(getTlcSettings(site));
  }

  @Nullable
  private Blob issuesAsJsonBlob(Map<String, List<Content>> issues) {
    if (issues.isEmpty()) {
      return null;
    }

    // all issues should have the severity ERROR when displayed in Studio
    Map<Severity, Map<String, List<Content>>> studioIssues = Collections.singletonMap(Severity.ERROR, issues);

    byte[] bytes = issuesAsJsonString(studioIssues).getBytes(StandardCharsets.UTF_8);
    return getConnection().getBlobService().fromBytes(bytes, MIME_TYPE_JSON);
  }

  private static String issuesAsJsonString(Map<Severity, Map<String, List<Content>>> issues) {
    Type typeToken = new TypeToken<Map<Severity, Map<XliffImportResultCode, List<Content>>>>() {
    }.getType();
    return contentObjectReturnsIdGson.toJson(issues, typeToken);
  }

  private static class ContentObjectSerializer implements JsonSerializer<ContentObject> {
    @Override
    public JsonElement serialize(ContentObject contentObject, Type type, JsonSerializationContext jsonSerializationContext) {
      if (contentObject == null) {
        return JsonNull.INSTANCE;
      }
      return new JsonPrimitive(contentObject.getId());
    }
  }

  private static class Parameters<P> {
    final P extendedParameters;
    final Collection<ContentObject> masterContentObjects;
    final int remainingAutomaticRetries;

    Parameters(P extendedParameters, Collection<ContentObject> masterContentObjects, int remainingAutomaticRetries) {
      this.extendedParameters = extendedParameters;
      this.masterContentObjects = masterContentObjects;
      this.remainingAutomaticRetries = remainingAutomaticRetries;
    }
  }

  private static class Result<R> {
    /** holds the result from {@link #doExecuteTranslineAction}, empty for no result */
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType") // suppress warning for non-typical usage of Optional
    Optional<R> extendedResult = Optional.empty();
    /** json with map from studio severity to map of error codes to possibly empty list of affected contents */
    Blob issues;
    /** number of remaining automatic retries, if there are issues */
    int remainingAutomaticRetries;
  }
}
