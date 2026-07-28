/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  net.minecraft.util.StringRepresentable
 */
package org.millenaire.block.mock;

import net.minecraft.util.StringRepresentable;

public final class TreeSpawnType
extends Enum<TreeSpawnType>
implements StringRepresentable {
    public static final /* enum */ TreeSpawnType OAK = new TreeSpawnType("oak");
    public static final /* enum */ TreeSpawnType PINE = new TreeSpawnType("pine");
    public static final /* enum */ TreeSpawnType BIRCH = new TreeSpawnType("birch");
    public static final /* enum */ TreeSpawnType JUNGLE = new TreeSpawnType("jungle");
    public static final /* enum */ TreeSpawnType ACACIA = new TreeSpawnType("acacia");
    public static final /* enum */ TreeSpawnType DARK_OAK = new TreeSpawnType("dark_oak");
    public static final /* enum */ TreeSpawnType APPLE = new TreeSpawnType("apple");
    public static final /* enum */ TreeSpawnType OLIVE = new TreeSpawnType("olive");
    public static final /* enum */ TreeSpawnType PISTACHIO = new TreeSpawnType("pistachio");
    private final String serializedName;
    private static final /* synthetic */ TreeSpawnType[] $VALUES;

    public static TreeSpawnType[] values() {
        return (TreeSpawnType[])$VALUES.clone();
    }

    public static TreeSpawnType valueOf(String name) {
        return Enum.valueOf(TreeSpawnType.class, name);
    }

    private TreeSpawnType(String serializedName) {
        this.serializedName = serializedName;
    }

    public String getSerializedName() {
        return this.serializedName;
    }

    private static /* synthetic */ TreeSpawnType[] $values() {
        return new TreeSpawnType[]{OAK, PINE, BIRCH, JUNGLE, ACACIA, DARK_OAK, APPLE, OLIVE, PISTACHIO};
    }

    static {
        $VALUES = TreeSpawnType.$values();
    }
}

