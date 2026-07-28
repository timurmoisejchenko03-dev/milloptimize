/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.network.chat.Component
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.Difficulty
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.entity.Mob
 *  net.minecraft.world.entity.monster.Creeper
 *  net.minecraft.world.entity.monster.Monster
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.phys.AABB
 *  org.jetbrains.annotations.Nullable
 */
package org.millenaire.goal.impl;

import java.util.List;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import org.millenaire.culture.ModCultures;
import org.millenaire.culture.VillagerType;
import org.millenaire.entity.MillVillager;
import org.millenaire.goal.GoalContext;
import org.millenaire.goal.NavigationHelperUtils;
import org.millenaire.goal.StopReason;
import org.millenaire.goal.TravelPhase;
import org.millenaire.goal.VillagerGoal;
import org.millenaire.goal.VillagerTask;
import org.millenaire.goal.impl.CombatGoalSupport;
import org.millenaire.hire.HiringHelper;
import org.millenaire.village.Village;

public class HiredEscortGoal
implements VillagerGoal {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)"hired_escort");

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public int computePriority(GoalContext c) {
        return 10000;
    }

    @Override
    public boolean isCombatUrgent() {
        return true;
    }

    @Override
    public boolean canBeDoneAtNight() {
        return true;
    }

    @Override
    public boolean canStart(GoalContext c) {
        return c.villager().isHired();
    }

    @Override
    public VillagerTask start(GoalContext c) {
        return new HiredEscortTask();
    }

    static boolean isOpponent(boolean targetsOwner, boolean isHostileMonster, boolean isHostileVillager, boolean isCreeper, boolean isOwner, boolean aggressive) {
        if (isOwner || isCreeper) {
            return false;
        }
        if (targetsOwner) {
            return true;
        }
        return aggressive && (isHostileMonster || isHostileVillager);
    }

    private static void followOwner(MillVillager v, ServerPlayer owner) {
        double dist = Math.sqrt(v.distanceToSqr((Entity)owner));
        if (dist > 16.0) {
            NavigationHelperUtils.teleportToSafe(v, owner.blockPosition());
        } else if (dist > 4.0) {
            v.getNavManager().navigateTo(v, owner.blockPosition(), 0.5);
        } else {
            v.getNavManager().stop(v);
        }
    }

    private static void selfHeal(MillVillager v) {
        if (v.getHealth() < v.getMaxHealth() && v.getRandom().nextInt(1600) == 0) {
            v.heal(1.0f);
        }
    }

    static void endHire(MillVillager v, ServerLevel level, String msgKey) {
        ServerPlayer p;
        Village village;
        UUID owner = v.getHiredBy();
        Village village2 = village = v.getVillageId() == null ? null : Village.resolve(level, v.getVillageId());
        if (village != null) {
            village.setVillagerHired(level, v.getUUID(), null, 0L);
        } else {
            v.setHireState(null, 0L);
        }
        if (owner != null && (p = level.getServer().getPlayerList().getPlayer(owner)) != null) {
            p.sendSystemMessage((Component)Component.translatable((String)msgKey, (Object[])new Object[]{v.getName()}));
        }
    }

    @Nullable
    private static LivingEntity acquireOpponent(MillVillager v, ServerPlayer owner, boolean aggressive) {
        AABB box = new AABB(owner.blockPosition()).inflate(16.0, 8.0, 16.0);
        LivingEntity best = null;
        double bestSq = 6400.0;
        for (LivingEntity e : v.level().getEntitiesOfClass(LivingEntity.class, box)) {
            double d;
            MillVillager mv;
            boolean hostileVillager;
            boolean monster;
            Mob m;
            boolean targetsOwner;
            if (e == v || e == owner || !e.isAlive() || !HiredEscortGoal.isOpponent(targetsOwner = e instanceof Mob && (m = (Mob)e).getTarget() == owner, monster = e instanceof Monster && !(e instanceof Creeper), hostileVillager = e instanceof MillVillager && HiredEscortGoal.isHostileVillager(mv = (MillVillager)e), e instanceof Creeper, false, aggressive) || !((d = v.distanceToSqr((Entity)e)) < bestSq)) continue;
            bestSq = d;
            best = e;
        }
        return best;
    }

    private static boolean isHostileVillager(MillVillager mv) {
        VillagerType type = ModCultures.getVillagerType(mv.getVillagerTypeId());
        return type != null && type.isHostile();
    }

    @Nullable
    private static LivingEntity retainValidTarget(MillVillager v, ServerPlayer owner, ServerLevel level) {
        LivingEntity cur = v.getAttackTarget();
        if (cur == null || !cur.isAlive()) {
            return null;
        }
        if (cur == owner || cur instanceof Creeper) {
            return null;
        }
        if (level.getDifficulty() == Difficulty.PEACEFUL) {
            return null;
        }
        if (v.distanceToSqr((Entity)cur) > 6400.0) {
            return null;
        }
        return cur;
    }

    static class HiredEscortTask
    implements VillagerTask {
        private boolean finished;

        HiredEscortTask() {
        }

        @Override
        public ResourceLocation goalId() {
            return ID;
        }

        @Override
        public boolean isFinished() {
            return this.finished;
        }

        @Override
        public List<ItemStack> getHeldItems(TravelPhase phase) {
            return List.of();
        }

        @Override
        public void tick(GoalContext ctx) {
            MillVillager v = ctx.villager();
            Level level = v.level();
            if (!(level instanceof ServerLevel)) {
                return;
            }
            ServerLevel level2 = (ServerLevel)level;
            if (!v.isHired()) {
                this.finished = true;
                return;
            }
            long now = level2.getGameTime();
            if (HiringHelper.isExpired(v.getHiredUntil(), now)) {
                HiredEscortGoal.endHire(v, level2, "message.millenaire.hire.hireover");
                this.finished = true;
                return;
            }
            ServerPlayer owner = level2.getServer().getPlayerList().getPlayer(v.getHiredBy());
            if (owner == null || owner.level() != level2) {
                v.getNavManager().stop(v);
                return;
            }
            HiredEscortGoal.selfHeal(v);
            v.setHireHoursLeft(HiringHelper.hoursLeft(v.getHiredUntil(), now));
            LivingEntity target = HiredEscortGoal.retainValidTarget(v, owner, level2);
            if (target == null) {
                target = HiredEscortGoal.acquireOpponent(v, owner, v.isAggressiveStance());
            }
            if (target != null) {
                v.setAttackTarget(target);
                v.ensureCombatWeaponEquipped();
                double dSq = v.distanceToSqr((Entity)target);
                if (CombatGoalSupport.shouldHoldAndFire(v, dSq)) {
                    v.getNavManager().stop(v);
                    v.performAttack(target);
                } else if (dSq <= 4.0) {
                    v.getNavManager().stop(v);
                    v.performAttack(target);
                } else {
                    v.getNavManager().navigateTo(v, target.blockPosition(), 0.5);
                }
                return;
            }
            v.setAttackTarget(null);
            HiredEscortGoal.followOwner(v, owner);
        }

        @Override
        public void stop(GoalContext ctx, StopReason reason) {
            if (ctx != null) {
                ctx.villager().getNavManager().stop(ctx.villager());
                ctx.villager().setAttackTarget(null);
            }
        }
    }
}

