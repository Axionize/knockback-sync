package me.caseload.knockbacksync.integration.grim;

import ac.grim.grimac.api.GrimAPIProvider;
import ac.grim.grimac.api.GrimAbstractAPI;
import ac.grim.grimac.api.GrimUser;
import ac.grim.grimac.api.event.events.GrimJoinEvent;
import ac.grim.grimac.api.event.events.GrimQuitEvent;
import ac.grim.grimac.api.event.events.GrimTransactionReceivedEvent;
import ac.grim.grimac.api.plugin.GrimPlugin;
import me.caseload.knockbacksync.latency.LatencyProvider;
import me.caseload.knockbacksync.latency.LatencyProviderListener;
import me.caseload.knockbacksync.latency.LatencySnapshot;

import java.util.UUID;

/** The only production class that directly links against the optional Grim API. */
public final class GrimLatencyProvider implements LatencyProvider {
    private final Object owner;
    private final GrimAbstractAPI api;

    private GrimTransactionReceivedEvent.Channel transactionChannel;
    private GrimJoinEvent.Channel joinChannel;
    private GrimQuitEvent.Channel quitChannel;
    private GrimTransactionReceivedEvent.Handler transactionHandler;
    private GrimJoinEvent.Handler joinHandler;
    private GrimQuitEvent.Handler quitHandler;

    public GrimLatencyProvider(Object owner) {
        this(owner, GrimAPIProvider.get());
    }

    GrimLatencyProvider(Object owner, GrimAbstractAPI api) {
        this.owner = owner;
        this.api = api;
    }

    @Override
    public void start(LatencyProviderListener listener) {
        GrimPlugin grimPlugin = api.getGrimPlugin(owner);
        transactionChannel = api.getEventBus().get(GrimTransactionReceivedEvent.class);
        joinChannel = api.getEventBus().get(GrimJoinEvent.class);
        quitChannel = api.getEventBus().get(GrimQuitEvent.class);

        transactionHandler = (user, transactionId, cancelled, timestamp) -> {
            int ping = user.getTransactionPing();
            if (ping >= 0) {
                listener.onSample(user.getUniqueId(), ping);
            }
        };
        joinHandler = user -> listener.onTrackingChanged(user.getUniqueId(), snapshot(user));
        quitHandler = user -> listener.onTrackingChanged(user.getUniqueId(), LatencySnapshot.untracked());

        transactionChannel.onTransactionReceived(grimPlugin, transactionHandler);
        joinChannel.onJoin(grimPlugin, joinHandler);
        quitChannel.onQuit(grimPlugin, quitHandler);
    }

    @Override
    public LatencySnapshot snapshot(UUID playerId) {
        GrimUser user = api.getGrimUser(playerId);
        return user == null ? LatencySnapshot.untracked() : snapshot(user);
    }

    private LatencySnapshot snapshot(GrimUser user) {
        // Grim initializes transaction ping to zero. A received transaction is
        // what distinguishes a real zero-millisecond sample from no sample yet.
        if (user.getLastTransactionReceived() <= 0) {
            return LatencySnapshot.trackedWithoutSample();
        }
        return LatencySnapshot.tracked(user.getTransactionPing());
    }

    @Override
    public void close() {
        if (transactionChannel != null && transactionHandler != null) {
            transactionChannel.unsubscribe(transactionHandler);
        }
        if (joinChannel != null && joinHandler != null) {
            joinChannel.unsubscribe(joinHandler);
        }
        if (quitChannel != null && quitHandler != null) {
            quitChannel.unsubscribe(quitHandler);
        }

        transactionChannel = null;
        joinChannel = null;
        quitChannel = null;
        transactionHandler = null;
        joinHandler = null;
        quitHandler = null;
    }
}
