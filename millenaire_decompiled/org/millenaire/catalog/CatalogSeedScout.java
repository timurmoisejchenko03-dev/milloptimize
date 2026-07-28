/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  com.google.gson.GsonBuilder
 *  com.mojang.logging.LogUtils
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerLevel
 *  org.slf4j.Logger
 */
package org.millenaire.catalog;

import com.google.gson.GsonBuilder;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import org.millenaire.catalog.CatalogBiomeLocator;
import org.millenaire.culture.ModCultures;
import org.slf4j.Logger;

public final class CatalogSeedScout {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int SITE_RADIUS = 48;
    private static final BlockPos SEARCH_ORIGIN = new BlockPos(0, 96, 0);
    private static final double NOT_FOUND_PENALTY = 1000000.0;

    private CatalogSeedScout() {
    }

    public static void run(ServerLevel level, long seed, Path reportDir) throws IOException {
        ArrayList<CultureSite> sites = new ArrayList<CultureSite>();
        for (ResourceLocation culture : ModCultures.getAllCultures().keySet()) {
            BlockPos biome = CatalogBiomeLocator.findCultureAnchor(level, culture, SEARCH_ORIGIN);
            double biomeDist = CatalogSeedScout.horizontalDistance(biome);
            BlockPos site = CatalogBiomeLocator.findValidatedSite(level, biome, 48, CatalogBiomeLocator.biomeMatcherFor(culture));
            boolean found = site != null;
            BlockPos at = found ? site : biome;
            sites.add(new CultureSite(culture.toString(), found, at.getX(), at.getZ(), biomeDist, found ? CatalogSeedScout.horizontalDistance(site) : -1.0));
            LOGGER.info("Scout seed {} culture {}: {} at {},{}", new Object[]{seed, culture, found ? "VALID" : "no valid site", at.getX(), at.getZ()});
        }
        int foundCount = (int)sites.stream().filter(CultureSite::found).count();
        double score = 0.0;
        for (CultureSite s : sites) {
            score += s.found() ? s.siteDistance() : 1000000.0;
        }
        Files.createDirectories(reportDir, new FileAttribute[0]);
        CatalogSeedScout.writeReport(reportDir, seed, sites, foundCount, score);
        CatalogSeedScout.appendSummary(reportDir, seed, foundCount, sites.size(), score);
    }

    private static double horizontalDistance(@Nullable BlockPos pos) {
        if (pos == null) {
            return -1.0;
        }
        return Math.sqrt((double)pos.getX() * (double)pos.getX() + (double)pos.getZ() * (double)pos.getZ());
    }

    private static void writeReport(Path reportDir, long seed, List<CultureSite> sites, int foundCount, double score) throws IOException {
        LinkedHashMap<String, Object> root = new LinkedHashMap<String, Object>();
        root.put("seed", seed);
        root.put("foundCount", foundCount);
        root.put("totalCultures", sites.size());
        root.put("score", score);
        root.put("sites", sites);
        Path out = reportDir.resolve("seed-" + seed + ".json");
        Files.writeString(out, (CharSequence)new GsonBuilder().setPrettyPrinting().create().toJson(root), new OpenOption[0]);
    }

    private static void appendSummary(Path reportDir, long seed, int foundCount, int total, double score) throws IOException {
        String line = seed + "\t" + foundCount + "\t" + total + "\t" + Math.round(score) + "\n";
        Files.writeString(reportDir.resolve("summary.tsv"), (CharSequence)line, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    private record CultureSite(String culture, boolean found, int x, int z, double biomeDistance, double siteDistance) {
    }
}

