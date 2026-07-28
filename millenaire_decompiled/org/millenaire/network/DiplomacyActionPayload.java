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
 *  net.minecraft.world.level.Level
 *  net.neoforged.neoforge.network.handling.IPayloadContext
 */
package org.millenaire.network;

import io.netty.buffer.ByteBuf;
import java.util.UUID;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.millenaire.entity.VillagerInteraction;
import org.millenaire.village.DiplomacyHelper;
import org.millenaire.village.Village;
import org.millenaire.village.VillageId;
import org.millenaire.village.VillageManager;
import org.millenaire.village.VillageSavedData;

public record DiplomacyActionPayload(String villageId, String targetVillageId, boolean isPraise) {
    public static final CustomPacketPayload.Type<DiplomacyActionPayload> TYPE = new CustomPacketPayload.Type(ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)"diplomacy_action"));
    public static final StreamCodec<ByteBuf, DiplomacyActionPayload> STREAM_CODEC = StreamCodec.of(DiplomacyActionPayload::encode, DiplomacyActionPayload::decode);
    private static final int MAX_ID_LENGTH = 128;

    private static void encode(ByteBuf buf, DiplomacyActionPayload payload) {
        ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)payload.villageId);
        ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)payload.targetVillageId);
        ByteBufCodecs.BOOL.encode((Object)buf, (Object)payload.isPraise);
    }

    private static DiplomacyActionPayload decode(ByteBuf buf) {
        String targetVillageId;
        String villageId = (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf);
        if (villageId.length() > 128) {
            villageId = villageId.substring(0, 128);
        }
        if ((targetVillageId = (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf)).length() > 128) {
            targetVillageId = targetVillageId.substring(0, 128);
        }
        boolean isPraise = (Boolean)ByteBufCodecs.BOOL.decode((Object)buf);
        return new DiplomacyActionPayload(villageId, targetVillageId, isPraise);
    }

    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleOnServer(DiplomacyActionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            UUID targetUuid;
            UUID actingUuid;
            Player patt0$temp = context.player();
            if (!(patt0$temp instanceof ServerPlayer)) {
                return;
            }
            ServerPlayer player = (ServerPlayer)patt0$temp;
            Level patt1$temp = player.level();
            if (!(patt1$temp instanceof ServerLevel)) {
                return;
            }
            ServerLevel serverLevel = (ServerLevel)patt1$temp;
            try {
                actingUuid = UUID.fromString(payload.villageId);
                targetUuid = UUID.fromString(payload.targetVillageId);
            }
            catch (IllegalArgumentException e) {
                return;
            }
            VillageManager vm = VillageSavedData.get(serverLevel).getVillageManager();
            VillageId actingId = new VillageId(actingUuid);
            VillageId targetId = new VillageId(targetUuid);
            Village village = vm.getVillage(actingId);
            if (village == null) {
                return;
            }
            if (!actingId.equals(targetId) && village.getRelations().containsKey(targetId)) {
                DiplomacyHelper.performDiplomacy(serverLevel, player, village, targetId, payload.isPraise);
            }
            VillagerInteraction.sendChiefRefresh(player, serverLevel, payload.villageId);
        });
    }
}

