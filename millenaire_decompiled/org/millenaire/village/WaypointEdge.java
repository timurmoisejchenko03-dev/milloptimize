/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 */
package org.millenaire.village;

import java.util.List;
import net.minecraft.core.BlockPos;
import org.millenaire.village.Waypoint;

public record WaypointEdge(Waypoint target, double cost, List<BlockPos> pathNodes) {
}

