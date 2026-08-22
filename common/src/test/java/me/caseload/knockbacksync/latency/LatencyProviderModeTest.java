package me.caseload.knockbacksync.latency;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LatencyProviderModeTest {
    @Test
    void parsesAllModesAndDefaultsInvalidValuesToAuto() {
        assertEquals(LatencyProviderMode.AUTO, LatencyProviderMode.fromConfig(null));
        assertEquals(LatencyProviderMode.AUTO, LatencyProviderMode.fromConfig("auto"));
        assertEquals(LatencyProviderMode.GRIM, LatencyProviderMode.fromConfig(" Grim "));
        assertEquals(LatencyProviderMode.PACKET_EVENTS, LatencyProviderMode.fromConfig("packet_events"));
        assertEquals(LatencyProviderMode.AUTO, LatencyProviderMode.fromConfig("unknown"));
    }
}
