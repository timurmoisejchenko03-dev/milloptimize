/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.ChatFormatting
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.chat.MutableComponent
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.phys.AABB
 */
package org.millenaire.village;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import org.millenaire.village.PlayerCultureReputation;
import org.millenaire.village.Village;
import org.millenaire.village.VillageId;
import org.millenaire.village.VillageRelations;

public final class VillageDiplomacyHelper {
    private VillageDiplomacyHelper() {
    }

    public static void performNightlyDiplomacyDrift(ServerLevel level, Village village) {
        if (village.isPlayerControlled()) {
            return;
        }
        if (village.isLoneBuilding()) {
            return;
        }
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        for (Map.Entry<VillageId, Integer> entry : village.getRelations().entrySet()) {
            if (((Random)rng).nextInt(10) != 0) continue;
            int relation = entry.getValue();
            VillageId otherId = entry.getKey();
            boolean improve = relation < -90 ? false : (relation < -50 ? ((Random)rng).nextInt(100) < 30 : (relation < 0 ? ((Random)rng).nextInt(100) < 40 : (relation > 90 ? true : (relation > 50 ? ((Random)rng).nextInt(100) < 70 : ((Random)rng).nextInt(100) < 60))));
            int change = 10 + ((Random)rng).nextInt(10);
            if (improve) {
                if (relation >= 100) continue;
                village.adjustRelationSymmetric(level, otherId, change, false);
                VillageDiplomacyHelper.notifyNearbyPlayersDiplomacy(level, village, otherId, true);
                continue;
            }
            if (relation <= -100) continue;
            village.adjustRelationSymmetric(level, otherId, -change, false);
            VillageDiplomacyHelper.notifyNearbyPlayersDiplomacy(level, village, otherId, false);
        }
    }

    static void notifyNearbyPlayersDiplomacy(ServerLevel level, Village village, VillageId otherId, boolean improving) {
        ChatFormatting color;
        MutableComponent msg;
        Village other = Village.resolve(level, otherId);
        if (other == null) {
            return;
        }
        String thisName = village.getVillageName() != null ? village.getVillageName() : village.getVillageTypeId().getPath();
        String otherName = other.getVillageName() != null ? other.getVillageName() : other.getVillageTypeId().getPath();
        int newRelation = village.getRelation(otherId);
        String relationKey = VillageRelations.getRelationKey(newRelation);
        if (improving) {
            msg = Component.translatable((String)"millenaire.diplomacy.improving", (Object[])new Object[]{thisName, otherName, Component.translatable((String)relationKey)});
            color = ChatFormatting.GREEN;
        } else {
            msg = Component.translatable((String)"millenaire.diplomacy.worsening", (Object[])new Object[]{thisName, otherName, Component.translatable((String)relationKey)});
            color = ChatFormatting.GOLD;
        }
        int radius = Village.getKeepActiveRadius();
        AABB area = new AABB((double)(village.getCenter().getX() - radius), (double)(village.getCenter().getY() - 256), (double)(village.getCenter().getZ() - radius), (double)(village.getCenter().getX() + radius), (double)(village.getCenter().getY() + 256), (double)(village.getCenter().getZ() + radius));
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, area)) {
            player.sendSystemMessage((Component)msg.copy().withStyle(color));
        }
    }

    public static void regenerateDiplomacyPointsForPlayers(ServerLevel level, Village village) {
        if (village.isPlayerControlled()) {
            return;
        }
        if (village.isLoneBuilding()) {
            return;
        }
        PlayerCultureReputation rep = PlayerCultureReputation.get(level);
        for (ServerPlayer player : level.players()) {
            rep.regenerateDiplomacyPoints(player.getUUID(), village.getId());
        }
    }
}

