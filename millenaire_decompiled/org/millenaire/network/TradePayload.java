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

public record TradePayload(int entityId, String displayName, String roleName, String villageName, int reputation, String reputationLabel) {
    public static final CustomPacketPayload.Type<TradePayload> TYPE = new CustomPacketPayload.Type(ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)"trade"));
    public static final StreamCodec<ByteBuf, TradePayload> STREAM_CODEC = StreamCodec.of(TradePayload::encode, TradePayload::decode);

    private static void encode(ByteBuf buf, TradePayload payload) {
        ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)payload.entityId);
        ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)payload.displayName);
        ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)payload.roleName);
        ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)payload.villageName);
        ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)payload.reputation);
        ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)payload.reputationLabel);
    }

    private static TradePayload decode(ByteBuf buf) {
        return new TradePayload((Integer)ByteBufCodecs.VAR_INT.decode((Object)buf), (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf), (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf), (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf), (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf), (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf));
    }

    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

