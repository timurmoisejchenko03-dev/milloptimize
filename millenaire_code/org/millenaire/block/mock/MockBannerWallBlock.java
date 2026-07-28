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
 *  net.minecraft.world.level.block.HorizontalDirectionalBlock
 *  net.minecraft.world.level.block.Mirror
 *  net.minecraft.world.level.block.Rotation
 *  net.minecraft.world.level.block.WallBannerBlock
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
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.WallBannerBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.millenaire.block.mock.BannerSubtype;
import org.millenaire.block.mock.MockBlock;
import org.millenaire.building.SpecialPoint;

public class MockBannerWallBlock
extends MockBlock {
    public static final EnumProperty<BannerSubtype> SUBTYPE = EnumProperty.create((String)"subtype", BannerSubtype.class);
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    private static final Map<Direction, VoxelShape> WALL_SHAPES = Map.of(Direction.NORTH, Block.box((double)0.0, (double)0.0, (double)15.0, (double)16.0, (double)16.0, (double)16.0), Direction.EAST, Block.box((double)15.0, (double)0.0, (double)0.0, (double)16.0, (double)16.0, (double)16.0), Direction.SOUTH, Block.box((double)0.0, (double)0.0, (double)0.0, (double)16.0, (double)16.0, (double)1.0), Direction.WEST, Block.box((double)0.0, (double)0.0, (double)0.0, (double)1.0, (double)16.0, (double)16.0));

    public MockBannerWallBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState((BlockState)((BlockState)((BlockState)this.stateDefinition.any()).setValue(SUBTYPE, (Comparable)((Object)BannerSubtype.VILLAGE))).setValue(FACING, (Comparable)Direction.NORTH));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return WALL_SHAPES.getOrDefault(state.getValue(FACING), Shapes.block());
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(new Property[]{SUBTYPE, FACING});
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
        return SUBTYPE;
    }

    @Override
    protected String translationKeyPrefix() {
        return "block.millenaire.mock_banner_wall.";
    }

    @Override
    public SpecialPoint toSpecialPoint(BlockState state, BlockPos pos) {
        BannerSubtype subtype = (BannerSubtype)((Object)state.getValue(SUBTYPE));
        Direction facing = (Direction)state.getValue(FACING);
        String placement = "wall_" + facing.getSerializedName();
        return new SpecialPoint("banner", subtype.specialPointSubtype(), facing.getSerializedName(), placement, pos);
    }

    @Override
    @Nullable
    public BlockState getReplacementState(BlockState mockState) {
        Direction facing = (Direction)mockState.getValue(FACING);
        return (BlockState)Blocks.WHITE_WALL_BANNER.defaultBlockState().setValue((Property)WallBannerBlock.FACING, (Comparable)facing);
    }
}

