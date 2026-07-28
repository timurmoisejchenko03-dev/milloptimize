/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 */
package org.millenaire.quest;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.Nullable;
import org.millenaire.quest.Quest;

public final class QuestRegistry {
    private static final Map<String, Quest> QUESTS = new LinkedHashMap<String, Quest>();

    private QuestRegistry() {
    }

    public static void clear() {
        QUESTS.clear();
    }

    public static void register(Quest q) {
        QUESTS.put(q.key(), q);
    }

    @Nullable
    public static Quest get(String key) {
        return QUESTS.get(key);
    }

    public static Collection<Quest> all() {
        return Collections.unmodifiableCollection(QUESTS.values());
    }

    public static int size() {
        return QUESTS.size();
    }
}

