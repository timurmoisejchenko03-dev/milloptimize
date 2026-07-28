/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 */
package org.millenaire.village;

import java.util.ArrayList;
import net.minecraft.core.BlockPos;
import org.millenaire.building.BuildingInstance;
import org.millenaire.building.BuildingPlanSet;
import org.millenaire.culture.ModCultures;
import org.millenaire.network.ControlledProjectsPayload;
import org.millenaire.village.Village;

public final class ControlledProjectsService {
    private ControlledProjectsService() {
    }

    public static ControlledProjectsPayload buildPayload(Village village) {
        BuildingPlanSet pendingSet;
        BlockPos center = village.getCenter();
        ArrayList<ControlledProjectsPayload.ProjectEntry> entries = new ArrayList<ControlledProjectsPayload.ProjectEntry>();
        BuildingInstance townhall = village.getTownhall();
        for (BuildingInstance building : village.getBuildings()) {
            BuildingPlanSet planSet;
            if (building.isSubBuilding() || (planSet = building.getPlanSetId() != null ? ModCultures.getBuildingPlanSet(building.getPlanSetId()) : null) == null) continue;
            String variant = building.getVariant() != null ? building.getVariant() : "0";
            int maxLevel = planSet.getLevelCount(variant);
            if (maxLevel <= 0) {
                maxLevel = 1;
            }
            String displayName = planSet.nativeName() != null ? planSet.nativeName() : planSet.id().getPath();
            String distance = ControlledProjectsService.formatDistanceDirection(center, building.getOrigin());
            boolean isTownHall = townhall != null && townhall.getId().equals(building.getId());
            entries.add(new ControlledProjectsPayload.ProjectEntry(building.getId().uuid().toString(), displayName, planSet.id().toString(), building.getLevel(), maxLevel, building.isUpgradesAllowed(), distance, isTownHall));
        }
        String pendingPlanName = "";
        Village.PendingProject pending = village.getPendingProject();
        if (pending != null && (pendingSet = ModCultures.getBuildingPlanSet(pending.planSetId())) != null) {
            pendingPlanName = pendingSet.nativeName() != null ? pendingSet.nativeName() : pendingSet.id().getPath();
        }
        String villageName = village.getVillageName() != null ? village.getVillageName() : "";
        return new ControlledProjectsPayload(village.getId().uuid().toString(), villageName, pendingPlanName, entries);
    }

    public static String formatDistanceDirection(BlockPos origin, BlockPos target) {
        String dir;
        int dx = target.getX() - origin.getX();
        int dz = target.getZ() - origin.getZ();
        int dist = (int)Math.sqrt(dx * dx + dz * dz);
        if (dx == 0 && dz == 0) {
            dir = "";
        } else {
            double angle = Math.atan2(-dx, -dz);
            int sector = (int)Math.round(angle / 0.7853981633974483);
            dir = switch (Math.floorMod(sector, 8)) {
                case 0 -> "N";
                case 1 -> "NE";
                case 2 -> "E";
                case 3 -> "SE";
                case 4 -> "S";
                case 5 -> "SW";
                case 6 -> "W";
                case 7 -> "NW";
                default -> "";
            };
        }
        return dir.isEmpty() ? dist + "m" : dist + "m " + dir;
    }
}

