package de.transline.labs.translation.tlc.workflow;

import de.transline.labs.translation.tlc.facade.TLCConfigProperty;
import de.transline.labs.translation.tlc.facade.TLCExchangeFacade;
import de.transline.labs.translation.tlc.facade.mock.MockedTLCExchangeFacade;
import com.coremedia.cap.content.Content;
import com.coremedia.cap.multisite.Site;
import com.coremedia.cap.test.xmlrepo.XmlRepoConfiguration;
import com.coremedia.cap.workflow.Task;
import com.coremedia.springframework.xml.ResourceAwareXmlBeanDefinitionReader;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.Scope;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(
  classes = TranslineActionTest.LocalConfig.class
)
@DirtiesContext(classMode = AFTER_CLASS)
class TranslineActionTest {

  @Mock
  private Site site;

  @Autowired
  private TranslineAction translineAction;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Configuration
  @Import(XmlRepoConfiguration.class)
  @ImportResource(
    reader = ResourceAwareXmlBeanDefinitionReader.class
  )
  static class LocalConfig {
    @Scope(BeanDefinition.SCOPE_SINGLETON)
    @Bean
    public TranslineAction sendToTranslineAction(ApplicationContext context) {
      return new MockedTranslineAction(context);
    }
  }

  @Test
  void testOpenSession() {
    TLCExchangeFacade facade = translineAction.openSession(site);
    assertThat(facade).isNotNull();
    assertThat(facade).isInstanceOf(MockedTLCExchangeFacade.class);
  }

  private static class MockedTranslineAction extends TranslineAction<Void, Void> {
    private static final long serialVersionUID = -288745610618179168L;
    private final ApplicationContext applicationContext;

    private MockedTranslineAction(ApplicationContext applicationContext) {
      super(true);
      this.applicationContext = applicationContext;
    }

    @Override
    Void doExtractParameters(Task task) {
      return null;
    }

    @Override
    void doExecuteTranslineAction(Void params, Consumer<? super Void> resultConsumer,
                                   TLCExchangeFacade facade, Map<String, List<Content>> issues) {
    }

    @Override
    protected ApplicationContext getSpringContext() {
      return applicationContext;
    }

    @Override
    protected Map<String, Object> getTlcSettings(Site site) {
      return ImmutableMap.of(
              TLCConfigProperty.KEY_URL, "http://lorem.ipsum.fun/",
              TLCConfigProperty.KEY_KEY, "012345",
              TLCConfigProperty.KEY_TYPE, "mock");
    }
  }
}
