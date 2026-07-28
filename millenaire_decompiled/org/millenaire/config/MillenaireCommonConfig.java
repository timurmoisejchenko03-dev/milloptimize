/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  net.neoforged.neoforge.common.ModConfigSpec
 *  net.neoforged.neoforge.common.ModConfigSpec$Builder
 *  net.neoforged.neoforge.common.ModConfigSpec$IntValue
 *  org.apache.commons.lang3.tuple.Pair
 */
package org.millenaire.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;
import org.millenaire.config.MillLog;

public final class MillenaireCommonConfig {
    public static final MillenaireCommonConfig COMMON;
    public static final ModConfigSpec SPEC;
    public final ModConfigSpec.IntValue logAi;
    public final ModConfigSpec.IntValue logPathfinding;
    public final ModConfigSpec.IntValue logConstruction;
    public final ModConfigSpec.IntValue logGathering;
    public final ModConfigSpec.IntValue logCommerce;
    public final ModConfigSpec.IntValue logVillage;
    public final ModConfigSpec.IntValue logWorldGen;
    public final ModConfigSpec.IntValue logCulture;
    public final ModConfigSpec.IntValue logNetwork;
    public final ModConfigSpec.IntValue logChunkLoading;
    public final ModConfigSpec.IntValue logDiplomacy;
    public final ModConfigSpec.IntValue logOther;

    private MillenaireCommonConfig(ModConfigSpec.Builder builder) {
        builder.comment(new String[]{"Per-category log levels (0=none, 1=basic, 2=detailed, 3=verbose).", "Iso-legacy: individual Log* fields in MillConfigValues.", "Change these at runtime by editing the TOML file \u2014 values reload automatically."}).push("log");
        this.logAi = MillenaireCommonConfig.defineLog(builder, "ai", "General AI, goal scheduling");
        this.logPathfinding = MillenaireCommonConfig.defineLog(builder, "pathfinding", "Navigation, waypoints, A*");
        this.logConstruction = MillenaireCommonConfig.defineLog(builder, "construction", "Building, placement, paths");
        this.logGathering = MillenaireCommonConfig.defineLog(builder, "gathering", "All gathering handlers");
        this.logCommerce = MillenaireCommonConfig.defineLog(builder, "commerce", "Trade, selling");
        this.logVillage = MillenaireCommonConfig.defineLog(builder, "village", "Village lifecycle, children, spawning");
        this.logWorldGen = MillenaireCommonConfig.defineLog(builder, "worldGen", "World generation, site validation");
        this.logCulture = MillenaireCommonConfig.defineLog(builder, "culture", "Culture loading, translations");
        this.logNetwork = MillenaireCommonConfig.defineLog(builder, "network", "Network packets");
        this.logChunkLoading = MillenaireCommonConfig.defineLog(builder, "chunkLoading", "Chunk loading/unloading");
        this.logDiplomacy = MillenaireCommonConfig.defineLog(builder, "diplomacy", "Inter-village relations");
        this.logOther = MillenaireCommonConfig.defineLog(builder, "other", "Miscellaneous");
        builder.pop();
    }

    private static ModConfigSpec.IntValue defineLog(ModConfigSpec.Builder builder, String key, String comment) {
        return builder.comment(comment).translation("millenaire.config.log." + key).defineInRange(key, 0, 0, 3);
    }

    public void bindLogCategories() {
        MillLog.LogCategory.AI.bind(((ModConfigSpec.IntValue)this.logAi)::getAsInt);
        MillLog.LogCategory.PATHFINDING.bind(((ModConfigSpec.IntValue)this.logPathfinding)::getAsInt);
        MillLog.LogCategory.CONSTRUCTION.bind(((ModConfigSpec.IntValue)this.logConstruction)::getAsInt);
        MillLog.LogCategory.GATHERING.bind(((ModConfigSpec.IntValue)this.logGathering)::getAsInt);
        MillLog.LogCategory.COMMERCE.bind(((ModConfigSpec.IntValue)this.logCommerce)::getAsInt);
        MillLog.LogCategory.VILLAGE.bind(((ModConfigSpec.IntValue)this.logVillage)::getAsInt);
        MillLog.LogCategory.WORLD_GEN.bind(((ModConfigSpec.IntValue)this.logWorldGen)::getAsInt);
        MillLog.LogCategory.CULTURE.bind(((ModConfigSpec.IntValue)this.logCulture)::getAsInt);
        MillLog.LogCategory.NETWORK.bind(((ModConfigSpec.IntValue)this.logNetwork)::getAsInt);
        MillLog.LogCategory.CHUNK_LOADING.bind(((ModConfigSpec.IntValue)this.logChunkLoading)::getAsInt);
        MillLog.LogCategory.DIPLOMACY.bind(((ModConfigSpec.IntValue)this.logDiplomacy)::getAsInt);
        MillLog.LogCategory.OTHER.bind(((ModConfigSpec.IntValue)this.logOther)::getAsInt);
    }

    static {
        Pair pair = new ModConfigSpec.Builder().configure(MillenaireCommonConfig::new);
        COMMON = (MillenaireCommonConfig)pair.getLeft();
        SPEC = (ModConfigSpec)pair.getRight();
    }
}

