package de.transline.labs.translation.tlc.workflow;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;

@DefaultAnnotation(NonNull.class)
class TranslineWorkflowErrorCodes {

  // ==== 10###: General/Unknown Problems
  static final String UNKNOWN_ERROR   = "TLC-WF-10000";

  // ==== 20###: TLC RestClient Problems
  static final String TRANSLINE_COMMUNICATION_ERROR = "TLC-WF-20000";

  // ==== 30###: Local IO Problems
  static final String LOCAL_IO_ERROR = "TLC-WF-30001";

  // ==== 40###: Configuration Problems
  static final String SETTINGS_ERROR                    = "TLC-WF-40000";
  static final String SETTINGS_FILE_TYPE_ERROR          = "TLC-WF-40001";
  static final String ILLEGAL_SUBMISSION_ID_ERROR       = "TLC-WF-40050";

  // ==== 50###: XLIFF Problems
  static final String XLIFF_EXPORT_FAILURE = "TLC-WF-50050";

  // ==== 60###: TLC State Problems
  static final String SUBMISSION_CANCEL_FAILURE = "TLC-WF-61001";

  private TranslineWorkflowErrorCodes() {
  }

}
