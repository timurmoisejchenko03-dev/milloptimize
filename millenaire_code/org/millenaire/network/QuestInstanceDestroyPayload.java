/*
 * Decompiled with CFR 0.152.
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

public record QuestInstanceDestroyPayload(long uniqueId) implements CustomPacketPayload
{
    public static final CustomPacketPayload.Type<QuestInstanceDestroyPayload> TYPE = new CustomPacketPayload.Type(ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)"quest_destroy"));
    public static final StreamCodec<ByteBuf, QuestInstanceDestroyPayload> STREAM_CODEC = StreamCodec.of(QuestInstanceDestroyPayload::encode, QuestInstanceDestroyPayload::decode);

    private static void encode(ByteBuf buf, QuestInstanceDestroyPayload payload) {
        ByteBufCodecs.VAR_LONG.encode((Object)buf, (Object)payload.uniqueId);
    }

    private static QuestInstanceDestroyPayload decode(ByteBuf buf) {
        return new QuestInstanceDestroyPayload((Long)ByteBufCodecs.VAR_LONG.decode((Object)buf));
    }

    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

