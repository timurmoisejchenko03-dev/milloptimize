/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 */
package org.millenaire.quest;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;

public final class QuestTextRegistry {
    private static final Map<String, Map<String, String>> TEXTS = new HashMap<String, Map<String, String>>();

    private QuestTextRegistry() {
    }

    public static void clear() {
        TEXTS.clear();
    }

    public static void register(String lang, Map<String, String> texts) {
        TEXTS.put(lang, texts);
    }

    @Nullable
    public static String getText(String lang, String key) {
        Map<String, String> enTexts;
        String text;
        Map<String, String> langTexts = TEXTS.get(lang);
        if (langTexts != null && (text = langTexts.get(key)) != null) {
            return text;
        }
        if (!"en".equals(lang) && (enTexts = TEXTS.get("en")) != null) {
            return enTexts.get(key);
        }
        return null;
    }

    public static Map<String, String> getTexts(String lang) {
        Map<String, String> langTexts = TEXTS.get(lang);
        return langTexts != null ? Collections.unmodifiableMap(langTexts) : Collections.emptyMap();
    }

    public static boolean hasLanguage(String lang) {
        return TEXTS.containsKey(lang);
    }

    public static int languageCount() {
        return TEXTS.size();
    }
}

