/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.Blocks
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

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.millenaire.block.BlockSilkWorm;
import org.millenaire.block.BlockSnailSoil;
import org.millenaire.block.ModBlocks;
import org.millenaire.block.mock.MarkerType;
import org.millenaire.block.mock.MockBlock;
import org.millenaire.building.SpecialPoint;

public class MockMarkerBlock
extends MockBlock {
    public static final EnumProperty<MarkerType> TYPE = EnumProperty.create((String)"type", MarkerType.class);
    private static final VoxelShape CARPET_SHAPE = Block.box((double)0.0, (double)0.0, (double)0.0, (double)16.0, (double)1.0, (double)16.0);

    public MockMarkerBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState((BlockState)((BlockState)this.stateDefinition.any()).setValue(TYPE, (Comparable)((Object)MarkerType.SLEEPING_POS)));
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(new Property[]{TYPE});
    }

    @Override
    public SpecialPoint toSpecialPoint(BlockState state, BlockPos pos) {
        MarkerType type = (MarkerType)((Object)state.getValue(TYPE));
        return new SpecialPoint(type.specialPointType(), type.specialPointSubtype(), null, pos);
    }

    @Override
    protected Property<? extends Comparable<?>> variantProperty() {
        return TYPE;
    }

    @Override
    protected String translationKeyPrefix() {
        return "block.millenaire.mock_marker.";
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return ((MarkerType)((Object)state.getValue(TYPE))).hasSolidCollision() ? Shapes.block() : CARPET_SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return ((MarkerType)((Object)state.getValue(TYPE))).hasSolidCollision() ? Shapes.block() : Shapes.empty();
    }

    @Override
    @Nullable
    public BlockState getReplacementState(BlockState mockState) {
        MarkerType type = (MarkerType)((Object)mockState.getValue(TYPE));
        return switch (type) {
            case MarkerType.TORCH -> Blocks.TORCH.defaultBlockState();
            case MarkerType.SILKWORM_BLOCK -> ((BlockSilkWorm)((Object)ModBlocks.SILK_WORM.get())).defaultBlockState();
            case MarkerType.SNAIL_SOIL_BLOCK -> ((BlockSnailSoil)((Object)ModBlocks.SNAIL_SOIL.get())).defaultBlockState();
            default -> null;
        };
    }
}

