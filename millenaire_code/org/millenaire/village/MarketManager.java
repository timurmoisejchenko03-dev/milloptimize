/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.level.Level
 *  org.slf4j.Logger
 */
package org.millenaire.village;

import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import org.millenaire.building.BuildingId;
import org.millenaire.building.BuildingInstance;
import org.millenaire.building.BuildingInventory;
import org.millenaire.building.BuildingPlanSet;
import org.millenaire.building.SpecialPoint;
import org.millenaire.culture.Culture;
import org.millenaire.culture.ModCultures;
import org.millenaire.culture.VillagerType;
import org.millenaire.entity.MillVillager;
import org.millenaire.entity.VillagerSpawnFactory;
import org.millenaire.item.ItemHelper;
import org.millenaire.village.Village;
import org.millenaire.village.VillageEventType;
import org.millenaire.village.VillageManager;
import org.millenaire.village.VillageSavedData;
import org.millenaire.village.VillagerRecord;
import org.slf4j.Logger;

public final class MarketManager {
    private static final Logger LOGGER = LogUtils.getLogger();

    private MarketManager() {
    }

    public static void updateMarket(ServerLevel level, Village village, BuildingInstance market, boolean isDaytime) {
        if (isDaytime) {
            return;
        }
        long currentDay = level.getDayTime() / 24000L;
        if (market.getLastMarketNightDay() > currentDay) {
            market.setLastMarketNightDay(-1L);
        }
        if (market.getLastMarketNightDay() >= currentDay) {
            return;
        }
        List<SpecialPoint> stalls = market.getPointsByType("stall");
        if (stalls.isEmpty()) {
            stalls = market.getPointsByType("sellingPos");
        }
        if (stalls.isEmpty()) {
            market.setLastMarketNightDay(currentDay);
            return;
        }
        int maxMerchants = stalls.size();
        int currentMerchants = MarketManager.countForeignMerchants(village, market.getId());
        if (currentMerchants < maxMerchants) {
            LOGGER.debug("[Millenaire] Market {} attempting to spawn foreign merchant ({}/{})", new Object[]{market.getPlanSetId(), currentMerchants, maxMerchants});
            VillagerType type = MarketManager.selectMerchantType(level, village);
            if (type != null) {
                MarketManager.spawnForeignMerchant(level, village, market, type, currentMerchants);
            }
        }
        market.setLastMarketNightDay(currentDay);
    }

    @Nullable
    private static VillagerType selectMerchantType(ServerLevel level, Village village) {
        ArrayList<VillagerType> foreignCandidates = new ArrayList<VillagerType>();
        VillageManager villageManager = VillageSavedData.get(level).getVillageManager();
        ResourceLocation ownCulture = village.getCultureId();
        for (Village otherVillage : villageManager.getAllVillages()) {
            VillagerType foreignType;
            ResourceLocation otherCulture;
            int relation;
            if (otherVillage.getId().equals(village.getId()) || (relation = village.getRelation(otherVillage.getId())) <= 70 || ownCulture.equals((Object)(otherCulture = otherVillage.getCultureId())) || !MarketManager.hasMarketBuilding(otherVillage) || (foreignType = MarketManager.getRandomForeignMerchant(otherCulture)) == null) continue;
            foreignCandidates.add(foreignType);
        }
        int foreignChance = Math.min(1 + foreignCandidates.size(), 5);
        if (!foreignCandidates.isEmpty() && ThreadLocalRandom.current().nextInt(11) < foreignChance) {
            return (VillagerType)foreignCandidates.get(ThreadLocalRandom.current().nextInt(foreignCandidates.size()));
        }
        return MarketManager.getRandomForeignMerchant(ownCulture);
    }

    private static void spawnForeignMerchant(ServerLevel level, Village village, BuildingInstance market, VillagerType type, int stallIndex) {
        Culture merchantCulture;
        BlockPos spawnPos = market.resolveNavigationTarget("pathStartPos", "sellingPos");
        BuildingId marketId = market.getId();
        MillVillager merchant = VillagerSpawnFactory.spawnInVillage(level, village, type.id(), spawnPos, marketId);
        if (merchant == null) {
            LOGGER.warn("[Millenaire] Failed to spawn foreign merchant {} at {}", (Object)type.id(), (Object)spawnPos.toShortString());
            return;
        }
        merchant.setForeignMerchantStallId(stallIndex);
        BuildingInventory inventory = market.getInventory();
        if (inventory != null && type.foreignMerchantStock() != null) {
            for (Map.Entry<ResourceLocation, Integer> entry : type.foreignMerchantStock().entrySet()) {
                Item item = ItemHelper.resolve(entry.getKey());
                if (item == null) continue;
                inventory.add((Level)level, item, entry.getValue());
            }
        }
        String cultureName = (merchantCulture = ModCultures.getCulture(type.culture())) != null ? merchantCulture.displayName() : type.culture().getPath();
        village.recordChronicleEvent(level, VillageEventType.MERCHANT_ARRIVED, type.id().getPath(), cultureName);
        LOGGER.debug("[Millenaire] Spawned foreign merchant {} (stall {}) at market {}", new Object[]{type.id(), stallIndex, market.getPlanSetId()});
    }

    private static int countForeignMerchants(Village village, BuildingId buildingId) {
        int count = 0;
        for (Map.Entry<UUID, VillagerRecord> entry : village.getVillagerRecords().entrySet()) {
            VillagerType vType;
            VillagerRecord record = entry.getValue();
            if (record.getHomeBuilding() == null || !record.getHomeBuilding().equals(buildingId) || (vType = ModCultures.getVillagerType(record.getVillagerTypeId())) == null || !vType.hasTag("foreignmerchant")) continue;
            ++count;
        }
        return count;
    }

    private static boolean hasMarketBuilding(Village village) {
        for (BuildingInstance b : village.getBuildings()) {
            BuildingPlanSet planSet;
            if (!b.isOperational() || (planSet = ModCultures.getBuildingPlanSet(b.getPlanSetId())) == null || !planSet.isMarket()) continue;
            return true;
        }
        return false;
    }

    @Nullable
    private static VillagerType getRandomForeignMerchant(ResourceLocation cultureId) {
        ArrayList<VillagerType> candidates = new ArrayList<VillagerType>();
        int totalWeight = 0;
        for (VillagerType vt : ModCultures.getAllVillagerTypes().values()) {
            if (!vt.culture().equals((Object)cultureId) || !vt.hasTag("foreignmerchant")) continue;
            candidates.add(vt);
            totalWeight += vt.spawnWeight();
        }
        if (candidates.isEmpty() || totalWeight <= 0) {
            return null;
        }
        int roll = ThreadLocalRandom.current().nextInt(totalWeight);
        int cumulative = 0;
        for (VillagerType vt : candidates) {
            if (roll >= (cumulative += vt.spawnWeight())) continue;
            return vt;
        }
        return (VillagerType)candidates.get(candidates.size() - 1);
    }
}

