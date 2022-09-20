/**
 * <p>
 * This package contains a facade for the TLC Rest Client. While it is especially
 * used for mocking, it also serves as overview which parts of the TLC Rest Client
 * API are actually used.
 * </p>
 * <p>
 * The main entry-point to this package is
 * {@link de.transline.labs.translation.tlc.facade.DefaultTLCExchangeFacadeSessionProvider} which
 * will open a session to TLC (or a mocked one) for you. The next important class
 * you should be aware of is the provided
 * {@link de.transline.labs.translation.tlc.facade.TLCExchangeFacade}
 * which is the facade to do all subsequent communication once you have
 * established a connection.
 * </p>
 */
package de.transline.labs.translation.tlc.facade;
