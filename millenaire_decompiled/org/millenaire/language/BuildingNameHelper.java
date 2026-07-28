/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.resources.ResourceLocation
 */
package org.millenaire.language;

import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import org.millenaire.building.BuildingInstance;
import org.millenaire.building.BuildingPlanSet;
import org.millenaire.culture.ModCultures;
import org.millenaire.culture.VillagerType;

public final class BuildingNameHelper {
    private BuildingNameHelper() {
    }

    public static String getTranslationKey(BuildingPlanSet planSet) {
        String cultureKey = planSet.culture().getPath();
        return "building.millenaire." + cultureKey + "." + planSet.buildingId();
    }

    @Nullable
    public static String getTranslationKey(ResourceLocation planSetId) {
        BuildingPlanSet planSet = ModCultures.getBuildingPlanSet(planSetId);
        if (planSet == null) {
            return null;
        }
        return BuildingNameHelper.getTranslationKey(planSet);
    }

    public static String getTranslationKey(BuildingInstance building) {
        BuildingPlanSet planSet;
        if (building.getPlanSetId() != null && (planSet = ModCultures.getBuildingPlanSet(building.getPlanSetId())) != null) {
            return BuildingNameHelper.getTranslationKey(planSet);
        }
        return building.getPlanId().getPath();
    }

    @Nullable
    public static String getPendingProjectTranslationKey(ResourceLocation planSetId) {
        BuildingPlanSet planSet = ModCultures.getBuildingPlanSet(planSetId);
        if (planSet == null) {
            return null;
        }
        return BuildingNameHelper.getTranslationKey(planSet);
    }

    public static String getServerFallbackName(BuildingInstance building) {
        BuildingPlanSet planSet;
        if (building.getPlanSetId() != null && (planSet = ModCultures.getBuildingPlanSet(building.getPlanSetId())) != null) {
            BuildingPlanSet.LevelDef levelDef = planSet.getLevel(building.getVariant(), building.getLevel());
            if (levelDef != null && levelDef.nativeName() != null) {
                return levelDef.nativeName();
            }
            return planSet.nativeName();
        }
        return building.getPlanId().getPath();
    }

    public static String getVillagerRoleKey(VillagerType vt) {
        return "role.millenaire." + vt.id().getPath().replace('/', '_');
    }
}

