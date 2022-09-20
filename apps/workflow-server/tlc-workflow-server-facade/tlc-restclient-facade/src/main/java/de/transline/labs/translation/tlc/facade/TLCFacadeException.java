package de.transline.labs.translation.tlc.facade;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;

/**
 * Base failure for any exceptions raised by this facade.
 */
@SuppressWarnings("WeakerAccess")
@DefaultAnnotation(NonNull.class)
public class TLCFacadeException extends RuntimeException {
  private static final long serialVersionUID = 8255693835569503552L;

  public TLCFacadeException() {
    super();
  }

  public TLCFacadeException(String message, @Nullable Object... args) {
    super(String.format(message, args));
  }

  public TLCFacadeException(Throwable cause, String message, @Nullable Object... args) {
    super(String.format(message, args), cause);
  }

  public TLCFacadeException(Throwable cause) {
    super(cause);
  }

  protected TLCFacadeException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
