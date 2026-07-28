/*
 * Decompiled with CFR 0.152.
 */
package org.millenaire.quest;

import java.util.List;
import org.millenaire.quest.QuestStep;
import org.millenaire.quest.QuestVillagerDef;

public record Quest(String key, double chancePerHour, int maxSimultaneous, int minReputation, List<String> globalTagsRequired, List<String> globalTagsForbidden, List<String> playerTagsRequired, List<String> playerTagsForbidden, List<QuestVillagerDef> villagerDefs, List<QuestStep> steps) {
}

