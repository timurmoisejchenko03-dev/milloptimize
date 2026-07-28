/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 */
package org.millenaire.village;

import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import org.millenaire.building.BuildingInstance;
import org.millenaire.culture.ModCultures;
import org.millenaire.culture.VillagerType;
import org.millenaire.village.Village;
import org.millenaire.village.VillagerRecord;

public final class HearthResidentResolver {
    private HearthResidentResolver() {
    }

    public static Optional<UUID> findResident(Village village, BuildingInstance building) {
        return village.getVillagerRecords().values().stream().filter(HearthResidentResolver::isEligible).filter(r -> building.getId().equals(r.getHomeBuilding())).map(VillagerRecord::getUuid).min(Comparator.naturalOrder());
    }

    public static boolean isDesignatedResident(Village village, BuildingInstance building, @Nullable UUID villagerUuid) {
        if (villagerUuid == null) {
            return false;
        }
        return HearthResidentResolver.findResident(village, building).map(uuid -> uuid.equals(villagerUuid)).orElse(false);
    }

    static boolean isEligible(VillagerRecord record) {
        if (record.isKilled()) {
            return false;
        }
        VillagerType vType = ModCultures.getVillagerType(record.getVillagerTypeId());
        if (vType == null) {
            return false;
        }
        return !vType.isChild();
    }
}

