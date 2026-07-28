/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  io.netty.buffer.ByteBuf
 *  net.minecraft.network.codec.ByteBufCodecs
 *  net.minecraft.network.codec.StreamCodec
 *  net.minecraft.network.protocol.common.custom.CustomPacketPayload
 *  net.minecraft.network.protocol.common.custom.CustomPacketPayload$Type
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.entity.player.Player
 *  net.neoforged.neoforge.network.handling.IPayloadContext
 *  org.slf4j.Logger
 */
package org.millenaire.network;

import com.mojang.logging.LogUtils;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.millenaire.village.TravelBookContentBuilder;
import org.millenaire.village.TravelBookScreenState;
import org.slf4j.Logger;

public record TravelBookRequestPayload(TravelBookScreenState targetState, String cultureKey, String categoryKey, String itemKey, int navAction) {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int MAX_KEY_LENGTH = 128;
    public static final CustomPacketPayload.Type<TravelBookRequestPayload> TYPE = new CustomPacketPayload.Type(ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)"travel_book_request"));
    public static final StreamCodec<ByteBuf, TravelBookRequestPayload> STREAM_CODEC = StreamCodec.of(TravelBookRequestPayload::encode, TravelBookRequestPayload::decode);

    private static void encode(ByteBuf buf, TravelBookRequestPayload payload) {
        ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)payload.targetState.ordinal());
        ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)payload.cultureKey);
        ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)payload.categoryKey);
        ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)payload.itemKey);
        ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)payload.navAction);
    }

    private static TravelBookRequestPayload decode(ByteBuf buf) {
        int stateOrdinal = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
        TravelBookScreenState[] states = TravelBookScreenState.values();
        TravelBookScreenState state = stateOrdinal >= 0 && stateOrdinal < states.length ? states[stateOrdinal] : TravelBookScreenState.HOME;
        String cultureKey = TravelBookRequestPayload.truncate((String)ByteBufCodecs.STRING_UTF8.decode((Object)buf));
        String categoryKey = TravelBookRequestPayload.truncate((String)ByteBufCodecs.STRING_UTF8.decode((Object)buf));
        String itemKey = TravelBookRequestPayload.truncate((String)ByteBufCodecs.STRING_UTF8.decode((Object)buf));
        int navAction = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
        if (navAction < 0 || navAction > 3) {
            navAction = 0;
        }
        return new TravelBookRequestPayload(state, cultureKey, categoryKey, itemKey, navAction);
    }

    private static String truncate(String raw) {
        return raw.length() > 128 ? raw.substring(0, 128) : raw;
    }

    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleOnServer(TravelBookRequestPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player patt0$temp = context.player();
            if (patt0$temp instanceof ServerPlayer) {
                ServerPlayer serverPlayer = (ServerPlayer)patt0$temp;
                TravelBookContentBuilder.handleRequest(serverPlayer, payload);
            }
        });
    }
}

