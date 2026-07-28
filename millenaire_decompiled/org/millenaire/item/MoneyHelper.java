/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  javax.annotation.Nullable
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.entity.player.Inventory
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.ItemLike
 *  org.slf4j.Logger
 */
package org.millenaire.item;

import com.mojang.logging.LogUtils;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import org.millenaire.advancement.MillAdvancements;
import org.millenaire.item.ModItems;
import org.millenaire.item.PurseItem;
import org.slf4j.Logger;

public final class MoneyHelper {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final int ARGENT_VALUE = 64;
    public static final int OR_VALUE = 4096;

    private MoneyHelper() {
    }

    public static int getTotalDeniers(Inventory inventory) {
        int total = 0;
        for (int i = 0; i < inventory.getContainerSize(); ++i) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty()) continue;
            total += MoneyHelper.valueOfStack(stack);
            if (!(stack.getItem() instanceof PurseItem)) continue;
            total += PurseItem.getTotalDeniers(stack);
        }
        return total;
    }

    public static boolean removeDeniers(Inventory inventory, int amount) {
        if (amount <= 0) {
            return true;
        }
        int available = MoneyHelper.getTotalDeniers(inventory);
        if (available < amount) {
            return false;
        }
        ItemStack purse = MoneyHelper.findPurse(inventory);
        if (purse != null) {
            int looseCoins = MoneyHelper.removeLooseCoins(inventory);
            int purseContent = PurseItem.getTotalDeniers(purse);
            int finalAmount = looseCoins + purseContent - amount;
            PurseItem.setDeniersFromTotal(purse, Math.max(0, finalAmount));
        } else {
            int totalRemoved = MoneyHelper.removeAllCoins(inventory);
            int change = totalRemoved - amount;
            if (change > 0) {
                MoneyHelper.addLooseCoins(inventory, change);
            }
        }
        return true;
    }

    public static void addDeniers(Inventory inventory, int amount, @Nullable Player player) {
        MoneyHelper.addDeniers(inventory, amount);
        if (player instanceof ServerPlayer) {
            ServerPlayer sp = (ServerPlayer)player;
            if (MoneyHelper.getTotalDeniers(inventory) >= 4096) {
                MillAdvancements.grant(sp, MillAdvancements.CRESUS);
            }
        }
    }

    public static void consolidateCoins(Inventory inventory) {
        ItemStack purse = MoneyHelper.findPurse(inventory);
        if (purse != null) {
            int looseCoins = MoneyHelper.removeLooseCoins(inventory);
            if (looseCoins > 0) {
                int purseContent = PurseItem.getTotalDeniers(purse);
                PurseItem.setDeniersFromTotal(purse, purseContent + looseCoins);
            }
        } else {
            int existingCoins = MoneyHelper.removeLooseCoins(inventory);
            if (existingCoins > 0) {
                MoneyHelper.addLooseCoins(inventory, existingCoins);
            }
        }
    }

    public static void addDeniers(Inventory inventory, int amount) {
        if (amount <= 0) {
            return;
        }
        ItemStack purse = MoneyHelper.findPurse(inventory);
        if (purse != null) {
            int looseCoins = MoneyHelper.removeLooseCoins(inventory);
            int purseContent = PurseItem.getTotalDeniers(purse);
            int finalAmount = looseCoins + purseContent + amount;
            PurseItem.setDeniersFromTotal(purse, finalAmount);
        } else {
            int existingCoins = MoneyHelper.removeLooseCoins(inventory);
            MoneyHelper.addLooseCoins(inventory, existingCoins + amount);
        }
    }

    static void addLooseCoins(Inventory inventory, int amount) {
        if (amount <= 0) {
            return;
        }
        int remaining = amount;
        int gold = remaining / 4096;
        int silver = (remaining %= 4096) / 64;
        int bronze = remaining %= 64;
        MoneyHelper.addStacks(inventory, (Item)ModItems.DENIER_OR.get(), gold);
        MoneyHelper.addStacks(inventory, (Item)ModItems.DENIER_ARGENT.get(), silver);
        MoneyHelper.addStacks(inventory, (Item)ModItems.DENIER.get(), bronze);
    }

    public static String formatPrice(int deniers) {
        int or = deniers / 4096;
        int rest = deniers % 4096;
        int argent = rest / 64;
        int bronze = rest % 64;
        StringBuilder sb = new StringBuilder();
        if (or > 0) {
            sb.append(or).append("o ");
        }
        if (argent > 0) {
            sb.append(argent).append("a ");
        }
        if (bronze > 0 || sb.isEmpty()) {
            sb.append(bronze).append("d");
        }
        return sb.toString().trim();
    }

    static Denomination toDenomination(int totalBronze) {
        int or = totalBronze / 4096;
        int rest = totalBronze % 4096;
        int argent = rest / 64;
        int bronze = rest % 64;
        return new Denomination(bronze, argent, or);
    }

    private static ItemStack findPurse(Inventory inventory) {
        ItemStack firstPurse = null;
        int firstPurseTotal = 0;
        for (int i = 0; i < inventory.getContainerSize(); ++i) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty() || !(stack.getItem() instanceof PurseItem)) continue;
            if (firstPurse == null) {
                firstPurse = stack;
                firstPurseTotal = PurseItem.getTotalDeniers(stack);
                continue;
            }
            firstPurseTotal += PurseItem.getTotalDeniers(stack);
            PurseItem.clearDeniers(stack);
        }
        if (firstPurse != null && firstPurseTotal > 0) {
            PurseItem.setDeniersFromTotal(firstPurse, firstPurseTotal);
        }
        return firstPurse;
    }

    private static int valueOfStack(ItemStack stack) {
        Item item = stack.getItem();
        if (item == ModItems.DENIER.get()) {
            return stack.getCount();
        }
        if (item == ModItems.DENIER_ARGENT.get()) {
            return stack.getCount() * 64;
        }
        if (item == ModItems.DENIER_OR.get()) {
            return stack.getCount() * 4096;
        }
        return 0;
    }

    private static int removeLooseCoins(Inventory inventory) {
        int total = 0;
        for (int i = 0; i < inventory.getContainerSize(); ++i) {
            int value;
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty() || (value = MoneyHelper.valueOfStack(stack)) <= 0) continue;
            total += value;
            inventory.setItem(i, ItemStack.EMPTY);
        }
        return total;
    }

    private static int removeAllCoins(Inventory inventory) {
        int total = 0;
        for (int i = 0; i < inventory.getContainerSize(); ++i) {
            int purseValue;
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty()) continue;
            int value = MoneyHelper.valueOfStack(stack);
            if (value > 0) {
                total += value;
                inventory.setItem(i, ItemStack.EMPTY);
            }
            if (!(stack.getItem() instanceof PurseItem) || (purseValue = PurseItem.getTotalDeniers(stack)) <= 0) continue;
            total += purseValue;
            PurseItem.clearDeniers(stack);
        }
        return total;
    }

    private static void addStacks(Inventory inventory, Item item, int count) {
        while (count > 0) {
            int stackSize = Math.min(count, 64);
            ItemStack stack = new ItemStack((ItemLike)item, stackSize);
            if (!inventory.add(stack)) {
                LOGGER.warn("[Mill\u00e9naire] Could not add {} x{} to inventory \u2014 inventory full, coins lost!", (Object)item, (Object)stack.getCount());
            }
            count -= stackSize;
        }
    }

    record Denomination(int bronze, int argent, int or) {
        int totalBronze() {
            return this.bronze + this.argent * 64 + this.or * 4096;
        }
    }
}

