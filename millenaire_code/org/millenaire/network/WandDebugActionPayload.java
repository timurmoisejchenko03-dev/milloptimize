/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.netty.buffer.ByteBuf
 *  net.minecraft.core.BlockPos
 *  net.minecraft.network.codec.ByteBufCodecs
 *  net.minecraft.network.codec.StreamCodec
 *  net.minecraft.network.protocol.common.custom.CustomPacketPayload
 *  net.minecraft.network.protocol.common.custom.CustomPacketPayload$Type
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.level.Level
 *  net.neoforged.neoforge.network.handling.IPayloadContext
 */
package org.millenaire.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.millenaire.item.WandDebugActions;

public record WandDebugActionPayload(String actionId, int targetEntityId, BlockPos targetPos) implements CustomPacketPayload
{
    private static final int MAX_STRING_LENGTH = 256;
    public static final CustomPacketPayload.Type<WandDebugActionPayload> TYPE = new CustomPacketPayload.Type(ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)"wand_debug_action"));
    public static final StreamCodec<ByteBuf, WandDebugActionPayload> STREAM_CODEC = StreamCodec.of(WandDebugActionPayload::encode, WandDebugActionPayload::decode);

    private static void encode(ByteBuf buf, WandDebugActionPayload payload) {
        ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)payload.actionId);
        ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)payload.targetEntityId);
        ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)payload.targetPos.getX());
        ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)payload.targetPos.getY());
        ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)payload.targetPos.getZ());
    }

    private static WandDebugActionPayload decode(ByteBuf buf) {
        String actionId = (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf);
        if (actionId.length() > 256) {
            actionId = actionId.substring(0, 256);
        }
        int targetEntityId = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
        int x = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
        int y = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
        int z = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
        BlockPos targetPos = new BlockPos(x, y, z);
        return new WandDebugActionPayload(actionId, targetEntityId, targetPos);
    }

    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleOnServer(WandDebugActionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player patt0$temp = context.player();
            if (!(patt0$temp instanceof ServerPlayer)) {
                return;
            }
            ServerPlayer player = (ServerPlayer)patt0$temp;
            if (!player.hasPermissions(2) && !player.server.isSingleplayer()) {
                return;
            }
            Level patt1$temp = player.level();
            if (!(patt1$temp instanceof ServerLevel)) {
                return;
            }
            ServerLevel level = (ServerLevel)patt1$temp;
            WandDebugActions.execute(level, player, payload);
        });
    }
}

