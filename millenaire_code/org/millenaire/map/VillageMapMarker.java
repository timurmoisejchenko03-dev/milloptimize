/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.network.chat.Component
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.level.Level
 */
package org.millenaire.map;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.millenaire.village.VillageId;

public final class VillageMapMarker {
    private static final String KEY_PREFIX = "millenaire:village/";

    private VillageMapMarker() {
    }

    public static Delta computeDelta(MapView map, List<VillageView> aliveVillages, Set<VillageId> discoveredByPlayer, Set<VillageId> trackedOnThisMap) {
        HashSet<VillageId> aliveIds = new HashSet<VillageId>();
        for (VillageView villageView : aliveVillages) {
            aliveIds.add(villageView.id());
        }
        ArrayList<MarkerToAdd> toAdd = new ArrayList<MarkerToAdd>();
        for (VillageView v : aliveVillages) {
            if (!discoveredByPlayer.contains(v.id()) || !v.dimension().equals(map.dimension()) || !VillageMapMarker.withinBounds(map, v.center())) continue;
            toAdd.add(new MarkerToAdd(KEY_PREFIX + String.valueOf(v.id().uuid()), v.cultureId(), v.center().getX(), v.center().getZ(), (Component)Component.literal((String)v.displayName())));
        }
        ArrayList<String> arrayList = new ArrayList<String>();
        for (VillageId tracked : trackedOnThisMap) {
            if (aliveIds.contains(tracked)) continue;
            arrayList.add(KEY_PREFIX + String.valueOf(tracked.uuid()));
        }
        return new Delta(toAdd, arrayList);
    }

    private static boolean withinBounds(MapView map, BlockPos pos) {
        int halfWidth = 63 * (1 << map.scale());
        return Math.abs(pos.getX() - map.centerX()) <= halfWidth && Math.abs(pos.getZ() - map.centerZ()) <= halfWidth;
    }

    public static String keyPrefix() {
        return KEY_PREFIX;
    }

    public record VillageView(VillageId id, BlockPos center, ResourceKey<Level> dimension, ResourceLocation cultureId, String displayName) {
        public VillageView {
            Objects.requireNonNull(displayName, "displayName");
        }
    }

    public record MapView(int centerX, int centerZ, int scale, ResourceKey<Level> dimension) {
    }

    public record MarkerToAdd(String key, ResourceLocation cultureId, int worldX, int worldZ, Component displayName) {
    }

    public record Delta(List<MarkerToAdd> toAdd, List<String> toRemove) {
    }
}

