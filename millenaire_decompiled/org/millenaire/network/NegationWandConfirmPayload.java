/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  io.netty.buffer.ByteBuf
 *  net.minecraft.core.Vec3i
 *  net.minecraft.network.codec.ByteBufCodecs
 *  net.minecraft.network.codec.StreamCodec
 *  net.minecraft.network.protocol.common.custom.CustomPacketPayload
 *  net.minecraft.network.protocol.common.custom.CustomPacketPayload$Type
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.Level
 *  net.neoforged.neoforge.network.handling.IPayloadContext
 *  org.slf4j.Logger
 */
package org.millenaire.network;

import com.mojang.logging.LogUtils;
import io.netty.buffer.ByteBuf;
import java.util.UUID;
import net.minecraft.core.Vec3i;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.millenaire.item.ModItems;
import org.millenaire.item.NegationWandItem;
import org.millenaire.village.Village;
import org.millenaire.village.VillageId;
import org.millenaire.village.VillageManager;
import org.millenaire.village.VillageSavedData;
import org.slf4j.Logger;

public record NegationWandConfirmPayload(String villageUuid) {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final double MAX_CONFIRM_DISTANCE = 256.0;
    private static final int MAX_UUID_LENGTH = 64;
    public static final CustomPacketPayload.Type<NegationWandConfirmPayload> TYPE = new CustomPacketPayload.Type(ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)"negation_wand_confirm"));
    public static final StreamCodec<ByteBuf, NegationWandConfirmPayload> STREAM_CODEC = StreamCodec.of(NegationWandConfirmPayload::encode, NegationWandConfirmPayload::decode);

    private static void encode(ByteBuf buf, NegationWandConfirmPayload payload) {
        ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)payload.villageUuid);
    }

    private static NegationWandConfirmPayload decode(ByteBuf buf) {
        String uuid = (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf);
        if (uuid.length() > 64) {
            uuid = uuid.substring(0, 64);
        }
        return new NegationWandConfirmPayload(uuid);
    }

    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleOnServer(NegationWandConfirmPayload payload, ServerPlayer player) {
        UUID uuid;
        boolean holdsWand;
        if (player == null) {
            return;
        }
        Level level = player.level();
        if (!(level instanceof ServerLevel)) {
            return;
        }
        ServerLevel serverLevel = (ServerLevel)level;
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();
        boolean bl = holdsWand = mainHand.is((Item)ModItems.NEGATION_WAND.get()) || offHand.is((Item)ModItems.NEGATION_WAND.get());
        if (!holdsWand) {
            LOGGER.warn("[Millenaire] Player {} sent negation wand confirm without holding wand", (Object)player.getName().getString());
            return;
        }
        try {
            uuid = UUID.fromString(payload.villageUuid);
        }
        catch (IllegalArgumentException e) {
            return;
        }
        VillageSavedData savedData = VillageSavedData.get(serverLevel);
        VillageManager manager = savedData.getVillageManager();
        VillageId villageId = new VillageId(uuid);
        Village village = manager.getVillage(villageId);
        if (village == null) {
            LOGGER.warn("[Millenaire] Player {} tried to delete non-existent village {}", (Object)player.getName().getString(), (Object)payload.villageUuid);
            return;
        }
        double distSq = player.blockPosition().distSqr((Vec3i)village.getCenter());
        if (distSq > 65536.0) {
            LOGGER.warn("[Millenaire] Player {} too far from village {} for negation wand confirm (dist={})", new Object[]{player.getName().getString(), payload.villageUuid, String.format("%.0f", Math.sqrt(distSq))});
            return;
        }
        if (village.areChestsLocked()) {
            LOGGER.warn("[Millenaire] Player {} tried to delete locked village {}", (Object)player.getName().getString(), (Object)payload.villageUuid);
            return;
        }
        NegationWandItem.performDeletion(serverLevel, savedData, village, player);
    }

    public static void handleOnServer(NegationWandConfirmPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player patt0$temp = context.player();
            if (patt0$temp instanceof ServerPlayer) {
                ServerPlayer player = (ServerPlayer)patt0$temp;
                NegationWandConfirmPayload.handleOnServer(payload, player);
            }
        });
    }
}

