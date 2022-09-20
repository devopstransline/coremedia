package de.transline.labs.translation.tlc.facade;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;

/**
 * Signals some local IO failure.
 */
@SuppressWarnings("unused")
@DefaultAnnotation(NonNull.class)
public class TLCFacadeIOException extends TLCFacadeException {
  private static final long serialVersionUID = 8022138209281797363L;

  public TLCFacadeIOException() {
  }

  public TLCFacadeIOException(String message, @Nullable Object... args) {
    super(message, args);
  }

  public TLCFacadeIOException(Throwable cause, String message, @Nullable Object... args) {
    super(cause, message, args);
  }

  public TLCFacadeIOException(Throwable cause) {
    super(cause);
  }

  protected TLCFacadeIOException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
