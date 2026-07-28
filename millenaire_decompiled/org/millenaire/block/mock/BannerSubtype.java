/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  net.minecraft.util.StringRepresentable
 */
package org.millenaire.block.mock;

import net.minecraft.util.StringRepresentable;

public final class BannerSubtype
extends Enum<BannerSubtype>
implements StringRepresentable {
    public static final /* enum */ BannerSubtype VILLAGE = new BannerSubtype("village", "village");
    public static final /* enum */ BannerSubtype CULTURE = new BannerSubtype("culture", "culture");
    private final String serializedName;
    private final String specialPointSubtype;
    private static final /* synthetic */ BannerSubtype[] $VALUES;

    public static BannerSubtype[] values() {
        return (BannerSubtype[])$VALUES.clone();
    }

    public static BannerSubtype valueOf(String name) {
        return Enum.valueOf(BannerSubtype.class, name);
    }

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

    private static /* synthetic */ BannerSubtype[] $values() {
        return new BannerSubtype[]{VILLAGE, CULTURE};
    }

    static {
        $VALUES = BannerSubtype.$values();
    }
}

