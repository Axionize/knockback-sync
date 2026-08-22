package me.caseload.knockbacksync.latency;

import java.util.Locale;

public enum LatencyProviderMode {
    AUTO,
    PACKET_EVENTS,
    GRIM;

    public static LatencyProviderMode fromConfig(String value) {
        if (value == null) {
            return AUTO;
        }

        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return AUTO;
        }
    }
}
