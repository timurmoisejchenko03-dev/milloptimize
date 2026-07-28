/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.tags.TagKey
 *  net.minecraft.world.level.biome.Biome
 *  net.minecraft.world.level.block.Rotation
 */
package org.millenaire.culture;

import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Rotation;
import org.millenaire.culture.TerrainQualifiers;
import org.millenaire.village.BrickColourTheme;

public record VillageType(ResourceLocation id, ResourceLocation culture, String name, int weight, List<TagKey<Biome>> biomeTags, List<LayoutSlot> layout, Map<String, Integer> sellingPriceOverrides, Map<String, Integer> buyingPriceOverrides, int maxSimultaneousConstructions, List<String> qualifiers, TerrainQualifiers terrainQualifiers, List<ResourceLocation> playerBuildings, List<BrickColourTheme> brickColourThemes, List<String> neverBuildings, boolean loneBuilding, int minDistanceFromSpawn, int max, boolean keyLoneBuilding, @Nullable String keyLoneBuildingGenerateTag, boolean generatedForPlayer, boolean spawnable, boolean showTownHallSigns, @Nullable String nameList, int radius, float minimumBiomeValidity, List<String> pathMaterials, boolean travelBookDisplay, List<ResourceLocation> hamlets, @Nullable String specialType, boolean allowExtraBuildings, @Nullable String icon, boolean playerControlled, @Nullable ResourceLocation outerWallType, @Nullable ResourceLocation innerWallType, int innerWallRadius, int maxSimultaneousWallConstructions, List<String> bannerJsons, boolean carriesRaid) {
    @Nullable
    public String forestQualifier() {
        return this.terrainQualifiers.forest();
    }

    @Nullable
    public String hillQualifier() {
        return this.terrainQualifiers.hill();
    }

    @Nullable
    public String mountainQualifier() {
        return this.terrainQualifiers.mountain();
    }

    @Nullable
    public String desertQualifier() {
        return this.terrainQualifiers.desert();
    }

    @Nullable
    public String lavaQualifier() {
        return this.terrainQualifiers.lava();
    }

    @Nullable
    public String lakeQualifier() {
        return this.terrainQualifiers.lake();
    }

    @Nullable
    public String oceanQualifier() {
        return this.terrainQualifiers.ocean();
    }

    public VillageType withLayout(List<LayoutSlot> newLayout) {
        return new VillageType(this.id, this.culture, this.name, this.weight, this.biomeTags, newLayout, this.sellingPriceOverrides, this.buyingPriceOverrides, this.maxSimultaneousConstructions, this.qualifiers, this.terrainQualifiers, this.playerBuildings, this.brickColourThemes, this.neverBuildings, this.loneBuilding, this.minDistanceFromSpawn, this.max, this.keyLoneBuilding, this.keyLoneBuildingGenerateTag, this.generatedForPlayer, this.spawnable, this.showTownHallSigns, this.nameList, this.radius, this.minimumBiomeValidity, this.pathMaterials, this.travelBookDisplay, this.hamlets, this.specialType, this.allowExtraBuildings, this.icon, this.playerControlled, this.outerWallType, this.innerWallType, this.innerWallRadius, this.maxSimultaneousWallConstructions, this.bannerJsons, this.carriesRaid);
    }

    public boolean isMarvel() {
        return "marvel".equals(this.specialType);
    }

    public boolean isRegularVillage() {
        return this.specialType == null && !this.loneBuilding;
    }

    public boolean isHamlet() {
        return "hameau".equals(this.specialType);
    }

    public boolean isPlayerControlled() {
        return this.playerControlled;
    }

    public record LayoutSlot(ResourceLocation plan, @Nullable BlockPos offset, @Nullable Rotation rotation, String role, double minDistance, double maxDistance, int priority, Map<String, Integer> farFromTags, Map<String, Integer> closeToTags, int clearMargin, @Nullable Integer fixedOrientation) {
        public boolean hasLegacyOffset() {
            return this.offset != null;
        }

        public boolean hasMinDistanceOverride() {
            return this.minDistance >= 0.0;
        }

        public boolean hasMaxDistanceOverride() {
            return this.maxDistance >= 0.0;
        }

        public boolean hasPriorityOverride() {
            return this.priority >= 0;
        }
    }
}

