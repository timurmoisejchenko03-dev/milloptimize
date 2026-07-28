/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.world.item.context.BlockPlaceContext
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.IronBarsBlock
 *  net.minecraft.world.level.block.state.BlockBehaviour$Properties
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.StateDefinition$Builder
 *  net.minecraft.world.level.block.state.properties.BlockStateProperties
 *  net.minecraft.world.level.block.state.properties.EnumProperty
 *  net.minecraft.world.level.block.state.properties.Half
 *  net.minecraft.world.level.block.state.properties.Property
 */
package org.millenaire.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.Property;

public class IndianRosetteBlock
extends IronBarsBlock {
    public static final Property<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<Half> HALF = BlockStateProperties.HALF;

    public IndianRosetteBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState((BlockState)((BlockState)((BlockState)((BlockState)((BlockState)((BlockState)((BlockState)((BlockState)this.stateDefinition.any()).setValue((Property)NORTH, (Comparable)Boolean.valueOf(false))).setValue((Property)EAST, (Comparable)Boolean.valueOf(false))).setValue((Property)SOUTH, (Comparable)Boolean.valueOf(false))).setValue((Property)WEST, (Comparable)Boolean.valueOf(false))).setValue((Property)WATERLOGGED, (Comparable)Boolean.valueOf(false))).setValue(FACING, (Comparable)Direction.SOUTH)).setValue(HALF, (Comparable)Half.TOP));
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(new Property[]{FACING, HALF});
    }

    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState baseState = super.getStateForPlacement(context);
        if (baseState == null) {
            return null;
        }
        return this.computeRosettePattern(baseState, (BlockGetter)context.getLevel(), context.getClickedPos());
    }

    private BlockState computeRosettePattern(BlockState state, BlockGetter level, BlockPos pos) {
        BlockState above = level.getBlockState(pos.above());
        BlockState below = level.getBlockState(pos.below());
        BlockState west = level.getBlockState(pos.west());
        BlockState east = level.getBlockState(pos.east());
        BlockState south = level.getBlockState(pos.south());
        BlockState north = level.getBlockState(pos.north());
        if (above.getBlock() == this && above.getValue(HALF) == Half.TOP) {
            return (BlockState)((BlockState)state.setValue(HALF, (Comparable)Half.BOTTOM)).setValue(FACING, (Comparable)((Direction)above.getValue(FACING)));
        }
        if (west.getBlock() == this && west.getValue(FACING) == Direction.WEST) {
            return (BlockState)((BlockState)state.setValue(FACING, (Comparable)Direction.EAST)).setValue(HALF, (Comparable)((Half)west.getValue(HALF)));
        }
        if (south.getBlock() == this && south.getValue(FACING) == Direction.SOUTH) {
            return (BlockState)((BlockState)state.setValue(FACING, (Comparable)Direction.NORTH)).setValue(HALF, (Comparable)((Half)south.getValue(HALF)));
        }
        if (below.getBlock() == this && below.getValue(HALF) == Half.BOTTOM) {
            return (BlockState)((BlockState)state.setValue(HALF, (Comparable)Half.TOP)).setValue(FACING, (Comparable)((Direction)below.getValue(FACING)));
        }
        if (east.getBlock() == this && east.getValue(FACING) == Direction.EAST) {
            return (BlockState)((BlockState)state.setValue(FACING, (Comparable)Direction.WEST)).setValue(HALF, (Comparable)((Half)east.getValue(HALF)));
        }
        if (north.getBlock() == this && north.getValue(FACING) == Direction.NORTH) {
            return (BlockState)((BlockState)state.setValue(FACING, (Comparable)Direction.SOUTH)).setValue(HALF, (Comparable)((Half)north.getValue(HALF)));
        }
        BlockState result = state;
        if (!above.isSolidRender(level, pos.above()) && below.isSolidRender(level, pos.below())) {
            result = (BlockState)result.setValue(HALF, (Comparable)Half.BOTTOM);
        }
        if (!west.isSolidRender(level, pos.west()) && east.isSolidRender(level, pos.east())) {
            result = (BlockState)result.setValue(FACING, (Comparable)Direction.EAST);
        } else if (!south.isSolidRender(level, pos.south()) && north.isSolidRender(level, pos.north())) {
            result = (BlockState)result.setValue(FACING, (Comparable)Direction.NORTH);
        } else if (south.isSolidRender(level, pos.south()) && !north.isSolidRender(level, pos.north())) {
            result = (BlockState)result.setValue(FACING, (Comparable)Direction.SOUTH);
        } else if (west.isSolidRender(level, pos.west()) && !east.isSolidRender(level, pos.east())) {
            result = (BlockState)result.setValue(FACING, (Comparable)Direction.WEST);
        }
        return result;
    }
}

