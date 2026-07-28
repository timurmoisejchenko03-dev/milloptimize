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
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.phys.AABB
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import org.millenaire.combat.CombatHelper;
import org.millenaire.culture.ModCultures;
import org.millenaire.culture.VillagerType;
import org.millenaire.entity.MillVillager;
import org.millenaire.entity.VillagerNavDriver;
import org.millenaire.goal.GoalContext;
import org.millenaire.goal.StopReason;
import org.millenaire.goal.TravelPhase;
import org.millenaire.goal.VillagerGoal;
import org.millenaire.goal.VillagerTask;
import org.millenaire.goal.impl.CombatGoalSupport;

public class HostileVillagerGoal
implements VillagerGoal {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)"hostile_villager");
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
        VillagerType type = ModCultures.getVillagerType(v.getVillagerTypeId());
        if (type == null || !type.isHostile()) {
            return false;
        }
        return HostileVillagerGoal.findTargetPlayer(v) != null;
    }

    @Override
    public VillagerTask start(GoalContext context) {
        return new HostileVillagerTask();
    }

    @Nullable
    static ServerPlayer findTargetPlayer(MillVillager v) {
        VillagerType type = ModCultures.getVillagerType(v.getVillagerTypeId());
        double range = type != null && type.isDefensive() ? 20.0 : 80.0;
        double rangeSq = range * range;
        double bestSq = rangeSq + 1.0;
        ServerPlayer best = null;
        AABB box = v.getBoundingBox().inflate(range);
        for (Player p : v.level().getEntitiesOfClass(Player.class, box, pl -> pl.isAlive() && CombatHelper.isPlayerTargetable(pl.isCreative(), pl.isSpectator()))) {
            if (!(p instanceof ServerPlayer)) continue;
            ServerPlayer sp = (ServerPlayer)p;
            double d = v.distanceToSqr((Entity)p);
            if (!(d < bestSq)) continue;
            bestSq = d;
            best = sp;
        }
        return best;
    }

    static class HostileVillagerTask
    implements VillagerTask {
        private static final double WALK_SPEED = 0.7;
        @Nullable
        private ServerPlayer target;
        @Nullable
        private MillVillager ctxVillager;
        private boolean done;

        HostileVillagerTask() {
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
            if (this.target == null || !this.target.isAlive() || !CombatHelper.isPlayerTargetable(this.target.isCreative(), this.target.isSpectator())) {
                this.target = HostileVillagerGoal.findTargetPlayer(v);
                if (this.target == null) {
                    this.done = true;
                    return;
                }
            }
            v.setAttackTarget((LivingEntity)this.target);
            double distSq = v.distanceToSqr((Entity)this.target);
            if (distSq > 6400.0) {
                this.done = true;
                return;
            }
            v.getLookControl().setLookAt((Entity)this.target, 30.0f, 30.0f);
            v.ensureCombatWeaponEquipped();
            if (CombatGoalSupport.shouldHoldAndFire(v, distSq)) {
                v.getNavManager().stop(v);
                v.performAttack((LivingEntity)this.target);
            } else if (distSq > 4.0) {
                BlockPos to = this.target.blockPosition();
                VillagerNavDriver nav = v.getNavManager();
                if (nav.getDestination() == null || nav.getDestination().distSqr((Vec3i)to) > 4.0) {
                    nav.navigateTo(v, to, 0.7);
                }
            } else {
                v.performAttack((LivingEntity)this.target);
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

