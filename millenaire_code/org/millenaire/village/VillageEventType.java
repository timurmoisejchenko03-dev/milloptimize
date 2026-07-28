/*
 * Decompiled with CFR 0.152.
 */
package org.millenaire.village;

import java.util.Locale;

public enum VillageEventType {
    FOUNDED,
    BUILDING_STARTED,
    BUILDING_COMPLETED,
    UPGRADE_STARTED,
    VILLAGER_SPAWNED,
    BIRTH,
    DEATH,
    CAME_OF_AGE,
    MERCHANT_ARRIVED,
    MIGRATION;


    public String i18nKey() {
        return "chronicle.millenaire." + this.name().toLowerCase(Locale.ROOT);
    }
}

