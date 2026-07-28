/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.google.gson.GsonBuilder
 */
package org.millenaire.content.legacy;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.millenaire.content.legacy.LegacyConversionReport;

public class QuestDefinitionConverter {
    private static final Path PROJECT_DIR = Path.of(System.getProperty("user.dir"), new String[0]);
    private static final Path LEGACY_ROOT = QuestDefinitionConverter.resolveLegacyRoot();
    private static final Path QUEST_OUTPUT = PROJECT_DIR.resolve("src/main/resources/millenaire/quests");
    private final Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private int totalConverted = 0;
    private int totalWarnings = 0;
    private final List<String> allQuestKeys = new ArrayList<String>();
    private Issues issues;
    private static final Map<String, String> QUEST_ITEM_MAP;

    public static void main(String[] args) {
        QuestDefinitionConverter converter = new QuestDefinitionConverter();
        try {
            converter.run();
        }
        catch (Exception e) {
            System.err.println("[FATAL] Quest conversion failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private void run() throws IOException {
        List<Path> subdirs;
        Path questsRoot = LEGACY_ROOT.resolve("quests");
        if (!Files.isDirectory(questsRoot, new LinkOption[0])) {
            System.err.println("[FATAL] Legacy quests directory not found: " + String.valueOf(questsRoot));
            System.exit(1);
        }
        System.out.println("=== QuestDefinitionConverter ===");
        System.out.println("Legacy quests: " + String.valueOf(questsRoot));
        System.out.println("Output: " + String.valueOf(QUEST_OUTPUT));
        try (Stream<Path> stream = Files.list(questsRoot);){
            subdirs = stream.filter(x$0 -> Files.isDirectory(x$0, new LinkOption[0])).sorted().toList();
        }
        for (Path subdir : subdirs) {
            this.processSubdirectory(subdir);
        }
        this.generateManifest();
        System.out.println("\n=== Summary ===");
        System.out.println("Quests converted: " + this.totalConverted);
        System.out.println("Warnings: " + this.totalWarnings);
        System.out.println("\n\u2713 Quest definition conversion completed.");
    }

    private void processSubdirectory(Path subdir) throws IOException {
        List<Path> questFiles;
        String subdirName = subdir.getFileName().toString();
        Path outputDir = QUEST_OUTPUT.resolve(subdirName);
        Files.createDirectories(outputDir, new FileAttribute[0]);
        try (Stream<Path> stream = Files.list(subdir);){
            questFiles = stream.filter(p -> p.toString().endsWith(".txt")).sorted().toList();
        }
        System.out.println("\n[" + subdirName + "] " + questFiles.size() + " quest files");
        for (Path questFile : questFiles) {
            try {
                this.convertQuestFile(questFile, outputDir, subdirName);
                ++this.totalConverted;
            }
            catch (Exception e) {
                System.err.println("  [ERROR] " + String.valueOf(questFile.getFileName()) + ": " + e.getMessage());
                ++this.totalWarnings;
            }
        }
    }

    private void convertQuestFile(Path questFile, Path outputDir, String subdirName) throws IOException {
        String fileName = questFile.getFileName().toString();
        String questKey = fileName.substring(0, fileName.length() - ".txt".length());
        Map<String, Object> quest = this.buildQuestMap(questFile, questKey);
        Path outputFile = outputDir.resolve(questKey + ".json");
        try (BufferedWriter writer = Files.newBufferedWriter(outputFile, StandardCharsets.UTF_8, new OpenOption[0]);){
            this.gson.toJson(quest, (Appendable)writer);
        }
        List steps = (List)quest.get("steps");
        List villagerDefs = (List)quest.get("villagerDefs");
        this.allQuestKeys.add(subdirName + "/" + questKey);
        System.out.println("  \u2713 " + questKey + " (" + steps.size() + " steps, " + villagerDefs.size() + " villagers)");
    }

    public static Map<String, Object> parseQuest(Path questFile) throws IOException {
        return QuestDefinitionConverter.parseQuest(questFile, null);
    }

    public static Map<String, Object> parseQuest(Path questFile, Issues sink) throws IOException {
        QuestDefinitionConverter scratch = new QuestDefinitionConverter();
        scratch.issues = sink;
        String fileName = questFile.getFileName().toString();
        String questKey = fileName.endsWith(".txt") ? fileName.substring(0, fileName.length() - ".txt".length()) : fileName;
        return scratch.buildQuestMap(questFile, questKey);
    }

    private Map<String, Object> buildQuestMap(Path questFile, String questKey) throws IOException {
        List<String> lines = Files.readAllLines(questFile, StandardCharsets.UTF_8);
        double chancePerHour = 0.0;
        int maxSimultaneous = 5;
        int minReputation = 0;
        ArrayList<String> globalTagsRequired = new ArrayList<String>();
        ArrayList<String> globalTagsForbidden = new ArrayList<String>();
        ArrayList<String> playerTagsRequired = new ArrayList<String>();
        ArrayList<String> playerTagsForbidden = new ArrayList<String>();
        ArrayList<Map<String, Object>> villagerDefs = new ArrayList<Map<String, Object>>();
        ArrayList<Map<String, Object>> steps = new ArrayList<Map<String, Object>>();
        Map<String, Object> currentStep = null;
        int stepIndex = -1;
        block90: for (String string : lines) {
            String line = string.trim();
            if (line.isEmpty() || line.startsWith("//")) continue;
            int colonIdx = line.indexOf(58);
            if (colonIdx < 0) {
                this.warn(questKey, "No colon in line: " + line);
                continue;
            }
            String key = line.substring(0, colonIdx).trim().toLowerCase();
            String value = line.substring(colonIdx + 1).trim();
            if (key.equals("definevillager")) {
                villagerDefs.add(this.parseVillagerDef(value, questKey));
                continue;
            }
            if (key.equals("step") && value.equals("new")) {
                currentStep = this.newStep(++stepIndex);
                steps.add(currentStep);
                continue;
            }
            if (currentStep == null) {
                switch (key) {
                    case "minreputation": {
                        minReputation = QuestDefinitionConverter.evaluateArithmetic(value);
                        continue block90;
                    }
                    case "chanceperhour": {
                        chancePerHour = Double.parseDouble(value.trim());
                        continue block90;
                    }
                    case "maxsimultaneous": {
                        maxSimultaneous = Integer.parseInt(value.trim());
                        continue block90;
                    }
                    case "requiredplayertag": {
                        playerTagsRequired.add(value);
                        continue block90;
                    }
                    case "forbiddenplayertag": {
                        playerTagsForbidden.add(value);
                        continue block90;
                    }
                    case "requiredglobaltag": {
                        globalTagsRequired.add(value);
                        continue block90;
                    }
                    case "forbiddenglobaltag": {
                        globalTagsForbidden.add(value);
                        continue block90;
                    }
                }
                this.warn(questKey, "Unknown quest-level key (before step:new): " + key);
                continue;
            }
            switch (key) {
                case "minreputation": {
                    minReputation = QuestDefinitionConverter.evaluateArithmetic(value);
                    continue block90;
                }
                case "chanceperhour": {
                    chancePerHour = Double.parseDouble(value.trim());
                    continue block90;
                }
                case "maxsimultaneous": {
                    maxSimultaneous = Integer.parseInt(value.trim());
                    continue block90;
                }
                case "requiredplayertag": {
                    playerTagsRequired.add(value);
                    continue block90;
                }
                case "forbiddenplayertag": {
                    playerTagsForbidden.add(value);
                    continue block90;
                }
                case "requiredglobaltag": {
                    globalTagsRequired.add(value);
                    continue block90;
                }
                case "forbiddenglobaltag": {
                    globalTagsForbidden.add(value);
                    continue block90;
                }
                case "villager": {
                    currentStep.put("villagerKey", value);
                    continue block90;
                }
                case "duration": {
                    currentStep.put("duration", QuestDefinitionConverter.evaluateArithmetic(value));
                    continue block90;
                }
                case "showrequiredgoods": {
                    currentStep.put("showRequiredGoods", Boolean.parseBoolean(value));
                    continue block90;
                }
                case "requiredgood": {
                    this.addGood(currentStep, "requiredGoods", value, questKey);
                    continue block90;
                }
                case "rewardgood": {
                    this.addGood(currentStep, "rewardGoods", value, questKey);
                    continue block90;
                }
                case "rewardreputation": {
                    currentStep.put("rewardReputation", QuestDefinitionConverter.evaluateArithmetic(value));
                    continue block90;
                }
                case "rewardmoney": {
                    currentStep.put("rewardMoney", QuestDefinitionConverter.evaluateArithmetic(value));
                    continue block90;
                }
                case "penaltyreputation": {
                    currentStep.put("penaltyReputation", QuestDefinitionConverter.evaluateArithmetic(value));
                    continue block90;
                }
                case "settagsuccess": {
                    this.addVillagerTag(currentStep, "villagerTagsSuccess", value);
                    continue block90;
                }
                case "settagfailure": {
                    this.addVillagerTag(currentStep, "villagerTagsFailure", value);
                    continue block90;
                }
                case "setplayertagsuccess": {
                    this.addToList(currentStep, "playerTagsSuccess", value);
                    continue block90;
                }
                case "setplayertagfailure": {
                    this.addToList(currentStep, "playerTagsFailure", value);
                    continue block90;
                }
                case "setglobaltagsuccess": {
                    this.addToList(currentStep, "globalTagsSuccess", value);
                    continue block90;
                }
                case "setglobaltagfailure": {
                    this.addToList(currentStep, "globalTagsFailure", value);
                    continue block90;
                }
                case "cleartagsuccess": {
                    this.addVillagerTag(currentStep, "clearTagsSuccess", value);
                    continue block90;
                }
                case "cleartagfailure": {
                    this.addVillagerTag(currentStep, "clearTagsFailure", value);
                    continue block90;
                }
                case "clearplayertagsuccess": {
                    this.addToList(currentStep, "clearPlayerTagsSuccess", value);
                    continue block90;
                }
                case "clearplayertagfailure": {
                    this.addToList(currentStep, "clearPlayerTagsFailure", value);
                    continue block90;
                }
                case "clearglobaltagsuccess": {
                    this.addToList(currentStep, "clearGlobalTagsSuccess", value);
                    continue block90;
                }
                case "clearglobaltagfailure": {
                    this.addToList(currentStep, "clearGlobalTagsFailure", value);
                    continue block90;
                }
                case "steprequiredplayertag": {
                    this.addToList(currentStep, "stepRequiredPlayerTags", value);
                    continue block90;
                }
                case "stepforbiddenplayertag": {
                    this.addToList(currentStep, "stepForbiddenPlayerTags", value);
                    continue block90;
                }
                case "steprequiredglobaltag": {
                    this.addToList(currentStep, "stepRequiredGlobalTags", value);
                    continue block90;
                }
                case "stepforbiddenglobaltag": {
                    this.addToList(currentStep, "stepForbiddenGlobalTags", value);
                    continue block90;
                }
                case "setactiondatasuccess": {
                    this.addActionData(currentStep, "actionDataSuccess", value);
                    continue block90;
                }
                case "relationchange": {
                    this.addRelationChange(currentStep, value);
                    continue block90;
                }
                case "bedrockbuilding": {
                    this.addBedrockBuilding(currentStep, value);
                    continue block90;
                }
            }
            this.warn(questKey, "Unknown step key: " + key + " = " + value);
        }
        if (steps.isEmpty()) {
            this.structural(questKey, 0, "Quest has no steps");
        } else {
            for (int i = 0; i < steps.size(); ++i) {
                Number n;
                Object duration;
                Map map = (Map)steps.get(i);
                String villagerKey = String.valueOf(map.getOrDefault("villagerKey", ""));
                if (villagerKey.isEmpty()) {
                    this.structural(questKey, i + 1, "Step has no 'villager:' directive");
                }
                if ((duration = map.get("duration")) != null && (!(duration instanceof Number) || (n = (Number)duration).intValue() != 0)) continue;
                this.structural(questKey, i + 1, "Step has no 'duration:' (will use default)");
            }
        }
        for (Map map : villagerDefs) {
            Object key = map.get("key");
            if (key == null || String.valueOf(key).isEmpty()) {
                this.structural(questKey, 0, "definevillager missing 'key=' parameter");
            }
            List types = map.getOrDefault("villagerTypes", List.of());
            List requiredTags = map.getOrDefault("requiredTags", List.of());
            List forbiddenTags = map.getOrDefault("forbiddenTags", List.of());
            if (types.isEmpty() && requiredTags.isEmpty() && forbiddenTags.isEmpty()) {
                this.structural(questKey, 0, "definevillager '" + String.valueOf(key) + "' missing 'type=' and 'requiredtag=' \u2014 villager cannot be matched");
            }
            for (String type : types) {
                if (type.contains("/")) continue;
                if (this.issues != null) {
                    this.issues.report().recordMissingCulturePrefix(null, this.issues.filePath(), "definevillager.type", type);
                }
                System.out.println("  [WARN] " + questKey + ": Villager type '" + type + "' missing culture prefix (expected 'culture/type')");
                ++this.totalWarnings;
            }
        }
        LinkedHashMap<String, Object> quest = new LinkedHashMap<String, Object>();
        quest.put("key", questKey);
        quest.put("chancePerHour", chancePerHour);
        quest.put("maxSimultaneous", maxSimultaneous);
        quest.put("minReputation", minReputation);
        quest.put("globalTagsRequired", globalTagsRequired);
        quest.put("globalTagsForbidden", globalTagsForbidden);
        quest.put("playerTagsRequired", playerTagsRequired);
        quest.put("playerTagsForbidden", playerTagsForbidden);
        quest.put("villagerDefs", villagerDefs);
        quest.put("steps", steps);
        return quest;
    }

    private void structural(String questKey, int step, String problem) {
        System.out.println("  [WARN] " + questKey + ": " + problem);
        ++this.totalWarnings;
        if (this.issues != null) {
            this.issues.report().recordQuestStructural(this.issues.questKey(), this.issues.filePath(), step, problem);
        }
    }

    private Map<String, Object> parseVillagerDef(String value, String questKey) {
        String[] pairs;
        LinkedHashMap<String, Object> def = new LinkedHashMap<String, Object>();
        ArrayList<String> villagerTypes = new ArrayList<String>();
        ArrayList<String> requiredTags = new ArrayList<String>();
        ArrayList<String> forbiddenTags = new ArrayList<String>();
        String defKey = null;
        String relatedTo = null;
        String relation = null;
        block16: for (String pair : pairs = value.split(",")) {
            int eqIdx = pair.indexOf(61);
            if (eqIdx < 0) {
                this.warn(questKey, "Invalid definevillager pair: " + pair);
                continue;
            }
            String k = pair.substring(0, eqIdx).trim().toLowerCase();
            String v = pair.substring(eqIdx + 1).trim();
            switch (k) {
                case "key": {
                    defKey = v;
                    continue block16;
                }
                case "type": {
                    villagerTypes.add(v);
                    continue block16;
                }
                case "relatedto": {
                    relatedTo = v;
                    continue block16;
                }
                case "relation": {
                    relation = v;
                    continue block16;
                }
                case "requiredtag": {
                    requiredTags.add(v);
                    continue block16;
                }
                case "forbiddentag": {
                    forbiddenTags.add(v);
                    continue block16;
                }
                default: {
                    this.warn(questKey, "Unknown definevillager field: " + k);
                }
            }
        }
        def.put("key", defKey);
        def.put("villagerTypes", villagerTypes);
        def.put("relatedTo", relatedTo);
        def.put("relation", relation);
        def.put("requiredTags", requiredTags);
        def.put("forbiddenTags", forbiddenTags);
        return def;
    }

    private Map<String, Object> newStep(int index) {
        LinkedHashMap<String, Object> step = new LinkedHashMap<String, Object>();
        step.put("index", index);
        step.put("villagerKey", "");
        step.put("duration", 0);
        step.put("showRequiredGoods", true);
        step.put("requiredGoods", new LinkedHashMap());
        step.put("rewardGoods", new LinkedHashMap());
        step.put("rewardMoney", 0);
        step.put("rewardReputation", 0);
        step.put("penaltyReputation", 0);
        step.put("villagerTagsSuccess", new ArrayList());
        step.put("villagerTagsFailure", new ArrayList());
        step.put("playerTagsSuccess", new ArrayList());
        step.put("playerTagsFailure", new ArrayList());
        step.put("globalTagsSuccess", new ArrayList());
        step.put("globalTagsFailure", new ArrayList());
        step.put("clearTagsSuccess", new ArrayList());
        step.put("clearTagsFailure", new ArrayList());
        step.put("clearPlayerTagsSuccess", new ArrayList());
        step.put("clearPlayerTagsFailure", new ArrayList());
        step.put("clearGlobalTagsSuccess", new ArrayList());
        step.put("clearGlobalTagsFailure", new ArrayList());
        step.put("stepRequiredGlobalTags", new ArrayList());
        step.put("stepForbiddenGlobalTags", new ArrayList());
        step.put("stepRequiredPlayerTags", new ArrayList());
        step.put("stepForbiddenPlayerTags", new ArrayList());
        step.put("actionDataSuccess", new ArrayList());
        step.put("relationChanges", new ArrayList());
        step.put("bedrockBuildings", new ArrayList());
        return step;
    }

    private void addGood(Map<String, Object> step, String field, String value, String questKey) {
        String[] parts = value.split(",");
        if (parts.length < 2) {
            this.warn(questKey, "Invalid good format: " + value);
            return;
        }
        String legacyItem = parts[0].trim();
        int count = QuestDefinitionConverter.evaluateArithmetic(parts[1].trim());
        String modernItem = this.mapItem(legacyItem, questKey);
        if (modernItem == null) {
            return;
        }
        Map goods = (Map)step.get(field);
        goods.merge(modernItem, count, Integer::sum);
    }

    private String mapItem(String legacyItem, String questKey) {
        String mapped = QUEST_ITEM_MAP.get(legacyItem);
        if (mapped == null) {
            mapped = QUEST_ITEM_MAP.get(legacyItem.toLowerCase());
        }
        if (mapped == null) {
            if (legacyItem.contains(":")) {
                return legacyItem;
            }
            this.warn(questKey, "Unmapped quest item: " + legacyItem + " \u2014 dropping (add a mapping in QUEST_ITEM_MAP)");
            return null;
        }
        return mapped;
    }

    private void addVillagerTag(Map<String, Object> step, String field, String value) {
        String[] parts = value.split(",");
        if (parts.length >= 2) {
            LinkedHashMap<String, String> tag = new LinkedHashMap<String, String>();
            tag.put("villagerKey", parts[0].trim());
            tag.put("tag", parts[1].trim());
            ((List)step.get(field)).add(tag);
        }
    }

    private void addToList(Map<String, Object> step, String field, String value) {
        ((List)step.get(field)).add(value.trim());
    }

    private void addActionData(Map<String, Object> step, String field, String value) {
        String[] parts = value.split(",");
        if (parts.length >= 2) {
            LinkedHashMap<String, String> entry = new LinkedHashMap<String, String>();
            entry.put("key", parts[0].trim());
            entry.put("value", parts[1].trim());
            ((List)step.get(field)).add(entry);
        }
    }

    private void addRelationChange(Map<String, Object> step, String value) {
        String[] parts = value.split(",");
        if (parts.length >= 3) {
            LinkedHashMap<String, Object> change = new LinkedHashMap<String, Object>();
            change.put("firstVillager", parts[0].trim());
            change.put("secondVillager", parts[1].trim());
            change.put("change", QuestDefinitionConverter.evaluateArithmetic(parts[2].trim()));
            ((List)step.get("relationChanges")).add(change);
        }
    }

    private void addBedrockBuilding(Map<String, Object> step, String value) {
        String[] parts = value.split(",");
        if (parts.length >= 2) {
            LinkedHashMap<String, String> building = new LinkedHashMap<String, String>();
            building.put("culture", parts[0].trim());
            building.put("villageType", parts[1].trim());
            ((List)step.get("bedrockBuildings")).add(building);
        }
    }

    static int evaluateArithmetic(String expr) {
        if ((expr = expr.trim()).contains("*")) {
            String[] parts = expr.split("\\*");
            int result = 1;
            for (String part : parts) {
                result *= Integer.parseInt(part.trim());
            }
            return result;
        }
        return Integer.parseInt(expr);
    }

    private void generateManifest() throws IOException {
        this.allQuestKeys.sort(String::compareTo);
        Path manifestFile = QUEST_OUTPUT.resolve("_manifest.json");
        try (BufferedWriter writer = Files.newBufferedWriter(manifestFile, StandardCharsets.UTF_8, new OpenOption[0]);){
            this.gson.toJson(this.allQuestKeys, (Appendable)writer);
        }
        System.out.println("\nManifest: " + this.allQuestKeys.size() + " entries \u2192 " + String.valueOf(manifestFile));
    }

    private void warn(String questKey, String message) {
        System.out.println("  [WARN] " + questKey + ": " + message);
        ++this.totalWarnings;
        if (this.issues != null) {
            this.issues.report().recordQuestStructural(this.issues.questKey(), this.issues.filePath(), 0, message);
        }
    }

    private static Path resolveLegacyRoot() {
        Path relative = PROJECT_DIR.resolve("../millenaire-1.12/content/millenaire");
        if (Files.isDirectory(relative, new LinkOption[0])) {
            return relative;
        }
        try {
            Path fromMainRepo;
            Path mainRepoRoot;
            ProcessBuilder pb = new ProcessBuilder("git", "rev-parse", "--path-format=absolute", "--git-common-dir");
            pb.directory(PROJECT_DIR.toFile());
            pb.redirectErrorStream(true);
            Process p = pb.start();
            String gitCommonDir = new String(p.getInputStream().readAllBytes()).trim();
            p.waitFor();
            if (p.exitValue() == 0 && (mainRepoRoot = Path.of(gitCommonDir, new String[0]).getParent()) != null && Files.isDirectory(fromMainRepo = mainRepoRoot.resolve("../millenaire-1.12/content/millenaire"), new LinkOption[0])) {
                return fromMainRepo;
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
        return relative;
    }

    static {
        LinkedHashMap<String, String> m = new LinkedHashMap<String, String>();
        m.put("cake", "minecraft:cake");
        m.put("bread", "minecraft:bread");
        m.put("apple", "minecraft:apple");
        m.put("cookie", "minecraft:cookie");
        m.put("cobblestone", "minecraft:cobblestone");
        m.put("stone", "minecraft:stone");
        m.put("iron", "minecraft:iron_ingot");
        m.put("gold", "minecraft:gold_ingot");
        m.put("diamond", "minecraft:diamond");
        m.put("coal", "minecraft:coal");
        m.put("redstone", "minecraft:redstone");
        m.put("obsidian", "minecraft:obsidian");
        m.put("paper", "minecraft:paper");
        m.put("book", "minecraft:book");
        m.put("bone", "minecraft:bone");
        m.put("leather", "minecraft:leather");
        m.put("feather", "minecraft:feather");
        m.put("egg", "minecraft:egg");
        m.put("string", "minecraft:string");
        m.put("flint", "minecraft:flint");
        m.put("ironore", "minecraft:raw_iron");
        m.put("goldore", "minecraft:raw_gold");
        m.put("dye_black", "minecraft:black_dye");
        m.put("dye_red", "minecraft:red_dye");
        m.put("dye_yellow", "minecraft:yellow_dye");
        m.put("dye_blue", "minecraft:lapis_lazuli");
        m.put("dye_lightblue", "minecraft:light_blue_dye");
        m.put("dye_brown", "minecraft:cocoa_beans");
        m.put("steelsword", "minecraft:iron_sword");
        m.put("steelhelmet", "minecraft:iron_helmet");
        m.put("steelchest", "minecraft:iron_chestplate");
        m.put("steellegs", "minecraft:iron_leggings");
        m.put("steelboots", "minecraft:iron_boots");
        m.put("diamondsword", "minecraft:diamond_sword");
        m.put("bow", "minecraft:bow");
        m.put("arrow", "minecraft:arrow");
        m.put("bucketempty", "minecraft:bucket");
        m.put("bucketwater", "minecraft:water_bucket");
        m.put("bucketlava", "minecraft:lava_bucket");
        m.put("mushroomred", "minecraft:red_mushroom");
        m.put("mushroombrown", "minecraft:brown_mushroom");
        m.put("vines", "minecraft:vine");
        m.put("rottenflesh", "minecraft:rotten_flesh");
        m.put("spidereye", "minecraft:spider_eye");
        m.put("ghasttear", "minecraft:ghast_tear");
        m.put("gunpowder", "minecraft:gunpowder");
        m.put("tnt", "minecraft:tnt");
        m.put("netherwart", "minecraft:nether_wart");
        m.put("cactus", "minecraft:cactus");
        m.put("sugarcane", "minecraft:sugar_cane");
        m.put("pumpkin", "minecraft:pumpkin");
        m.put("enderpearl", "minecraft:ender_pearl");
        m.put("yellowflower", "minecraft:dandelion");
        m.put("redflower", "minecraft:poppy");
        m.put("fishraw", "minecraft:cod");
        m.put("fishcooked", "minecraft:cooked_cod");
        m.put("chickenmeat", "minecraft:chicken");
        m.put("beefraw", "minecraft:beef");
        m.put("rabbit", "minecraft:rabbit");
        m.put("potato", "minecraft:potato");
        m.put("seeds", "minecraft:wheat_seeds");
        m.put("sapling", "minecraft:oak_sapling");
        m.put("sapling_birch", "minecraft:birch_sapling");
        m.put("ironnugget", "minecraft:iron_nugget");
        m.put("cider", "millenaire:cider");
        m.put("calva", "millenaire:calva");
        m.put("boudin", "millenaire:boudin");
        m.put("tripes", "millenaire:tripes");
        m.put("tapestry", "millenaire:wall_tapestry");
        m.put("normansword", "millenaire:norman_sword");
        m.put("normanbroadsword", "millenaire:norman_sword");
        m.put("normanaxe", "millenaire:norman_axe");
        m.put("normanpickaxe", "millenaire:norman_pickaxe");
        m.put("normanshovel", "millenaire:norman_shovel");
        m.put("rasgulla", "millenaire:rasgulla");
        m.put("rice", "millenaire:rice");
        m.put("turmeric", "millenaire:turmeric");
        m.put("chickencurry", "millenaire:chickencurry");
        m.put("vegcurry", "millenaire:vegcurry");
        m.put("cotton", "millenaire:cotton");
        m.put("cacauhaa", "millenaire:cacauhaa");
        m.put("winefancy", "millenaire:winefancy");
        m.put("bearmeat_raw", "millenaire:bearmeat_raw");
        m.put("inuitpotatostew", "millenaire:inuitpotatostew");
        m.put("ayran", "millenaire:ayran");
        m.put("pide", "millenaire:pide");
        m.put("helva", "millenaire:helva");
        m.put("mudbrick", "millenaire:mud_brick");
        m.put("mudbrick_seljuk_ornamented", "millenaire:mud_brick_seljuk_ornamented");
        m.put("unknownpowder", "millenaire:unknown_powder");
        m.put("alchemistexplosive", "millenaire:alchemist_explosive");
        m.put("alchemist_amulet", "millenaire:alchemist_amulet");
        m.put("vishnu_amulet", "millenaire:vishnu_amulet");
        m.put("parchment_sadhu", "millenaire:parchment_sadhu");
        m.put("mayanquestcrown", "millenaire:mayan_quest_crown");
        m.put("enchantedsword", "minecraft:iron_sword");
        QUEST_ITEM_MAP = Collections.unmodifiableMap(m);
    }

    public record Issues(LegacyConversionReport report, String questKey, String filePath) {
    }
}

