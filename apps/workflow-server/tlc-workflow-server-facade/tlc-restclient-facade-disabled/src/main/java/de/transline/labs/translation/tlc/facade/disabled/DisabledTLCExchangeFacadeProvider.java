package de.transline.labs.translation.tlc.facade.disabled;

import de.transline.labs.translation.tlc.facade.TLCExchangeFacade;
import de.transline.labs.translation.tlc.facade.TLCExchangeFacadeProvider;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;

import java.util.Map;

@DefaultAnnotation(NonNull.class)
public class DisabledTLCExchangeFacadeProvider implements TLCExchangeFacadeProvider {
  private static final String TYPE_TOKEN = "disabled";

  @Override
  public String getTypeToken() {
    return TYPE_TOKEN;
  }

  @Override
  public TLCExchangeFacade getFacade(Map<String, Object> settings) {
    return DisabledTLCExchangeFacade.getInstance();
  }
}
