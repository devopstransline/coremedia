package de.transline.labs.translation.tlc.facade.def;

import de.transline.labs.translation.tlc.facade.TLCConfigProperty;
import de.transline.labs.translation.tlc.facade.TLCExchangeFacade;
import de.transline.labs.translation.tlc.facade.TLCRestClient;
import de.transline.labs.translation.tlc.facade.TLCExchangeFacadeSessionProvider;
import de.transline.labs.translation.tlc.facade.TLCFacadeCommunicationException;
import de.transline.labs.translation.tlc.facade.TLCFacadeConfigException;
import de.transline.labs.translation.tlc.facade.TLCFacadeException;
import de.transline.labs.translation.tlc.facade.TLCFacadeFileTypeConfigException;
import de.transline.labs.translation.tlc.facade.TLCFacadeIOException;
import de.transline.labs.translation.tlc.facade.TLCSubmissionModel;
import de.transline.labs.translation.tlc.facade.TLCSubmissionState;
import de.transline.labs.translation.tlc.facade.TLCTaskModel;
import com.google.common.annotations.VisibleForTesting;
import de.transline.labs.translation.tlc.facade.client.Order;
import de.transline.labs.translation.tlc.facade.client.Position;
import de.transline.labs.translation.tlc.facade.client.PositionStatus;
import de.transline.labs.translation.tlc.facade.client.DefaultTLCRestClient;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import org.slf4j.Logger;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Strings.nullToEmpty;
import static java.lang.invoke.MethodHandles.lookup;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static org.slf4j.LoggerFactory.getLogger;
import java.util.function.BiPredicate;

/**
 * <p>
 * This is the default facade which directly interacts with the TLC REST
 * Backend via TLC Java RestClient API.
 * </p>
 * <p>
 * To create an instance of this facade, use {@link TLCExchangeFacadeSessionProvider}.
 * </p>
 */
@DefaultAnnotation(NonNull.class)
public class DefaultTLCExchangeFacade implements TLCExchangeFacade {
  private static final Logger LOG = getLogger(lookup().lookupClass());
  /**
   * Maximum length of submission name. May require adjustment on
   * TLC update.
   */
  private static final int SUBMISSION_NAME_MAX_LENGTH = 150;
  private static final Integer HTTP_OK = 200;
  /**
   * Some string, so TLC can identify the source of requests.
   */
  //private static final String USER_AGENT = lookup().lookupClass().getPackage().getName();

  //private final GCExchange delegate;
  private final DefaultTLCRestClient tlcRestClient;

  /**
   * Constructor.
   *
   * @param config configuration using keys as provided in {@link TLCConfigProperty}.
   * @throws TLCFacadeConfigException        if configuration is incomplete
   * @throws TLCFacadeCommunicationException if connection to TLC failed.
   */
  DefaultTLCExchangeFacade(Map<String, Object> config) {
    String apiUrl = requireNonNullConfig(config, TLCConfigProperty.KEY_URL);
    String connectorKey = requireNonNullConfig(config, TLCConfigProperty.KEY_KEY);

    this.tlcRestClient = new DefaultTLCRestClient(apiUrl, connectorKey);
    assertSupportedFileType(String.valueOf(config.get(TLCConfigProperty.KEY_FILE_TYPE)));
  }

  @VisibleForTesting DefaultTLCExchangeFacade(DefaultTLCRestClient tlcRestClient) {
    this.tlcRestClient = tlcRestClient;
  }

  private static String requireNonNullConfig(Map<String, Object> config, String key) {
    Object value = config.get(key);
    if (value == null) {
      throw new TLCFacadeConfigException("Configuration for %s is missing. Configuration (values hidden): %s", key, config.entrySet().stream()
              .collect(toMap(Map.Entry::getKey, e -> TLCConfigProperty.KEY_URL.equals(e.getKey()) ? e.getValue() : "*****"))
      );
    }
    return String.valueOf(value);
  }

  @Override
  public void uploadContent(String submissionId, Resource resource) {
    try {
      tlcRestClient.uploadFile(submissionId, resource);
    } catch (TLCFacadeException e) {
      throw e;
    } catch (RuntimeException e) {
      throw new TLCFacadeCommunicationException(e, "Failed to upload content: %s", resource.getFilename());
    }
  }

  @Override
  public String createSubmission(@Nullable String subject, @Nullable String comment, Locale sourceLocale,
      Set<Locale> targetLocales) {

    Set<String> targetLanguages = targetLocales.stream()
        .map(Locale::toLanguageTag)
        .collect(toSet());

    try {
      return tlcRestClient.createOrder(
          createSubmissionName(subject, sourceLocale, targetLanguages),
          nullToEmpty(comment).trim(),
          sourceLocale.toLanguageTag(),
          targetLanguages
      );
    } catch (RuntimeException e) {
      throw new TLCFacadeCommunicationException(e, "Failed to create submission: subject=%s, source-locale=%s", subject, sourceLocale);
    }
  }

  @Override
  public void finishSubmission(String submissionId) {
    try {
      tlcRestClient.finishUpload(submissionId);
    } catch (RuntimeException e) {
      throw new TLCFacadeCommunicationException(e, "Failed to finish submission: %s", submissionId);
    }
  }

  @Override
  public int cancelSubmission(String submissionId) {
    return HTTP_OK;
  }

  /**
   * Generates a submission name which shall be suitable for easily detecting the
   * submission in project director. Submission name will be truncated
   * to the maximum submission name length available at TLC if required.
   *
   * @param subject      workflow subject
   * @param sourceLocale source locale
   * @param targetLanguages   target languages
   * @return a descriptive string.
   */
  private static String createSubmissionName(@Nullable String subject, Locale sourceLocale, Set<String> targetLanguages) {
    String trimmedSubject = nullToEmpty(subject).trim();
    if (trimmedSubject.length() >= SUBMISSION_NAME_MAX_LENGTH) {
      if (trimmedSubject.length() == SUBMISSION_NAME_MAX_LENGTH) {
        LOG.debug("Given subject at maximum length {}. Skipping applying further information to subject.", SUBMISSION_NAME_MAX_LENGTH);
      } else {
        String truncatedSubject = trimmedSubject.substring(0, SUBMISSION_NAME_MAX_LENGTH);
        LOG.warn("Given subject exceeds maximum length of {}. Will truncate subject and skip adding further information: {} → {}",
                SUBMISSION_NAME_MAX_LENGTH,
                trimmedSubject,
                truncatedSubject);
        trimmedSubject = truncatedSubject;
      }
      return trimmedSubject;
    }

    String allTargetLocales = targetLanguages.stream().collect(joining(", "));

    if (trimmedSubject.isEmpty()) {
      trimmedSubject = Instant.now().toString();
    }
    trimmedSubject = trimmedSubject + " [" + sourceLocale.toLanguageTag() + " → " + allTargetLocales + ']';
    if (trimmedSubject.length() > SUBMISSION_NAME_MAX_LENGTH) {
      return trimmedSubject.substring(0, SUBMISSION_NAME_MAX_LENGTH);
    }
    return trimmedSubject;
  }

  @Override
  public boolean downloadCompletedTasks(String submissionId, BiPredicate<? super InputStream, ? super TLCTaskModel> taskDataConsumer) {
    // we want to download only if all tasks are completed
    List<TLCTaskModel> completedTasks = getTasksIfAllCompleted(submissionId);

    if (!completedTasks.isEmpty()) {
      LOG.debug("Tasks completed of submission {}", submissionId);

      completedTasks.forEach(task -> downloadTask(task, taskDataConsumer));
      return true;
    } else {
      return false;
    }
  }

  private void downloadTask(TLCTaskModel task, BiPredicate<? super InputStream, ? super TLCTaskModel> taskDataConsumer) {
    String downloadUrl = task.getDownloadUrl();
    try (InputStream is = tlcRestClient.downLoadFile(downloadUrl)) {
      if (taskDataConsumer.test(is, task)) {
        LOG.debug("Downloaded task {}", downloadUrl);
      }
    } catch (IOException | RuntimeException e) {
      throw new TLCFacadeCommunicationException(e, "Failed to download and confirm delivery for the task %s", task.toString());
    }
  }

  // Used only by CancelTranslationTranslineAction
  @Override
  public void confirmCompletedTasks(String submissionId, Set<? super Locale> completedLocales) {
    List<TLCTaskModel> completedTasks = getOnlyCompletedTasks(submissionId);

    LOG.debug("Completed Task IDs of submission {}: {}", submissionId, completedTasks);

    for (TLCTaskModel task : completedTasks) {
      try {
        completedLocales.add(task.getTaskLocale());
        LOG.debug("Confirmed delivery for the task {} of submission {}", task.toString(), submissionId);
      } catch (RuntimeException e) {
        throw new TLCFacadeCommunicationException(e, "Failed to confirm delivery for the task %s", task.toString());
      }
    }
  }

  @Override
  public void confirmCancelledTasks(String submissionId) {
  }

  /**
   * Retrieves all positions of the given submission.
   *
   * @param submissionId submission ID
   * @return list of positions
   * @throws TLCFacadeCommunicationException if positions could be not be retrieved.
   */
  private List<Position> getPositions(String submissionId) {
    Order order = getOrderById(submissionId);
    if (order != null) {
      return order.getPositions();
    }
    return Collections.emptyList();
  }

  /**
   * Retrieves all tasks of the given submission if they are all completed.
   *
   * @param submissionId submission ID
   * @return list of {@link TLCTaskModel}
   * @throws TLCFacadeCommunicationException if tasks could be not be retrieved.
   */
  private List<TLCTaskModel> getTasksIfAllCompleted(String submissionId) {
    List<Position> positions = getPositions(submissionId);

    positions = positions.stream().filter(p -> p.getExternalTargetLanguage() != null
            && !p.getExternalTargetLanguage().isEmpty()
            && p.getTargetLanguage() != null
            && !p.getTargetLanguage().isEmpty()
    ).collect(Collectors.toList());

    boolean allCompleted = positions.stream().allMatch(p -> PositionStatus.parseStatusName(p.getStatus()) == PositionStatus.Delivered && !p.getDelivery().isEmpty());
    if (allCompleted) {
      return positions.stream().map(p ->
              new TLCTaskModel(
                  new Locale.Builder().setLanguageTag(p.getExternalTargetLanguage()).build(),
                  // we assume there is only one delivery file because we sent only one source file
                  p.getDelivery().values().stream().findFirst().get()
              )
          ).collect(toList());
    }
    return Collections.emptyList();
  }

  /**
   * Retrieves only completed tasks of the given submission.
   *
   * @param submissionId submission ID
   * @return list of {@link TLCTaskModel}
   * @throws TLCFacadeCommunicationException if tasks could be not be retrieved.
   */
  private List<TLCTaskModel> getOnlyCompletedTasks(String submissionId) {
    List<Position> positions = getPositions(submissionId);
    return positions.stream()
        .filter(p -> PositionStatus.parseStatusName(p.getStatus()) == PositionStatus.Delivered && !p.getDelivery().isEmpty())
        .map(p -> new TLCTaskModel(
            new Locale.Builder().setLanguageTag(p.getExternalTargetLanguage()).build(),
            // we assume there is only one delivery file because we sent only one source file
            p.getDelivery().values().stream().findFirst().get()
          )
        ).collect(toList());
  }

  @Override
  public TLCSubmissionModel getSubmission(String submissionId) {
    Order order = getOrderById(submissionId);
    if (order == null) {
      LOG.warn("Failed to retrieve order for ID {}. Will fallback to signal submission state OTHER.", submissionId);
      return new TLCSubmissionModel(submissionId);
    }
    TLCSubmissionState state = TLCSubmissionState.parseSubmissionStatusName(order.getStatus());

    if (state == TLCSubmissionState.CANCELLED) {
      /*
       * In order to know, that there is no more interaction with TLC backend
       * required to put the submission into a valid finished state, we split
       * the cancelled state into two. Only when the state is
       * "Cancellation Confirmed" there is nothing more to do. When it is
       * only "Cancelled" there are still tasks which need to be finished
       * either by confirming their cancellation or by downloading their
       * results.
       */
      if (areAllSubmissionTasksDone(submissionId)) {
        state = TLCSubmissionState.CANCELLATION_CONFIRMED;
      } else {
        state = TLCSubmissionState.CANCELLED;
      }
    }
    return new TLCSubmissionModel(submissionId, state);
  }

  /**
   * All submission tasks are considered done if they are either
   * delivered or canceled.
   *
   * @param submissionId ID of submission
   * @return {@code true} if all tasks are considered done; {@code false} otherwise
   * @throws TLCFacadeCommunicationException if the status could not be retrieved.
   */
  private boolean areAllSubmissionTasksDone(String submissionId) {
    AtomicBoolean allDone = new AtomicBoolean(true);
    List<Position> positions = getPositions(submissionId);

    positions = positions.stream().filter(p -> p.getExternalTargetLanguage() != null
            && !p.getExternalTargetLanguage().isEmpty()
            && p.getTargetLanguage() != null
            && !p.getTargetLanguage().isEmpty()
    ).collect(Collectors.toList());

    for (Position position : positions) {
      PositionStatus status = PositionStatus.parseStatusName(position.getStatus());
      if (status != PositionStatus.Delivered && status != PositionStatus.Canceled) {
        allDone.set(false);
        break;
      }
    }
    return allDone.get();
  }

  /**
   * Retrieves the submission by ID.
   *
   * @param orderId ID of the oirder
   * @return order found; {@code null} if not found
   * @throws TLCFacadeCommunicationException if unable to retrieve.
   */
  @Nullable
  private Order getOrderById(String orderId) {
    try {
      return tlcRestClient.getOrder(orderId);
    } catch (RuntimeException e) {
      throw new TLCFacadeCommunicationException(e, "Failed to retrieve order for order ID " + orderId);
    }
  }

  private void assertSupportedFileType(String configuredFileType) {
    List<String> supportedFileTypes = Stream.of("xliff").collect(toList());

    if (configuredFileType != null && !supportedFileTypes.contains(configuredFileType)) {
      throw new TLCFacadeFileTypeConfigException("Configured file type '%s' not in supported ones %s for Transline " +
          "connection", configuredFileType, supportedFileTypes);
    }
  }
}
