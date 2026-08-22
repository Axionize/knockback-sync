package me.caseload.knockbacksync.latency;

import me.caseload.knockbacksync.manager.PlayerDataManager;

import java.util.Collection;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Selects the latency source per player and provides the single gate through
 * which KnockbackSync may send a synthetic measurement packet.
 */
public final class LatencyService {
    private final Function<UUID, ? extends LatencyTarget> targetLookup;
    private final Supplier<? extends Collection<? extends LatencyTarget>> targets;

    private volatile LatencyProvider provider = LatencyProvider.PACKET_EVENTS;

    public LatencyService() {
        this(PlayerDataManager::getPlayerData, PlayerDataManager::getAllPlayerData);
    }

    public LatencyService(Function<UUID, ? extends LatencyTarget> targetLookup,
                          Supplier<? extends Collection<? extends LatencyTarget>> targets) {
        this.targetLookup = Objects.requireNonNull(targetLookup, "targetLookup");
        this.targets = Objects.requireNonNull(targets, "targets");
    }

    public synchronized void replaceProvider(LatencyProvider newProvider) throws Exception {
        Objects.requireNonNull(newProvider, "newProvider");
        LatencyProvider previous = provider;
        // Publish the replacement before detaching the old provider. Packet
        // senders therefore never observe a PacketEvents gap during reload.
        provider = newProvider;
        closeQuietly(previous);
        try {
            newProvider.start(new ProviderListener(newProvider));
            synchronizeAllPlayers();
        } catch (Throwable throwable) {
            provider = LatencyProvider.PACKET_EVENTS;
            closeQuietly(newProvider);
            setAllPlayersExternal(false);
            if (throwable instanceof Exception) {
                throw (Exception) throwable;
            }
            throw (Error) throwable;
        }
    }

    public synchronized void deactivate() {
        LatencyProvider previous = provider;
        provider = LatencyProvider.PACKET_EVENTS;
        closeQuietly(previous);
        setAllPlayersExternal(false);
    }

    public void synchronizePlayer(LatencyTarget target) {
        LatencyProvider selected = provider;
        LatencySnapshot snapshot = safeSnapshot(selected, target.getUniqueId());
        if (selected == provider) {
            applySnapshot(target, snapshot);
        }
    }

    public boolean shouldSuppressSyntheticPing(LatencyTarget target) {
        LatencyProvider selected = provider;
        LatencySnapshot snapshot = safeSnapshot(selected, target.getUniqueId());
        if (selected != provider) {
            return shouldSuppressSyntheticPing(target);
        }

        applySnapshot(target, snapshot);
        return snapshot.isTracked();
    }

    /**
     * Executes a synthetic packet send only when the selected external provider
     * does not currently track this player.
     */
    public boolean runSyntheticPingIfRequired(LatencyTarget target, Runnable packetSend) {
        if (shouldSuppressSyntheticPing(target)) {
            return false;
        }
        packetSend.run();
        return true;
    }

    private void synchronizeAllPlayers() {
        for (LatencyTarget target : targets.get()) {
            synchronizePlayer(target);
        }
    }

    private void setAllPlayersExternal(boolean active) {
        for (LatencyTarget target : targets.get()) {
            target.setExternalLatencyActive(active);
        }
    }

    private LatencySnapshot safeSnapshot(LatencyProvider selected, UUID playerId) {
        try {
            LatencySnapshot snapshot = selected.snapshot(playerId);
            return snapshot == null ? LatencySnapshot.untracked() : snapshot;
        } catch (Throwable ignored) {
            // Once an external provider is active, a transient API failure must
            // not open a window in which KBS sends a packet to a tracked user.
            return selected == LatencyProvider.PACKET_EVENTS
                    ? LatencySnapshot.untracked()
                    : LatencySnapshot.trackedWithoutSample();
        }
    }

    private void applySnapshot(LatencyTarget target, LatencySnapshot snapshot) {
        boolean changed = target.setExternalLatencyActive(snapshot.isTracked());
        if (changed && snapshot.hasSample()) {
            target.recordPingSample(snapshot.getPingMillis());
        }
    }

    private void closeQuietly(LatencyProvider selected) {
        if (selected == LatencyProvider.PACKET_EVENTS) {
            return;
        }
        try {
            selected.close();
        } catch (Throwable ignored) {
        }
    }

    private final class ProviderListener implements LatencyProviderListener {
        private final LatencyProvider source;

        private ProviderListener(LatencyProvider source) {
            this.source = source;
        }

        @Override
        public void onTrackingChanged(UUID playerId, LatencySnapshot snapshot) {
            if (provider != source) {
                return;
            }
            LatencyTarget target = targetLookup.apply(playerId);
            if (target != null) {
                applySnapshot(target, snapshot);
            }
        }

        @Override
        public void onSample(UUID playerId, double pingMillis) {
            if (provider != source || pingMillis < 0 || Double.isNaN(pingMillis) || Double.isInfinite(pingMillis)) {
                return;
            }
            LatencyTarget target = targetLookup.apply(playerId);
            if (target != null) {
                target.setExternalLatencyActive(true);
                target.recordPingSample(pingMillis);
            }
        }
    }
}
