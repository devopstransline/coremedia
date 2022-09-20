package de.transline.labs.translation.tlc.facade.mock;

import de.transline.labs.translation.tlc.facade.TLCExchangeFacade;
import de.transline.labs.translation.tlc.facade.TLCExchangeFacadeProvider;
import com.google.common.annotations.VisibleForTesting;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;

import java.util.Arrays;
import java.util.Map;

@DefaultAnnotation(NonNull.class)
public class MockTLCExchangeFacadeProvider implements TLCExchangeFacadeProvider {
  @VisibleForTesting
  static final String TYPE_TOKEN = "mock";

  private static final String CONFIG_DELAY_SECONDS = "mockDelaySeconds";
  private static final String CONFIG_DELAY_OFFSET_PERCENTAGE = "mockDelayOffsetPercentage";
  private static final String CONFIG_MOCK_ERROR = "mockError";

  enum MockError {
    DOWNLOAD_XLIFF, DOWNLOAD_COMMUNICATION, UPLOAD_COMMUNICATION, CANCEL_COMMUNICATION, CANCEL_RESULT
  }

  @Override
  public String getTypeToken() {
    return TYPE_TOKEN;
  }

  @Override
  public TLCExchangeFacade getFacade(Map<String, Object> settings) {
    MockedTLCExchangeFacade facade = new MockedTLCExchangeFacade();

    Object delaySeconds = settings.get(CONFIG_DELAY_SECONDS);
    if (delaySeconds != null) {
      facade.setDelayBaseSeconds(Long.parseLong(String.valueOf(delaySeconds)));
    }
    Object delayOffsetPercentage = settings.get(CONFIG_DELAY_OFFSET_PERCENTAGE);
    if (delayOffsetPercentage != null) {
      facade.setDelayOffsetPercentage(Integer.parseInt(String.valueOf(delayOffsetPercentage)));
    }
    String mockError = String.valueOf(settings.get(CONFIG_MOCK_ERROR));
    Arrays.stream(MockError.values())
            .filter(e -> e.toString().equalsIgnoreCase(mockError))
            .findAny()
            .ifPresent(facade::setMockError);

    return facade;
  }
}
