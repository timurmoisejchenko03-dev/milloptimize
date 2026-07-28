/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.util.StringRepresentable
 */
package org.millenaire.block.mock;

import net.minecraft.util.StringRepresentable;

public enum SourceType implements StringRepresentable
{
    STONE("stone"),
    SAND("sand"),
    SANDSTONE("sandstone"),
    CLAY("clay"),
    GRAVEL("gravel"),
    GRANITE("granite"),
    DIORITE("diorite"),
    ANDESITE("andesite"),
    SNOW("snow"),
    ICE("ice"),
    RED_SANDSTONE("red_sandstone"),
    QUARTZ("quartz");

    private final String serializedName;

    private SourceType(String serializedName) {
        this.serializedName = serializedName;
    }

    public String getSerializedName() {
        return this.serializedName;
    }
}

