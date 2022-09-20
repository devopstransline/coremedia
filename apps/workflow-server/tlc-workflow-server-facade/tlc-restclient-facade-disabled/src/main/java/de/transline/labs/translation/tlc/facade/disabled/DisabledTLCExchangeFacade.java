package de.transline.labs.translation.tlc.facade.disabled;

import de.transline.labs.translation.tlc.facade.TLCRestClient;
import de.transline.labs.translation.tlc.facade.TLCExchangeFacade;
import de.transline.labs.translation.tlc.facade.TLCExchangeFacadeSessionProvider;
import de.transline.labs.translation.tlc.facade.TLCFacadeException;
import de.transline.labs.translation.tlc.facade.TLCSubmissionModel;
import de.transline.labs.translation.tlc.facade.TLCTaskModel;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import org.springframework.core.io.Resource;

import java.io.InputStream;
import java.util.Locale;
import java.util.Set;
import java.util.function.BiPredicate;

/**
 * <p>
 * This facade will do essentially nothing. It will signal this either via
 * throwing exceptions or by providing default answers. One intended use case
 * is to disable communication to TLC by intention, for example for service
 * maintenance slots.
 * </p>
 * <p>
 * To get an instance of this facade, use {@link TLCExchangeFacadeSessionProvider}.
 * </p>
 */
@DefaultAnnotation(NonNull.class)
public final class DisabledTLCExchangeFacade implements TLCExchangeFacade {
  private static final TLCExchangeFacade INSTANCE = new DisabledTLCExchangeFacade();

  private DisabledTLCExchangeFacade() {
  }

  static TLCExchangeFacade getInstance() {
    return INSTANCE;
  }

  @Override
  public String createSubmission(@Nullable String subject, @Nullable String comment, Locale sourceLocale, Set<Locale> targetLocales) {
    throw createDisabledException();
  }

  @Override
  public void uploadContent(String submissionId, Resource resource) {
    throw createDisabledException();
  }

  @Override
  public void finishSubmission(String submissionId) {
    throw createDisabledException();
  }

  @Override
  public int cancelSubmission(String submissionId) {
    throw createDisabledException();
  }

  @Override
  public boolean downloadCompletedTasks(String submissionId, BiPredicate<? super InputStream, ? super TLCTaskModel> taskDataConsumer) {
    throw createDisabledException();
  }

  @Override
  public void confirmCompletedTasks(String submissionId, Set<? super Locale> completedLocales) {
    throw createDisabledException();
  }

  @Override
  public void confirmCancelledTasks(String submissionIds) {
    throw createDisabledException();
  }

  @Override
  public TLCSubmissionModel getSubmission(String submissionId) {
    return new TLCSubmissionModel(submissionId);
  }

  private static TLCFacadeException createDisabledException() {
    return new TLCFacadeException("TLC Service disabled.");
  }

}
