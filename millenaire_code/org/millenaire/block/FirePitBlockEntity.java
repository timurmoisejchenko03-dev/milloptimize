/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.HolderLookup$Provider
 *  net.minecraft.core.NonNullList
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.protocol.Packet
 *  net.minecraft.network.protocol.game.ClientGamePacketListener
 *  net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
 *  net.minecraft.util.Mth
 *  net.minecraft.world.Container
 *  net.minecraft.world.ContainerHelper
 *  net.minecraft.world.Containers
 *  net.minecraft.world.MenuProvider
 *  net.minecraft.world.entity.player.Inventory
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.inventory.AbstractContainerMenu
 *  net.minecraft.world.inventory.ContainerData
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.crafting.RecipeHolder
 *  net.minecraft.world.item.crafting.RecipeInput
 *  net.minecraft.world.item.crafting.RecipeType
 *  net.minecraft.world.item.crafting.SingleRecipeInput
 *  net.minecraft.world.item.crafting.SmeltingRecipe
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.level.block.entity.BlockEntityType
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.properties.Property
 */
package org.millenaire.block;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.millenaire.block.FirePitBlock;
import org.millenaire.block.FirePitMenu;
import org.millenaire.block.ModBlockEntities;

public class FirePitBlockEntity
extends BlockEntity
implements MenuProvider,
Container {
    public static final int SLOT_COUNT = 7;
    public static final int INPUT_START = 0;
    public static final int INPUT_END = 3;
    public static final int FUEL_SLOT = 3;
    public static final int OUTPUT_START = 4;
    public static final int OUTPUT_END = 7;
    public static final int COOK_TIME_TOTAL = 200;
    private final NonNullList<ItemStack> items = NonNullList.withSize((int)7, (Object)ItemStack.EMPTY);
    private int[] cookTimes = new int[3];
    private int burnTime = 0;
    private int totalBurnTime = 0;
    private float[] pendingXp = new float[3];
    private int[] itemsSmelted = new int[3];
    private final ContainerData dataAccess = new ContainerData(){

        public int get(int index) {
            return switch (index) {
                case 0 -> FirePitBlockEntity.this.cookTimes[0];
                case 1 -> FirePitBlockEntity.this.cookTimes[1];
                case 2 -> FirePitBlockEntity.this.cookTimes[2];
                case 3 -> FirePitBlockEntity.this.burnTime;
                case 4 -> FirePitBlockEntity.this.totalBurnTime;
                default -> 0;
            };
        }

        public void set(int index, int value) {
            switch (index) {
                case 0: {
                    FirePitBlockEntity.this.cookTimes[0] = value;
                    break;
                }
                case 1: {
                    FirePitBlockEntity.this.cookTimes[1] = value;
                    break;
                }
                case 2: {
                    FirePitBlockEntity.this.cookTimes[2] = value;
                    break;
                }
                case 3: {
                    FirePitBlockEntity.this.burnTime = value;
                    break;
                }
                case 4: {
                    FirePitBlockEntity.this.totalBurnTime = value;
                }
            }
        }

        public int getCount() {
            return 5;
        }
    };

    public FirePitBlockEntity(BlockPos pos, BlockState state) {
        super((BlockEntityType)ModBlockEntities.FIRE_PIT.get(), pos, state);
    }

    public static boolean isFirePitBurnable(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        boolean isFood = stack.getFoodProperties(null) != null;
        return isFood;
    }

    public static boolean isFirePitBurnable(ItemStack stack, Level level) {
        if (stack.isEmpty()) {
            return false;
        }
        boolean isFood = stack.getFoodProperties(null) != null;
        Optional recipeHolder = level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, (RecipeInput)new SingleRecipeInput(stack), level);
        if (recipeHolder.isEmpty()) {
            return false;
        }
        ItemStack result = ((SmeltingRecipe)((RecipeHolder)recipeHolder.get()).value()).getResultItem((HolderLookup.Provider)level.registryAccess());
        if (result.isEmpty()) {
            return false;
        }
        boolean resultIsFood = result.getFoodProperties(null) != null;
        return isFood || resultIsFood;
    }

    public int getContainerSize() {
        return 7;
    }

    public boolean isEmpty() {
        for (ItemStack stack : this.items) {
            if (stack.isEmpty()) continue;
            return false;
        }
        return true;
    }

    public ItemStack getItem(int slot) {
        return (ItemStack)this.items.get(slot);
    }

    public ItemStack removeItem(int slot, int amount) {
        ItemStack result = ContainerHelper.removeItem(this.items, (int)slot, (int)amount);
        if (!result.isEmpty()) {
            this.setChanged();
        }
        return result;
    }

    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(this.items, (int)slot);
    }

    public void setItem(int slot, ItemStack stack) {
        if (slot >= 4 && slot < 7 && stack.isEmpty()) {
            int lane = slot - 4;
            this.pendingXp[lane] = 0.0f;
            this.itemsSmelted[lane] = 0;
        }
        this.items.set(slot, (Object)stack);
        if (!stack.isEmpty() && stack.getCount() > stack.getMaxStackSize()) {
            stack.setCount(stack.getMaxStackSize());
        }
        this.setChanged();
    }

    public boolean stillValid(Player player) {
        return true;
    }

    public void clearContent() {
        this.items.clear();
    }

    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot >= 4 && slot < 7) {
            return false;
        }
        if (slot == 3) {
            return AbstractFurnaceBlockEntity.isFuel((ItemStack)stack);
        }
        if (slot >= 0 && slot < 3) {
            Level level = this.getLevel();
            if (level != null) {
                return FirePitBlockEntity.isFirePitBurnable(stack, level);
            }
            return FirePitBlockEntity.isFirePitBurnable(stack);
        }
        return false;
    }

    public NonNullList<ItemStack> getItems() {
        return this.items;
    }

    public ItemStack getInputItem(int lane) {
        return (ItemStack)this.items.get(0 + lane);
    }

    public ItemStack getFuelItem() {
        return (ItemStack)this.items.get(3);
    }

    public ItemStack getOutputItem(int lane) {
        return (ItemStack)this.items.get(4 + lane);
    }

    public int getBurnTime() {
        return this.burnTime;
    }

    public int getTotalBurnTime() {
        return this.totalBurnTime;
    }

    public int getCookTime(int lane) {
        return this.cookTimes[lane];
    }

    public ContainerData getDataAccess() {
        return this.dataAccess;
    }

    public float claimPendingXp(int lane, int itemsTaken) {
        if (this.itemsSmelted[lane] <= 0 || this.pendingXp[lane] <= 0.0f) {
            return 0.0f;
        }
        int taken = Math.min(itemsTaken, this.itemsSmelted[lane]);
        float fraction = (float)taken / (float)this.itemsSmelted[lane];
        float xp = this.pendingXp[lane] * fraction;
        int n = lane;
        this.pendingXp[n] = this.pendingXp[n] - xp;
        int n2 = lane;
        this.itemsSmelted[n2] = this.itemsSmelted[n2] - taken;
        if (this.itemsSmelted[lane] <= 0) {
            this.pendingXp[lane] = 0.0f;
            this.itemsSmelted[lane] = 0;
        }
        return xp;
    }

    public void discardPendingXp(int lane, int itemsRemoved) {
        if (this.itemsSmelted[lane] <= 0) {
            return;
        }
        int removed = Math.min(itemsRemoved, this.itemsSmelted[lane]);
        float fraction = (float)removed / (float)this.itemsSmelted[lane];
        int n = lane;
        this.pendingXp[n] = this.pendingXp[n] - this.pendingXp[lane] * fraction;
        int n2 = lane;
        this.itemsSmelted[n2] = this.itemsSmelted[n2] - removed;
        if (this.itemsSmelted[lane] <= 0) {
            this.pendingXp[lane] = 0.0f;
            this.itemsSmelted[lane] = 0;
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, FirePitBlockEntity blockEntity) {
        boolean wasBurning = blockEntity.burnTime > 0;
        boolean dirty = false;
        boolean renderChanged = false;
        if (wasBurning) {
            --blockEntity.burnTime;
        }
        ItemStack fuelStack = blockEntity.getFuelItem();
        for (int i = 0; i < 3; ++i) {
            ItemStack inputStack = blockEntity.getInputItem(i);
            if (!(blockEntity.burnTime <= 0 && fuelStack.isEmpty() || inputStack.isEmpty())) {
                if (blockEntity.burnTime <= 0 && blockEntity.canSmelt(i)) {
                    blockEntity.totalBurnTime = blockEntity.burnTime = blockEntity.getFuelBurnTime(fuelStack);
                    if (blockEntity.burnTime > 0) {
                        dirty = true;
                        if (!fuelStack.isEmpty()) {
                            renderChanged = true;
                            fuelStack.shrink(1);
                            if (fuelStack.isEmpty()) {
                                ItemStack containerItem = fuelStack.getCraftingRemainingItem();
                                blockEntity.setItem(3, containerItem);
                                fuelStack = blockEntity.getFuelItem();
                            }
                        }
                    }
                }
                if (blockEntity.burnTime > 0 && blockEntity.canSmelt(i)) {
                    int n = i;
                    blockEntity.cookTimes[n] = blockEntity.cookTimes[n] + 1;
                    if (blockEntity.cookTimes[i] != 200) continue;
                    blockEntity.cookTimes[i] = 0;
                    blockEntity.smeltItem(i);
                    dirty = true;
                    renderChanged = true;
                    continue;
                }
                blockEntity.cookTimes[i] = 0;
                continue;
            }
            if (blockEntity.burnTime > 0 || blockEntity.cookTimes[i] <= 0) continue;
            dirty = true;
            blockEntity.cookTimes[i] = Mth.clamp((int)(blockEntity.cookTimes[i] - 2), (int)0, (int)200);
        }
        if (wasBurning != blockEntity.burnTime > 0) {
            dirty = true;
            BlockState currentState = level.getBlockState(pos);
            if (currentState.getBlock() instanceof FirePitBlock) {
                level.setBlock(pos, (BlockState)currentState.setValue((Property)FirePitBlock.LIT, (Comparable)Boolean.valueOf(blockEntity.burnTime > 0)), 3);
            }
        }
        if (dirty) {
            blockEntity.setChangedSavingOnly();
        }
        if (renderChanged) {
            blockEntity.syncToClients();
        }
    }

    private boolean canSmelt(int lane) {
        ItemStack input = this.getInputItem(lane);
        if (input.isEmpty()) {
            return false;
        }
        Level level = this.getLevel();
        if (level == null) {
            return false;
        }
        Optional recipeHolder = level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, (RecipeInput)new SingleRecipeInput(input), level);
        if (recipeHolder.isEmpty()) {
            return false;
        }
        ItemStack result = ((SmeltingRecipe)((RecipeHolder)recipeHolder.get()).value()).getResultItem((HolderLookup.Provider)level.registryAccess());
        if (result.isEmpty()) {
            return false;
        }
        ItemStack output = this.getOutputItem(lane);
        return output.isEmpty() || ItemStack.isSameItemSameComponents((ItemStack)result, (ItemStack)output) && output.getCount() + result.getCount() <= result.getMaxStackSize();
    }

    private void smeltItem(int lane) {
        if (!this.canSmelt(lane)) {
            return;
        }
        ItemStack input = this.getInputItem(lane);
        Level level = this.getLevel();
        if (level == null) {
            return;
        }
        Optional recipeHolder = level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, (RecipeInput)new SingleRecipeInput(input), level);
        if (recipeHolder.isEmpty()) {
            return;
        }
        SmeltingRecipe recipe = (SmeltingRecipe)((RecipeHolder)recipeHolder.get()).value();
        ItemStack result = recipe.getResultItem((HolderLookup.Provider)level.registryAccess());
        ItemStack output = this.getOutputItem(lane);
        if (output.isEmpty()) {
            this.setItem(4 + lane, result.copy());
        } else {
            output.grow(result.getCount());
        }
        input.shrink(1);
        int n = lane;
        this.pendingXp[n] = this.pendingXp[n] + recipe.getExperience();
        int n2 = lane;
        this.itemsSmelted[n2] = this.itemsSmelted[n2] + result.getCount();
    }

    private int getFuelBurnTime(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        return stack.getBurnTime(RecipeType.SMELTING);
    }

    public void dropAllItems() {
        Level level = this.getLevel();
        if (level == null) {
            return;
        }
        BlockPos pos = this.getBlockPos();
        for (int i = 0; i < this.items.size(); ++i) {
            ItemStack stack = (ItemStack)this.items.get(i);
            if (stack.isEmpty()) continue;
            Containers.dropItemStack((Level)level, (double)pos.getX(), (double)pos.getY(), (double)pos.getZ(), (ItemStack)stack);
            this.items.set(i, (Object)ItemStack.EMPTY);
        }
    }

    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems((CompoundTag)tag, this.items, (HolderLookup.Provider)registries);
        tag.putInt("BurnTime", this.burnTime);
        tag.putIntArray("CookTime", this.cookTimes);
        tag.putInt("TotalBurnTime", this.totalBurnTime);
        tag.putFloat("PendingXp0", this.pendingXp[0]);
        tag.putFloat("PendingXp1", this.pendingXp[1]);
        tag.putFloat("PendingXp2", this.pendingXp[2]);
        tag.putInt("ItemsSmelted0", this.itemsSmelted[0]);
        tag.putInt("ItemsSmelted1", this.itemsSmelted[1]);
        tag.putInt("ItemsSmelted2", this.itemsSmelted[2]);
    }

    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        Collections.fill(this.items, ItemStack.EMPTY);
        ContainerHelper.loadAllItems((CompoundTag)tag, this.items, (HolderLookup.Provider)registries);
        this.burnTime = tag.getInt("BurnTime");
        this.cookTimes = Arrays.copyOf(tag.getIntArray("CookTime"), 3);
        this.totalBurnTime = tag.getInt("TotalBurnTime");
        this.pendingXp[0] = tag.getFloat("PendingXp0");
        this.pendingXp[1] = tag.getFloat("PendingXp1");
        this.pendingXp[2] = tag.getFloat("PendingXp2");
        this.itemsSmelted[0] = tag.getInt("ItemsSmelted0");
        this.itemsSmelted[1] = tag.getInt("ItemsSmelted1");
        this.itemsSmelted[2] = tag.getInt("ItemsSmelted2");
    }

    @Nullable
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create((BlockEntity)this);
    }

    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        this.saveAdditional(tag, registries);
        return tag;
    }

    private void syncToClients() {
        if (this.level != null && !this.level.isClientSide) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 2);
        }
    }

    public void setChanged() {
        super.setChanged();
        this.syncToClients();
    }

    private void setChangedSavingOnly() {
        super.setChanged();
    }

    public Component getDisplayName() {
        return Component.translatable((String)"block.millenaire.fire_pit");
    }

    @Nullable
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new FirePitMenu(containerId, playerInventory, this, this.dataAccess);
    }
}

