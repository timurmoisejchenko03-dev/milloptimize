/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.server.level.ServerLevel
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
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.millenaire.goal.GoalContext;
import org.millenaire.goal.gathering.GatheringTarget;
import org.millenaire.goal.gathering.GatheringType;
import org.millenaire.goal.gathering.handler.AbstractGatheringHandler;
import org.slf4j.Logger;

public class CocoaPlantingHandler
extends AbstractGatheringHandler {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Direction[] HORIZONTAL_DIRECTIONS = new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};

    @Override
    public String id() {
        return "cocoa_planting";
    }

    @Override
    public boolean canStart(GoalContext ctx, GatheringType type) {
        return this.findPlantablePosition(ctx.level(), ctx, type, null) != null;
    }

    @Override
    @Nullable
    public GatheringTarget findTarget(GoalContext ctx, GatheringType type, @Nullable GatheringTarget lastTarget) {
        BlockPos pos = this.findPlantablePosition(ctx.level(), ctx, type, lastTarget);
        return pos != null ? new GatheringTarget.BlockTarget(pos) : null;
    }

    @Override
    public boolean performAction(GoalContext ctx, GatheringType type, GatheringTarget target) {
        if (!(target instanceof GatheringTarget.BlockTarget)) {
            return true;
        }
        GatheringTarget.BlockTarget blockTarget = (GatheringTarget.BlockTarget)target;
        BlockPos pos = blockTarget.pos();
        ServerLevel level = ctx.level();
        if (!level.getBlockState(pos).isAir()) {
            return true;
        }
        Direction logDirection = this.findAdjacentLogDirection(level, pos);
        if (logDirection == null) {
            return true;
        }
        Direction cocoaFacing = logDirection.getOpposite();
        BlockState cocoaState = (BlockState)((BlockState)Blocks.COCOA.defaultBlockState().setValue((Property)CocoaBlock.FACING, (Comparable)cocoaFacing)).setValue((Property)CocoaBlock.AGE, (Comparable)Integer.valueOf(0));
        level.setBlock(pos, cocoaState, 3);
        return true;
    }

    @Nullable
    private BlockPos findPlantablePosition(ServerLevel level, GoalContext ctx, GatheringType type, @Nullable GatheringTarget lastTarget) {
        List<BlockPos> soilPositions = this.collectSoilPositions(ctx, type);
        BlockPos reference = lastTarget != null ? lastTarget.navigationPos() : ctx.villager().blockPosition();
        return CocoaPlantingHandler.findClosestBlock(soilPositions, soil -> level.isLoaded(soil) && level.getBlockState(soil).isAir() && this.findAdjacentLogDirection(level, (BlockPos)soil) != null, reference, lastTarget, type.batchRadius());
    }

    @Nullable
    private Direction findAdjacentLogDirection(ServerLevel level, BlockPos pos) {
        Direction found = null;
        for (Direction dir : HORIZONTAL_DIRECTIONS) {
            BlockState adjacentState;
            BlockPos adjacent = pos.relative(dir);
            if (!level.isLoaded(adjacent) || !(adjacentState = level.getBlockState(adjacent)).is(Blocks.JUNGLE_LOG)) continue;
            found = dir;
        }
        return found;
    }
}

