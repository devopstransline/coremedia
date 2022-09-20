package de.transline.labs.translation.tlc.facade;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;

/**
 * Signals a communication error with the TLC REST Backend via TLC Java RestClient.
 */
@SuppressWarnings("unused")
@DefaultAnnotation(NonNull.class)
public class TLCFacadeCommunicationException extends TLCFacadeException {
  private static final long serialVersionUID = -4226793602127027111L;

  public TLCFacadeCommunicationException() {
  }

  public TLCFacadeCommunicationException(String message, @Nullable Object... args) {
    super(message, args);
  }

  public TLCFacadeCommunicationException(Throwable cause, String message, @Nullable Object... args) {
    super(cause, message, args);
  }

  public TLCFacadeCommunicationException(Throwable cause) {
    super(cause);
  }

  protected TLCFacadeCommunicationException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
