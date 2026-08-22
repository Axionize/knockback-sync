package me.caseload.knockbacksync.integration;

import me.caseload.knockbacksync.latency.LatencyProvider;
import me.caseload.knockbacksync.latency.LatencyProviderMode;
import me.caseload.knockbacksync.latency.LatencyService;
import me.caseload.knockbacksync.latency.LatencySnapshot;
import me.caseload.knockbacksync.latency.LatencyTarget;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FabricLatencyIntegrationControllerTest {
    @Test
    void loadsOnlyAvailableGrimAndReturnsToPacketEventsWhenConfigured() {
        FakeTarget target = new FakeTarget();
        Collection<FakeTarget> targets = Collections.singleton(target);
        LatencyService service = new LatencyService(
                playerId -> target.id.equals(playerId) ? target : null,
                () -> targets
        );
        AtomicReference<LatencyProviderMode> mode = new AtomicReference<>(LatencyProviderMode.AUTO);
        AtomicBoolean grimLoaded = new AtomicBoolean(false);
        AtomicInteger providerCreations = new AtomicInteger();
        FakeProvider provider = new FakeProvider(target.id);

        FabricLatencyIntegrationController controller = new FabricLatencyIntegrationController(
                mode::get,
                service,
                Logger.getAnonymousLogger(),
                grimLoaded::get,
                () -> {
                    providerCreations.incrementAndGet();
                    return provider;
                }
        );
        controller.enable();

        assertEquals(0, providerCreations.get(), "absent Grim must not load the optional provider");
        assertFalse(target.external);

        grimLoaded.set(true);
        controller.reconfigure();

        AtomicInteger syntheticSends = new AtomicInteger();
        assertEquals(1, providerCreations.get());
        assertTrue(target.external);
        assertFalse(service.runSyntheticPingIfRequired(target, syntheticSends::incrementAndGet));
        assertEquals(0, syntheticSends.get());

        mode.set(LatencyProviderMode.PACKET_EVENTS);
        controller.reconfigure();

        assertTrue(provider.closed);
        assertFalse(target.external);
        assertTrue(service.runSyntheticPingIfRequired(target, syntheticSends::incrementAndGet));
        assertEquals(1, syntheticSends.get());
    }

    private static final class FakeProvider implements LatencyProvider {
        private final UUID trackedPlayer;
        private boolean closed;

        private FakeProvider(UUID trackedPlayer) {
            this.trackedPlayer = trackedPlayer;
        }

        @Override
        public LatencySnapshot snapshot(UUID playerId) {
            return trackedPlayer.equals(playerId)
                    ? LatencySnapshot.trackedWithoutSample()
                    : LatencySnapshot.untracked();
        }

        @Override
        public void close() {
            closed = true;
        }
    }

    private static final class FakeTarget implements LatencyTarget {
        private final UUID id = UUID.randomUUID();
        private boolean external;

        @Override
        public UUID getUniqueId() {
            return id;
        }

        @Override
        public boolean setExternalLatencyActive(boolean active) {
            boolean changed = external != active;
            external = active;
            return changed;
        }

        @Override
        public void recordPingSample(double pingMillis) {
        }
    }
}
