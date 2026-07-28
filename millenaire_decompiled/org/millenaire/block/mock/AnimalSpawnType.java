/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  net.minecraft.util.StringRepresentable
 */
package org.millenaire.block.mock;

import net.minecraft.util.StringRepresentable;

public final class AnimalSpawnType
extends Enum<AnimalSpawnType>
implements StringRepresentable {
    public static final /* enum */ AnimalSpawnType COW = new AnimalSpawnType("cow");
    public static final /* enum */ AnimalSpawnType PIG = new AnimalSpawnType("pig");
    public static final /* enum */ AnimalSpawnType SHEEP = new AnimalSpawnType("sheep");
    public static final /* enum */ AnimalSpawnType CHICKEN = new AnimalSpawnType("chicken");
    public static final /* enum */ AnimalSpawnType SQUID = new AnimalSpawnType("squid");
    public static final /* enum */ AnimalSpawnType WOLF = new AnimalSpawnType("wolf");
    public static final /* enum */ AnimalSpawnType POLAR_BEAR = new AnimalSpawnType("polar_bear");
    private final String serializedName;
    private static final /* synthetic */ AnimalSpawnType[] $VALUES;

    public static AnimalSpawnType[] values() {
        return (AnimalSpawnType[])$VALUES.clone();
    }

    public static AnimalSpawnType valueOf(String name) {
        return Enum.valueOf(AnimalSpawnType.class, name);
    }

    private AnimalSpawnType(String serializedName) {
        this.serializedName = serializedName;
    }

    public String getSerializedName() {
        return this.serializedName;
    }

    private static /* synthetic */ AnimalSpawnType[] $values() {
        return new AnimalSpawnType[]{COW, PIG, SHEEP, CHICKEN, SQUID, WOLF, POLAR_BEAR};
    }

    static {
        $VALUES = AnimalSpawnType.$values();
    }
}

