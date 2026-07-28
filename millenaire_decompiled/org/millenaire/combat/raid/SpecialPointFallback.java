/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 */
package org.millenaire.combat.raid;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;

public final class SpecialPointFallback {
    private SpecialPointFallback() {
    }

    @Nullable
    public static BlockPos resolveOrFallback(@Nullable BlockPos specialPointPos, @Nullable BlockPos origin) {
        return specialPointPos != null ? specialPointPos : origin;
    }
}

