package de.transline.labs.translation.tlc.workflow;

import de.transline.labs.translation.tlc.facade.TLCExchangeFacade;
import de.transline.labs.translation.tlc.facade.TLCSubmissionModel;
import de.transline.labs.translation.tlc.facade.TLCSubmissionState;
import de.transline.labs.translation.tlc.facade.TLCTaskModel;
import com.coremedia.cap.common.CapConnection;
import com.coremedia.cap.common.IdHelper;
import com.coremedia.cap.content.Content;
import com.coremedia.cap.content.ContentType;
import com.coremedia.cap.content.Version;
import com.coremedia.cap.translate.xliff.XliffImportResultItem;
import com.coremedia.cap.translate.xliff.config.XliffImporterConfiguration;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiPredicate;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import static de.transline.labs.translation.tlc.workflow.ActionTestBaseConfiguration.CONTENT_TYPE_NAME;
import static de.transline.labs.translation.tlc.workflow.ActionTestBaseConfiguration.LOCALE_PROPERTY;
import static de.transline.labs.translation.tlc.workflow.ActionTestBaseConfiguration.MASTER_PROPERTY;
import static de.transline.labs.translation.tlc.workflow.ActionTestBaseConfiguration.MASTER_VERSION_PROPERTY;
import static com.coremedia.cap.translate.xliff.XliffImportIssueSeverity.MAJOR;
import static com.coremedia.cap.translate.xliff.XliffImportResultCode.NO_SUCH_PROPERTY;
import static com.google.common.io.Resources.getResource;
import static java.util.Collections.singletonMap;
import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_SINGLETON;
import static org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS;

/**
 * Tests {@link DownloadFromTranslineAction}.
 */
@SuppressWarnings("UnstableApiUsage")
@ExtendWith(SpringExtension.class)
@ExtendWith(MockitoExtension.class)
@ContextConfiguration(classes = DownloadFromTranslineActionTest.LocalConfig.class)
@DirtiesContext(classMode = AFTER_CLASS)
class DownloadFromTranslineActionTest {

  private static final Pattern REPLACE_TARGET_PATTERN = Pattern.compile("(?<attr>cmxliff:target)=\"(?<value>[^\"]*)\"");
  private static final Pattern REPLACE_ORIGINAL_PATTERN = Pattern.compile("(?<attr>original)=\"(?<value>[^\"]*)\"");

  private Version masterVersion;
  private Content targetContent;

  @BeforeEach
  void setUp(TestInfo testInfo,
             @Autowired CapConnection connection) {
    ContentType contentType = requireNonNull(connection.getContentRepository().getContentType(CONTENT_TYPE_NAME), "Required content type not available.");
    Content masterContent = contentType.createByTemplate("/", testInfo.getDisplayName(), "{3} ({1})", singletonMap(LOCALE_PROPERTY, Locale.US.toLanguageTag()));
    masterVersion = masterContent.checkIn();
    targetContent = contentType.createByTemplate("/", testInfo.getDisplayName(), "{3} ({1})", ImmutableMap.<String, Object>builder()
            .put(LOCALE_PROPERTY, Locale.GERMANY.toLanguageTag())
            .put(MASTER_PROPERTY, Collections.singletonList(masterContent))
            .put(MASTER_VERSION_PROPERTY, IdHelper.parseVersionId(masterVersion.getId()))
            .build()
    );
    targetContent.checkIn();
  }

  @Test
  void executeXliffDownload(@Autowired DownloadFromTranslineAction action,
                            @Autowired TLCExchangeFacade gcExchangeFacade) throws IOException {
    String xliff = readXliff(masterVersion, targetContent);

    mockXliffDownload(gcExchangeFacade, xliff);

    action.doExecuteTranslineAction(new DownloadFromTranslineAction.Parameters("1", new HashSet<>(), false), r -> {
    }, gcExchangeFacade, new HashMap<>());
    assertThat(targetContent.getString("string")).isEqualTo("L??rem Ips??m");
  }

  @Nested
  @DisplayName("Tests error handling on XLIFF import.")
  @ContextConfiguration(classes = DownloadFromTranslineActionTest.LocalConfig.class)
  @DirtiesContext(classMode = AFTER_CLASS)
  class XliffImportErrorHandling {
    @Test
    void importErrorInResult(@Autowired DownloadFromTranslineAction action,
                             @Autowired TLCExchangeFacade gcExchangeFacade) throws IOException {
      String xliff = readXliff(masterVersion, targetContent)
              .replace("property:string:string", "property:string:nosuchproperty");

      mockXliffDownload(gcExchangeFacade, xliff);

      AtomicReference<DownloadFromTranslineAction.Result> resultHolder = new AtomicReference<>();
      action.doExecuteTranslineAction(new DownloadFromTranslineAction.Parameters("1", new HashSet<>(), false), resultHolder::set, gcExchangeFacade, new HashMap<>());
      DownloadFromTranslineAction.Result result = resultHolder.get();

      assertThat(result.resultItems.values().stream().anyMatch(list -> {
        XliffImportResultItem resultItem = list.isEmpty() ? null : list.get(0);
        return resultItem != null && resultItem.getCode().equals(NO_SUCH_PROPERTY) &&
                resultItem.getSeverity().equals(MAJOR) && resultItem.getContent().equals(targetContent) &&
                resultItem.getProperty().equals("nosuchproperty");
      })).isTrue();
      assertThat(result.resultItems).containsKey("1");
      assertThat(targetContent.getString("string")).isEmpty();
    }
  }

  private static void mockXliffDownload(@Autowired TLCExchangeFacade gcExchangeFacade, String xliff) {
    Mockito.doAnswer((Answer<Boolean>) invocationOnMock -> {
      BiPredicate<InputStream, TLCTaskModel> consumer = invocationOnMock.getArgument(1);
      ByteArrayInputStream inputStream = new ByteArrayInputStream(xliff.getBytes(StandardCharsets.UTF_8));
      consumer.test(inputStream, new TLCTaskModel(Locale.GERMANY, "url"));
      return true;
    }).when(gcExchangeFacade).downloadCompletedTasks(anyString(), any());
    Mockito.doReturn(new TLCSubmissionModel("1", TLCSubmissionState.DONE)).when(gcExchangeFacade).getSubmission(anyString());
  }

  private static String readXliff(Version masterVersion, Content targetContent) throws IOException {
    return readXliff(masterVersion.getId(), targetContent.getId());
  }

  private static String readXliff(String masterVersionId, String targetContentId) throws IOException {
    String raw = Resources.toString(getResource(DownloadFromTranslineActionTest.class, "LoremIpsum.xliff"), StandardCharsets.UTF_8);
    String result = REPLACE_TARGET_PATTERN.matcher(raw).replaceAll(String.format("${attr}=\"%s\"", targetContentId));
    result = REPLACE_ORIGINAL_PATTERN.matcher(result).replaceAll(String.format("${attr}=\"%s\"", masterVersionId));
    return result;
  }

  @Configuration
  @Import({ActionTestBaseConfiguration.class, XliffImporterConfiguration.class})
  static class LocalConfig extends ActionTestBaseConfiguration {
    @Scope(SCOPE_SINGLETON)
    @Bean
    public DownloadFromTranslineAction downloadFromTranslineAction(ApplicationContext context) {
      return new MockedDownloadFromTranslineAction(context);
    }
  }

  private static final class MockedDownloadFromTranslineAction extends DownloadFromTranslineAction {
    private static final long serialVersionUID = -4082795575498550151L;
    private final ApplicationContext applicationContext;

    private MockedDownloadFromTranslineAction(ApplicationContext applicationContext) {
      this.applicationContext = applicationContext;
    }

    @Override
    protected ApplicationContext getSpringContext() {
      return applicationContext;
    }

    @Override
    <T> T asRobotUser(Supplier<T> run) {
      return run.get();
    }
  }
}
