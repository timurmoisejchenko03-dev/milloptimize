/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.netty.buffer.ByteBuf
 *  net.minecraft.network.codec.StreamCodec
 *  net.minecraft.network.protocol.common.custom.CustomPacketPayload
 *  net.minecraft.network.protocol.common.custom.CustomPacketPayload$Type
 *  net.minecraft.resources.ResourceLocation
 */
package org.millenaire.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.millenaire.network.VillageChiefPayload;

public record VillageChiefUpdatePayload(VillageChiefPayload.ChiefDynamic dynamic) implements CustomPacketPayload
{
    public static final CustomPacketPayload.Type<VillageChiefUpdatePayload> TYPE = new CustomPacketPayload.Type(ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)"village_chief_update"));
    public static final StreamCodec<ByteBuf, VillageChiefUpdatePayload> STREAM_CODEC = StreamCodec.of((buf, payload) -> VillageChiefPayload.encodeDynamic(buf, payload.dynamic), buf -> new VillageChiefUpdatePayload(VillageChiefPayload.decodeDynamic(buf)));

    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

