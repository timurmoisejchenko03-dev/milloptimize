/*
 * Decompiled with CFR 0.150.
 */
package org.millenaire.diagnostics;

public record NavEvent(long tick, Layer layer, Type type, String detail) {
    @Override
    public String toString() {
        return "[t=" + this.tick + "] " + String.valueOf((Object)this.layer) + "/" + String.valueOf((Object)this.type) + (String)(this.detail.isEmpty() ? "" : " " + this.detail);
    }

    public static final class Layer
    extends Enum<Layer> {
        public static final /* enum */ Layer VNM = new Layer();
        public static final /* enum */ Layer WAYPOINT = new Layer();
        public static final /* enum */ Layer GATHERING = new Layer();
        public static final /* enum */ Layer REST = new Layer();
        public static final /* enum */ Layer SCHEDULER = new Layer();
        public static final /* enum */ Layer RELOAD = new Layer();
        private static final /* synthetic */ Layer[] $VALUES;

        public static Layer[] values() {
            return (Layer[])$VALUES.clone();
        }

        public static Layer valueOf(String name) {
            return Enum.valueOf(Layer.class, name);
        }

        private static /* synthetic */ Layer[] $values() {
            return new Layer[]{VNM, WAYPOINT, GATHERING, REST, SCHEDULER, RELOAD};
        }

        static {
            $VALUES = Layer.$values();
        }
    }

    public static final class Type
    extends Enum<Type> {
        public static final /* enum */ Type NAV_START = new Type();
        public static final /* enum */ Type NAV_STOP = new Type();
        public static final /* enum */ Type REPATH = new Type();
        public static final /* enum */ Type SHORT_JUMP = new Type();
        public static final /* enum */ Type TELEPORT = new Type();
        public static final /* enum */ Type STUCK_DETECTED = new Type();
        public static final /* enum */ Type TARGET_INVALID = new Type();
        public static final /* enum */ Type ACTING_WATCHDOG_FIRED = new Type();
        public static final /* enum */ Type GOAL_ABANDONED = new Type();
        public static final /* enum */ Type POSE_SLEEPING_RESTORED = new Type();
        public static final /* enum */ Type POSE_SLEEPING_CLEARED = new Type();
        public static final /* enum */ Type BED_SUFFOCATION = new Type();
        public static final /* enum */ Type LEAF_CLEAR_SKIPPED = new Type();
        private static final /* synthetic */ Type[] $VALUES;

        public static Type[] values() {
            return (Type[])$VALUES.clone();
        }

        public static Type valueOf(String name) {
            return Enum.valueOf(Type.class, name);
        }

        private static /* synthetic */ Type[] $values() {
            return new Type[]{NAV_START, NAV_STOP, REPATH, SHORT_JUMP, TELEPORT, STUCK_DETECTED, TARGET_INVALID, ACTING_WATCHDOG_FIRED, GOAL_ABANDONED, POSE_SLEEPING_RESTORED, POSE_SLEEPING_CLEARED, BED_SUFFOCATION, LEAF_CLEAR_SKIPPED};
        }

        static {
            $VALUES = Type.$values();
        }
    }
}

