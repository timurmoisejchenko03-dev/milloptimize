/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.Direction$Axis
 *  net.minecraft.util.StringRepresentable
 */
package org.millenaire.block;

import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;

public final class FirePitAlignment
extends Enum<FirePitAlignment>
implements StringRepresentable {
    public static final /* enum */ FirePitAlignment X = new FirePitAlignment("x", 90.0);
    public static final /* enum */ FirePitAlignment Z = new FirePitAlignment("z", 0.0);
    private final String name;
    public final double angle;
    private static final /* synthetic */ FirePitAlignment[] $VALUES;

    public static FirePitAlignment[] values() {
        return (FirePitAlignment[])$VALUES.clone();
    }

    public static FirePitAlignment valueOf(String name) {
        return Enum.valueOf(FirePitAlignment.class, name);
    }

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

    private static /* synthetic */ FirePitAlignment[] $values() {
        return new FirePitAlignment[]{X, Z};
    }

    static {
        $VALUES = FirePitAlignment.$values();
    }
}

