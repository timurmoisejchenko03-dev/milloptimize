/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.google.gson.JsonArray
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  com.mojang.logging.LogUtils
 *  javax.annotation.Nullable
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.util.GsonHelper
 *  org.slf4j.Logger
 */
package org.millenaire.goal.visit;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.runtime.SwitchBootstraps;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import org.millenaire.content.ContentDirectoryManager;
import org.millenaire.content.ContentFs;
import org.millenaire.content.CustomContentIndex;
import org.millenaire.content.DisabledIdsLoader;
import org.millenaire.content.Resource;
import org.millenaire.content.SourceKind;
import org.millenaire.content.SubmodRoot;
import org.millenaire.culture.CultureLoader;
import org.millenaire.culture.JsonLoaderUtils;
import org.millenaire.goal.GoalRegistry;
import org.millenaire.goal.VillagerGoal;
import org.millenaire.goal.impl.ObserveVillagerGoal;
import org.millenaire.goal.impl.PlayGoal;
import org.millenaire.goal.impl.VisitBuildingGoal;
import org.millenaire.goal.visit.SpecialPointResolver;
import org.millenaire.goal.visit.VisitGoalSchema;
import org.slf4j.Logger;

public final class VisitGoalLoader {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = JsonLoaderUtils.GSON;
    private static final String VISIT_GOAL_DIR = "/millenaire/visit_goal";
    private static final Set<String> WARNED_REPLACE_IDS = ConcurrentHashMap.newKeySet();

    public static void resetForTesting() {
        WARNED_REPLACE_IDS.clear();
    }

    private VisitGoalLoader() {
    }

    public static void loadAll(GoalRegistry goalRegistry) {
        WARNED_REPLACE_IDS.clear();
        List<String> filenames = VisitGoalLoader.discoverVisitGoals();
        if (filenames.isEmpty()) {
            throw new RuntimeException("No visit_goal/*.json files found on the classpath under /millenaire/visit_goal \u2014 check that the resource directory is packaged correctly");
        }
        int count = 0;
        for (String filename : filenames) {
            VisitGoalSchema schema = VisitGoalLoader.parseFile(filename);
            if (schema == null) {
                throw new RuntimeException("Failed to load visit_goal file '" + filename + ".json' \u2014 see logs above");
            }
            VillagerGoal goal = VisitGoalLoader.buildGoal(schema);
            goalRegistry.register(goal);
            ++count;
            LOGGER.debug("Visit goal loaded: {}", (Object)schema.id());
        }
        LOGGER.info("{} visit goals loaded", (Object)count);
        VisitGoalLoader.loadPerCultureGoals(goalRegistry, CultureLoader.BUILTIN_CULTURES);
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    @Nullable
    static VisitGoalSchema parseFile(String filename) {
        String path = "/millenaire/visit_goal/" + filename + ".json";
        try (InputStream is = VisitGoalLoader.class.getResourceAsStream(path);){
            JsonObject json;
            if (is == null) {
                LOGGER.error("Visit goal file not found: {}", (Object)path);
                VisitGoalSchema visitGoalSchema2 = null;
                return visitGoalSchema2;
            }
            try (InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8);){
                json = (JsonObject)GSON.fromJson((Reader)reader, JsonObject.class);
            }
            VisitGoalSchema visitGoalSchema = VisitGoalLoader.parseSchema(filename, json);
            return visitGoalSchema;
        }
        catch (Exception e) {
            LOGGER.error("Error parsing visit goal {}: {}", new Object[]{filename, e.getMessage(), e});
            return null;
        }
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    @Nullable
    static VisitGoalSchema parsePerCultureFile(String culture, String id) {
        String path = "/millenaire/cultures/" + culture + "/goal/" + id + ".json";
        String combinedFilename = culture + "/" + id;
        try (InputStream is = VisitGoalLoader.class.getResourceAsStream(path);){
            JsonObject json;
            if (is == null) {
                LOGGER.error("Per-culture visit goal file not found: {}", (Object)path);
                VisitGoalSchema visitGoalSchema2 = null;
                return visitGoalSchema2;
            }
            try (InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8);){
                json = (JsonObject)GSON.fromJson((Reader)reader, JsonObject.class);
            }
            VisitGoalSchema visitGoalSchema = VisitGoalLoader.parseSchema(combinedFilename, json);
            return visitGoalSchema;
        }
        catch (Exception e) {
            LOGGER.error("Error parsing per-culture visit goal {}: {}", new Object[]{path, e.getMessage(), e});
            return null;
        }
    }

    /*
     * Enabled aggressive exception aggregation
     */
    @Nullable
    static VisitGoalSchema parsePerCultureExternal(Path file, String culture, String id) {
        String combinedFilename = culture + "/" + id;
        if (!(Files.isRegularFile(file, new LinkOption[0]) && ContentDirectoryManager.isInsideRoot(file) && ContentDirectoryManager.checkSize(file, 1000000L))) {
            return null;
        }
        try (InputStream is = Files.newInputStream(file, new OpenOption[0]);){
            VisitGoalSchema visitGoalSchema;
            try (InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8);){
                JsonObject json = (JsonObject)GSON.fromJson((Reader)reader, JsonObject.class);
                visitGoalSchema = VisitGoalLoader.parseSchema(combinedFilename, json);
            }
            return visitGoalSchema;
        }
        catch (Exception e) {
            LOGGER.error("Error parsing external per-culture visit goal {}: {}", new Object[]{file, e.getMessage(), e});
            return null;
        }
    }

    /*
     * Enabled aggressive exception aggregation
     */
    @Nullable
    static VisitGoalSchema parsePerCultureExternal(Resource res, String culture, String id) {
        String combinedFilename = culture + "/" + id;
        try (InputStream is = res.open();){
            VisitGoalSchema visitGoalSchema;
            try (InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8);){
                JsonObject json = (JsonObject)GSON.fromJson((Reader)reader, JsonObject.class);
                visitGoalSchema = VisitGoalLoader.parseSchema(combinedFilename, json);
            }
            return visitGoalSchema;
        }
        catch (Exception e) {
            LOGGER.error("Error parsing external per-culture visit goal {}: {}", new Object[]{res.relPath(), e.getMessage(), e});
            return null;
        }
    }

    static VisitGoalSchema parseSchema(String filename, JsonObject json) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)filename);
        String type = GsonHelper.getAsString((JsonObject)json, (String)"type");
        String goalKey = GsonHelper.getAsString((JsonObject)json, (String)"goalKey");
        return switch (type) {
            case "visitBuilding" -> VisitGoalLoader.parseVisitBuilding(filename, id, goalKey, json);
            case "observeVillager" -> VisitGoalLoader.parseObserveVillager(filename, id, goalKey, json);
            case "play" -> VisitGoalLoader.parsePlay(filename, id, goalKey, json);
            default -> throw new IllegalArgumentException("Unknown visit goal type '" + type + "' in " + filename + ".json");
        };
    }

    private static VisitGoalSchema.VisitBuilding parseVisitBuilding(String filename, ResourceLocation id, String goalKey, JsonObject json) {
        String buildingTag = GsonHelper.getAsString((JsonObject)json, (String)"buildingTag");
        String requiredTag = VisitGoalLoader.getStringOrNull(json, "requiredTag");
        int basePriority = GsonHelper.getAsInt((JsonObject)json, (String)"basePriority");
        int priorityRandom = GsonHelper.getAsInt((JsonObject)json, (String)"priorityRandom", (int)0);
        int durationTicks = GsonHelper.getAsInt((JsonObject)json, (String)"durationTicks");
        int reoccurDelayTicks = GsonHelper.getAsInt((JsonObject)json, (String)"reoccurDelayTicks", (int)-1);
        boolean allowRandomMoves = GsonHelper.getAsBoolean((JsonObject)json, (String)"allowRandomMoves", (boolean)false);
        boolean leisure = GsonHelper.getAsBoolean((JsonObject)json, (String)"leisure", (boolean)true);
        String targetPosType = SpecialPointResolver.resolveOrThrow(VisitGoalLoader.getStringOrNull(json, "targetPosType"), filename + ".json");
        int minimumHour = GsonHelper.getAsInt((JsonObject)json, (String)"minimumHour", (int)-1);
        int maximumHour = GsonHelper.getAsInt((JsonObject)json, (String)"maximumHour", (int)-1);
        int maxSimultaneousInBuilding = GsonHelper.getAsInt((JsonObject)json, (String)"maxSimultaneousInBuilding", (int)0);
        List<String> heldItems = VisitGoalLoader.parseItemList(json, "heldItems", filename);
        List<String> heldItemsDestination = VisitGoalLoader.parseItemList(json, "heldItemsDestination", filename);
        return new VisitGoalSchema.VisitBuilding(id, goalKey, buildingTag, requiredTag, basePriority, priorityRandom, durationTicks, reoccurDelayTicks, allowRandomMoves, leisure, targetPosType, minimumHour, maximumHour, maxSimultaneousInBuilding, heldItems, heldItemsDestination);
    }

    private static VisitGoalSchema.ObserveVillager parseObserveVillager(String filename, ResourceLocation id, String goalKey, JsonObject json) {
        VisitGoalLoader.enforceGoalKeyMatchesId("observeVillager", filename, id, goalKey);
        String targetGoalTag = VisitGoalLoader.getStringOrNull(json, "targetGoalTag");
        Set<ResourceLocation> targetGoalIds = VisitGoalLoader.parseResourceLocationSet(json, "targetGoalIds");
        if (targetGoalTag == null == (targetGoalIds == null)) {
            throw new IllegalArgumentException("observeVillager " + filename + ": exactly one of targetGoalTag / targetGoalIds must be set");
        }
        int basePriority = GsonHelper.getAsInt((JsonObject)json, (String)"basePriority");
        int priorityRandom = GsonHelper.getAsInt((JsonObject)json, (String)"priorityRandom", (int)0);
        int durationTicks = GsonHelper.getAsInt((JsonObject)json, (String)"durationTicks");
        int reoccurDelayTicks = GsonHelper.getAsInt((JsonObject)json, (String)"reoccurDelayTicks", (int)-1);
        int minimumHour = GsonHelper.getAsInt((JsonObject)json, (String)"minimumHour", (int)-1);
        int maximumHour = GsonHelper.getAsInt((JsonObject)json, (String)"maximumHour", (int)-1);
        String buildingTag = VisitGoalLoader.getStringOrNull(json, "buildingTag");
        String requiredTag = VisitGoalLoader.getStringOrNull(json, "requiredTag");
        List<String> heldItems = VisitGoalLoader.parseItemList(json, "heldItems", filename);
        return new VisitGoalSchema.ObserveVillager(id, goalKey, targetGoalTag, targetGoalIds, basePriority, priorityRandom, durationTicks, reoccurDelayTicks, minimumHour, maximumHour, buildingTag, requiredTag, heldItems);
    }

    private static VisitGoalSchema.Play parsePlay(String filename, ResourceLocation id, String goalKey, JsonObject json) {
        VisitGoalLoader.enforceGoalKeyMatchesId("play", filename, id, goalKey);
        boolean withFriends = GsonHelper.getAsBoolean((JsonObject)json, (String)"withFriends");
        return new VisitGoalSchema.Play(id, goalKey, withFriends);
    }

    private static void enforceGoalKeyMatchesId(String variant, String filename, ResourceLocation id, String goalKey) {
        if (!id.getPath().equals(goalKey)) {
            throw new IllegalArgumentException(variant + " " + filename + ".json: goalKey '" + goalKey + "' must equal id path '" + id.getPath() + "' \u2014 the " + variant + " engine does not support goalKey overrides");
        }
    }

    @Nullable
    private static String getStringOrNull(JsonObject json, String key) {
        if (!json.has(key)) {
            return null;
        }
        JsonElement el = json.get(key);
        if (el == null || el.isJsonNull()) {
            return null;
        }
        return el.getAsString();
    }

    @Nullable
    private static List<String> parseItemList(JsonObject json, String key, String filename) {
        if (!json.has(key)) {
            return null;
        }
        JsonElement el = json.get(key);
        if (el == null || el.isJsonNull()) {
            return null;
        }
        JsonArray arr = el.getAsJsonArray();
        if (arr.size() == 0) {
            return null;
        }
        ArrayList<String> items = new ArrayList<String>(arr.size());
        for (JsonElement item : arr) {
            String itemId = item.getAsString();
            if (itemId == null || itemId.isEmpty()) {
                throw new IllegalArgumentException("Empty item id in " + filename + ".json field '" + key + "'");
            }
            items.add(itemId);
        }
        return List.copyOf(items);
    }

    @Nullable
    private static Set<ResourceLocation> parseResourceLocationSet(JsonObject json, String key) {
        if (!json.has(key)) {
            return null;
        }
        JsonElement el = json.get(key);
        if (el == null || el.isJsonNull()) {
            return null;
        }
        JsonArray arr = el.getAsJsonArray();
        if (arr.size() == 0) {
            return null;
        }
        LinkedHashSet<ResourceLocation> out = new LinkedHashSet<ResourceLocation>();
        for (JsonElement rl : arr) {
            out.add(ResourceLocation.parse((String)rl.getAsString()));
        }
        return Collections.unmodifiableSet(out);
    }

    private static VillagerGoal buildGoal(VisitGoalSchema schema) {
        VisitGoalSchema visitGoalSchema = schema;
        Objects.requireNonNull(visitGoalSchema);
        VisitGoalSchema visitGoalSchema2 = visitGoalSchema;
        int n = 0;
        return switch (SwitchBootstraps.typeSwitch("typeSwitch", new Object[]{VisitGoalSchema.VisitBuilding.class, VisitGoalSchema.ObserveVillager.class, VisitGoalSchema.Play.class}, (Object)visitGoalSchema2, n)) {
            default -> throw new MatchException(null, null);
            case 0 -> {
                VisitGoalSchema.VisitBuilding vb = (VisitGoalSchema.VisitBuilding)visitGoalSchema2;
                yield VisitBuildingGoal.fromSchema(vb);
            }
            case 1 -> {
                VisitGoalSchema.ObserveVillager ov = (VisitGoalSchema.ObserveVillager)visitGoalSchema2;
                yield ObserveVillagerGoal.fromSchema(ov);
            }
            case 2 -> {
                VisitGoalSchema.Play p = (VisitGoalSchema.Play)visitGoalSchema2;
                yield PlayGoal.fromSchema(p);
            }
        };
    }

    static List<String> discoverVisitGoals() {
        ArrayList<String> names;
        block10: {
            names = new ArrayList<String>();
            try {
                URL url = VisitGoalLoader.class.getResource(VISIT_GOAL_DIR);
                if (url == null) {
                    LOGGER.warn("Visit goal directory not found in classpath: {}", (Object)VISIT_GOAL_DIR);
                    return names;
                }
                URI uri = url.toURI();
                if ("jar".equals(uri.getScheme())) {
                    try (FileSystem fs = FileSystems.newFileSystem(uri, Map.of());){
                        Path dirPath = fs.getPath(VISIT_GOAL_DIR, new String[0]);
                        VisitGoalLoader.collectJsonNames(dirPath, names);
                        break block10;
                    }
                }
                Path dirPath = Path.of(uri);
                VisitGoalLoader.collectJsonNames(dirPath, names);
            }
            catch (Exception e) {
                LOGGER.error("Error scanning visit_goal directory", (Throwable)e);
            }
        }
        return names;
    }

    private static void collectJsonNames(Path dirPath, List<String> names) throws IOException {
        try (Stream<Path> stream = Files.list(dirPath);){
            stream.filter(p -> p.toString().endsWith(".json")).map(p -> p.getFileName().toString().replace(".json", "")).sorted().forEach(names::add);
        }
    }

    public static void loadPerCultureGoals(GoalRegistry goalRegistry, List<String> cultures) {
        if (cultures == null || cultures.isEmpty()) {
            return;
        }
        int total = 0;
        for (String culture : cultures) {
            Set<String> disabledIds = VisitGoalLoader.resolvePerCultureDisabledIds(culture);
            List<String> ids = VisitGoalLoader.discoverCultureGoals(culture);
            for (String id : ids) {
                if (disabledIds.contains(id)) {
                    LOGGER.debug("Skipping per-culture visit goal '{}/{}' (listed in _disabled.json)", (Object)culture, (Object)id);
                    continue;
                }
                String classpathPath = "/millenaire/cultures/" + culture + "/goal/" + id + ".json";
                VisitGoalLoader.warnIfMultipleSubmodsShipReplace(culture, id, "visit_goal:" + culture + "/" + id);
                VisitGoalSchema schema = VisitGoalLoader.loadPerCultureSchema(culture, id, classpathPath);
                if (schema == null) continue;
                VillagerGoal goal = VisitGoalLoader.buildGoal(schema);
                goalRegistry.register(goal);
                ++total;
                LOGGER.debug("Per-culture visit goal loaded: {}", (Object)schema.id());
            }
        }
        if (total > 0) {
            LOGGER.info("{} per-culture visit goals loaded", (Object)total);
        }
    }

    public static void loadExternalPerCultureGoals(GoalRegistry goalRegistry, List<String> cultures) {
        if (!ContentDirectoryManager.isInitialized()) {
            return;
        }
        if (cultures == null || cultures.isEmpty()) {
            return;
        }
        WARNED_REPLACE_IDS.clear();
        int added = 0;
        int skipped = 0;
        for (String culture : cultures) {
            List<SubmodRoot> roots = CustomContentIndex.current().rootsForCulture(culture);
            if (roots.isEmpty()) continue;
            Set<String> disabledIds = VisitGoalLoader.resolvePerCultureDisabledIds(culture);
            HashSet<String> alreadyLoaded = new HashSet<String>(VisitGoalLoader.discoverCultureGoals(culture));
            HashSet<String> seenInThisRun = new HashSet<String>();
            for (SubmodRoot submod : roots) {
                Path goalDir = submod.root().resolve("cultures").resolve(culture).resolve("goal");
                if (!Files.isDirectory(goalDir, new LinkOption[0])) continue;
                try {
                    Stream<Path> stream = ContentDirectoryManager.safeWalk(goalDir);
                    try {
                        for (Path p : stream::iterator) {
                            String id;
                            String name;
                            if (!Files.isRegularFile(p, new LinkOption[0]) || (name = p.getFileName().toString()).startsWith("_") || !name.endsWith(".json") || alreadyLoaded.contains(id = name.substring(0, name.length() - 5))) continue;
                            if (disabledIds.contains(id)) {
                                ++skipped;
                                continue;
                            }
                            if (!seenInThisRun.add(id)) {
                                String warnKey = "visit_goal:" + culture + "/" + id;
                                if (!WARNED_REPLACE_IDS.add(warnKey)) continue;
                                LOGGER.warn("Per-culture visit goal '{}/{}' already loaded from an earlier sub-mod; ignoring duplicate from '{}'", new Object[]{culture, id, submod.displayName()});
                                continue;
                            }
                            VisitGoalSchema schema = VisitGoalLoader.parsePerCultureExternal(p, culture, id);
                            if (schema == null) continue;
                            VillagerGoal goal = VisitGoalLoader.buildGoal(schema);
                            goalRegistry.register(goal);
                            ++added;
                            LOGGER.debug("Loaded external per-culture visit goal '{}/{}' from sub-mod '{}'", new Object[]{culture, id, submod.displayName()});
                        }
                    }
                    finally {
                        if (stream == null) continue;
                        stream.close();
                    }
                }
                catch (IOException e) {
                    LOGGER.error("Failed to walk external per-culture goal dir {}: {}", new Object[]{goalDir, e.getMessage(), e});
                }
            }
        }
        if (added + skipped > 0) {
            LOGGER.info("External per-culture visit goals: {} added, {} disabled", (Object)added, (Object)skipped);
        }
    }

    @Nullable
    private static VisitGoalSchema loadPerCultureSchema(String culture, String id, String classpathPath) {
        Resource res;
        ContentFs cultureFs;
        Optional<Resource> hit;
        if (ContentDirectoryManager.isInitialized() && (hit = (cultureFs = CustomContentIndex.current().forCulture(culture)).findFirst("goal/" + id + ".json")).isPresent() && ((res = hit.get()).kind() == SourceKind.SUBMOD || res.kind() == SourceKind.STANDARD)) {
            VisitGoalSchema schema = VisitGoalLoader.parsePerCultureExternal(res, culture, id);
            if (schema != null) {
                return schema;
            }
            LOGGER.warn("External per-culture visit goal {} failed to parse; falling back to JAR.", (Object)res.relPath());
        }
        return VisitGoalLoader.parsePerCultureFile(culture, id);
    }

    static List<String> discoverCultureGoals(String culture) {
        ArrayList<String> names;
        block10: {
            names = new ArrayList<String>();
            String dir = "/millenaire/cultures/" + culture + "/goal";
            try {
                URL url = VisitGoalLoader.class.getResource(dir);
                if (url == null) {
                    LOGGER.debug("Per-culture goal directory not found in classpath: {}", (Object)dir);
                    return names;
                }
                URI uri = url.toURI();
                if ("jar".equals(uri.getScheme())) {
                    try (FileSystem fs = FileSystems.newFileSystem(uri, Map.of());){
                        Path dirPath = fs.getPath(dir, new String[0]);
                        VisitGoalLoader.collectJsonNames(dirPath, names);
                        break block10;
                    }
                }
                Path dirPath = Path.of(uri);
                VisitGoalLoader.collectJsonNames(dirPath, names);
            }
            catch (Exception e) {
                LOGGER.error("Error scanning per-culture goal directory {}", (Object)dir, (Object)e);
            }
        }
        return names;
    }

    private static Set<String> resolvePerCultureDisabledIds(String culture) {
        if (!ContentDirectoryManager.isInitialized()) {
            return Collections.emptySet();
        }
        List<SubmodRoot> roots = CustomContentIndex.current().rootsForCulture(culture);
        if (roots.isEmpty()) {
            return Collections.emptySet();
        }
        ArrayList<Path> dirs = new ArrayList<Path>(roots.size());
        for (SubmodRoot root : roots) {
            dirs.add(root.root().resolve("cultures").resolve(culture).resolve("goal"));
        }
        return DisabledIdsLoader.loadUnion(dirs);
    }

    private static void warnIfMultipleSubmodsShipReplace(String culture, String id, String logicalId) {
        if (!ContentDirectoryManager.isInitialized()) {
            return;
        }
        ContentFs cultureFs = CustomContentIndex.current().forCulture(culture);
        List<Resource> all = cultureFs.findAll("goal/" + id + ".json");
        ArrayList<Resource> submods = new ArrayList<Resource>();
        for (Resource res : all) {
            if (res.kind() != SourceKind.SUBMOD) continue;
            submods.add(res);
        }
        if (submods.size() <= 1) {
            return;
        }
        if (!WARNED_REPLACE_IDS.add(logicalId)) {
            return;
        }
        LOGGER.warn("Multiple sub-mods ship {}: {}. Using {} (overlay top-most).", new Object[]{logicalId, submods.stream().map(r -> r.source().displayName()).toList(), ((Resource)submods.get(0)).source().displayName()});
    }
}

