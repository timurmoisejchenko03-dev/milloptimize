/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  net.minecraft.network.chat.Component
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.InteractionResult
 *  net.minecraft.world.item.BlockItem
 *  net.minecraft.world.item.Item$Properties
 *  net.minecraft.world.item.context.UseOnContext
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.Block
 */
package org.millenaire.item;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.millenaire.village.PlayerCultureReputation;

public class LearnedSaplingItem
extends BlockItem {
    private final String cropKey;

    public LearnedSaplingItem(Block block, String cropKey, Item.Properties properties) {
        super(block, properties);
        this.cropKey = cropKey;
    }

    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
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
        return super.useOn(context);
    }
}

