/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 */
package org.millenaire.world;

import java.util.Map;
import javax.annotation.Nullable;
import org.millenaire.building.BuildingPlanSet;
import org.millenaire.config.MillenaireServerConfig;
import org.millenaire.culture.VillageType;

public record PlacementConstraints(int minDistance, int maxDistance, Map<String, Integer> farFromTags, Map<String, Integer> closeToTags, int clearMargin, @Nullable Integer fixedOrientation) {
    public static final int DEFAULT_MIN_DISTANCE = 5;
    public static final int DEFAULT_MAX_DISTANCE = 60;
    private static final int DEFAULT_CLEAR_MARGIN = 5;

    public static int getDefaultClearMargin() {
        try {
            return MillenaireServerConfig.SERVER.minBuildingSpacing.getAsInt();
        }
        catch (IllegalStateException e) {
            return 5;
        }
    }

    public static PlacementConstraints resolve(BuildingPlanSet planSet, @Nullable VillageType.LayoutSlot slot, int radius) {
        int maxDist;
        double minFraction = slot != null && slot.hasMinDistanceOverride() ? slot.minDistance() : planSet.minDistance();
        double maxFraction = slot != null && slot.hasMaxDistanceOverride() ? slot.maxDistance() : planSet.maxDistance();
        int minDist = Math.max(5, (int)(minFraction * (double)radius));
        if (minDist > (maxDist = Math.max(5, (int)(maxFraction * (double)radius)))) {
            minDist = maxDist;
        }
        Map<String, Integer> farFrom = slot != null && !slot.farFromTags().isEmpty() ? slot.farFromTags() : planSet.farFromTags();
        Map<String, Integer> closeTo = slot != null && !slot.closeToTags().isEmpty() ? slot.closeToTags() : planSet.closeToTags();
        Integer orientation = slot != null && slot.fixedOrientation() != null ? slot.fixedOrientation() : planSet.fixedOrientation();
        return new PlacementConstraints(minDist, maxDist, farFrom, closeTo, slot != null ? slot.clearMargin() : PlacementConstraints.getDefaultClearMargin(), orientation);
    }
}

