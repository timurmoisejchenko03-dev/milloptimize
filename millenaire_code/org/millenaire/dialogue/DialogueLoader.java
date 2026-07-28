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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.invoke.LambdaMetafactory;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import org.millenaire.content.ContentFs;
import org.millenaire.content.CustomContentIndex;
import org.millenaire.content.Resource;
import org.millenaire.content.SourceKind;
import org.millenaire.dialogue.Dialogue;
import org.slf4j.Logger;

public final class DialogueLoader {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<String, Map<ResourceLocation, Map<String, Dialogue>>> ALL_DIALOGUES = new ConcurrentHashMap<String, Map<ResourceLocation, Map<String, Dialogue>>>();
    private static final Set<String> WARNED_REPLACE_IDS = ConcurrentHashMap.newKeySet();

    private DialogueLoader() {
    }

    public static void loadDialogues(String cultureName, String lang) {
        ResourceLocation cultureId = ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)cultureName);
        String relPath = lang + "/" + cultureName + "_dialogues.txt";
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
        HashSet<String> overlayKeysFromPriorSubmods = new HashSet<String>();
        if (base != null) {
            try (InputStream is = base.open();){
                baseCount = DialogueLoader.parseDialogueStream(is, cultureId, lang, base.relPath(), false, null, null);
            }
            catch (Exception e) {
                LOGGER.error("Error loading dialogues {}: {}", (Object)base.relPath(), (Object)e.getMessage());
            }
        } else {
            String classpathPath = "/millenaire/languages/" + relPath;
            try (InputStream is = DialogueLoader.class.getResourceAsStream(classpathPath);){
                if (is != null) {
                    baseCount = DialogueLoader.parseDialogueStream(is, cultureId, lang, classpathPath, false, null, null);
                } else {
                    LOGGER.debug("No dialogue file found: languages/{}", (Object)relPath);
                }
            }
            catch (Exception e) {
                LOGGER.error("Error loading dialogues {}: {}", (Object)classpathPath, (Object)e.getMessage());
            }
        }
        int externalCount = 0;
        for (Resource ext : overlays) {
            Set<String> priorForThisFile = Set.copyOf(overlayKeysFromPriorSubmods);
            try {
                InputStream extIs = ext.open();
                try {
                    int added = DialogueLoader.parseDialogueStream(extIs, cultureId, lang, ext.source().displayName() + ":" + ext.relPath(), true, priorForThisFile, overlayKeysFromPriorSubmods);
                    externalCount += added;
                }
                finally {
                    if (extIs == null) continue;
                    extIs.close();
                }
            }
            catch (Exception e) {
                LOGGER.error("Error loading external dialogues {}: {}", (Object)ext.relPath(), (Object)e.getMessage());
            }
        }
        int total = baseCount + externalCount;
        if (total > 0) {
            if (externalCount > 0) {
                LOGGER.info("Dialogues loaded for {} ({}): {} conversations (base={}, custom new={})", new Object[]{cultureName, lang, total, baseCount, externalCount});
            } else {
                LOGGER.debug("Dialogues loaded for {} ({}): {} conversations", new Object[]{cultureName, lang, total});
            }
        }
    }

    /*
     * Unable to fully structure code
     */
    private static int parseDialogueStream(InputStream is, ResourceLocation cultureId, String lang, String sourceLabel, boolean isOverlay, @Nullable Set<String> priorSubmodKeys, @Nullable Set<String> recordNewKeysInto) {
        try {
            byCulture = DialogueLoader.ALL_DIALOGUES.computeIfAbsent(lang, (Function<String, Map>)LambdaMetafactory.metafactory(null, null, null, (Ljava/lang/Object;)Ljava/lang/Object;, lambda$parseDialogueStream$0(java.lang.String ), (Ljava/lang/String;)Ljava/util/Map;)());
            byKey = byCulture.computeIfAbsent(cultureId, (Function<ResourceLocation, Map>)LambdaMetafactory.metafactory(null, null, null, (Ljava/lang/Object;)Ljava/lang/Object;, lambda$parseDialogueStream$1(net.minecraft.resources.ResourceLocation ), (Lnet/minecraft/resources/ResourceLocation;)Ljava/util/Map;)());
            currentKey = null;
            currentWeight = 10;
            currentTags = new ArrayList<String>();
            v1Parts = new ArrayList<String>();
            v2Parts = new ArrayList<String>();
            currentLines = new ArrayList<Dialogue.Line>();
            currentBuildings = new ArrayList<String>();
            currentNotBuildings = new ArrayList<String>();
            currentVillagers = new ArrayList<String>();
            currentNotVillagers = new ArrayList<String>();
            currentRelations = new ArrayList<String>();
            currentNotRelations = new ArrayList<String>();
            count = 0;
            reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
lbl18:
            // 2 sources

            try {
                while ((line = reader.readLine()) != null) {
                    block23: {
                        if ((line = line.trim()).isEmpty() || line.startsWith("//")) continue;
                        if (!line.startsWith("newchat;")) break block23;
                        if (currentKey != null && !currentLines.isEmpty()) {
                            count += DialogueLoader.registerDialogue(byKey, currentKey, currentWeight, currentTags, v1Parts, v2Parts, currentLines, cultureId, currentBuildings, currentNotBuildings, currentVillagers, currentNotVillagers, currentRelations, currentNotRelations, lang, isOverlay, priorSubmodKeys, recordNewKeysInto, sourceLabel);
                        }
                        currentKey = null;
                        currentWeight = 10;
                        currentTags = new ArrayList<E>();
                        v1Parts = new ArrayList<E>();
                        v2Parts = new ArrayList<E>();
                        currentLines = new ArrayList<E>();
                        currentBuildings = new ArrayList<E>();
                        currentNotBuildings = new ArrayList<E>();
                        currentVillagers = new ArrayList<E>();
                        currentNotVillagers = new ArrayList<E>();
                        currentRelations = new ArrayList<E>();
                        currentNotRelations = new ArrayList<E>();
                        params = line.substring("newchat;".length());
                        if (params.endsWith(";")) {
                            params = params.substring(0, params.length() - 1);
                        }
                        for (String param : params.split(",")) {
                            if ((param = param.trim()).startsWith("key:")) {
                                currentKey = param.substring(4).toLowerCase();
                                continue;
                            }
                            if (param.startsWith("weigth:") || param.startsWith("weight:")) {
                                currentWeight = Integer.parseInt(param.substring(param.indexOf(58) + 1).trim());
                                continue;
                            }
                            if (param.startsWith("tag:")) {
                                currentTags.add(param.substring(4));
                                continue;
                            }
                            if (param.startsWith("v1:")) {
                                v1Parts.add(param.substring(3));
                                continue;
                            }
                            if (param.startsWith("v2:")) {
                                v2Parts.add(param.substring(3));
                                continue;
                            }
                            if (param.startsWith("notbuilding:")) {
                                currentNotBuildings.add(param.substring("notbuilding:".length()));
                                continue;
                            }
                            if (param.startsWith("building:")) {
                                currentBuildings.add(param.substring("building:".length()));
                                continue;
                            }
                            if (param.startsWith("notvillager:")) {
                                currentNotVillagers.add(param.substring("notvillager:".length()));
                                continue;
                            }
                            if (param.startsWith("villager:")) {
                                currentVillagers.add(param.substring("villager:".length()));
                                continue;
                            }
                            if (param.startsWith("notrel:")) {
                                currentNotRelations.add(param.substring("notrel:".length()));
                                continue;
                            }
                            if (!param.startsWith("rel:")) continue;
                            currentRelations.add(param.substring("rel:".length()));
                        }
                        ** GOTO lbl18
                    }
                    if (!line.startsWith("v1;") && !line.startsWith("v2;")) continue;
                    speaker = line.charAt(1) - 48;
                    rest = line.substring(3);
                    semiIdx = rest.indexOf(59);
                    if (semiIdx <= 0) continue;
                    delay = Integer.parseInt(rest.substring(0, semiIdx).trim());
                    text = rest.substring(semiIdx + 1);
                    currentLines.add(new Dialogue.Line(speaker, delay, text));
                }
                if (currentKey != null && !currentLines.isEmpty()) {
                    count += DialogueLoader.registerDialogue(byKey, currentKey, currentWeight, currentTags, v1Parts, v2Parts, currentLines, cultureId, currentBuildings, currentNotBuildings, currentVillagers, currentNotVillagers, currentRelations, currentNotRelations, lang, isOverlay, priorSubmodKeys, recordNewKeysInto, sourceLabel);
                }
            }
            finally {
                reader.close();
            }
            return count;
        }
        catch (Exception e) {
            DialogueLoader.LOGGER.error("Error parsing dialogues {}: {}", (Object)sourceLabel, (Object)e.getMessage());
            return 0;
        }
    }

    private static int registerDialogue(Map<String, Dialogue> byKey, String key, int weight, List<String> tags, List<String> v1Parts, List<String> v2Parts, List<Dialogue.Line> lines, ResourceLocation cultureId, List<String> buildings, List<String> notBuildings, List<String> villagers, List<String> notVillagers, List<String> relations, List<String> notRelations, String lang, boolean isOverlay, @Nullable Set<String> priorSubmodKeys, @Nullable Set<String> recordNewKeysInto, String sourceLabel) {
        if (byKey.containsKey(key)) {
            if (isOverlay) {
                if (priorSubmodKeys != null && priorSubmodKeys.contains(key)) {
                    if (WARNED_REPLACE_IDS.add("dialogue:" + lang + ":" + String.valueOf(cultureId) + ":" + key)) {
                        LOGGER.warn("Custom dialogue key '{}' in {} ({}) shadowed by earlier sub-mod (cross-pack REPLACE conflict; source={}) \u2014 skipping", new Object[]{key, lang, cultureId, sourceLabel});
                    }
                } else {
                    LOGGER.debug("Custom dialogue key '{}' in {} ({}) shadowed by base or within-file (speechRef protocol invariant; source={}) \u2014 skipping", new Object[]{key, lang, cultureId, sourceLabel});
                }
            } else {
                LOGGER.warn("Duplicate dialogue key '{}' in {} for culture {} \u2014 skipping", new Object[]{key, lang, cultureId});
            }
            return 0;
        }
        String v1 = v1Parts.isEmpty() ? null : String.join((CharSequence)",", v1Parts);
        String v2 = v2Parts.isEmpty() ? null : String.join((CharSequence)",", v2Parts);
        Dialogue dialogue = new Dialogue(key, weight, List.copyOf(tags), v1, v2, List.copyOf(lines), cultureId, List.copyOf(buildings), List.copyOf(notBuildings), List.copyOf(villagers), List.copyOf(notVillagers), List.copyOf(relations), List.copyOf(notRelations));
        byKey.put(key, dialogue);
        if (recordNewKeysInto != null) {
            recordNewKeysInto.add(key);
        }
        return 1;
    }

    public static Set<String> getLoadedLanguages() {
        return Set.copyOf(ALL_DIALOGUES.keySet());
    }

    public static List<Dialogue> getDialogues(ResourceLocation culture, String lang) {
        Map<ResourceLocation, Map<String, Dialogue>> byLang = ALL_DIALOGUES.get(lang);
        if (byLang == null) {
            return List.of();
        }
        Map<String, Dialogue> byCulture = byLang.get(culture);
        if (byCulture == null) {
            return List.of();
        }
        return List.copyOf(byCulture.values());
    }

    @Nullable
    public static String getDialogueLine(ResourceLocation culture, String lang, String dialogueKey, int lineIdx) {
        Map<ResourceLocation, Map<String, Dialogue>> byLang = ALL_DIALOGUES.get(lang);
        if (byLang == null) {
            return null;
        }
        Map<String, Dialogue> byCulture = byLang.get(culture);
        if (byCulture == null) {
            return null;
        }
        Dialogue dialogue = byCulture.get(dialogueKey);
        if (dialogue == null) {
            return null;
        }
        if (lineIdx < 0 || lineIdx >= dialogue.lines().size()) {
            return null;
        }
        String text = dialogue.lines().get(lineIdx).text();
        return text == null || text.isEmpty() ? null : text;
    }

    public static void clear() {
        ALL_DIALOGUES.clear();
        WARNED_REPLACE_IDS.clear();
    }

    private static /* synthetic */ Map lambda$parseDialogueStream$1(ResourceLocation k) {
        return new ConcurrentHashMap();
    }

    private static /* synthetic */ Map lambda$parseDialogueStream$0(String k) {
        return new ConcurrentHashMap();
    }
}

