/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  net.neoforged.neoforge.common.ModConfigSpec
 *  net.neoforged.neoforge.common.ModConfigSpec$BooleanValue
 *  net.neoforged.neoforge.common.ModConfigSpec$Builder
 *  net.neoforged.neoforge.common.ModConfigSpec$IntValue
 *  org.apache.commons.lang3.tuple.Pair
 */
package org.millenaire.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public final class MillenaireClientConfig {
    public static final MillenaireClientConfig CLIENT;
    public static final ModConfigSpec SPEC;
    public final ModConfigSpec.BooleanValue showStartMessage;
    public final ModConfigSpec.BooleanValue showNames;
    public final ModConfigSpec.IntValue namesDistance;

    private MillenaireClientConfig(ModConfigSpec.Builder builder) {
        builder.comment("Display settings").push("display");
        this.showStartMessage = builder.comment("Display Millenaire version at startup (not yet wired)").translation("millenaire.config.showStartMessage").define("showStartMessage", true);
        this.showNames = builder.comment("Display names and occupations above villagers").translation("millenaire.config.showNames").define("showNames", true);
        this.namesDistance = builder.comment("Distance from which villager names are visible (blocks)").translation("millenaire.config.namesDistance").defineInRange("namesDistance", 12, 5, 50);
        builder.pop();
    }

    static {
        Pair pair = new ModConfigSpec.Builder().configure(MillenaireClientConfig::new);
        CLIENT = (MillenaireClientConfig)pair.getLeft();
        SPEC = (ModConfigSpec)pair.getRight();
    }
}

