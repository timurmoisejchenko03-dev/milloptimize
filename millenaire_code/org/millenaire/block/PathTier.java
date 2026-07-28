/*
 * Decompiled with CFR 0.152.
 */
package org.millenaire.block;

public enum PathTier {
    RUSTIC(1.1f, 0.4f),
    PAVED(1.15f, 0.2f),
    STONE(1.2f, 0.0f);

    private final float speedFactor;
    private final float preferenceMalus;

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
}

