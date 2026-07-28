/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.world.item.Item
 */
package org.millenaire.goal.gathering;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.world.item.Item;
import org.millenaire.building.BuildingInstance;
import org.millenaire.goal.GoalContext;
import org.millenaire.goal.gathering.GatheringTarget;
import org.millenaire.goal.gathering.GatheringType;

public interface GatheringHandler {
    public String id();

    public boolean canStart(GoalContext var1, GatheringType var2);

    @Nullable
    public GatheringTarget findTarget(GoalContext var1, GatheringType var2, @Nullable GatheringTarget var3);

    public boolean performAction(GoalContext var1, GatheringType var2, GatheringTarget var3);

    default public boolean isTargetStillValid(GoalContext ctx, GatheringType type, GatheringTarget target) {
        return true;
    }

    default public int actingWatchdogTicks(GatheringType type) {
        return Math.max(600, type.actionCooldown() * 2);
    }

    default public void onStuckTeleport(GoalContext ctx, GatheringType type, GatheringTarget target) {
    }

    default public List<String> validate(GatheringType type) {
        return List.of();
    }

    default public boolean supportsRemoteAction() {
        return false;
    }

    @Nullable
    default public BuildingInstance resolveBuildingLimitTarget(GoalContext ctx, GatheringType type) {
        return null;
    }

    default public int getActionCooldown(GoalContext ctx, GatheringType type) {
        return type.actionCooldown();
    }

    @Nullable
    default public String getHeldToolCategoryId(GatheringType type) {
        return null;
    }

    @Nullable
    default public Item getDefaultHeldItem(GatheringType type) {
        return null;
    }

    default public void onClear() {
    }
}

