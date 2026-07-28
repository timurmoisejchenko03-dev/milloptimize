/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.resources.ResourceLocation
 */
package org.millenaire.goal.visit;

import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;

public interface VisitGoalSchema {
    public ResourceLocation id();

    public String goalKey();

    public record Play(ResourceLocation id, String goalKey, boolean withFriends) {
    }

    public record ObserveVillager(ResourceLocation id, String goalKey, @Nullable String targetGoalTag, @Nullable Set<ResourceLocation> targetGoalIds, int basePriority, int priorityRandom, int durationTicks, int reoccurDelayTicks, int minimumHour, int maximumHour, @Nullable String buildingTag, @Nullable String requiredTag, @Nullable List<String> heldItems) {
    }

    public record VisitBuilding(ResourceLocation id, String goalKey, String buildingTag, @Nullable String requiredTag, int basePriority, int priorityRandom, int durationTicks, int reoccurDelayTicks, boolean allowRandomMoves, boolean leisure, @Nullable String targetPosType, int minimumHour, int maximumHour, int maxSimultaneousInBuilding, @Nullable List<String> heldItems, @Nullable List<String> heldItemsDestination) {
    }
}

