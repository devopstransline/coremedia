package de.transline.labs.translation.tlc.facade.def;

import de.transline.labs.translation.tlc.facade.TLCFacadeCommunicationException;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;

import java.time.ZonedDateTime;
import java.util.Date;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.time.ZoneOffset.UTC;

/**
 * Utility class for TLC Facade.
 */
@DefaultAnnotation(NonNull.class)
final class TLCUtil {
  private TLCUtil() {
    // Utility class
  }

  /**
   * Timestamps for TLC are always in UTC timezone. Thus, this method
   * converts the given zoned date-time object into a date for UTC timezone.
   *
   * @param dateTime date-time object to convert
   * @return date in UTC timezone
   */
  static Date toUnixDateUtc(ZonedDateTime dateTime) {
    ZonedDateTime utcDateTime = dateTime.withZoneSameInstant(UTC);
    return Date.from(utcDateTime.toInstant());
  }

}
