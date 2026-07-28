/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.EquipmentSlot
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.entity.ai.attributes.Attributes
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.component.ItemAttributeModifiers$Entry
 *  net.minecraft.world.phys.AABB
 */
package org.millenaire.combat;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.phys.AABB;
import org.millenaire.culture.ModCultures;
import org.millenaire.culture.VillagerType;
import org.millenaire.entity.MillVillager;

public final class CombatHelper {
    private CombatHelper() {
    }

    public static void callForHelp(MillVillager victim, LivingEntity attacker) {
        double attackRangeSq = 6400.0;
        AABB box = victim.getBoundingBox().inflate(80.0);
        for (MillVillager other : victim.level().getEntitiesOfClass(MillVillager.class, box)) {
            VillagerType type;
            if (other == victim || !other.isAlive() || other.getAttackTarget() != null || other.isRaiderEntity() || (type = ModCultures.getVillagerType(other.getVillagerTypeId())) == null || !type.isHelpInAttacks() || other.distanceToSqr((Entity)attacker) >= attackRangeSq) continue;
            other.setAttackTarget(attacker);
            other.speakCombatCall("calltoarms");
        }
    }

    public static int entityAttackStrength(int baseAttackStrength, double weaponDamage) {
        return baseAttackStrength + (int)Math.ceil(weaponDamage / 2.0);
    }

    public static double weaponDamage(ItemStack stack) {
        for (ItemAttributeModifiers.Entry entry : stack.getAttributeModifiers().modifiers()) {
            if (!entry.attribute().equals((Object)Attributes.ATTACK_DAMAGE) || !entry.slot().test(EquipmentSlot.MAINHAND)) continue;
            return entry.modifier().amount();
        }
        return 0.0;
    }

    public static boolean isPlayerTargetable(boolean creative, boolean spectator) {
        return !creative && !spectator;
    }

    public static boolean isVillagerHostile(boolean selfIsRaiderEntity, boolean attackerIsRaiderEntity, boolean sameVillage) {
        return selfIsRaiderEntity != attackerIsRaiderEntity || !sameVillage;
    }

    public static boolean canTeleport(boolean hasNoTeleportTag, boolean isRaider) {
        return !hasNoTeleportTag || isRaider;
    }

    public static AttackType decideAttack(double distance, boolean isArcher, boolean hasBow, int attackCooldown, boolean verticalOverlap) {
        boolean archerBranch;
        boolean bl = archerBranch = isArcher && hasBow && distance > 5.0;
        if (archerBranch) {
            if (distance < 20.0 && attackCooldown <= 0) {
                return AttackType.RANGED;
            }
            return AttackType.NONE;
        }
        if (attackCooldown <= 0 && distance < 2.0 && verticalOverlap) {
            return AttackType.MELEE;
        }
        return AttackType.NONE;
    }

    public static enum AttackType {
        NONE,
        MELEE,
        RANGED;

    }
}

