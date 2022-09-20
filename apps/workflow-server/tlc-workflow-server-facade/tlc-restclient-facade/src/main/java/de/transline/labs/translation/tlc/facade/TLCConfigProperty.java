package de.transline.labs.translation.tlc.facade;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * Properties to be used in {@code Transline} settings document.
 */
@DefaultAnnotation(NonNull.class)
public final class TLCConfigProperty {
  /**
   * Root node for TLC Settings in Struct.
   */
  public static final String KEY_TRANSLINE_ROOT = "transline";

  /**
   * The API URL for TLC REST endpoint.
   */
  public static final String KEY_URL = "url";

  /**
   * Connector Key for TLC connection.
   */
  public static final String KEY_KEY = "key";

  /**
   * Transline file type to use. Optional setting.
   * Defaults to first file type from list of file types that Transline returns in its connector config.
   */
  public static final String KEY_FILE_TYPE = "fileType";

  /**
   * Type of facade to instantiate. Optional key. Will default to
   * {@link #VALUE_TYPE_DEFAULT}.
   */
  public static final String KEY_TYPE = "type";

  /**
   * Boolean that determines if the name of the submitter is passed to Transline. For privacy reason this should be
   * deactivated by default.
   */
  public static final String KEY_IS_SEND_SUBMITTER = "isSendSubmitter";

  /**
   * Default type for facades. Will be used also if unset or if the type
   * cannot be parsed/is unknown.
   */
  static final String VALUE_TYPE_DEFAULT = "default";

  private TLCConfigProperty() {
  }
}
