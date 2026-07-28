/*
 * Decompiled with CFR 0.150.
 */
package org.millenaire.diagnostics;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.millenaire.diagnostics.NavEvent;

public final class NavigationCounters {
    private static final AtomicLong TELEPORT_TOTAL = new AtomicLong();
    private static final Map<NavEvent.Layer, AtomicLong> TELEPORT_PER_LAYER = new EnumMap<NavEvent.Layer, AtomicLong>(NavEvent.Layer.class);
    private static final AtomicLong GOAL_ABANDONED = new AtomicLong();
    private static final AtomicLong TARGET_INVALID = new AtomicLong();
    private static final AtomicLong SHORT_JUMP = new AtomicLong();
    private static final AtomicLong POSE_SLEEPING_RESTORED = new AtomicLong();
    private static final AtomicLong POSE_SLEEPING_CLEARED = new AtomicLong();
    private static final AtomicLong BED_SUFFOCATION = new AtomicLong();
    private static final AtomicLong LEAF_CLEAR_SKIPPED_IN_BUILDING = new AtomicLong();

    private NavigationCounters() {
    }

    public static void incTeleport(NavEvent.Layer layer) {
        TELEPORT_TOTAL.incrementAndGet();
        TELEPORT_PER_LAYER.get((Object)layer).incrementAndGet();
    }

    public static void incShortJump() {
        SHORT_JUMP.incrementAndGet();
    }

    public static void incGoalAbandoned() {
        GOAL_ABANDONED.incrementAndGet();
    }

    public static void incTargetInvalid() {
        TARGET_INVALID.incrementAndGet();
    }

    public static void incPoseSleepingRestored() {
        POSE_SLEEPING_RESTORED.incrementAndGet();
    }

    public static void incPoseSleepingCleared() {
        POSE_SLEEPING_CLEARED.incrementAndGet();
    }

    public static void incBedSuffocation() {
        BED_SUFFOCATION.incrementAndGet();
    }

    public static void incLeafClearSkippedInBuilding() {
        LEAF_CLEAR_SKIPPED_IN_BUILDING.incrementAndGet();
    }

    public static long teleportTotal() {
        return TELEPORT_TOTAL.get();
    }

    public static long teleportFor(NavEvent.Layer layer) {
        return TELEPORT_PER_LAYER.get((Object)layer).get();
    }

    public static long goalAbandoned() {
        return GOAL_ABANDONED.get();
    }

    public static long targetInvalid() {
        return TARGET_INVALID.get();
    }

    public static long shortJump() {
        return SHORT_JUMP.get();
    }

    public static long poseSleepingRestored() {
        return POSE_SLEEPING_RESTORED.get();
    }

    public static long poseSleepingCleared() {
        return POSE_SLEEPING_CLEARED.get();
    }

    public static long bedSuffocation() {
        return BED_SUFFOCATION.get();
    }

    public static long leafClearSkippedInBuilding() {
        return LEAF_CLEAR_SKIPPED_IN_BUILDING.get();
    }

    public static void resetAll() {
        TELEPORT_TOTAL.set(0L);
        GOAL_ABANDONED.set(0L);
        TARGET_INVALID.set(0L);
        SHORT_JUMP.set(0L);
        POSE_SLEEPING_RESTORED.set(0L);
        POSE_SLEEPING_CLEARED.set(0L);
        BED_SUFFOCATION.set(0L);
        LEAF_CLEAR_SKIPPED_IN_BUILDING.set(0L);
        TELEPORT_PER_LAYER.values().forEach(a -> a.set(0L));
    }

    static {
        for (NavEvent.Layer l : NavEvent.Layer.values()) {
            TELEPORT_PER_LAYER.put(l, new AtomicLong());
        }
    }
}

