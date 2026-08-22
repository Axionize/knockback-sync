package me.caseload.knockbacksync.latency;

import me.caseload.knockbacksync.player.PingSampleState;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LatencyServiceTest {
    @Test
    void selectsExternalPerPlayerAndFallsBackWithoutAnInitialSample() throws Exception {
        FakeTarget target = new FakeTarget();
        Map<UUID, FakeTarget> targets = Collections.singletonMap(target.id, target);
        LatencyService service = service(targets);
        FakeProvider provider = new FakeProvider();
        provider.snapshots.put(target.id, LatencySnapshot.trackedWithoutSample());

        service.replaceProvider(provider);

        assertTrue(target.external);
        assertNull(target.samples.getPing());
        assertEquals(88.0, target.samples.getPingOr(88.0));

        AtomicInteger sends = new AtomicInteger();
        assertFalse(service.runSyntheticPingIfRequired(target, sends::incrementAndGet));
        assertEquals(0, sends.get(), "tracked Grim users must never receive a KBS synthetic ping");

        provider.fireSample(target.id, 52.0);
        provider.fireSample(target.id, 67.0);
        assertEquals(67.0, target.samples.getPing());
        assertEquals(52.0, target.samples.getPreviousPing());

        provider.snapshots.put(target.id, LatencySnapshot.untracked());
        provider.fireTracking(target.id, LatencySnapshot.untracked());
        assertFalse(target.external);
        assertNull(target.samples.getPing());
        assertTrue(service.runSyntheticPingIfRequired(target, sends::incrementAndGet));
        assertEquals(1, sends.get());
    }

    @Test
    void reloadKeepsTheLastSampleClosesOldProviderAndIgnoresLateEvents() throws Exception {
        FakeTarget target = new FakeTarget();
        Map<UUID, FakeTarget> targets = Collections.singletonMap(target.id, target);
        LatencyService service = service(targets);
        FakeProvider first = new FakeProvider();
        first.snapshots.put(target.id, LatencySnapshot.tracked(30.0));
        FakeProvider second = new FakeProvider();
        second.snapshots.put(target.id, LatencySnapshot.trackedWithoutSample());

        service.replaceProvider(first);
        assertEquals(30.0, target.samples.getPing());

        service.replaceProvider(second);
        assertTrue(first.closed);
        assertTrue(target.external);
        assertEquals(30.0, target.samples.getPing());
        first.fireSample(target.id, 200.0);
        assertEquals(30.0, target.samples.getPing());
    }

    @Test
    void activeProviderFailureFailsClosedForSyntheticSends() throws Exception {
        FakeTarget target = new FakeTarget();
        Map<UUID, FakeTarget> targets = Collections.singletonMap(target.id, target);
        LatencyService service = service(targets);
        LatencyProvider failing = playerId -> {
            throw new IllegalStateException("temporary provider failure");
        };

        service.replaceProvider(failing);

        AtomicInteger sends = new AtomicInteger();
        assertFalse(service.runSyntheticPingIfRequired(target, sends::incrementAndGet));
        assertEquals(0, sends.get());
    }

    private static LatencyService service(Map<UUID, FakeTarget> targets) {
        Collection<FakeTarget> values = targets.values();
        return new LatencyService(targets::get, () -> values);
    }

    private static final class FakeTarget implements LatencyTarget {
        private final UUID id = UUID.randomUUID();
        private final PingSampleState samples = new PingSampleState();
        private boolean external;

        @Override
        public UUID getUniqueId() {
            return id;
        }

        @Override
        public boolean setExternalLatencyActive(boolean active) {
            if (external == active) {
                return false;
            }
            external = active;
            samples.reset();
            return true;
        }

        @Override
        public void recordPingSample(double pingMillis) {
            samples.record(pingMillis);
        }
    }

    private static final class FakeProvider implements LatencyProvider {
        private final Map<UUID, LatencySnapshot> snapshots = new HashMap<>();
        private LatencyProviderListener listener;
        private boolean closed;

        @Override
        public LatencySnapshot snapshot(UUID playerId) {
            return snapshots.getOrDefault(playerId, LatencySnapshot.untracked());
        }

        @Override
        public void start(LatencyProviderListener listener) {
            this.listener = listener;
        }

        void fireTracking(UUID playerId, LatencySnapshot snapshot) {
            listener.onTrackingChanged(playerId, snapshot);
        }

        void fireSample(UUID playerId, double pingMillis) {
            listener.onSample(playerId, pingMillis);
        }

        @Override
        public void close() {
            closed = true;
        }
    }
}
