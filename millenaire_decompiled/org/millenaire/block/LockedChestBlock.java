/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.MapCodec
 *  net.minecraft.core.BlockPos
 *  net.minecraft.network.chat.Component
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.Container
 *  net.minecraft.world.InteractionHand
 *  net.minecraft.world.InteractionResult
 *  net.minecraft.world.ItemInteractionResult
 *  net.minecraft.world.MenuProvider
 *  net.minecraft.world.SimpleMenuProvider
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.ChestBlock
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.level.block.entity.BlockEntityTicker
 *  net.minecraft.world.level.block.entity.BlockEntityType
 *  net.minecraft.world.level.block.entity.ChestBlockEntity
 *  net.minecraft.world.level.block.state.BlockBehaviour$Properties
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.properties.ChestType
 *  net.minecraft.world.level.block.state.properties.Property
 *  net.minecraft.world.phys.BlockHitResult
 */
package org.millenaire.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import org.millenaire.block.LockedChestBlockEntity;
import org.millenaire.block.ModBlockEntities;
import org.millenaire.commerce.LockedChestMenu;
import org.millenaire.item.SummoningWandItem;

public class LockedChestBlock
extends ChestBlock {
    public static final MapCodec<LockedChestBlock> CODEC = LockedChestBlock.simpleCodec(LockedChestBlock::new);

    public LockedChestBlock(BlockBehaviour.Properties properties) {
        super(properties, () -> (BlockEntityType)ModBlockEntities.LOCKED_CHEST.get());
    }

    public MapCodec<? extends ChestBlock> codec() {
        return CODEC;
    }

    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LockedChestBlockEntity(pos, state);
    }

    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return LockedChestBlock.createTickerHelper(type, (BlockEntityType)((BlockEntityType)ModBlockEntities.LOCKED_CHEST.get()), ChestBlockEntity::lidAnimateTick);
    }

    public BlockEntityType<? extends ChestBlockEntity> blockEntityType() {
        return (BlockEntityType)ModBlockEntities.LOCKED_CHEST.get();
    }

    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        return this.handleInteraction(state, level, pos, player, hitResult);
    }

    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (stack.getItem() instanceof SummoningWandItem) {
            return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    private InteractionResult handleInteraction(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        Container container;
        BlockPos otherPos;
        BlockEntity otherBe;
        ChestType chestType;
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof LockedChestBlockEntity)) {
            return InteractionResult.PASS;
        }
        LockedChestBlockEntity chest = (LockedChestBlockEntity)be;
        boolean locked = chest.isLockedFor(player);
        if (!locked && state.hasProperty((Property)ChestBlock.TYPE) && (chestType = (ChestType)state.getValue((Property)ChestBlock.TYPE)) != ChestType.SINGLE && (otherBe = level.getBlockEntity(otherPos = pos.relative(ChestBlock.getConnectedDirection((BlockState)state)))) instanceof LockedChestBlockEntity) {
            LockedChestBlockEntity otherChest = (LockedChestBlockEntity)otherBe;
            locked = otherChest.isLockedFor(player);
        }
        if ((container = ChestBlock.getContainer((ChestBlock)this, (BlockState)state, (Level)level, (BlockPos)pos, (boolean)true)) == null) {
            return InteractionResult.PASS;
        }
        Component title = chest.getDisplayName();
        boolean isLocked = locked;
        if (player instanceof ServerPlayer) {
            ServerPlayer serverPlayer = (ServerPlayer)player;
            serverPlayer.openMenu((MenuProvider)new SimpleMenuProvider((containerId, playerInventory, p) -> new LockedChestMenu(containerId, playerInventory, container, isLocked), title), buf -> {
                buf.writeByte(container.getContainerSize() / 9);
                buf.writeBoolean(isLocked);
            });
        }
        return InteractionResult.SUCCESS;
    }

    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            level.updateNeighbourForOutputSignal(pos, (Block)this);
            level.removeBlockEntity(pos);
        }
    }
}

