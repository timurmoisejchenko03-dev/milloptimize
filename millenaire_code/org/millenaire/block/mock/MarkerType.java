/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.util.StringRepresentable
 */
package org.millenaire.block.mock;

import net.minecraft.util.StringRepresentable;

public enum MarkerType implements StringRepresentable
{
    SLEEPING_POS("sleeping_pos", "sleepingPos"),
    SELLING_POS("selling_pos", "sellingPos"),
    CRAFTING_POS("crafting_pos", "craftingPos"),
    DEFENDING_POS("defending_pos", "defendingPos"),
    SHELTER_POS("shelter_pos", "shelterPos"),
    PATH_START_POS("path_start_pos", "pathStartPos"),
    LEISURE_POS("leisure_pos", "leisurePos"),
    STALL("stall", "stall"),
    FISHING_SPOT("fishing_spot", "fishingSpot"),
    PRESERVE_GROUND("preserve_ground", "preserve_ground"),
    PRESERVE_GROUND_DEPTH("preserve_ground_depth", "preserve_ground"),
    PRESERVE_GROUND_ALLBUTTREES("preserve_ground_allbuttrees", "preserve_ground"),
    PRESERVE_GROUND_GRASS("preserve_ground_grass", "preserve_ground"),
    TORCH("torch", "torchGuess"),
    HEALING_SPOT("healing_spot", "healingSpot"),
    BRICK_SPOT("brick_spot", "brick_spot"),
    SILKWORM_BLOCK("silkworm_block", "silkwormBlock"),
    SNAIL_SOIL_BLOCK("snail_soil_block", "snailSoilBlock"),
    CACAO_SPOT("cacao_spot", "cacaoSpot"),
    FIREPLACE("fireplace", "fireplace");

    private final String serializedName;
    private final String specialPointType;

    private MarkerType(String serializedName, String specialPointType) {
        this.serializedName = serializedName;
        this.specialPointType = specialPointType;
    }

    public String getSerializedName() {
        return this.serializedName;
    }

    public String specialPointType() {
        return this.specialPointType;
    }

    public String specialPointSubtype() {
        return switch (this.ordinal()) {
            case 9 -> "surface";
            case 10 -> "depth";
            case 11 -> "allbuttrees";
            case 12 -> "grass";
            default -> null;
        };
    }

    public boolean hasSolidCollision() {
        return switch (this.ordinal()) {
            case 9, 10, 11, 12, 15, 16, 17 -> true;
            default -> false;
        };
    }
}

