/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  net.neoforged.neoforge.common.ModConfigSpec
 *  net.neoforged.neoforge.common.ModConfigSpec$BooleanValue
 *  net.neoforged.neoforge.common.ModConfigSpec$Builder
 *  net.neoforged.neoforge.common.ModConfigSpec$ConfigValue
 *  net.neoforged.neoforge.common.ModConfigSpec$EnumValue
 *  net.neoforged.neoforge.common.ModConfigSpec$IntValue
 *  org.apache.commons.lang3.tuple.Pair
 */
package org.millenaire.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;
import org.millenaire.config.NavDriverType;

public final class MillenaireServerConfig {
    public static final MillenaireServerConfig SERVER;
    public static final ModConfigSpec SPEC;
    public final ModConfigSpec.BooleanValue generateVillages;
    public final ModConfigSpec.BooleanValue generateLoneBuildings;
    public final ModConfigSpec.BooleanValue generateHamlets;
    public final ModConfigSpec.IntValue minVillageDistance;
    public final ModConfigSpec.IntValue minLoneBuildingDistance;
    public final ModConfigSpec.IntValue minVillageLoneBuildingDistance;
    public final ModConfigSpec.IntValue spawnProtectionRadius;
    public final ModConfigSpec.IntValue completionMaxPercentage;
    public final ModConfigSpec.IntValue completionMinDistance;
    public final ModConfigSpec.IntValue completionMaxDistance;
    public final ModConfigSpec.BooleanValue logSpawnAttempts;
    public final ModConfigSpec.IntValue keepActiveRadius;
    public final ModConfigSpec.IntValue villageRadiusOverride;
    public final ModConfigSpec.IntValue minBuildingSpacing;
    public final ModConfigSpec.BooleanValue buildPaths;
    public final ModConfigSpec.IntValue maxChildren;
    public final ModConfigSpec.IntValue backgroundRadius;
    public final ModConfigSpec.EnumValue<NavDriverType> navDriver;
    public final ModConfigSpec.BooleanValue languageLearning;
    public final ModConfigSpec.BooleanValue travelBookLearning;
    public final ModConfigSpec.IntValue sentenceDistanceSingleplayer;
    public final ModConfigSpec.IntValue sentenceDistanceMultiplayer;
    public final ModConfigSpec.BooleanValue ignoreResourceCost;
    public final ModConfigSpec.BooleanValue sendStatistics;
    public final ModConfigSpec.BooleanValue sendPlayerName;
    public final ModConfigSpec.ConfigValue<String> apiBaseUrlOverride;
    public final ModConfigSpec.IntValue banditRaidRadius;
    public final ModConfigSpec.IntValue raidingRate;
    public final ModConfigSpec.BooleanValue legacyAutoConvert;
    public final ModConfigSpec.IntValue legacyAutoConvertMaxPngs;

    private MillenaireServerConfig(ModConfigSpec.Builder builder) {
        builder.comment("World generation settings").push("generation");
        this.generateVillages = builder.comment("Generate Millenaire villages in new chunks").translation("millenaire.config.generateVillages").define("generateVillages", true);
        this.generateLoneBuildings = builder.comment("Generate lone buildings (inns, shrines, ruins) in new chunks").translation("millenaire.config.generateLoneBuildings").define("generateLoneBuildings", true);
        this.generateHamlets = builder.comment(new String[]{"Generate hamlet satellite villages around parent village types (e.g. Gros Bourg).", "When disabled, parent types that define hamlets are excluded from natural spawn.", "iso-legacy: disabled by default (hamlets take a lot of space)"}).translation("millenaire.config.generateHamlets").define("generateHamlets", false);
        this.minVillageDistance = builder.comment("Minimum distance between two villages (blocks)").translation("millenaire.config.minVillageDistance").defineInRange("minVillageDistance", 500, 300, 1000);
        this.minLoneBuildingDistance = builder.comment("Minimum distance between two lone buildings (blocks)").translation("millenaire.config.minLoneBuildingDistance").defineInRange("minLoneBuildingDistance", 500, 300, 1000);
        this.minVillageLoneBuildingDistance = builder.comment("Minimum distance between a village and a lone building (blocks)").translation("millenaire.config.minVillageLoneBuildingDistance").defineInRange("minVillageLoneBuildingDistance", 250, 100, 800);
        this.spawnProtectionRadius = builder.comment("Protected area around world spawn where nothing generates (blocks)").translation("millenaire.config.spawnProtectionRadius").defineInRange("spawnProtectionRadius", 250, 0, 500);
        builder.comment("Progressive completion for distant villages").push("completion");
        this.completionMaxPercentage = builder.comment("Maximum initial progress % for distant villages").translation("millenaire.config.completionMaxPercentage").defineInRange("maxPercentage", 25, 0, 100);
        this.completionMinDistance = builder.comment("Distance from spawn where initial progress starts (blocks)").translation("millenaire.config.completionMinDistance").defineInRange("minDistance", 2000, 0, 25000);
        this.completionMaxDistance = builder.comment("Distance from spawn where max initial progress is reached (blocks)").translation("millenaire.config.completionMaxDistance").defineInRange("maxDistance", 10000, 0, 100000);
        builder.pop();
        builder.pop();
        builder.comment("Debug and logging settings").push("debug");
        this.logSpawnAttempts = builder.comment("Log every village/lone building spawn attempt with rejection reasons").translation("millenaire.config.logSpawnAttempts").define("logSpawnAttempts", false);
        builder.pop();
        builder.comment("Village behaviour settings").push("village");
        this.keepActiveRadius = builder.comment("Radius for keeping village chunks loaded (blocks). 0 = disabled.").translation("millenaire.config.keepActiveRadius").defineInRange("keepActiveRadius", 200, 0, 2000);
        this.villageRadiusOverride = builder.comment(new String[]{"Override all village type radii with this value (blocks).", "-1 = use per-type JSON value. Requires world restart to take effect."}).translation("millenaire.config.villageRadiusOverride").worldRestart().defineInRange("villageRadiusOverride", -1, -1, 120);
        this.minBuildingSpacing = builder.comment("Minimum spacing between buildings in a village (blocks)").translation("millenaire.config.minBuildingSpacing").worldRestart().defineInRange("minBuildingSpacing", 5, 0, 10);
        this.buildPaths = builder.comment("Generate and upgrade paths between village buildings (lateral paths Pass 3)").translation("millenaire.config.buildPaths").define("buildPaths", true);
        this.maxChildren = builder.comment("Maximum number of children per village").translation("millenaire.config.maxChildren").defineInRange("maxChildren", 10, 2, 20);
        this.backgroundRadius = builder.comment("Radius for inter-village relations: diplomacy, trade, raids (blocks). 0 = disabled.").translation("millenaire.config.backgroundRadius").defineInRange("backgroundRadius", 2000, 0, 3000);
        this.navDriver = builder.comment(new String[]{"Villager navigation driver (EXPERIMENTAL toggle for A/B testing).", "WAYPOINT (default): the current driver \u2014 vanilla micro A* + a macro", "  waypoint graph with a DIRECT->REPATH->MACRO->TELEPORT escalation.", "LOCAL_RECOVERY: experimental port of the Millenaire 1.15 movement", "  controller \u2014 vanilla A* with soft re-path, a perpendicular door/gate", "  nudge to clear chokepoints, and teleport only as a last resort.", "Applies to villagers spawned/loaded after the change. Default: WAYPOINT."}).translation("millenaire.config.navDriver").defineEnum("navDriver", (Enum)NavDriverType.WAYPOINT);
        builder.pop();
        builder.comment("Gameplay settings").push("gameplay");
        this.languageLearning = builder.comment("Whether NPC languages need to be learned through interaction").translation("millenaire.config.languageLearning").define("languageLearning", true);
        this.travelBookLearning = builder.comment("Whether Travel Book content needs to be discovered through interaction (nearby villagers, buildings, trade)").translation("millenaire.config.travelBookLearning").define("travelBookLearning", true);
        this.sentenceDistanceSingleplayer = builder.comment("Distance for villager sentences in chat \u2014 singleplayer (blocks). 0 = disabled.").translation("millenaire.config.sentenceDistanceSP").defineInRange("sentenceDistanceSingleplayer", 6, 0, 10);
        this.sentenceDistanceMultiplayer = builder.comment("Distance for villager sentences in chat \u2014 multiplayer (blocks). 0 = disabled.").translation("millenaire.config.sentenceDistanceMP").defineInRange("sentenceDistanceMultiplayer", 0, 0, 10);
        this.ignoreResourceCost = builder.comment(new String[]{"Villages build and upgrade for free, ignoring resource requirements (creative-style).", "iso-legacy: MillConfigValues.ignoreResourceCost. Default: false."}).translation("millenaire.config.ignoreResourceCost").define("ignoreResourceCost", false);
        builder.pop();
        builder.comment("Anonymous usage statistics sent to millenaire.org").push("statistics");
        this.sendStatistics = builder.comment(new String[]{"Send Mill\u00e9naire statistics to millenaire.org. Master switch: when off, nothing is sent.", "The session ping carries no personally identifiable information (anonymous world UID,", "version, OS, locale, player count). Advancement progress is reported per player using a", "pseudonymous (non re-identifiable) identifier, and only attaches the username (login)", "when sendPlayerName is enabled. Note: the donor check sends the username out of", "functional necessity, independently of these two flags."}).translation("millenaire.config.sendStatistics").define("sendStatistics", true);
        this.sendPlayerName = builder.comment(new String[]{"Attach the player's Minecraft username to the per-player advancement report.", "When off, that report uses only the pseudonymous identifier. Off by default."}).translation("millenaire.config.sendPlayerName").define("sendPlayerName", false);
        this.apiBaseUrlOverride = builder.comment(new String[]{"Override the millenaire.org API base URL the mod contacts (stats, version, donor).", "Empty = use the URLs baked in at build time (primary, then fallback). A non-empty", "value forces a single URL with no fallback, e.g. http://localhost:4000 for local testing."}).translation("millenaire.config.apiBaseUrlOverride").define("apiBaseUrlOverride", (Object)"");
        builder.pop();
        builder.comment("Raid settings (combat AI not yet implemented \u2014 values reserved)").push("raids");
        this.banditRaidRadius = builder.comment("TODO: Radius for bandit raids (blocks). 0 = disabled.").translation("millenaire.config.banditRaidRadius").defineInRange("banditRaidRadius", 1500, 0, 2000);
        this.raidingRate = builder.comment("TODO: % chance per night of a raid attempt. 0 = disabled.").translation("millenaire.config.raidingRate").defineInRange("raidingRate", 20, 0, 100);
        builder.pop();
        builder.comment("Automatic conversion of legacy 1.12 content packs dropped into millenaire-custom/").push("legacy");
        this.legacyAutoConvert = builder.comment(new String[]{"Enable automatic conversion of legacy 1.12 Millenaire content packs", "dropped into millenaire-custom/. When true, TXT/PNG files are detected", "at server start, converted to JSON/NBT in place, and originals renamed", "to .legacy. Set to false if you prefer running /millenaire dev convert-addon", "manually (required for read-only filesystems, large packs, or custom", "staging workflows)."}).translation("millenaire.config.legacyAutoConvert").define("legacyAutoConvert", true);
        this.legacyAutoConvertMaxPngs = builder.comment(new String[]{"Per-boot cap on PNG file count before auto-conversion refuses to run.", "Large packs should use /millenaire dev convert-addon during a maintenance", "window to avoid multi-minute boot freezes \u2014 cold-JVM PngToNbtConverter", "costs ~30-60s per legacy culture."}).translation("millenaire.config.legacyAutoConvertMaxPngs").defineInRange("legacyAutoConvertMaxPngs", 300, 0, 5000);
        builder.pop();
    }

    static {
        Pair pair = new ModConfigSpec.Builder().configure(MillenaireServerConfig::new);
        SERVER = (MillenaireServerConfig)pair.getLeft();
        SPEC = (ModConfigSpec)pair.getRight();
    }
}

