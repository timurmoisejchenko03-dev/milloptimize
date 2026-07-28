/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  net.minecraft.resources.ResourceLocation
 *  org.slf4j.Logger
 */
package org.millenaire.goal.visit;

import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import org.millenaire.building.BuildingPlan;
import org.millenaire.culture.ModCultures;
import org.millenaire.goal.GoalRegistry;
import org.millenaire.goal.VillagerGoal;
import org.millenaire.goal.impl.ObserveVillagerGoal;
import org.millenaire.goal.impl.VisitBuildingGoal;
import org.millenaire.item.ItemHelper;
import org.slf4j.Logger;

public final class VisitGoalValidator {
    private static final Logger LOGGER = LogUtils.getLogger();

    private VisitGoalValidator() {
    }

    public static void validateAll(GoalRegistry goalRegistry) {
        int warnings = 0;
        for (ResourceLocation id : goalRegistry.getAllIds()) {
            VillagerGoal goal = goalRegistry.get(id);
            if (goal instanceof VisitBuildingGoal) {
                VisitBuildingGoal vb = (VisitBuildingGoal)goal;
                warnings += VisitGoalValidator.validateVisitBuilding(vb);
                continue;
            }
            if (!(goal instanceof ObserveVillagerGoal)) continue;
            ObserveVillagerGoal ov = (ObserveVillagerGoal)goal;
            warnings += VisitGoalValidator.validateObserveVillager(ov, goalRegistry);
        }
        if (warnings > 0) {
            LOGGER.warn("Visit goal validator: {} warning(s)", (Object)warnings);
        }
    }

    private static int validateVisitBuilding(VisitBuildingGoal goal) {
        int warnings = 0;
        String buildingTag = goal.buildingTag();
        if (buildingTag != null && !buildingTag.isEmpty() && !VisitGoalValidator.buildingTagExists(buildingTag)) {
            LOGGER.warn("Visit goal {}: buildingTag '{}' not referenced by any loaded BuildingPlan", (Object)goal.id(), (Object)buildingTag);
            ++warnings;
        }
        warnings += VisitGoalValidator.validateItemList(goal.id(), goal.heldItems(), "heldItems");
        return warnings += VisitGoalValidator.validateItemList(goal.id(), goal.heldItemsDestination(), "heldItemsDestination");
    }

    private static int validateObserveVillager(ObserveVillagerGoal goal, GoalRegistry goalRegistry) {
        Set<ResourceLocation> targetIds;
        int warnings = 0;
        String buildingTag = goal.buildingTag();
        if (buildingTag != null && !buildingTag.isEmpty() && !VisitGoalValidator.buildingTagExists(buildingTag)) {
            LOGGER.warn("Observe goal {}: buildingTag '{}' not referenced by any loaded BuildingPlan", (Object)goal.id(), (Object)buildingTag);
            ++warnings;
        }
        if ((targetIds = goal.targetGoalIds()) != null) {
            for (ResourceLocation rl : targetIds) {
                if (goalRegistry.get(rl) != null) continue;
                LOGGER.warn("Observe goal {}: targetGoalId '{}' not registered in GoalRegistry", (Object)goal.id(), (Object)rl);
                ++warnings;
            }
        }
        return warnings += VisitGoalValidator.validateItemList(goal.id(), goal.heldItems(), "heldItems");
    }

    private static int validateItemList(ResourceLocation goalId, List<String> items, String field) {
        if (items == null) {
            return 0;
        }
        int warnings = 0;
        for (String itemId : items) {
            if (ItemHelper.resolve(itemId) != null) continue;
            LOGGER.warn("Visit/Observe goal {}: unresolvable {} item '{}'", new Object[]{goalId, field, itemId});
            ++warnings;
        }
        return warnings;
    }

    private static boolean buildingTagExists(String tag) {
        for (BuildingPlan plan : ModCultures.getAllBuildingPlans().values()) {
            if (!plan.hasTag(tag)) continue;
            return true;
        }
        return false;
    }
}

