package de.transline.labs.translation.tlc.facade;

import java.util.Locale;
import java.util.Objects;

/**
 * Model to store the data of a Task, returned by the TLC-Client.
 */
public class TLCTaskModel {
  private final Locale taskLocale;

  private final String downloadUrl;

  public TLCTaskModel(Locale taskLocale, String downloadUrl) {
    this.taskLocale = taskLocale;
    this.downloadUrl = downloadUrl;
  }

  public Locale getTaskLocale() {
    return taskLocale;
  }

  public String getDownloadUrl() {
    return downloadUrl;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TLCTaskModel that = (TLCTaskModel) o;
    return Objects.equals(taskLocale, that.taskLocale) &&
        downloadUrl.equals(that.downloadUrl);
  }

  @Override
  public int hashCode() {
    return Objects.hash(taskLocale, downloadUrl);
  }

  @Override
  public String toString() {
    return String.valueOf(taskLocale) + " -> " + downloadUrl;
  }
}
