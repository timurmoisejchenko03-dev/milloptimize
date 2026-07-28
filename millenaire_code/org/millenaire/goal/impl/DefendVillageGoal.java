/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.item.ItemStack
 */
package org.millenaire.goal.impl;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.millenaire.combat.raid.RaidManager;
import org.millenaire.culture.ModCultures;
import org.millenaire.culture.VillagerType;
import org.millenaire.entity.MillVillager;
import org.millenaire.goal.GoalContext;
import org.millenaire.goal.StopReason;
import org.millenaire.goal.TravelPhase;
import org.millenaire.goal.VillagerGoal;
import org.millenaire.goal.VillagerTask;
import org.millenaire.goal.impl.CombatGoalSupport;

public class DefendVillageGoal
implements VillagerGoal {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)"defend_village");
    static final int PRIORITY = 9999;

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public int computePriority(GoalContext context) {
        return 9999;
    }

    @Override
    public boolean canBeDoneAtNight() {
        return true;
    }

    @Override
    public boolean isCombatUrgent() {
        return true;
    }

    @Override
    public boolean canStart(GoalContext context) {
        MillVillager v = context.villager();
        if (v.isRaiderEntity()) {
            return false;
        }
        VillagerType type = ModCultures.getVillagerType(v.getVillagerTypeId());
        if (type == null || !type.isHelpInAttacks()) {
            return false;
        }
        return context.village() != null && context.village().isUnderAttack();
    }

    @Override
    public VillagerTask start(GoalContext context) {
        return new DefendVillageTask();
    }

    static class DefendVillageTask
    implements VillagerTask {
        private static final double WALK_SPEED = 0.7;
        @Nullable
        private BlockPos defendingPos;
        @Nullable
        private MillVillager ctxVillager;
        private boolean finished;
        private static final double ACQUIRE_RADIUS = 5.0;
        private static final double FALLBACK_ARRIVE_SQ = 9.0;

        DefendVillageTask() {
        }

        @Override
        public ResourceLocation goalId() {
            return ID;
        }

        @Override
        public List<ItemStack> getHeldItems(TravelPhase phase) {
            MillVillager v = this.ctxVillager;
            if (v == null) {
                return List.of();
            }
            return List.of(v.getCombatWeapon());
        }

        @Override
        public void tick(GoalContext ctx) {
            MillVillager v;
            this.ctxVillager = v = ctx.villager();
            if (ctx.village() == null || !ctx.village().isUnderAttack()) {
                this.finished = true;
                v.getNavManager().stop(v);
                return;
            }
            if (this.defendingPos == null) {
                this.defendingPos = RaidManager.resolveDefendingPos(ctx.village());
            }
            CombatGoalSupport.tickCombat(v, this.defendingPos, 5.0, 0.7, 9.0, MillVillager::isRaiderEntity);
        }

        @Override
        public boolean isFinished() {
            return this.finished;
        }

        @Override
        public void stop(GoalContext ctx, StopReason reason) {
            if (ctx != null) {
                ctx.villager().getNavManager().stop(ctx.villager());
            }
        }
    }
}

