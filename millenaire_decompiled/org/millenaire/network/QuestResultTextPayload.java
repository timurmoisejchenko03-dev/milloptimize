/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  io.netty.buffer.ByteBuf
 *  net.minecraft.network.codec.ByteBufCodecs
 *  net.minecraft.network.codec.StreamCodec
 *  net.minecraft.network.protocol.common.custom.CustomPacketPayload
 *  net.minecraft.network.protocol.common.custom.CustomPacketPayload$Type
 *  net.minecraft.resources.ResourceLocation
 */
package org.millenaire.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record QuestResultTextPayload(long questUniqueId, String resultText, boolean isSuccess) {
    public static final CustomPacketPayload.Type<QuestResultTextPayload> TYPE = new CustomPacketPayload.Type(ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)"quest_result"));
    public static final StreamCodec<ByteBuf, QuestResultTextPayload> STREAM_CODEC = StreamCodec.of(QuestResultTextPayload::encode, QuestResultTextPayload::decode);

    private static void encode(ByteBuf buf, QuestResultTextPayload payload) {
        ByteBufCodecs.VAR_LONG.encode((Object)buf, (Object)payload.questUniqueId);
        ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)payload.resultText);
        ByteBufCodecs.BOOL.encode((Object)buf, (Object)payload.isSuccess);
    }

    private static QuestResultTextPayload decode(ByteBuf buf) {
        return new QuestResultTextPayload((Long)ByteBufCodecs.VAR_LONG.decode((Object)buf), (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf), (Boolean)ByteBufCodecs.BOOL.decode((Object)buf));
    }

    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

