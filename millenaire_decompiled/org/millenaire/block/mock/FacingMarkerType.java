/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  net.minecraft.util.StringRepresentable
 */
package org.millenaire.block.mock;

import net.minecraft.util.StringRepresentable;

public final class FacingMarkerType
extends Enum<FacingMarkerType>
implements StringRepresentable {
    public static final /* enum */ FacingMarkerType FURNACE = new FacingMarkerType("furnace", "furnace");
    public static final /* enum */ FacingMarkerType SIGN_POS = new FacingMarkerType("sign_pos", "signPos");
    private final String serializedName;
    private final String specialPointType;
    private static final /* synthetic */ FacingMarkerType[] $VALUES;

    public static FacingMarkerType[] values() {
        return (FacingMarkerType[])$VALUES.clone();
    }

    public static FacingMarkerType valueOf(String name) {
        return Enum.valueOf(FacingMarkerType.class, name);
    }

    private FacingMarkerType(String serializedName, String specialPointType) {
        this.serializedName = serializedName;
        this.specialPointType = specialPointType;
    }

    public String getSerializedName() {
        return this.serializedName;
    }

    public String specialPointType() {
        return this.specialPointType;
    }

    public static FacingMarkerType byName(String name) {
        for (FacingMarkerType type : FacingMarkerType.values()) {
            if (!type.serializedName.equals(name)) continue;
            return type;
        }
        return FURNACE;
    }

    private static /* synthetic */ FacingMarkerType[] $values() {
        return new FacingMarkerType[]{FURNACE, SIGN_POS};
    }

    static {
        $VALUES = FacingMarkerType.$values();
    }
}

