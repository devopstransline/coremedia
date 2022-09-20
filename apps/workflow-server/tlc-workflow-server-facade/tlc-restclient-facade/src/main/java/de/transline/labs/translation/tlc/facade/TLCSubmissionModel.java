package de.transline.labs.translation.tlc.facade;

import java.util.List;
import java.util.Objects;

/**
 * Model to store the data of an order returned by the TLC-Client.
 */
public class TLCSubmissionModel {
  private final String submissionId;
  private final TLCSubmissionState state;

  /**
   * Create a submission that does not have a state yet. This state is reflected by {@link TLCSubmissionState#OTHER}.
   * @param submissionId the internal id used by the API
   * @param pdSubmissionIds the ids shown to editors
   */
  public TLCSubmissionModel(String submissionId) {
    this.submissionId = submissionId;
    this.state = TLCSubmissionState.OTHER;
  }

  public TLCSubmissionModel(String submissionId, TLCSubmissionState state) {
    this.submissionId = submissionId;
    this.state = state;
  }

  public String getSubmissionId() {
    return submissionId;
  }

  public TLCSubmissionState getState() {
    return state;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TLCSubmissionModel that = (TLCSubmissionModel) o;
    return submissionId.equals(that.submissionId) &&
            Objects.equals(state, that.state);
  }

  @Override
  public int hashCode() {
    return Objects.hash(submissionId, state);
  }

  @Override
  public String toString() {
    return submissionId;
  }
}
