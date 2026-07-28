/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.state.BlockBehaviour$Properties
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.StateDefinition$Builder
 *  net.minecraft.world.level.block.state.properties.EnumProperty
 *  net.minecraft.world.level.block.state.properties.Property
 *  net.minecraft.world.phys.shapes.CollisionContext
 *  net.minecraft.world.phys.shapes.VoxelShape
 */
package org.millenaire.block.mock;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.millenaire.block.mock.MockBlock;
import org.millenaire.block.mock.TreeSpawnType;
import org.millenaire.building.SpecialPoint;

public class MockTreeSpawnBlock
extends MockBlock {
    public static final EnumProperty<TreeSpawnType> TREE = EnumProperty.create((String)"tree", TreeSpawnType.class);
    private static final VoxelShape CARPET_SHAPE = Block.box((double)0.0, (double)0.0, (double)0.0, (double)16.0, (double)1.0, (double)16.0);

    public MockTreeSpawnBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState((BlockState)((BlockState)this.stateDefinition.any()).setValue(TREE, (Comparable)((Object)TreeSpawnType.OAK)));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return CARPET_SHAPE;
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(new Property[]{TREE});
    }

    @Override
    protected Property<? extends Comparable<?>> variantProperty() {
        return TREE;
    }

    @Override
    protected String translationKeyPrefix() {
        return "block.millenaire.mock_tree_spawn.";
    }

    @Override
    public SpecialPoint toSpecialPoint(BlockState state, BlockPos pos) {
        TreeSpawnType tree = (TreeSpawnType)((Object)state.getValue(TREE));
        return new SpecialPoint("treeSpawn", tree.getSerializedName(), null, pos);
    }

    @Override
    @Nullable
    public BlockState getReplacementState(BlockState mockState) {
        return null;
    }
}

