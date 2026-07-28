/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
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
 *  net.neoforged.neoforge.network.PacketDistributor
 *  net.neoforged.neoforge.network.handling.IPayloadContext
 *  org.slf4j.Logger
 */
package org.millenaire.network;

import com.mojang.logging.LogUtils;
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
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.millenaire.building.BuildingPlan;
import org.millenaire.building.BuildingPlanSet;
import org.millenaire.culture.ModCultures;
import org.millenaire.network.PayloadRateLimiter;
import org.millenaire.village.ControlledProjectsService;
import org.millenaire.village.PlacementSignHelper;
import org.millenaire.village.Village;
import org.millenaire.village.VillageId;
import org.millenaire.village.VillageSavedData;
import org.slf4j.Logger;

public record BuildingProjectCancelRequestPayload(String villageUuid) {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int MAX_STRING_LENGTH = 256;
    private static final double MAX_DISTANCE_SQ = 4096.0;
    private static final PayloadRateLimiter RATE_LIMITER = new PayloadRateLimiter(200L);
    public static final CustomPacketPayload.Type<BuildingProjectCancelRequestPayload> TYPE = new CustomPacketPayload.Type(ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)"building_project_cancel_request"));
    public static final StreamCodec<ByteBuf, BuildingProjectCancelRequestPayload> STREAM_CODEC = StreamCodec.of(BuildingProjectCancelRequestPayload::encode, BuildingProjectCancelRequestPayload::decode);

    private static void encode(ByteBuf buf, BuildingProjectCancelRequestPayload payload) {
        ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)payload.villageUuid);
    }

    private static BuildingProjectCancelRequestPayload decode(ByteBuf buf) {
        String villageUuid = (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf);
        if (villageUuid.length() > 256) {
            villageUuid = villageUuid.substring(0, 256);
        }
        return new BuildingProjectCancelRequestPayload(villageUuid);
    }

    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleOnServer(BuildingProjectCancelRequestPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            BuildingPlanSet set;
            UUID uuid;
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
            ServerLevel overworld = serverLevel.getServer().getLevel(Level.OVERWORLD);
            if (overworld == null) {
                return;
            }
            try {
                uuid = UUID.fromString(payload.villageUuid);
            }
            catch (IllegalArgumentException e) {
                return;
            }
            VillageSavedData savedData = VillageSavedData.get(overworld);
            Village village = savedData.getVillageManager().getVillage(new VillageId(uuid));
            if (village == null) {
                return;
            }
            if (!village.isControlledBy(player.getUUID())) {
                LOGGER.warn("[Millenaire] BuildingProjectCancel: player {} is not owner of village {}", (Object)player.getName().getString(), (Object)payload.villageUuid);
                return;
            }
            if (player.distanceToSqr((double)village.getCenter().getX(), (double)village.getCenter().getY(), (double)village.getCenter().getZ()) > 4096.0) {
                LOGGER.warn("[Millenaire] BuildingProjectCancel: player {} too far from village {}", (Object)player.getName().getString(), (Object)payload.villageUuid);
                return;
            }
            if (!RATE_LIMITER.acquire(player.getUUID())) {
                return;
            }
            Village.PendingProject pending = village.getPendingProject();
            if (pending == null) {
                return;
            }
            if (pending.plannedLocation() != null && (set = ModCultures.getBuildingPlanSet(pending.planSetId())) != null) {
                BuildingPlan plan;
                BuildingPlanSet.LevelDef levelDef = set.getLevel(pending.variant(), pending.level());
                BuildingPlan buildingPlan = plan = levelDef != null ? ModCultures.getBuildingPlan(levelDef.planId()) : null;
                if (plan != null) {
                    PlacementSignHelper.removeCornerSigns(overworld, plan, pending.plannedLocation());
                }
            }
            village.setPendingProject(null);
            savedData.setDirty();
            set = ModCultures.getBuildingPlanSet(pending.planSetId());
            String name = set != null ? set.nativeName() : pending.planSetId().getPath();
            player.sendSystemMessage((Component)Component.translatable((String)"gui.millenaire.building_project.cancelled", (Object[])new Object[]{name}));
            PacketDistributor.sendToPlayer((ServerPlayer)player, (CustomPacketPayload)ControlledProjectsService.buildPayload(village), (CustomPacketPayload[])new CustomPacketPayload[0]);
        });
    }
}

