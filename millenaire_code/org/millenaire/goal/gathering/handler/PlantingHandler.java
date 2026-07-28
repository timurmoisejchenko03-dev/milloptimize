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
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraft.world.level.block.BushBlock
 *  net.minecraft.world.level.block.CropBlock
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.properties.Half
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import org.millenaire.block.BlockGrapeVine;
import org.millenaire.entity.VillagerInventory;
import org.millenaire.goal.GoalContext;
import org.millenaire.goal.gathering.GatheringTarget;
import org.millenaire.goal.gathering.GatheringType;
import org.millenaire.goal.gathering.handler.AbstractGatheringHandler;
import org.millenaire.item.BlockHelper;
import org.slf4j.Logger;

public class PlantingHandler
extends AbstractGatheringHandler {
    private static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public String id() {
        return "planting";
    }

    @Override
    public Item getDefaultHeldItem(GatheringType type) {
        return Items.WHEAT_SEEDS;
    }

    @Override
    public List<String> validate(GatheringType type) {
        ArrayList<String> errors = new ArrayList<String>();
        errors.addAll(PlantingHandler.validateBlockParam(type, "cropBlock", true));
        errors.addAll(PlantingHandler.validateItemParam(type, "seedItem", false));
        return errors;
    }

    @Override
    public boolean canStart(GoalContext ctx, GatheringType type) {
        Item seedItem = this.resolveSeedItem(type);
        if (seedItem != null && !ctx.villager().getInventory().has(seedItem, 1) && !this.homeBuildingHasItem(ctx, seedItem)) {
            return false;
        }
        return this.findPlantableBlock(ctx.level(), ctx, type, null, ctx.villager().blockPosition(), type.batchRadius()) != null;
    }

    @Override
    @Nullable
    public GatheringTarget findTarget(GoalContext ctx, GatheringType type, @Nullable GatheringTarget lastTarget) {
        BlockPos found = this.findPlantableBlock(ctx.level(), ctx, type, lastTarget, ctx.villager().blockPosition(), type.batchRadius());
        return found != null ? new GatheringTarget.BlockTarget(found) : null;
    }

    @Override
    public boolean performAction(GoalContext ctx, GatheringType type, GatheringTarget target) {
        BlockPos upperPos;
        JsonObject params;
        String cropBlockId;
        if (!(target instanceof GatheringTarget.BlockTarget)) {
            return true;
        }
        GatheringTarget.BlockTarget blockTarget = (GatheringTarget.BlockTarget)target;
        BlockPos soilPos = blockTarget.pos();
        ServerLevel level = ctx.level();
        BlockPos placePos = soilPos.above();
        BlockState aboveState = level.getBlockState(placePos);
        if (!aboveState.isAir() && !PlantingHandler.isClearableAboveSoil(aboveState)) {
            return true;
        }
        Item seedItem = this.resolveSeedItem(type);
        VillagerInventory inventory = ctx.villager().getInventory();
        if (seedItem != null && !inventory.has(seedItem, 1) && !this.takeFromHomeBuilding(ctx, seedItem, 1)) {
            return false;
        }
        BlockState soilState = level.getBlockState(soilPos);
        if (soilState.is(Blocks.GRASS_BLOCK) || soilState.is(Blocks.DIRT)) {
            level.setBlock(soilPos, Blocks.FARMLAND.defaultBlockState(), 3);
        }
        if ((cropBlockId = GsonHelper.getAsString((JsonObject)(params = type.handlerParams()), (String)"cropBlock", null)) == null) {
            return true;
        }
        Block cropBlock = BlockHelper.resolve(cropBlockId);
        if (cropBlock == null) {
            return true;
        }
        boolean isVine = cropBlock instanceof BlockGrapeVine;
        if (!level.getBlockState(placePos).isAir()) {
            level.destroyBlock(placePos, false);
        }
        if (isVine && !level.getBlockState(upperPos = placePos.above()).isAir()) {
            level.destroyBlock(upperPos, false);
        }
        BlockState cropState = cropBlock.defaultBlockState();
        if (seedItem != null && inventory.remove(seedItem, 1) < 1) {
            return false;
        }
        level.setBlock(placePos, cropState, 3);
        if (isVine) {
            BlockState upperState = (BlockState)cropState.setValue(BlockGrapeVine.HALF, (Comparable)Half.TOP);
            level.setBlock(placePos.above(), upperState, 3);
        }
        return true;
    }

    @Nullable
    private BlockPos findPlantableBlock(ServerLevel level, GoalContext ctx, GatheringType type, @Nullable GatheringTarget lastTarget, BlockPos villagerPos, int batchRadius) {
        List<BlockPos> soilPositions = this.collectSoilPositions(ctx, type);
        BlockPos reference = lastTarget != null ? lastTarget.navigationPos() : villagerPos;
        boolean isDoubleHeight = this.isDoubleHeightCrop(type);
        return PlantingHandler.findClosestBlock(soilPositions, soil -> level.isLoaded(soil) && PlantingHandler.isPlantableSoil(level.getBlockState(soil)) && PlantingHandler.isFreeToPlan(level.getBlockState(soil.above())) && (!isDoubleHeight || PlantingHandler.isFreeToPlan(level.getBlockState(soil.above(2)))), reference, lastTarget, batchRadius);
    }

    private static boolean isPlantableSoil(BlockState state) {
        return state.is(Blocks.FARMLAND) || state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT);
    }

    private static boolean isFreeToPlan(BlockState state) {
        return state.isAir() || PlantingHandler.isClearableAboveSoil(state);
    }

    private boolean isDoubleHeightCrop(GatheringType type) {
        String cropBlockId = GsonHelper.getAsString((JsonObject)type.handlerParams(), (String)"cropBlock", null);
        if (cropBlockId == null) {
            return false;
        }
        Block block = BlockHelper.resolve(cropBlockId);
        return block instanceof BlockGrapeVine;
    }

    private static boolean isClearableAboveSoil(BlockState state) {
        if (state.is(Blocks.SNOW) || state.is(BlockTags.LEAVES)) {
            return true;
        }
        Block block = state.getBlock();
        return block instanceof BushBlock && !(block instanceof CropBlock) && !(block instanceof BlockGrapeVine);
    }
}

