/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 *  net.minecraft.network.chat.Component
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  org.slf4j.Logger
 */
package org.millenaire.quest;

import com.mojang.logging.LogUtils;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.millenaire.quest.QuestInstance;
import org.millenaire.quest.QuestInstanceVillager;
import org.millenaire.quest.QuestStep;
import org.millenaire.quest.QuestTextRegistry;
import org.millenaire.village.Village;
import org.millenaire.village.VillageId;
import org.millenaire.village.VillageSavedData;
import org.millenaire.village.VillagerRecord;
import org.millenaire.world.VillageNotifier;
import org.slf4j.Logger;

public final class QuestTextRenderer {
    private static final Logger LOGGER = LogUtils.getLogger();

    private QuestTextRenderer() {
    }

    public static String playerLocale(ServerPlayer player) {
        String prefix;
        String mcLocale = player.clientInformation().language();
        if (mcLocale == null || mcLocale.isEmpty()) {
            return "en";
        }
        if (QuestTextRegistry.hasLanguage(mcLocale)) {
            return mcLocale;
        }
        String string = prefix = mcLocale.length() >= 2 ? mcLocale.substring(0, 2) : mcLocale;
        if (QuestTextRegistry.hasLanguage(prefix)) {
            return prefix;
        }
        return "en";
    }

    public static String substitute(String text, QuestInstance questInstance, String playerName, ServerLevel overworld) {
        if (text == null) {
            return "";
        }
        QuestStep step = questInstance.getCurrentStep();
        if (step == null) {
            return text;
        }
        QuestInstanceVillager giverQiv = questInstance.getVillagers().get(step.villagerKey());
        if (giverQiv == null) {
            return text;
        }
        Village giverVillage = Village.resolve(overworld, new VillageId(giverQiv.getVillageId()));
        if (giverVillage == null) {
            return text;
        }
        BlockPos giverPos = giverVillage.getCenter();
        String s = text;
        VillageSavedData savedData = VillageSavedData.get(overworld);
        for (Map.Entry<String, QuestInstanceVillager> entry : questInstance.getVillagers().entrySet()) {
            String key = entry.getKey();
            QuestInstanceVillager qiv = entry.getValue();
            Village th = Village.resolve(overworld, new VillageId(qiv.getVillageId()));
            if (th == null) continue;
            BlockPos thPos = th.getCenter();
            s = s.replace("$" + key + "_villagename$", QuestTextRenderer.getVillageQualifiedName(th));
            s = s.replace("$" + key + "_direction$", VillageNotifier.cardinalDirection(giverPos, thPos));
            s = s.replace("$" + key + "_tothedirection$", QuestTextRenderer.toTheDirection(giverPos, thPos));
            s = s.replace("$" + key + "_directionshort$", QuestTextRenderer.directionShort(giverPos, thPos));
            s = s.replace("$" + key + "_distance$", QuestTextRenderer.approximateDistanceLong(giverPos, thPos));
            s = s.replace("$" + key + "_distanceshort$", QuestTextRenderer.approximateDistanceShort(giverPos, thPos));
            VillagerRecord villager = QuestTextRenderer.findVillagerRecord(savedData, qiv.getVillagerId());
            if (villager != null) {
                s = s.replace("$" + key + "_villagername$", villager.getFirstName());
                s = s.replace("$" + key + "_villagerrole$", QuestTextRenderer.resolveGameOccupation(villager));
            }
            for (Map.Entry<String, QuestInstanceVillager> entry2 : questInstance.getVillagers().entrySet()) {
                String key2 = entry2.getKey();
                QuestInstanceVillager qiv2 = entry2.getValue();
                Village th2 = Village.resolve(overworld, new VillageId(qiv2.getVillageId()));
                if (th2 != null) {
                    BlockPos th2Pos = th2.getCenter();
                    s = s.replace("$" + key + "_" + key2 + "_direction$", VillageNotifier.cardinalDirection(thPos, th2Pos));
                    s = s.replace("$" + key + "_" + key2 + "_directionshort$", QuestTextRenderer.directionShort(thPos, th2Pos));
                    s = s.replace("$" + key + "_" + key2 + "_distance$", QuestTextRenderer.approximateDistanceLong(thPos, th2Pos));
                    s = s.replace("$" + key + "_" + key2 + "_distanceshort$", QuestTextRenderer.approximateDistanceShort(thPos, th2Pos));
                    continue;
                }
                s = s.replace("$" + key + "_" + key2 + "_direction$", "");
                s = s.replace("$" + key + "_" + key2 + "_directionshort$", "");
                s = s.replace("$" + key + "_" + key2 + "_distance$", "");
                s = s.replace("$" + key + "_" + key2 + "_distanceshort$", "");
            }
        }
        s = s.replace("$name", playerName);
        return s;
    }

    public static String lookupText(String textKey, String locale, @Nullable String fallback) {
        String text = QuestTextRegistry.getText(locale, textKey);
        if (text != null) {
            return text;
        }
        if (fallback != null && !fallback.isEmpty()) {
            return fallback;
        }
        return "";
    }

    private static String toTheDirection(BlockPos from, BlockPos to) {
        int xdist = to.getX() - from.getX();
        int zdist = to.getZ() - from.getZ();
        String prefix = "other.millenaire.tothe";
        String direction = QuestTextRenderer.computeDirectionKey(xdist, zdist, prefix);
        return Component.translatable((String)direction).getString();
    }

    private static String directionShort(BlockPos from, BlockPos to) {
        Object direction;
        boolean diagonal;
        int xdist = to.getX() - from.getX();
        int zdist = to.getZ() - from.getZ();
        boolean bl = diagonal = (double)Math.abs(xdist) > (double)Math.abs(zdist) * 0.6 && (double)Math.abs(xdist) < (double)Math.abs(zdist) * 1.4 || (double)Math.abs(zdist) > (double)Math.abs(xdist) * 0.6 && (double)Math.abs(zdist) < (double)Math.abs(xdist) * 1.4;
        if (diagonal) {
            String ns = zdist > 0 ? Component.translatable((String)"other.millenaire.south_short").getString() : Component.translatable((String)"other.millenaire.north_short").getString();
            String ew = xdist > 0 ? Component.translatable((String)"other.millenaire.east_short").getString() : Component.translatable((String)"other.millenaire.west_short").getString();
            direction = ns + ew;
        } else {
            direction = Math.abs(xdist) > Math.abs(zdist) ? (xdist > 0 ? Component.translatable((String)"other.millenaire.east_short").getString() : Component.translatable((String)"other.millenaire.west_short").getString()) : (zdist > 0 ? Component.translatable((String)"other.millenaire.south_short").getString() : Component.translatable((String)"other.millenaire.north_short").getString());
        }
        return direction;
    }

    private static String approximateDistanceLong(BlockPos from, BlockPos to) {
        int dist = VillageNotifier.horizontalDistance(from, to);
        if (dist < 950) {
            return dist / 100 * 100 + " " + Component.translatable((String)"other.millenaire.metre").getString();
        }
        if ((dist = (dist + 500) / 1000) % 2 == 0) {
            return dist / 2 + " " + Component.translatable((String)"other.millenaire.kilometre").getString();
        }
        return (dist - 1) / 2 + Component.translatable((String)"other.millenaire.andhalf").getString() + " " + Component.translatable((String)"other.millenaire.kilometre").getString();
    }

    private static String approximateDistanceShort(BlockPos from, BlockPos to) {
        int dist = VillageNotifier.horizontalDistance(from, to);
        if (dist < 950) {
            return dist / 100 * 100 + "m";
        }
        if ((dist = (dist + 500) / 1000) % 2 == 0) {
            return dist / 2 + " km";
        }
        return (dist - 1) / 2 + ".5 km";
    }

    private static String computeDirectionKey(int xdist, int zdist, String prefix) {
        String direction;
        boolean diagonal;
        boolean bl = diagonal = (double)Math.abs(xdist) > (double)Math.abs(zdist) * 0.6 && (double)Math.abs(xdist) < (double)Math.abs(zdist) * 1.4 || (double)Math.abs(zdist) > (double)Math.abs(xdist) * 0.6 && (double)Math.abs(zdist) < (double)Math.abs(xdist) * 1.4;
        if (diagonal) {
            String ns = zdist > 0 ? prefix + "south" : prefix + "north";
            String ew = xdist > 0 ? "east" : "west";
            direction = ns + "-" + ew;
        } else {
            direction = Math.abs(xdist) > Math.abs(zdist) ? (xdist > 0 ? prefix + "east" : prefix + "west") : (zdist > 0 ? prefix + "south" : prefix + "north");
        }
        return direction;
    }

    private static String getVillageQualifiedName(Village village) {
        String name = village.getVillageName();
        return name != null ? name : "?";
    }

    private static String resolveGameOccupation(VillagerRecord vr) {
        String roleName = vr.getRoleName();
        if (roleName != null && !roleName.isEmpty()) {
            if (roleName.startsWith("role.")) {
                return Component.translatable((String)roleName).getString();
            }
            return roleName;
        }
        return "";
    }

    @Nullable
    private static VillagerRecord findVillagerRecord(VillageSavedData savedData, UUID villagerId) {
        for (Village village : savedData.getVillageManager().getAllVillages()) {
            VillagerRecord vr = village.getVillagerRecord(villagerId);
            if (vr == null) continue;
            return vr;
        }
        return null;
    }
}

