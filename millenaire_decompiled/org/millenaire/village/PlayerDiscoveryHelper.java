/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.phys.AABB
 */
package org.millenaire.village;

import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.millenaire.advancement.MillAdvancements;
import org.millenaire.building.BuildingInstance;
import org.millenaire.building.BuildingInventory;
import org.millenaire.building.BuildingPlan;
import org.millenaire.building.BuildingPlanSet;
import org.millenaire.config.MillenaireServerConfig;
import org.millenaire.culture.ModCultures;
import org.millenaire.culture.VillageType;
import org.millenaire.discovery.DiscoveryToasts;
import org.millenaire.discovery.DiscoveryTracker;
import org.millenaire.item.ModItems;
import org.millenaire.village.PlayerCultureReputation;
import org.millenaire.village.Village;
import org.millenaire.village.VillageBookService;

public final class PlayerDiscoveryHelper {
    private PlayerDiscoveryHelper() {
    }

    public static void checkExplorationAdvancements(ServerLevel level, Village village) {
        VillageType vType = ModCultures.getVillageType(village.getVillageTypeId());
        if (vType == null) {
            return;
        }
        int hRadius = vType.radius();
        BlockPos center = village.getCenter();
        AABB area = new AABB((double)(center.getX() - hRadius), (double)(center.getY() - 20), (double)(center.getZ() - hRadius), (double)(center.getX() + hRadius), (double)(center.getY() + 20), (double)(center.getZ() + hRadius));
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, area)) {
            int totalCultures;
            BuildingPlan plan;
            BuildingInstance townhall;
            if (vType.loneBuilding()) {
                MillAdvancements.grant(player, MillAdvancements.EXPLORER);
            }
            if ((townhall = village.getTownhall()) != null && (plan = ModCultures.getBuildingPlan(townhall.getPlanId())) != null && plan.hasTag("hof")) {
                MillAdvancements.grant(player, MillAdvancements.PANTHEON);
            }
            PlayerCultureReputation cultureRep = PlayerCultureReputation.get(level);
            cultureRep.markCultureVisited(player.getUUID(), village.getCultureId());
            int culturesVisited = cultureRep.countCulturesVisited(player.getUUID());
            if (culturesVisited >= 3) {
                MillAdvancements.grant(player, MillAdvancements.MARCO_POLO);
            }
            if (culturesVisited < (totalCultures = MillAdvancements.ADVANCEMENT_CULTURES.size())) continue;
            MillAdvancements.grant(player, MillAdvancements.MAGELLAN);
        }
    }

    public static void checkTravelBookDiscoveries(ServerLevel level, Village village, AABB villageArea) {
        if (!((Boolean)MillenaireServerConfig.SERVER.travelBookLearning.get()).booleanValue()) {
            return;
        }
        List nearbyPlayers = level.getEntitiesOfClass(ServerPlayer.class, villageArea);
        if (nearbyPlayers.isEmpty()) {
            return;
        }
        String cultureKey = village.getCultureId().getPath();
        DiscoveryTracker tracker = DiscoveryTracker.get(level);
        VillageType vType = ModCultures.getVillageType(village.getVillageTypeId());
        for (BuildingInstance building : village.getBuildings()) {
            BuildingPlan plan;
            if (!building.isOperational() || (plan = ModCultures.getBuildingPlan(building.getPlanId())) == null) continue;
            BlockPos o = building.getOrigin();
            int minX = o.getX() + building.getCachedMinX() - 2;
            int maxX = o.getX() + building.getCachedMaxX() + 2;
            int minZ = o.getZ() + building.getCachedMinZ() - 2;
            int maxZ = o.getZ() + building.getCachedMaxZ() + 2;
            double minY = o.getY() - 5;
            double maxY = o.getY() + plan.height() + 5;
            for (ServerPlayer player : nearbyPlayers) {
                BuildingPlanSet planSet;
                double px = player.getX();
                double py = player.getY();
                double pz = player.getZ();
                if (px < (double)minX || px > (double)maxX || py < minY || py > maxY || pz < (double)minZ || pz > (double)maxZ) continue;
                ResourceLocation planSetId = building.getPlanSetId();
                if (planSetId != null && (planSet = ModCultures.getBuildingPlanSet(planSetId)) != null && planSet.travelBookDisplay() && tracker.unlockBuilding(player.getUUID(), cultureKey, planSetId.getPath())) {
                    DiscoveryToasts.sendBuildingToast(player, level, cultureKey, planSet);
                }
                if (vType == null || !vType.travelBookDisplay() || vType.loneBuilding() || !tracker.unlockVillage(player.getUUID(), cultureKey, village.getVillageTypeId().getPath())) continue;
                DiscoveryToasts.sendVillageToast(player, level, cultureKey, vType);
            }
        }
    }

    public static void regenerateScrollIfNeeded(ServerLevel level, Village village) {
        if (!village.isPlayerControlled()) {
            return;
        }
        BuildingInstance townhall = village.getTownhall();
        if (townhall == null) {
            return;
        }
        BuildingInventory inv = townhall.getInventory();
        if (inv == null) {
            return;
        }
        Map<Item, Integer> contents = inv.scanChests((Level)level);
        if (contents.getOrDefault(ModItems.VILLAGE_SCROLL.get(), 0) > 0) {
            return;
        }
        ItemStack scroll = VillageBookService.createScrollForVillage(village);
        inv.addStack((Level)level, scroll);
    }
}

