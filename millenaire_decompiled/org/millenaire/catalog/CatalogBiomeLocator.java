/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.util.Pair
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Holder
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.tags.TagKey
 *  net.minecraft.world.level.biome.Biome
 *  net.minecraft.world.level.levelgen.Heightmap$Types
 */
package org.millenaire.catalog;

import com.mojang.datafixers.util.Pair;
import java.util.HashSet;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import org.millenaire.culture.ModCultures;
import org.millenaire.culture.VillageType;
import org.millenaire.world.SiteValidator;

public final class CatalogBiomeLocator {
    private static final int SEARCH_RADIUS = 6400;
    private static final int HORIZONTAL_STEP = 32;
    private static final int VERTICAL_STEP = 64;
    private static final int DRY_LAND_RADIUS = 128;
    private static final int DRY_LAND_STEP = 8;
    private static final int SPOT_RADIUS = 96;
    private static final int SPOT_STEP = 16;
    private static final int SITE_MARGIN = 12;

    private CatalogBiomeLocator() {
    }

    public static BlockPos findCultureAnchor(ServerLevel level, ResourceLocation cultureId, BlockPos searchOrigin) {
        Predicate<Holder<Biome>> matches = CatalogBiomeLocator.biomeMatcherFor(cultureId);
        if (matches == null) {
            return searchOrigin;
        }
        Pair found = level.findClosestBiome3d(matches, searchOrigin, 6400, 32, 64);
        return found == null ? searchOrigin : (BlockPos)found.getFirst();
    }

    @Nullable
    public static Predicate<Holder<Biome>> biomeMatcherFor(ResourceLocation cultureId) {
        HashSet<TagKey<Biome>> tags = new HashSet<TagKey<Biome>>();
        for (VillageType vt : ModCultures.getAllVillageTypes().values()) {
            if (!cultureId.equals((Object)vt.culture()) || !vt.spawnable()) continue;
            tags.addAll(vt.biomeTags());
        }
        if (tags.isEmpty()) {
            return null;
        }
        return holder -> {
            for (TagKey tag : tags) {
                if (!holder.is(tag)) continue;
                return true;
            }
            return false;
        };
    }

    public static BlockPos snapToDryLand(ServerLevel level, int x, int z) {
        int sea = level.getSeaLevel();
        for (int r = 0; r <= 128; r += 8) {
            for (int dx = -r; dx <= r; dx += 8) {
                for (int dz = -r; dz <= r; dz += 8) {
                    int cz;
                    int cx;
                    int top;
                    if (r != 0 && Math.max(Math.abs(dx), Math.abs(dz)) != r || (top = level.getHeight(Heightmap.Types.WORLD_SURFACE, cx = x + dx, cz = z + dz)) - 1 < sea || !level.getBlockState(new BlockPos(cx, top - 1, cz)).getFluidState().isEmpty()) continue;
                    return new BlockPos(cx, top, cz);
                }
            }
        }
        return new BlockPos(x, level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z), z);
    }

    @Nullable
    public static BlockPos findValidatedSite(ServerLevel level, BlockPos center, int siteRadius, @Nullable Predicate<Holder<Biome>> biomeMatch) {
        for (int r = 0; r <= 96; r += 16) {
            for (int dx = -r; dx <= r; dx += 16) {
                for (int dz = -r; dz <= r; dz += 16) {
                    if (r != 0 && Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
                    int cx = center.getX() + dx;
                    int cz = center.getZ() + dz;
                    int cy = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, cx, cz);
                    BlockPos siteCenter = new BlockPos(cx, cy, cz);
                    if (biomeMatch != null && !CatalogBiomeLocator.biomeInterior(level, siteCenter, siteRadius, biomeMatch) || !SiteValidator.validate(level, siteCenter, siteRadius)) continue;
                    return siteCenter;
                }
            }
        }
        return null;
    }

    private static boolean biomeInterior(ServerLevel level, BlockPos center, int radius, Predicate<Holder<Biome>> match) {
        int[][] offsets;
        for (int[] o : offsets = new int[][]{{0, 0}, {-radius, -radius}, {radius, -radius}, {-radius, radius}, {radius, radius}}) {
            BlockPos p = new BlockPos(center.getX() + o[0], center.getY(), center.getZ() + o[1]);
            if (match.test((Holder<Biome>)level.getBiome(p))) continue;
            return false;
        }
        return true;
    }

    @Nullable
    public static BlockPos findBuildingSpot(ServerLevel level, BlockPos center, int footW, int footD, @Nullable ResourceLocation cultureId) {
        Predicate<Holder<Biome>> match;
        int siteRadius = Math.max(footW, footD) / 2 + 12;
        BlockPos site = CatalogBiomeLocator.findValidatedSite(level, center, siteRadius, match = cultureId == null ? null : CatalogBiomeLocator.biomeMatcherFor(cultureId));
        if (site == null) {
            return null;
        }
        return new BlockPos(site.getX() - footW / 2, site.getY(), site.getZ() - footD / 2);
    }
}

