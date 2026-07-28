/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  net.minecraft.util.StringRepresentable
 */
package org.millenaire.block.mock;

import net.minecraft.util.StringRepresentable;

public final class MarkerType
extends Enum<MarkerType>
implements StringRepresentable {
    public static final /* enum */ MarkerType SLEEPING_POS = new MarkerType("sleeping_pos", "sleepingPos");
    public static final /* enum */ MarkerType SELLING_POS = new MarkerType("selling_pos", "sellingPos");
    public static final /* enum */ MarkerType CRAFTING_POS = new MarkerType("crafting_pos", "craftingPos");
    public static final /* enum */ MarkerType DEFENDING_POS = new MarkerType("defending_pos", "defendingPos");
    public static final /* enum */ MarkerType SHELTER_POS = new MarkerType("shelter_pos", "shelterPos");
    public static final /* enum */ MarkerType PATH_START_POS = new MarkerType("path_start_pos", "pathStartPos");
    public static final /* enum */ MarkerType LEISURE_POS = new MarkerType("leisure_pos", "leisurePos");
    public static final /* enum */ MarkerType STALL = new MarkerType("stall", "stall");
    public static final /* enum */ MarkerType FISHING_SPOT = new MarkerType("fishing_spot", "fishingSpot");
    public static final /* enum */ MarkerType PRESERVE_GROUND = new MarkerType("preserve_ground", "preserve_ground");
    public static final /* enum */ MarkerType PRESERVE_GROUND_DEPTH = new MarkerType("preserve_ground_depth", "preserve_ground");
    public static final /* enum */ MarkerType PRESERVE_GROUND_ALLBUTTREES = new MarkerType("preserve_ground_allbuttrees", "preserve_ground");
    public static final /* enum */ MarkerType PRESERVE_GROUND_GRASS = new MarkerType("preserve_ground_grass", "preserve_ground");
    public static final /* enum */ MarkerType TORCH = new MarkerType("torch", "torchGuess");
    public static final /* enum */ MarkerType HEALING_SPOT = new MarkerType("healing_spot", "healingSpot");
    public static final /* enum */ MarkerType BRICK_SPOT = new MarkerType("brick_spot", "brick_spot");
    public static final /* enum */ MarkerType SILKWORM_BLOCK = new MarkerType("silkworm_block", "silkwormBlock");
    public static final /* enum */ MarkerType SNAIL_SOIL_BLOCK = new MarkerType("snail_soil_block", "snailSoilBlock");
    public static final /* enum */ MarkerType CACAO_SPOT = new MarkerType("cacao_spot", "cacaoSpot");
    public static final /* enum */ MarkerType FIREPLACE = new MarkerType("fireplace", "fireplace");
    private final String serializedName;
    private final String specialPointType;
    private static final /* synthetic */ MarkerType[] $VALUES;

    public static MarkerType[] values() {
        return (MarkerType[])$VALUES.clone();
    }

    public static MarkerType valueOf(String name) {
        return Enum.valueOf(MarkerType.class, name);
    }

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

    private static /* synthetic */ MarkerType[] $values() {
        return new MarkerType[]{SLEEPING_POS, SELLING_POS, CRAFTING_POS, DEFENDING_POS, SHELTER_POS, PATH_START_POS, LEISURE_POS, STALL, FISHING_SPOT, PRESERVE_GROUND, PRESERVE_GROUND_DEPTH, PRESERVE_GROUND_ALLBUTTREES, PRESERVE_GROUND_GRASS, TORCH, HEALING_SPOT, BRICK_SPOT, SILKWORM_BLOCK, SNAIL_SOIL_BLOCK, CACAO_SPOT, FIREPLACE};
    }

    static {
        $VALUES = MarkerType.$values();
    }
}

