/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
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
 *  net.neoforged.neoforge.network.PacketDistributor
 *  net.neoforged.neoforge.network.handling.IPayloadContext
 *  org.slf4j.Logger
 */
package org.millenaire.network;

import com.mojang.logging.LogUtils;
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
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.millenaire.building.BuildingId;
import org.millenaire.building.BuildingInstance;
import org.millenaire.network.PayloadRateLimiter;
import org.millenaire.village.ControlledProjectsService;
import org.millenaire.village.Village;
import org.millenaire.village.VillageId;
import org.millenaire.village.VillageSavedData;
import org.slf4j.Logger;

public record BuildingUpgradeToggleRequestPayload(String villageUuid, String buildingId, boolean allow) implements CustomPacketPayload
{
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int MAX_STRING_LENGTH = 256;
    private static final double MAX_DISTANCE_SQ = 4096.0;
    private static final PayloadRateLimiter RATE_LIMITER = new PayloadRateLimiter(200L);
    public static final CustomPacketPayload.Type<BuildingUpgradeToggleRequestPayload> TYPE = new CustomPacketPayload.Type(ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)"building_upgrade_toggle_request"));
    public static final StreamCodec<ByteBuf, BuildingUpgradeToggleRequestPayload> STREAM_CODEC = StreamCodec.of(BuildingUpgradeToggleRequestPayload::encode, BuildingUpgradeToggleRequestPayload::decode);

    private static void encode(ByteBuf buf, BuildingUpgradeToggleRequestPayload payload) {
        ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)payload.villageUuid);
        ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)payload.buildingId);
        buf.writeBoolean(payload.allow);
    }

    private static BuildingUpgradeToggleRequestPayload decode(ByteBuf buf) {
        String buildingId;
        String villageUuid = (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf);
        if (villageUuid.length() > 256) {
            villageUuid = villageUuid.substring(0, 256);
        }
        if ((buildingId = (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf)).length() > 256) {
            buildingId = buildingId.substring(0, 256);
        }
        return new BuildingUpgradeToggleRequestPayload(villageUuid, buildingId, buf.readBoolean());
    }

    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleOnServer(BuildingUpgradeToggleRequestPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            UUID buildingUuid;
            UUID villageUuid;
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
                villageUuid = UUID.fromString(payload.villageUuid);
                buildingUuid = UUID.fromString(payload.buildingId);
            }
            catch (IllegalArgumentException e) {
                return;
            }
            VillageSavedData savedData = VillageSavedData.get(overworld);
            Village village = savedData.getVillageManager().getVillage(new VillageId(villageUuid));
            if (village == null) {
                LOGGER.warn("[Millenaire] BuildingUpgradeToggle: village {} not found", (Object)payload.villageUuid);
                return;
            }
            if (!village.isControlledBy(player.getUUID())) {
                LOGGER.warn("[Millenaire] BuildingUpgradeToggle: player {} is not owner of village {}", (Object)player.getName().getString(), (Object)payload.villageUuid);
                return;
            }
            if (player.distanceToSqr((double)village.getCenter().getX(), (double)village.getCenter().getY(), (double)village.getCenter().getZ()) > 4096.0) {
                LOGGER.warn("[Millenaire] BuildingUpgradeToggle: player {} too far from village {}", (Object)player.getName().getString(), (Object)payload.villageUuid);
                return;
            }
            if (!RATE_LIMITER.acquire(player.getUUID())) {
                return;
            }
            BuildingInstance building = village.findBuildingById(new BuildingId(buildingUuid));
            if (building == null) {
                LOGGER.warn("[Millenaire] BuildingUpgradeToggle: building {} not found in village {}", (Object)payload.buildingId, (Object)payload.villageUuid);
                return;
            }
            building.setUpgradesAllowed(payload.allow);
            savedData.setDirty();
            PacketDistributor.sendToPlayer((ServerPlayer)player, (CustomPacketPayload)ControlledProjectsService.buildPayload(village), (CustomPacketPayload[])new CustomPacketPayload[0]);
        });
    }
}

