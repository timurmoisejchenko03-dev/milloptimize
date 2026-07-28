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
import org.millenaire.culture.Culture;
import org.millenaire.culture.ModCultures;
import org.millenaire.entity.VillagerInteraction;
import org.millenaire.village.PlayerCultureReputation;
import org.millenaire.village.Village;
import org.millenaire.village.VillageId;
import org.millenaire.village.VillageManager;
import org.millenaire.village.VillageSavedData;

public record CultureControlPurchasePayload(String villageId) {
    public static final CustomPacketPayload.Type<CultureControlPurchasePayload> TYPE = new CustomPacketPayload.Type(ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)"culture_control_purchase"));
    public static final StreamCodec<ByteBuf, CultureControlPurchasePayload> STREAM_CODEC = StreamCodec.of(CultureControlPurchasePayload::encode, CultureControlPurchasePayload::decode);
    private static final int MAX_ID_LENGTH = 128;

    private static void encode(ByteBuf buf, CultureControlPurchasePayload payload) {
        ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)payload.villageId);
    }

    private static CultureControlPurchasePayload decode(ByteBuf buf) {
        String villageId = (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf);
        if (villageId.length() > 128) {
            villageId = villageId.substring(0, 128);
        }
        return new CultureControlPurchasePayload(villageId);
    }

    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleOnServer(CultureControlPurchasePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
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
            try {
                uuid = UUID.fromString(payload.villageId);
            }
            catch (IllegalArgumentException e) {
                return;
            }
            VillageManager vm = VillageSavedData.get(serverLevel).getVillageManager();
            Village village = vm.getVillage(new VillageId(uuid));
            if (village == null) {
                return;
            }
            CultureControlPurchasePayload.applyCultureControl(player, serverLevel, village);
            VillagerInteraction.sendChiefRefresh(player, serverLevel, payload.villageId);
        });
    }

    private static void applyCultureControl(ServerPlayer player, ServerLevel serverLevel, Village village) {
        if (player.distanceToSqr((double)village.getCenter().getX(), (double)village.getCenter().getY(), (double)village.getCenter().getZ()) > 4096.0) {
            return;
        }
        int reputation = village.getCombinedReputation(serverLevel, player.getUUID());
        if (reputation < 131072) {
            return;
        }
        PlayerCultureReputation cultureRep = PlayerCultureReputation.get(serverLevel);
        if (cultureRep.hasCultureControl(player.getUUID(), village.getCultureId())) {
            return;
        }
        cultureRep.grantCultureControl(player.getUUID(), village.getCultureId());
        Culture culture = ModCultures.getCulture(village.getCultureId());
        String cultureName = culture != null ? culture.displayName() : village.getCultureId().getPath();
        player.sendSystemMessage((Component)Component.translatable((String)"millenaire.ui.control_gotten", (Object[])new Object[]{cultureName}));
    }
}

