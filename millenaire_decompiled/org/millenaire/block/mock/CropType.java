/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  net.minecraft.util.StringRepresentable
 */
package org.millenaire.block.mock;

import net.minecraft.util.StringRepresentable;

public final class CropType
extends Enum<CropType>
implements StringRepresentable {
    public static final /* enum */ CropType WHEAT = new CropType("wheat");
    public static final /* enum */ CropType RICE = new CropType("rice");
    public static final /* enum */ CropType TURMERIC = new CropType("turmeric");
    public static final /* enum */ CropType SUGAR_CANE = new CropType("sugarcane");
    public static final /* enum */ CropType POTATO = new CropType("potato");
    public static final /* enum */ CropType NETHER_WART = new CropType("netherwart");
    public static final /* enum */ CropType VINE = new CropType("vine");
    public static final /* enum */ CropType MAIZE = new CropType("maize");
    public static final /* enum */ CropType CACAO = new CropType("cacao");
    public static final /* enum */ CropType CARROT = new CropType("carrot");
    public static final /* enum */ CropType FLOWER = new CropType("flower");
    public static final /* enum */ CropType COTTON = new CropType("cotton");
    private final String serializedName;
    private static final /* synthetic */ CropType[] $VALUES;

    public static CropType[] values() {
        return (CropType[])$VALUES.clone();
    }

    public static CropType valueOf(String name) {
        return Enum.valueOf(CropType.class, name);
    }

    private CropType(String serializedName) {
        this.serializedName = serializedName;
    }

    public String getSerializedName() {
        return this.serializedName;
    }

    private static /* synthetic */ CropType[] $values() {
        return new CropType[]{WHEAT, RICE, TURMERIC, SUGAR_CANE, POTATO, NETHER_WART, VINE, MAIZE, CACAO, CARROT, FLOWER, COTTON};
    }

    static {
        $VALUES = CropType.$values();
    }
}

