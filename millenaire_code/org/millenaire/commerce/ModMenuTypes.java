/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.Registry
 *  net.minecraft.core.registries.BuiltInRegistries
 *  net.minecraft.world.inventory.MenuType
 *  net.neoforged.neoforge.common.extensions.IMenuTypeExtension
 *  net.neoforged.neoforge.registries.DeferredRegister
 */
package org.millenaire.commerce;

import java.util.function.Supplier;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.millenaire.block.FirePitMenu;
import org.millenaire.commerce.LockedChestMenu;
import org.millenaire.commerce.TradeMenu;

public final class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create((Registry)BuiltInRegistries.MENU, (String)"millenaire");
    public static final Supplier<MenuType<TradeMenu>> TRADE = MENU_TYPES.register("trade", () -> IMenuTypeExtension.create(TradeMenu::fromNetwork));
    public static final Supplier<MenuType<LockedChestMenu>> LOCKED_CHEST = MENU_TYPES.register("locked_chest", () -> IMenuTypeExtension.create(LockedChestMenu::fromNetwork));
    public static final Supplier<MenuType<FirePitMenu>> FIRE_PIT = MENU_TYPES.register("fire_pit", () -> IMenuTypeExtension.create(FirePitMenu::fromNetwork));

    private ModMenuTypes() {
    }
}

