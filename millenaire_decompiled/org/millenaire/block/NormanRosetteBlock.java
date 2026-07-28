/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.world.item.context.BlockPlaceContext
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.LevelAccessor
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.IronBarsBlock
 *  net.minecraft.world.level.block.state.BlockBehaviour$Properties
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.StateDefinition$Builder
 *  net.minecraft.world.level.block.state.properties.BooleanProperty
 *  net.minecraft.world.level.block.state.properties.Property
 */
package org.millenaire.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;

public class NormanRosetteBlock
extends IronBarsBlock {
    public static final BooleanProperty ROS_NORTH = BooleanProperty.create((String)"ros_n");
    public static final BooleanProperty ROS_EAST = BooleanProperty.create((String)"ros_e");
    public static final BooleanProperty ROS_SOUTH = BooleanProperty.create((String)"ros_s");
    public static final BooleanProperty ROS_WEST = BooleanProperty.create((String)"ros_w");
    public static final BooleanProperty ROS_UP = BooleanProperty.create((String)"ros_u");
    public static final BooleanProperty ROS_DOWN = BooleanProperty.create((String)"ros_d");

    public NormanRosetteBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState((BlockState)((BlockState)((BlockState)((BlockState)((BlockState)((BlockState)((BlockState)((BlockState)((BlockState)((BlockState)((BlockState)((BlockState)this.stateDefinition.any()).setValue((Property)NORTH, (Comparable)Boolean.valueOf(false))).setValue((Property)EAST, (Comparable)Boolean.valueOf(false))).setValue((Property)SOUTH, (Comparable)Boolean.valueOf(false))).setValue((Property)WEST, (Comparable)Boolean.valueOf(false))).setValue((Property)WATERLOGGED, (Comparable)Boolean.valueOf(false))).setValue((Property)ROS_NORTH, (Comparable)Boolean.valueOf(false))).setValue((Property)ROS_EAST, (Comparable)Boolean.valueOf(false))).setValue((Property)ROS_SOUTH, (Comparable)Boolean.valueOf(false))).setValue((Property)ROS_WEST, (Comparable)Boolean.valueOf(false))).setValue((Property)ROS_UP, (Comparable)Boolean.valueOf(false))).setValue((Property)ROS_DOWN, (Comparable)Boolean.valueOf(false)));
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(new Property[]{ROS_NORTH, ROS_EAST, ROS_SOUTH, ROS_WEST, ROS_UP, ROS_DOWN});
    }

    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        if (state == null) {
            return null;
        }
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        return this.withRosetteProperties(state, (BlockGetter)level, pos);
    }

    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        BlockState updated = super.updateShape(state, direction, neighborState, level, pos, neighborPos);
        return this.withRosetteProperties(updated, (BlockGetter)level, pos);
    }

    private boolean hasRosette(BlockGetter level, BlockPos pos, Direction direction) {
        return level.getBlockState(pos.relative(direction)).getBlock() == this;
    }

    private BlockState withRosetteProperties(BlockState state, BlockGetter level, BlockPos pos) {
        return (BlockState)((BlockState)((BlockState)((BlockState)((BlockState)((BlockState)state.setValue((Property)ROS_NORTH, (Comparable)Boolean.valueOf(this.hasRosette(level, pos, Direction.NORTH)))).setValue((Property)ROS_EAST, (Comparable)Boolean.valueOf(this.hasRosette(level, pos, Direction.EAST)))).setValue((Property)ROS_SOUTH, (Comparable)Boolean.valueOf(this.hasRosette(level, pos, Direction.SOUTH)))).setValue((Property)ROS_WEST, (Comparable)Boolean.valueOf(this.hasRosette(level, pos, Direction.WEST)))).setValue((Property)ROS_UP, (Comparable)Boolean.valueOf(this.hasRosette(level, pos, Direction.UP)))).setValue((Property)ROS_DOWN, (Comparable)Boolean.valueOf(this.hasRosette(level, pos, Direction.DOWN)));
    }
}

