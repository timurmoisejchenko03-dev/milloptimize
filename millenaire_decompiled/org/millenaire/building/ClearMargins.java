/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  net.minecraft.world.level.block.Rotation
 */
package org.millenaire.building;

import net.minecraft.world.level.block.Rotation;

public record ClearMargins(int lengthBefore, int lengthAfter, int widthBefore, int widthAfter) {
    public static final int DEFAULT = 5;

    public static ClearMargins symmetric(int margin) {
        return new ClearMargins(margin, margin, margin, margin);
    }

    public static ClearMargins defaults() {
        return ClearMargins.symmetric(5);
    }

    public static ClearMargins fromLegacy(int areaToClear, int lengthBefore, int lengthAfter, int widthBefore, int widthAfter) {
        int base = areaToClear > 0 ? areaToClear : 5;
        return new ClearMargins(lengthBefore >= 0 ? lengthBefore : base, lengthAfter >= 0 ? lengthAfter : base, widthBefore >= 0 ? widthBefore : base, widthAfter >= 0 ? widthAfter : base);
    }

    public int maxMargin() {
        return Math.max(Math.max(this.lengthBefore, this.lengthAfter), Math.max(this.widthBefore, this.widthAfter));
    }

    public ClearMargins swapAxes() {
        return new ClearMargins(this.widthBefore, this.widthAfter, this.lengthBefore, this.lengthAfter);
    }

    public ClearMargins forRotation(Rotation rotation) {
        return switch (rotation) {
            case Rotation.CLOCKWISE_90 -> new ClearMargins(this.widthAfter, this.widthBefore, this.lengthBefore, this.lengthAfter);
            case Rotation.CLOCKWISE_180 -> new ClearMargins(this.lengthAfter, this.lengthBefore, this.widthAfter, this.widthBefore);
            case Rotation.COUNTERCLOCKWISE_90 -> new ClearMargins(this.widthBefore, this.widthAfter, this.lengthAfter, this.lengthBefore);
            default -> this;
        };
    }

    public ClearMargins atLeast(int minimum) {
        return new ClearMargins(Math.max(this.lengthBefore, minimum), Math.max(this.lengthAfter, minimum), Math.max(this.widthBefore, minimum), Math.max(this.widthAfter, minimum));
    }
}

