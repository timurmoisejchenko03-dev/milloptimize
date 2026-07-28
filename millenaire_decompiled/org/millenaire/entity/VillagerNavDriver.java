/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 */
package org.millenaire.entity;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import org.millenaire.entity.MillVillager;
import org.millenaire.village.Village;

public interface VillagerNavDriver {
    public void navigateTo(MillVillager var1, BlockPos var2, double var3);

    public void tick(MillVillager var1, @Nullable Village var2);

    public boolean isArrived(MillVillager var1, double var2);

    public boolean isArrivedHorizontal(MillVillager var1, double var2);

    public boolean isArrivedSameFloor(MillVillager var1, double var2);

    public boolean isAbandoned();

    public void stop(MillVillager var1);

    @Nullable
    public BlockPos getDestination();

    public NavDiagnostics getDiagnostics();

    public record NavDiagnostics(int localStuck, int longDistanceStuck, int teleportCount, @Nullable String waypointState) {
    }
}

