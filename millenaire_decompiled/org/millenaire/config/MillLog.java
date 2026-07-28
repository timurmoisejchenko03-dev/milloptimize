/*
 * Decompiled with CFR 0.150.
 */
package org.millenaire.config;

import java.util.function.IntSupplier;

public final class MillLog {
    private MillLog() {
    }

    public static boolean isEnabled(LogCategory category, int level) {
        return category.getLevel() >= level;
    }

    public static final class LogCategory
    extends Enum<LogCategory> {
        public static final /* enum */ LogCategory AI = new LogCategory();
        public static final /* enum */ LogCategory PATHFINDING = new LogCategory();
        public static final /* enum */ LogCategory CONSTRUCTION = new LogCategory();
        public static final /* enum */ LogCategory GATHERING = new LogCategory();
        public static final /* enum */ LogCategory COMMERCE = new LogCategory();
        public static final /* enum */ LogCategory VILLAGE = new LogCategory();
        public static final /* enum */ LogCategory WORLD_GEN = new LogCategory();
        public static final /* enum */ LogCategory CULTURE = new LogCategory();
        public static final /* enum */ LogCategory NETWORK = new LogCategory();
        public static final /* enum */ LogCategory CHUNK_LOADING = new LogCategory();
        public static final /* enum */ LogCategory DIPLOMACY = new LogCategory();
        public static final /* enum */ LogCategory OTHER = new LogCategory();
        private IntSupplier levelSupplier = () -> 0;
        private static final /* synthetic */ LogCategory[] $VALUES;

        public static LogCategory[] values() {
            return (LogCategory[])$VALUES.clone();
        }

        public static LogCategory valueOf(String name) {
            return Enum.valueOf(LogCategory.class, name);
        }

        public void bind(IntSupplier supplier) {
            this.levelSupplier = supplier;
        }

        public int getLevel() {
            return this.levelSupplier.getAsInt();
        }

        private static /* synthetic */ LogCategory[] $values() {
            return new LogCategory[]{AI, PATHFINDING, CONSTRUCTION, GATHERING, COMMERCE, VILLAGE, WORLD_GEN, CULTURE, NETWORK, CHUNK_LOADING, DIPLOMACY, OTHER};
        }

        static {
            $VALUES = LogCategory.$values();
        }
    }
}

