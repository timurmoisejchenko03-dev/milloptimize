/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  net.minecraft.util.RandomSource
 */
package org.millenaire.entity;

import java.util.ArrayList;
import net.minecraft.util.RandomSource;
import org.millenaire.entity.WallDecorationType;

public final class WallDecorationVariant
extends Enum<WallDecorationVariant> {
    public static final /* enum */ WallDecorationVariant Griffon = new WallDecorationVariant("Griffon", 1, 1, 0, 0, WallDecorationType.NORMAN_TAPESTRY);
    public static final /* enum */ WallDecorationVariant Oiseau = new WallDecorationVariant("Oiseau", 1, 1, 16, 0, WallDecorationType.NORMAN_TAPESTRY);
    public static final /* enum */ WallDecorationVariant CorbeauRenard = new WallDecorationVariant("CorbeauRenard", 2, 1, 32, 0, WallDecorationType.NORMAN_TAPESTRY);
    public static final /* enum */ WallDecorationVariant Serment = new WallDecorationVariant("Serment", 5, 3, 0, 16, WallDecorationType.NORMAN_TAPESTRY);
    public static final /* enum */ WallDecorationVariant MortHarold = new WallDecorationVariant("MortHarold", 4, 3, 80, 16, WallDecorationType.NORMAN_TAPESTRY);
    public static final /* enum */ WallDecorationVariant Drakar = new WallDecorationVariant("Drakar", 6, 3, 144, 16, WallDecorationType.NORMAN_TAPESTRY);
    public static final /* enum */ WallDecorationVariant MontStMichel = new WallDecorationVariant("MontStMichel", 3, 2, 0, 64, WallDecorationType.NORMAN_TAPESTRY);
    public static final /* enum */ WallDecorationVariant Bucherons = new WallDecorationVariant("Bucherons", 3, 2, 48, 64, WallDecorationType.NORMAN_TAPESTRY);
    public static final /* enum */ WallDecorationVariant Cuisine = new WallDecorationVariant("Cuisine", 3, 2, 96, 64, WallDecorationType.NORMAN_TAPESTRY);
    public static final /* enum */ WallDecorationVariant Flotte = new WallDecorationVariant("Flotte", 15, 3, 0, 96, WallDecorationType.NORMAN_TAPESTRY);
    public static final /* enum */ WallDecorationVariant Chasse = new WallDecorationVariant("Chasse", 6, 3, 0, 144, WallDecorationType.NORMAN_TAPESTRY);
    public static final /* enum */ WallDecorationVariant Siege = new WallDecorationVariant("Siege", 16, 3, 0, 192, WallDecorationType.NORMAN_TAPESTRY);
    public static final /* enum */ WallDecorationVariant Ganesh = new WallDecorationVariant("Ganesh", 2, 3, 0, 0, WallDecorationType.INDIAN_STATUE);
    public static final /* enum */ WallDecorationVariant Kali = new WallDecorationVariant("Kali", 2, 3, 32, 0, WallDecorationType.INDIAN_STATUE);
    public static final /* enum */ WallDecorationVariant Shiva = new WallDecorationVariant("Shiva", 2, 3, 64, 0, WallDecorationType.INDIAN_STATUE);
    public static final /* enum */ WallDecorationVariant Osiyan = new WallDecorationVariant("Osiyan", 2, 3, 96, 0, WallDecorationType.INDIAN_STATUE);
    public static final /* enum */ WallDecorationVariant Durga = new WallDecorationVariant("Durga", 2, 3, 128, 0, WallDecorationType.INDIAN_STATUE);
    public static final /* enum */ WallDecorationVariant MayanTeal = new WallDecorationVariant("MayanTeal", 2, 2, 0, 48, WallDecorationType.MAYAN_STATUE);
    public static final /* enum */ WallDecorationVariant MayanGold = new WallDecorationVariant("MayanGold", 2, 2, 32, 48, WallDecorationType.MAYAN_STATUE);
    public static final /* enum */ WallDecorationVariant LargeJesus = new WallDecorationVariant("LargeJesus", 2, 3, 0, 80, WallDecorationType.BYZANTINE_ICON_LARGE);
    public static final /* enum */ WallDecorationVariant LargeVirgin = new WallDecorationVariant("LargeVirgin", 2, 3, 32, 80, WallDecorationType.BYZANTINE_ICON_LARGE);
    public static final /* enum */ WallDecorationVariant MediumVirgin1 = new WallDecorationVariant("MediumVirgin1", 2, 2, 0, 128, WallDecorationType.BYZANTINE_ICON_MEDIUM);
    public static final /* enum */ WallDecorationVariant MediumVirgin2 = new WallDecorationVariant("MediumVirgin2", 2, 2, 32, 128, WallDecorationType.BYZANTINE_ICON_MEDIUM);
    public static final /* enum */ WallDecorationVariant SmallJesus1 = new WallDecorationVariant("SmallJesus1", 1, 1, 0, 160, WallDecorationType.BYZANTINE_ICON_SMALL);
    public static final /* enum */ WallDecorationVariant SmallJesus2 = new WallDecorationVariant("SmallJesus2", 1, 1, 16, 160, WallDecorationType.BYZANTINE_ICON_SMALL);
    public static final /* enum */ WallDecorationVariant SmallSaint1 = new WallDecorationVariant("SmallSaint1", 1, 1, 32, 160, WallDecorationType.BYZANTINE_ICON_SMALL);
    public static final /* enum */ WallDecorationVariant SmallAngel1 = new WallDecorationVariant("SmallAngel1", 1, 1, 48, 160, WallDecorationType.BYZANTINE_ICON_SMALL);
    public static final /* enum */ WallDecorationVariant SmallVirgin1 = new WallDecorationVariant("SmallVirgin1", 1, 1, 64, 160, WallDecorationType.BYZANTINE_ICON_SMALL);
    public static final /* enum */ WallDecorationVariant SmallAngel2 = new WallDecorationVariant("SmallAngel2", 1, 1, 80, 160, WallDecorationType.BYZANTINE_ICON_SMALL);
    public static final /* enum */ WallDecorationVariant HideSmallCow = new WallDecorationVariant("HideSmallCow", 1, 1, 0, 176, WallDecorationType.HIDE_HANGING, 10);
    public static final /* enum */ WallDecorationVariant HideSmallRabbit = new WallDecorationVariant("HideSmallRabbit", 1, 1, 16, 176, WallDecorationType.HIDE_HANGING, 10);
    public static final /* enum */ WallDecorationVariant HideSmallSpider = new WallDecorationVariant("HideSmallSpider", 1, 1, 32, 176, WallDecorationType.HIDE_HANGING, 1);
    public static final /* enum */ WallDecorationVariant HideLargeCow = new WallDecorationVariant("HideLargeCow", 2, 2, 0, 192, WallDecorationType.HIDE_HANGING, 10);
    public static final /* enum */ WallDecorationVariant HideLargeBear = new WallDecorationVariant("HideLargeBear", 2, 2, 32, 192, WallDecorationType.HIDE_HANGING, 5);
    public static final /* enum */ WallDecorationVariant HideLargeZombie = new WallDecorationVariant("HideLargeZombie", 2, 2, 64, 192, WallDecorationType.HIDE_HANGING, 1);
    public static final /* enum */ WallDecorationVariant HideLargeWolf = new WallDecorationVariant("HideLargeWolf", 2, 2, 96, 192, WallDecorationType.HIDE_HANGING, 5);
    public static final /* enum */ WallDecorationVariant WallCarpet1 = new WallDecorationVariant("WallCarpet1", 1, 2, 0, 224, WallDecorationType.WALL_CARPET_SMALL);
    public static final /* enum */ WallDecorationVariant WallCarpet2 = new WallDecorationVariant("WallCarpet2", 1, 2, 16, 224, WallDecorationType.WALL_CARPET_SMALL);
    public static final /* enum */ WallDecorationVariant WallCarpet3 = new WallDecorationVariant("WallCarpet3", 1, 2, 32, 224, WallDecorationType.WALL_CARPET_SMALL);
    public static final /* enum */ WallDecorationVariant WallCarpet4 = new WallDecorationVariant("WallCarpet4", 1, 2, 48, 224, WallDecorationType.WALL_CARPET_SMALL);
    public static final /* enum */ WallDecorationVariant WallCarpet5 = new WallDecorationVariant("WallCarpet5", 1, 2, 64, 224, WallDecorationType.WALL_CARPET_SMALL);
    public static final /* enum */ WallDecorationVariant WallCarpet6 = new WallDecorationVariant("WallCarpet6", 1, 2, 80, 224, WallDecorationType.WALL_CARPET_SMALL);
    public static final /* enum */ WallDecorationVariant WallCarpet7 = new WallDecorationVariant("WallCarpet7", 1, 2, 96, 224, WallDecorationType.WALL_CARPET_SMALL);
    public static final /* enum */ WallDecorationVariant WallCarpet8 = new WallDecorationVariant("WallCarpet8", 2, 3, 160, 176, WallDecorationType.WALL_CARPET_MEDIUM);
    public static final /* enum */ WallDecorationVariant WallCarpet9 = new WallDecorationVariant("WallCarpet9", 2, 3, 192, 176, WallDecorationType.WALL_CARPET_MEDIUM);
    public static final /* enum */ WallDecorationVariant WallCarpet10 = new WallDecorationVariant("WallCarpet10", 2, 3, 224, 176, WallDecorationType.WALL_CARPET_MEDIUM);
    public static final /* enum */ WallDecorationVariant WallCarpet11 = new WallDecorationVariant("WallCarpet11", 3, 2, 112, 224, WallDecorationType.WALL_CARPET_LARGE);
    public static final /* enum */ WallDecorationVariant WallCarpet12 = new WallDecorationVariant("WallCarpet12", 3, 2, 160, 224, WallDecorationType.WALL_CARPET_LARGE);
    public static final /* enum */ WallDecorationVariant WallCarpet13 = new WallDecorationVariant("WallCarpet13", 3, 2, 208, 224, WallDecorationType.WALL_CARPET_LARGE);
    private final String title;
    private final int widthBlocks;
    private final int heightBlocks;
    private final int textureOffsetX;
    private final int textureOffsetY;
    private final WallDecorationType type;
    private final int weight;
    private static final /* synthetic */ WallDecorationVariant[] $VALUES;

    public static WallDecorationVariant[] values() {
        return (WallDecorationVariant[])$VALUES.clone();
    }

    public static WallDecorationVariant valueOf(String name) {
        return Enum.valueOf(WallDecorationVariant.class, name);
    }

    private WallDecorationVariant(String title, int widthBlocks, int heightBlocks, int textureOffsetX, int textureOffsetY, WallDecorationType type) {
        this(title, widthBlocks, heightBlocks, textureOffsetX, textureOffsetY, type, 1);
    }

    private WallDecorationVariant(String title, int widthBlocks, int heightBlocks, int textureOffsetX, int textureOffsetY, WallDecorationType type, int weight) {
        this.title = title;
        this.widthBlocks = widthBlocks;
        this.heightBlocks = heightBlocks;
        this.textureOffsetX = textureOffsetX;
        this.textureOffsetY = textureOffsetY;
        this.type = type;
        this.weight = weight;
    }

    public String title() {
        return this.title;
    }

    public int widthBlocks() {
        return this.widthBlocks;
    }

    public int heightBlocks() {
        return this.heightBlocks;
    }

    public int widthPixels() {
        return this.widthBlocks * 16;
    }

    public int heightPixels() {
        return this.heightBlocks * 16;
    }

    public int textureOffsetX() {
        return this.textureOffsetX;
    }

    public int textureOffsetY() {
        return this.textureOffsetY;
    }

    public WallDecorationType type() {
        return this.type;
    }

    public int weight() {
        return this.weight;
    }

    public static WallDecorationVariant fromTitle(String title) {
        for (WallDecorationVariant v : WallDecorationVariant.values()) {
            if (!v.title.equals(title)) continue;
            return v;
        }
        return null;
    }

    public static WallDecorationVariant selectRandom(WallDecorationType type, int maxWidthBlocks, int maxHeightBlocks, boolean largestPossible, RandomSource random) {
        ArrayList<WallDecorationVariant> candidates = new ArrayList<WallDecorationVariant>();
        int maxArea = 0;
        for (WallDecorationVariant v : WallDecorationVariant.values()) {
            if (v.type != type || v.widthBlocks > maxWidthBlocks || v.heightBlocks > maxHeightBlocks) continue;
            int area = v.widthBlocks * v.heightBlocks;
            if (!largestPossible && area > maxArea) {
                candidates.clear();
            }
            if (largestPossible || area >= maxArea) {
                candidates.add(v);
            }
            maxArea = Math.max(maxArea, area);
        }
        if (candidates.isEmpty()) {
            return null;
        }
        int totalWeight = 0;
        for (WallDecorationVariant v : candidates) {
            totalWeight += v.weight;
        }
        int roll = random.nextInt(totalWeight);
        int cumulative = 0;
        for (WallDecorationVariant v : candidates) {
            if (roll >= (cumulative += v.weight)) continue;
            return v;
        }
        return (WallDecorationVariant)((Object)candidates.getLast());
    }

    private static /* synthetic */ WallDecorationVariant[] $values() {
        return new WallDecorationVariant[]{Griffon, Oiseau, CorbeauRenard, Serment, MortHarold, Drakar, MontStMichel, Bucherons, Cuisine, Flotte, Chasse, Siege, Ganesh, Kali, Shiva, Osiyan, Durga, MayanTeal, MayanGold, LargeJesus, LargeVirgin, MediumVirgin1, MediumVirgin2, SmallJesus1, SmallJesus2, SmallSaint1, SmallAngel1, SmallVirgin1, SmallAngel2, HideSmallCow, HideSmallRabbit, HideSmallSpider, HideLargeCow, HideLargeBear, HideLargeZombie, HideLargeWolf, WallCarpet1, WallCarpet2, WallCarpet3, WallCarpet4, WallCarpet5, WallCarpet6, WallCarpet7, WallCarpet8, WallCarpet9, WallCarpet10, WallCarpet11, WallCarpet12, WallCarpet13};
    }

    static {
        $VALUES = WallDecorationVariant.$values();
    }
}

