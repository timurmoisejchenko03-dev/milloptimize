/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  net.minecraft.world.item.BowItem
 *  net.minecraft.world.item.Item$Properties
 */
package org.millenaire.item;

import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.Item;

public class MillenaireBow
extends BowItem {
    private final float speedFactor;
    private final float damageBonus;
    private final int enchantability;

    public MillenaireBow(Item.Properties properties, float speedFactor, float damageBonus, int enchantability) {
        super(properties);
        this.speedFactor = speedFactor;
        this.damageBonus = damageBonus;
        this.enchantability = enchantability;
    }

    public int getEnchantability() {
        return this.enchantability;
    }

    public float getSpeedFactor() {
        return this.speedFactor;
    }

    public float getDamageBonus() {
        return this.damageBonus;
    }
}

