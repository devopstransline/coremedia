package de.transline.labs.translation.tlc.facade;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Factory which, depending on given settings, will create either a
 * default communication channel to TLC, a mocked one or a disabled one.
 */
@DefaultAnnotation(NonNull.class)
public final class DefaultTLCExchangeFacadeSessionProvider implements TLCExchangeFacadeSessionProvider {
  private static final Logger LOG = getLogger(lookup().lookupClass());

  private static final TLCExchangeFacadeSessionProvider INSTANCE = new DefaultTLCExchangeFacadeSessionProvider();

  private final List<TLCExchangeFacadeProvider> facadeProviders;

  private DefaultTLCExchangeFacadeSessionProvider() {
    ServiceLoader<TLCExchangeFacadeProvider> loader = ServiceLoader.load(TLCExchangeFacadeProvider.class);
    facadeProviders = StreamSupport.stream(loader.spliterator(), false).collect(Collectors.toUnmodifiableList());
  }

  /**
   * {@inheritDoc}
   *
   * @implNote Uses property {@value TLCConfigProperty#KEY_TYPE} to decide which
   * facade to instantiate.
   */
  @Override
  public TLCExchangeFacade openSession(Map<String, Object> settings) {
    String facadeType = String.valueOf(settings.getOrDefault(TLCConfigProperty.KEY_TYPE, TLCConfigProperty.VALUE_TYPE_DEFAULT));
    LOG.debug("Identified facade type to use: {}", facadeType);
    TLCExchangeFacadeProvider defaultFacadeProvider = null;
    for (TLCExchangeFacadeProvider facadeProvider : facadeProviders) {
      if (facadeProvider.isApplicable(facadeType)) {
        LOG.debug("Found TLCExchange facade provider: {}", facadeProvider);
        return facadeProvider.getFacade(settings);
      }
      if (facadeProvider.isDefault()) {
        defaultFacadeProvider = facadeProvider;
      }
    }
    if (defaultFacadeProvider == null) {
      throw new IllegalStateException("No TLCExchange facade available as default/fallback.");
    }
    return defaultFacadeProvider.getFacade(settings);
  }

  /**
   * Provides a singleton instance of the factory.
   *
   * @return factory
   */
  public static TLCExchangeFacadeSessionProvider defaultFactory() {
    return INSTANCE;
  }
}
