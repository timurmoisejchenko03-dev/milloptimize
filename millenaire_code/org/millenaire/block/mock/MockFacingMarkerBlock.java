/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.world.item.context.BlockPlaceContext
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraft.world.level.block.FurnaceBlock
 *  net.minecraft.world.level.block.HorizontalDirectionalBlock
 *  net.minecraft.world.level.block.Mirror
 *  net.minecraft.world.level.block.Rotation
 *  net.minecraft.world.level.block.state.BlockBehaviour$Properties
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.StateDefinition$Builder
 *  net.minecraft.world.level.block.state.properties.BooleanProperty
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
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FurnaceBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.millenaire.block.mock.FacingMarkerType;
import org.millenaire.block.mock.MockBlock;
import org.millenaire.building.SpecialPoint;

public class MockFacingMarkerBlock
extends MockBlock {
    public static final EnumProperty<FacingMarkerType> TYPE = EnumProperty.create((String)"type", FacingMarkerType.class);
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty GUESS = BooleanProperty.create((String)"guess");
    private static final Map<Direction, VoxelShape> SIGN_SHAPES = Map.of(Direction.NORTH, Block.box((double)0.0, (double)0.0, (double)15.0, (double)16.0, (double)16.0, (double)16.0), Direction.EAST, Block.box((double)15.0, (double)0.0, (double)0.0, (double)16.0, (double)16.0, (double)16.0), Direction.SOUTH, Block.box((double)0.0, (double)0.0, (double)0.0, (double)16.0, (double)16.0, (double)1.0), Direction.WEST, Block.box((double)0.0, (double)0.0, (double)0.0, (double)1.0, (double)16.0, (double)16.0));

    public MockFacingMarkerBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState((BlockState)((BlockState)((BlockState)((BlockState)this.stateDefinition.any()).setValue(TYPE, (Comparable)((Object)FacingMarkerType.FURNACE))).setValue(FACING, (Comparable)Direction.NORTH)).setValue((Property)GUESS, (Comparable)Boolean.valueOf(false)));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch ((FacingMarkerType)((Object)state.getValue(TYPE))) {
            default -> throw new MatchException(null, null);
            case FacingMarkerType.FURNACE -> Shapes.block();
            case FacingMarkerType.SIGN_POS -> SIGN_SHAPES.getOrDefault(state.getValue(FACING), Shapes.block());
        };
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(new Property[]{TYPE, FACING, GUESS});
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
        return TYPE;
    }

    @Override
    protected String translationKeyPrefix() {
        return "block.millenaire.mock_facing_marker.";
    }

    @Override
    public SpecialPoint toSpecialPoint(BlockState state, BlockPos pos) {
        FacingMarkerType type = (FacingMarkerType)((Object)state.getValue(TYPE));
        Direction facing = (Direction)state.getValue(FACING);
        boolean guess = (Boolean)state.getValue((Property)GUESS);
        String orientation = guess ? "guess" : facing.getSerializedName();
        return new SpecialPoint(type.specialPointType(), null, orientation, pos);
    }

    @Override
    @Nullable
    public BlockState getReplacementState(BlockState mockState) {
        FacingMarkerType type = (FacingMarkerType)((Object)mockState.getValue(TYPE));
        Direction facing = (Direction)mockState.getValue(FACING);
        return switch (type) {
            default -> throw new MatchException(null, null);
            case FacingMarkerType.FURNACE -> (BlockState)Blocks.FURNACE.defaultBlockState().setValue((Property)FurnaceBlock.FACING, (Comparable)facing);
            case FacingMarkerType.SIGN_POS -> null;
        };
    }
}

