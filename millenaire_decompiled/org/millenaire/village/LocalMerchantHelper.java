/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.registries.BuiltInRegistries
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.level.Level
 *  org.slf4j.Logger
 */
package org.millenaire.village;

import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import org.millenaire.building.BuildingInstance;
import org.millenaire.building.BuildingInventory;
import org.millenaire.building.BuildingPlanSet;
import org.millenaire.building.ConstructionTask;
import org.millenaire.commerce.TradeGood;
import org.millenaire.commerce.TradeGoodsLoader;
import org.millenaire.culture.ModCultures;
import org.millenaire.culture.VillagerType;
import org.millenaire.entity.MillVillager;
import org.millenaire.item.ItemHelper;
import org.millenaire.village.Village;
import org.millenaire.village.VillageId;
import org.millenaire.village.VillageManager;
import org.millenaire.village.VillageSavedData;
import org.millenaire.village.VillagerRecord;
import org.slf4j.Logger;

public final class LocalMerchantHelper {
    private static final Logger LOGGER = LogUtils.getLogger();
    static final double MAX_MERCHANT_DISTANCE = 2000.0;
    static final int MIN_NIGHTS_BEFORE_MOVE = 2;
    static final int NIGHTS_BEFORE_BACKUP = 3;
    private static final long IMPORTS_CACHE_TTL_MS = 60000L;

    private LocalMerchantHelper() {
    }

    public static void attemptMerchantMoves(ServerLevel level, Village village) {
        List<BuildingInstance> inns = village.getBuildingsWithTag("inn");
        if (inns.isEmpty()) {
            return;
        }
        VillageManager vm = VillageSavedData.get(level).getVillageManager();
        LOGGER.debug("[Millenaire] [LocalMerchant] {} \u2014 checking {} inn(s) for merchant moves", (Object)village.getVillageName(), (Object)inns.size());
        for (BuildingInstance inn : inns) {
            VillagerRecord merchant = LocalMerchantHelper.getMerchantRecord(village, inn);
            if (merchant == null) {
                LOGGER.debug("[Millenaire] [LocalMerchant] {} \u2014 no merchant record in inn {}", (Object)village.getVillageName(), (Object)inn.getId());
                continue;
            }
            int nbNights = inn.incrementAndGetMerchantNights();
            if (nbNights < 2) continue;
            LocalMerchantHelper.attemptMerchantMoveFromInn(level, village, inn, merchant, vm, false);
        }
    }

    public static void forceAttemptMerchantMoves(ServerLevel level, Village village) {
        VillageManager vm = VillageSavedData.get(level).getVillageManager();
        for (BuildingInstance building : village.getBuildingsWithTag("inn")) {
            VillagerRecord merchant = LocalMerchantHelper.getMerchantRecord(village, building);
            if (merchant == null) continue;
            LocalMerchantHelper.attemptMerchantMoveFromInn(level, village, building, merchant, vm, true);
        }
    }

    static void attemptMerchantMoveFromInn(ServerLevel level, Village village, BuildingInstance inn, VillagerRecord merchant, VillageManager vm, boolean forced) {
        ArrayList<WeightedTarget> targets = new ArrayList<WeightedTarget>();
        ArrayList<WeightedTarget> backupTargets = new ArrayList<WeightedTarget>();
        Map<Item, Integer> innContents = LocalMerchantHelper.getInnContents(level, inn);
        for (Map.Entry<VillageId, Integer> rel : village.getRelations().entrySet()) {
            double distance;
            Village other;
            if (rel.getValue() < 0 || (other = vm.getVillage(rel.getKey())) == null || other == village || other.getVillageTypeId().equals((Object)village.getVillageTypeId()) || (distance = LocalMerchantHelper.linearDistance(village.getCenter(), other.getCenter())) >= 2000.0) continue;
            for (BuildingInstance destInn : other.getBuildingsWithTag("inn")) {
                boolean moveNeeded = false;
                for (Map.Entry<Item, Integer> content : innContents.entrySet()) {
                    if (content.getValue() <= 0 || LocalMerchantHelper.nbGoodNeeded(level, other, content.getKey()) <= 0) continue;
                    moveNeeded = true;
                    break;
                }
                if (moveNeeded) {
                    VillagerRecord destMerchant = LocalMerchantHelper.getMerchantRecord(other, destInn);
                    if (destMerchant == null) {
                        targets.add(new WeightedTarget(destInn, other));
                        targets.add(new WeightedTarget(destInn, other));
                        targets.add(new WeightedTarget(destInn, other));
                        continue;
                    }
                    if (destInn.getNbNightsMerchant() <= 1 && !forced) continue;
                    targets.add(new WeightedTarget(destInn, other));
                    continue;
                }
                if (inn.getNbNightsMerchant() <= 3) continue;
                backupTargets.add(new WeightedTarget(destInn, other));
            }
        }
        if (targets.isEmpty() && backupTargets.isEmpty()) {
            LOGGER.debug("[Millenaire] [LocalMerchant] No destination found for merchant {} (inn={}, nights={}, relations={}, innContents={})", new Object[]{merchant.getFirstName(), inn.getId(), inn.getNbNightsMerchant(), village.getRelations().size(), innContents.size()});
            return;
        }
        WeightedTarget target = !targets.isEmpty() ? (WeightedTarget)targets.get(ThreadLocalRandom.current().nextInt(targets.size())) : (WeightedTarget)backupTargets.get(ThreadLocalRandom.current().nextInt(backupTargets.size()));
        VillagerRecord destMerchant = LocalMerchantHelper.getMerchantRecord(target.village(), target.inn());
        if (destMerchant == null) {
            LocalMerchantHelper.moveMerchant(level, village, inn, merchant, target.village(), target.inn());
        } else if (target.inn().getNbNightsMerchant() > 1 || forced) {
            LocalMerchantHelper.swapMerchants(level, village, inn, merchant, target.village(), target.inn(), destMerchant);
        }
    }

    private static void moveMerchant(ServerLevel level, Village srcVillage, BuildingInstance srcInn, VillagerRecord merchant, Village destVillage, BuildingInstance destInn) {
        LocalMerchantHelper.transferGoods(level, srcInn, destInn);
        srcVillage.transferVillagerPermanently(level, merchant.getUuid(), destVillage, destInn.getId());
        String merchantName = merchant.getFirstName() + " " + merchant.getFamilyName();
        srcInn.addVisitorLog("panels.merchantmovedout;" + merchantName + ";" + merchant.getRoleName() + ";" + destVillage.getVillageName() + ";" + srcInn.getNbNightsMerchant());
        destInn.addVisitorLog("panels.merchantarrived;" + merchantName + ";" + merchant.getRoleName() + ";" + srcVillage.getVillageName());
        LOGGER.info("[Millenaire] [LocalMerchant] Moved merchant {} from {} to {}", new Object[]{merchantName, srcVillage.getVillageName(), destVillage.getVillageName()});
        srcInn.resetMerchantNights();
    }

    private static void swapMerchants(ServerLevel level, Village srcVillage, BuildingInstance srcInn, VillagerRecord srcMerchant, Village destVillage, BuildingInstance destInn, VillagerRecord destMerchant) {
        Map<Item, Integer> srcContents = LocalMerchantHelper.getInnContents(level, srcInn);
        Map<Item, Integer> destContents = LocalMerchantHelper.getInnContents(level, destInn);
        LocalMerchantHelper.transferGoodsFromSnapshot(level, srcInn, destInn, srcContents);
        LocalMerchantHelper.transferGoodsFromSnapshot(level, destInn, srcInn, destContents);
        srcVillage.transferVillagerPermanently(level, srcMerchant.getUuid(), destVillage, destInn.getId());
        destVillage.transferVillagerPermanently(level, destMerchant.getUuid(), srcVillage, srcInn.getId());
        String srcName = srcMerchant.getFirstName() + " " + srcMerchant.getFamilyName();
        String destName = destMerchant.getFirstName() + " " + destMerchant.getFamilyName();
        srcInn.addVisitorLog("panels.merchantmovedout;" + srcName + ";" + srcMerchant.getRoleName() + ";" + destVillage.getVillageName() + ";" + srcInn.getNbNightsMerchant());
        destInn.addVisitorLog("panels.merchantmovedout;" + destName + ";" + destMerchant.getRoleName() + ";" + srcVillage.getVillageName() + ";" + destInn.getNbNightsMerchant());
        srcInn.addVisitorLog("panels.merchantarrived;" + destName + ";" + destMerchant.getRoleName() + ";" + destVillage.getVillageName());
        destInn.addVisitorLog("panels.merchantarrived;" + srcName + ";" + srcMerchant.getRoleName() + ";" + srcVillage.getVillageName());
        LOGGER.info("[Millenaire] [LocalMerchant] Swapped merchants {} ({}) and {} ({})", new Object[]{srcName, srcVillage.getVillageName(), destName, destVillage.getVillageName()});
        srcInn.resetMerchantNights();
        destInn.resetMerchantNights();
    }

    private static void transferGoods(ServerLevel level, BuildingInstance srcInn, BuildingInstance destInn) {
        Map<Item, Integer> contents = LocalMerchantHelper.getInnContents(level, srcInn);
        LocalMerchantHelper.transferGoodsFromSnapshot(level, srcInn, destInn, contents);
    }

    private static void transferGoodsFromSnapshot(ServerLevel level, BuildingInstance srcInn, BuildingInstance destInn, Map<Item, Integer> contents) {
        BuildingInventory srcInv = srcInn.getInventory();
        BuildingInventory destInv = destInn.getInventory();
        if (srcInv == null || destInv == null) {
            return;
        }
        for (Map.Entry<Item, Integer> entry : contents.entrySet()) {
            int taken;
            Item item = entry.getKey();
            int count = entry.getValue();
            if (count <= 0 || (taken = srcInv.remove((Level)level, item, count)) <= 0) continue;
            destInv.add((Level)level, item, taken);
            destInn.addImported(item, taken);
            srcInn.addExported(item, taken);
        }
    }

    @Nullable
    public static VillagerRecord getMerchantRecord(Village village, BuildingInstance inn) {
        for (VillagerRecord record : village.getVillagerRecords().values()) {
            VillagerType vType;
            if (record.isKilled() || !inn.getId().equals(record.getHomeBuilding()) || (vType = ModCultures.getVillagerType(record.getVillagerTypeId())) == null || !vType.hasTag("localmerchant") || vType.isChild()) continue;
            return record;
        }
        return null;
    }

    private static Map<Item, Integer> getInnContents(ServerLevel level, BuildingInstance inn) {
        BuildingInventory inv = inn.getInventory();
        if (inv == null) {
            return Map.of();
        }
        return inv.scanChests((Level)level);
    }

    private static double linearDistance(BlockPos a, BlockPos b) {
        double dx = a.getX() - b.getX();
        double dy = a.getY() - b.getY();
        double dz = a.getZ() - b.getZ();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    static int nbGoodNeeded(ServerLevel level, Village village, Item item) {
        BuildingPlanSet.LevelDef levelDef;
        BuildingPlanSet planSet;
        Village.PendingProject pending;
        BuildingInstance townhall = village.getTownhall();
        if (townhall == null) {
            return 0;
        }
        BuildingInventory inv = townhall.getInventory();
        int currentStock = 0;
        if (inv != null) {
            currentStock = inv.getCount((Level)level, item);
        }
        if ((pending = village.getPendingProject()) != null) {
            for (BuildingInstance building : village.getBuildings()) {
                Entity entity;
                UUID builderUuid;
                Object task;
                if (!building.isBeingBuilt() || !building.getPlanSetId().equals((Object)pending.planSetId()) || (task = building.getConstructionTask()) == null || (builderUuid = ((ConstructionTask)task).getReservedBuilder()) == null || !((entity = level.getEntity(builderUuid)) instanceof MillVillager)) continue;
                MillVillager villager = (MillVillager)entity;
                currentStock += villager.getInventory().getCount(item);
            }
        }
        int targetAmount = 0;
        List<TradeGood> goods = TradeGoodsLoader.getGoods(village.getCultureId());
        for (TradeGood good : goods) {
            Item goodItem = ItemHelper.resolve(good.item());
            if (goodItem == null || goodItem != item || good.targetQuantity() <= 0) continue;
            targetAmount += good.targetQuantity();
        }
        int neededForProject = 0;
        if (pending != null && (planSet = ModCultures.getBuildingPlanSet(pending.planSetId())) != null && (levelDef = planSet.getLevel(pending.variant(), pending.level())) != null) {
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey((Object)item);
            Map<ResourceLocation, Integer> resCost = levelDef.requiredResources();
            if (resCost.containsKey((Object)itemId)) {
                neededForProject = resCost.get((Object)itemId);
            }
        }
        return Math.max(neededForProject + targetAmount - currentStock, 0);
    }

    public static Map<Item, Integer> getImportsNeededByOtherVillages(ServerLevel level, Village village) {
        long now = System.currentTimeMillis();
        Map<Item, Integer> cached = village.getImportsNeededCache();
        if (cached != null && now < village.getImportsNeededCacheExpiry()) {
            return cached;
        }
        HashMap<Item, Integer> result = new HashMap<Item, Integer>();
        VillageManager vm = VillageSavedData.get(level).getVillageManager();
        for (Village other : vm.getAllVillages()) {
            if (other == village || !other.getCultureId().equals((Object)village.getCultureId()) || other.getVillageTypeId().equals((Object)village.getVillageTypeId()) || !other.isActive() || other.getBuildingsWithTag("inn").isEmpty()) continue;
            List<TradeGood> otherGoods = TradeGoodsLoader.getGoods(other.getCultureId());
            for (TradeGood good : otherGoods) {
                int needed;
                Item item;
                if (good.targetQuantity() <= 0 || (item = ItemHelper.resolve(good.item())) == null || (needed = LocalMerchantHelper.nbGoodNeeded(level, other, item)) <= 0) continue;
                result.merge(item, needed, Integer::sum);
            }
        }
        village.setImportsNeededCache(result, now + 60000L);
        return result;
    }

    private record WeightedTarget(BuildingInstance inn, Village village) {
    }
}

