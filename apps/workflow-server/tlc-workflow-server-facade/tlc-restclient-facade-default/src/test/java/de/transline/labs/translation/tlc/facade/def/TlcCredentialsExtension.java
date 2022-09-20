package de.transline.labs.translation.tlc.facade.def;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.lang.reflect.Parameter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Properties;

import static java.lang.invoke.MethodHandles.lookup;
import static java.util.stream.Collectors.toMap;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Requires a test to have TLC credentials at hand. If they are not available,
 * the extended tests are ignored. If they are available, a test may get the
 * read credentials injected into a {@code Map<String, String>} parameter.
 */
public class TlcCredentialsExtension implements ExecutionCondition, ParameterResolver {
  private static final Logger LOG = getLogger(lookup().lookupClass());

  private static final String USER_HOME = System.getProperty("user.home");
  private static final Path TLC_CREDENTIALS_PATH = Paths.get(USER_HOME, ".tlc.properties");
  private Map<String, String> tlcProperties;

  @Override
  public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
    synchronized (this) {
      if (tlcProperties != null) {
        return ConditionEvaluationResult.enabled("TLC Properties already read.");
      }
    }
    if (!Files.exists(TLC_CREDENTIALS_PATH)) {
      return ConditionEvaluationResult.disabled("Missing properties file: " + TLC_CREDENTIALS_PATH + ". Test disabled.");
    }

    Properties properties = new Properties();
    try (BufferedReader reader = Files.newBufferedReader(TLC_CREDENTIALS_PATH, StandardCharsets.UTF_8)) {
      properties.load(reader);
    } catch (IOException e) {
      LOG.error("Failed to read properties file: {}", TLC_CREDENTIALS_PATH, e);
      return ConditionEvaluationResult.disabled("Failed to read properties file: " + TLC_CREDENTIALS_PATH + ". Test disabled. Cause: " + e.getMessage());
    }
    synchronized (this) {
      tlcProperties = properties.entrySet().stream()
        .collect(toMap(e -> String.valueOf(e.getKey()), e -> String.valueOf(e.getValue())));
    }
    return ConditionEvaluationResult.enabled("TLC Properties read from " + TLC_CREDENTIALS_PATH);
  }

  @Override
  public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
    Parameter parameter = parameterContext.getParameter();
    return Map.class.isAssignableFrom(parameter.getType());
  }

  @Override
  public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
    synchronized (this) {
      return tlcProperties;
    }
  }
}
