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

public record NegationWandPayload(String villageId, String villageTypeId, String villageName) {
    public static final CustomPacketPayload.Type<NegationWandPayload> TYPE = new CustomPacketPayload.Type(ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)"negation_wand"));
    public static final StreamCodec<ByteBuf, NegationWandPayload> STREAM_CODEC = StreamCodec.of(NegationWandPayload::encode, NegationWandPayload::decode);

    private static void encode(ByteBuf buf, NegationWandPayload payload) {
        ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)payload.villageId);
        ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)payload.villageTypeId);
        ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)payload.villageName);
    }

    private static NegationWandPayload decode(ByteBuf buf) {
        String villageId = (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf);
        String villageTypeId = (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf);
        String villageName = (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf);
        return new NegationWandPayload(villageId, villageTypeId, villageName);
    }

    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

