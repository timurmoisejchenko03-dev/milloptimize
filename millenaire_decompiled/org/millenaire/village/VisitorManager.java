/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  net.minecraft.core.BlockPos
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.util.RandomSource
 *  org.slf4j.Logger
 */
package org.millenaire.village;

import com.mojang.logging.LogUtils;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import org.millenaire.building.BuildingId;
import org.millenaire.building.BuildingInstance;
import org.millenaire.building.BuildingPlanSet;
import org.millenaire.culture.ModCultures;
import org.millenaire.entity.MillVillager;
import org.millenaire.entity.VillagerSpawnFactory;
import org.millenaire.village.Village;
import org.millenaire.village.VillagerRecord;
import org.slf4j.Logger;

public final class VisitorManager {
    private static final Logger LOGGER = LogUtils.getLogger();

    private VisitorManager() {
    }

    public static void updateVisitors(ServerLevel level, Village village, BuildingInstance building, boolean isDaytime) {
        if (isDaytime) {
            return;
        }
        BuildingPlanSet planSet = ModCultures.getBuildingPlanSet(building.getPlanSetId());
        if (planSet == null || planSet.visitors().isEmpty()) {
            return;
        }
        long currentDay = level.getDayTime() / 24000L;
        if (building.getLastMarketNightDay() > currentDay) {
            building.setLastMarketNightDay(-1L);
        }
        if (building.getLastMarketNightDay() >= currentDay) {
            return;
        }
        LinkedHashMap<String, Integer> targetCount = new LinkedHashMap<String, Integer>();
        for (String visitorType : planSet.visitors()) {
            targetCount.merge(visitorType, 1, Integer::sum);
        }
        String culturePath = village.getCultureId().getPath();
        BuildingId buildingId = building.getId();
        RandomSource random = level.getRandom();
        for (Map.Entry entry : targetCount.entrySet()) {
            int currentCount;
            String visitorType = (String)entry.getKey();
            int target = (Integer)entry.getValue();
            ResourceLocation visitorTypeId = ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)(culturePath + "/" + visitorType));
            for (int i = currentCount = VisitorManager.countVisitors(village, buildingId, visitorTypeId); i < target; ++i) {
                if (random.nextInt(2) != 0) continue;
                VisitorManager.spawnVisitor(level, village, building, visitorTypeId);
            }
        }
        building.setLastMarketNightDay(currentDay);
    }

    private static int countVisitors(Village village, BuildingId buildingId, ResourceLocation visitorTypeId) {
        int count = 0;
        for (VillagerRecord record : village.getVillagerRecords().values()) {
            BuildingId home = record.getHomeBuilding();
            if (home == null || !home.equals(buildingId) || !visitorTypeId.equals((Object)record.getVillagerTypeId())) continue;
            ++count;
        }
        return count;
    }

    private static void spawnVisitor(ServerLevel level, Village village, BuildingInstance building, ResourceLocation visitorTypeId) {
        BlockPos spawnPos = building.getSleepingPos();
        MillVillager visitor = VillagerSpawnFactory.spawnInVillage(level, village, visitorTypeId, spawnPos, building.getId());
        if (visitor == null) {
            LOGGER.warn("[Millenaire] Failed to spawn visitor {} at {}", (Object)visitorTypeId, (Object)spawnPos.toShortString());
            return;
        }
        LOGGER.debug("[Millenaire] Spawned visitor {} at building {}", (Object)visitorTypeId, (Object)building.getPlanSetId());
    }
}

