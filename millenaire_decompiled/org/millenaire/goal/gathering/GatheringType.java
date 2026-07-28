/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonObject
 *  javax.annotation.Nullable
 *  net.minecraft.resources.ResourceLocation
 */
package org.millenaire.goal.gathering;

import com.google.gson.JsonObject;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;

public record GatheringType(ResourceLocation id, String handlerId, int priority, int priorityRandom, int scanRadius, int batchRadius, int maxActionsPerTask, int actionCooldown, int stuckTimeout, int arrivalRange, double walkSpeed, @Nullable Map<String, Integer> villageLimit, int maxSimultaneousTotal, int minimumHour, int maximumHour, int reoccurDelay, @Nullable Map<String, Integer> buildingLimit, @Nullable Map<String, Integer> townhallLimit, @Nullable Map<String, String> itemsBalance, int maxSimultaneousInBuilding, @Nullable String destinationBuilding, @Nullable List<String> heldItems, @Nullable List<String> heldItemsDestination, @Nullable String sound, @Nullable List<String> priorityInvPenaltyItems, int priorityInvPenaltyBase, JsonObject handlerParams, @Nullable String sentenceKey, @Nullable String labelKey, @Nullable String tag, boolean leisure) {
}

