/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 */
package org.millenaire.client;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import org.millenaire.network.QuestInstanceSyncPayload;

public final class ClientQuestCache {
    private static final Map<Long, CachedQuest> quests = new ConcurrentHashMap<Long, CachedQuest>();

    private ClientQuestCache() {
    }

    public static void update(QuestInstanceSyncPayload payload) {
        UUID stepVillagerId = null;
        if (payload.currentStepVillagerId() != null && !payload.currentStepVillagerId().isEmpty()) {
            try {
                stepVillagerId = UUID.fromString(payload.currentStepVillagerId());
            }
            catch (IllegalArgumentException illegalArgumentException) {
                // empty catch block
            }
        }
        quests.put(payload.uniqueId(), new CachedQuest(payload.uniqueId(), payload.questKey(), payload.currentStep(), payload.startTime(), payload.currentStepStart(), payload.villagers(), payload.displayLabel() != null ? payload.displayLabel() : payload.questKey(), stepVillagerId));
    }

    public static void remove(long uniqueId) {
        quests.remove(uniqueId);
    }

    public static void clear() {
        quests.clear();
    }

    @Nullable
    public static String getQuestLabelForVillager(UUID villagerId) {
        for (CachedQuest quest : quests.values()) {
            if (quest.currentStepVillagerId == null || !quest.currentStepVillagerId.equals(villagerId)) continue;
            return quest.displayLabel;
        }
        return null;
    }

    @Nullable
    public static CachedQuest getQuest(long uniqueId) {
        return quests.get(uniqueId);
    }

    @Nullable
    public static CachedQuest getQuestForVillager(UUID villagerId) {
        for (CachedQuest quest : quests.values()) {
            for (QuestInstanceSyncPayload.VillagerData vd : quest.villagers.values()) {
                if (!vd.villagerId().equals(villagerId)) continue;
                return quest;
            }
        }
        return null;
    }

    public record CachedQuest(long uniqueId, String questKey, int currentStep, long startTime, long currentStepStart, Map<String, QuestInstanceSyncPayload.VillagerData> villagers, String displayLabel, @Nullable UUID currentStepVillagerId) {
    }
}

