/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonObject
 *  com.mojang.logging.LogUtils
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.tags.BlockTags
 *  net.minecraft.util.GsonHelper
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.Items
 *  net.minecraft.world.level.LevelReader
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraft.world.level.block.state.BlockState
 *  org.slf4j.Logger
 */
package org.millenaire.goal.gathering.handler;

import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.millenaire.block.AppleTreeSaplingBlock;
import org.millenaire.block.ModBlocks;
import org.millenaire.block.OliveTreeSaplingBlock;
import org.millenaire.block.PistachioTreeSaplingBlock;
import org.millenaire.building.BuildingInstance;
import org.millenaire.building.BuildingPlan;
import org.millenaire.building.SpecialPoint;
import org.millenaire.culture.ModCultures;
import org.millenaire.goal.GoalContext;
import org.millenaire.goal.gathering.GatheringTarget;
import org.millenaire.goal.gathering.GatheringType;
import org.millenaire.goal.gathering.handler.AbstractGatheringHandler;
import org.millenaire.item.BlockHelper;
import org.millenaire.village.Village;
import org.slf4j.Logger;

public class SaplingPlantingHandler
extends AbstractGatheringHandler {
    private static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public String id() {
        return "sapling_planting";
    }

    @Override
    public Item getDefaultHeldItem(GatheringType type) {
        return Items.OAK_SAPLING;
    }

    @Override
    public boolean canStart(GoalContext ctx, GatheringType type) {
        List<BuildingInstance> buildings = this.resolveTargetBuildingsWithGrove(ctx, type);
        String subtypeFilter = this.getSubtypeFilter(type);
        return !this.collectEmptyTreeSpawns(ctx.level(), buildings, subtypeFilter).isEmpty();
    }

    @Override
    @Nullable
    public GatheringTarget findTarget(GoalContext ctx, GatheringType type, @Nullable GatheringTarget lastTarget) {
        List<BuildingInstance> buildings = this.resolveTargetBuildingsWithGrove(ctx, type);
        String subtypeFilter = this.getSubtypeFilter(type);
        List<BlockPos> candidates = this.collectEmptyTreeSpawns(ctx.level(), buildings, subtypeFilter);
        BlockPos reference = lastTarget != null ? lastTarget.navigationPos() : ctx.villager().blockPosition();
        BlockPos best = SaplingPlantingHandler.findClosestBlock(candidates, reference, lastTarget, type.batchRadius());
        return best != null ? new GatheringTarget.BlockTarget(best) : null;
    }

    @Override
    public boolean performAction(GoalContext ctx, GatheringType type, GatheringTarget target) {
        if (!(target instanceof GatheringTarget.BlockTarget)) {
            return true;
        }
        GatheringTarget.BlockTarget blockTarget = (GatheringTarget.BlockTarget)target;
        BlockPos pos = blockTarget.pos();
        ServerLevel level = ctx.level();
        String subtype = this.findSubtypeAt(pos, ctx.village());
        BlockState stateAtPos = level.getBlockState(pos);
        if (!SaplingPlantingHandler.isPlantablePosition(stateAtPos)) {
            return true;
        }
        Block sapling = this.resolveSaplingFromSubtype(subtype);
        BlockState saplingState = sapling.defaultBlockState();
        if (!saplingState.canSurvive((LevelReader)level, pos)) {
            return true;
        }
        level.setBlock(pos, saplingState, 3);
        return true;
    }

    private List<BuildingInstance> resolveTargetBuildingsWithGrove(GoalContext ctx, GatheringType type) {
        List<BuildingInstance> buildings = this.resolveTargetBuildings(ctx, type);
        if (!buildings.isEmpty()) {
            return buildings;
        }
        return this.findBuildingsWithTag(ctx.village(), "grove");
    }

    @Nullable
    private String getSubtypeFilter(GatheringType type) {
        JsonObject params = type.handlerParams();
        return GsonHelper.getAsString((JsonObject)params, (String)"subtypeFilter", null);
    }

    private List<BlockPos> collectEmptyTreeSpawns(ServerLevel level, List<BuildingInstance> buildings, @Nullable String subtypeFilter) {
        ArrayList<BlockPos> candidates = new ArrayList<BlockPos>();
        for (BuildingInstance building : buildings) {
            if (!building.isOperational()) continue;
            for (SpecialPoint sp : building.getPointsByType("treeSpawn")) {
                Block sapling;
                BlockState stateAt;
                BlockPos spPos;
                if (subtypeFilter != null && !subtypeFilter.equals(sp.subtype()) || !level.isLoaded(spPos = sp.pos()) || (stateAt = level.getBlockState(spPos)).is(BlockTags.SAPLINGS) || stateAt.is(BlockTags.LOGS) || stateAt.is(BlockTags.LEAVES) || stateAt.getBlock() instanceof AppleTreeSaplingBlock || stateAt.getBlock() instanceof OliveTreeSaplingBlock || stateAt.getBlock() instanceof PistachioTreeSaplingBlock || !SaplingPlantingHandler.isPlantablePosition(stateAt) || !(sapling = this.resolveSaplingFromSubtype(sp.subtype())).defaultBlockState().canSurvive((LevelReader)level, spPos)) continue;
                candidates.add(spPos);
            }
        }
        return candidates;
    }

    @Nullable
    private String findSubtypeAt(BlockPos pos, Village village) {
        for (BuildingInstance building : village.getBuildings()) {
            BuildingPlan plan = ModCultures.getBuildingPlan(building.getPlanId());
            if (plan == null) continue;
            for (SpecialPoint sp : building.getPointsByType("treeSpawn")) {
                if (!sp.pos().equals((Object)pos)) continue;
                return sp.subtype();
            }
        }
        return null;
    }

    private Block resolveSaplingFromSubtype(@Nullable String subtype) {
        if (subtype == null) {
            return Blocks.OAK_SAPLING;
        }
        return switch (subtype) {
            case "oak" -> Blocks.OAK_SAPLING;
            case "pine" -> Blocks.SPRUCE_SAPLING;
            case "dark_oak" -> Blocks.DARK_OAK_SAPLING;
            case "birch" -> Blocks.BIRCH_SAPLING;
            case "jungle" -> Blocks.JUNGLE_SAPLING;
            case "acacia" -> Blocks.ACACIA_SAPLING;
            case "apple" -> (AppleTreeSaplingBlock)((Object)ModBlocks.APPLE_TREE_SAPLING.get());
            case "olive" -> (OliveTreeSaplingBlock)((Object)ModBlocks.OLIVE_TREE_SAPLING.get());
            case "pistachio" -> (PistachioTreeSaplingBlock)((Object)ModBlocks.PISTACHIO_TREE_SAPLING.get());
            default -> Blocks.OAK_SAPLING;
        };
    }

    private static boolean isPlantablePosition(BlockState state) {
        if (state.isAir()) {
            return true;
        }
        if (state.is(Blocks.SNOW)) {
            return true;
        }
        if (state.is(BlockTags.REPLACEABLE_BY_TREES)) {
            return true;
        }
        if (BlockHelper.isDecorativeFlower(state)) {
            return true;
        }
        return state.is(BlockTags.SMALL_FLOWERS);
    }
}

