/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 */
package org.millenaire.building;

import java.util.Collection;
import javax.annotation.Nullable;
import org.millenaire.building.BuildingInstance;
import org.millenaire.building.BuildingPlan;
import org.millenaire.building.BuildingPlanSet;
import org.millenaire.culture.ModCultures;
import org.millenaire.village.Village;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RuntimeTagHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(RuntimeTagHelper.class);

    private RuntimeTagHelper() {
    }

    public static void applyCompletionTags(BuildingInstance building, Collection<String> planTags, BuildingPlanSet.LevelDef levelDef, @Nullable BuildingInstance parent, @Nullable BuildingInstance townhall) {
        building.addRuntimeTags(planTags);
        if (!levelDef.clearTags().isEmpty()) {
            building.removeRuntimeTags(levelDef.clearTags());
        }
        if (!levelDef.parentTags().isEmpty()) {
            if (parent != null) {
                parent.addRuntimeTags(levelDef.parentTags());
            } else {
                LOGGER.warn("Building {} has parentTags but no parent found", (Object)building.getId());
            }
        }
        if (!levelDef.villageTags().isEmpty()) {
            if (townhall != null) {
                townhall.addRuntimeTags(levelDef.villageTags());
            } else {
                LOGGER.warn("Building {} has villageTags but no TownHall found", (Object)building.getId());
            }
        }
    }

    public static void applyCompletionTagsForBuilding(Village village, BuildingInstance building) {
        BuildingPlanSet planSet;
        BuildingPlan plan = ModCultures.getBuildingPlan(building.getPlanId());
        if (plan == null) {
            return;
        }
        BuildingPlanSet.LevelDef levelDef = null;
        if (building.getPlanSetId() != null && building.getVariant() != null && (planSet = ModCultures.getBuildingPlanSet(building.getPlanSetId())) != null) {
            levelDef = planSet.getLevel(building.getVariant(), building.getLevel());
        }
        BuildingInstance parent = null;
        if (building.getParentBuildingId() != null && (parent = village.findBuildingById(building.getParentBuildingId())) == null) {
            LOGGER.warn("Parent building {} not found for {}", (Object)building.getParentBuildingId(), (Object)building.getId());
        }
        BuildingInstance townhall = village.getTownhall();
        if (levelDef != null) {
            RuntimeTagHelper.applyCompletionTags(building, plan.tags(), levelDef, parent, townhall);
        } else {
            building.addRuntimeTags(plan.tags());
        }
        village.invalidateBuildingTagCache();
    }
}

