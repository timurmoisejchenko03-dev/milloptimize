/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.resources.ResourceLocation
 */
package org.millenaire.goal;

import net.minecraft.resources.ResourceLocation;
import org.millenaire.goal.GoalContext;
import org.millenaire.goal.VillagerTask;

public interface VillagerGoal {
    public ResourceLocation id();

    public int computePriority(GoalContext var1);

    public boolean canStart(GoalContext var1);

    public VillagerTask start(GoalContext var1);

    default public boolean isLeisure() {
        return false;
    }

    default public boolean showInTravelBook() {
        return true;
    }

    default public boolean canBeDoneAtNight() {
        return false;
    }

    default public boolean canBeDoneInDayTime() {
        return true;
    }

    default public long reoccurDelayTicks() {
        return 0L;
    }

    default public boolean isCombatUrgent() {
        return false;
    }
}

