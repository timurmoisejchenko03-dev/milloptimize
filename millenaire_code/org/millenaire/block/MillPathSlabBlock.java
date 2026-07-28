/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.world.entity.Mob
 *  net.minecraft.world.item.context.BlockPlaceContext
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.SlabBlock
 *  net.minecraft.world.level.block.state.BlockBehaviour$Properties
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.StateDefinition$Builder
 *  net.minecraft.world.level.block.state.properties.BooleanProperty
 *  net.minecraft.world.level.block.state.properties.Property
 *  net.minecraft.world.level.block.state.properties.SlabType
 *  net.minecraft.world.level.pathfinder.PathType
 *  net.minecraft.world.phys.shapes.CollisionContext
 *  net.minecraft.world.phys.shapes.VoxelShape
 *  org.jetbrains.annotations.Nullable
 */
package org.millenaire.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.millenaire.block.PathTier;

public class MillPathSlabBlock
extends SlabBlock {
    public static final BooleanProperty STABLE = BooleanProperty.create((String)"stable");
    private static final VoxelShape BOTTOM_SHAPE = Block.box((double)0.0, (double)0.0, (double)0.0, (double)16.0, (double)7.0, (double)16.0);
    private static final VoxelShape TOP_SHAPE = Block.box((double)0.0, (double)8.0, (double)0.0, (double)16.0, (double)15.0, (double)16.0);
    private static final VoxelShape DOUBLE_SHAPE = Block.box((double)0.0, (double)0.0, (double)0.0, (double)16.0, (double)15.0, (double)16.0);
    private final PathTier tier;

    public MillPathSlabBlock(BlockBehaviour.Properties properties, PathTier tier) {
        super(properties.speedFactor(tier.speedFactor()));
        this.tier = tier;
        this.registerDefaultState((BlockState)((BlockState)((BlockState)((BlockState)this.stateDefinition.any()).setValue((Property)TYPE, (Comparable)SlabType.BOTTOM)).setValue((Property)WATERLOGGED, (Comparable)Boolean.valueOf(false))).setValue((Property)STABLE, (Comparable)Boolean.valueOf(false)));
    }

    public PathTier tier() {
        return this.tier;
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(new Property[]{STABLE});
    }

    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        return state != null ? (BlockState)state.setValue((Property)STABLE, (Comparable)Boolean.valueOf(true)) : null;
    }

    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        SlabType type = (SlabType)state.getValue((Property)TYPE);
        return switch (type) {
            case SlabType.TOP -> TOP_SHAPE;
            case SlabType.DOUBLE -> DOUBLE_SHAPE;
            default -> BOTTOM_SHAPE;
        };
    }

    public boolean useShapeForLightOcclusion(BlockState state) {
        return true;
    }

    @Nullable
    public PathType getBlockPathType(BlockState state, BlockGetter level, BlockPos pos, @Nullable Mob mob) {
        return PathType.WALKABLE;
    }
}

