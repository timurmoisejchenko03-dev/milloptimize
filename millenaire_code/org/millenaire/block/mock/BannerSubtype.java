/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.util.StringRepresentable
 */
package org.millenaire.block.mock;

import net.minecraft.util.StringRepresentable;

public enum BannerSubtype implements StringRepresentable
{
    VILLAGE("village", "village"),
    CULTURE("culture", "culture");

    private final String serializedName;
    private final String specialPointSubtype;

    private BannerSubtype(String serializedName, String specialPointSubtype) {
        this.serializedName = serializedName;
        this.specialPointSubtype = specialPointSubtype;
    }

    public String getSerializedName() {
        return this.serializedName;
    }

    public String specialPointSubtype() {
        return this.specialPointSubtype;
    }
}

