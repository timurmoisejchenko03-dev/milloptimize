/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 */
package org.millenaire.village;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.millenaire.village.PlayerCultureReputation;
import org.millenaire.village.Village;
import org.millenaire.village.VillageId;

public final class DiplomacyHelper {
    private static final int MAX_REP = 32768;

    private DiplomacyHelper() {
    }

    public static void performDiplomacy(ServerLevel level, ServerPlayer player, Village village, VillageId targetVillageId, boolean isPraise) {
        Village target = Village.resolve(level, targetVillageId);
        if (target == null) {
            return;
        }
        if (target.getId().equals(village.getId())) {
            return;
        }
        PlayerCultureReputation rep = PlayerCultureReputation.get(level);
        int playerRep = village.getCombinedReputation(level, player.getUUID());
        if (playerRep <= 0) {
            return;
        }
        if (rep.getDiplomacyPoints(player.getUUID(), village.getId()) <= 0) {
            return;
        }
        int reputation = Math.min(playerRep, 32768);
        float effect = isPraise ? 10.0f : -10.0f;
        float coeff = (float)((Math.log(reputation) / Math.log(32768.0) * 2.0 + (double)(reputation / 32768)) / 3.0);
        effect *= coeff;
        village.adjustRelationSymmetric(level, targetVillageId, (int)(effect *= (float)(80 + level.random.nextInt(40)) / 100.0f), false);
        rep.consumeDiplomacyPoint(player.getUUID(), village.getId());
    }
}

