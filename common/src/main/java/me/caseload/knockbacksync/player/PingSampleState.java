package me.caseload.knockbacksync.player;

import org.jetbrains.annotations.Nullable;

/** Stores latency samples with cross-thread visibility for Bukkit/Folia/Netty readers. */
public final class PingSampleState {
    private final JitterCalculator jitterCalculator = new JitterCalculator();

    @Nullable private volatile Double ping;
    @Nullable private volatile Double previousPing;
    private volatile double jitter;

    public synchronized void record(double pingMillis) {
        previousPing = ping;
        ping = pingMillis;
        jitterCalculator.addPing(Math.round(pingMillis * 1_000_000.0));
        jitter = jitterCalculator.calculateJitter();
    }

    public synchronized void reset() {
        ping = null;
        previousPing = null;
        jitter = 0;
        jitterCalculator.reset();
    }

    @Nullable
    public Double getPing() {
        return ping;
    }

    @Nullable
    public Double getPreviousPing() {
        return previousPing;
    }

    public double getPingOr(double fallback) {
        Double current = ping;
        return current == null ? fallback : current;
    }

    public double getPreviousPingOr(double fallback) {
        Double previous = previousPing;
        return previous == null ? fallback : previous;
    }

    public double getJitter() {
        return jitter;
    }

    public JitterCalculator getJitterCalculator() {
        return jitterCalculator;
    }
}
