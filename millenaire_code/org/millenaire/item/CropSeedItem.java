/*
 * Decompiled with CFR 0.152.
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
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.Item$Properties
 *  net.minecraft.world.item.context.UseOnContext
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.LevelReader
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.state.BlockState
 */
package org.millenaire.item;

import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.millenaire.village.PlayerCultureReputation;

public class CropSeedItem
extends Item {
    private final Supplier<? extends Block> cropBlock;
    private final String cropKey;

    public CropSeedItem(Supplier<? extends Block> cropBlock, String cropKey, Item.Properties properties) {
        super(properties);
        this.cropBlock = cropBlock;
        this.cropKey = cropKey;
    }

    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos clickedPos = context.getClickedPos();
        if (context.getClickedFace() != Direction.UP) {
            return InteractionResult.PASS;
        }
        BlockPos placePos = clickedPos.above();
        BlockState cropState = this.cropBlock.get().defaultBlockState();
        if (!cropState.canSurvive((LevelReader)level, placePos)) {
            return InteractionResult.PASS;
        }
        if (!level.getBlockState(placePos).canBeReplaced()) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            ServerPlayer player = (ServerPlayer)context.getPlayer();
            if (player == null) {
                return InteractionResult.PASS;
            }
            ServerLevel serverLevel = (ServerLevel)level;
            PlayerCultureReputation cultureRep = PlayerCultureReputation.get(serverLevel);
            if (!cultureRep.hasLearnedCrop(player.getUUID(), this.cropKey)) {
                player.sendSystemMessage((Component)Component.translatable((String)"message.millenaire.crop_planting_knowledge"));
                return InteractionResult.FAIL;
            }
            level.setBlock(placePos, cropState, 3);
            if (!player.getAbilities().instabuild) {
                context.getItemInHand().shrink(1);
            }
            level.playSound(null, placePos, SoundEvents.CROP_PLANTED, SoundSource.BLOCKS, 1.0f, 1.0f);
        }
        return InteractionResult.sidedSuccess((boolean)level.isClientSide);
    }
}

