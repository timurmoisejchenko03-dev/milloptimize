/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  javax.annotation.Nullable
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.protocol.common.custom.CustomPacketPayload
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.level.Level
 *  net.neoforged.neoforge.network.PacketDistributor
 *  org.slf4j.Logger
 */
package org.millenaire.village;

import com.mojang.logging.LogUtils;
import java.util.concurrent.ThreadLocalRandom;
import java.util.random.RandomGenerator;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import org.millenaire.config.MillenaireServerConfig;
import org.millenaire.culture.Gender;
import org.millenaire.culture.ModCultures;
import org.millenaire.culture.VillagerType;
import org.millenaire.dialogue.SentenceLoader;
import org.millenaire.entity.MillVillager;
import org.millenaire.network.SpeechChatPayload;
import org.millenaire.village.PlayerCultureReputation;
import org.slf4j.Logger;

public final class VillagerAnnouncementHelper {
    private static final Logger LOGGER = LogUtils.getLogger();

    private VillagerAnnouncementHelper() {
    }

    @Nullable
    public static String resolveSpeechRef(ResourceLocation cultureId, String role, @Nullable String gender, String goalKey, RandomGenerator rng) {
        String effectiveRole = SentenceLoader.resolveEffectiveRole(cultureId, "native", role, gender, goalKey);
        if (effectiveRole == null) {
            return null;
        }
        int count = SentenceLoader.countVariants(cultureId, "native", effectiveRole, goalKey);
        if (count == 0) {
            return null;
        }
        int idx = rng.nextInt(count);
        return "s:" + cultureId.getPath() + ":" + effectiveRole + ":" + goalKey + ":" + idx;
    }

    public static void sendAnnouncement(MillVillager villager, ServerPlayer targetPlayer, String goalKey, String vanillaFallbackKey) {
        int langScore;
        String villagerName = villager.getVillagerDisplayName();
        ResourceLocation villagerTypeId = villager.getVillagerTypeId();
        if (villagerTypeId == null) {
            VillagerAnnouncementHelper.sendVanillaFallback(targetPlayer, vanillaFallbackKey, villagerName);
            return;
        }
        ResourceLocation cultureId = ModCultures.extractCultureId(villagerTypeId);
        VillagerType vtype = ModCultures.getVillagerType(villagerTypeId);
        if (ModCultures.getCulture(cultureId) == null || vtype == null) {
            VillagerAnnouncementHelper.sendVanillaFallback(targetPlayer, vanillaFallbackKey, villagerName);
            return;
        }
        String vtPath = villagerTypeId.getPath();
        int slashIdx = vtPath.indexOf(47);
        String role = slashIdx > 0 ? vtPath.substring(slashIdx + 1) : vtPath;
        String gender = switch (vtype.gender()) {
            default -> throw new MatchException(null, null);
            case Gender.MALE -> "male";
            case Gender.FEMALE -> "female";
        };
        String speechRef = VillagerAnnouncementHelper.resolveSpeechRef(cultureId, role, gender, goalKey, ThreadLocalRandom.current());
        if (speechRef == null) {
            VillagerAnnouncementHelper.sendVanillaFallback(targetPlayer, vanillaFallbackKey, villagerName);
            return;
        }
        if (!((Boolean)MillenaireServerConfig.SERVER.languageLearning.get()).booleanValue()) {
            langScore = Integer.MAX_VALUE;
        } else {
            Level level = targetPlayer.level();
            if (level instanceof ServerLevel) {
                ServerLevel sl = (ServerLevel)level;
                langScore = PlayerCultureReputation.get(sl).getLanguageKnowledge(targetPlayer.getUUID(), cultureId);
            } else {
                langScore = 0;
            }
        }
        SpeechChatPayload payload = new SpeechChatPayload(villagerName != null ? villagerName : "", speechRef, cultureId.getPath(), langScore, vanillaFallbackKey);
        PacketDistributor.sendToPlayer((ServerPlayer)targetPlayer, (CustomPacketPayload)payload, (CustomPacketPayload[])new CustomPacketPayload[0]);
        LOGGER.debug("[Millenaire] Announcement {} sent to {} (ref: {})", new Object[]{goalKey, targetPlayer.getName().getString(), speechRef});
    }

    private static void sendVanillaFallback(ServerPlayer targetPlayer, String vanillaFallbackKey, @Nullable String villagerName) {
        if (vanillaFallbackKey == null || vanillaFallbackKey.isEmpty()) {
            return;
        }
        targetPlayer.sendSystemMessage((Component)Component.translatable((String)vanillaFallbackKey, (Object[])new Object[]{villagerName != null ? villagerName : ""}));
    }
}

