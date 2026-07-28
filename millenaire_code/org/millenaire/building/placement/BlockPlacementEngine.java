/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.tags.BlockTags
 *  net.minecraft.world.level.block.BedBlock
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraft.world.level.block.ChestBlock
 *  net.minecraft.world.level.block.DoorBlock
 *  net.minecraft.world.level.block.FarmBlock
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.properties.BedPart
 *  net.minecraft.world.level.block.state.properties.BlockStateProperties
 *  net.minecraft.world.level.block.state.properties.BooleanProperty
 *  net.minecraft.world.level.block.state.properties.ChestType
 *  net.minecraft.world.level.block.state.properties.DoubleBlockHalf
 *  net.minecraft.world.level.block.state.properties.Property
 */
package org.millenaire.building.placement;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Property;
import org.millenaire.block.MillPathBlock;
import org.millenaire.block.MillPathSlabBlock;

public final class BlockPlacementEngine {
    private BlockPlacementEngine() {
    }

    public static int getPlacementFlags(BlockState state) {
        if (state.is(BlockTags.BEDS) || state.getBlock() instanceof ChestBlock || state.getBlock() instanceof DoorBlock) {
            return 2;
        }
        return 3;
    }

    public static BlockState hydrateIfFarmland(BlockState state) {
        if (state.is(Blocks.FARMLAND)) {
            return (BlockState)state.setValue((Property)FarmBlock.MOISTURE, (Comparable)Integer.valueOf(7));
        }
        return state;
    }

    public static BlockState stabilizePathBlock(BlockState state) {
        if (state.getBlock() instanceof MillPathBlock) {
            return (BlockState)state.setValue((Property)MillPathBlock.STABLE, (Comparable)Boolean.valueOf(true));
        }
        if (state.getBlock() instanceof MillPathSlabBlock) {
            return (BlockState)state.setValue((Property)MillPathSlabBlock.STABLE, (Comparable)Boolean.valueOf(true));
        }
        return state;
    }

    public static boolean isBlockAlreadySuitable(BlockState existing, BlockState target) {
        if (existing.equals(target)) {
            return true;
        }
        if (target.is(Blocks.DIRT)) {
            return existing.is(Blocks.GRASS_BLOCK) || existing.is(Blocks.PODZOL) || existing.is(Blocks.MYCELIUM) || existing.is(Blocks.DIRT);
        }
        if (target.is(Blocks.GRASS_BLOCK)) {
            return existing.is(Blocks.GRASS_BLOCK) || existing.is(Blocks.PODZOL) || existing.is(Blocks.MYCELIUM);
        }
        return false;
    }

    public static void fixWaterlogging(ServerLevel level, BlockPos pos, BlockState templateState) {
        BooleanProperty waterloggedProp = BlockStateProperties.WATERLOGGED;
        if (!templateState.hasProperty((Property)waterloggedProp)) {
            return;
        }
        if (((Boolean)templateState.getValue((Property)waterloggedProp)).booleanValue()) {
            return;
        }
        BlockState worldState = level.getBlockState(pos);
        if (worldState.hasProperty((Property)waterloggedProp) && ((Boolean)worldState.getValue((Property)waterloggedProp)).booleanValue()) {
            level.setBlock(pos, (BlockState)worldState.setValue((Property)waterloggedProp, (Comparable)Boolean.valueOf(false)), 2);
        }
    }

    public static void fixAdjacentChestType(ServerLevel level, BlockPos pos, BlockState placed) {
        Direction facing = (Direction)placed.getValue((Property)ChestBlock.FACING);
        ChestType currentType = (ChestType)placed.getValue((Property)ChestBlock.TYPE);
        if (currentType != ChestType.SINGLE) {
            return;
        }
        for (Direction dir : new Direction[]{facing.getClockWise(), facing.getCounterClockWise()}) {
            BlockPos adjPos = pos.relative(dir);
            BlockState adjState = level.getBlockState(adjPos);
            if (adjState.getBlock() != placed.getBlock() || adjState.getValue((Property)ChestBlock.FACING) != facing || adjState.getValue((Property)ChestBlock.TYPE) != ChestType.SINGLE) continue;
            ChestType thisType = dir == facing.getClockWise() ? ChestType.LEFT : ChestType.RIGHT;
            ChestType otherType = thisType == ChestType.LEFT ? ChestType.RIGHT : ChestType.LEFT;
            level.setBlock(pos, (BlockState)placed.setValue((Property)ChestBlock.TYPE, (Comparable)thisType), 2);
            level.setBlock(adjPos, (BlockState)adjState.setValue((Property)ChestBlock.TYPE, (Comparable)otherType), 2);
            return;
        }
    }

    public static void clearBedIfPresent(ServerLevel level, BlockPos pos) {
        BlockState oldState = level.getBlockState(pos);
        if (!(oldState.getBlock() instanceof BedBlock)) {
            return;
        }
        if (!oldState.hasProperty((Property)BedBlock.PART) || !oldState.hasProperty((Property)BedBlock.FACING)) {
            return;
        }
        Direction facing = (Direction)oldState.getValue((Property)BedBlock.FACING);
        BedPart part = (BedPart)oldState.getValue((Property)BedBlock.PART);
        BlockPos otherPos = part == BedPart.HEAD ? pos.relative(facing.getOpposite()) : pos.relative(facing);
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
        if (level.getBlockState(otherPos).getBlock() instanceof BedBlock) {
            level.setBlock(otherPos, Blocks.AIR.defaultBlockState(), 2);
        }
    }

    public static void clearDoorIfPresent(ServerLevel level, BlockPos pos) {
        BlockState oldState = level.getBlockState(pos);
        if (!(oldState.getBlock() instanceof DoorBlock)) {
            return;
        }
        if (!oldState.hasProperty((Property)DoorBlock.HALF)) {
            return;
        }
        DoubleBlockHalf half = (DoubleBlockHalf)oldState.getValue((Property)DoorBlock.HALF);
        BlockPos otherPos = half == DoubleBlockHalf.LOWER ? pos.above() : pos.below();
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
        if (level.getBlockState(otherPos).getBlock() instanceof DoorBlock) {
            level.setBlock(otherPos, Blocks.AIR.defaultBlockState(), 2);
        }
    }

    @Nullable
    public static BlockPos generateBedFoot(ServerLevel level, BlockPos headPos, BlockState state) {
        if (!(state.getBlock() instanceof BedBlock)) {
            return null;
        }
        if (!state.hasProperty((Property)BedBlock.PART)) {
            return null;
        }
        if (state.getValue((Property)BedBlock.PART) != BedPart.HEAD) {
            return null;
        }
        Direction facing = (Direction)state.getValue((Property)BedBlock.FACING);
        BlockPos footPos = headPos.relative(facing.getOpposite());
        BlockState existing = level.getBlockState(footPos);
        if (existing.getBlock() instanceof BedBlock) {
            return null;
        }
        BlockState footState = (BlockState)state.setValue((Property)BedBlock.PART, (Comparable)BedPart.FOOT);
        level.setBlock(footPos, footState, 2);
        level.setBlock(headPos, state, 2);
        return footPos;
    }

    public static void generateDoorUpper(ServerLevel level, BlockPos lowerPos, BlockState state) {
        if (!(state.getBlock() instanceof DoorBlock)) {
            return;
        }
        if (!state.hasProperty((Property)DoorBlock.HALF)) {
            return;
        }
        if (state.getValue((Property)DoorBlock.HALF) != DoubleBlockHalf.LOWER) {
            return;
        }
        BlockPos upperPos = lowerPos.above();
        BlockState upperState = (BlockState)state.setValue((Property)DoorBlock.HALF, (Comparable)DoubleBlockHalf.UPPER);
        BlockState existing = level.getBlockState(upperPos);
        if (existing.equals(upperState)) {
            return;
        }
        level.setBlock(upperPos, upperState, 2);
        level.setBlock(lowerPos, state, 2);
    }
}

