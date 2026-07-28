/*
 * Decompiled with CFR 0.152.
 */
package org.millenaire.goal.gathering;

final class InvalidationWindow {
    private final int windowTicks;
    private final int maxCount;
    private int count;
    private long windowStartTick = -1L;

    InvalidationWindow(int windowTicks, int maxCount) {
        this.windowTicks = windowTicks;
        this.maxCount = maxCount;
    }

    boolean record(long now) {
        if (this.windowStartTick < 0L || now - this.windowStartTick > (long)this.windowTicks) {
            this.windowStartTick = now;
            this.count = 0;
        }
        ++this.count;
        return this.count > this.maxCount;
    }

    int count() {
        return this.count;
    }

    int maxCount() {
        return this.maxCount;
    }
}

