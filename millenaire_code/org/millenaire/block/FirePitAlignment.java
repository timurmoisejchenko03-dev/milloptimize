/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.Direction$Axis
 *  net.minecraft.util.StringRepresentable
 */
package org.millenaire.block;

import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;

public enum FirePitAlignment implements StringRepresentable
{
    X("x", 90.0),
    Z("z", 0.0);

    private final String name;
    public final double angle;

    private FirePitAlignment(String name, double angle) {
        this.name = name;
        this.angle = angle;
    }

    public String getSerializedName() {
        return this.name;
    }

    public static FirePitAlignment fromAxis(Direction.Axis axis) {
        if (axis == Direction.Axis.X) {
            return Z;
        }
        return X;
    }
}

