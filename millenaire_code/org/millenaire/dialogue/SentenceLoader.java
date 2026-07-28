/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  javax.annotation.Nullable
 *  net.minecraft.resources.ResourceLocation
 *  org.slf4j.Logger
 */
package org.millenaire.dialogue;

import com.mojang.logging.LogUtils;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import org.millenaire.content.ContentFs;
import org.millenaire.content.CustomContentIndex;
import org.millenaire.content.Resource;
import org.millenaire.content.SourceKind;
import org.slf4j.Logger;

public final class SentenceLoader {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<String, Map<ResourceLocation, Map<String, List<String>>>> ALL_SENTENCES = new ConcurrentHashMap<String, Map<ResourceLocation, Map<String, List<String>>>>();

    private SentenceLoader() {
    }

    public static void loadSentences(String cultureName, String lang) {
        ResourceLocation cultureId = ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)cultureName);
        String relPath = lang + "/" + cultureName + "_sentences.txt";
        ContentFs languagesFs = CustomContentIndex.current().forGlobalContent("languages");
        List<Resource> all = languagesFs.findAll(relPath);
        Resource base = null;
        ArrayList<Resource> overlays = new ArrayList<Resource>();
        for (Resource res : all) {
            if (res.kind() == SourceKind.SUBMOD) {
                overlays.add(res);
                continue;
            }
            if (base != null) continue;
            base = res;
        }
        Collections.reverse(overlays);
        int baseCount = 0;
        int externalCount = 0;
        if (base != null) {
            try (InputStream is = base.open();){
                baseCount = SentenceLoader.parseAndMerge(cultureId, lang, is);
            }
            catch (Exception e) {
                LOGGER.error("Error loading sentences {}: {}", (Object)base.relPath(), (Object)e.getMessage());
            }
        } else {
            String classpathPath = "/millenaire/languages/" + relPath;
            try (InputStream is = SentenceLoader.class.getResourceAsStream(classpathPath);){
                if (is != null) {
                    baseCount = SentenceLoader.parseAndMerge(cultureId, lang, is);
                } else {
                    LOGGER.debug("No sentence file found: languages/{}", (Object)relPath);
                }
            }
            catch (Exception e) {
                LOGGER.error("Error loading sentences {}: {}", (Object)classpathPath, (Object)e.getMessage());
            }
        }
        for (Resource ext : overlays) {
            try {
                InputStream extIs = ext.open();
                try {
                    externalCount += SentenceLoader.parseAndMerge(cultureId, lang, extIs);
                }
                finally {
                    if (extIs == null) continue;
                    extIs.close();
                }
            }
            catch (Exception e) {
                LOGGER.error("Error loading external sentences {}: {}", (Object)ext.relPath(), (Object)e.getMessage());
            }
        }
        if (baseCount + externalCount > 0) {
            if (externalCount > 0) {
                LOGGER.info("Sentences loaded for {} ({}): {} phrases (base={}, custom={})", new Object[]{cultureName, lang, baseCount + externalCount, baseCount, externalCount});
            } else {
                LOGGER.debug("Sentences loaded for {} ({}): {} phrases", new Object[]{cultureName, lang, baseCount});
            }
        }
    }

    private static int parseAndMerge(ResourceLocation cultureId, String lang, InputStream is) throws IOException {
        Map byCulture = ALL_SENTENCES.computeIfAbsent(lang, k -> new ConcurrentHashMap());
        Map byKey = byCulture.computeIfAbsent(cultureId, k -> new ConcurrentHashMap());
        int count = 0;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));){
            String line;
            while ((line = reader.readLine()) != null) {
                String goalKey;
                String role;
                String compositeKey;
                List texts;
                int eqIdx;
                if ((line = line.trim()).isEmpty() || line.startsWith("//") || (eqIdx = line.indexOf(61)) <= 0) continue;
                String key = line.substring(0, eqIdx).trim();
                String text = line.substring(eqIdx + 1).trim();
                int dotIdx = key.indexOf(46);
                if (dotIdx <= 0 || (texts = byKey.computeIfAbsent(compositeKey = (role = key.substring(0, dotIdx).toLowerCase()) + "." + (goalKey = key.substring(dotIdx + 1).toLowerCase()), k -> new ArrayList())).contains(text)) continue;
                texts.add(text);
                ++count;
            }
        }
        return count;
    }

    public static Set<String> getLoadedLanguages() {
        return Set.copyOf(ALL_SENTENCES.keySet());
    }

    public static List<String> getSentences(ResourceLocation culture, String lang, String role, String goalKey) {
        return SentenceLoader.getSentences(culture, lang, role, null, goalKey);
    }

    public static List<String> getSentences(ResourceLocation culture, String lang, String role, @Nullable String gender, String goalKey) {
        Map<String, List<String>> byCulture = SentenceLoader.getCultureMap(culture, lang);
        if (byCulture == null) {
            return List.of();
        }
        String key = SentenceLoader.resolveCompositeKey(byCulture, role, gender, goalKey);
        return key != null ? byCulture.get(key) : List.of();
    }

    @Nullable
    public static String resolveEffectiveRole(ResourceLocation culture, String lang, String role, @Nullable String gender, String goalKey) {
        Map<String, List<String>> byCulture = SentenceLoader.getCultureMap(culture, lang);
        if (byCulture == null) {
            return null;
        }
        String key = SentenceLoader.resolveCompositeKey(byCulture, role, gender, goalKey);
        return key != null ? key.substring(0, key.indexOf(46)) : null;
    }

    public static int countVariants(ResourceLocation culture, String lang, String role, String goalKey) {
        Map<String, List<String>> byCulture = SentenceLoader.getCultureMap(culture, lang);
        if (byCulture == null) {
            return 0;
        }
        List<String> texts = byCulture.get(role + "." + goalKey);
        return texts != null ? texts.size() : 0;
    }

    @Nullable
    public static String getSentence(ResourceLocation culture, String lang, String role, String goalKey, int idx) {
        Map<String, List<String>> byCulture = SentenceLoader.getCultureMap(culture, lang);
        if (byCulture == null) {
            return null;
        }
        List<String> texts = byCulture.get(role + "." + goalKey);
        if (texts == null || idx < 0 || idx >= texts.size()) {
            return null;
        }
        return texts.get(idx);
    }

    @Nullable
    private static Map<String, List<String>> getCultureMap(ResourceLocation culture, String lang) {
        Map<ResourceLocation, Map<String, List<String>>> byLang = ALL_SENTENCES.get(lang);
        if (byLang == null) {
            return null;
        }
        return byLang.get(culture);
    }

    @Nullable
    private static String resolveCompositeKey(Map<String, List<String>> byCulture, String role, @Nullable String gender, String goalKey) {
        String key = role + "." + goalKey;
        List<String> result = byCulture.get(key);
        if (result != null && !result.isEmpty()) {
            return key;
        }
        if (!(gender == null || gender.equals(role) || "villager".equals(gender) || (result = byCulture.get(key = gender + "." + goalKey)) == null || result.isEmpty())) {
            return key;
        }
        if (!"villager".equals(role) && (result = byCulture.get(key = "villager." + goalKey)) != null && !result.isEmpty()) {
            return key;
        }
        return null;
    }

    public static void clear() {
        ALL_SENTENCES.clear();
    }
}

