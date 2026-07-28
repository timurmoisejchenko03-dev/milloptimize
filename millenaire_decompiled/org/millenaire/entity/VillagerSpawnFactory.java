/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.EntityType
 *  net.minecraft.world.entity.ai.attributes.AttributeInstance
 *  net.minecraft.world.entity.ai.attributes.Attributes
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.levelgen.Heightmap$Types
 *  org.slf4j.Logger
 */
package org.millenaire.entity;

import com.mojang.logging.LogUtils;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import org.millenaire.Millenaire;
import org.millenaire.building.BuildingId;
import org.millenaire.culture.ModCultures;
import org.millenaire.culture.VillagerType;
import org.millenaire.entity.MillVillager;
import org.millenaire.entity.ModEntities;
import org.millenaire.entity.VillagerAppearanceFactory;
import org.millenaire.goal.GoalRegistry;
import org.millenaire.item.ItemHelper;
import org.millenaire.village.Village;
import org.millenaire.village.VillagerRecord;
import org.slf4j.Logger;

public final class VillagerSpawnFactory {
    private static final Logger LOGGER = LogUtils.getLogger();

    private VillagerSpawnFactory() {
    }

    @Nullable
    public static MillVillager spawnInVillage(ServerLevel level, Village village, ResourceLocation villagerTypeId, BlockPos spawnPos, @Nullable BuildingId homeBuilding) {
        return VillagerSpawnFactory.spawnInVillage(level, village, villagerTypeId, spawnPos, homeBuilding, true);
    }

    @Nullable
    public static MillVillager spawnInVillage(ServerLevel level, Village village, ResourceLocation villagerTypeId, BlockPos spawnPos, @Nullable BuildingId homeBuilding, boolean applyInitialInventory) {
        return VillagerSpawnFactory.spawnInVillage(level, village, villagerTypeId, spawnPos, homeBuilding, applyInitialInventory, null);
    }

    @Nullable
    public static MillVillager spawnInVillage(ServerLevel level, Village village, ResourceLocation villagerTypeId, BlockPos spawnPos, @Nullable BuildingId homeBuilding, @Nullable VillagerRecord existingRecord) {
        return VillagerSpawnFactory.spawnInVillage(level, village, villagerTypeId, spawnPos, homeBuilding, existingRecord == null, existingRecord);
    }

    @Nullable
    private static MillVillager spawnInVillage(ServerLevel level, Village village, ResourceLocation villagerTypeId, BlockPos spawnPos, @Nullable BuildingId homeBuilding, boolean applyInitialInventory, @Nullable VillagerRecord existingRecord) {
        AttributeInstance healthAttr;
        VillagerType vType = ModCultures.getVillagerType(villagerTypeId);
        if (vType == null) {
            LOGGER.warn("[Mill\u00e9naire] Villager type not found: {}", (Object)villagerTypeId);
            return null;
        }
        MillVillager villager = (MillVillager)((EntityType)ModEntities.MILL_VILLAGER.get()).create((Level)level);
        if (villager == null) {
            return null;
        }
        villager.setVillageId(village.getId());
        villager.setVillagerTypeId(villagerTypeId);
        villager.setHomeBuilding(homeBuilding);
        if (vType.maxHealth() != 20.0f && (healthAttr = villager.getAttribute(Attributes.MAX_HEALTH)) != null) {
            healthAttr.setBaseValue((double)vType.maxHealth());
            villager.setHealth(vType.maxHealth());
        }
        BlockPos safePos = VillagerSpawnFactory.findSafeSpawnPos(level, spawnPos);
        villager.moveTo((double)safePos.getX() + 0.5, safePos.getY(), (double)safePos.getZ() + 0.5, 0.0f, 0.0f);
        GoalRegistry registry = Millenaire.getGoalRegistry();
        if (registry != null) {
            villager.initGoals(registry, vType);
        }
        if (existingRecord != null) {
            existingRecord.applyToEntity(villager);
            if (existingRecord.isRaidingVillage()) {
                villager.setRaiderEntity(true);
            }
        } else {
            VillagerAppearanceFactory.randomizeAppearance(villager, vType);
            if (applyInitialInventory) {
                for (Map.Entry<ResourceLocation, Integer> entry : vType.initialInventory().entrySet()) {
                    Item item = ItemHelper.resolve(entry.getKey());
                    if (item == null) continue;
                    villager.getInventory().add(item, entry.getValue());
                }
            }
        }
        if (existingRecord != null) {
            existingRecord.setUuid(villager.getUUID());
            village.addVillager(existingRecord);
        }
        if (!level.addFreshEntity((Entity)villager)) {
            if (existingRecord != null) {
                village.removeVillagerRecord(villager.getUUID());
            }
            return null;
        }
        if (existingRecord != null) {
            existingRecord.setKilled(false);
        }
        if (existingRecord == null) {
            village.addVillager(villager.getUUID(), villagerTypeId, homeBuilding);
            VillagerRecord newRecord = village.getVillagerRecord(villager.getUUID());
            if (newRecord != null) {
                newRecord.updateFromEntity(villager);
            }
        }
        return villager;
    }

    private static BlockPos findSafeSpawnPos(ServerLevel level, BlockPos pos) {
        for (int nudge = 1; nudge <= 5; ++nudge) {
            BlockPos candidate = pos.above(nudge);
            if (level.getBlockState(candidate).isSuffocating((BlockGetter)level, candidate) || level.getBlockState(candidate.above()).isSuffocating((BlockGetter)level, candidate.above())) continue;
            return candidate;
        }
        int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos.getX(), pos.getZ());
        LOGGER.warn("[Millenaire] findSafeSpawnPos \u2014 all nudges failed at {}, falling back to surface Y={}", (Object)pos.toShortString(), (Object)surfaceY);
        return new BlockPos(pos.getX(), surfaceY, pos.getZ());
    }
}

