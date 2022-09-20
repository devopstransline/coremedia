package de.transline.labs.translation.tlc.facade.client;

import edu.umd.cs.findbugs.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Arrays;

import static java.util.Objects.nonNull;
import static org.slf4j.LoggerFactory.getLogger;
import static java.lang.invoke.MethodHandles.lookup;

/**
 * Created by aga on 26.01.2022.
 */
public enum PositionStatus {

    Created("created"),
    Prepared("prepared"),
    Excluded("excluded"),
    Planned("planned"),
    Production("production"),
    Finished("finished"),
    Delivered("delivered"),
    Canceled("canceled"),

    /**
     * Artificial job state for any other state. Note, that this may signal
     * an unexpected TLC API change. You may also use this state if you
     * actually don't care about more details on the state.
     */
    Other(null);

    private static final Logger LOG = getLogger(lookup().lookupClass());

    @Nullable
    private String value;

    PositionStatus(String value) {
        this.value = value;
    }

    /**
     * Parse the status name and return the matching enum value. Empty, if
     * no status with the given name could be found.
     *
     * @param statusName name to parse
     * @return status; {@link #Other} for any yet unknown status
     */
    public static PositionStatus parseStatusName(String statusName) {
        return Arrays.stream(values())
            .filter(s -> nonNull(s.value))
            .filter(s -> statusName.equalsIgnoreCase(s.value))
            .findAny()
            .orElseGet(() -> {
                LOG.warn("Unknown position state: {}. Using Other as state.", statusName);
                return Other;
            });
    }
}
