/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Vec3i
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.entity.monster.Monster
 *  net.minecraft.world.item.ItemStack
 */
package org.millenaire.goal.impl;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import org.millenaire.combat.CombatHelper;
import org.millenaire.entity.MillVillager;
import org.millenaire.entity.VillagerNavDriver;
import org.millenaire.goal.GoalContext;
import org.millenaire.goal.StopReason;
import org.millenaire.goal.TravelPhase;
import org.millenaire.goal.VillagerGoal;
import org.millenaire.goal.VillagerTask;
import org.millenaire.goal.impl.CombatGoalSupport;

public class EngageTargetGoal
implements VillagerGoal {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)"engage_target");
    static final int PRIORITY = 5000;

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public int computePriority(GoalContext context) {
        return 5000;
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
        return EngageTargetGoal.resolveTarget(v) != null;
    }

    @Override
    public VillagerTask start(GoalContext context) {
        return new EngageTargetTask();
    }

    @Nullable
    static LivingEntity resolveTarget(MillVillager v) {
        LivingEntity t = v.getAttackTarget();
        if (t == null || !t.isAlive()) {
            return null;
        }
        if (t instanceof ServerPlayer) {
            ServerPlayer sp = (ServerPlayer)t;
            return CombatHelper.isPlayerTargetable(sp.isCreative(), sp.isSpectator()) ? sp : null;
        }
        if (t instanceof Monster || t instanceof MillVillager) {
            return t;
        }
        return null;
    }

    static class EngageTargetTask
    implements VillagerTask {
        private static final double WALK_SPEED = 0.7;
        @Nullable
        private MillVillager ctxVillager;
        private boolean done;

        EngageTargetTask() {
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
            LivingEntity target = EngageTargetGoal.resolveTarget(v);
            if (target == null) {
                this.done = true;
                return;
            }
            double distSq = v.distanceToSqr((Entity)target);
            if (distSq > 6400.0) {
                this.done = true;
                return;
            }
            v.getLookControl().setLookAt((Entity)target, 30.0f, 30.0f);
            v.ensureCombatWeaponEquipped();
            if (CombatGoalSupport.shouldHoldAndFire(v, distSq)) {
                v.getNavManager().stop(v);
                v.performAttack(target);
            } else if (distSq > 4.0) {
                BlockPos to = target.blockPosition();
                VillagerNavDriver nav = v.getNavManager();
                if (nav.getDestination() == null || nav.getDestination().distSqr((Vec3i)to) > 4.0) {
                    nav.navigateTo(v, to, 0.7);
                }
            } else {
                v.performAttack(target);
            }
        }

        @Override
        public boolean isFinished() {
            return this.done;
        }

        @Override
        public void stop(GoalContext ctx, StopReason reason) {
            if (ctx != null) {
                ctx.villager().getNavManager().stop(ctx.villager());
            }
        }
    }
}

