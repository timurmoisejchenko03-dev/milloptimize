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
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.phys.AABB
 *  net.neoforged.neoforge.network.handling.IPayloadContext
 */
package org.millenaire.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.millenaire.entity.MillVillager;
import org.millenaire.village.Village;
import org.millenaire.village.VillagerRecord;

public record ToggleStancePayload(boolean aggressive) {
    private static final double RADIUS_XZ = 16.0;
    private static final double RADIUS_Y = 8.0;
    public static final CustomPacketPayload.Type<ToggleStancePayload> TYPE = new CustomPacketPayload.Type(ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)"toggle_stance"));
    public static final StreamCodec<ByteBuf, ToggleStancePayload> STREAM_CODEC = StreamCodec.of((buf, p) -> ByteBufCodecs.BOOL.encode(buf, (Object)p.aggressive), buf -> new ToggleStancePayload((Boolean)ByteBufCodecs.BOOL.decode(buf)));

    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleOnServer(ToggleStancePayload p, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Player patt0$temp = ctx.player();
            if (!(patt0$temp instanceof ServerPlayer)) {
                return;
            }
            ServerPlayer player = (ServerPlayer)patt0$temp;
            ServerLevel level = player.serverLevel();
            AABB box = player.getBoundingBox().inflate(16.0, 8.0, 16.0);
            for (MillVillager v : level.getEntitiesOfClass(MillVillager.class, box)) {
                VillagerRecord rec;
                Village village;
                if (!player.getUUID().equals(v.getHiredBy())) continue;
                v.setAggressiveStance(p.aggressive());
                if (v.getVillageId() == null || (village = Village.resolve(level, v.getVillageId())) == null || (rec = village.getVillagerRecord(v.getUUID())) == null) continue;
                rec.setAggressiveStance(p.aggressive());
                village.markDirty();
            }
        });
    }
}

