/*
 * Decompiled with CFR 0.152.
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
import org.millenaire.block.mock.MockBlock;
import org.millenaire.block.mock.SourceType;
import org.millenaire.building.SpecialPoint;

public class MockSourceBlock
extends MockBlock {
    public static final EnumProperty<SourceType> MATERIAL = EnumProperty.create((String)"material", SourceType.class);

    public MockSourceBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState((BlockState)((BlockState)this.stateDefinition.any()).setValue(MATERIAL, (Comparable)((Object)SourceType.STONE)));
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(new Property[]{MATERIAL});
    }

    @Override
    protected Property<? extends Comparable<?>> variantProperty() {
        return MATERIAL;
    }

    @Override
    protected String translationKeyPrefix() {
        return "block.millenaire.mock_source.";
    }

    @Override
    public SpecialPoint toSpecialPoint(BlockState state, BlockPos pos) {
        SourceType material = (SourceType)((Object)state.getValue(MATERIAL));
        return new SpecialPoint("source", material.getSerializedName(), null, pos);
    }

    @Override
    @Nullable
    public BlockState getReplacementState(BlockState mockState) {
        SourceType material = (SourceType)((Object)mockState.getValue(MATERIAL));
        return switch (material) {
            default -> throw new MatchException(null, null);
            case SourceType.STONE -> Blocks.STONE.defaultBlockState();
            case SourceType.SAND -> Blocks.SAND.defaultBlockState();
            case SourceType.SANDSTONE -> Blocks.SANDSTONE.defaultBlockState();
            case SourceType.CLAY -> Blocks.CLAY.defaultBlockState();
            case SourceType.GRAVEL -> Blocks.GRAVEL.defaultBlockState();
            case SourceType.GRANITE -> Blocks.GRANITE.defaultBlockState();
            case SourceType.DIORITE -> Blocks.DIORITE.defaultBlockState();
            case SourceType.ANDESITE -> Blocks.ANDESITE.defaultBlockState();
            case SourceType.SNOW -> Blocks.SNOW_BLOCK.defaultBlockState();
            case SourceType.ICE -> Blocks.ICE.defaultBlockState();
            case SourceType.RED_SANDSTONE -> Blocks.RED_SANDSTONE.defaultBlockState();
            case SourceType.QUARTZ -> Blocks.QUARTZ_BLOCK.defaultBlockState();
        };
    }
}

