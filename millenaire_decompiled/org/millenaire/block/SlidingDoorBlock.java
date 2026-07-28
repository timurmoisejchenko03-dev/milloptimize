/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.DoorBlock
 *  net.minecraft.world.level.block.state.BlockBehaviour$Properties
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.properties.BlockSetType
 *  net.minecraft.world.level.block.state.properties.DoorHingeSide
 *  net.minecraft.world.level.block.state.properties.Property
 *  net.minecraft.world.phys.shapes.CollisionContext
 *  net.minecraft.world.phys.shapes.VoxelShape
 */
package org.millenaire.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class SlidingDoorBlock
extends DoorBlock {
    private static final VoxelShape CLOSED_NS = Block.box((double)0.0, (double)0.0, (double)7.0, (double)16.0, (double)16.0, (double)9.0);
    private static final VoxelShape CLOSED_EW = Block.box((double)7.0, (double)0.0, (double)0.0, (double)9.0, (double)16.0, (double)16.0);
    private static final VoxelShape OPEN_SOUTH = Block.box((double)7.0, (double)0.0, (double)14.0, (double)9.0, (double)16.0, (double)30.0);
    private static final VoxelShape OPEN_NORTH = Block.box((double)7.0, (double)0.0, (double)-14.0, (double)9.0, (double)16.0, (double)2.0);
    private static final VoxelShape OPEN_EAST = Block.box((double)14.0, (double)0.0, (double)7.0, (double)30.0, (double)16.0, (double)9.0);
    private static final VoxelShape OPEN_WEST = Block.box((double)-14.0, (double)0.0, (double)7.0, (double)2.0, (double)16.0, (double)9.0);

    public SlidingDoorBlock(BlockSetType type, BlockBehaviour.Properties props) {
        super(type, props);
    }

    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        if (!((Boolean)state.getValue((Property)OPEN)).booleanValue()) {
            Direction facing = (Direction)state.getValue((Property)FACING);
            return facing == Direction.NORTH || facing == Direction.SOUTH ? CLOSED_NS : CLOSED_EW;
        }
        Direction facing = (Direction)state.getValue((Property)FACING);
        boolean hingeRight = state.getValue((Property)HINGE) == DoorHingeSide.RIGHT;
        return switch (facing) {
            case Direction.EAST -> {
                if (hingeRight) {
                    yield OPEN_SOUTH;
                }
                yield OPEN_NORTH;
            }
            case Direction.SOUTH -> {
                if (hingeRight) {
                    yield OPEN_EAST;
                }
                yield OPEN_WEST;
            }
            case Direction.WEST -> {
                if (hingeRight) {
                    yield OPEN_NORTH;
                }
                yield OPEN_SOUTH;
            }
            case Direction.NORTH -> {
                if (hingeRight) {
                    yield OPEN_WEST;
                }
                yield OPEN_EAST;
            }
            default -> CLOSED_EW;
        };
    }

    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return super.getShape(state, level, pos, ctx);
    }
}

