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

public record QuestOpenPayload(int villagerEntityId, long questUniqueId, String villagerDisplayName, String villagerNativeOccupation, String villagerGameOccupation, String descriptionText, String conditionText, boolean conditionsMet, boolean isFirstStep, int currentStepIndex, String cultureKey, String villagerTypeKey) {
    public static final CustomPacketPayload.Type<QuestOpenPayload> TYPE = new CustomPacketPayload.Type(ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)"quest_open"));
    public static final StreamCodec<ByteBuf, QuestOpenPayload> STREAM_CODEC = StreamCodec.of(QuestOpenPayload::encode, QuestOpenPayload::decode);

    private static void encode(ByteBuf buf, QuestOpenPayload payload) {
        ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)payload.villagerEntityId);
        ByteBufCodecs.VAR_LONG.encode((Object)buf, (Object)payload.questUniqueId);
        ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)payload.villagerDisplayName);
        ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)payload.villagerNativeOccupation);
        ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)payload.villagerGameOccupation);
        ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)payload.descriptionText);
        ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)payload.conditionText);
        ByteBufCodecs.BOOL.encode((Object)buf, (Object)payload.conditionsMet);
        ByteBufCodecs.BOOL.encode((Object)buf, (Object)payload.isFirstStep);
        ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)payload.currentStepIndex);
        ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)payload.cultureKey);
        ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)payload.villagerTypeKey);
    }

    private static QuestOpenPayload decode(ByteBuf buf) {
        return new QuestOpenPayload((Integer)ByteBufCodecs.VAR_INT.decode((Object)buf), (Long)ByteBufCodecs.VAR_LONG.decode((Object)buf), (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf), (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf), (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf), (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf), (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf), (Boolean)ByteBufCodecs.BOOL.decode((Object)buf), (Boolean)ByteBufCodecs.BOOL.decode((Object)buf), (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf), (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf), (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf));
    }

    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

