/*
 * Decompiled with CFR 0.150.
 */
package org.millenaire.block;

public final class PathTier
extends Enum<PathTier> {
    public static final /* enum */ PathTier RUSTIC = new PathTier(1.1f, 0.4f);
    public static final /* enum */ PathTier PAVED = new PathTier(1.15f, 0.2f);
    public static final /* enum */ PathTier STONE = new PathTier(1.2f, 0.0f);
    private final float speedFactor;
    private final float preferenceMalus;
    private static final /* synthetic */ PathTier[] $VALUES;

    public static PathTier[] values() {
        return (PathTier[])$VALUES.clone();
    }

    public static PathTier valueOf(String name) {
        return Enum.valueOf(PathTier.class, name);
    }

    private PathTier(float speedFactor, float preferenceMalus) {
        this.speedFactor = speedFactor;
        this.preferenceMalus = preferenceMalus;
    }

    public float speedFactor() {
        return this.speedFactor;
    }

    public float preferenceMalus() {
        return this.preferenceMalus;
    }

    private static /* synthetic */ PathTier[] $values() {
        return new PathTier[]{RUSTIC, PAVED, STONE};
    }

    static {
        $VALUES = PathTier.$values();
    }
}

