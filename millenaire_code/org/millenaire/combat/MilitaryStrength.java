/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.world.item.ArmorItem
 *  net.minecraft.world.item.ArmorMaterial
 *  net.minecraft.world.item.BowItem
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.ItemLike
 */
package org.millenaire.combat;

import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import org.millenaire.combat.CombatHelper;
import org.millenaire.entity.VillagerInventory;

public final class MilitaryStrength {
    private MilitaryStrength() {
    }

    public static int compute(int maxHealth, int baseAttackStrength, int bestMeleeWeaponDamage, boolean archerWithBow, int armorValue) {
        int strength = maxHealth / 2;
        int attack = baseAttackStrength + bestMeleeWeaponDamage;
        strength += attack * 2;
        if (archerWithBow) {
            strength += 10;
        }
        return strength += armorValue * 2;
    }

    public static InventoryScan scan(VillagerInventory inventory) {
        if (inventory == null) {
            return new InventoryScan(0, false, 0);
        }
        int bestMeleeDmg = 0;
        boolean hasBow = false;
        int armorTotal = 0;
        for (Item item : inventory.getAll().keySet()) {
            if (item instanceof BowItem) {
                hasBow = true;
                continue;
            }
            if (item instanceof ArmorItem) {
                ArmorItem armor = (ArmorItem)item;
                armorTotal += ((ArmorMaterial)armor.getMaterial().value()).defense().getOrDefault(armor.getType(), 0).intValue();
                continue;
            }
            int damage = (int)CombatHelper.weaponDamage(new ItemStack((ItemLike)item));
            if (damage <= 1 || damage <= bestMeleeDmg) continue;
            bestMeleeDmg = damage;
        }
        return new InventoryScan(bestMeleeDmg, hasBow, armorTotal);
    }

    public record InventoryScan(int bestMeleeWeaponDamage, boolean hasBow, int totalArmorValue) {
    }
}

