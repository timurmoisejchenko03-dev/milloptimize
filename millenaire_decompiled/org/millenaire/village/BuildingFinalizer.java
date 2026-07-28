/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  net.minecraft.server.level.ServerLevel
 */
package org.millenaire.village;

import net.minecraft.server.level.ServerLevel;
import org.millenaire.building.BuildingInstance;
import org.millenaire.building.BuildingPlan;
import org.millenaire.building.RuntimeTagHelper;
import org.millenaire.village.Village;
import org.millenaire.village.VillageBannerApplier;
import org.millenaire.village.panel.PanelPlacer;
import org.millenaire.world.BuildingPlacer;

public final class BuildingFinalizer {
    private BuildingFinalizer() {
    }

    public static void applyPostPlacement(ServerLevel level, Village village, BuildingInstance instance, BuildingPlan plan) {
        instance.resolveSpecialPoints(plan);
        BuildingPlacer.clearPlantsAboveSoil(level, instance);
        instance.initInventory();
        instance.linkChestsToBuilding(level);
        VillageBannerApplier.applyBanners(level, instance, village);
    }

    public static void applyCompletionEffects(ServerLevel level, Village village, BuildingInstance instance) {
        RuntimeTagHelper.applyCompletionTagsForBuilding(village, instance);
        PanelPlacer.placePanels(level, village, instance);
        if (!instance.getPointsByType("animalSpawn").isEmpty()) {
            village.updatePens(level, true);
        }
    }

    public static void applyVillageUpdates(ServerLevel level, Village village) {
        village.markWaypointGraphDirty();
        village.sendFireplacePositions(level);
    }
}

