/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.component.DataComponents
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.server.MinecraftServer
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.Items
 *  net.minecraft.world.item.MapItem
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.LevelAccessor
 *  net.minecraft.world.level.saveddata.maps.MapDecorationType
 *  net.minecraft.world.level.saveddata.maps.MapId
 *  net.minecraft.world.level.saveddata.maps.MapItemSavedData
 *  net.neoforged.neoforge.registries.DeferredHolder
 */
package org.millenaire.map;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.saveddata.maps.MapDecorationType;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.millenaire.building.BuildingInstance;
import org.millenaire.culture.Culture;
import org.millenaire.culture.ModCultures;
import org.millenaire.map.MillenaireMapMarkersData;
import org.millenaire.map.VillageDiscoveryHelper;
import org.millenaire.map.VillageMapDecorationTypes;
import org.millenaire.map.VillageMapMarker;
import org.millenaire.village.PlayerCultureReputation;
import org.millenaire.village.Village;
import org.millenaire.village.VillageId;
import org.millenaire.village.VillageManager;
import org.millenaire.village.VillageSavedData;

public final class VillageMapMarkerService {
    private VillageMapMarkerService() {
    }

    public static void processAllPlayers(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        if (overworld == null) {
            return;
        }
        VillageManager villageManager = VillageSavedData.get(overworld).getVillageManager();
        Collection<Village> villages = villageManager.getAllVillages();
        HashMap<VillageId, VillageDiscoveryHelper.Footprint> townHallFootprints = new HashMap<VillageId, VillageDiscoveryHelper.Footprint>(villages.size());
        ArrayList<VillageMapMarker.VillageView> aliveViews = new ArrayList<VillageMapMarker.VillageView>(villages.size());
        HashSet<VillageId> aliveIds = new HashSet<VillageId>(villages.size());
        for (Village v : villages) {
            if (v.isLoneBuilding()) continue;
            aliveIds.add(v.getId());
            aliveViews.add(new VillageMapMarker.VillageView(v.getId(), v.getCenter(), (ResourceKey<Level>)overworld.dimension(), v.getCultureId(), v.getVillageName() == null ? "" : v.getVillageName()));
            BuildingInstance townhall = v.getTownhall();
            if (townhall == null || townhall.getEffectiveWidth() <= 0) continue;
            int ox = townhall.getOrigin().getX();
            int oz = townhall.getOrigin().getZ();
            townHallFootprints.put(v.getId(), new VillageDiscoveryHelper.Footprint(ox + townhall.getCachedMinX(), ox + townhall.getCachedMaxX(), oz + townhall.getCachedMinZ(), oz + townhall.getCachedMaxZ()));
        }
        PlayerCultureReputation rep = PlayerCultureReputation.get(overworld);
        MillenaireMapMarkersData tracking = MillenaireMapMarkersData.get(overworld);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!player.level().dimension().equals(overworld.dimension())) continue;
            UUID uuid = player.getUUID();
            HashSet<VillageId> discoveredSnapshot = new HashSet<VillageId>(rep.getDiscoveredVillages(uuid));
            if (VillageMapMarkerService.isHoldingFilledMap(player)) {
                BlockPos pos = player.blockPosition();
                Set<VillageId> newly = VillageDiscoveryHelper.findNewlyDiscovered(pos.getX(), pos.getZ(), townHallFootprints, discoveredSnapshot);
                for (VillageId vid : newly) {
                    rep.markVillageDiscovered(uuid, vid);
                    discoveredSnapshot.add(vid);
                }
            }
            VillageMapMarkerService.refreshHeldMaps(player, overworld, aliveViews, aliveIds, discoveredSnapshot, tracking);
        }
    }

    private static boolean isHoldingFilledMap(ServerPlayer player) {
        return player.getMainHandItem().is(Items.FILLED_MAP) || player.getOffhandItem().is(Items.FILLED_MAP);
    }

    private static void refreshHeldMaps(ServerPlayer player, ServerLevel level, List<VillageMapMarker.VillageView> aliveViews, Set<VillageId> aliveIds, Set<VillageId> discovered, MillenaireMapMarkersData tracking) {
        ItemStack[] hands;
        for (ItemStack stack : hands = new ItemStack[]{player.getMainHandItem(), player.getOffhandItem()}) {
            MapItemSavedData data;
            MapId mapId;
            if (!stack.is(Items.FILLED_MAP) || (mapId = (MapId)stack.get(DataComponents.MAP_ID)) == null || (data = MapItem.getSavedData((ItemStack)stack, (Level)level)) == null) continue;
            VillageMapMarkerService.applyDelta(level, data, mapId.id(), aliveViews, aliveIds, discovered, tracking);
        }
    }

    private static void applyDelta(ServerLevel level, MapItemSavedData data, int mapIdInt, List<VillageMapMarker.VillageView> aliveViews, Set<VillageId> aliveIds, Set<VillageId> discovered, MillenaireMapMarkersData tracking) {
        VillageMapMarker.MapView mv = new VillageMapMarker.MapView(data.centerX, data.centerZ, data.scale, (ResourceKey<Level>)data.dimension);
        HashSet<VillageId> trackedOnThisMap = new HashSet<VillageId>(tracking.tracked(mapIdInt));
        VillageMapMarker.Delta delta = VillageMapMarker.computeDelta(mv, aliveViews, discovered, trackedOnThisMap);
        if (delta.toAdd().isEmpty() && delta.toRemove().isEmpty()) {
            return;
        }
        String prefix = VillageMapMarker.keyPrefix();
        for (VillageMapMarker.MarkerToAdd m : delta.toAdd()) {
            Culture culture = ModCultures.getCulture(m.cultureId());
            DeferredHolder<MapDecorationType, MapDecorationType> type = culture != null ? VillageMapDecorationTypes.resolveHolder(culture) : VillageMapDecorationTypes.GENERIC;
            data.addDecoration(type, (LevelAccessor)level, m.key(), (double)m.worldX(), (double)m.worldZ(), 0.0, m.displayName());
            tracking.addTracked(mapIdInt, new VillageId(UUID.fromString(m.key().substring(prefix.length()))));
        }
        for (String key : delta.toRemove()) {
            data.removeDecoration(key);
            tracking.removeTracked(mapIdInt, new VillageId(UUID.fromString(key.substring(prefix.length()))));
        }
    }
}

