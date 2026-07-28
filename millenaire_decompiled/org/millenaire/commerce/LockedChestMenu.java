/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  net.minecraft.network.FriendlyByteBuf
 *  net.minecraft.world.Container
 *  net.minecraft.world.SimpleContainer
 *  net.minecraft.world.entity.player.Inventory
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.inventory.AbstractContainerMenu
 *  net.minecraft.world.inventory.ClickType
 *  net.minecraft.world.inventory.Slot
 *  net.minecraft.world.item.ItemStack
 */
package org.millenaire.commerce;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.millenaire.commerce.ModMenuTypes;

public class LockedChestMenu
extends AbstractContainerMenu {
    private final Container chestInventory;
    private final int numRows;
    private final boolean locked;

    public LockedChestMenu(int containerId, Inventory playerInventory, Container chestInventory, boolean locked) {
        super(ModMenuTypes.LOCKED_CHEST.get(), containerId);
        int col;
        int row;
        this.chestInventory = chestInventory;
        this.numRows = chestInventory.getContainerSize() / 9;
        this.locked = locked;
        chestInventory.startOpen(playerInventory.player);
        int yOffset = (this.numRows - 4) * 18;
        for (row = 0; row < this.numRows; ++row) {
            for (col = 0; col < 9; ++col) {
                int slotIndex = col + row * 9;
                int x = 8 + col * 18;
                int y = 18 + row * 18;
                if (locked) {
                    this.addSlot(new LockedSlot(chestInventory, slotIndex, x, y));
                    continue;
                }
                this.addSlot(new Slot(chestInventory, slotIndex, x, y));
            }
        }
        for (row = 0; row < 3; ++row) {
            for (col = 0; col < 9; ++col) {
                this.addSlot(new Slot((Container)playerInventory, col + row * 9 + 9, 8 + col * 18, 103 + row * 18 + yOffset));
            }
        }
        for (int col2 = 0; col2 < 9; ++col2) {
            this.addSlot(new Slot((Container)playerInventory, col2, 8 + col2 * 18, 161 + yOffset));
        }
    }

    public static LockedChestMenu fromNetwork(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        byte rows = buf.readByte();
        boolean locked = buf.readBoolean();
        SimpleContainer container = new SimpleContainer(rows * 9);
        return new LockedChestMenu(containerId, playerInventory, (Container)container, locked);
    }

    public void writeToBuffer(FriendlyByteBuf buf) {
        buf.writeByte(this.numRows);
        buf.writeBoolean(this.locked);
    }

    public boolean isLocked() {
        return this.locked;
    }

    public int getNumRows() {
        return this.numRows;
    }

    public Container getContainer() {
        return this.chestInventory;
    }

    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        Slot slot;
        if (this.locked && slotId >= 0 && slotId < this.slots.size() && (slot = (Slot)this.slots.get(slotId)) instanceof LockedSlot) {
            ItemStack carried = this.getCarried();
            if (!carried.isEmpty() && slot.mayPlace(carried)) {
                super.clicked(slotId, button, clickType, player);
                return;
            }
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = (Slot)this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            result = slotStack.copy();
            if (index < this.numRows * 9) {
                if (slot instanceof LockedSlot) {
                    return ItemStack.EMPTY;
                }
                if (!this.moveItemStackTo(slotStack, this.numRows * 9, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(slotStack, 0, this.numRows * 9, false)) {
                return ItemStack.EMPTY;
            }
            if (slotStack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return result;
    }

    public boolean stillValid(Player player) {
        return this.chestInventory.stillValid(player);
    }

    public void removed(Player player) {
        super.removed(player);
        this.chestInventory.stopOpen(player);
    }

    private static class LockedSlot
    extends Slot {
        public LockedSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        public boolean mayPickup(Player player) {
            return false;
        }

        public boolean mayPlace(ItemStack stack) {
            return true;
        }
    }
}

