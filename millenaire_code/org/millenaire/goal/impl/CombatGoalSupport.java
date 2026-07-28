/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Vec3i
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.item.BowItem
 */
package org.millenaire.goal.impl;

import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BowItem;
import org.millenaire.culture.ModCultures;
import org.millenaire.culture.VillagerType;
import org.millenaire.entity.MillVillager;

final class CombatGoalSupport {
    private CombatGoalSupport() {
    }

    static boolean shouldHoldAndFire(MillVillager v, double distSq) {
        VillagerType vt = ModCultures.getVillagerType(v.getVillagerTypeId());
        if (vt == null || !vt.isArcher()) {
            return false;
        }
        if (!(v.getMainHandItem().getItem() instanceof BowItem)) {
            return false;
        }
        double d = Math.sqrt(distSq);
        return d > 5.0 && d < 20.0;
    }

    static void tickCombat(MillVillager v, BlockPos defendingPos, double acquireRadius, double walkSpeed, double fallbackArriveSq, Predicate<MillVillager> isOpponent) {
        MillVillager target = CombatGoalSupport.validateOrAcquireTarget(v, defendingPos, acquireRadius, isOpponent);
        if (target != null) {
            v.setAttackTarget((LivingEntity)target);
            v.getLookControl().setLookAt((Entity)target, 30.0f, 30.0f);
            double tDistSq = v.distanceToSqr((Entity)target);
            v.ensureCombatWeaponEquipped();
            if (CombatGoalSupport.shouldHoldAndFire(v, tDistSq)) {
                v.getNavManager().stop(v);
                v.performAttack((LivingEntity)target);
            } else if (tDistSq > 4.0) {
                v.getNavManager().navigateTo(v, target.blockPosition(), walkSpeed);
            } else {
                v.getNavManager().stop(v);
                v.performAttack((LivingEntity)target);
            }
            return;
        }
        if (v.blockPosition().distSqr((Vec3i)defendingPos) > fallbackArriveSq) {
            v.getNavManager().navigateTo(v, defendingPos, walkSpeed);
        } else {
            v.getNavManager().stop(v);
        }
    }

    @Nullable
    private static MillVillager validateOrAcquireTarget(MillVillager v, BlockPos defendingPos, double acquireRadius, Predicate<MillVillager> isOpponent) {
        MillVillager mv;
        LivingEntity current = v.getAttackTarget();
        if (current instanceof MillVillager && (mv = (MillVillager)current).isAlive() && isOpponent.test(mv)) {
            boolean tooFar;
            VillagerType vt = ModCultures.getVillagerType(v.getVillagerTypeId());
            boolean defensiveLeashBroken = vt != null && vt.isDefensive() && Math.sqrt(v.blockPosition().distSqr((Vec3i)defendingPos)) > 20.0;
            boolean bl = tooFar = Math.sqrt(v.distanceToSqr((Entity)mv)) > 80.0;
            if (!defensiveLeashBroken && !tooFar) {
                return mv;
            }
        }
        if (current != null) {
            v.setAttackTarget(null);
        }
        return CombatGoalSupport.findClosestOpponent(v, acquireRadius, isOpponent);
    }

    @Nullable
    private static MillVillager findClosestOpponent(MillVillager v, double radius, Predicate<MillVillager> isOpponent) {
        double bestSq = radius * radius;
        MillVillager best = null;
        for (MillVillager other : v.level().getEntitiesOfClass(MillVillager.class, v.getBoundingBox().inflate(radius))) {
            double d;
            if (other == v || !other.isAlive() || !isOpponent.test(other) || !((d = v.distanceToSqr((Entity)other)) < bestSq)) continue;
            bestSq = d;
            best = other;
        }
        return best;
    }
}

