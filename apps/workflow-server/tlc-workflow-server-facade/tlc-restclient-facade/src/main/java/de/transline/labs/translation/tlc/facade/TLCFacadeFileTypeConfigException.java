package de.transline.labs.translation.tlc.facade;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;

/**
 * Thrown if the configured file type is not supported by Transline.
 */
@DefaultAnnotation(NonNull.class)
public class TLCFacadeFileTypeConfigException extends TLCFacadeConfigException {
  private static final long serialVersionUID = 2180398367798706948L;

  public TLCFacadeFileTypeConfigException() {
  }

  public TLCFacadeFileTypeConfigException(String message, @Nullable Object... args) {
    super(message, args);
  }

  public TLCFacadeFileTypeConfigException(Throwable cause, String message, @Nullable Object... args) {
    super(cause, message, args);
  }

  public TLCFacadeFileTypeConfigException(Throwable cause) {
    super(cause);
  }

  public TLCFacadeFileTypeConfigException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }

}
