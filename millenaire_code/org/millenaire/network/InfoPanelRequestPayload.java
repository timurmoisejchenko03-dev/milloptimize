/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  io.netty.buffer.ByteBuf
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.millenaire.culture.Culture;
import org.millenaire.culture.ModCultures;
import org.millenaire.culture.ReputationLabel;
import org.millenaire.network.InfoPanelContentPayload;
import org.millenaire.network.PayloadRateLimiter;
import org.millenaire.village.PlayerCultureReputation;
import org.millenaire.village.VillageReputation;
import org.slf4j.Logger;

public record InfoPanelRequestPayload() implements CustomPacketPayload
{
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final CustomPacketPayload.Type<InfoPanelRequestPayload> TYPE = new CustomPacketPayload.Type(ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)"info_panel_request"));
    public static final StreamCodec<ByteBuf, InfoPanelRequestPayload> STREAM_CODEC = StreamCodec.of((buf, payload) -> {}, buf -> new InfoPanelRequestPayload());
    private static final PayloadRateLimiter RATE_LIMITER = new PayloadRateLimiter(200L);

    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleOnServer(InfoPanelRequestPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player patt0$temp = context.player();
            if (!(patt0$temp instanceof ServerPlayer)) {
                return;
            }
            ServerPlayer serverPlayer = (ServerPlayer)patt0$temp;
            Level patt1$temp = serverPlayer.level();
            if (!(patt1$temp instanceof ServerLevel)) {
                return;
            }
            ServerLevel serverLevel = (ServerLevel)patt1$temp;
            if (!RATE_LIMITER.acquire(serverPlayer.getUUID())) {
                return;
            }
            PlayerCultureReputation cultureRep = PlayerCultureReputation.get(serverLevel);
            ArrayList<InfoPanelContentPayload.CultureEntry> entries = new ArrayList<InfoPanelContentPayload.CultureEntry>();
            Map<ResourceLocation, Culture> allCultures = ModCultures.getAllCultures();
            for (Map.Entry<ResourceLocation, Culture> entry : allCultures.entrySet()) {
                String repLabel;
                ResourceLocation cultureId = entry.getKey();
                Culture culture = entry.getValue();
                int repValue = cultureRep.get(serverPlayer.getUUID(), cultureId);
                List<ReputationLabel> labels = ModCultures.getCultureReputationLabels(cultureId);
                if (labels == null) {
                    labels = ModCultures.getReputationLabels(cultureId);
                }
                if ((repLabel = VillageReputation.getLabel(repValue, labels)) == null) {
                    repLabel = "reputation.neutral";
                }
                int languageScore = cultureRep.getLanguageKnowledge(serverPlayer.getUUID(), cultureId);
                String cultureNameKey = "culture.millenaire." + cultureId.getPath();
                entries.add(new InfoPanelContentPayload.CultureEntry(cultureNameKey, repValue, repLabel, languageScore));
            }
            PacketDistributor.sendToPlayer((ServerPlayer)serverPlayer, (CustomPacketPayload)new InfoPanelContentPayload(entries), (CustomPacketPayload[])new CustomPacketPayload[0]);
            LOGGER.debug("InfoPanel payload sent to {} with {} cultures", (Object)serverPlayer.getName().getString(), (Object)entries.size());
        });
    }
}

