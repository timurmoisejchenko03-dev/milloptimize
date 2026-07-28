/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.network.chat.Component
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.sounds.SoundEvents
 *  net.minecraft.sounds.SoundSource
 *  net.minecraft.world.InteractionResult
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.Item$Properties
 *  net.minecraft.world.item.context.UseOnContext
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.properties.Property
 */
package org.millenaire.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.millenaire.block.ModBlocks;
import org.millenaire.block.RicePaddyBlock;
import org.millenaire.village.PlayerCultureReputation;

public class RiceSeedItem
extends Item {
    public RiceSeedItem(Item.Properties properties) {
        super(properties);
    }

    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos clickedPos = context.getClickedPos();
        if (context.getClickedFace() != Direction.UP) {
            return InteractionResult.PASS;
        }
        BlockPos waterPos = clickedPos.above();
        BlockState waterState = level.getBlockState(waterPos);
        if (!waterState.is(Blocks.WATER) || !level.getFluidState(waterPos).isSource()) {
            Player player;
            if (!level.isClientSide && (player = context.getPlayer()) instanceof ServerPlayer) {
                ServerPlayer sp = (ServerPlayer)player;
                sp.sendSystemMessage((Component)Component.translatable((String)"message.millenaire.rice_needs_water"));
            }
            return InteractionResult.FAIL;
        }
        BlockState groundState = level.getBlockState(clickedPos);
        if (!groundState.isFaceSturdy((BlockGetter)level, clickedPos, Direction.UP)) {
            Player player;
            if (!level.isClientSide && (player = context.getPlayer()) instanceof ServerPlayer) {
                ServerPlayer sp = (ServerPlayer)player;
                sp.sendSystemMessage((Component)Component.translatable((String)"message.millenaire.rice_needs_water"));
            }
            return InteractionResult.FAIL;
        }
        BlockPos aboveWater = waterPos.above();
        BlockState aboveWaterState = level.getBlockState(aboveWater);
        if (!aboveWaterState.isAir()) {
            Player player;
            if (!level.isClientSide && (player = context.getPlayer()) instanceof ServerPlayer) {
                ServerPlayer sp = (ServerPlayer)player;
                sp.sendSystemMessage((Component)Component.translatable((String)"message.millenaire.rice_needs_water"));
            }
            return InteractionResult.FAIL;
        }
        if (!level.isClientSide) {
            ServerPlayer player = (ServerPlayer)context.getPlayer();
            if (player == null) {
                return InteractionResult.PASS;
            }
            ServerLevel serverLevel = (ServerLevel)level;
            PlayerCultureReputation cultureRep = PlayerCultureReputation.get(serverLevel);
            if (!cultureRep.hasLearnedCrop(player.getUUID(), "rice")) {
                player.sendSystemMessage((Component)Component.translatable((String)"message.millenaire.crop_planting_knowledge"));
                return InteractionResult.FAIL;
            }
            BlockState paddyState = (BlockState)((BlockState)((BlockState)((Block)ModBlocks.RICE_PADDY.get()).defaultBlockState().setValue((Property)RicePaddyBlock.PLANTED, (Comparable)Boolean.valueOf(true))).setValue((Property)RicePaddyBlock.AGE, (Comparable)Integer.valueOf(0))).setValue((Property)RicePaddyBlock.WATERLOGGED, (Comparable)Boolean.valueOf(true));
            level.setBlock(waterPos, paddyState, 3);
            if (!player.getAbilities().instabuild) {
                context.getItemInHand().shrink(1);
            }
            level.playSound(null, waterPos, SoundEvents.CROP_PLANTED, SoundSource.BLOCKS, 1.0f, 1.0f);
        }
        return InteractionResult.sidedSuccess((boolean)level.isClientSide);
    }
}

