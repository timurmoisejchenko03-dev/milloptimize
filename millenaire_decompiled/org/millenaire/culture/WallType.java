/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.resources.ResourceLocation
 */
package org.millenaire.culture;

import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;

public record WallType(ResourceLocation id, ResourceLocation culture, String key, @Nullable ResourceLocation wallPlanSet, @Nullable ResourceLocation towerPlanSet, ResourceLocation gatewayPlanSet, @Nullable ResourceLocation cornerPlanSet, @Nullable ResourceLocation capRightPlanSet, @Nullable ResourceLocation capLeftPlanSet, @Nullable ResourceLocation capBothPlanSet, @Nullable ResourceLocation slope1LeftPlanSet, @Nullable ResourceLocation slope1RightPlanSet, @Nullable ResourceLocation slope2LeftPlanSet, @Nullable ResourceLocation slope2RightPlanSet, @Nullable ResourceLocation slope3LeftPlanSet, @Nullable ResourceLocation slope3RightPlanSet, boolean wallSpawn, boolean towerSpawn, boolean gatewaySpawn, boolean cornerSpawn, boolean capSpawn, int wallsBetweenTowers, int nbSmoothRuns, int maxYDelta) {
}

