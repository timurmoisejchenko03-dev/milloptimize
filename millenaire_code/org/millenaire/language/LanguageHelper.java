/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.level.Level
 */
package org.millenaire.language;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.millenaire.config.MillenaireServerConfig;
import org.millenaire.village.PlayerCultureReputation;

public final class LanguageHelper {
    private LanguageHelper() {
    }

    public static boolean canReadBuildingNames(ServerPlayer player, ResourceLocation cultureId) {
        if (!((Boolean)MillenaireServerConfig.SERVER.languageLearning.get()).booleanValue()) {
            return true;
        }
        return LanguageHelper.getLanguageKnowledge(player, cultureId) >= 100;
    }

    public static boolean canReadVillagerNames(ServerPlayer player, ResourceLocation cultureId) {
        if (!((Boolean)MillenaireServerConfig.SERVER.languageLearning.get()).booleanValue()) {
            return true;
        }
        return LanguageHelper.getLanguageKnowledge(player, cultureId) >= 200;
    }

    public static boolean canReadDialogues(ServerPlayer player, ResourceLocation cultureId) {
        if (!((Boolean)MillenaireServerConfig.SERVER.languageLearning.get()).booleanValue()) {
            return true;
        }
        return LanguageHelper.getLanguageKnowledge(player, cultureId) >= 500;
    }

    public static int getLanguageKnowledge(ServerPlayer player, ResourceLocation cultureId) {
        Level level = player.level();
        if (level instanceof ServerLevel) {
            ServerLevel sl = (ServerLevel)level;
            return PlayerCultureReputation.get(sl).getLanguageKnowledge(player.getUUID(), cultureId);
        }
        return 0;
    }

    public static PlayerCultureReputation.LanguageLevel getLanguageLevel(ServerPlayer player, ResourceLocation cultureId) {
        Level level = player.level();
        if (level instanceof ServerLevel) {
            ServerLevel sl = (ServerLevel)level;
            return PlayerCultureReputation.get(sl).getLanguageLevel(player.getUUID(), cultureId);
        }
        return PlayerCultureReputation.LanguageLevel.MINIMAL;
    }
}

