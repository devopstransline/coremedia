package de.transline.labs.translation.tlc.facade.def;

import de.transline.labs.translation.tlc.facade.TLCFacadeCommunicationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests {@link TLCUtil}.
 */
class TLCUtilTest {
  @ParameterizedTest
  @DisplayName("toUnixDateUtc: Dates should be represented as UTC")
  @ArgumentsSource(InstantArgumentsProvider.class)
  void toUnixDateUtcShouldRepresentDatesAsUtc(ZonedDateTime probe) {
    Date actualDate = TLCUtil.toUnixDateUtc(probe);
    ZonedDateTime expectedDateTime = probe.withZoneSameInstant(ZoneOffset.UTC);
    ZonedDateTime actualDateTime = ZonedDateTime.ofInstant(actualDate.toInstant(), ZoneOffset.UTC);
    assertThat(actualDateTime).isEqualToIgnoringNanos(expectedDateTime);
  }

  private static final class InstantArgumentsProvider implements ArgumentsProvider {
    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
      LocalDateTime someTime = LocalDateTime.of(2018, 7, 15, 13, 11, 30, 0);
      return Stream.concat(
              Stream.of(
                      ZonedDateTime.of(LocalDateTime.ofEpochSecond(0L, 0, ZoneOffset.UTC), ZoneOffset.UTC),
                      ZonedDateTime.of(LocalDateTime.of(2018, 10, 28, 2, 0, 0), ZoneId.of("Europe/Berlin")),
                      ZonedDateTime.of(LocalDateTime.of(2018, 10, 28, 3, 0, 0), ZoneId.of("Europe/Berlin")),
                      ZonedDateTime.of(LocalDateTime.now(), ZoneId.systemDefault())
              ),
              ZoneId.getAvailableZoneIds().stream()
                      .map(ZoneId::of)
                      .map(z -> ZonedDateTime.of(someTime, z))
      )
              .map(Arguments::of);
    }
  }
}
