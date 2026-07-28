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
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record InfoPanelContentPayload(List<CultureEntry> cultures) {
    private static final int MAX_CULTURES = 32;
    public static final CustomPacketPayload.Type<InfoPanelContentPayload> TYPE = new CustomPacketPayload.Type(ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)"info_panel_content"));
    public static final StreamCodec<ByteBuf, InfoPanelContentPayload> STREAM_CODEC = StreamCodec.of(InfoPanelContentPayload::encode, InfoPanelContentPayload::decode);

    private static void encode(ByteBuf buf, InfoPanelContentPayload payload) {
        ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)payload.cultures.size());
        for (CultureEntry entry : payload.cultures) {
            ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)entry.cultureNameKey);
            ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)entry.reputation);
            ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)entry.reputationLabel);
            ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)entry.languageScore);
        }
    }

    private static InfoPanelContentPayload decode(ByteBuf buf) {
        int count = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
        count = Math.min(count, 32);
        ArrayList<CultureEntry> entries = new ArrayList<CultureEntry>(count);
        for (int i = 0; i < count; ++i) {
            String cultureNameKey = (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf);
            int reputation = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
            String reputationLabel = (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf);
            int languageScore = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
            entries.add(new CultureEntry(cultureNameKey, reputation, reputationLabel, languageScore));
        }
        return new InfoPanelContentPayload(entries);
    }

    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public record CultureEntry(String cultureNameKey, int reputation, String reputationLabel, int languageScore) {
    }
}

