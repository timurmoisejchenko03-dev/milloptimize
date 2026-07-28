/*
 * Decompiled with CFR 0.150.
 */
package org.millenaire.village;

import java.util.Locale;

public final class VillageEventType
extends Enum<VillageEventType> {
    public static final /* enum */ VillageEventType FOUNDED = new VillageEventType();
    public static final /* enum */ VillageEventType BUILDING_STARTED = new VillageEventType();
    public static final /* enum */ VillageEventType BUILDING_COMPLETED = new VillageEventType();
    public static final /* enum */ VillageEventType UPGRADE_STARTED = new VillageEventType();
    public static final /* enum */ VillageEventType VILLAGER_SPAWNED = new VillageEventType();
    public static final /* enum */ VillageEventType BIRTH = new VillageEventType();
    public static final /* enum */ VillageEventType DEATH = new VillageEventType();
    public static final /* enum */ VillageEventType CAME_OF_AGE = new VillageEventType();
    public static final /* enum */ VillageEventType MERCHANT_ARRIVED = new VillageEventType();
    public static final /* enum */ VillageEventType MIGRATION = new VillageEventType();
    private static final /* synthetic */ VillageEventType[] $VALUES;

    public static VillageEventType[] values() {
        return (VillageEventType[])$VALUES.clone();
    }

    public static VillageEventType valueOf(String name) {
        return Enum.valueOf(VillageEventType.class, name);
    }

    public String i18nKey() {
        return "chronicle.millenaire." + this.name().toLowerCase(Locale.ROOT);
    }

    private static /* synthetic */ VillageEventType[] $values() {
        return new VillageEventType[]{FOUNDED, BUILDING_STARTED, BUILDING_COMPLETED, UPGRADE_STARTED, VILLAGER_SPAWNED, BIRTH, DEATH, CAME_OF_AGE, MERCHANT_ARRIVED, MIGRATION};
    }

    static {
        $VALUES = VillageEventType.$values();
    }
}

