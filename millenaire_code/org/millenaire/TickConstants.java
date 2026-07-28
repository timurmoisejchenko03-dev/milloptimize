/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.world.level.Level
 */
package org.millenaire;

import net.minecraft.world.level.Level;

public final class TickConstants {
    public static final long DAY_LENGTH = 24000L;

    private TickConstants() {
    }

    public static boolean isNight(Level level) {
        return !level.isDay();
    }
}

