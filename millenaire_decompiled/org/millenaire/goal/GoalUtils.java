/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.entity.Entity
 */
package org.millenaire.goal;

import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.millenaire.entity.MillVillager;
import org.millenaire.goal.GoalContext;
import org.millenaire.goal.GoalScheduler;

public final class GoalUtils {
    private GoalUtils() {
    }

    public static int countSimultaneous(GoalContext ctx, ResourceLocation goalId) {
        int count = 0;
        ServerLevel level = ctx.level();
        UUID selfUuid = ctx.villager().getUUID();
        for (UUID uuid : ctx.village().getVillagerUuids()) {
            MillVillager other;
            GoalScheduler scheduler;
            Entity entity;
            if (uuid.equals(selfUuid) || !((entity = level.getEntity(uuid)) instanceof MillVillager) || (scheduler = (other = (MillVillager)entity).getGoalScheduler()) == null || !goalId.equals((Object)scheduler.getCurrentGoalId())) continue;
            ++count;
        }
        return count;
    }
}

