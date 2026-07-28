/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.network.chat.Component
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.item.ItemStack
 */
package org.millenaire.goal;

import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.millenaire.goal.GoalContext;
import org.millenaire.goal.StopReason;
import org.millenaire.goal.TravelPhase;

public interface VillagerTask {
    public ResourceLocation goalId();

    public void tick(GoalContext var1);

    public boolean isFinished();

    public void stop(GoalContext var1, StopReason var2);

    default public List<ItemStack> getHeldItems(TravelPhase phase) {
        return List.of();
    }

    default public List<ItemStack> getOffHandItems(TravelPhase phase) {
        return List.of();
    }

    default public TravelPhase getTravelPhase() {
        return TravelPhase.AT_DESTINATION;
    }

    @Nullable
    default public Component getGoalLabel() {
        ResourceLocation id = this.goalId();
        if (id == null) {
            return null;
        }
        return Component.translatable((String)("goal.millenaire." + id.getPath()));
    }

    default public void reportProgress() {
    }

    default public boolean consumeProgress() {
        return false;
    }

    default public Map<String, String> getNavDebugInfo() {
        return Map.of();
    }
}

