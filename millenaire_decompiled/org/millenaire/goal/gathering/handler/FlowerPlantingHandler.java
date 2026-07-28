/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonArray
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  com.mojang.logging.LogUtils
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.level.LevelAccessor
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraft.world.level.block.DoublePlantBlock
 *  net.minecraft.world.level.block.state.BlockState
 *  org.slf4j.Logger
 */
package org.millenaire.goal.gathering.handler;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.millenaire.entity.VillagerInventory;
import org.millenaire.goal.GoalContext;
import org.millenaire.goal.gathering.GatheringTarget;
import org.millenaire.goal.gathering.GatheringType;
import org.millenaire.goal.gathering.handler.AbstractGatheringHandler;
import org.millenaire.item.BlockHelper;
import org.slf4j.Logger;

public class FlowerPlantingHandler
extends AbstractGatheringHandler {
    private static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public String id() {
        return "flower_planting";
    }

    @Override
    public List<String> validate(GatheringType type) {
        ArrayList<String> errors = new ArrayList<String>();
        errors.addAll(FlowerPlantingHandler.validateBlockList(type, "flowers", true));
        errors.addAll(FlowerPlantingHandler.validateItemParam(type, "seedItem", false));
        return errors;
    }

    @Override
    public boolean canStart(GoalContext ctx, GatheringType type) {
        Item seedItem = this.resolveSeedItem(type);
        if (seedItem != null && !ctx.villager().getInventory().has(seedItem, 1) && !this.homeBuildingHasItem(ctx, seedItem)) {
            return false;
        }
        return this.findPlantableBlock(ctx.level(), ctx, type, null) != null;
    }

    @Override
    @Nullable
    public GatheringTarget findTarget(GoalContext ctx, GatheringType type, @Nullable GatheringTarget lastTarget) {
        BlockPos found = this.findPlantableBlock(ctx.level(), ctx, type, lastTarget);
        return found != null ? new GatheringTarget.BlockTarget(found) : null;
    }

    @Override
    public boolean performAction(GoalContext ctx, GatheringType type, GatheringTarget target) {
        if (!(target instanceof GatheringTarget.BlockTarget)) {
            return true;
        }
        GatheringTarget.BlockTarget blockTarget = (GatheringTarget.BlockTarget)target;
        BlockPos soilPos = blockTarget.pos();
        ServerLevel level = ctx.level();
        BlockPos placePos = soilPos.above();
        if (!level.getBlockState(placePos).isAir()) {
            return true;
        }
        List<Block> flowers = this.resolveFlowers(type);
        if (flowers.isEmpty()) {
            return true;
        }
        Block flower = flowers.get(ThreadLocalRandom.current().nextInt(flowers.size()));
        if (flower instanceof DoublePlantBlock && !level.getBlockState(placePos.above()).isAir()) {
            return true;
        }
        Item seedItem = this.resolveSeedItem(type);
        if (seedItem != null) {
            VillagerInventory inventory = ctx.villager().getInventory();
            if (!inventory.has(seedItem, 1) && !this.takeFromHomeBuilding(ctx, seedItem, 1)) {
                return false;
            }
            inventory.remove(seedItem, 1);
        }
        if (flower instanceof DoublePlantBlock) {
            DoublePlantBlock doublePlant = (DoublePlantBlock)flower;
            DoublePlantBlock.placeAt((LevelAccessor)level, (BlockState)doublePlant.defaultBlockState(), (BlockPos)placePos, (int)3);
        } else {
            level.setBlock(placePos, flower.defaultBlockState(), 3);
        }
        return true;
    }

    @Nullable
    private BlockPos findPlantableBlock(ServerLevel level, GoalContext ctx, GatheringType type, @Nullable GatheringTarget lastTarget) {
        List<BlockPos> soilPositions = this.collectSoilPositions(ctx, type);
        boolean needsDoubleAir = this.allFlowersAreDoublePlant(type);
        BlockPos reference = lastTarget != null ? lastTarget.navigationPos() : ctx.villager().blockPosition();
        return FlowerPlantingHandler.findClosestBlock(soilPositions, soil -> level.isLoaded(soil) && level.getBlockState(soil).is(Blocks.GRASS_BLOCK) && level.getBlockState(soil.above()).isAir() && (!needsDoubleAir || level.getBlockState(soil.above().above()).isAir()), reference, lastTarget, type.batchRadius());
    }

    private boolean allFlowersAreDoublePlant(GatheringType type) {
        List<Block> flowers = this.resolveFlowers(type);
        if (flowers.isEmpty()) {
            return false;
        }
        for (Block flower : flowers) {
            if (flower instanceof DoublePlantBlock) continue;
            return false;
        }
        return true;
    }

    private List<Block> resolveFlowers(GatheringType type) {
        ArrayList<Block> flowers = new ArrayList<Block>();
        JsonObject params = type.handlerParams();
        if (!params.has("flowers")) {
            return flowers;
        }
        JsonArray array = params.getAsJsonArray("flowers");
        for (JsonElement elem : array) {
            String blockId = elem.getAsString();
            Block block = BlockHelper.resolve(blockId);
            if (block == null) continue;
            flowers.add(block);
        }
        return flowers;
    }
}

