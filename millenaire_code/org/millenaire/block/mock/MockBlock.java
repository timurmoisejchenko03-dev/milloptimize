/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.component.DataComponents
 *  net.minecraft.network.chat.Component
 *  net.minecraft.util.StringRepresentable
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.component.BlockItemStateProperties
 *  net.minecraft.world.item.component.CustomModelData
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.level.ItemLike
 *  net.minecraft.world.level.LevelReader
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.state.BlockBehaviour$Properties
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.properties.Property
 *  net.minecraft.world.phys.shapes.CollisionContext
 *  net.minecraft.world.phys.shapes.Shapes
 *  net.minecraft.world.phys.shapes.VoxelShape
 */
package org.millenaire.block.mock;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.millenaire.building.SpecialPoint;

public abstract class MockBlock
extends Block {
    protected MockBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    public abstract SpecialPoint toSpecialPoint(BlockState var1, BlockPos var2);

    @Nullable
    public abstract BlockState getReplacementState(BlockState var1);

    protected abstract Property<? extends Comparable<?>> variantProperty();

    protected abstract String translationKeyPrefix();

    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        ItemStack stack = new ItemStack((ItemLike)this);
        stack.set(DataComponents.BLOCK_STATE, (Object)MockBlock.withVariant(this.variantProperty(), state));
        Comparable value = state.getValue(this.variantProperty());
        stack.set(DataComponents.ITEM_NAME, (Object)Component.translatable((String)(this.translationKeyPrefix() + ((StringRepresentable)value).getSerializedName())));
        if (value instanceof Enum) {
            Enum enumValue = (Enum)((Object)value);
            stack.set(DataComponents.CUSTOM_MODEL_DATA, (Object)new CustomModelData(enumValue.ordinal() + 1));
        }
        return stack;
    }

    private static <T extends Comparable<T>> BlockItemStateProperties withVariant(Property<?> property, BlockState state) {
        Property<?> typedProp = property;
        return BlockItemStateProperties.EMPTY.with(typedProp, state.getValue(typedProp));
    }

    public static BlockBehaviour.Properties mockProperties() {
        return BlockBehaviour.Properties.of().strength(-1.0f, 3600000.0f).noOcclusion().noCollission().forceSolidOn().noLootTable();
    }

    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }
}

