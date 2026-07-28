/*
 * Decompiled with CFR 0.152.
 */
package org.millenaire.diagnostics;

public record NavEvent(long tick, Layer layer, Type type, String detail) {
    @Override
    public String toString() {
        return "[t=" + this.tick + "] " + String.valueOf((Object)this.layer) + "/" + String.valueOf((Object)this.type) + (String)(this.detail.isEmpty() ? "" : " " + this.detail);
    }

    public static enum Layer {
        VNM,
        WAYPOINT,
        GATHERING,
        REST,
        SCHEDULER,
        RELOAD;

    }

    public static enum Type {
        NAV_START,
        NAV_STOP,
        REPATH,
        SHORT_JUMP,
        TELEPORT,
        STUCK_DETECTED,
        TARGET_INVALID,
        ACTING_WATCHDOG_FIRED,
        GOAL_ABANDONED,
        POSE_SLEEPING_RESTORED,
        POSE_SLEEPING_CLEARED,
        BED_SUFFOCATION,
        LEAF_CLEAR_SKIPPED;

    }
}

