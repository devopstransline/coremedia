package de.transline.labs.translation.tlc.facade;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;

import java.util.Map;

/**
 * An implementation of this factory may decide on different facades
 * to instantiate when opening a session. Some of these facades may
 * do real communication, some may provide mock answers for testing
 * purpose and others just may signal a disabled communication state.
 */
@DefaultAnnotation(NonNull.class)
@FunctionalInterface
public interface TLCExchangeFacadeSessionProvider {
  /**
   * <p>
   * Open an auto-closeable session with the given settings, which
   * by default should contain credentials given by keys as specified
   * in {@link TLCConfigProperty}.
   * </p>
   *
   * @param settings settings, like login credentials
   * @return a facade to communicate with TLC
   * @throws TLCFacadeException on error
   * @implSpec Factories may support extra settings in order to control which
   * facade to instantiate.
   */
  TLCExchangeFacade openSession(Map<String, Object> settings);
}
