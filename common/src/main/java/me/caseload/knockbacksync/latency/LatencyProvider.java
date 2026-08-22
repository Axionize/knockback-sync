package me.caseload.knockbacksync.latency;

import java.util.UUID;

public interface LatencyProvider extends AutoCloseable {
    LatencyProvider PACKET_EVENTS = new LatencyProvider() {
        @Override
        public LatencySnapshot snapshot(UUID playerId) {
            return LatencySnapshot.untracked();
        }
    };

    LatencySnapshot snapshot(UUID playerId);

    default void start(LatencyProviderListener listener) throws Exception {
    }

    @Override
    default void close() {
    }
}
