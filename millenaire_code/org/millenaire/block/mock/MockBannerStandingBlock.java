/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.level.block.BannerBlock
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraft.world.level.block.Mirror
 *  net.minecraft.world.level.block.Rotation
 *  net.minecraft.world.level.block.state.BlockBehaviour$Properties
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.StateDefinition$Builder
 *  net.minecraft.world.level.block.state.properties.EnumProperty
 *  net.minecraft.world.level.block.state.properties.IntegerProperty
 *  net.minecraft.world.level.block.state.properties.Property
 *  net.minecraft.world.phys.shapes.CollisionContext
 *  net.minecraft.world.phys.shapes.VoxelShape
 */
package org.millenaire.block.mock;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BannerBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.millenaire.block.mock.BannerSubtype;
import org.millenaire.block.mock.MockBlock;
import org.millenaire.building.SpecialPoint;

public class MockBannerStandingBlock
extends MockBlock {
    public static final EnumProperty<BannerSubtype> SUBTYPE = EnumProperty.create((String)"subtype", BannerSubtype.class);
    public static final IntegerProperty ROTATION = BannerBlock.ROTATION;
    private static final VoxelShape SHAPE_NS = Block.box((double)3.0, (double)0.0, (double)7.0, (double)13.0, (double)16.0, (double)9.0);
    private static final VoxelShape SHAPE_EW = Block.box((double)7.0, (double)0.0, (double)3.0, (double)9.0, (double)16.0, (double)13.0);

    public MockBannerStandingBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState((BlockState)((BlockState)((BlockState)this.stateDefinition.any()).setValue(SUBTYPE, (Comparable)((Object)BannerSubtype.VILLAGE))).setValue((Property)ROTATION, (Comparable)Integer.valueOf(0)));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int bucket = ((Integer)state.getValue((Property)ROTATION) + 2) / 4 % 4;
        return bucket % 2 == 0 ? SHAPE_NS : SHAPE_EW;
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(new Property[]{SUBTYPE, ROTATION});
    }

    public BlockState rotate(BlockState state, Rotation rotation) {
        return (BlockState)state.setValue((Property)ROTATION, (Comparable)Integer.valueOf(rotation.rotate(((Integer)state.getValue((Property)ROTATION)).intValue(), 16)));
    }

    public BlockState mirror(BlockState state, Mirror mirror) {
        return (BlockState)state.setValue((Property)ROTATION, (Comparable)Integer.valueOf(mirror.mirror(((Integer)state.getValue((Property)ROTATION)).intValue(), 16)));
    }

    @Override
    protected Property<? extends Comparable<?>> variantProperty() {
        return SUBTYPE;
    }

    @Override
    protected String translationKeyPrefix() {
        return "block.millenaire.mock_banner_standing.";
    }

    @Override
    public SpecialPoint toSpecialPoint(BlockState state, BlockPos pos) {
        BannerSubtype subtype = (BannerSubtype)((Object)state.getValue(SUBTYPE));
        int rotation = (Integer)state.getValue((Property)ROTATION);
        String placement = "standing_" + rotation;
        return new SpecialPoint("banner", subtype.specialPointSubtype(), String.valueOf(rotation), placement, pos);
    }

    @Override
    @Nullable
    public BlockState getReplacementState(BlockState mockState) {
        int rotation = (Integer)mockState.getValue((Property)ROTATION);
        return (BlockState)Blocks.WHITE_BANNER.defaultBlockState().setValue((Property)BannerBlock.ROTATION, (Comparable)Integer.valueOf(rotation));
    }
}

