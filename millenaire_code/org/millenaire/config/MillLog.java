/*
 * Decompiled with CFR 0.152.
 */
package org.millenaire.config;

import java.util.function.IntSupplier;

public final class MillLog {
    private MillLog() {
    }

    public static boolean isEnabled(LogCategory category, int level) {
        return category.getLevel() >= level;
    }

    public static enum LogCategory {
        AI,
        PATHFINDING,
        CONSTRUCTION,
        GATHERING,
        COMMERCE,
        VILLAGE,
        WORLD_GEN,
        CULTURE,
        NETWORK,
        CHUNK_LOADING,
        DIPLOMACY,
        OTHER;

        private IntSupplier levelSupplier = () -> 0;

        public void bind(IntSupplier supplier) {
            this.levelSupplier = supplier;
        }

        public int getLevel() {
            return this.levelSupplier.getAsInt();
        }
    }
}

