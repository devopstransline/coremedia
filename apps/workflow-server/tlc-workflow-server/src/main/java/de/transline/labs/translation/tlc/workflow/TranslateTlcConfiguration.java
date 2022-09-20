package de.transline.labs.translation.tlc.workflow;

import java.util.HashMap;
import java.util.Map;

import com.coremedia.cap.translate.xliff.config.XliffExporterConfiguration;
import com.coremedia.cap.translate.xliff.config.XliffImporterConfiguration;
import com.coremedia.translate.item.TranslateItemConfiguration;
import com.coremedia.translate.workflow.DefaultTranslationWorkflowDerivedContentsStrategy;
import com.coremedia.translate.workflow.TranslationWorkflowDerivedContentsStrategy;
import com.google.common.collect.ImmutableList;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import org.springframework.beans.factory.BeanNotOfRequiredTypeException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

@Configuration
@Import({
        XliffImporterConfiguration.class,
        XliffExporterConfiguration.class,
        TranslateItemConfiguration.class})
@PropertySource(value = "classpath:META-INF/coremedia/tlc-workflow.properties")
@DefaultAnnotation(NonNull.class)
public class TranslateTlcConfiguration {

  /**
   * A strategy for extracting derived contents from the default translation.xml workflow definition.
   *
   * @return translineTranslationWorkflowDerivedContentsStrategy
   */
  @Bean
  TranslationWorkflowDerivedContentsStrategy translineTranslationWorkflowDerivedContentsStrategy(){
    DefaultTranslationWorkflowDerivedContentsStrategy translineTranslationWorkflowDerivedContentsStrategy = new DefaultTranslationWorkflowDerivedContentsStrategy();
    translineTranslationWorkflowDerivedContentsStrategy.setProcessDefinitionName("TranslationTransline");

    return translineTranslationWorkflowDerivedContentsStrategy;
  }

  @ConfigurationProperties(prefix = "tlc")
  @Bean
  public Map<String, Object> tlcConfigurationProperties() {
    return new HashMap<>();
  }
}
