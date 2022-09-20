package de.transline.labs.translation.tlc.workflow;

import com.coremedia.cap.content.Content;
import com.coremedia.cap.content.ContentObject;
import com.coremedia.cap.multisite.ContentObjectSiteAspect;
import com.coremedia.cap.multisite.Site;
import com.coremedia.cap.multisite.SitesService;
import com.coremedia.cap.translate.xliff.XliffExporter;
import com.coremedia.cap.workflow.Process;
import com.coremedia.cap.workflow.Task;
import de.transline.labs.translation.tlc.facade.TLCExchangeFacade;
import de.transline.labs.translation.tlc.facade.TLCFacadeCommunicationException;
import com.coremedia.translate.item.ContentToTranslateItemTransformer;
import com.coremedia.translate.item.TranslateItem;
import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.coremedia.cap.translate.xliff.XliffExportOptions.EmptyOption.EMPTY_IGNORE;
import static com.coremedia.cap.translate.xliff.XliffExportOptions.TargetOption.TARGET_SOURCE;
import static com.coremedia.cap.translate.xliff.XliffExportOptions.xliffExportOptions;
import static de.transline.labs.translation.tlc.workflow.TranslineWorkflowErrorCodes.XLIFF_EXPORT_FAILURE;
import static com.coremedia.translate.item.TransformStrategy.ITEM_PER_TARGET;
import static java.lang.invoke.MethodHandles.lookup;
import static java.util.stream.Collectors.groupingBy;

/**
 * Requests a translation from Transline by opening a submission with uploaded XLIFF for translatable
 * properties of content specified by the workflow.
 */
public class SendToTranslineAction extends TranslineAction<SendToTranslineAction.Parameters, String> {
  private static final Logger LOG = LoggerFactory.getLogger(lookup().lookupClass());

  private static final long serialVersionUID = 7530762957907324426L;

  private String derivedContentsVariable;
  private String subjectVariable;
  private String commentVariable;
  private String performerVariable;
  private String translineDueDateVariable;
  private String translineWorkflowVariable;

  // --- construct and configure ----------------------------------------------------------------------

  public SendToTranslineAction() {
    // Escalate in case of errors.
    // Some exceptions that are worth retrying are handled in storeResult
    // and suppressed from escalation.
    super(true);
  }

  /**
   * Sets the name of the process variable that stores the list of derived contents
   * for which a translation should be requested.
   *
   * @param derivedContentsVariable the name of the process variable
   */
  @SuppressWarnings("unused") // set from workflow definition
  public void setDerivedContentsVariable(String derivedContentsVariable) {
    this.derivedContentsVariable = derivedContentsVariable;
  }

  /**
   * Sets the variable to read the subject/title of the translation workflow from.
   * This will be used to name the translation submission accordingly.
   *
   * @param subjectVariable subject variable name
   */
  @SuppressWarnings("unused") // set from workflow definition
  public void setSubjectVariable(String subjectVariable) {
    this.subjectVariable = subjectVariable;
  }

  /**
   * Sets the variable to read the comment/instructions of the translation workflow from.
   * This will be used for the translation submission accordingly.
   *
   * @param commentVariable subject variable name
   */
  @SuppressWarnings("unused") // set from workflow definition
  public void setCommentVariable(String commentVariable) {
    this.commentVariable = commentVariable;
  }

  /**
   * Sets the variable to read the performer of the translation workflow from.
   * This will be used to add the name of the submitter to the translation submission (if enabled=.
   *
   * @param performerVariable performer variable name
   */
  @SuppressWarnings("unused") // set from workflow definition
  public void setPerformerVariable(String performerVariable) {
    this.performerVariable = performerVariable;
  }

  /**
   * Sets the variable to read the dueDate, that will be sent to Transline.
   *
   * @param translineDueDateVariable subject variable name
   */
  @SuppressWarnings("unused") // set from workflow definition
  public void setTranslineDueDateVariable(String translineDueDateVariable) {
    this.translineDueDateVariable = translineDueDateVariable;
  }

  /**
   * Sets the variable to define, which translation workflow is used on Transline side.
   *
   * @param translineWorkflowVariable workflow variable name
   */
  @SuppressWarnings("unused") // set from workflow definition
  public void setTranslineWorkflowVariable(String translineWorkflowVariable) {
    this.translineWorkflowVariable = translineWorkflowVariable;
  }

  // --- TranslineAction interface ----------------------------------------------------------------------

  @Override
  Parameters doExtractParameters(Task task) {
    Process process = task.getContainingProcess();

    String subject = process.getString(subjectVariable);
    String comment = commentVariable != null ? process.getString(commentVariable) : null;
    List<Content> derivedContents = process.getLinks(derivedContentsVariable);
    List<ContentObject> masterContentObjects = process.getLinksAndVersions(getMasterContentObjectsVariable());
    Calendar date = process.getDate(translineDueDateVariable);
    ZonedDateTime dueDate = ZonedDateTime.ofInstant(date.toInstant(), date.getTimeZone().toZoneId());
    String workflow = translineWorkflowVariable != null ? process.getString(translineWorkflowVariable) : null;
    String submitterName = null;
    if (performerVariable != null) {
      submitterName = process.getUser(performerVariable).getName();
    }
    return new Parameters(subject, comment, derivedContents, masterContentObjects, dueDate, workflow, submitterName);
  }

  /**
   * Sends a submission to Transline with to be translated XLIFF from the contents specified in the workflow
   * and sets the submission ID as string value as result, which will be stored in the action's {@code resultVariable}.
   *
   * @param params parameters returned by {@link #doExtractParameters(Task)}
   * @param resultConsumer consumer that takes the submission ID as result
   * @param facade the facade to communicate with Transline
   * @param issues map to add issues to that occurred during action execution and will be stored in the workflow
   *               variable set with {@link #setIssuesVariable(String)}. The workflow can display
   *               these issues to the end-user, who may trigger a retry, for example.
   */
  @Override
  void doExecuteTranslineAction(Parameters params, Consumer<? super String> resultConsumer,
                                   TLCExchangeFacade facade, Map<String, List<Content>> issues) {
    Collection<Content> derivedContents = params.derivedContents;
    Collection<ContentObject> masterContentObjects = params.masterContentObjects;
    if (derivedContents.isEmpty() || masterContentObjects.isEmpty()) {
      LOG.error("Master and/or derived contents not set. Nothing to translate");
      return;
    }

    Function<ContentObjectSiteAspect, Locale> localeMapper = SendToTranslineAction::preferSiteLocale;
    Map<Locale, List<TranslateItem>> translationItemsByLocale
            = getTranslationItemsByLocale(masterContentObjects, derivedContents, localeMapper);
    Locale firstMasterLocale = findFirstMasterLocale(masterContentObjects, localeMapper)
            .orElseThrow(() -> new IllegalStateException("Unable to identify master locale."));

    String submissionId = submitSubmission(facade, params.subject, params.comment, firstMasterLocale, translationItemsByLocale);
    resultConsumer.accept(submissionId);
  }

  // --- Internal ----------------------------------------------------------------------

  @SuppressWarnings("NestedTryStatement")
  private Path exportToXliff(Locale sourceLocale, Set<Locale> targetLocales, List<TranslateItem> itemsToTranslate) {
    XliffExporter xliffExporter = getSpringContext().getBean(XliffExporter.class);
    Set<String> targetLanguages = targetLocales.stream().map(Locale::toLanguageTag).collect(Collectors.toSet());

    Path xliffPath;
    try {
      xliffPath = Files.createTempFile(lookup().lookupClass().getSimpleName(),
              '.' + sourceLocale.toLanguageTag() + '2' + targetLanguages.stream().collect(Collectors.joining("_")) + ".xliff").toAbsolutePath();

      try (Writer xliffWriter = Files.newBufferedWriter(xliffPath, StandardCharsets.UTF_8)) {
        xliffExporter.exportXliff(
                itemsToTranslate,
                xliffWriter,
                xliffExportOptions()
                        .option(EMPTY_IGNORE)
                        .option(TARGET_SOURCE)
                        .build());
      }
      LOG.debug("Exported XLIFF to: '{}'", xliffPath);
    } catch (IOException e) {
      throw new TranslineWorkflowException(XLIFF_EXPORT_FAILURE, "Failed to export XLIFF", e);
    }

    return xliffPath;
  }

  private Optional<Locale> findFirstMasterLocale(Collection<ContentObject> masterObjects, Function<ContentObjectSiteAspect, Locale> localeMapper) {
    SitesService sitesService = getSitesService();
    return masterObjects.stream().map(sitesService::getSiteAspect).map(localeMapper).findFirst();
  }

  private Map<Locale, List<TranslateItem>> getTranslationItemsByLocale(Collection<ContentObject> masterContentObjects,
                                                                       Collection<Content> derivedContents,
                                                                       Function<ContentObjectSiteAspect, Locale> localeMapper) {
    ContentToTranslateItemTransformer transformer = getSpringContext().getBean(ContentToTranslateItemTransformer.class);
    return transformer
            .transform(
                    masterContentObjects,
                    derivedContents,
                    localeMapper,
                    ITEM_PER_TARGET
            )
            .collect(groupingBy(TranslateItem::getSingleTargetLocale));
  }

  /**
   * Create a submission for the given translation items and return its unique identifier.
   *
   * @param facade                   the facade to communicate with Transline
   * @param subject                  subject, will be part of the submission name
   * @param comment                  comment, will be the instructions of the submission
   * @param sourceLocale             locale of master site
   * @param translationItemsByLocale translation items grouped by target locale
   * @param dueDate                  date that will be sent as 'dueDate' parameter
   * @param workflow                 workflow to be used for the translation, if not the default
   * @param submitter                username of the submitter
   * @return the result that contains the ID of the created submission or an error result
   * @throws TLCFacadeCommunicationException if submitting the submission failed
   */
  protected String submitSubmission(TLCExchangeFacade facade, String subject, String comment,
                                  Locale sourceLocale,
                                  Map<Locale, List<TranslateItem>> translationItemsByLocale) {
    String submissionId = facade.createSubmission(subject, comment, sourceLocale, translationItemsByLocale.keySet());

    // we assume that all items have to be translated in all target languages
    Optional<List<TranslateItem>> itemsToTranslate = translationItemsByLocale.values().stream().findFirst();
    if (itemsToTranslate.isPresent()) {
      uploadContents(facade, submissionId, sourceLocale, translationItemsByLocale.keySet(), itemsToTranslate.get());
    }

    facade.finishSubmission(submissionId);

    LOG.debug("Submitted submission {} to TLC.", submissionId);
    return submissionId;
  }

  private void uploadContents(TLCExchangeFacade facade, String submissionId, Locale sourceLocale, Set<Locale> targetLocales,
    List<TranslateItem> itemsToTranslate) {
    Path xliffPath = exportToXliff(sourceLocale, targetLocales, itemsToTranslate);
    try {
      facade.uploadContent(submissionId, new FileSystemResource(xliffPath));
      LOG.debug(
              "submitSubmission/Upload: Succeeded for {} translation items. SubmissionId {}.",
              itemsToTranslate.size(),
              submissionId);
    } finally {
      tryDeleteIfExists(xliffPath);
    }
  }

  private static void tryDeleteIfExists(Path xliffPath) {
    try {
      Files.deleteIfExists(xliffPath);
    } catch (IOException e) {
      LOG.error("Failed to delete temporary XLIFF file: '{}'", xliffPath, e);
    }
  }

  private static Locale preferSiteLocale(ContentObjectSiteAspect aspect) {
    Site site = aspect.getSite();
    if (site == null) {
      return aspect.getLocale();
    }
    return site.getLocale();
  }

  static final class Parameters {
    final String subject;
    final String comment;
    final Collection<Content> derivedContents;
    final Collection<ContentObject> masterContentObjects;
    final ZonedDateTime dueDate;
    final String workflow;
    final String submitter;

    Parameters(String subject,
               String comment,
               Collection<Content> derivedContents,
               Collection<ContentObject> masterContentObjects,
               ZonedDateTime dueDate,
               String workflow,
               String submitter) {
      this.subject = subject;
      this.comment = comment;
      this.derivedContents = derivedContents;
      this.masterContentObjects = masterContentObjects;
      this.dueDate = dueDate;
      this.workflow = workflow;
      this.submitter = submitter;
    }
  }

}
