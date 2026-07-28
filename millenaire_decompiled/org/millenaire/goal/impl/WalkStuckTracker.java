/*
 * Decompiled with CFR 0.150.
 */
package org.millenaire.goal.impl;

final class WalkStuckTracker {
    private final int timeoutTicks;
    private final double progressEpsilonSq;
    private double bestDistSq = Double.MAX_VALUE;
    private int stuckTicks;

    WalkStuckTracker(int timeoutTicks, double progressEpsilonSq) {
        this.timeoutTicks = timeoutTicks;
        this.progressEpsilonSq = progressEpsilonSq;
    }

    boolean update(double distSq) {
        if (distSq < this.bestDistSq - this.progressEpsilonSq) {
            this.bestDistSq = distSq;
            this.stuckTicks = 0;
            return false;
        }
        ++this.stuckTicks;
        if (this.stuckTicks > this.timeoutTicks) {
            this.reset();
            return true;
        }
        return false;
    }

    void reset() {
        this.bestDistSq = Double.MAX_VALUE;
        this.stuckTicks = 0;
    }
}

