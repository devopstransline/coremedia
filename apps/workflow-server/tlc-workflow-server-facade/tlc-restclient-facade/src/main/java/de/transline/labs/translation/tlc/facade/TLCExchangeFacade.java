package de.transline.labs.translation.tlc.facade;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import org.springframework.core.io.Resource;

import java.io.InputStream;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;

/**
 * <p>
 * Facade for Access to Transline API
 * </p>
 * <p>
 * Get instances of facades via a {@link TLCExchangeFacadeSessionProvider}.
 * </p>
 */
@DefaultAnnotation(NonNull.class)
public interface TLCExchangeFacade {
  /**
   * Create submission.
   * @param subject       workflow subject
   * @param comment       instructions for translators (optional)
   * @param sourceLocale  source locale
   * @param targetLocales desired target locales to translate to
   * @return ID of the submission; to be used later on to upload the files, finish and track the state
   */
  String createSubmission(@Nullable String subject, @Nullable String comment, Locale sourceLocale,
      Set<Locale> targetLocales);

  /**
   * Uploads the given content, like for example an XLIFF file.
   *
   * @param submissionId the ID of the submission
   * @param resource the resource to send
   * @throws TLCFacadeIOException            if the resource cannot be read
   * @throws TLCFacadeCommunicationException if the file cannot be uploaded
   */
  void uploadContent(String submissionId, Resource resource);

  /**
   * Finish submission for the given contents uploaded before.
   * @param submissionId the ID of the submission
   * @throws TLCFacadeCommunicationException if finishing the submission failed
   * @see #uploadContent(String, Resource)
   */
  void finishSubmission(String submissionId);

  /**
   * Cancel a submission.
   *
   * @param submissionId the ID of the submission to cancel
   * @return The http result code of the underlying rest call
   */
  int cancelSubmission(String submissionId);

  /**
   * <p>
   * Downloads all completed tasks for the given submission ID. Downloaded
   * data are piped into {@code taskDataConsumer}.
   * </p>
   * <dl>
   * <dt><strong>TaskDataConsumer:</strong></dt>
   * <dd>
   * <p>
   * The {@code taskDataConsumer} has two important tasks:
   * </p>
   * <ul>
   * <li>apply the translation result to target contents (most likely XLIFF-import), and</li>
   * <li>signal, if it TLC backend shall be informed on successful delivery.</li>
   * </ul>
   * <p>
   * The {@code taskDataConsumer} must support two arguments:
   * </p>
   * <ul>
   * <li>An {@code InputStream}: The translation result</li>
   * <li>A {@code TLCTaskModel}: The task, which may be useful for logging or other tracking purposes.</li>
   * </ul>
   * <p>
   * The {@code taskDataConsumer} must return {@code true} if and only if you never will try to download the corresponding
   * data again &mdash; in other words: if really everything was successful.
   * </p>
   * <p>
   * To signal a failure you may as well return {@code false} as you may raise a
   * {@link RuntimeException}. Both approaches will skip from marking the corresponding
   * data as delivered. Both approaches will not escalate the workflow (the exception
   * will just be logged, not rethrown).
   * </p>
   * </dd>
   * </dl>
   *
   * @param submissionId     ID of the submission to download completed task data of
   * @param taskDataConsumer consumer for the input data
   * @throws TLCFacadeCommunicationException if completed tasks could not be downloaded or the {@code taskDataConsumer} threw an exception
   */
  boolean downloadCompletedTasks(String submissionId, BiPredicate<? super InputStream, ? super TLCTaskModel> taskDataConsumer);

  /**
   * Confirms the download of all completed tasks without actually downloading their data. This method basically
   * ignores the result of completed translations.
   * Will also add the Locales of the completed Tasks to the given 'completedLocales' Set.
   *
   * <p>Use {@link #downloadCompletedTasks(long, BiPredicate)} instead to download the translation for all completed
   * tasks and confirm delivery, if successful.
   *
   * @param submissionId     ID of the submission to download completed task data of
   * @param completedLocales a Set of Locales where the Locales of the completed Tasks to confirm will be added to
   * @throws TLCFacadeCommunicationException if the delivery for completed tasks could not be confirmed
   */
  void confirmCompletedTasks(String submissionId, Set<? super Locale> completedLocales);

  /**
   * <p>
   * Confirm all cancelled tasks of the given submission.
   * </p>
   *
   * @param submissionId ID of the submission
   * @throws TLCFacadeCommunicationException if cancelled tasks could not be confirmed
   */
  void confirmCancelledTasks(String submissionId);

  /**
   * Get the submission model which contains information like its state
   *
   * @param submissionId ID of the submission
   * @return the submission model
   * @throws TLCFacadeCommunicationException if the submission could not be retrieved
   */
  TLCSubmissionModel getSubmission(String submissionId);
}
