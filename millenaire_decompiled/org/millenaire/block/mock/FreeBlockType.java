/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  net.minecraft.util.StringRepresentable
 */
package org.millenaire.block.mock;

import net.minecraft.util.StringRepresentable;

public final class FreeBlockType
extends Enum<FreeBlockType>
implements StringRepresentable {
    public static final /* enum */ FreeBlockType STONE = new FreeBlockType("stone");
    public static final /* enum */ FreeBlockType SAND = new FreeBlockType("sand");
    public static final /* enum */ FreeBlockType GRAVEL = new FreeBlockType("gravel");
    public static final /* enum */ FreeBlockType SANDSTONE = new FreeBlockType("sandstone");
    public static final /* enum */ FreeBlockType WOOL = new FreeBlockType("wool");
    public static final /* enum */ FreeBlockType COBBLESTONE = new FreeBlockType("cobblestone");
    public static final /* enum */ FreeBlockType STONE_BRICK = new FreeBlockType("stone_brick");
    public static final /* enum */ FreeBlockType PAINTED_BRICK = new FreeBlockType("painted_brick");
    public static final /* enum */ FreeBlockType GRASS_BLOCK = new FreeBlockType("grass_block");
    private final String serializedName;
    private static final /* synthetic */ FreeBlockType[] $VALUES;

    public static FreeBlockType[] values() {
        return (FreeBlockType[])$VALUES.clone();
    }

    public static FreeBlockType valueOf(String name) {
        return Enum.valueOf(FreeBlockType.class, name);
    }

    private FreeBlockType(String serializedName) {
        this.serializedName = serializedName;
    }

    public String getSerializedName() {
        return this.serializedName;
    }

    private static /* synthetic */ FreeBlockType[] $values() {
        return new FreeBlockType[]{STONE, SAND, GRAVEL, SANDSTONE, WOOL, COBBLESTONE, STONE_BRICK, PAINTED_BRICK, GRASS_BLOCK};
    }

    static {
        $VALUES = FreeBlockType.$values();
    }
}

