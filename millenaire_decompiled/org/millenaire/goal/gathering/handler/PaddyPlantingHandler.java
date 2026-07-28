/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.properties.Property
 */
package org.millenaire.goal.gathering.handler;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.millenaire.block.RicePaddyBlock;
import org.millenaire.goal.GoalContext;
import org.millenaire.goal.gathering.GatheringTarget;
import org.millenaire.goal.gathering.GatheringType;
import org.millenaire.goal.gathering.handler.AbstractGatheringHandler;

public class PaddyPlantingHandler
extends AbstractGatheringHandler {
    @Override
    public String id() {
        return "paddy_planting";
    }

    @Override
    public boolean supportsRemoteAction() {
        return true;
    }

    @Override
    public boolean canStart(GoalContext ctx, GatheringType type) {
        BlockPos result = this.findPlantableBlock(ctx.level(), ctx, type, null, ctx.villager().blockPosition(), type.batchRadius());
        return result != null;
    }

    @Override
    @Nullable
    public GatheringTarget findTarget(GoalContext ctx, GatheringType type, @Nullable GatheringTarget lastTarget) {
        BlockPos found = this.findPlantableBlock(ctx.level(), ctx, type, lastTarget, ctx.villager().blockPosition(), type.batchRadius());
        return found != null ? new GatheringTarget.BlockTarget(found) : null;
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
        if (!RicePaddyBlock.canPlant(state)) {
            return true;
        }
        level.setBlock(pos, (BlockState)((BlockState)state.setValue((Property)RicePaddyBlock.PLANTED, (Comparable)Boolean.valueOf(true))).setValue((Property)RicePaddyBlock.AGE, (Comparable)Integer.valueOf(0)), 3);
        return true;
    }

    @Nullable
    private BlockPos findPlantableBlock(ServerLevel level, GoalContext ctx, GatheringType type, @Nullable GatheringTarget lastTarget, BlockPos villagerPos, int batchRadius) {
        List<BlockPos> soilPositions = this.collectSoilPositions(ctx, type);
        BlockPos reference = lastTarget != null ? lastTarget.navigationPos() : villagerPos;
        return PaddyPlantingHandler.findClosestBlock(soilPositions, pos -> level.isLoaded(pos) && RicePaddyBlock.canPlant(level.getBlockState(pos)), reference, lastTarget, batchRadius);
    }
}

