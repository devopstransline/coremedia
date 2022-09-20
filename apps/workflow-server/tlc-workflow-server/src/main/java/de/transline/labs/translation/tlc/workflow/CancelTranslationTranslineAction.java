package de.transline.labs.translation.tlc.workflow;

import de.transline.labs.translation.tlc.facade.TLCExchangeFacade;
import de.transline.labs.translation.tlc.facade.TLCSubmissionModel;
import de.transline.labs.translation.tlc.facade.TLCSubmissionState;
import com.coremedia.cap.content.Content;
import com.coremedia.cap.workflow.Process;
import com.coremedia.cap.workflow.Task;
import edu.umd.cs.findbugs.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static de.transline.labs.translation.tlc.facade.TLCSubmissionState.CANCELLATION_CONFIRMED;
import static de.transline.labs.translation.tlc.facade.TLCSubmissionState.CANCELLED;
import static de.transline.labs.translation.tlc.facade.TLCSubmissionState.DONE;
import static de.transline.labs.translation.tlc.workflow.TranslineWorkflowErrorCodes.SUBMISSION_CANCEL_FAILURE;
import static java.util.Objects.requireNonNull;

/**
 * Workflow action that cancels a Transline submission.
 */
public class CancelTranslationTranslineAction extends
        TranslineAction<CancelTranslationTranslineAction.Parameters, CancelTranslationTranslineAction.Result> {
  private static final long serialVersionUID = -4912724475227423848L;

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final int HTTP_OK = 200;

  private String translineSubmissionIdVariable;
  private String translinePdSubmissionIdsVariable;
  private String translineSubmissionStatusVariable;
  private String cancelledVariable;
  private String completedLocalesVariable;

  // --- construct and configure ------------------------------------

  public CancelTranslationTranslineAction() {
    // Escalate in case of errors.
    // Some particular exceptions are handled in storeResult
    // and suppressed from escalation.
    super(true);
  }

  /**
   * Sets the name of the string process variable holding the ID of the translation submission.
   *
   * @param translineSubmissionIdVariable string workflow variable name
   */
  @SuppressWarnings("unused") // set from workflow definition
  public void setTranslineSubmissionIdVariable(String translineSubmissionIdVariable) {
    this.translineSubmissionIdVariable = requireNonNull(translineSubmissionIdVariable);
  }

  /**
   * Sets the name of the process variable that holds the submission IDs shown to editors in Studio and
   * in the Transline tools.
   *
   * @param translinePdSubmissionIdsVariable string workflow variable name
   */
  @SuppressWarnings("unused") // set from workflow definition
  public void setTranslinePdSubmissionIdsVariable(String translinePdSubmissionIdsVariable) {
    this.translinePdSubmissionIdsVariable = requireNonNull(translinePdSubmissionIdsVariable);
  }

  /**
   * Sets the name of the String process variable that represents the Transline submission status
   *
   * @param translineSubmissionStatusVariable boolean workflow variable name
   */
  @SuppressWarnings("unused") // set from workflow definition
  public void setTranslineSubmissionStatusVariable(String translineSubmissionStatusVariable) {
    this.translineSubmissionStatusVariable = requireNonNull(translineSubmissionStatusVariable);
  }

  /**
   * Sets the name of the boolean process variable to set to true if a cancel request was sent to the
   * translation service.
   *
   * @param cancelledVariable boolean workflow variable name
   */
  @SuppressWarnings("unused") // set from workflow definition
  public void setCancelledVariable(String cancelledVariable) {
    this.cancelledVariable = cancelledVariable;
  }


  /**
   * Sets the name of the String process variable that represents the already translated locales
   *
   * @param completedLocalesVariable list workflow variable name
   */
  @SuppressWarnings("unused") // set from workflow definition
  public void setCompletedLocalesVariable(String completedLocalesVariable) {
    this.completedLocalesVariable = completedLocalesVariable;
  }


  // --- TranslineAction interface ----------------------------------------------------------------------

  @Override
  Parameters doExtractParameters(Task task) {
    Process process = task.getContainingProcess();
    String submissionId = process.getString(translineSubmissionIdVariable);
    boolean cancelled = process.getBoolean(cancelledVariable);
    Set<Locale> completedLocales = process.getStrings(completedLocalesVariable).stream()
            .map(Locale::forLanguageTag)
            .collect(Collectors.toCollection(HashSet::new));
    return new Parameters(parseSubmissionId(submissionId, task.getId()), cancelled, completedLocales);
  }

  @Override
  void doExecuteTranslineAction(Parameters params, Consumer<? super Result> resultConsumer,
                                 TLCExchangeFacade facade, Map<String, List<Content>> issues) {
    String submissionId = params.submissionId;
    TLCSubmissionModel submission = facade.getSubmission(submissionId);
    TLCSubmissionState submissionState = submission.getState();
    boolean cancelled = params.cancelled;

    // Also store the PD submission ids - potentially they were not available before
    Result result = new Result(submissionState, cancelled, params.completedLocales);
    resultConsumer.accept(result);

    // nothing to do, if submission is already cancelled and confirmed or delivered
    if (submissionState == CANCELLATION_CONFIRMED || submissionState == DONE) {
      return;
    }

    // not yet cancelled -> cancel
    if (!cancelled && submissionState != CANCELLED) {
      result.cancelled = cancel(facade, submissionId, issues);
      result.submissionState = facade.getSubmission(submissionId).getState();
    }

    // cancelled but not yet confirmed -> confirm
    if (result.submissionState == CANCELLED) {
      facade.confirmCancelledTasks(submissionId);
      result.submissionState = facade.getSubmission(submissionId).getState();
    }
  }

  @Nullable
  @Override
  Void doStoreResult(Task task, Result result) {
    Process process = task.getContainingProcess();
    process.set(translineSubmissionStatusVariable, result.submissionState.toString());
    process.set(cancelledVariable, result.cancelled);

    List<String> completedLocalesStringList = result.completedLocales.stream()
            .map(Locale::toLanguageTag)
            .collect(Collectors.toList());
    process.set(completedLocalesVariable, completedLocalesStringList);
    return null;
  }

  // --- Internal ----------------------------------------------------------------------

  private static boolean cancel(TLCExchangeFacade facade, String submissionId, Map<String, List<Content>> issues) {
    int httpStatus = facade.cancelSubmission(submissionId);
    if (httpStatus == HTTP_OK) {
      LOG.debug("Cancelled submission {}", submissionId);
      return true;

    }
    String errorCode = SUBMISSION_CANCEL_FAILURE;
    LOG.warn("Unable to cancel submission {}. Received status code: {} ({})", submissionId, httpStatus, errorCode);
    issues.put(errorCode, Collections.emptyList());
    return false;
  }

  static class Parameters {
    final String submissionId;
    final boolean cancelled;
    final Set<Locale> completedLocales;

    Parameters(String submissionId, boolean cancelled, Set<Locale> completedLocales) {
      this.submissionId = submissionId;
      this.cancelled = cancelled;
      this.completedLocales = completedLocales;
    }
  }

  static class Result {
    TLCSubmissionState submissionState;
    boolean cancelled;
    final Set<Locale> completedLocales;

    Result(TLCSubmissionState submissionState, boolean cancelled, Set<Locale> completedLocales) {
      this.submissionState = submissionState;
      this.cancelled = cancelled;
      this.completedLocales = completedLocales;
    }
  }
}
