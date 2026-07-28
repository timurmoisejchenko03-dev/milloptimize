/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraft.world.level.block.state.BlockBehaviour$Properties
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.StateDefinition$Builder
 *  net.minecraft.world.level.block.state.properties.EnumProperty
 *  net.minecraft.world.level.block.state.properties.Property
 */
package org.millenaire.block.mock;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import org.millenaire.block.ModBlocks;
import org.millenaire.block.RicePaddyBlock;
import org.millenaire.block.mock.CropType;
import org.millenaire.block.mock.MockBlock;
import org.millenaire.building.SpecialPoint;

public class MockSoilBlock
extends MockBlock {
    public static final EnumProperty<CropType> CROP = EnumProperty.create((String)"crop", CropType.class);

    public MockSoilBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState((BlockState)((BlockState)this.stateDefinition.any()).setValue(CROP, (Comparable)((Object)CropType.WHEAT)));
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(new Property[]{CROP});
    }

    @Override
    protected Property<? extends Comparable<?>> variantProperty() {
        return CROP;
    }

    @Override
    protected String translationKeyPrefix() {
        return "block.millenaire.mock_soil.";
    }

    @Override
    public SpecialPoint toSpecialPoint(BlockState state, BlockPos pos) {
        CropType crop = (CropType)((Object)state.getValue(CROP));
        return new SpecialPoint("soil", crop.getSerializedName(), null, pos);
    }

    @Override
    @Nullable
    public BlockState getReplacementState(BlockState mockState) {
        CropType crop = (CropType)((Object)mockState.getValue(CROP));
        if (crop == CropType.RICE) {
            return (BlockState)((BlockState)((Block)ModBlocks.RICE_PADDY.get()).defaultBlockState().setValue((Property)RicePaddyBlock.PLANTED, (Comparable)Boolean.valueOf(false))).setValue((Property)RicePaddyBlock.WATERLOGGED, (Comparable)Boolean.valueOf(true));
        }
        if (crop == CropType.SUGAR_CANE) {
            return Blocks.SAND.defaultBlockState();
        }
        if (crop == CropType.FLOWER) {
            return Blocks.GRASS_BLOCK.defaultBlockState();
        }
        return Blocks.DIRT.defaultBlockState();
    }
}

