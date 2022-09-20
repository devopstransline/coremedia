package de.transline.labs.translation.tlc.facade;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Arrays;

import static java.lang.invoke.MethodHandles.lookup;
import static java.util.Objects.nonNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * <p>
 * Convenience enum for status of the order which especially allows
 * to parse the String from a job list response for example.
 * </p>
 */
@DefaultAnnotation(NonNull.class)
public enum TLCSubmissionState {

  /**
   * Note: If you add or delete values of this Enum, also adapt the localization for Studio in the appurtenant file: 'TlcProcessDefinitions.properties' (also for other languages)
   */

  DRAFT("draft"),
  PREPARATION("preparation"),
  PRODUCTION("production"),
  DONE("done"),
  CANCELLED("canceled"),

  /**
   * Artificial submission status for a cancelled submission completely
   * being marked as cancellation confirmed. In other words a submission
   * is considered to be in state <em>Cancellation Confirmed</em> when
   * the submission is cancelled and all of its tasks are either
   * cancelled (confirmed) or delivered.
   */
  CANCELLATION_CONFIRMED("Cancellation Confirmed"),
  /**
   * Artificial job state for any other state. Note, that this may signal
   * an unexpected TLC API change. You may also use this state if you
   * actually don't care about more details on the state.
   */
  OTHER();

  private static final Logger LOG = getLogger(lookup().lookupClass());

  @Nullable
  private final String submissionStatusText;

  TLCSubmissionState() {
    this.submissionStatusText = null;
  }

  TLCSubmissionState(@NonNull String submissionStatusText) {
    this.submissionStatusText = submissionStatusText;
  }

  /**
   * Parse the status name and return the matching enum value. Empty, if
   * no status with the given name could be found.
   *
   * @param taskStatusName name to parse
   * @return status; {@link #OTHER} for any yet unknown status
   */
  public static TLCSubmissionState parseSubmissionStatusName(String taskStatusName) {
    if (taskStatusName == null) {
      LOG.warn("Submission status name unavailable (= null). Using OTHER as state.");
      return OTHER;
    }
    return Arrays.stream(values())
            .filter(s -> nonNull(s.submissionStatusText))
            .filter(s -> taskStatusName.equalsIgnoreCase(s.submissionStatusText))
            .findAny()
            .orElseGet(() -> {
              LOG.warn("Unknown submission state: {}. Using OTHER as state.", taskStatusName);
              return OTHER;
            });
  }
}
