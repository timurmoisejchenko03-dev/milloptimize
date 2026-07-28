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

public record BuildingPurchasePayload(String villageId, String planSetId) implements CustomPacketPayload
{
    public static final CustomPacketPayload.Type<BuildingPurchasePayload> TYPE = new CustomPacketPayload.Type(ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)"building_purchase"));
    public static final StreamCodec<ByteBuf, BuildingPurchasePayload> STREAM_CODEC = StreamCodec.of(BuildingPurchasePayload::encode, BuildingPurchasePayload::decode);
    private static final int MAX_ID_LENGTH = 128;

    private static void encode(ByteBuf buf, BuildingPurchasePayload payload) {
        ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)payload.villageId);
        ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)payload.planSetId);
    }

    private static BuildingPurchasePayload decode(ByteBuf buf) {
        String planSetId;
        String villageId = (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf);
        if (villageId.length() > 128) {
            villageId = villageId.substring(0, 128);
        }
        if ((planSetId = (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf)).length() > 128) {
            planSetId = planSetId.substring(0, 128);
        }
        return new BuildingPurchasePayload(villageId, planSetId);
    }

    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

