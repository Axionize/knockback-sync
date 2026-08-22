package me.caseload.knockbacksync.latency;

import java.util.UUID;

public interface LatencyTarget {
    UUID getUniqueId();

    /**
     * Switches the target's active latency source.
     *
     * @return true when the source changed
     */
    boolean setExternalLatencyActive(boolean active);

    void recordPingSample(double pingMillis);
}
