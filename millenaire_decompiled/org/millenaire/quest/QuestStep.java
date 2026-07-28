/*
 * Decompiled with CFR 0.150.
 */
package org.millenaire.quest;

import java.util.List;
import java.util.Map;
import org.millenaire.quest.ActionDataEntry;
import org.millenaire.quest.BedrockBuilding;
import org.millenaire.quest.QuestItemRef;
import org.millenaire.quest.RelationChange;
import org.millenaire.quest.VillagerTagAction;

public record QuestStep(int index, String villagerKey, int duration, boolean showRequiredGoods, Map<QuestItemRef, Integer> requiredGoods, Map<QuestItemRef, Integer> rewardGoods, int rewardMoney, int rewardReputation, int penaltyReputation, List<VillagerTagAction> villagerTagsSuccess, List<VillagerTagAction> villagerTagsFailure, List<String> playerTagsSuccess, List<String> playerTagsFailure, List<String> globalTagsSuccess, List<String> globalTagsFailure, List<VillagerTagAction> clearTagsSuccess, List<VillagerTagAction> clearTagsFailure, List<String> clearPlayerTagsSuccess, List<String> clearPlayerTagsFailure, List<String> clearGlobalTagsSuccess, List<String> clearGlobalTagsFailure, List<String> stepRequiredGlobalTags, List<String> stepForbiddenGlobalTags, List<String> stepRequiredPlayerTags, List<String> stepForbiddenPlayerTags, List<ActionDataEntry> actionDataSuccess, List<RelationChange> relationChanges, List<BedrockBuilding> bedrockBuildings, Map<String, String> labels, Map<String, String> descriptions, Map<String, String> descriptionsSuccess, Map<String, String> descriptionsRefuse, Map<String, String> descriptionsTimeUp, Map<String, String> listings) {
}

