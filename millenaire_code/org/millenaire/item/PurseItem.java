/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.component.DataComponents
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.chat.MutableComponent
 *  net.minecraft.network.chat.Style
 *  net.minecraft.world.InteractionHand
 *  net.minecraft.world.InteractionResultHolder
 *  net.minecraft.world.entity.player.Inventory
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.Item$Properties
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.component.CustomData
 *  net.minecraft.world.level.Level
 */
package org.millenaire.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import org.millenaire.item.ModItems;
import org.millenaire.item.MoneyHelper;

public class PurseItem
extends Item {
    private static final String TAG_DENIER = "denier";
    private static final String TAG_DENIER_ARGENT = "denier_argent";
    private static final String TAG_DENIER_OR = "denier_or";

    public PurseItem(Item.Properties properties) {
        super(properties);
    }

    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack purse = player.getItemInHand(hand);
        if (level.isClientSide()) {
            return InteractionResultHolder.success((Object)purse);
        }
        int total = PurseItem.getTotalDeniers(purse);
        if (total > 0) {
            this.removeDeniersFromPurse(purse, player);
        } else {
            this.storeDeniersInPurse(purse, player);
        }
        return InteractionResultHolder.success((Object)purse);
    }

    public Component getName(ItemStack stack) {
        int total = PurseItem.getTotalDeniers(stack);
        if (total == 0) {
            return super.getName(stack);
        }
        CompoundTag tag = PurseItem.getTag(stack);
        int or = tag.getInt(TAG_DENIER_OR);
        int argent = tag.getInt(TAG_DENIER_ARGENT);
        int bronze = tag.getInt(TAG_DENIER);
        MutableComponent label = Component.translatable((String)this.getDescriptionId());
        StringBuilder sb = new StringBuilder(" : ");
        if (or > 0) {
            sb.append(or).append("o ");
        }
        if (argent > 0) {
            sb.append(argent).append("a ");
        }
        if (bronze > 0 || or == 0 && argent == 0) {
            sb.append(bronze).append("d");
        }
        return label.append((Component)Component.literal((String)sb.toString().trim()).withStyle(Style.EMPTY.withColor(16766720)));
    }

    private void storeDeniersInPurse(ItemStack purse, Player player) {
        Inventory inv = player.getInventory();
        int bronze = PurseItem.countAndRemoveItem(inv, (Item)ModItems.DENIER.get());
        int argent = PurseItem.countAndRemoveItem(inv, (Item)ModItems.DENIER_ARGENT.get());
        int or = PurseItem.countAndRemoveItem(inv, (Item)ModItems.DENIER_OR.get());
        CompoundTag tag = PurseItem.getTag(purse);
        int totalBronze = (bronze += tag.getInt(TAG_DENIER)) + (argent += tag.getInt(TAG_DENIER_ARGENT)) * 64 + (or += tag.getInt(TAG_DENIER_OR)) * 4096;
        or = totalBronze / 4096;
        argent = (totalBronze %= 4096) / 64;
        bronze = totalBronze % 64;
        PurseItem.setTag(purse, bronze, argent, or);
    }

    private void removeDeniersFromPurse(ItemStack purse, Player player) {
        int totalInPurse = PurseItem.getTotalDeniers(purse);
        int before = MoneyHelper.getTotalDeniers(player.getInventory());
        MoneyHelper.addLooseCoins(player.getInventory(), totalInPurse);
        int after = MoneyHelper.getTotalDeniers(player.getInventory());
        int actuallyAdded = after - before;
        int remaining = totalInPurse - actuallyAdded;
        PurseItem.setDeniersFromTotal(purse, Math.max(0, remaining));
    }

    private static CompoundTag getTag(ItemStack stack) {
        CustomData data = (CustomData)stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return new CompoundTag();
        }
        return data.copyTag();
    }

    private static void setTag(ItemStack stack, int bronze, int argent, int or) {
        CompoundTag tag = new CompoundTag();
        tag.putInt(TAG_DENIER, bronze);
        tag.putInt(TAG_DENIER_ARGENT, argent);
        tag.putInt(TAG_DENIER_OR, or);
        stack.set(DataComponents.CUSTOM_DATA, (Object)CustomData.of((CompoundTag)tag));
    }

    public static void clearDeniers(ItemStack stack) {
        PurseItem.setTag(stack, 0, 0, 0);
    }

    public static void setDeniersFromTotal(ItemStack stack, int totalBronze) {
        int or = totalBronze / 4096;
        int rest = totalBronze % 4096;
        int argent = rest / 64;
        int bronze = rest % 64;
        PurseItem.setTag(stack, bronze, argent, or);
    }

    public static int getTotalDeniers(ItemStack stack) {
        CompoundTag tag = PurseItem.getTag(stack);
        return tag.getInt(TAG_DENIER) + tag.getInt(TAG_DENIER_ARGENT) * 64 + tag.getInt(TAG_DENIER_OR) * 4096;
    }

    private static int countAndRemoveItem(Inventory inv, Item item) {
        int total = 0;
        for (int i = 0; i < inv.getContainerSize(); ++i) {
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty() || !stack.is(item)) continue;
            total += stack.getCount();
            inv.setItem(i, ItemStack.EMPTY);
        }
        return total;
    }
}

