/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  io.netty.buffer.ByteBuf
 *  net.minecraft.core.BlockPos
 *  net.minecraft.network.codec.ByteBufCodecs
 *  net.minecraft.network.codec.StreamCodec
 *  net.minecraft.network.protocol.common.custom.CustomPacketPayload
 *  net.minecraft.network.protocol.common.custom.CustomPacketPayload$Type
 *  net.minecraft.resources.ResourceLocation
 */
package org.millenaire.network;

import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record WandDebugMenuPayload(int targetEntityId, BlockPos targetPos, String headerTitle, List<ActionEntry> actions) {
    private static final int MAX_ACTIONS = 32;
    private static final int MAX_STRING_LENGTH = 256;
    public static final CustomPacketPayload.Type<WandDebugMenuPayload> TYPE = new CustomPacketPayload.Type(ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)"wand_debug_menu"));
    public static final StreamCodec<ByteBuf, WandDebugMenuPayload> STREAM_CODEC = StreamCodec.of(WandDebugMenuPayload::encode, WandDebugMenuPayload::decode);

    private static void encode(ByteBuf buf, WandDebugMenuPayload payload) {
        ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)payload.targetEntityId);
        ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)payload.targetPos.getX());
        ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)payload.targetPos.getY());
        ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)payload.targetPos.getZ());
        ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)payload.headerTitle);
        ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)payload.actions.size());
        for (ActionEntry entry : payload.actions) {
            ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)entry.id);
            ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)entry.translationKey);
        }
    }

    private static WandDebugMenuPayload decode(ByteBuf buf) {
        int targetEntityId = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
        int x = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
        int y = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
        int z = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
        BlockPos targetPos = new BlockPos(x, y, z);
        String headerTitle = (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf);
        if (headerTitle.length() > 256) {
            headerTitle = headerTitle.substring(0, 256);
        }
        int rawCount = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
        int count = Math.min(rawCount, 32);
        ArrayList<ActionEntry> actions = new ArrayList<ActionEntry>(count);
        for (int i = 0; i < rawCount; ++i) {
            String id = (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf);
            String translationKey = (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf);
            if (i >= count) continue;
            if (id.length() > 256) {
                id = id.substring(0, 256);
            }
            if (translationKey.length() > 256) {
                translationKey = translationKey.substring(0, 256);
            }
            actions.add(new ActionEntry(id, translationKey));
        }
        return new WandDebugMenuPayload(targetEntityId, targetPos, headerTitle, Collections.unmodifiableList(actions));
    }

    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public record ActionEntry(String id, String translationKey) {
    }
}

