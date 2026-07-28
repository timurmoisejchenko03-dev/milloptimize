/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 */
package org.millenaire.building;

import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;

public record SpecialPoint(String type, @Nullable String subtype, @Nullable String orientation, @Nullable String placement, BlockPos pos) {
    public static final String SLEEPING_POS = "sleepingPos";
    public static final String SELLING_POS = "sellingPos";
    public static final String CRAFTING_POS = "craftingPos";
    public static final String DEFENDING_POS = "defendingPos";
    public static final String SHELTER_POS = "shelterPos";
    public static final String PATH_START_POS = "pathStartPos";
    public static final String LEISURE_POS = "leisurePos";
    public static final String SOIL = "soil";
    public static final String CHEST = "chest";
    public static final String FURNACE = "furnace";
    public static final String TREE_SPAWN = "treeSpawn";
    public static final String ANIMAL_SPAWN = "animalSpawn";
    public static final String SOURCE = "source";
    public static final String STALL = "stall";
    public static final String FREE_BLOCK = "freeBlock";
    public static final String FISHING_SPOT = "fishingSpot";
    public static final String PRESERVE_GROUND = "preserve_ground";
    public static final String SIGN_POS = "signPos";
    public static final String BRICK_SPOT = "brick_spot";
    public static final String SILKWORM_BLOCK = "silkwormBlock";
    public static final String SNAIL_SOIL_BLOCK = "snailSoilBlock";
    public static final String CACAO_SPOT = "cacaoSpot";
    public static final String WALL_DECORATION = "wall_decoration";
    public static final String FIREPLACE = "fireplace";
    public static final String HEARTH = "hearth";
    public static final String FIRE_PIT = "fire_pit";
    public static final String BANNER = "banner";
    public static final String BANNER_SUBTYPE_VILLAGE = "village";
    public static final String BANNER_SUBTYPE_CULTURE = "culture";

    public SpecialPoint(String type, @Nullable String subtype, @Nullable String orientation, BlockPos pos) {
        this(type, subtype, orientation, null, pos);
    }

    public boolean isType(String expectedType) {
        return this.type.equals(expectedType);
    }

    public boolean isType(String expectedType, String expectedSubtype) {
        return this.type.equals(expectedType) && expectedSubtype.equals(this.subtype);
    }

    public static Set<String> values() {
        return Set.of(SLEEPING_POS, SELLING_POS, CRAFTING_POS, DEFENDING_POS, SHELTER_POS, PATH_START_POS, LEISURE_POS, SOIL, CHEST, FURNACE, TREE_SPAWN, ANIMAL_SPAWN, SOURCE, STALL, FREE_BLOCK, FISHING_SPOT, PRESERVE_GROUND, SIGN_POS, BRICK_SPOT, SILKWORM_BLOCK, SNAIL_SOIL_BLOCK, CACAO_SPOT, WALL_DECORATION, FIREPLACE, HEARTH, FIRE_PIT, BANNER);
    }
}

