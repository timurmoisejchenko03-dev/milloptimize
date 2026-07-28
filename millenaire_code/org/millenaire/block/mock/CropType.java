/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.util.StringRepresentable
 */
package org.millenaire.block.mock;

import net.minecraft.util.StringRepresentable;

public enum CropType implements StringRepresentable
{
    WHEAT("wheat"),
    RICE("rice"),
    TURMERIC("turmeric"),
    SUGAR_CANE("sugarcane"),
    POTATO("potato"),
    NETHER_WART("netherwart"),
    VINE("vine"),
    MAIZE("maize"),
    CACAO("cacao"),
    CARROT("carrot"),
    FLOWER("flower"),
    COTTON("cotton");

    private final String serializedName;

    private CropType(String serializedName) {
        this.serializedName = serializedName;
    }

    public String getSerializedName() {
        return this.serializedName;
    }
}

