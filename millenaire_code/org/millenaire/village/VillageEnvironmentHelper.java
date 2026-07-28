/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.EntityType
 *  net.minecraft.world.entity.animal.Animal
 *  net.minecraft.world.entity.monster.Creeper
 *  net.minecraft.world.entity.monster.EnderMan
 *  net.minecraft.world.entity.monster.Monster
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.SaplingBlock
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.phys.AABB
 *  org.slf4j.Logger
 */
package org.millenaire.village;

import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.millenaire.block.AppleTreeSaplingBlock;
import org.millenaire.building.BuildingInstance;
import org.millenaire.building.SpecialPoint;
import org.millenaire.culture.ModCultures;
import org.millenaire.culture.VillageType;
import org.millenaire.village.Village;
import org.slf4j.Logger;

public final class VillageEnvironmentHelper {
    private static final Logger LOGGER = LogUtils.getLogger();

    private VillageEnvironmentHelper() {
    }

    public static void despawnDangerousMobs(ServerLevel level, AABB dangerousMobsArea, Village village) {
        for (Monster mob : level.getEntitiesOfClass(Monster.class, dangerousMobsArea, e -> e instanceof Creeper || e instanceof EnderMan)) {
            mob.discard();
        }
        List<BuildingInstance> allMobsBuildings = village.getOperationalBuildingsWithTag("despawnallmobs");
        if (allMobsBuildings.isEmpty()) {
            return;
        }
        VillageType vType = ModCultures.getVillageType(village.getVillageTypeId());
        int radius = vType != null ? vType.radius() : 90;
        for (BuildingInstance b : allMobsBuildings) {
            BlockPos origin = b.getOrigin();
            AABB box = new AABB((double)(origin.getX() - radius), (double)(origin.getY() - 20), (double)(origin.getZ() - radius), (double)(origin.getX() + radius), (double)(origin.getY() + 50), (double)(origin.getZ() + radius));
            for (Monster mob : level.getEntitiesOfClass(Monster.class, box)) {
                mob.discard();
            }
        }
    }

    public static void updatePens(ServerLevel level, Village village, boolean completeRespawn) {
        boolean isDay = level.isDay();
        if (isDay && !completeRespawn) {
            return;
        }
        for (BuildingInstance building : village.getBuildings()) {
            List<SpecialPoint> animalPoints;
            if (!building.isOperational() || (animalPoints = building.getPointsByType("animalSpawn")).isEmpty()) continue;
            long lastSpawn = building.getLastAnimalSpawnTick();
            if (!completeRespawn && lastSpawn > 0L && level.getGameTime() - lastSpawn < 12000L) continue;
            HashMap<String, List> pointsByAnimal = new HashMap<String, List>();
            for (SpecialPoint sp : animalPoints) {
                String animalType = sp.subtype() != null ? sp.subtype() : "cow";
                pointsByAnimal.computeIfAbsent(animalType, k -> new ArrayList()).add(sp.pos());
            }
            boolean spawned = false;
            for (Map.Entry entry : pointsByAnimal.entrySet()) {
                BlockPos origin;
                AABB area;
                int existing;
                String animalType = (String)entry.getKey();
                List spawnPositions = (List)entry.getValue();
                int target = Math.min(spawnPositions.size(), 8);
                EntityType<?> entityType = VillageEnvironmentHelper.resolveAnimalType(animalType);
                if (entityType == null || (existing = level.getEntitiesOfClass(Animal.class, area = new AABB((double)((origin = building.getOrigin()).getX() - 15), (double)(origin.getY() - 20), (double)(origin.getZ() - 15), (double)(origin.getX() + 15), (double)(origin.getY() + 20), (double)(origin.getZ() + 15)), e -> e.getType() == entityType && !e.isBaby()).size()) >= target) continue;
                for (int i = existing; i < target; ++i) {
                    if (!completeRespawn && level.random.nextInt(10) != 0) continue;
                    BlockPos spawnPos = (BlockPos)spawnPositions.get(level.random.nextInt(spawnPositions.size()));
                    Entity animal = entityType.create((Level)level);
                    if (animal == null) continue;
                    animal.moveTo((double)spawnPos.getX() + 0.5, (double)spawnPos.getY(), (double)spawnPos.getZ() + 0.5, level.random.nextFloat() * 360.0f, 0.0f);
                    level.addFreshEntity(animal);
                    spawned = true;
                }
            }
            if (!spawned) continue;
            building.setLastAnimalSpawnTick(level.getGameTime());
        }
    }

    @Nullable
    private static EntityType<?> resolveAnimalType(String animalType) {
        return EntityType.byString((String)("minecraft:" + animalType)).orElse(null);
    }

    public static void tickGroveSaplings(ServerLevel level, Village village) {
        for (BuildingInstance building : village.getOperationalBuildingsWithTag("grove")) {
            for (SpecialPoint sp : building.getPointsByType("treeSpawn")) {
                BlockPos saplingPos;
                if (level.random.nextInt(200) != 0 || !level.isLoaded(saplingPos = sp.pos())) continue;
                BlockState state = level.getBlockState(saplingPos);
                Block block = state.getBlock();
                if (block instanceof SaplingBlock) {
                    SaplingBlock saplingBlock = (SaplingBlock)block;
                    saplingBlock.advanceTree(level, saplingPos, state, level.random);
                    continue;
                }
                block = state.getBlock();
                if (!(block instanceof AppleTreeSaplingBlock)) continue;
                AppleTreeSaplingBlock appleTreeSapling = (AppleTreeSaplingBlock)block;
                appleTreeSapling.advanceTree(level, saplingPos, state, level.random);
            }
        }
    }
}

