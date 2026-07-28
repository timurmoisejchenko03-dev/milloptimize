/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Vec3i
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.entity.monster.Creeper
 *  net.minecraft.world.entity.monster.Monster
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.phys.AABB
 */
package org.millenaire.goal.impl;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.millenaire.building.BuildingInstance;
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
import org.millenaire.village.Village;
import org.millenaire.world.TerrainPreparer;

public class HuntMonsterGoal
implements VillagerGoal {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)"hunt_monster");
    private static final int PRIORITY = 50;
    private static final double SCAN_RADIUS = 50.0;
    private static final double SCAN_VERTICAL = 10.0;
    private static final double ATTACK_RANGE_SQ = 16.0;
    private static final int UNDERGROUND_MARGIN = 2;

    public static boolean isHuntableEntity(LivingEntity entity) {
        if (entity == null || !entity.isAlive()) {
            return false;
        }
        if (entity instanceof Player) {
            return false;
        }
        if (entity instanceof MillVillager) {
            return false;
        }
        if (entity instanceof Creeper) {
            return false;
        }
        return entity instanceof Monster;
    }

    static boolean isAtOrAboveSurface(double entityY, int surfaceY) {
        return entityY >= (double)(surfaceY - 2);
    }

    static boolean isAboveGround(LivingEntity e) {
        Level level = e.level();
        if (!(level instanceof ServerLevel)) {
            return true;
        }
        ServerLevel level2 = (ServerLevel)level;
        int surfaceY = TerrainPreparer.getGroundHeight(level2, e.getBlockX(), e.getBlockZ());
        return HuntMonsterGoal.isAtOrAboveSurface(e.getY(), surfaceY);
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public int computePriority(GoalContext context) {
        return 50;
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
        if (type == null || !type.isHelpInAttacks()) {
            return false;
        }
        if (context.village() == null) {
            return false;
        }
        return HuntMonsterGoal.findClosestHuntable(context) != null;
    }

    @Override
    public VillagerTask start(GoalContext context) {
        return new HuntMonsterTask(HuntMonsterGoal.findClosestHuntable(context));
    }

    @Nullable
    static LivingEntity findClosestHuntable(GoalContext context) {
        Village village = context.village();
        long now = context.villager().level().getGameTime();
        BuildingInstance townhall = village.getTownhall();
        BlockPos center = townhall != null ? townhall.getOrigin() : context.villager().blockPosition();
        AABB box = new AABB(center).inflate(50.0, 10.0, 50.0);
        LivingEntity best = null;
        double bestSq = Double.MAX_VALUE;
        for (LivingEntity e : context.villager().level().getEntitiesOfClass(LivingEntity.class, box, HuntMonsterGoal::isHuntableEntity)) {
            double d;
            if (!HuntMonsterGoal.isAboveGround(e) || village.isHuntUnreachable(e.blockPosition(), now) || !((d = context.villager().distanceToSqr((Entity)e)) < bestSq)) continue;
            bestSq = d;
            best = e;
        }
        return best;
    }

    static class HuntMonsterTask
    implements VillagerTask {
        private static final double WALK_SPEED = 0.65;
        private static final int MAX_TICKS = 600;
        @Nullable
        private LivingEntity target;
        @Nullable
        private MillVillager ctxVillager;
        private int ticks;

        HuntMonsterTask(@Nullable LivingEntity initial) {
            this.target = initial;
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
            ++this.ticks;
            this.ctxVillager = v = ctx.villager();
            if (this.target == null || !HuntMonsterGoal.isHuntableEntity(this.target) || this.ticks % 10 == 0) {
                this.target = HuntMonsterGoal.findClosestHuntable(ctx);
                if (this.target == null) {
                    return;
                }
            }
            v.setAttackTarget(this.target);
            double distSq = v.distanceToSqr((Entity)this.target);
            v.ensureCombatWeaponEquipped();
            if (CombatGoalSupport.shouldHoldAndFire(v, distSq)) {
                v.getLookControl().setLookAt((Entity)this.target, 30.0f, 30.0f);
                v.getNavManager().stop(v);
                v.performAttack(this.target);
            } else if (distSq <= 16.0) {
                v.getLookControl().setLookAt((Entity)this.target, 30.0f, 30.0f);
                v.performAttack(this.target);
                v.getNavManager().stop(v);
            } else {
                BlockPos to = this.target.blockPosition();
                VillagerNavDriver nav = v.getNavManager();
                if (nav.getDestination() == null || nav.getDestination().distSqr((Vec3i)to) > 4.0) {
                    nav.navigateTo(v, to, 0.65);
                }
            }
        }

        @Override
        public boolean isFinished() {
            return this.ticks > 600 || this.target == null || !this.target.isAlive();
        }

        @Override
        public void stop(GoalContext ctx, StopReason reason) {
            if (ctx != null) {
                ctx.villager().getNavManager().stop(ctx.villager());
                if (reason != StopReason.INTERRUPTED) {
                    ctx.villager().setAttackTarget(null);
                }
            }
        }
    }
}

