package de.transline.labs.translation.tlc.workflow;

import de.transline.labs.translation.tlc.facade.TLCExchangeFacade;
import com.coremedia.cap.common.CapConnection;
import com.coremedia.cap.common.IdHelper;
import com.coremedia.cap.content.Content;
import com.coremedia.cap.content.ContentObject;
import com.coremedia.cap.content.ContentType;
import com.coremedia.cap.content.Version;
import com.coremedia.cap.translate.xliff.config.XliffExporterConfiguration;
import com.coremedia.translate.item.TranslateItemConfiguration;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.ByteStreams;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.Resource;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_SINGLETON;
import static org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS;

/**
 * Tests {@link SendToTranslineAction}.
 */
@SuppressWarnings("UnstableApiUsage")
@ExtendWith(SpringExtension.class)
@ExtendWith(MockitoExtension.class)
@ContextConfiguration(classes = SendToTranslineActionTest.LocalConfig.class)
@DirtiesContext(classMode = AFTER_CLASS)
class SendToTranslineActionTest {

  @SuppressWarnings("unchecked")
  @Test
  void startTranslationJob(TestInfo testInfo,
                           @Autowired SendToTranslineAction action,
                           @Autowired TLCExchangeFacade gcExchangeFacade,
                           @Autowired CapConnection connection) {
    ContentType contentType = requireNonNull(connection.getContentRepository().getContentType(ActionTestBaseConfiguration.CONTENT_TYPE_NAME), "Required content type not available.");

    long expectedSubmissionId = 42L;
    Locale masterLocale = Locale.US;
    final Locale derivedLocale = Locale.GERMANY;

    String displayName = testInfo.getDisplayName();
    Content masterContent = contentType.createByTemplate("/", displayName, "{3} ({1})", ImmutableMap.<String,Object>builder()
            .put(ActionTestBaseConfiguration.LOCALE_PROPERTY, masterLocale.toLanguageTag())
            .put(ActionTestBaseConfiguration.TRANSLATABLE_STRING_PROPERTY, "Lorem Ipsum")
            .build()
    );
    Version masterVersion = masterContent.checkIn();
    List<ContentObject> masterContents = singletonList(masterContent);
    Content targetContent = contentType.createByTemplate("/", displayName, "{3} ({1})", ImmutableMap.<String, Object>builder()
      .put(ActionTestBaseConfiguration.LOCALE_PROPERTY, derivedLocale.toLanguageTag())
      .put(ActionTestBaseConfiguration.MASTER_PROPERTY, masterContents)
      .put(ActionTestBaseConfiguration.MASTER_VERSION_PROPERTY, IdHelper.parseVersionId(masterVersion.getId()))
      .build()
    );
    targetContent.checkIn();

    String expectedFileId = targetContent.getId();
    final String[] uploadedXliff = {null};

    Mockito.doAnswer(invocation -> readXliff(invocation, expectedFileId, uploadedXliff))
            .when(gcExchangeFacade)
            .uploadContent(anyString(), any(Resource.class));

    Mockito.doReturn(expectedSubmissionId).when(gcExchangeFacade).createSubmission(anyString(), anyString(), any(Locale.class), anySet());

    List<Content> derivedContents = singletonList(targetContent);
    String comment = "Test";
    ZonedDateTime dueDate = ZonedDateTime.of(LocalDateTime.now().plusDays(30), ZoneId.systemDefault());
    String workflow = "pseudo translation";
    String submitter = "admin";
    SendToTranslineAction.Parameters params = new SendToTranslineAction.Parameters(displayName, comment, derivedContents, masterContents, dueDate, workflow, submitter);
    AtomicReference<String> resultHolder = new AtomicReference<>();
    action.doExecuteTranslineAction(params, resultHolder::set, gcExchangeFacade, new HashMap<>());
    String submissionId = resultHolder.get();

    ArgumentCaptor<Set<Locale>> contentSetCaptor = ArgumentCaptor.forClass(Set.class);
    ArgumentCaptor<Locale> masterLocaleCaptor = ArgumentCaptor.forClass(Locale.class);

    Mockito.verify(gcExchangeFacade).uploadContent(anyString(), any(Resource.class));
    Mockito.verify(gcExchangeFacade).createSubmission(anyString(), anyString(), masterLocaleCaptor.capture(), contentSetCaptor.capture());

    assertThat(uploadedXliff[0])
            .describedAs("XLIFF shall contain all relevant information.")
            .isNotNull()
            .contains("Lorem Ipsum")
            .contains(masterVersion.getId())
            .contains(targetContent.getId());
    assertThat(masterLocaleCaptor.getValue()).isEqualTo(masterLocale);
    assertThat(submissionId).isEqualTo(String.valueOf(expectedSubmissionId));
  }

  private static Object readXliff(InvocationOnMock invocation, String expectedFileId, String[] uploadedXliff) throws IOException {
    Resource resource = invocation.getArgument(1);
    byte[] bytes;
    try (InputStream stream = resource.getInputStream()) {
      bytes = ByteStreams.toByteArray(stream);
    }
    uploadedXliff[0] = new String(bytes, StandardCharsets.UTF_8);
    return expectedFileId;
  }

  @Configuration
  @Import({ActionTestBaseConfiguration.class, XliffExporterConfiguration.class, TranslateItemConfiguration.class})
  static class LocalConfig extends ActionTestBaseConfiguration {
    @Scope(SCOPE_SINGLETON)
    @Bean
    public SendToTranslineAction sendToTranslineAction(ApplicationContext context) {
      return new MockedSendToTranslineAction(context);
    }
  }

  private static final class MockedSendToTranslineAction extends SendToTranslineAction {
    private static final long serialVersionUID = -4082795575498550151L;
    private final ApplicationContext applicationContext;

    private MockedSendToTranslineAction(ApplicationContext applicationContext) {
      this.applicationContext = applicationContext;
    }

    @Override
    protected ApplicationContext getSpringContext() {
      return applicationContext;
    }

  }
}
