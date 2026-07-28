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

public record SpeechChatPayload(String villagerName, String speechRef, String cultureKey, int languageScore, String vanillaFallbackKey) {
    public static final CustomPacketPayload.Type<SpeechChatPayload> TYPE = new CustomPacketPayload.Type(ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)"speech_chat"));
    public static final StreamCodec<ByteBuf, SpeechChatPayload> STREAM_CODEC = StreamCodec.of(SpeechChatPayload::encode, SpeechChatPayload::decode);

    private static void encode(ByteBuf buf, SpeechChatPayload payload) {
        ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)payload.villagerName);
        ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)payload.speechRef);
        ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)payload.cultureKey);
        ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)payload.languageScore);
        ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)payload.vanillaFallbackKey);
    }

    private static SpeechChatPayload decode(ByteBuf buf) {
        String villagerName = (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf);
        String speechRef = (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf);
        String cultureKey = (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf);
        int languageScore = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
        String vanillaFallbackKey = (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf);
        return new SpeechChatPayload(villagerName, speechRef, cultureKey, languageScore, vanillaFallbackKey);
    }

    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

