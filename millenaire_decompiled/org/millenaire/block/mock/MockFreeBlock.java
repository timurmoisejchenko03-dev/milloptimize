/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 *  net.minecraft.world.item.DyeColor
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
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import org.millenaire.block.ModBlocks;
import org.millenaire.block.mock.FreeBlockType;
import org.millenaire.block.mock.MockBlock;
import org.millenaire.building.SpecialPoint;

public class MockFreeBlock
extends MockBlock {
    public static final EnumProperty<FreeBlockType> MATERIAL = EnumProperty.create((String)"material", FreeBlockType.class);

    public MockFreeBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState((BlockState)((BlockState)this.stateDefinition.any()).setValue(MATERIAL, (Comparable)((Object)FreeBlockType.STONE)));
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
        return "block.millenaire.mock_free.";
    }

    @Override
    public SpecialPoint toSpecialPoint(BlockState state, BlockPos pos) {
        FreeBlockType material = (FreeBlockType)((Object)state.getValue(MATERIAL));
        return new SpecialPoint("freeBlock", material.getSerializedName(), null, pos);
    }

    @Override
    @Nullable
    public BlockState getReplacementState(BlockState mockState) {
        FreeBlockType material = (FreeBlockType)((Object)mockState.getValue(MATERIAL));
        return switch (material) {
            default -> throw new MatchException(null, null);
            case FreeBlockType.STONE -> Blocks.STONE.defaultBlockState();
            case FreeBlockType.SAND -> Blocks.SAND.defaultBlockState();
            case FreeBlockType.GRAVEL -> Blocks.GRAVEL.defaultBlockState();
            case FreeBlockType.SANDSTONE -> Blocks.SANDSTONE.defaultBlockState();
            case FreeBlockType.WOOL -> Blocks.WHITE_WOOL.defaultBlockState();
            case FreeBlockType.COBBLESTONE -> Blocks.COBBLESTONE.defaultBlockState();
            case FreeBlockType.STONE_BRICK -> Blocks.STONE_BRICKS.defaultBlockState();
            case FreeBlockType.PAINTED_BRICK -> ((Block)ModBlocks.PAINTED_BRICKS.get((Object)DyeColor.WHITE).get()).defaultBlockState();
            case FreeBlockType.GRASS_BLOCK -> Blocks.GRASS_BLOCK.defaultBlockState();
        };
    }
}

