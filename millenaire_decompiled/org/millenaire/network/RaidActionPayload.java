/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  io.netty.buffer.ByteBuf
 *  net.minecraft.network.chat.Component
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
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.millenaire.combat.raid.RaidManager;
import org.millenaire.entity.VillagerInteraction;
import org.millenaire.village.ControlledMilitaryService;
import org.millenaire.village.Village;
import org.millenaire.village.VillageId;
import org.millenaire.village.VillageManager;
import org.millenaire.village.VillageSavedData;

public record RaidActionPayload(String villageId, String targetVillageId, int action, boolean fromControlledMilitary) {
    public static final int ACTION_PLAN_RAID = 0;
    public static final int ACTION_CANCEL_RAID = 1;
    public static final CustomPacketPayload.Type<RaidActionPayload> TYPE = new CustomPacketPayload.Type(ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)"raid_action"));
    public static final StreamCodec<ByteBuf, RaidActionPayload> STREAM_CODEC = StreamCodec.of(RaidActionPayload::encode, RaidActionPayload::decode);
    private static final int MAX_ID_LENGTH = 128;

    public RaidActionPayload(String villageId, String targetVillageId, int action) {
        this(villageId, targetVillageId, action, false);
    }

    private static void encode(ByteBuf buf, RaidActionPayload payload) {
        ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)payload.villageId);
        ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)(payload.targetVillageId == null ? "" : payload.targetVillageId));
        ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)payload.action);
        buf.writeBoolean(payload.fromControlledMilitary);
    }

    private static RaidActionPayload decode(ByteBuf buf) {
        String targetVillageId;
        String villageId = (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf);
        if (villageId.length() > 128) {
            villageId = villageId.substring(0, 128);
        }
        if ((targetVillageId = (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf)).length() > 128) {
            targetVillageId = targetVillageId.substring(0, 128);
        }
        int action = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
        boolean fromControlledMilitary = buf.readBoolean();
        return new RaidActionPayload(villageId, targetVillageId, action, fromControlledMilitary);
    }

    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleOnServer(RaidActionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
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
            }
            catch (IllegalArgumentException e) {
                return;
            }
            VillageManager vm = VillageSavedData.get(serverLevel).getVillageManager();
            Village village = vm.getVillage(new VillageId(actingUuid));
            if (village == null) {
                return;
            }
            RaidActionPayload.applyRaidAction(payload, player, serverLevel, vm, village);
            if (payload.fromControlledMilitary) {
                ControlledMilitaryService.sendRefresh(player, serverLevel, village);
            } else {
                VillagerInteraction.sendChiefRefresh(player, serverLevel, payload.villageId);
            }
        });
    }

    private static void applyRaidAction(RaidActionPayload payload, ServerPlayer player, ServerLevel serverLevel, VillageManager vm, Village village) {
        if (village.getOwnerUUID() == null || !village.getOwnerUUID().equals(player.getUUID())) {
            player.sendSystemMessage((Component)Component.translatable((String)"millenaire.combat.raid_not_owner"));
            return;
        }
        switch (payload.action) {
            case 0: {
                UUID targetUuid;
                try {
                    targetUuid = UUID.fromString(payload.targetVillageId);
                }
                catch (IllegalArgumentException e) {
                    return;
                }
                Village target = vm.getVillage(new VillageId(targetUuid));
                if (target == null) {
                    return;
                }
                if (village.getRaidTarget() != null) {
                    player.sendSystemMessage((Component)Component.translatable((String)"millenaire.combat.raid_already_planned"));
                    return;
                }
                village.adjustRelationSymmetric(serverLevel, target.getId(), -100, true);
                RaidManager.planRaid(village, target, serverLevel);
                player.sendSystemMessage((Component)Component.translatable((String)"millenaire.combat.raid_planned_ack", (Object[])new Object[]{Component.literal((String)(target.getVillageName() != null ? target.getVillageName() : target.getVillageTypeId().getPath()))}));
                break;
            }
            case 1: {
                if (village.getRaidTarget() == null) {
                    return;
                }
                if (village.getRaidStart() != 0L) {
                    return;
                }
                village.clearRaid();
                VillageSavedData.get(serverLevel).setDirty();
                player.sendSystemMessage((Component)Component.translatable((String)"millenaire.combat.raid_cancelled_ack"));
                break;
            }
        }
    }
}

