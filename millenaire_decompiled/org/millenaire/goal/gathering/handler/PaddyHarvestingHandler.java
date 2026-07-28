/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.properties.Property
 */
package org.millenaire.goal.gathering.handler;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.millenaire.block.RicePaddyBlock;
import org.millenaire.goal.GoalContext;
import org.millenaire.goal.gathering.GatheringTarget;
import org.millenaire.goal.gathering.GatheringType;
import org.millenaire.goal.gathering.handler.AbstractGatheringHandler;

public class PaddyHarvestingHandler
extends AbstractGatheringHandler {
    @Override
    public String id() {
        return "paddy_harvesting";
    }

    @Override
    public boolean supportsRemoteAction() {
        return true;
    }

    @Override
    public boolean canStart(GoalContext ctx, GatheringType type) {
        return this.findHarvestableBlock(ctx.level(), ctx, type) != null;
    }

    @Override
    @Nullable
    public GatheringTarget findTarget(GoalContext ctx, GatheringType type, @Nullable GatheringTarget lastTarget) {
        ServerLevel level = ctx.level();
        List<BlockPos> soilPositions = this.collectSoilPositions(ctx, type);
        BlockPos reference = lastTarget != null ? lastTarget.navigationPos() : ctx.villager().blockPosition();
        BlockPos best = PaddyHarvestingHandler.findClosestBlock(soilPositions, pos -> level.isLoaded(pos) && RicePaddyBlock.canHarvest(level.getBlockState(pos)), reference, lastTarget, type.batchRadius());
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
        BlockState state = level.getBlockState(pos);
        if (!RicePaddyBlock.canHarvest(state)) {
            return true;
        }
        List drops = Block.getDrops((BlockState)state, (ServerLevel)level, (BlockPos)pos, null);
        for (ItemStack drop : drops) {
            ctx.villager().getInventory().add(drop.getItem(), drop.getCount());
        }
        this.applyIrrigationBonus(ctx, type);
        level.setBlock(pos, (BlockState)((BlockState)state.setValue((Property)RicePaddyBlock.PLANTED, (Comparable)Boolean.valueOf(false))).setValue((Property)RicePaddyBlock.AGE, (Comparable)Integer.valueOf(0)), 3);
        return true;
    }

    @Nullable
    private BlockPos findHarvestableBlock(ServerLevel level, GoalContext ctx, GatheringType type) {
        List<BlockPos> soilPositions = this.collectSoilPositions(ctx, type);
        for (BlockPos soil : soilPositions) {
            if (!level.isLoaded(soil) || !RicePaddyBlock.canHarvest(level.getBlockState(soil))) continue;
            return soil;
        }
        return null;
    }
}

