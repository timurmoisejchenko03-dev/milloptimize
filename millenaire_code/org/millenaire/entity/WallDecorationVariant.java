/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.util.RandomSource
 */
package org.millenaire.entity;

import java.util.ArrayList;
import net.minecraft.util.RandomSource;
import org.millenaire.entity.WallDecorationType;

public enum WallDecorationVariant {
    Griffon("Griffon", 1, 1, 0, 0, WallDecorationType.NORMAN_TAPESTRY),
    Oiseau("Oiseau", 1, 1, 16, 0, WallDecorationType.NORMAN_TAPESTRY),
    CorbeauRenard("CorbeauRenard", 2, 1, 32, 0, WallDecorationType.NORMAN_TAPESTRY),
    Serment("Serment", 5, 3, 0, 16, WallDecorationType.NORMAN_TAPESTRY),
    MortHarold("MortHarold", 4, 3, 80, 16, WallDecorationType.NORMAN_TAPESTRY),
    Drakar("Drakar", 6, 3, 144, 16, WallDecorationType.NORMAN_TAPESTRY),
    MontStMichel("MontStMichel", 3, 2, 0, 64, WallDecorationType.NORMAN_TAPESTRY),
    Bucherons("Bucherons", 3, 2, 48, 64, WallDecorationType.NORMAN_TAPESTRY),
    Cuisine("Cuisine", 3, 2, 96, 64, WallDecorationType.NORMAN_TAPESTRY),
    Flotte("Flotte", 15, 3, 0, 96, WallDecorationType.NORMAN_TAPESTRY),
    Chasse("Chasse", 6, 3, 0, 144, WallDecorationType.NORMAN_TAPESTRY),
    Siege("Siege", 16, 3, 0, 192, WallDecorationType.NORMAN_TAPESTRY),
    Ganesh("Ganesh", 2, 3, 0, 0, WallDecorationType.INDIAN_STATUE),
    Kali("Kali", 2, 3, 32, 0, WallDecorationType.INDIAN_STATUE),
    Shiva("Shiva", 2, 3, 64, 0, WallDecorationType.INDIAN_STATUE),
    Osiyan("Osiyan", 2, 3, 96, 0, WallDecorationType.INDIAN_STATUE),
    Durga("Durga", 2, 3, 128, 0, WallDecorationType.INDIAN_STATUE),
    MayanTeal("MayanTeal", 2, 2, 0, 48, WallDecorationType.MAYAN_STATUE),
    MayanGold("MayanGold", 2, 2, 32, 48, WallDecorationType.MAYAN_STATUE),
    LargeJesus("LargeJesus", 2, 3, 0, 80, WallDecorationType.BYZANTINE_ICON_LARGE),
    LargeVirgin("LargeVirgin", 2, 3, 32, 80, WallDecorationType.BYZANTINE_ICON_LARGE),
    MediumVirgin1("MediumVirgin1", 2, 2, 0, 128, WallDecorationType.BYZANTINE_ICON_MEDIUM),
    MediumVirgin2("MediumVirgin2", 2, 2, 32, 128, WallDecorationType.BYZANTINE_ICON_MEDIUM),
    SmallJesus1("SmallJesus1", 1, 1, 0, 160, WallDecorationType.BYZANTINE_ICON_SMALL),
    SmallJesus2("SmallJesus2", 1, 1, 16, 160, WallDecorationType.BYZANTINE_ICON_SMALL),
    SmallSaint1("SmallSaint1", 1, 1, 32, 160, WallDecorationType.BYZANTINE_ICON_SMALL),
    SmallAngel1("SmallAngel1", 1, 1, 48, 160, WallDecorationType.BYZANTINE_ICON_SMALL),
    SmallVirgin1("SmallVirgin1", 1, 1, 64, 160, WallDecorationType.BYZANTINE_ICON_SMALL),
    SmallAngel2("SmallAngel2", 1, 1, 80, 160, WallDecorationType.BYZANTINE_ICON_SMALL),
    HideSmallCow("HideSmallCow", 1, 1, 0, 176, WallDecorationType.HIDE_HANGING, 10),
    HideSmallRabbit("HideSmallRabbit", 1, 1, 16, 176, WallDecorationType.HIDE_HANGING, 10),
    HideSmallSpider("HideSmallSpider", 1, 1, 32, 176, WallDecorationType.HIDE_HANGING, 1),
    HideLargeCow("HideLargeCow", 2, 2, 0, 192, WallDecorationType.HIDE_HANGING, 10),
    HideLargeBear("HideLargeBear", 2, 2, 32, 192, WallDecorationType.HIDE_HANGING, 5),
    HideLargeZombie("HideLargeZombie", 2, 2, 64, 192, WallDecorationType.HIDE_HANGING, 1),
    HideLargeWolf("HideLargeWolf", 2, 2, 96, 192, WallDecorationType.HIDE_HANGING, 5),
    WallCarpet1("WallCarpet1", 1, 2, 0, 224, WallDecorationType.WALL_CARPET_SMALL),
    WallCarpet2("WallCarpet2", 1, 2, 16, 224, WallDecorationType.WALL_CARPET_SMALL),
    WallCarpet3("WallCarpet3", 1, 2, 32, 224, WallDecorationType.WALL_CARPET_SMALL),
    WallCarpet4("WallCarpet4", 1, 2, 48, 224, WallDecorationType.WALL_CARPET_SMALL),
    WallCarpet5("WallCarpet5", 1, 2, 64, 224, WallDecorationType.WALL_CARPET_SMALL),
    WallCarpet6("WallCarpet6", 1, 2, 80, 224, WallDecorationType.WALL_CARPET_SMALL),
    WallCarpet7("WallCarpet7", 1, 2, 96, 224, WallDecorationType.WALL_CARPET_SMALL),
    WallCarpet8("WallCarpet8", 2, 3, 160, 176, WallDecorationType.WALL_CARPET_MEDIUM),
    WallCarpet9("WallCarpet9", 2, 3, 192, 176, WallDecorationType.WALL_CARPET_MEDIUM),
    WallCarpet10("WallCarpet10", 2, 3, 224, 176, WallDecorationType.WALL_CARPET_MEDIUM),
    WallCarpet11("WallCarpet11", 3, 2, 112, 224, WallDecorationType.WALL_CARPET_LARGE),
    WallCarpet12("WallCarpet12", 3, 2, 160, 224, WallDecorationType.WALL_CARPET_LARGE),
    WallCarpet13("WallCarpet13", 3, 2, 208, 224, WallDecorationType.WALL_CARPET_LARGE);

    private final String title;
    private final int widthBlocks;
    private final int heightBlocks;
    private final int textureOffsetX;
    private final int textureOffsetY;
    private final WallDecorationType type;
    private final int weight;

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
}

