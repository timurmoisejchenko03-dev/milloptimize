/*
 * Decompiled with CFR 0.150.
 */
package org.millenaire.hire;

public final class HiringHelper {
    private HiringHelper() {
    }

    public static boolean isHireable(int hiringCost) {
        return hiringCost > 0;
    }

    public static int hireCost(int baseCost, boolean controlled) {
        return controlled ? baseCost / 2 : baseCost;
    }

    public static boolean isExpired(long hiredUntil, long gameTime) {
        return gameTime > hiredUntil;
    }

    public static int hoursLeft(long hiredUntil, long gameTime) {
        return (int)Math.max(0L, (hiredUntil - gameTime) / 1000L);
    }
}

