package de.transline.labs.translation.tlc.workflow;

import com.coremedia.cap.common.CapException;

import static java.util.Arrays.stream;

class TranslineWorkflowException extends CapException {

  private static final long serialVersionUID = -6281116907297375413L;

  TranslineWorkflowException(String errorCode, String message, Object... parameters) {
    this(errorCode, message, null, parameters);
  }

  TranslineWorkflowException(String errorCode, String message, Throwable cause, Object... parameters) {
    super(
      "transline",
      errorCode,
      errorCode,
      message,
      stream(parameters).map(o -> o == null ? null : String.valueOf(o)).toArray(String[]::new),
      cause);
  }

}
