package de.transline.labs.translation.tlc.facade.mock;

import de.transline.labs.translation.tlc.facade.TLCRestClient;
import de.transline.labs.translation.tlc.facade.TLCExchangeFacade;
import de.transline.labs.translation.tlc.facade.TLCExchangeFacadeSessionProvider;
import de.transline.labs.translation.tlc.facade.TLCFacadeCommunicationException;
import de.transline.labs.translation.tlc.facade.TLCSubmissionModel;
import de.transline.labs.translation.tlc.facade.TLCTaskModel;
import com.google.common.annotations.VisibleForTesting;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import org.slf4j.Logger;
import org.springframework.core.io.Resource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;

import static java.lang.invoke.MethodHandles.lookup;
import static java.util.stream.Collectors.toList;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * <p>
 * This facade will mock the behavior of TLC. It is especially meant for
 * demo cases and for testing purpose.
 * </p>
 * <p>
 * To get an instance of this facade, use {@link TLCExchangeFacadeSessionProvider}.
 * </p>
 */
@DefaultAnnotation(NonNull.class)
public final class MockedTLCExchangeFacade implements TLCExchangeFacade {
  private static final Logger LOG = getLogger(lookup().lookupClass());

  private static final ContentStore contentStore = new ContentStore();
  private static final SubmissionStore submissionStore = new SubmissionStore();
  @Nullable
  private MockTLCExchangeFacadeProvider.MockError mockError = null;

  MockedTLCExchangeFacade() {
  }

  @SuppressWarnings("SameParameterValue")
  @VisibleForTesting
  MockedTLCExchangeFacade setDelayBaseSeconds(long delayBaseSeconds) {
    submissionStore.setDelayBaseSeconds(delayBaseSeconds);
    return this;
  }

  @SuppressWarnings({"SameParameterValue", "UnusedReturnValue"})
  @VisibleForTesting
  MockedTLCExchangeFacade setDelayOffsetPercentage(int delayOffsetPercentage) {
    submissionStore.setDelayOffsetPercentage(delayOffsetPercentage);
    return this;
  }

  void setMockError(@Nullable MockTLCExchangeFacadeProvider.MockError mockError) {
    this.mockError = mockError;
  }

  @Override
  public String createSubmission(@Nullable String subject, @Nullable String comment, Locale sourceLocale, Set<Locale> targetLocales) {
    SubmissionContent content = new SubmissionContent(
            "1",
            // Reads and removes the content.
            contentStore.removeContent(),
            new ArrayList<>(targetLocales));
    return submissionStore.addSubmission(subject, Collections.singletonList(content));
  }

  @Override
  public void uploadContent(String submissionId, Resource resource) {
    if (mockError == MockTLCExchangeFacadeProvider.MockError.UPLOAD_COMMUNICATION) {
      throw new TLCFacadeCommunicationException("Exception to test upload communication errors with translation service.");
    }
    contentStore.addContent(resource);
  }

  @Override
  public void finishSubmission(String submissionId) {

  }

  @Override
  public int cancelSubmission(String submissionId) {

    return 200;  // http ok
  }

  @Override
  public boolean downloadCompletedTasks(String submissionId, BiPredicate<? super InputStream, ? super TLCTaskModel> taskDataConsumer) {
    Collection<Task> completedTasks = submissionStore.getCompletedTasks(submissionId);
    completedTasks.forEach(t -> downloadTask(t, taskDataConsumer));
    return true;
  }

  @Override
  public void confirmCompletedTasks(String submissionId, Set<? super Locale> completedLocales) {
    Collection<Task> completedTasks = submissionStore.getCompletedTasks(submissionId);
    for (Task completedTask : completedTasks) {
      completedLocales.add(completedTask.getTargetLocale());
      completedTask.markAsDelivered();
    }
  }

  private void downloadTask(Task task, BiPredicate<? super InputStream, ? super TLCTaskModel> taskDataConsumer) {
    if (mockError == MockTLCExchangeFacadeProvider.MockError.DOWNLOAD_COMMUNICATION) {
      throw new TLCFacadeCommunicationException("Exception to test download communication errors with translation service.");
    }
    boolean success = false;
    String untranslatedContent = task.getContent();
    String translatedContent = TranslationUtil.translateXliff(untranslatedContent, mockError == MockTLCExchangeFacadeProvider.MockError.DOWNLOAD_XLIFF);
    try (InputStream is = new ByteArrayInputStream(translatedContent.getBytes(StandardCharsets.UTF_8))) {
      success = taskDataConsumer.test(is, new TLCTaskModel(task.getTargetLocale(), task.getDownloadUrl()));
    } catch (IOException e) {
      LOG.warn("Failed to read stream.", e);
    }
    if (success) {
      task.markAsDelivered();
    }
  }

  @Override
  public void confirmCancelledTasks(String submissionId) {

  }

  @Override
  public TLCSubmissionModel getSubmission(String submissionId) {
    return new TLCSubmissionModel(submissionId, submissionStore.getSubmissionState(submissionId));
  }

}
