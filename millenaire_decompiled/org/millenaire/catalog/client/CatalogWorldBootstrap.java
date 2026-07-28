/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.screens.Screen
 *  net.minecraft.client.gui.screens.TitleScreen
 *  net.minecraft.core.Holder
 *  net.minecraft.core.HolderLookup$RegistryLookup
 *  net.minecraft.core.RegistryAccess
 *  net.minecraft.core.registries.Registries
 *  net.minecraft.world.Difficulty
 *  net.minecraft.world.level.GameRules
 *  net.minecraft.world.level.GameType
 *  net.minecraft.world.level.LevelSettings
 *  net.minecraft.world.level.WorldDataConfiguration
 *  net.minecraft.world.level.biome.Biomes
 *  net.minecraft.world.level.chunk.ChunkGenerator
 *  net.minecraft.world.level.levelgen.FlatLevelSource
 *  net.minecraft.world.level.levelgen.WorldDimensions
 *  net.minecraft.world.level.levelgen.WorldOptions
 *  net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings
 *  net.minecraft.world.level.levelgen.presets.WorldPreset
 *  net.minecraft.world.level.levelgen.presets.WorldPresets
 *  org.slf4j.Logger
 */
package org.millenaire.catalog.client;

import com.mojang.logging.LogUtils;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import org.millenaire.Millenaire;
import org.millenaire.catalog.CatalogJob;
import org.millenaire.catalog.client.CatalogClientEvents;
import org.slf4j.Logger;

public final class CatalogWorldBootstrap {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String WORLD_NAME = "catalog";
    private static boolean requested = false;

    private CatalogWorldBootstrap() {
    }

    public static void ensureWorld(Minecraft mc) {
        boolean exists;
        if (requested) {
            return;
        }
        requested = true;
        Millenaire.setNaturalSpawnSuppressed(true);
        try {
            exists = mc.getLevelSource().levelExists(WORLD_NAME);
        }
        catch (Exception e) {
            LOGGER.error("Could not check catalog world existence: {}", (Object)e.toString());
            return;
        }
        if (exists) {
            LOGGER.info("Loading existing catalog world");
            mc.createWorldOpenFlows().openWorld(WORLD_NAME, () -> {
                requested = false;
            });
        } else {
            LOGGER.info("Creating catalog world");
            LevelSettings settings = new LevelSettings(WORLD_NAME, GameType.CREATIVE, false, Difficulty.PEACEFUL, true, new GameRules(), WorldDataConfiguration.DEFAULT);
            WorldOptions options = new WorldOptions(CatalogWorldBootstrap.jobSeed(), false, false);
            mc.createWorldOpenFlows().createFreshLevel(WORLD_NAME, settings, options, CatalogWorldBootstrap::voidDimensions, (Screen)new TitleScreen());
        }
    }

    private static WorldDimensions voidDimensions(RegistryAccess registryAccess) {
        HolderLookup.RegistryLookup biomes = registryAccess.lookupOrThrow(Registries.BIOME);
        FlatLevelGeneratorSettings flat = new FlatLevelGeneratorSettings(Optional.empty(), (Holder)biomes.getOrThrow(Biomes.THE_VOID), List.of());
        WorldDimensions normal = ((WorldPreset)registryAccess.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.NORMAL).value()).createWorldDimensions();
        return normal.replaceOverworldGenerator(registryAccess, (ChunkGenerator)new FlatLevelSource(flat));
    }

    private static long jobSeed() {
        try {
            return CatalogJob.fromJson(Files.readString(CatalogClientEvents.jobFile())).seed();
        }
        catch (Exception e) {
            LOGGER.warn("Could not read seed from job, using default: {}", (Object)e.toString());
            return 80085L;
        }
    }
}

