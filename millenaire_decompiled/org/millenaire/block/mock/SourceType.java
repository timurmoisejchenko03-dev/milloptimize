/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  net.minecraft.util.StringRepresentable
 */
package org.millenaire.block.mock;

import net.minecraft.util.StringRepresentable;

public final class SourceType
extends Enum<SourceType>
implements StringRepresentable {
    public static final /* enum */ SourceType STONE = new SourceType("stone");
    public static final /* enum */ SourceType SAND = new SourceType("sand");
    public static final /* enum */ SourceType SANDSTONE = new SourceType("sandstone");
    public static final /* enum */ SourceType CLAY = new SourceType("clay");
    public static final /* enum */ SourceType GRAVEL = new SourceType("gravel");
    public static final /* enum */ SourceType GRANITE = new SourceType("granite");
    public static final /* enum */ SourceType DIORITE = new SourceType("diorite");
    public static final /* enum */ SourceType ANDESITE = new SourceType("andesite");
    public static final /* enum */ SourceType SNOW = new SourceType("snow");
    public static final /* enum */ SourceType ICE = new SourceType("ice");
    public static final /* enum */ SourceType RED_SANDSTONE = new SourceType("red_sandstone");
    public static final /* enum */ SourceType QUARTZ = new SourceType("quartz");
    private final String serializedName;
    private static final /* synthetic */ SourceType[] $VALUES;

    public static SourceType[] values() {
        return (SourceType[])$VALUES.clone();
    }

    public static SourceType valueOf(String name) {
        return Enum.valueOf(SourceType.class, name);
    }

    private SourceType(String serializedName) {
        this.serializedName = serializedName;
    }

    public String getSerializedName() {
        return this.serializedName;
    }

    private static /* synthetic */ SourceType[] $values() {
        return new SourceType[]{STONE, SAND, SANDSTONE, CLAY, GRAVEL, GRANITE, DIORITE, ANDESITE, SNOW, ICE, RED_SANDSTONE, QUARTZ};
    }

    static {
        $VALUES = SourceType.$values();
    }
}

