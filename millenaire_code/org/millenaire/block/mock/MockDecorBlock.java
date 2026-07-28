/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.util.StringRepresentable
 *  net.minecraft.world.item.context.BlockPlaceContext
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.HorizontalDirectionalBlock
 *  net.minecraft.world.level.block.Mirror
 *  net.minecraft.world.level.block.Rotation
 *  net.minecraft.world.level.block.state.BlockBehaviour$Properties
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.StateDefinition$Builder
 *  net.minecraft.world.level.block.state.properties.EnumProperty
 *  net.minecraft.world.level.block.state.properties.Property
 *  net.minecraft.world.phys.shapes.CollisionContext
 *  net.minecraft.world.phys.shapes.Shapes
 *  net.minecraft.world.phys.shapes.VoxelShape
 */
package org.millenaire.block.mock;

import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.millenaire.block.mock.MockBlock;
import org.millenaire.building.SpecialPoint;

public class MockDecorBlock
extends MockBlock {
    public static final EnumProperty<DecorType> DECOR_TYPE = EnumProperty.create((String)"decor_type", DecorType.class);
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    private static final Map<Direction, VoxelShape> WALL_SHAPES = Map.of(Direction.NORTH, Block.box((double)0.0, (double)0.0, (double)15.0, (double)16.0, (double)16.0, (double)16.0), Direction.EAST, Block.box((double)15.0, (double)0.0, (double)0.0, (double)16.0, (double)16.0, (double)16.0), Direction.SOUTH, Block.box((double)0.0, (double)0.0, (double)0.0, (double)16.0, (double)16.0, (double)1.0), Direction.WEST, Block.box((double)0.0, (double)0.0, (double)0.0, (double)1.0, (double)16.0, (double)16.0));

    public MockDecorBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState((BlockState)((BlockState)((BlockState)this.stateDefinition.any()).setValue(DECOR_TYPE, (Comparable)((Object)DecorType.TAPESTRY))).setValue(FACING, (Comparable)Direction.NORTH));
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(new Property[]{DECOR_TYPE, FACING});
    }

    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return (BlockState)this.defaultBlockState().setValue(FACING, (Comparable)context.getHorizontalDirection().getOpposite());
    }

    public BlockState rotate(BlockState state, Rotation rotation) {
        return (BlockState)state.setValue(FACING, (Comparable)rotation.rotate((Direction)state.getValue(FACING)));
    }

    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation((Direction)state.getValue(FACING)));
    }

    @Override
    protected Property<? extends Comparable<?>> variantProperty() {
        return DECOR_TYPE;
    }

    @Override
    protected String translationKeyPrefix() {
        return "block.millenaire.mock_decor.";
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return WALL_SHAPES.getOrDefault(state.getValue(FACING), Shapes.block());
    }

    @Override
    public SpecialPoint toSpecialPoint(BlockState state, BlockPos pos) {
        DecorType type = (DecorType)((Object)state.getValue(DECOR_TYPE));
        return new SpecialPoint("wall_decoration", type.getSerializedName(), null, pos);
    }

    @Override
    @Nullable
    public BlockState getReplacementState(BlockState mockState) {
        return null;
    }

    public static enum DecorType implements StringRepresentable
    {
        TAPESTRY("tapestry"),
        INDIAN_STATUE("indian_statue"),
        MAYAN_STATUE("mayan_statue"),
        BYZANTINE_ICON_SMALL("byzantine_icon_small"),
        BYZANTINE_ICON_MEDIUM("byzantine_icon_medium"),
        BYZANTINE_ICON_LARGE("byzantine_icon_large"),
        HIDE_HANGING("hide_hanging"),
        WALL_CARPET_SMALL("wall_carpet_small"),
        WALL_CARPET_MEDIUM("wall_carpet_medium"),
        WALL_CARPET_LARGE("wall_carpet_large");

        private final String name;

        private DecorType(String name) {
            this.name = name;
        }

        public String getSerializedName() {
            return this.name;
        }
    }
}

