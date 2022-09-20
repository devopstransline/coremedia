package de.transline.labs.translation.tlc.facade;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;

/**
 * Signals a configuration problem, like especially invalid settings.
 */
@SuppressWarnings("unused")
@DefaultAnnotation(NonNull.class)
public class TLCFacadeConfigException extends TLCFacadeException {
  private static final long serialVersionUID = 6482445402768874493L;

  public TLCFacadeConfigException() {
  }

  public TLCFacadeConfigException(String message, @Nullable Object... args) {
    super(message, args);
  }

  public TLCFacadeConfigException(Throwable cause, String message, @Nullable Object... args) {
    super(cause, message, args);
  }

  public TLCFacadeConfigException(Throwable cause) {
    super(cause);
  }

  protected TLCFacadeConfigException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
