/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  net.minecraft.ChatFormatting
 */
package org.millenaire.village;

import net.minecraft.ChatFormatting;

public final class VillageRelations {
    public static final int MAX = 100;
    public static final int EXCELLENT = 90;
    public static final int VERY_GOOD = 70;
    public static final int GOOD = 50;
    public static final int DECENT = 30;
    public static final int FAIR = 10;
    public static final int NEUTRAL = 0;
    public static final int CHILLY = -10;
    public static final int BAD = -30;
    public static final int VERY_BAD = -50;
    public static final int ATROCIOUS = -70;
    public static final int OPEN_CONFLICT = -90;
    public static final int MIN = -100;

    private VillageRelations() {
    }

    public static String getRelationKey(int relation) {
        if (relation >= 90) {
            return "relation.millenaire.excellent";
        }
        if (relation >= 70) {
            return "relation.millenaire.verygood";
        }
        if (relation >= 50) {
            return "relation.millenaire.good";
        }
        if (relation >= 30) {
            return "relation.millenaire.decent";
        }
        if (relation >= 10) {
            return "relation.millenaire.fair";
        }
        if (relation <= -90) {
            return "relation.millenaire.openconflict";
        }
        if (relation <= -70) {
            return "relation.millenaire.atrocious";
        }
        if (relation <= -50) {
            return "relation.millenaire.verybad";
        }
        if (relation <= -30) {
            return "relation.millenaire.bad";
        }
        if (relation <= -10) {
            return "relation.millenaire.chilly";
        }
        return "relation.millenaire.neutral";
    }

    public static ChatFormatting getRelationColor(int relation) {
        if (relation >= 50) {
            return ChatFormatting.GREEN;
        }
        if (relation >= 0) {
            return ChatFormatting.YELLOW;
        }
        if (relation > -50) {
            return ChatFormatting.GOLD;
        }
        return ChatFormatting.RED;
    }
}

