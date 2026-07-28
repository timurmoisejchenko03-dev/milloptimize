/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.item.Items
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraft.world.level.block.CocoaBlock
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.properties.Property
 *  org.slf4j.Logger
 */
package org.millenaire.goal.gathering.handler;

import com.mojang.logging.LogUtils;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.millenaire.goal.GoalContext;
import org.millenaire.goal.gathering.GatheringTarget;
import org.millenaire.goal.gathering.GatheringType;
import org.millenaire.goal.gathering.handler.AbstractGatheringHandler;
import org.slf4j.Logger;

public class CocoaHarvestingHandler
extends AbstractGatheringHandler {
    private static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public String id() {
        return "cocoa_harvesting";
    }

    @Override
    public boolean canStart(GoalContext ctx, GatheringType type) {
        List<BlockPos> soilPositions = this.collectSoilPositions(ctx, type);
        for (BlockPos soil : soilPositions) {
            BlockState state;
            if (!ctx.level().isLoaded(soil) || !(state = ctx.level().getBlockState(soil)).is(Blocks.COCOA) || (Integer)state.getValue((Property)CocoaBlock.AGE) < 2) continue;
            return true;
        }
        return false;
    }

    @Override
    @Nullable
    public GatheringTarget findTarget(GoalContext ctx, GatheringType type, @Nullable GatheringTarget lastTarget) {
        ServerLevel level = ctx.level();
        List<BlockPos> soilPositions = this.collectSoilPositions(ctx, type);
        BlockPos reference = lastTarget != null ? lastTarget.navigationPos() : ctx.villager().blockPosition();
        BlockPos best = CocoaHarvestingHandler.findClosestBlock(soilPositions, pos -> level.isLoaded(pos) && CocoaHarvestingHandler.isMatureCocoa(level.getBlockState(pos)), reference, lastTarget, type.batchRadius());
        return best != null ? new GatheringTarget.BlockTarget(best) : null;
    }

    private static boolean isMatureCocoa(BlockState state) {
        return state.is(Blocks.COCOA) && (Integer)state.getValue((Property)CocoaBlock.AGE) >= 2;
    }

    @Override
    public boolean performAction(GoalContext ctx, GatheringType type, GatheringTarget target) {
        if (!(target instanceof GatheringTarget.BlockTarget)) {
            return true;
        }
        GatheringTarget.BlockTarget blockTarget = (GatheringTarget.BlockTarget)target;
        BlockPos pos = blockTarget.pos();
        ServerLevel level = ctx.level();
        BlockState state = level.getBlockState(pos);
        if (!state.is(Blocks.COCOA) || (Integer)state.getValue((Property)CocoaBlock.AGE) < 2) {
            return true;
        }
        level.destroyBlock(pos, false);
        int nbCrop = 2;
        int irrigation = ctx.village().getVillageIrrigation();
        if (irrigation > 0 && Math.random() < (double)irrigation / 100.0) {
            ++nbCrop;
        }
        ctx.villager().getInventory().add(Items.COCOA_BEANS, nbCrop);
        return true;
    }
}

