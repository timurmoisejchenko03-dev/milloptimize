/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.world.item.context.BlockPlaceContext
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.ChestBlock
 *  net.minecraft.world.level.block.HorizontalDirectionalBlock
 *  net.minecraft.world.level.block.Mirror
 *  net.minecraft.world.level.block.Rotation
 *  net.minecraft.world.level.block.state.BlockBehaviour$Properties
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.StateDefinition$Builder
 *  net.minecraft.world.level.block.state.properties.BooleanProperty
 *  net.minecraft.world.level.block.state.properties.EnumProperty
 *  net.minecraft.world.level.block.state.properties.Property
 */
package org.millenaire.block.mock;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import org.millenaire.block.LockedChestBlock;
import org.millenaire.block.ModBlocks;
import org.millenaire.block.mock.MockBlock;
import org.millenaire.block.mock.MockChestType;
import org.millenaire.building.SpecialPoint;

public class MockChestBlock
extends MockBlock {
    public static final EnumProperty<MockChestType> CHEST_TYPE = EnumProperty.create((String)"chest_type", MockChestType.class);
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty GUESS = BooleanProperty.create((String)"guess");

    public MockChestBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState((BlockState)((BlockState)((BlockState)((BlockState)this.stateDefinition.any()).setValue(CHEST_TYPE, (Comparable)((Object)MockChestType.MAIN))).setValue(FACING, (Comparable)Direction.NORTH)).setValue((Property)GUESS, (Comparable)Boolean.valueOf(false)));
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(new Property[]{CHEST_TYPE, FACING, GUESS});
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
        return CHEST_TYPE;
    }

    @Override
    protected String translationKeyPrefix() {
        return "block.millenaire.mock_chest.";
    }

    @Override
    public SpecialPoint toSpecialPoint(BlockState state, BlockPos pos) {
        MockChestType chestType = (MockChestType)((Object)state.getValue(CHEST_TYPE));
        Direction facing = (Direction)state.getValue(FACING);
        return new SpecialPoint("chest", chestType.getSerializedName(), facing.getSerializedName(), pos);
    }

    @Override
    @Nullable
    public BlockState getReplacementState(BlockState mockState) {
        MockChestType chestType = (MockChestType)((Object)mockState.getValue(CHEST_TYPE));
        Direction facing = (Direction)mockState.getValue(FACING);
        return (BlockState)((LockedChestBlock)((Object)ModBlocks.LOCKED_CHEST.get())).defaultBlockState().setValue((Property)ChestBlock.FACING, (Comparable)facing);
    }
}

