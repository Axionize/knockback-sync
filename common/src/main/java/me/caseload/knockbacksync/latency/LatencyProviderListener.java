package me.caseload.knockbacksync.latency;

import java.util.UUID;

public interface LatencyProviderListener {
    void onTrackingChanged(UUID playerId, LatencySnapshot snapshot);

    void onSample(UUID playerId, double pingMillis);
}
