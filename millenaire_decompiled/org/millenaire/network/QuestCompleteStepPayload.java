/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  io.netty.buffer.ByteBuf
 *  net.minecraft.network.codec.ByteBufCodecs
 *  net.minecraft.network.codec.StreamCodec
 *  net.minecraft.network.protocol.common.custom.CustomPacketPayload
 *  net.minecraft.network.protocol.common.custom.CustomPacketPayload$Type
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.level.Level
 *  net.neoforged.neoforge.network.handling.IPayloadContext
 *  org.slf4j.Logger
 */
package org.millenaire.network;

import com.mojang.logging.LogUtils;
import io.netty.buffer.ByteBuf;
import java.util.UUID;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.millenaire.entity.MillVillager;
import org.millenaire.quest.QuestInstance;
import org.millenaire.quest.QuestRegistry;
import org.millenaire.village.PlayerQuestData;
import org.slf4j.Logger;

public record QuestCompleteStepPayload(long questUniqueId, String villagerId) {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final CustomPacketPayload.Type<QuestCompleteStepPayload> TYPE = new CustomPacketPayload.Type(ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)"quest_complete_step"));
    public static final StreamCodec<ByteBuf, QuestCompleteStepPayload> STREAM_CODEC = StreamCodec.of(QuestCompleteStepPayload::encode, QuestCompleteStepPayload::decode);
    private static final int MAX_ID_LENGTH = 64;

    private static void encode(ByteBuf buf, QuestCompleteStepPayload payload) {
        ByteBufCodecs.VAR_LONG.encode((Object)buf, (Object)payload.questUniqueId);
        ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)payload.villagerId);
    }

    private static QuestCompleteStepPayload decode(ByteBuf buf) {
        long questUniqueId = (Long)ByteBufCodecs.VAR_LONG.decode((Object)buf);
        String villagerId = (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf);
        if (villagerId.length() > 64) {
            villagerId = villagerId.substring(0, 64);
        }
        return new QuestCompleteStepPayload(questUniqueId, villagerId);
    }

    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(QuestCompleteStepPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            UUID villagerUuid;
            Player patt0$temp = context.player();
            if (!(patt0$temp instanceof ServerPlayer)) {
                return;
            }
            ServerPlayer player = (ServerPlayer)patt0$temp;
            Level patt1$temp = player.level();
            if (!(patt1$temp instanceof ServerLevel)) {
                return;
            }
            ServerLevel serverLevel = (ServerLevel)patt1$temp;
            ServerLevel overworld = serverLevel.getServer().getLevel(Level.OVERWORLD);
            if (overworld == null) {
                return;
            }
            PlayerQuestData data = PlayerQuestData.get(overworld, QuestRegistry::get);
            UUID playerId = player.getUUID();
            QuestInstance quest = null;
            for (QuestInstance qi : data.getActiveQuests(playerId)) {
                if (qi.getUniqueId() != payload.questUniqueId) continue;
                quest = qi;
                break;
            }
            if (quest == null) {
                LOGGER.debug("QuestCompleteStep: quest {} not found for player {}", (Object)payload.questUniqueId, (Object)player.getName().getString());
                return;
            }
            try {
                villagerUuid = UUID.fromString(payload.villagerId);
            }
            catch (IllegalArgumentException e) {
                return;
            }
            Entity entity = serverLevel.getEntity(villagerUuid);
            if (!(entity instanceof MillVillager)) {
                LOGGER.debug("QuestCompleteStep: villager entity {} not found or not MillVillager", (Object)payload.villagerId);
                return;
            }
            MillVillager villager = (MillVillager)entity;
            quest.completeStep(player, villager);
        });
    }
}

