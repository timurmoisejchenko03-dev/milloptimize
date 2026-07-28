/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.resources.ResourceLocation
 */
package org.millenaire.entity;

import net.minecraft.resources.ResourceLocation;

public enum WallDecorationType {
    NORMAN_TAPESTRY(1, "norman_tapestry", "textures/painting/tapestry.png"),
    INDIAN_STATUE(2, "indian_statue", "textures/painting/sculptures.png"),
    MAYAN_STATUE(3, "mayan_statue", "textures/painting/sculptures.png"),
    BYZANTINE_ICON_SMALL(4, "byzantine_icon_small", "textures/painting/sculptures.png"),
    BYZANTINE_ICON_MEDIUM(5, "byzantine_icon_medium", "textures/painting/sculptures.png"),
    BYZANTINE_ICON_LARGE(6, "byzantine_icon_large", "textures/painting/sculptures.png"),
    HIDE_HANGING(7, "hide_hanging", "textures/painting/sculptures.png"),
    WALL_CARPET_SMALL(8, "wall_carpet_small", "textures/painting/sculptures.png"),
    WALL_CARPET_MEDIUM(9, "wall_carpet_medium", "textures/painting/sculptures.png"),
    WALL_CARPET_LARGE(10, "wall_carpet_large", "textures/painting/sculptures.png");

    private final int legacyId;
    private final String name;
    private final ResourceLocation texture;

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
}

