/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  net.minecraft.resources.ResourceLocation
 */
package org.millenaire.entity;

import net.minecraft.resources.ResourceLocation;

public final class WallDecorationType
extends Enum<WallDecorationType> {
    public static final /* enum */ WallDecorationType NORMAN_TAPESTRY = new WallDecorationType(1, "norman_tapestry", "textures/painting/tapestry.png");
    public static final /* enum */ WallDecorationType INDIAN_STATUE = new WallDecorationType(2, "indian_statue", "textures/painting/sculptures.png");
    public static final /* enum */ WallDecorationType MAYAN_STATUE = new WallDecorationType(3, "mayan_statue", "textures/painting/sculptures.png");
    public static final /* enum */ WallDecorationType BYZANTINE_ICON_SMALL = new WallDecorationType(4, "byzantine_icon_small", "textures/painting/sculptures.png");
    public static final /* enum */ WallDecorationType BYZANTINE_ICON_MEDIUM = new WallDecorationType(5, "byzantine_icon_medium", "textures/painting/sculptures.png");
    public static final /* enum */ WallDecorationType BYZANTINE_ICON_LARGE = new WallDecorationType(6, "byzantine_icon_large", "textures/painting/sculptures.png");
    public static final /* enum */ WallDecorationType HIDE_HANGING = new WallDecorationType(7, "hide_hanging", "textures/painting/sculptures.png");
    public static final /* enum */ WallDecorationType WALL_CARPET_SMALL = new WallDecorationType(8, "wall_carpet_small", "textures/painting/sculptures.png");
    public static final /* enum */ WallDecorationType WALL_CARPET_MEDIUM = new WallDecorationType(9, "wall_carpet_medium", "textures/painting/sculptures.png");
    public static final /* enum */ WallDecorationType WALL_CARPET_LARGE = new WallDecorationType(10, "wall_carpet_large", "textures/painting/sculptures.png");
    private final int legacyId;
    private final String name;
    private final ResourceLocation texture;
    private static final /* synthetic */ WallDecorationType[] $VALUES;

    public static WallDecorationType[] values() {
        return (WallDecorationType[])$VALUES.clone();
    }

    public static WallDecorationType valueOf(String name) {
        return Enum.valueOf(WallDecorationType.class, name);
    }

    private WallDecorationType(int legacyId, String name, String texturePath) {
        this.legacyId = legacyId;
        this.name = name;
        this.texture = ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)texturePath);
    }

    public int legacyId() {
        return this.legacyId;
    }

    public String typeName() {
        return this.name;
    }

    public ResourceLocation texture() {
        return this.texture;
    }

    public static WallDecorationType fromLegacyId(int id) {
        for (WallDecorationType t : WallDecorationType.values()) {
            if (t.legacyId != id) continue;
            return t;
        }
        return null;
    }

    public static WallDecorationType fromName(String name) {
        for (WallDecorationType t : WallDecorationType.values()) {
            if (!t.name.equals(name)) continue;
            return t;
        }
        return null;
    }

    private static /* synthetic */ WallDecorationType[] $values() {
        return new WallDecorationType[]{NORMAN_TAPESTRY, INDIAN_STATUE, MAYAN_STATUE, BYZANTINE_ICON_SMALL, BYZANTINE_ICON_MEDIUM, BYZANTINE_ICON_LARGE, HIDE_HANGING, WALL_CARPET_SMALL, WALL_CARPET_MEDIUM, WALL_CARPET_LARGE};
    }

    static {
        $VALUES = WallDecorationType.$values();
    }
}

