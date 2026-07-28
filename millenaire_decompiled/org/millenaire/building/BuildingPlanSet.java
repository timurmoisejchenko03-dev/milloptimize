/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.item.DyeColor
 */
package org.millenaire.building;

import java.util.List;
import java.util.Map;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import org.millenaire.building.ClearMargins;
import org.millenaire.village.BrickColourTheme;

public record BuildingPlanSet(ResourceLocation id, ResourceLocation culture, String buildingId, String category, String nativeName, int maxCount, double minDistance, double maxDistance, List<String> maleResidents, List<String> femaleResidents, int priorityMoveIn, List<String> tags, String terrainPolicy, String constructionOrder, Map<String, List<LevelDef>> variants, List<String> startingSubBuildings, @Nullable String icon, ClearMargins clearMargins, int price, int reputation, Map<DyeColor, List<BrickColourTheme.WeightedColor>> randomBrickColours, List<StartingGood> startingGoods, @Nullable String travelBookCategory, boolean travelBookDisplay, boolean isSubBuilding, boolean isTownHall, Map<String, Integer> farFromTags, Map<String, Integer> closeToTags, @Nullable Integer fixedOrientation, boolean isWallSegment, boolean isBorderBuilding, int extraWallConstructionSlots, boolean isGift, List<String> visitors) {
    public String pickRandomVariant(Random random) {
        List<String> keys = List.copyOf(this.variants.keySet());
        return keys.get(random.nextInt(keys.size()));
    }

    public int getLevelCount(String variant) {
        List<LevelDef> levels = this.variants.get(variant);
        return levels != null ? levels.size() : 0;
    }

    @Nullable
    public LevelDef getLevel(String variant, int level) {
        List<LevelDef> levels = this.variants.get(variant);
        if (levels == null || level < 0 || level >= levels.size()) {
            return null;
        }
        return levels.get(level);
    }

    @Nullable
    public ResourceLocation getPlanId(String variant, int level) {
        LevelDef def = this.getLevel(variant, level);
        return def != null ? def.planId() : null;
    }

    public boolean hasNextLevel(String variant, int currentLevel) {
        return this.getLevelCount(variant) > currentLevel + 1;
    }

    public boolean isInn() {
        return this.tags.contains("inn");
    }

    public boolean isMarket() {
        return this.tags.contains("market");
    }

    public boolean hasVisitors() {
        return this.isMarket() || !this.visitors.isEmpty();
    }

    public boolean isArchives() {
        return this.tags.contains("archives");
    }

    public boolean isBorderPost() {
        return this.tags.contains("borderpostsign");
    }

    public boolean isHoF() {
        return this.tags.contains("hof");
    }

    public boolean isResidential() {
        return !this.maleResidents.isEmpty() || !this.femaleResidents.isEmpty();
    }

    public boolean hasTag(String tag) {
        return this.tags.contains(tag);
    }

    public record LevelDef(int level, ResourceLocation planId, String nbtPath, int width, int height, int depth, int groundLevel, int priority, @Nullable String nativeName, List<String> requiredTags, List<String> forbiddenTagsInVillage, List<String> requiredVillageTags, List<String> parentTags, List<String> requiredParentTags, List<String> clearTags, List<String> villageTags, Map<ResourceLocation, Integer> requiredResources, List<String> subBuildings, @Nullable int[] signOrder, int pathLevel, boolean rebuildPath, int pathWidth, Map<String, Integer> abstractedProduction, int irrigation) {
    }

    public record StartingGood(String item, double probability, int fixedNumber, int randomNumber) {
    }
}

