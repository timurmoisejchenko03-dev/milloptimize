/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  net.minecraft.network.RegistryFriendlyByteBuf
 *  net.minecraft.world.Container
 *  net.minecraft.world.SimpleContainer
 *  net.minecraft.world.entity.player.Inventory
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.inventory.AbstractContainerMenu
 *  net.minecraft.world.inventory.ContainerData
 *  net.minecraft.world.inventory.SimpleContainerData
 *  net.minecraft.world.inventory.Slot
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity
 */
package org.millenaire.block;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.millenaire.block.FirePitBlockEntity;
import org.millenaire.block.FirePitFuelSlot;
import org.millenaire.block.FirePitInputSlot;
import org.millenaire.block.FirePitOutputSlot;
import org.millenaire.commerce.ModMenuTypes;

public class FirePitMenu
extends AbstractContainerMenu {
    private static final int[][] INPUT_POSITIONS = new int[][]{{56, 8}, {44, 28}, {56, 48}};
    private static final int[][] OUTPUT_POSITIONS = new int[][]{{104, 8}, {116, 28}, {104, 48}};
    private static final int[] FUEL_POSITION = new int[]{80, 70};
    private static final int[] INV_POSITION = new int[]{8, 93};
    private final Container container;
    private final ContainerData data;
    private final int inputStart;
    private final int inputEnd;
    private final int fuelStart;
    private final int fuelEnd;
    private final int outputStart;
    private final int outputEnd;
    private final int inventoryStart;
    private final int inventoryEnd;
    private final int hotbarStart;
    private final int hotbarEnd;

    public FirePitMenu(int containerId, Inventory playerInventory, Container container, ContainerData data) {
        super(ModMenuTypes.FIRE_PIT.get(), containerId);
        int i;
        this.container = container;
        this.data = data;
        this.inputStart = this.slots.size();
        for (i = 0; i < 3; ++i) {
            this.addSlot(new FirePitInputSlot(container, i, INPUT_POSITIONS[i][0], INPUT_POSITIONS[i][1]));
        }
        this.inputEnd = this.slots.size();
        this.fuelStart = this.slots.size();
        this.addSlot(new FirePitFuelSlot(container, 3, FUEL_POSITION[0], FUEL_POSITION[1]));
        this.fuelEnd = this.slots.size();
        this.outputStart = this.slots.size();
        for (i = 0; i < 3; ++i) {
            this.addSlot(new FirePitOutputSlot(playerInventory.player, container, 4 + i, OUTPUT_POSITIONS[i][0], OUTPUT_POSITIONS[i][1]));
        }
        this.outputEnd = this.slots.size();
        this.inventoryStart = this.slots.size();
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot((Container)playerInventory, col + row * 9 + 9, INV_POSITION[0] + col * 18, INV_POSITION[1] + row * 18));
            }
        }
        this.inventoryEnd = this.slots.size();
        this.hotbarStart = this.slots.size();
        for (i = 0; i < 9; ++i) {
            this.addSlot(new Slot((Container)playerInventory, i, INV_POSITION[0] + i * 18, INV_POSITION[1] + 54 + 4));
        }
        this.hotbarEnd = this.slots.size();
        this.addDataSlots(data);
    }

    public static FirePitMenu fromNetwork(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        return new FirePitMenu(containerId, playerInventory, (Container)new SimpleContainer(7), (ContainerData)new SimpleContainerData(5));
    }

    public boolean stillValid(Player player) {
        return true;
    }

    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack original = ItemStack.EMPTY;
        Slot slot = (Slot)this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            original = stackInSlot.copy();
            if (FirePitMenu.inRange(index, this.outputStart, this.outputEnd)) {
                if (!this.moveItemStackTo(stackInSlot, this.inventoryStart, this.hotbarEnd, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickCraft(stackInSlot, original);
            } else if (FirePitMenu.inRange(index, this.inventoryStart, this.hotbarEnd) ? (this.isFirePitBurnableForMenu(stackInSlot) ? !this.moveItemStackTo(stackInSlot, this.inputStart, this.inputEnd, false) : (FirePitMenu.isFuel(stackInSlot) ? !this.moveItemStackTo(stackInSlot, this.fuelStart, this.fuelEnd, false) : (FirePitMenu.inRange(index, this.inventoryStart, this.inventoryEnd) ? !this.moveItemStackTo(stackInSlot, this.hotbarStart, this.hotbarEnd, false) : !this.moveItemStackTo(stackInSlot, this.inventoryStart, this.inventoryStart, false)))) : !this.moveItemStackTo(stackInSlot, this.inventoryStart, this.hotbarEnd, false)) {
                return ItemStack.EMPTY;
            }
            if (stackInSlot.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            if (stackInSlot.getCount() == original.getCount()) {
                return ItemStack.EMPTY;
            }
            slot.onTake(player, stackInSlot);
        }
        return original;
    }

    public int getBurnTime() {
        return this.data.get(3);
    }

    public int getTotalBurnTime() {
        return this.data.get(4);
    }

    public int getCookTime(int lane) {
        return this.data.get(lane);
    }

    public int getBurnLeftScaled(int pixels) {
        int total = this.getTotalBurnTime();
        if (total == 0) {
            total = 200;
        }
        return this.getBurnTime() * pixels / total;
    }

    public int getCookProgressScaled(int lane, int pixels) {
        int cook = this.getCookTime(lane);
        return cook != 0 ? cook * pixels / 200 : 0;
    }

    private static boolean inRange(int index, int start, int end) {
        return start <= index && index < end;
    }

    private static boolean isFuel(ItemStack stack) {
        return AbstractFurnaceBlockEntity.isFuel((ItemStack)stack);
    }

    private boolean isFirePitBurnableForMenu(ItemStack stack) {
        FirePitBlockEntity blockEntity;
        Level level;
        Container container = this.container;
        if (container instanceof FirePitBlockEntity && (level = (blockEntity = (FirePitBlockEntity)container).getLevel()) != null) {
            return FirePitBlockEntity.isFirePitBurnable(stack, level);
        }
        return FirePitBlockEntity.isFirePitBurnable(stack);
    }
}

