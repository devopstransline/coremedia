package de.transline.labs.translation.tlc.facade.def;

import de.transline.labs.translation.tlc.facade.TLCExchangeFacade;
import de.transline.labs.translation.tlc.facade.TLCExchangeFacadeProvider;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;

import java.util.Map;

@DefaultAnnotation(NonNull.class)
public class DefaultTLCExchangeFacadeProvider implements TLCExchangeFacadeProvider {
  private static final String TYPE_TOKEN = "default";

  @Override
  public String getTypeToken() {
    return TYPE_TOKEN;
  }

  @Override
  public boolean isDefault() {
    return true;
  }

  @Override
  public TLCExchangeFacade getFacade(Map<String, Object> settings) {
    return new DefaultTLCExchangeFacade(settings);
  }

}
