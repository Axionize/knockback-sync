package me.caseload.knockbacksync.latency;

import org.jetbrains.annotations.Nullable;

public final class LatencySnapshot {
    private static final LatencySnapshot UNTRACKED = new LatencySnapshot(false, null);
    private static final LatencySnapshot TRACKED_WITHOUT_SAMPLE = new LatencySnapshot(true, null);

    private final boolean tracked;
    @Nullable private final Double pingMillis;

    private LatencySnapshot(boolean tracked, @Nullable Double pingMillis) {
        this.tracked = tracked;
        this.pingMillis = pingMillis;
    }

    public static LatencySnapshot untracked() {
        return UNTRACKED;
    }

    public static LatencySnapshot trackedWithoutSample() {
        return TRACKED_WITHOUT_SAMPLE;
    }

    public static LatencySnapshot tracked(double pingMillis) {
        return new LatencySnapshot(true, pingMillis);
    }

    public boolean isTracked() {
        return tracked;
    }

    public boolean hasSample() {
        return pingMillis != null;
    }

    public double getPingMillis() {
        if (pingMillis == null) {
            throw new IllegalStateException("No latency sample is available");
        }
        return pingMillis;
    }
}
