/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.level.Level
 */
package org.millenaire.network;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.millenaire.network.QuestInstanceSyncPayload;
import org.millenaire.quest.QuestInstance;
import org.millenaire.quest.QuestInstanceVillager;
import org.millenaire.quest.QuestStep;
import org.millenaire.quest.QuestTextRenderer;

public final class QuestNetworkHelper {
    private QuestNetworkHelper() {
    }

    public static QuestInstanceSyncPayload buildSyncPayload(QuestInstance qi, ServerPlayer player) {
        HashMap<String, QuestInstanceSyncPayload.VillagerData> villagerData = new HashMap<String, QuestInstanceSyncPayload.VillagerData>();
        for (Map.Entry<String, QuestInstanceVillager> entry : qi.getVillagers().entrySet()) {
            villagerData.put(entry.getKey(), new QuestInstanceSyncPayload.VillagerData(entry.getValue().getVillagerId(), entry.getValue().getVillageId()));
        }
        String displayLabel = QuestNetworkHelper.resolveStepLabel(qi, player);
        UUID stepVillagerId = qi.getCurrentStepVillagerId();
        String stepVillagerIdStr = stepVillagerId != null ? stepVillagerId.toString() : "";
        return new QuestInstanceSyncPayload(qi.getUniqueId(), qi.getQuest().key(), qi.getCurrentStepIndex(), qi.getStartTime(), qi.getCurrentStepStart(), villagerData, displayLabel, stepVillagerIdStr);
    }

    private static String resolveStepLabel(QuestInstance qi, ServerPlayer player) {
        QuestStep step = qi.getCurrentStep();
        if (step == null) {
            return qi.getQuest().key();
        }
        String locale = QuestTextRenderer.playerLocale(player);
        String labelKey = qi.getQuest().key() + "_" + qi.getCurrentStepIndex() + "_label";
        String inlineLabel = step.labels().getOrDefault(locale, step.labels().getOrDefault("en", qi.getQuest().key()));
        String label = QuestTextRenderer.lookupText(labelKey, locale, inlineLabel);
        ServerLevel overworld = player.server.getLevel(Level.OVERWORLD);
        if (overworld == null) {
            return label;
        }
        return QuestTextRenderer.substitute(label, qi, player.getName().getString(), overworld);
    }
}

