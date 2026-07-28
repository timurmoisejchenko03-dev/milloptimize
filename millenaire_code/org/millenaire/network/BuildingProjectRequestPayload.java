/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  io.netty.buffer.ByteBuf
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
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
 *  org.slf4j.Logger
 */
package org.millenaire.network;

import com.mojang.logging.LogUtils;
import io.netty.buffer.ByteBuf;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
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
import org.millenaire.building.BuildingPlan;
import org.millenaire.building.BuildingPlanSet;
import org.millenaire.culture.ModCultures;
import org.millenaire.culture.VillageType;
import org.millenaire.network.PayloadRateLimiter;
import org.millenaire.village.ControlledProjectsService;
import org.millenaire.village.PlacementSignHelper;
import org.millenaire.village.Village;
import org.millenaire.village.VillageGrowthManager;
import org.millenaire.village.VillageId;
import org.millenaire.village.VillageSavedData;
import org.millenaire.world.BuildingLocationFinder;
import org.millenaire.world.PlacedLocation;
import org.slf4j.Logger;

public record BuildingProjectRequestPayload(String villageUuid, String planSetId, BlockPos clickedPos) implements CustomPacketPayload
{
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int MAX_STRING_LENGTH = 256;
    private static final double MIN_PROXIMITY = 64.0;
    private static final double PROXIMITY_MARGIN = 30.0;
    private static final PayloadRateLimiter RATE_LIMITER = new PayloadRateLimiter(200L);
    public static final CustomPacketPayload.Type<BuildingProjectRequestPayload> TYPE = new CustomPacketPayload.Type(ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)"building_project_request"));
    public static final StreamCodec<ByteBuf, BuildingProjectRequestPayload> STREAM_CODEC = StreamCodec.of(BuildingProjectRequestPayload::encode, BuildingProjectRequestPayload::decode);

    private static void encode(ByteBuf buf, BuildingProjectRequestPayload payload) {
        ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)payload.villageUuid);
        ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)payload.planSetId);
        ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)payload.clickedPos.getX());
        ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)payload.clickedPos.getY());
        ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)payload.clickedPos.getZ());
    }

    private static BuildingProjectRequestPayload decode(ByteBuf buf) {
        String planSetId;
        String villageUuid = (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf);
        if (villageUuid.length() > 256) {
            villageUuid = villageUuid.substring(0, 256);
        }
        if ((planSetId = (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf)).length() > 256) {
            planSetId = planSetId.substring(0, 256);
        }
        int px = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
        int py = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
        int pz = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
        return new BuildingProjectRequestPayload(villageUuid, planSetId, new BlockPos(px, py, pz));
    }

    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleOnServer(BuildingProjectRequestPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            BuildingPlanSet previousSet;
            BuildingLocationFinder.AnchorEvaluation evaluation;
            BuildingPlan plan;
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
                LOGGER.warn("[Millenaire] BuildingProjectRequest: village {} not found for player {}", (Object)payload.villageUuid, (Object)player.getName().getString());
                return;
            }
            if (!village.isControlledBy(player.getUUID())) {
                LOGGER.warn("[Millenaire] BuildingProjectRequest: player {} is not owner of village {}", (Object)player.getName().getString(), (Object)payload.villageUuid);
                return;
            }
            VillageType vt = ModCultures.getVillageType(village.getVillageTypeId());
            double allowed = Math.max(64.0, (double)(vt != null ? vt.radius() : 0) + 30.0);
            if (player.distanceToSqr((double)village.getCenter().getX(), (double)village.getCenter().getY(), (double)village.getCenter().getZ()) > allowed * allowed) {
                LOGGER.warn("[Millenaire] BuildingProjectRequest: player {} too far from village {}", (Object)player.getName().getString(), (Object)payload.villageUuid);
                player.sendSystemMessage((Component)Component.translatable((String)"gui.millenaire.building_project.too_far"));
                return;
            }
            if (!RATE_LIMITER.acquire(player.getUUID())) {
                return;
            }
            ResourceLocation planSetLoc = ResourceLocation.tryParse((String)payload.planSetId);
            if (planSetLoc == null) {
                return;
            }
            BuildingPlanSet planSet = ModCultures.getBuildingPlanSet(planSetLoc);
            if (planSet == null) {
                LOGGER.warn("[Millenaire] BuildingProjectRequest: plan set {} not found", (Object)payload.planSetId);
                return;
            }
            String variant = planSet.pickRandomVariant(ThreadLocalRandom.current());
            BuildingPlanSet.LevelDef levelDef = planSet.getLevel(variant, 0);
            BuildingPlan buildingPlan = plan = levelDef != null ? ModCultures.getBuildingPlan(levelDef.planId()) : null;
            if (plan == null) {
                LOGGER.warn("[Millenaire] BuildingProjectRequest: plan {} has no level 0 / plan resource", (Object)payload.planSetId);
                player.sendSystemMessage((Component)Component.translatable((String)"gui.millenaire.building_project.no_location", (Object[])new Object[]{planSet.nativeName()}));
                return;
            }
            VillageType.LayoutSlot slot = null;
            if (vt != null) {
                for (VillageType.LayoutSlot s : vt.layout()) {
                    if (!planSetLoc.equals((Object)s.plan())) continue;
                    slot = s;
                    break;
                }
            }
            if (!(evaluation = VillageGrowthManager.validateLocationAt(overworld, village, planSet, plan, slot, payload.clickedPos)).isSuccess()) {
                String hint = evaluation.errorPos() != null ? ControlledProjectsService.formatDistanceDirection(payload.clickedPos, evaluation.errorPos()) : "";
                player.sendSystemMessage((Component)Component.translatable((String)BuildingProjectRequestPayload.errorKeyFor(evaluation.reason()), (Object[])new Object[]{planSet.nativeName(), hint}));
                return;
            }
            PlacedLocation planned = evaluation.location();
            Village.PendingProject previous = village.getPendingProject();
            if (previous != null && previous.plannedLocation() != null && (previousSet = ModCultures.getBuildingPlanSet(previous.planSetId())) != null) {
                BuildingPlan prevPlan;
                BuildingPlanSet.LevelDef prevLevel = previousSet.getLevel(previous.variant(), previous.level());
                BuildingPlan buildingPlan2 = prevPlan = prevLevel != null ? ModCultures.getBuildingPlan(prevLevel.planId()) : null;
                if (prevPlan != null) {
                    PlacementSignHelper.removeCornerSigns(overworld, prevPlan, previous.plannedLocation());
                }
            }
            village.setPendingProject(new Village.PendingProject(planSetLoc, variant, 0, false, null, planned));
            village.setNoProjectsLeftUntil(0L);
            PlacementSignHelper.placeCornerSigns(overworld, plan, planned, planSet.nativeName());
            player.sendSystemMessage((Component)Component.translatable((String)"gui.millenaire.building_project.queued", (Object[])new Object[]{planSet.nativeName()}));
        });
    }

    static String errorKeyFor(@Nullable BuildingLocationFinder.FailureReason reason) {
        if (reason == null) {
            return "gui.millenaire.building_project.no_location";
        }
        return switch (reason) {
            default -> throw new MatchException(null, null);
            case BuildingLocationFinder.FailureReason.CONSTRUCTION_FORBIDDEN -> "gui.millenaire.building_project.construction_forbidden";
            case BuildingLocationFinder.FailureReason.LOCATION_CLASH -> "gui.millenaire.building_project.location_clash";
            case BuildingLocationFinder.FailureReason.OUTSIDE_RADIUS -> "gui.millenaire.building_project.outside_radius";
            case BuildingLocationFinder.FailureReason.WRONG_ALTITUDE -> "gui.millenaire.building_project.wrong_elevation";
            case BuildingLocationFinder.FailureReason.DANGER -> "gui.millenaire.building_project.danger";
            case BuildingLocationFinder.FailureReason.NOT_REACHABLE -> "gui.millenaire.building_project.not_reachable";
            case BuildingLocationFinder.FailureReason.GENERIC -> "gui.millenaire.building_project.no_location";
        };
    }
}

