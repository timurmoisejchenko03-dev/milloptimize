/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  io.netty.buffer.ByteBuf
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
import org.millenaire.advancement.MillAdvancements;
import org.millenaire.config.MillenaireServerConfig;
import org.millenaire.culture.ModCultures;
import org.millenaire.culture.VillageType;
import org.millenaire.network.PayloadRateLimiter;
import org.millenaire.village.PlayerCultureReputation;
import org.millenaire.village.VillageManager;
import org.millenaire.village.VillageSavedData;
import org.millenaire.world.VillageSpawner;
import org.slf4j.Logger;

public record VillageCreationRequestPayload(BlockPos pos, String cultureKey, String typeKey) {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final CustomPacketPayload.Type<VillageCreationRequestPayload> TYPE = new CustomPacketPayload.Type(ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)"village_creation_request"));
    public static final StreamCodec<ByteBuf, VillageCreationRequestPayload> STREAM_CODEC = StreamCodec.of(VillageCreationRequestPayload::encode, VillageCreationRequestPayload::decode);
    private static final int MAX_KEY_LENGTH = 128;
    private static final PayloadRateLimiter RATE_LIMITER = new PayloadRateLimiter(1000L);

    private static void encode(ByteBuf buf, VillageCreationRequestPayload payload) {
        ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)payload.pos.getX());
        ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)payload.pos.getY());
        ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)payload.pos.getZ());
        ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)payload.cultureKey);
        ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)payload.typeKey);
    }

    private static VillageCreationRequestPayload decode(ByteBuf buf) {
        String typeKey;
        int x = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
        int y = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
        int z = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
        String cultureKey = (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf);
        if (cultureKey.length() > 128) {
            cultureKey = cultureKey.substring(0, 128);
        }
        if ((typeKey = (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf)).length() > 128) {
            typeKey = typeKey.substring(0, 128);
        }
        return new VillageCreationRequestPayload(new BlockPos(x, y, z), cultureKey, typeKey);
    }

    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleOnServer(VillageCreationRequestPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            PlayerCultureReputation cultureRep;
            int minDistance;
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
            if (!RATE_LIMITER.acquire(player.getUUID())) {
                return;
            }
            if (!serverLevel.dimension().equals((Object)Level.OVERWORLD)) {
                player.sendSystemMessage((Component)Component.literal((String)"The summoning wand only works in the Overworld."));
                return;
            }
            ResourceLocation villageTypeId = ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)payload.typeKey);
            VillageType villageType = ModCultures.getVillageType(villageTypeId);
            if (villageType == null) {
                player.sendSystemMessage((Component)Component.literal((String)("Village type not found: " + payload.typeKey)));
                return;
            }
            VillageManager villageManager = VillageSavedData.get(serverLevel).getVillageManager();
            if (villageManager.isWithinMinDistance(payload.pos, minDistance = MillenaireServerConfig.SERVER.minVillageDistance.getAsInt())) {
                player.sendSystemMessage((Component)Component.literal((String)("A village already exists within " + minDistance + " blocks.")));
                return;
            }
            if (villageType.playerControlled() && !(cultureRep = PlayerCultureReputation.get(serverLevel)).hasCultureControl(player.getUUID(), villageType.culture())) {
                player.sendSystemMessage((Component)Component.translatable((String)"millenaire.error.no_culture_control", (Object[])new Object[]{villageType.name()}));
                return;
            }
            Component failure = VillageSpawner.spawnVillage(serverLevel, payload.pos, villageType, 0, null, null, (ServerPlayer)(villageType.playerControlled() ? player : null));
            if (failure == null) {
                player.sendSystemMessage((Component)Component.literal((String)("Village " + villageType.name() + " (" + villageTypeId.getPath() + ") created at " + payload.pos.toShortString())));
                MillAdvancements.grant(player, MillAdvancements.SUMMONING_WAND);
            } else {
                player.sendSystemMessage(failure);
            }
        });
    }
}

