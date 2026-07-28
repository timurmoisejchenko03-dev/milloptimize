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

public record VillageScrollPurchasePayload(String villageId) implements CustomPacketPayload
{
    public static final CustomPacketPayload.Type<VillageScrollPurchasePayload> TYPE = new CustomPacketPayload.Type(ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)"scroll_purchase"));
    public static final StreamCodec<ByteBuf, VillageScrollPurchasePayload> STREAM_CODEC = StreamCodec.of(VillageScrollPurchasePayload::encode, VillageScrollPurchasePayload::decode);
    private static final int MAX_VILLAGE_ID_LENGTH = 36;

    private static void encode(ByteBuf buf, VillageScrollPurchasePayload payload) {
        ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)payload.villageId);
    }

    private static VillageScrollPurchasePayload decode(ByteBuf buf) {
        String raw = (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf);
        if (raw.length() > 36) {
            raw = raw.substring(0, 36);
        }
        return new VillageScrollPurchasePayload(raw);
    }

    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

