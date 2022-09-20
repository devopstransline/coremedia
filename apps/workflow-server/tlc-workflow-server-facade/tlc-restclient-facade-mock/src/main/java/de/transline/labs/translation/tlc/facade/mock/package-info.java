/**
 * <p>
 * Contains classes for mock translation. Main entry point is the mock facade:
 * {@link de.transline.labs.translation.tlc.facade.mock.MockedTLCExchangeFacade}. All
 * other classes are meant to support mocking and to keep state in between
 * the different calls to the facade.
 * </p>
 * <p>
 * The class {@link de.transline.labs.translation.tlc.facade.mock.Task} contains
 * some intended latency to simulate that translation of contents takes some
 * time.
 * </p>
 */
package de.transline.labs.translation.tlc.facade.mock;
