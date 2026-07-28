/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.google.gson.JsonArray
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  com.google.gson.JsonParser
 *  com.mojang.logging.LogUtils
 *  javax.annotation.Nullable
 *  net.minecraft.util.GsonHelper
 *  org.slf4j.Logger
 */
package org.millenaire.quest;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import net.minecraft.util.GsonHelper;
import org.millenaire.content.ContentFs;
import org.millenaire.content.CustomContentIndex;
import org.millenaire.content.Resource;
import org.millenaire.content.SourceKind;
import org.millenaire.culture.CultureLoader;
import org.millenaire.culture.JsonLoaderUtils;
import org.millenaire.quest.ActionDataEntry;
import org.millenaire.quest.BedrockBuilding;
import org.millenaire.quest.Quest;
import org.millenaire.quest.QuestItemRef;
import org.millenaire.quest.QuestRegistry;
import org.millenaire.quest.QuestStep;
import org.millenaire.quest.QuestTextRegistry;
import org.millenaire.quest.QuestVillagerDef;
import org.millenaire.quest.RelationChange;
import org.millenaire.quest.VillagerTagAction;
import org.slf4j.Logger;

public final class QuestLoader {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = JsonLoaderUtils.GSON;
    private static final String QUESTS_DIR = "quests";
    private static final String LANGUAGES_DIR = "languages";
    private static volatile String[] cachedLanguages = null;
    private static final String[] FALLBACK_LANGUAGES = new String[]{"ar", "cs", "de", "en", "es", "fr", "it", "ko", "nl", "pl", "pt", "pt_br", "ru", "sl", "sv", "uk", "zh_cn", "zh_tw"};
    private static final Set<String> WARNED_TEXT_SHADOW = ConcurrentHashMap.newKeySet();

    public static void resetForTesting() {
        WARNED_TEXT_SHADOW.clear();
        cachedLanguages = null;
    }

    private QuestLoader() {
    }

    public static void loadAll() {
        QuestRegistry.clear();
        QuestTextRegistry.clear();
        WARNED_TEXT_SHADOW.clear();
        cachedLanguages = null;
        QuestLoader.loadQuestDefinitions();
        QuestLoader.loadQuestTexts();
        LOGGER.info("Loaded {} quest definitions, {} quest languages", (Object)QuestRegistry.size(), (Object)QuestTextRegistry.languageCount());
    }

    private static void loadQuestDefinitions() {
        Set<String> disabledIds = QuestLoader.loadQuestDisabledIds();
        HashSet<String> loadedQuestPaths = new HashSet<String>();
        List<String> manifest = QuestLoader.loadManifest();
        if (manifest == null) {
            LOGGER.warn("No quest manifest found \u2014 no top-level quests will be loaded");
        } else {
            int skipped = 0;
            int manifestDisabled = 0;
            for (String questPath : manifest) {
                if (QuestLoader.isWorldQuestPath(questPath)) {
                    ++skipped;
                    continue;
                }
                if (disabledIds.contains(questPath)) {
                    ++manifestDisabled;
                    continue;
                }
                Quest quest = QuestLoader.loadQuest(questPath);
                if (quest == null) continue;
                QuestRegistry.register(quest);
                loadedQuestPaths.add(questPath);
                LOGGER.debug("Quest loaded: {}", (Object)quest.key());
            }
            if (skipped > 0) {
                LOGGER.info("Skipped {} world quests (special action mechanisms not yet implemented)", (Object)skipped);
            }
            if (manifestDisabled > 0) {
                LOGGER.info("Skipped {} quest(s) listed in _disabled.json", (Object)manifestDisabled);
            }
        }
        QuestLoader.loadPerCultureQuests(loadedQuestPaths, disabledIds);
        QuestLoader.loadExternalQuestExtras(loadedQuestPaths, disabledIds);
    }

    private static boolean isWorldQuestPath(String questPath) {
        return questPath.startsWith("worldquest-") || questPath.contains("/worldquest-");
    }

    private static Set<String> loadQuestDisabledIds() {
        HashSet<String> out = new HashSet<String>();
        ContentFs root = CustomContentIndex.current().root();
        for (Resource res : root.findAll("quests/_disabled.json")) {
            out.addAll(QuestLoader.parseDisabledIds(res));
        }
        for (String culture : CultureLoader.discoverCultures()) {
            ContentFs cultureFs = CustomContentIndex.current().forCulture(culture);
            for (Resource res : cultureFs.findAll("quests/_disabled.json")) {
                for (String id : QuestLoader.parseDisabledIds(res)) {
                    out.add("cultures/" + culture + "/" + id);
                }
            }
        }
        return out;
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    private static Set<String> parseDisabledIds(Resource res) {
        try (InputStream in = res.open();){
            String body = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            JsonElement root = JsonParser.parseString((String)body);
            if (!root.isJsonArray()) {
                LOGGER.warn("{} from {} must be a JSON array; ignoring", (Object)res.relPath(), (Object)res.source().displayName());
                Set<String> set = Set.of();
                return set;
            }
            HashSet<String> out = new HashSet<String>();
            for (JsonElement e : root.getAsJsonArray()) {
                if (!e.isJsonPrimitive() || !e.getAsJsonPrimitive().isString()) continue;
                out.add(e.getAsString());
            }
            HashSet<String> hashSet = out;
            return hashSet;
        }
        catch (Exception e) {
            LOGGER.warn("Could not parse {} from {}: {}", new Object[]{res.relPath(), res.source().displayName(), e.getMessage()});
            return Set.of();
        }
    }

    private static void loadPerCultureQuests(Set<String> alreadyLoaded, Set<String> disabledIds) {
        int loaded = 0;
        int perCultureDisabled = 0;
        for (String culture : CultureLoader.discoverCultures()) {
            ContentFs cultureFs = CustomContentIndex.current().forCulture(culture);
            List<Resource> entries = cultureFs.walk(QUESTS_DIR, Integer.MAX_VALUE).toList();
            String typeRootPrefix = ("cultures/" + culture + "/quests/").toLowerCase(Locale.ROOT);
            for (Resource res : entries) {
                String filename;
                String relPathFull = res.relPath();
                if (!relPathFull.endsWith(".json") || (filename = relPathFull.substring(relPathFull.lastIndexOf(47) + 1)).startsWith("_") || !relPathFull.startsWith(typeRootPrefix)) continue;
                String relInsideQuest = relPathFull.substring(typeRootPrefix.length());
                String relWithoutExt = relInsideQuest.substring(0, relInsideQuest.length() - 5);
                String questPath = "cultures/" + culture + "/" + relWithoutExt;
                if (QuestLoader.isWorldQuestPath(relWithoutExt) || alreadyLoaded.contains(questPath)) continue;
                if (disabledIds.contains(questPath)) {
                    ++perCultureDisabled;
                    continue;
                }
                Quest quest = QuestLoader.parseQuestResource(res);
                if (quest == null) continue;
                QuestRegistry.register(quest);
                alreadyLoaded.add(questPath);
                ++loaded;
                LOGGER.debug("Per-culture quest loaded: {} (path={})", (Object)quest.key(), (Object)questPath);
            }
        }
        if (loaded > 0) {
            LOGGER.info("{} per-culture quest(s) loaded", (Object)loaded);
        }
        if (perCultureDisabled > 0) {
            LOGGER.info("Skipped {} per-culture quest(s) listed in _disabled.json", (Object)perCultureDisabled);
        }
    }

    private static void loadExternalQuestExtras(Set<String> alreadyLoaded, Set<String> disabledIds) {
        ContentFs questsFs = CustomContentIndex.current().forGlobalContent(QUESTS_DIR);
        int externalDisabled = 0;
        int externalLoaded = 0;
        List<Resource> entries = questsFs.walk("", Integer.MAX_VALUE).toList();
        String typeRootPrefix = "quests/".toLowerCase(Locale.ROOT);
        String langPrefix = "quests/lang/".toLowerCase(Locale.ROOT);
        for (Resource res : entries) {
            String relInside;
            String questPath;
            String filename;
            String relPathFull = res.relPath();
            if (!relPathFull.endsWith(".json") || (filename = relPathFull.substring(relPathFull.lastIndexOf(47) + 1)).startsWith("_") || !relPathFull.startsWith(typeRootPrefix) || relPathFull.startsWith(langPrefix) || QuestLoader.isWorldQuestPath(questPath = (relInside = relPathFull.substring(typeRootPrefix.length())).substring(0, relInside.length() - 5)) || alreadyLoaded.contains(questPath)) continue;
            if (disabledIds.contains(questPath)) {
                ++externalDisabled;
                continue;
            }
            Quest quest = QuestLoader.parseQuestResource(res);
            if (quest == null) continue;
            QuestRegistry.register(quest);
            alreadyLoaded.add(questPath);
            ++externalLoaded;
            LOGGER.debug("Loaded external quest '{}' from {}", (Object)questPath, (Object)res.source().displayName());
        }
        if (externalLoaded > 0) {
            LOGGER.info("Loaded {} external quest(s) from sub-mods", (Object)externalLoaded);
        }
        if (externalDisabled > 0) {
            LOGGER.info("Skipped {} external quest(s) via cross-sub-mod _disabled.json", (Object)externalDisabled);
        }
    }

    /*
     * Enabled aggressive exception aggregation
     */
    @Nullable
    private static List<String> loadManifest() {
        ContentFs root = CustomContentIndex.current().root();
        List<Resource> matches = root.findAll("quests/_manifest.json");
        Resource jarManifest = null;
        for (Resource res : matches) {
            if (res.kind() != SourceKind.CLASSPATH) continue;
            jarManifest = res;
            break;
        }
        if (jarManifest == null) {
            jarManifest = root.findFirst("quests/_manifest.json").orElse(null);
        }
        if (jarManifest == null) {
            return null;
        }
        try (InputStream is = jarManifest.open();){
            ArrayList<String> arrayList;
            try (InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8);){
                JsonArray array = (JsonArray)GSON.fromJson((Reader)reader, JsonArray.class);
                ArrayList<String> paths = new ArrayList<String>();
                for (JsonElement e : array) {
                    paths.add(e.getAsString());
                }
                arrayList = paths;
            }
            return arrayList;
        }
        catch (Exception e) {
            LOGGER.error("Error reading quest manifest", (Throwable)e);
            return null;
        }
    }

    @Nullable
    private static Quest loadQuest(String questPath) {
        Resource res = QuestLoader.resolveQuestResource(questPath);
        if (res == null) {
            LOGGER.debug("Quest file not found via ContentFs: {}", (Object)questPath);
            return null;
        }
        return QuestLoader.parseQuestResource(res);
    }

    @Nullable
    private static Resource resolveQuestResource(String questPath) {
        ContentFs root = CustomContentIndex.current().root();
        if (questPath.startsWith("cultures/")) {
            String tail = questPath.substring("cultures/".length());
            int slash = tail.indexOf(47);
            if (slash <= 0) {
                return root.findFirst("quests/" + questPath + ".json").orElse(null);
            }
            String culture = tail.substring(0, slash);
            String rest = tail.substring(slash + 1);
            return CustomContentIndex.current().forCulture(culture).findFirst("quests/" + rest + ".json").orElse(null);
        }
        return root.findFirst("quests/" + questPath + ".json").orElse(null);
    }

    /*
     * Enabled aggressive exception aggregation
     */
    @Nullable
    private static Quest parseQuestResource(Resource res) {
        try (InputStream is = res.open();){
            Quest quest;
            try (InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8);){
                JsonObject json = (JsonObject)GSON.fromJson((Reader)reader, JsonObject.class);
                quest = QuestLoader.parseQuest(json);
            }
            return quest;
        }
        catch (Exception e) {
            LOGGER.error("Error reading quest from {}: {}", new Object[]{res.relPath(), e.getMessage(), e});
            return null;
        }
    }

    @Nullable
    private static Quest parseQuest(JsonObject json) {
        String key = GsonHelper.getAsString((JsonObject)json, (String)"key");
        double chancePerHour = GsonHelper.getAsDouble((JsonObject)json, (String)"chancePerHour", (double)0.0);
        int maxSimultaneous = GsonHelper.getAsInt((JsonObject)json, (String)"maxSimultaneous", (int)5);
        int minReputation = GsonHelper.getAsInt((JsonObject)json, (String)"minReputation", (int)0);
        List<String> globalTagsRequired = JsonLoaderUtils.parseStringList(json, "globalTagsRequired");
        List<String> globalTagsForbidden = JsonLoaderUtils.parseStringList(json, "globalTagsForbidden");
        List<String> playerTagsRequired = JsonLoaderUtils.parseStringList(json, "playerTagsRequired");
        List<String> playerTagsForbidden = JsonLoaderUtils.parseStringList(json, "playerTagsForbidden");
        ArrayList<QuestVillagerDef> villagerDefs = new ArrayList<QuestVillagerDef>();
        JsonArray villagersArray = GsonHelper.getAsJsonArray((JsonObject)json, (String)"villagerDefs");
        for (JsonElement e : villagersArray) {
            villagerDefs.add(QuestLoader.parseVillagerDef(e.getAsJsonObject()));
        }
        ArrayList<QuestStep> steps = new ArrayList<QuestStep>();
        JsonArray stepsArray = GsonHelper.getAsJsonArray((JsonObject)json, (String)"steps");
        for (JsonElement e : stepsArray) {
            steps.add(QuestLoader.parseStep(e.getAsJsonObject()));
        }
        if (villagerDefs.isEmpty()) {
            LOGGER.error("Quest '{}' has no villagerDefs \u2014 skipping", (Object)key);
            return null;
        }
        if (steps.isEmpty()) {
            LOGGER.error("Quest '{}' has no steps \u2014 skipping", (Object)key);
            return null;
        }
        return new Quest(key, chancePerHour, maxSimultaneous, minReputation, globalTagsRequired, globalTagsForbidden, playerTagsRequired, playerTagsForbidden, villagerDefs, steps);
    }

    private static QuestVillagerDef parseVillagerDef(JsonObject json) {
        String key = GsonHelper.getAsString((JsonObject)json, (String)"key");
        List<String> types = JsonLoaderUtils.parseStringList(json, "villagerTypes");
        String relatedTo = json.has("relatedTo") ? GsonHelper.getAsString((JsonObject)json, (String)"relatedTo") : null;
        String relation = json.has("relation") ? GsonHelper.getAsString((JsonObject)json, (String)"relation") : null;
        List<String> requiredTags = JsonLoaderUtils.parseStringList(json, "requiredTags");
        List<String> forbiddenTags = JsonLoaderUtils.parseStringList(json, "forbiddenTags");
        return new QuestVillagerDef(key, types, relatedTo, relation, requiredTags, forbiddenTags);
    }

    private static QuestStep parseStep(JsonObject json) {
        int index = GsonHelper.getAsInt((JsonObject)json, (String)"index");
        String villagerKey = GsonHelper.getAsString((JsonObject)json, (String)"villagerKey");
        int duration = GsonHelper.getAsInt((JsonObject)json, (String)"duration", (int)3);
        boolean showRequiredGoods = GsonHelper.getAsBoolean((JsonObject)json, (String)"showRequiredGoods", (boolean)true);
        Map<QuestItemRef, Integer> requiredGoods = QuestLoader.parseItemRefMap(json, "requiredGoods");
        Map<QuestItemRef, Integer> rewardGoods = QuestLoader.parseItemRefMap(json, "rewardGoods");
        int rewardMoney = GsonHelper.getAsInt((JsonObject)json, (String)"rewardMoney", (int)0);
        int rewardReputation = GsonHelper.getAsInt((JsonObject)json, (String)"rewardReputation", (int)0);
        int penaltyReputation = GsonHelper.getAsInt((JsonObject)json, (String)"penaltyReputation", (int)0);
        List<VillagerTagAction> villagerTagsSuccess = QuestLoader.parseVillagerTagActions(json, "villagerTagsSuccess");
        List<VillagerTagAction> villagerTagsFailure = QuestLoader.parseVillagerTagActions(json, "villagerTagsFailure");
        List<String> playerTagsSuccess = JsonLoaderUtils.parseStringList(json, "playerTagsSuccess");
        List<String> playerTagsFailure = JsonLoaderUtils.parseStringList(json, "playerTagsFailure");
        List<String> globalTagsSuccess = JsonLoaderUtils.parseStringList(json, "globalTagsSuccess");
        List<String> globalTagsFailure = JsonLoaderUtils.parseStringList(json, "globalTagsFailure");
        List<VillagerTagAction> clearTagsSuccess = QuestLoader.parseVillagerTagActions(json, "clearTagsSuccess");
        List<VillagerTagAction> clearTagsFailure = QuestLoader.parseVillagerTagActions(json, "clearTagsFailure");
        List<String> clearPlayerTagsSuccess = JsonLoaderUtils.parseStringList(json, "clearPlayerTagsSuccess");
        List<String> clearPlayerTagsFailure = JsonLoaderUtils.parseStringList(json, "clearPlayerTagsFailure");
        List<String> clearGlobalTagsSuccess = JsonLoaderUtils.parseStringList(json, "clearGlobalTagsSuccess");
        List<String> clearGlobalTagsFailure = JsonLoaderUtils.parseStringList(json, "clearGlobalTagsFailure");
        List<String> stepRequiredGlobalTags = JsonLoaderUtils.parseStringList(json, "stepRequiredGlobalTags");
        List<String> stepForbiddenGlobalTags = JsonLoaderUtils.parseStringList(json, "stepForbiddenGlobalTags");
        List<String> stepRequiredPlayerTags = JsonLoaderUtils.parseStringList(json, "stepRequiredPlayerTags");
        List<String> stepForbiddenPlayerTags = JsonLoaderUtils.parseStringList(json, "stepForbiddenPlayerTags");
        List<ActionDataEntry> actionDataSuccess = QuestLoader.parseActionDataEntries(json, "actionDataSuccess");
        List<RelationChange> relationChanges = QuestLoader.parseRelationChanges(json, "relationChanges");
        List<BedrockBuilding> bedrockBuildings = QuestLoader.parseBedrockBuildings(json, "bedrockBuildings");
        Map<String, String> labels = QuestLoader.parseStringStringMap(json, "labels");
        Map<String, String> descriptions = QuestLoader.parseStringStringMap(json, "descriptions");
        Map<String, String> descriptionsSuccess = QuestLoader.parseStringStringMap(json, "descriptionsSuccess");
        Map<String, String> descriptionsRefuse = QuestLoader.parseStringStringMap(json, "descriptionsRefuse");
        Map<String, String> descriptionsTimeUp = QuestLoader.parseStringStringMap(json, "descriptionsTimeUp");
        Map<String, String> listings = QuestLoader.parseStringStringMap(json, "listings");
        return new QuestStep(index, villagerKey, duration, showRequiredGoods, requiredGoods, rewardGoods, rewardMoney, rewardReputation, penaltyReputation, villagerTagsSuccess, villagerTagsFailure, playerTagsSuccess, playerTagsFailure, globalTagsSuccess, globalTagsFailure, clearTagsSuccess, clearTagsFailure, clearPlayerTagsSuccess, clearPlayerTagsFailure, clearGlobalTagsSuccess, clearGlobalTagsFailure, stepRequiredGlobalTags, stepForbiddenGlobalTags, stepRequiredPlayerTags, stepForbiddenPlayerTags, actionDataSuccess, relationChanges, bedrockBuildings, labels, descriptions, descriptionsSuccess, descriptionsRefuse, descriptionsTimeUp, listings);
    }

    private static Map<QuestItemRef, Integer> parseItemRefMap(JsonObject parent, String key) {
        LinkedHashMap<QuestItemRef, Integer> map = new LinkedHashMap<QuestItemRef, Integer>();
        if (!parent.has(key)) {
            return map;
        }
        JsonObject obj = GsonHelper.getAsJsonObject((JsonObject)parent, (String)key);
        for (Map.Entry entry : obj.entrySet()) {
            String itemKey = (String)entry.getKey();
            int count = ((JsonElement)entry.getValue()).getAsInt();
            int meta = 0;
            if (itemKey.contains("|")) {
                String[] parts = itemKey.split("\\|");
                itemKey = parts[0];
                meta = Integer.parseInt(parts[1]);
            }
            map.put(new QuestItemRef(itemKey, meta), count);
        }
        return map;
    }

    private static List<VillagerTagAction> parseVillagerTagActions(JsonObject parent, String key) {
        ArrayList<VillagerTagAction> list = new ArrayList<VillagerTagAction>();
        if (!parent.has(key)) {
            return list;
        }
        JsonArray array = parent.getAsJsonArray(key);
        for (JsonElement e : array) {
            JsonObject obj = e.getAsJsonObject();
            list.add(new VillagerTagAction(GsonHelper.getAsString((JsonObject)obj, (String)"villagerKey"), GsonHelper.getAsString((JsonObject)obj, (String)"tag")));
        }
        return list;
    }

    private static List<ActionDataEntry> parseActionDataEntries(JsonObject parent, String key) {
        ArrayList<ActionDataEntry> list = new ArrayList<ActionDataEntry>();
        if (!parent.has(key)) {
            return list;
        }
        JsonArray array = parent.getAsJsonArray(key);
        for (JsonElement e : array) {
            JsonObject obj = e.getAsJsonObject();
            list.add(new ActionDataEntry(GsonHelper.getAsString((JsonObject)obj, (String)"key"), GsonHelper.getAsString((JsonObject)obj, (String)"value")));
        }
        return list;
    }

    private static List<RelationChange> parseRelationChanges(JsonObject parent, String key) {
        ArrayList<RelationChange> list = new ArrayList<RelationChange>();
        if (!parent.has(key)) {
            return list;
        }
        JsonArray array = parent.getAsJsonArray(key);
        for (JsonElement e : array) {
            JsonObject obj = e.getAsJsonObject();
            list.add(new RelationChange(GsonHelper.getAsString((JsonObject)obj, (String)"firstVillager"), GsonHelper.getAsString((JsonObject)obj, (String)"secondVillager"), GsonHelper.getAsInt((JsonObject)obj, (String)"change")));
        }
        return list;
    }

    private static List<BedrockBuilding> parseBedrockBuildings(JsonObject parent, String key) {
        ArrayList<BedrockBuilding> list = new ArrayList<BedrockBuilding>();
        if (!parent.has(key)) {
            return list;
        }
        JsonArray array = parent.getAsJsonArray(key);
        for (JsonElement e : array) {
            JsonObject obj = e.getAsJsonObject();
            list.add(new BedrockBuilding(GsonHelper.getAsString((JsonObject)obj, (String)"culture"), GsonHelper.getAsString((JsonObject)obj, (String)"villageType")));
        }
        return list;
    }

    private static Map<String, String> parseStringStringMap(JsonObject parent, String key) {
        HashMap<String, String> map = new HashMap<String, String>();
        if (!parent.has(key)) {
            return map;
        }
        JsonObject obj = GsonHelper.getAsJsonObject((JsonObject)parent, (String)key);
        for (Map.Entry entry : obj.entrySet()) {
            map.put((String)entry.getKey(), ((JsonElement)entry.getValue()).getAsString());
        }
        return map;
    }

    private static void loadQuestTexts() {
        for (String lang : QuestLoader.discoverQuestLanguages()) {
            HashMap<String, String> texts = new HashMap<String, String>();
            ContentFs root = CustomContentIndex.current().root();
            String basePath = "quests/lang/" + lang + ".json";
            for (Resource res : root.findAll(basePath)) {
                if (res.kind() != SourceKind.CLASSPATH) continue;
                QuestLoader.mergePut(res, texts);
                break;
            }
            for (Resource res : root.findAll(basePath)) {
                if (res.kind() != SourceKind.STANDARD) continue;
                QuestLoader.mergePut(res, texts);
            }
            int baseCount = texts.size();
            String customPath = "languages/" + lang + "/quest_lang.json";
            ArrayList<Resource> custom = new ArrayList<Resource>(root.findAll(customPath));
            Collections.reverse(custom);
            HashSet<String> customSeen = new HashSet<String>();
            for (Resource res : custom) {
                if (res.kind() == SourceKind.CLASSPATH) continue;
                if (res.kind() == SourceKind.STANDARD) {
                    QuestLoader.mergePut(res, texts);
                    continue;
                }
                QuestLoader.mergeCustomFirstAlpha(res, texts, customSeen, lang);
            }
            if (texts.isEmpty()) continue;
            QuestTextRegistry.register(lang, texts);
            int externalCount = texts.size() - baseCount;
            if (externalCount > 0) {
                LOGGER.debug("Quest texts loaded: {} ({} entries, {} from overlay)", new Object[]{lang, texts.size(), externalCount});
                continue;
            }
            LOGGER.debug("Quest texts loaded: {} ({} entries)", (Object)lang, (Object)texts.size());
        }
    }

    private static void mergePut(Resource res, Map<String, String> texts) {
        try (InputStream is = res.open();
             InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8);){
            JsonObject json = (JsonObject)GSON.fromJson((Reader)reader, JsonObject.class);
            if (json == null) {
                return;
            }
            for (Map.Entry entry : json.entrySet()) {
                texts.put((String)entry.getKey(), ((JsonElement)entry.getValue()).getAsString());
            }
        }
        catch (Exception e) {
            LOGGER.error("Error loading quest texts {} from {}: {}", new Object[]{res.relPath(), res.source().displayName(), e.getMessage(), e});
        }
    }

    private static void mergeCustomFirstAlpha(Resource res, Map<String, String> texts, Set<String> customSeen, String lang) {
        try (InputStream is = res.open();
             InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8);){
            JsonObject json = (JsonObject)GSON.fromJson((Reader)reader, JsonObject.class);
            if (json == null) {
                return;
            }
            for (Map.Entry entry : json.entrySet()) {
                String key = (String)entry.getKey();
                String value = ((JsonElement)entry.getValue()).getAsString();
                if (customSeen.add(key)) {
                    texts.put(key, value);
                    continue;
                }
                if (!WARNED_TEXT_SHADOW.add("quest_text:" + lang + ":" + key)) continue;
                LOGGER.warn("Quest text key '{}' ({}) in {} shadowed by earlier sub-mod", new Object[]{key, lang, res.source().displayName()});
            }
        }
        catch (Exception e) {
            LOGGER.error("Error loading quest texts {} from {}: {}", new Object[]{res.relPath(), res.source().displayName(), e.getMessage(), e});
        }
    }

    static String[] discoverQuestLanguages() {
        String[] cached = cachedLanguages;
        if (cached != null) {
            return cached;
        }
        LinkedHashSet<String> languages = new LinkedHashSet<String>();
        boolean fromManifest = QuestLoader.readQuestLanguagesManifest(languages);
        if (!fromManifest && languages.isEmpty()) {
            for (String lang : FALLBACK_LANGUAGES) {
                languages.add(lang);
            }
        }
        QuestLoader.addSubmodQuestLanguages(languages);
        String[] out = languages.toArray(new String[0]);
        cachedLanguages = out;
        return out;
    }

    /*
     * Enabled aggressive exception aggregation
     */
    private static boolean readQuestLanguagesManifest(LinkedHashSet<String> out) {
        ContentFs root = CustomContentIndex.current().root();
        Resource manifest = null;
        for (Resource res : root.findAll("quests/lang/_manifest.json")) {
            if (res.kind() != SourceKind.CLASSPATH) continue;
            manifest = res;
            break;
        }
        if (manifest == null) {
            LOGGER.warn("Quest language manifest not found at {}/lang/_manifest.json \u2014 falling back to built-in list", (Object)QUESTS_DIR);
            return false;
        }
        try (InputStream is = manifest.open();){
            boolean bl;
            JsonArray arr;
            InputStreamReader reader;
            block18: {
                reader = new InputStreamReader(is, StandardCharsets.UTF_8);
                JsonObject obj = (JsonObject)GSON.fromJson((Reader)reader, JsonObject.class);
                JsonArray jsonArray = arr = obj != null ? obj.getAsJsonArray(LANGUAGES_DIR) : null;
                if (arr != null && !arr.isEmpty()) break block18;
                LOGGER.warn("Quest language manifest has no 'languages' array \u2014 falling back to built-in list");
                boolean bl2 = false;
                reader.close();
                return bl2;
            }
            try {
                for (JsonElement e : arr) {
                    out.add(e.getAsString());
                }
                bl = true;
            }
            catch (Throwable throwable) {
                try {
                    reader.close();
                }
                catch (Throwable throwable2) {
                    throwable.addSuppressed(throwable2);
                }
                throw throwable;
            }
            reader.close();
            return bl;
        }
        catch (Exception e) {
            LOGGER.warn("Error reading quest language manifest: {} \u2014 falling back to built-in list", (Object)e.getMessage());
            return false;
        }
    }

    private static void addSubmodQuestLanguages(LinkedHashSet<String> out) {
        ContentFs languagesFs = CustomContentIndex.current().forGlobalContent(LANGUAGES_DIR);
        languagesFs.walk("", 1).forEach(res -> {
            String relPath = res.relPath();
            String tail = relPath.substring("languages/".length());
            int slash = tail.indexOf(47);
            if (slash <= 0) {
                return;
            }
            String filename = tail.substring(slash + 1);
            if (!"quest_lang.json".equals(filename)) {
                return;
            }
            String lang = tail.substring(0, slash);
            if (lang.startsWith("_") || "native".equals(lang)) {
                return;
            }
            out.add(lang);
        });
    }

    static List<String> discoverCultureQuests(String culture) {
        ContentFs cultureFs = CustomContentIndex.current().forCulture(culture);
        String typeRootPrefix = ("cultures/" + culture + "/quests/").toLowerCase(Locale.ROOT);
        ArrayList<String> rels = new ArrayList<String>();
        cultureFs.walk(QUESTS_DIR, Integer.MAX_VALUE).forEach(res -> {
            if (res.kind() != SourceKind.CLASSPATH) {
                return;
            }
            String full = res.relPath();
            if (!full.endsWith(".json")) {
                return;
            }
            String filename = full.substring(full.lastIndexOf(47) + 1);
            if (filename.startsWith("_")) {
                return;
            }
            if (!full.startsWith(typeRootPrefix)) {
                return;
            }
            String inside = full.substring(typeRootPrefix.length());
            rels.add(inside.substring(0, inside.length() - 5));
        });
        rels.sort(String::compareTo);
        return rels;
    }
}

