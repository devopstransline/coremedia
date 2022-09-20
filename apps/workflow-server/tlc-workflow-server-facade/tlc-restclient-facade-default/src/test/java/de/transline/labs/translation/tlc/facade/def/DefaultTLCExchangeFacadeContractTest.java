package de.transline.labs.translation.tlc.facade.def;

import de.transline.labs.translation.tlc.facade.TLCExchangeFacade;
import de.transline.labs.translation.tlc.facade.TLCSubmissionState;
import de.transline.labs.translation.tlc.facade.TLCTaskModel;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.ByteSource;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.Logger;
import org.springframework.core.io.ByteArrayResource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.BiPredicate;

import static java.lang.invoke.MethodHandles.lookup;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * <p>
 * Contract test for TLC RestClient.
 * </p>
 * <p>
 * This is a test which should run on demand for example if you extended
 * the facade or if either the TLC Java API got updated or the corresponding
 * TLC REST Backend.
 * </p>
 * <p>
 * It is a so called contract test and thus tests the contract between
 * the consumer (this facade) and the producer (the TLC Java API).
 * </p>
 * <p>
 * In order to run the test, you need to add a file {@code .tlc.properties}
 * to your user home folder:
 * </p>
 * <pre>
 * url=https://connect-dev.translations.com/api/v2/
 * key=0e...abc
 * fileType=xliff
 * </pre>
 */
@ExtendWith(TlcCredentialsExtension.class)
class DefaultTLCExchangeFacadeContractTest {
  private static final Logger LOG = getLogger(lookup().lookupClass());
  private static final String XML_CONTENT = "<?xml version=\"1.0\" encoding=\"utf-8\" standalone=\"yes\"?><test>Lorem Ipsum</test>";
  private static final String XLIFF_CONTENT_PATTERN = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
          "<xliff xmlns=\"urn:oasis:names:tc:xliff:document:1.2\" version=\"1.2\">\n" +
          "  <file original=\"someId\" source-language=\"%s\" datatype=\"xml\" target-language=\"%s\">\n" +
          "    <body>\n" +
          "      <trans-unit id=\"1\" datatype=\"plaintext\">\n" +
          "        <source>Lorem Ipsum</source>\n" +
          "        <target>Lorem Ipsum</target>\n" +
          "      </trans-unit>\n" +
          "    </body>\n" +
          "  </file>\n" +
          "</xliff>\n";
  private static final int TRANSLATION_TIMEOUT_MINUTES = 30;
  private static final int SUBMISSION_VALID_TIMEOUT_MINUTES = 2;

  @Nested
  @DisplayName("Test general content submission")
  class ContentSubmission {
    @Test
    @DisplayName("Test simple submission")
    void submitXml(TestInfo testInfo, Map<String, Object> tlcProperties) {
      String testName = testInfo.getDisplayName();

      TLCExchangeFacade facade = new DefaultTLCExchangeFacade(tlcProperties);
      String submissionId = facade.createSubmission(testName, null, Locale.US, Collections.singleton(Locale.GERMANY));

      assertThat(submissionId).isNotNull();

      facade.uploadContent(submissionId, new ByteArrayResource(XML_CONTENT.getBytes(StandardCharsets.UTF_8)));
      facade.finishSubmission(submissionId);

      assertThat(facade.getSubmission(submissionId)).isNotNull();
    }
  }

  /**
   * This test addresses the full process of submitting files and receiving them
   * from TLC sandbox. The locales to use will be derived from the supported locales
   * at TLC. The test requires at minimum 2 supported locales and will fail otherwise.
   *
   * @param testInfo      test-info to generate names
   * @param tlcProperties properties to log in
   */
  @Test
  @Tag("slow")
  @Tag("full")
  @DisplayName("Translate XLIFF and receive results (takes about 10 Minutes)")
  void translateXliff(TestInfo testInfo, Map<String, Object> tlcProperties) {
    String testName = testInfo.getDisplayName();

    TLCExchangeFacade facade = new DefaultTLCExchangeFacade(tlcProperties);
    Set<Locale> targetLocales = Collections.singleton(Locale.GERMANY);
    Locale masterLocale = Locale.US;

    String submissionId = facade.createSubmission(testName, null, masterLocale, targetLocales);
    uploadContents(facade, submissionId, masterLocale, new ArrayList<>(targetLocales));
    facade.finishSubmission(submissionId);

    assertSubmissionReachesState(facade, submissionId, TLCSubmissionState.DONE, TRANSLATION_TIMEOUT_MINUTES);

    List<String> xliffResults = new ArrayList<>();

    facade.downloadCompletedTasks(submissionId, new TaskDataConsumer(xliffResults));

    assertThat(xliffResults)
            .describedAs("All XLIFFs shall have been pseudo-translated.")
            .hasSize(targetLocales.size())
            .allSatisfy(s -> assertThat(s).doesNotContain("<target>Lorem Ipsum"));

  }

  private static class TaskDataConsumer implements BiPredicate<InputStream, TLCTaskModel> {
    private final List<String> xliffResults;

    private TaskDataConsumer(List<String> xliffResults) {
      this.xliffResults = xliffResults;
    }

    @Override
    public boolean test(InputStream is, TLCTaskModel task) {
      ByteSource byteSource = new ByteSource() {
        @Override
        public InputStream openStream() {
          return is;
        }
      };
      try {
        StringBuilder xliffResult = new StringBuilder();
        byteSource.asCharSource(StandardCharsets.UTF_8).copyTo(xliffResult);
        xliffResults.add(xliffResult.toString());
      } catch (IOException e) {
        return false;
      }
      return true;
    }
  }

  private static void uploadContents(TLCExchangeFacade facade, String submissionId, Locale masterLocale, List<Locale> targetLocales) {
    for (Locale targetLocale : targetLocales) {
      String xliffContent = String.format(XLIFF_CONTENT_PATTERN, masterLocale.toLanguageTag(), targetLocale.toLanguageTag());
      facade.uploadContent(submissionId, new ByteArrayResource(xliffContent.getBytes(StandardCharsets.UTF_8)));
    }
  }

  private static void assertSubmissionReachesState(TLCExchangeFacade facade, String submissionId, TLCSubmissionState stateToReach, int timeout) {
    Awaitility.await("Wait for translation to complete.")
            .atMost(timeout, TimeUnit.MINUTES)
            .pollDelay(1, TimeUnit.MINUTES)
            .pollInterval(1, TimeUnit.MINUTES)
            .conditionEvaluationListener(condition -> LOG.info("Submission {}, Current State: {}, elapsed time in seconds: {}", submissionId, facade.getSubmission(submissionId).getState(), condition.getElapsedTimeInMS() / 1000L))
            .untilAsserted(() -> assertThat(facade.getSubmission(submissionId).getState()).isEqualTo(stateToReach));
  }
}
