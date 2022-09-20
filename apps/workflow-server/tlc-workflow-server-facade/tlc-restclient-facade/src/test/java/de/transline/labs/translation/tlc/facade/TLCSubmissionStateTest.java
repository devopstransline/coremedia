package de.transline.labs.translation.tlc.facade;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class TLCSubmissionStateTest {

  @ParameterizedTest
  @ValueSource(strings = {
    "draft",
    "preparation",
    "production",
    "done",
    "canceled"
})
  void someSubmissionStateForAnyTlcState(String stateForRestRequest) {
    assertThat(TLCSubmissionState.parseSubmissionStatusName(stateForRestRequest)).isNotNull();
  }

  @Test
  void acceptNullAsStateFromResponse() {
    // Directly after creating a submission its state may be null.
    assertThat(TLCSubmissionState.parseSubmissionStatusName(null)).isEqualTo(TLCSubmissionState.OTHER);
  }

}
