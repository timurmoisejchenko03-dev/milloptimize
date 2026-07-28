/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.core.registries.BuiltInRegistries
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.ItemLike
 *  net.minecraft.world.level.Level
 */
package org.millenaire.building;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import org.millenaire.building.AnywoodHelper;
import org.millenaire.building.BuildingId;
import org.millenaire.building.BuildingInstance;
import org.millenaire.building.BuildingInventory;
import org.millenaire.building.BuildingPlan;
import org.millenaire.building.BuildingPlanSet;
import org.millenaire.building.ConstructionTask;
import org.millenaire.commerce.ShopProfile;
import org.millenaire.commerce.ShopProfileLoader;
import org.millenaire.commerce.TradeGood;
import org.millenaire.commerce.TradeGoodsLoader;
import org.millenaire.culture.ModCultures;
import org.millenaire.culture.VillagerType;
import org.millenaire.entity.MillVillager;
import org.millenaire.village.Village;
import org.millenaire.village.VillagerRecord;

public final class GoodAvailabilityHelper {
    private GoodAvailabilityHelper() {
    }

    public static int nbGoodAvailable(BuildingInstance building, Item item, ServerLevel level, Village village, ResourceLocation cultureId, boolean forShop, boolean forConstruction) {
        String shopId;
        BuildingInventory inv = building.getInventory();
        if (inv == null) {
            return 0;
        }
        int nb = inv.getCount((Level)level, item);
        if (nb <= 0) {
            return 0;
        }
        BuildingPlan plan = ModCultures.getBuildingPlan(building.getPlanId());
        boolean isTownhall = plan != null && "townhall".equals(plan.shopId());
        String string = shopId = plan != null ? plan.shopId() : null;
        if (isTownhall) {
            nb -= GoodAvailabilityHelper.getConstructionReservedQuantity(item, level, village, null);
        }
        if (!forConstruction) {
            ShopProfile profile;
            boolean tradedHere = false;
            if (shopId != null && (profile = ShopProfileLoader.getProfile(cultureId, shopId)) != null) {
                tradedHere = GoodAvailabilityHelper.isItemSoldAt(item, profile, cultureId);
            }
            if (isTownhall || tradedHere) {
                int reserved = GoodAvailabilityHelper.getReservedQuantity(item, cultureId);
                nb -= reserved;
            }
            nb -= GoodAvailabilityHelper.getResidentNeeds(building, item, village);
        }
        return Math.max(nb, 0);
    }

    public static int nbGoodAvailable(BuildingInstance building, Item item, ServerLevel level, Village village, ResourceLocation cultureId, boolean forShop, boolean forConstruction, @Nullable Village.PendingProject excludeProject) {
        String shopId;
        BuildingInventory inv = building.getInventory();
        if (inv == null) {
            return 0;
        }
        int nb = inv.getCount((Level)level, item);
        if (nb <= 0) {
            return 0;
        }
        BuildingPlan plan = ModCultures.getBuildingPlan(building.getPlanId());
        boolean isTownhall = plan != null && "townhall".equals(plan.shopId());
        String string = shopId = plan != null ? plan.shopId() : null;
        if (isTownhall) {
            nb -= GoodAvailabilityHelper.getConstructionReservedQuantity(item, level, village, excludeProject);
        }
        if (!forConstruction) {
            ShopProfile profile;
            boolean tradedHere = false;
            if (shopId != null && (profile = ShopProfileLoader.getProfile(cultureId, shopId)) != null) {
                tradedHere = GoodAvailabilityHelper.isItemSoldAt(item, profile, cultureId);
            }
            if (isTownhall || tradedHere) {
                int reserved = GoodAvailabilityHelper.getReservedQuantity(item, cultureId);
                nb -= reserved;
            }
            nb -= GoodAvailabilityHelper.getResidentNeeds(building, item, village);
        }
        return Math.max(nb, 0);
    }

    public static int nbGoodAvailable(BuildingInstance building, Item item, ServerLevel level, Village village, ResourceLocation cultureId, boolean forShop) {
        return GoodAvailabilityHelper.nbGoodAvailable(building, item, level, village, cultureId, forShop, false);
    }

    private static boolean isItemSoldAt(Item item, ShopProfile profile, ResourceLocation cultureId) {
        ItemStack stack = new ItemStack((ItemLike)item);
        for (String sellName : profile.sells()) {
            TradeGood good = TradeGoodsLoader.getGoodById(cultureId, sellName);
            if (good == null || !good.matchesItem(stack)) continue;
            return true;
        }
        return false;
    }

    private static int getReservedQuantity(Item item, ResourceLocation cultureId) {
        List<TradeGood> tradeGoods = TradeGoodsLoader.getGoods(cultureId);
        if (tradeGoods == null) {
            return 0;
        }
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey((Object)item);
        for (TradeGood good : tradeGoods) {
            if (good.isTag() || !good.itemLocation().equals((Object)itemId)) continue;
            return good.reservedQuantity();
        }
        return 0;
    }

    public static int getConstructionReservedQuantity(Item item, ServerLevel level, Village village, @Nullable Village.PendingProject excludeProject) {
        Map<ResourceLocation, Integer> resCost;
        int pendingLevel;
        String variant;
        BuildingPlanSet.LevelDef pendingDef;
        BuildingPlanSet planSet;
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey((Object)item);
        int reserved = 0;
        Village.PendingProject pending = village.getPendingProject();
        boolean pendingHandled = false;
        for (BuildingInstance building : village.getBuildings()) {
            ConstructionTask task = building.getConstructionTask();
            if (task == null || task.isComplete() || !building.isBeingBuilt()) continue;
            UUID builderUuid = task.getReservedBuilder();
            if (builderUuid == null) {
                if (pending == null || pendingHandled || building.getPlanSetId() == null || !building.getPlanSetId().equals((Object)pending.planSetId()) || !pending.variant().equals(building.getVariant()) || pending.level() != building.getLevel() || pending.isUpgrade() && !Objects.equals(pending.buildingId(), building.getId())) continue;
                pendingHandled = true;
                continue;
            }
            BuildingPlanSet.LevelDef levelDef = GoodAvailabilityHelper.getLevelDefForBuilding(building);
            if (levelDef == null) continue;
            Map<ResourceLocation, Integer> resCost2 = levelDef.requiredResources();
            if (resCost2.containsKey(itemId)) {
                int needed = resCost2.get(itemId);
                int builderHas = 0;
                Entity entity = level.getEntity(builderUuid);
                if (entity instanceof MillVillager) {
                    MillVillager villager = (MillVillager)entity;
                    builderHas = villager.getInventory().getCount(item);
                }
                if (builderHas < needed) {
                    reserved += needed - builderHas;
                }
            }
            if (pending == null || pendingHandled || building.getPlanSetId() == null || !building.getPlanSetId().equals((Object)pending.planSetId()) || !pending.variant().equals(building.getVariant()) || pending.level() != building.getLevel() || pending.isUpgrade() && !Objects.equals(pending.buildingId(), building.getId())) continue;
            pendingHandled = true;
        }
        if (pending != null && !pendingHandled && !pending.equals(excludeProject) && (planSet = ModCultures.getBuildingPlanSet(pending.planSetId())) != null && (pendingDef = planSet.getLevel(variant = pending.variant(), pendingLevel = pending.level())) != null && (resCost = pendingDef.requiredResources()).containsKey(itemId)) {
            reserved += resCost.get(itemId).intValue();
        }
        return reserved;
    }

    public static Set<ResourceLocation> collectConstructionNeedItems(Village village) {
        BuildingPlanSet.LevelDef levelDef;
        BuildingPlanSet planSet;
        HashSet<ResourceLocation> items = new HashSet<ResourceLocation>();
        Village.PendingProject pending = village.getPendingProject();
        if (pending != null && (planSet = ModCultures.getBuildingPlanSet(pending.planSetId())) != null && (levelDef = planSet.getLevel(pending.variant(), pending.level())) != null) {
            items.addAll(levelDef.requiredResources().keySet());
        }
        for (BuildingInstance building : village.getBuildings()) {
            BuildingPlanSet.LevelDef levelDef2;
            ConstructionTask task = building.getConstructionTask();
            if (task == null || task.isComplete() || !building.isBeingBuilt() || (levelDef2 = GoodAvailabilityHelper.getLevelDefForBuilding(building)) == null) continue;
            items.addAll(levelDef2.requiredResources().keySet());
        }
        return items;
    }

    public static int getAnywoodReservedQuantity(ServerLevel level, Village village, @Nullable Village.PendingProject excludeProject) {
        Integer anywoodNeeded;
        int pendingLevel;
        String variant;
        BuildingPlanSet.LevelDef pendingDef;
        BuildingPlanSet planSet;
        int reserved = 0;
        Village.PendingProject pending = village.getPendingProject();
        boolean pendingHandled = false;
        for (BuildingInstance building : village.getBuildings()) {
            ConstructionTask task = building.getConstructionTask();
            if (task == null || task.isComplete() || !building.isBeingBuilt()) continue;
            UUID builderUuid = task.getReservedBuilder();
            if (builderUuid == null) {
                if (pending == null || pendingHandled || building.getPlanSetId() == null || !building.getPlanSetId().equals((Object)pending.planSetId()) || !pending.variant().equals(building.getVariant()) || pending.level() != building.getLevel() || pending.isUpgrade() && !Objects.equals(pending.buildingId(), building.getId())) continue;
                pendingHandled = true;
                continue;
            }
            BuildingPlanSet.LevelDef levelDef = GoodAvailabilityHelper.getLevelDefForBuilding(building);
            if (levelDef == null) continue;
            Map<ResourceLocation, Integer> resCost = levelDef.requiredResources();
            Integer anywoodNeeded2 = resCost.get(AnywoodHelper.ANYWOOD_LOG);
            if (anywoodNeeded2 != null && anywoodNeeded2 > 0) {
                int builderHas = 0;
                Entity entity = level.getEntity(builderUuid);
                if (entity instanceof MillVillager) {
                    MillVillager villager = (MillVillager)entity;
                    builderHas = villager.getInventory().getCountByTag(AnywoodHelper.LOGS_TAG);
                }
                if (builderHas < anywoodNeeded2) {
                    reserved += anywoodNeeded2 - builderHas;
                }
            }
            if (pending == null || pendingHandled || building.getPlanSetId() == null || !building.getPlanSetId().equals((Object)pending.planSetId()) || !pending.variant().equals(building.getVariant()) || pending.level() != building.getLevel() || pending.isUpgrade() && !Objects.equals(pending.buildingId(), building.getId())) continue;
            pendingHandled = true;
        }
        if (pending != null && !pendingHandled && !pending.equals(excludeProject) && (planSet = ModCultures.getBuildingPlanSet(pending.planSetId())) != null && (pendingDef = planSet.getLevel(variant = pending.variant(), pendingLevel = pending.level())) != null && (anywoodNeeded = pendingDef.requiredResources().get(AnywoodHelper.ANYWOOD_LOG)) != null) {
            reserved += anywoodNeeded.intValue();
        }
        return reserved;
    }

    @Nullable
    private static BuildingPlanSet.LevelDef getLevelDefForBuilding(BuildingInstance building) {
        ResourceLocation planSetId = building.getPlanSetId();
        if (planSetId == null) {
            return null;
        }
        BuildingPlanSet planSet = ModCultures.getBuildingPlanSet(planSetId);
        if (planSet == null) {
            return null;
        }
        String variant = building.getVariant();
        if (variant == null) {
            return null;
        }
        return planSet.getLevel(variant, building.getLevel());
    }

    private static int getResidentNeeds(BuildingInstance building, Item item, Village village) {
        int needs = 0;
        for (VillagerRecord record : village.getVillagerRecords().values()) {
            Integer qty;
            VillagerType vtype;
            BuildingId homeId = record.getHomeBuilding();
            if (homeId == null || !homeId.equals(building.getId()) || (vtype = ModCultures.getVillagerType(record.getVillagerTypeId())) == null || (qty = vtype.resolvedRequiredGoods().get(item)) == null) continue;
            needs += qty.intValue();
        }
        return needs;
    }
}

