package de.transline.labs.translation.tlc.facade.def;

import de.transline.labs.translation.tlc.facade.TLCConfigProperty;
import de.transline.labs.translation.tlc.facade.TLCExchangeFacade;
import de.transline.labs.translation.tlc.facade.TLCFacadeCommunicationException;
import de.transline.labs.translation.tlc.facade.TLCFacadeIOException;
import de.transline.labs.translation.tlc.facade.TLCSubmissionState;
import de.transline.labs.translation.tlc.facade.TLCTaskModel;
import com.google.common.io.ByteStreams;
import de.transline.labs.translation.tlc.facade.client.Order;
import de.transline.labs.translation.tlc.facade.client.Position;
import de.transline.labs.translation.tlc.facade.client.DefaultTLCRestClient;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.BiPredicate;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests {@link DefaultTLCExchangeFacade} by mocking the TLC REST Client API.
 */
@ExtendWith(MockitoExtension.class)
@DefaultAnnotation(NonNull.class)
class DefaultTLCExchangeFacadeTest {
  private static final String LOREM_IPSUM = "Lorem Ipsum";
  @Mock
  private DefaultTLCRestClient tlcRestClient;

  @ParameterizedTest(name = "[{index}] Missing required parameter: ''{0}''")
  @DisplayName("Constructor should signal missing required keys in configuration.")
  @ValueSource(strings = {
          TLCConfigProperty.KEY_URL,
          TLCConfigProperty.KEY_KEY
  })
  void failOnMissingRequiredConfiguration(String excludedKey) {
    Map<String, Object> config = new HashMap<>();
    List<String> requiredKeys = new ArrayList<>(asList(
            TLCConfigProperty.KEY_URL,
            TLCConfigProperty.KEY_KEY
    ));
    requiredKeys.remove(excludedKey);

    for (String requiredKey : requiredKeys) {
      config.put(requiredKey, "non-null");
    }
    assertThatCode(() -> new DefaultTLCExchangeFacade(config)).hasMessageContaining(excludedKey);
  }

  @Test
  void wrapExceptionsDuringUpload(TestInfo testInfo) {
    String submissionId = "some id";
    Resource resource = new ByteArrayResource(new byte[]{42});
    doThrow(RuntimeException.class).when(tlcRestClient).uploadFile(submissionId, resource);

    TLCExchangeFacade facade = new MockDefaultTLCExchangeFacade(tlcRestClient);
    assertThatThrownBy(() -> facade.uploadContent(submissionId, resource))
            .isInstanceOf(TLCFacadeCommunicationException.class)
            .hasCauseInstanceOf(RuntimeException.class)
            .hasMessageContaining("Failed to upload content");
  }

  @Nested
  @DisplayName("Tests for createSubmission")
  class SubmitSubmission {
    @Test
    @DisplayName("Test for successful submission.")
    void happyPath(TestInfo testInfo) {
      String subject = testInfo.getDisplayName();
      String comment = "Test";
      Locale sourceLocale = Locale.US;
      Locale targetLocale = Locale.FRANCE;
      String expectedSubmissionId = "42";

      Set<Locale> targetLocales = Collections.singleton(targetLocale);
      Set<String> targetLanguages = targetLocales.stream()
              .map(Locale::toLanguageTag)
              .collect(toSet());

      String submissionName = "Test for successful submission. [en-US → fr-FR]";

      when(tlcRestClient.createOrder(submissionName, comment, sourceLocale.toLanguageTag(), targetLanguages)).thenReturn(expectedSubmissionId);

      TLCExchangeFacade facade = new MockDefaultTLCExchangeFacade(tlcRestClient);
      String submissionId = facade.createSubmission(subject, comment, sourceLocale,  Collections.singleton(targetLocale));

      assertThat(submissionId).isEqualTo(expectedSubmissionId);
    }

    @Test
    @DisplayName("Correctly deal with communication errors.")
    void dealWithCommunicationExceptions(TestInfo testInfo) {
      String subject = testInfo.getDisplayName();
      String comment = "Test";
      Locale sourceLocale = Locale.US;
      Locale targetLocale = Locale.FRANCE;

      when(tlcRestClient.createOrder(subject, comment, sourceLocale.toLanguageTag(), Collections.singleton(targetLocale.toLanguageTag()))).thenThrow(RuntimeException.class);

      TLCExchangeFacade facade = new MockDefaultTLCExchangeFacade(tlcRestClient);
      assertThatThrownBy(() -> facade.createSubmission(subject, comment, sourceLocale, Collections.singleton(targetLocale)))
              .isInstanceOf(TLCFacadeCommunicationException.class)
              .hasCauseInstanceOf(RuntimeException.class)
              .hasMessageContaining(subject)
              .hasMessageContaining(sourceLocale.toString());
    }
  }

  @Nested
  @DisplayName("Tests for downloadCompletedTasks")
  class DownloadCompletedTasks {
    private final String downloadUrl = "abc";

    @BeforeEach
    void setUp() {
      Order order = new Order();
      Position pos = new Position();
      pos.setStatus("delivered");
      pos.setTargetLanguage("de_CH");
      pos.setExternalTargetLanguage("de-CH");
      pos.setDelivery(Collections.singletonMap("file1", downloadUrl));
      order.setPositions(Collections.singletonList(pos));
      when(tlcRestClient.getOrder(any())).thenReturn(order);
    }

    @Nested
    @DisplayName("Tests for downloading single tasks")
    class SingleTasks {
      @Mock
      private TLCTaskModel gcTask;
      private final String expectedSubmissionId = "1234";
      private final long expectedTaskId = 5678L;
      private final byte[] expectedContent = LOREM_IPSUM.getBytes(StandardCharsets.UTF_8);

      @Test
      @DisplayName("Test for successful download.")
      void happyPath() throws IOException {
        when(tlcRestClient.downLoadFile(eq(downloadUrl))).thenReturn(new ByteArrayInputStream(expectedContent));

        TLCExchangeFacade facade = new MockDefaultTLCExchangeFacade(tlcRestClient);
        String[] actualContentRead = {null};

        facade.downloadCompletedTasks(expectedSubmissionId, new HappyPathTaskDataConsumer(actualContentRead));

        assertThat(actualContentRead[0]).isEqualTo(LOREM_IPSUM);
      }

      private class HappyPathTaskDataConsumer implements BiPredicate<InputStream, TLCTaskModel> {
        private final String[] actualContentRead;

        private HappyPathTaskDataConsumer(String[] actualContentRead) {
          this.actualContentRead = actualContentRead;
        }

        @Override
        public boolean test(InputStream is, TLCTaskModel task) {
          try {
            actualContentRead[0] = new String(ByteStreams.toByteArray(is), StandardCharsets.UTF_8);
            return true;
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        }
      }
    }
  }

  private static final class MockDefaultTLCExchangeFacade extends DefaultTLCExchangeFacade {
    MockDefaultTLCExchangeFacade(DefaultTLCRestClient tlcRestClient) {
      super(tlcRestClient);
    }
  }
}
