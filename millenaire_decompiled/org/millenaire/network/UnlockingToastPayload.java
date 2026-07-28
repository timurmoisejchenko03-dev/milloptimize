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

public record UnlockingToastPayload(String category, String title, String cultureName, int nbUnlocked, int nbTotal, String iconId) {
    public static final CustomPacketPayload.Type<UnlockingToastPayload> TYPE = new CustomPacketPayload.Type(ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)"unlocking_toast"));
    public static final StreamCodec<ByteBuf, UnlockingToastPayload> STREAM_CODEC = StreamCodec.of(UnlockingToastPayload::encode, UnlockingToastPayload::decode);
    private static final int MAX_STR = 256;

    private static void encode(ByteBuf buf, UnlockingToastPayload p) {
        ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)p.category);
        ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)p.title);
        ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)p.cultureName);
        ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)p.nbUnlocked);
        ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)p.nbTotal);
        ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)p.iconId);
    }

    private static UnlockingToastPayload decode(ByteBuf buf) {
        String category = UnlockingToastPayload.clamp((String)ByteBufCodecs.STRING_UTF8.decode((Object)buf));
        String title = UnlockingToastPayload.clamp((String)ByteBufCodecs.STRING_UTF8.decode((Object)buf));
        String cultureName = UnlockingToastPayload.clamp((String)ByteBufCodecs.STRING_UTF8.decode((Object)buf));
        int nbUnlocked = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
        int nbTotal = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
        String iconId = UnlockingToastPayload.clamp((String)ByteBufCodecs.STRING_UTF8.decode((Object)buf));
        return new UnlockingToastPayload(category, title, cultureName, nbUnlocked, nbTotal, iconId);
    }

    private static String clamp(String s) {
        return s.length() > 256 ? s.substring(0, 256) : s;
    }

    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

