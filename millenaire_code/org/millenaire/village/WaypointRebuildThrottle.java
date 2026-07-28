/*
 * Decompiled with CFR 0.152.
 */
package org.millenaire.village;

final class WaypointRebuildThrottle {
    static final long BACKOFF_WINDOW_TICKS = 600L;
    static final int BACKOFF_CAP = 6;
    static final long BACKOFF_MAX_TICKS = 1200L;
    private long lastRebuildTick = Long.MIN_VALUE;
    private long lastRequestTick = Long.MIN_VALUE;
    private int backoff;
    private boolean dirty = true;

    WaypointRebuildThrottle() {
    }

    void markDirty() {
        this.dirty = true;
    }

    boolean isDirty() {
        return this.dirty;
    }

    void onRebuilt(long now) {
        this.lastRebuildTick = now;
        this.dirty = false;
    }

    boolean tryAcquire(long now, long minIntervalTicks) {
        long sinceLastRequest;
        if (!this.dirty) {
            this.lastRequestTick = now;
            return false;
        }
        long sinceLastRebuild = this.lastRebuildTick == Long.MIN_VALUE ? Long.MAX_VALUE : now - this.lastRebuildTick;
        long l = sinceLastRequest = this.lastRequestTick == Long.MIN_VALUE ? Long.MAX_VALUE : now - this.lastRequestTick;
        if (sinceLastRequest > 600L) {
            this.backoff = 0;
        }
        this.lastRequestTick = now;
        long effectiveInterval = Math.min(minIntervalTicks << Math.min(this.backoff, 6), 1200L);
        if (sinceLastRebuild < effectiveInterval) {
            return false;
        }
        if (this.backoff < 6) {
            ++this.backoff;
        }
        return true;
    }
}

