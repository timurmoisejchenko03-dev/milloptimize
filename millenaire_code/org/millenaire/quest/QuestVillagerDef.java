/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 */
package org.millenaire.quest;

import java.util.List;
import javax.annotation.Nullable;

public record QuestVillagerDef(String key, List<String> villagerTypes, @Nullable String relatedTo, @Nullable String relation, List<String> requiredTags, List<String> forbiddenTags) {
}

