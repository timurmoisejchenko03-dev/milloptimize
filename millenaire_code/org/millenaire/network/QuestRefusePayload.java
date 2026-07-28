/*
 * Decompiled with CFR 0.152.
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.millenaire.quest.QuestInstance;
import org.millenaire.quest.QuestRegistry;
import org.millenaire.village.PlayerQuestData;
import org.slf4j.Logger;

public record QuestRefusePayload(long questUniqueId) implements CustomPacketPayload
{
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final CustomPacketPayload.Type<QuestRefusePayload> TYPE = new CustomPacketPayload.Type(ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)"quest_refuse"));
    public static final StreamCodec<ByteBuf, QuestRefusePayload> STREAM_CODEC = StreamCodec.of(QuestRefusePayload::encode, QuestRefusePayload::decode);

    private static void encode(ByteBuf buf, QuestRefusePayload payload) {
        ByteBufCodecs.VAR_LONG.encode((Object)buf, (Object)payload.questUniqueId);
    }

    private static QuestRefusePayload decode(ByteBuf buf) {
        return new QuestRefusePayload((Long)ByteBufCodecs.VAR_LONG.decode((Object)buf));
    }

    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleOnServer(QuestRefusePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
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
                LOGGER.debug("QuestRefuse: quest {} not found for player {}", (Object)payload.questUniqueId, (Object)player.getName().getString());
                return;
            }
            quest.refuseQuest(player);
        });
    }
}

