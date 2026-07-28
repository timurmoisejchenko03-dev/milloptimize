/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  com.google.gson.JsonParser
 *  com.mojang.logging.LogUtils
 *  javax.annotation.Nullable
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.util.GsonHelper
 *  net.neoforged.fml.ModList
 *  org.slf4j.Logger
 */
package org.millenaire.goal.gathering;

import com.google.gson.Gson;
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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.neoforged.fml.ModList;
import org.millenaire.content.ContentDirectoryManager;
import org.millenaire.content.ContentFs;
import org.millenaire.content.CustomContentIndex;
import org.millenaire.content.Resource;
import org.millenaire.content.SourceKind;
import org.millenaire.culture.JsonLoaderUtils;
import org.millenaire.goal.GoalRegistry;
import org.millenaire.goal.gathering.GatheringGoal;
import org.millenaire.goal.gathering.GatheringHandler;
import org.millenaire.goal.gathering.GatheringHandlerRegistry;
import org.millenaire.goal.gathering.GatheringType;
import org.slf4j.Logger;

public final class GatheringTypeLoader {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = JsonLoaderUtils.GSON;
    private static final String GATHERING_TYPE_DIR = "gathering_type";
    private static final String JSON_EXT = ".json";
    private static final Set<String> WARNED_REPLACE_IDS = ConcurrentHashMap.newKeySet();

    public static void resetForTesting() {
        WARNED_REPLACE_IDS.clear();
    }

    private GatheringTypeLoader() {
    }

    public static void loadAll(GoalRegistry goalRegistry) {
        ContentFs fs = GatheringTypeLoader.jarFs();
        Set<String> disabledIds = GatheringTypeLoader.readDisabledIds(fs);
        int count = 0;
        List<Resource> entries = fs.walk("", 1).filter(r -> r.kind() == SourceKind.CLASSPATH).toList();
        for (Resource res : entries) {
            String filename = GatheringTypeLoader.leafName(res.relPath());
            if (filename == null || filename.startsWith("_") || !filename.endsWith(JSON_EXT)) continue;
            String id = filename.substring(0, filename.length() - JSON_EXT.length());
            if (disabledIds.contains(id)) {
                LOGGER.debug("Skipping JAR gathering type '{}' (listed in _disabled.json)", (Object)id);
                continue;
            }
            GatheringType type = GatheringTypeLoader.parseFromResource(res, id);
            if (type == null) continue;
            GatheringHandler handler = GatheringHandlerRegistry.get(type.handlerId());
            if (handler == null) {
                LOGGER.error("Unknown handler '{}' for gathering type {}", (Object)type.handlerId(), (Object)type.id());
                continue;
            }
            GatheringGoal goal = new GatheringGoal(type, handler);
            goalRegistry.register(goal);
            ++count;
            LOGGER.debug("GatheringType loaded: {} (handler={})", (Object)type.id(), (Object)type.handlerId());
        }
        LOGGER.info("{} gathering types loaded", (Object)count);
    }

    public static void loadExternal(GoalRegistry goalRegistry) {
        WARNED_REPLACE_IDS.clear();
        ContentFs fs = CustomContentIndex.current().forGlobalContent(GATHERING_TYPE_DIR);
        Set<String> disabledIds = GatheringTypeLoader.readDisabledIds(fs);
        List<Resource> entries = fs.walk("", 1).filter(r -> r.kind() != SourceKind.CLASSPATH).toList();
        if (entries.isEmpty() && disabledIds.isEmpty()) {
            return;
        }
        int added = 0;
        int overridden = 0;
        for (Resource res : entries) {
            GatheringType type;
            String id;
            String filename = GatheringTypeLoader.leafName(res.relPath());
            if (filename == null || filename.startsWith("_") || !filename.endsWith(JSON_EXT) || disabledIds.contains(id = filename.substring(0, filename.length() - JSON_EXT.length())) || (type = GatheringTypeLoader.parseFromResource(res, id)) == null) continue;
            GatheringHandler handler = GatheringHandlerRegistry.get(type.handlerId());
            if (handler == null) {
                LOGGER.error("External gathering type {} references unknown handler '{}' \u2014 skipping", (Object)type.id(), (Object)type.handlerId());
                continue;
            }
            GatheringGoal goal = new GatheringGoal(type, handler);
            boolean wasPresent = goalRegistry.get(type.id()) != null;
            goalRegistry.replace(goal);
            if (wasPresent) {
                ++overridden;
                continue;
            }
            ++added;
        }
        int skipped = disabledIds.size();
        if (added + overridden + skipped > 0) {
            LOGGER.info("External gathering types: {} added, {} overridden, {} disabled", new Object[]{added, overridden, skipped});
        }
    }

    public static void validateAll(GoalRegistry goalRegistry) {
        Pattern unresolvablePattern = Pattern.compile("unresolvable (?:item|block) (?:'([^']+)'|in '[^']+': (\\S+))");
        LinkedHashMap<String, List> foreignByNs = new LinkedHashMap<String, List>();
        ArrayList<String> internalErrors = new ArrayList<String>();
        int errorCount = 0;
        for (GatheringGoal gatheringGoal : goalRegistry.getGatheringGoals()) {
            GatheringType type = gatheringGoal.getGatheringType();
            GatheringHandler handler = gatheringGoal.getHandler();
            List<String> errors = handler.validate(type);
            for (String error : errors) {
                ++errorCount;
                Matcher m = unresolvablePattern.matcher(error);
                String namespace = null;
                if (m.find()) {
                    String id = m.group(1) != null ? m.group(1) : m.group(2);
                    int colon = id.indexOf(58);
                    String string = namespace = colon > 0 ? id.substring(0, colon) : "minecraft";
                }
                if (namespace != null && !GatheringTypeLoader.isKnownNamespace(namespace)) {
                    foreignByNs.computeIfAbsent(namespace, k -> new ArrayList()).add(String.valueOf(type.id()) + " (handler=" + type.handlerId() + "): " + error);
                    continue;
                }
                LOGGER.error("Gathering type {} (handler={}): {}", new Object[]{type.id(), type.handlerId(), error});
                internalErrors.add(type.id().toString());
            }
        }
        for (Map.Entry entry : foreignByNs.entrySet()) {
            String ns = (String)entry.getKey();
            List details = (List)entry.getValue();
            LOGGER.warn("Gathering types reference {} unresolvable id(s) from namespace '{}:' \u2014 likely an uninstalled dependency mod. Affected goals will be inactive. Enable DEBUG on GatheringTypeLoader for the full list.", (Object)details.size(), (Object)ns);
            for (String d : details) {
                LOGGER.debug("  - {}", (Object)d);
            }
        }
        if (errorCount > 0) {
            int foreignCount = errorCount - internalErrors.size();
            LOGGER.error("{} validation error(s) found in gathering types \u2014 affected goals will malfunction (internal: {}, foreign-namespace: {})", new Object[]{errorCount, internalErrors.size(), foreignCount});
        }
    }

    private static boolean isKnownNamespace(String namespace) {
        if ("minecraft".equals(namespace) || "millenaire".equals(namespace)) {
            return true;
        }
        try {
            return ModList.get().isLoaded(namespace);
        }
        catch (Exception | LinkageError t) {
            return false;
        }
    }

    public static List<String> discoverGatheringTypes() {
        ContentFs fs = GatheringTypeLoader.jarFs();
        ArrayList<String> names = new ArrayList<String>();
        fs.walk("", 1).filter(r -> r.kind() == SourceKind.CLASSPATH).forEach(r -> {
            String filename = GatheringTypeLoader.leafName(r.relPath());
            if (filename == null || filename.startsWith("_")) {
                return;
            }
            if (!filename.endsWith(JSON_EXT)) {
                return;
            }
            names.add(filename.substring(0, filename.length() - JSON_EXT.length()));
        });
        if (names.isEmpty()) {
            LOGGER.warn("No gathering_type files found via ContentFs overlay");
        }
        Collections.sort(names);
        return names;
    }

    private static ContentFs jarFs() {
        ContentFs jarOnly;
        if (!ContentDirectoryManager.isInitialized()) {
            return CustomContentIndex.jarOnly().forGlobalContent(GATHERING_TYPE_DIR);
        }
        ContentFs fs = CustomContentIndex.current().forGlobalContent(GATHERING_TYPE_DIR);
        if (fs.walk("", 1).findAny().isEmpty() && (jarOnly = CustomContentIndex.jarOnly().forGlobalContent(GATHERING_TYPE_DIR)).walk("", 1).findAny().isPresent()) {
            return jarOnly;
        }
        return fs;
    }

    private static Set<String> readDisabledIds(ContentFs fs) {
        List<Resource> manifests = fs.findAll("_disabled.json");
        if (manifests.isEmpty()) {
            return Collections.emptySet();
        }
        HashSet<String> out = new HashSet<String>();
        for (Resource res : manifests) {
            try {
                InputStream in = res.open();
                try {
                    String body = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                    JsonElement root = JsonParser.parseString((String)body);
                    if (!root.isJsonArray()) {
                        LOGGER.warn("_disabled.json from {} must be a JSON array; ignoring", (Object)res.source().displayName());
                        continue;
                    }
                    for (JsonElement element : root.getAsJsonArray()) {
                        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) continue;
                        out.add(element.getAsString());
                    }
                }
                finally {
                    if (in == null) continue;
                    in.close();
                }
            }
            catch (Exception e) {
                LOGGER.warn("Could not parse _disabled.json from {}: {}", (Object)res.source().displayName(), (Object)e.getMessage());
            }
        }
        return out;
    }

    /*
     * Enabled aggressive exception aggregation
     */
    @Nullable
    private static GatheringType parseFromResource(Resource res, String filename) {
        try (InputStream is = res.open();){
            GatheringType gatheringType;
            try (InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8);){
                JsonObject json = (JsonObject)GSON.fromJson((Reader)reader, JsonObject.class);
                gatheringType = GatheringTypeLoader.parseGatheringType(filename, json);
            }
            return gatheringType;
        }
        catch (Exception e) {
            LOGGER.error("Error loading gathering type {} from {}: {}", new Object[]{filename, res.source().displayName(), e.getMessage(), e});
            return null;
        }
    }

    @Nullable
    private static String leafName(String relPath) {
        if (relPath == null || relPath.isEmpty()) {
            return null;
        }
        int slash = relPath.lastIndexOf(47);
        return slash < 0 ? relPath : relPath.substring(slash + 1);
    }

    @Nullable
    private static GatheringType parseGatheringType(String filename, JsonObject json) {
        try {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)filename.toLowerCase(Locale.ROOT));
            String handlerId = GsonHelper.getAsString((JsonObject)json, (String)"handler");
            int priority = GsonHelper.getAsInt((JsonObject)json, (String)"priority", (int)50);
            int priorityRandom = GsonHelper.getAsInt((JsonObject)json, (String)"priorityRandom", (int)10);
            int scanRadius = GsonHelper.getAsInt((JsonObject)json, (String)"scanRadius", (int)32);
            int batchRadius = GsonHelper.getAsInt((JsonObject)json, (String)"batchRadius", (int)8);
            int maxActionsPerTask = GsonHelper.getAsInt((JsonObject)json, (String)"maxActionsPerTask", (int)16);
            int actionCooldown = GsonHelper.getAsInt((JsonObject)json, (String)"actionCooldown", (int)10);
            int stuckTimeout = GsonHelper.getAsInt((JsonObject)json, (String)"stuckTimeout", (int)4000);
            int arrivalRange = GsonHelper.getAsInt((JsonObject)json, (String)"arrivalRange", (int)3);
            double walkSpeed = GsonHelper.getAsFloat((JsonObject)json, (String)"walkSpeed", (float)0.6f);
            HashMap<String, Integer> villageLimit = null;
            if (json.has("villageLimit")) {
                villageLimit = new HashMap<String, Integer>();
                JsonObject limitObj = GsonHelper.getAsJsonObject((JsonObject)json, (String)"villageLimit");
                for (String key : limitObj.keySet()) {
                    villageLimit.put(key, limitObj.get(key).getAsInt());
                }
            }
            int maxSimultaneousTotal = GsonHelper.getAsInt((JsonObject)json, (String)"maxSimultaneousTotal", (int)-1);
            int minimumHour = GsonHelper.getAsInt((JsonObject)json, (String)"minimumHour", (int)-1);
            int maximumHour = GsonHelper.getAsInt((JsonObject)json, (String)"maximumHour", (int)-1);
            int reoccurDelay = GsonHelper.getAsInt((JsonObject)json, (String)"reoccurDelay", (int)-1);
            JsonObject handlerParams = json.has("handlerParams") ? GsonHelper.getAsJsonObject((JsonObject)json, (String)"handlerParams") : new JsonObject();
            Map<String, Integer> buildingLimit = JsonLoaderUtils.parseStringIntMapOrNull(json, "buildingLimit");
            Map<String, Integer> townhallLimit = JsonLoaderUtils.parseStringIntMapOrNull(json, "townhallLimit");
            Map<String, String> itemsBalance = GatheringTypeLoader.parseStringStringMap(json, "itemsBalance");
            int maxSimultaneousInBuilding = GsonHelper.getAsInt((JsonObject)json, (String)"maxSimultaneousInBuilding", (int)-1);
            String destinationBuilding = json.has("destinationBuilding") ? GsonHelper.getAsString((JsonObject)json, (String)"destinationBuilding") : null;
            List<String> heldItems = JsonLoaderUtils.parseStringListOrNull(json, "heldItems");
            List<String> heldItemsDestination = JsonLoaderUtils.parseStringListOrNull(json, "heldItemsDestination");
            String sound = json.has("sound") ? GsonHelper.getAsString((JsonObject)json, (String)"sound") : null;
            List<String> priorityInvPenaltyItems = JsonLoaderUtils.parseStringListOrNull(json, "priorityInvPenaltyItems");
            int priorityInvPenaltyBase = GsonHelper.getAsInt((JsonObject)json, (String)"priorityInvPenaltyBase", (int)0);
            String sentenceKey = json.has("sentenceKey") ? GsonHelper.getAsString((JsonObject)json, (String)"sentenceKey") : null;
            String labelKey = json.has("labelKey") ? GsonHelper.getAsString((JsonObject)json, (String)"labelKey") : null;
            String tag = json.has("tag") ? GsonHelper.getAsString((JsonObject)json, (String)"tag") : null;
            boolean leisure = GsonHelper.getAsBoolean((JsonObject)json, (String)"leisure", (boolean)false);
            return new GatheringType(id, handlerId, priority, priorityRandom, scanRadius, batchRadius, maxActionsPerTask, actionCooldown, stuckTimeout, arrivalRange, walkSpeed, villageLimit, maxSimultaneousTotal, minimumHour, maximumHour, reoccurDelay, buildingLimit, townhallLimit, itemsBalance, maxSimultaneousInBuilding, destinationBuilding, heldItems, heldItemsDestination, sound, priorityInvPenaltyItems, priorityInvPenaltyBase, handlerParams, sentenceKey, labelKey, tag, leisure);
        }
        catch (Exception e) {
            LOGGER.error("Error parsing gathering type {}", (Object)filename, (Object)e);
            return null;
        }
    }

    @Nullable
    private static Map<String, String> parseStringStringMap(JsonObject json, String key) {
        if (!json.has(key)) {
            return null;
        }
        HashMap<String, String> map = new HashMap<String, String>();
        JsonObject obj = GsonHelper.getAsJsonObject((JsonObject)json, (String)key);
        for (String k : obj.keySet()) {
            map.put(k, obj.get(k).getAsString());
        }
        return map;
    }
}

