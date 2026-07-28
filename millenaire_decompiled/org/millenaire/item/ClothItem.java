/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.Item$Properties
 */
package org.millenaire.item;

import net.minecraft.world.item.Item;

public class ClothItem
extends Item {
    private final String clothName;
    private final int priority;

    public ClothItem(String clothName, int priority, Item.Properties properties) {
        super(properties);
        this.clothName = clothName;
        this.priority = priority;
    }

    public String getClothName() {
        return this.clothName;
    }

    public int getPriority() {
        return this.priority;
    }
}

