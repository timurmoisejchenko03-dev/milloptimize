/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.tags.TagKey
 *  net.minecraft.world.Container
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.ItemLike
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.ChestBlock
 *  net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.level.block.entity.ChestBlockEntity
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.properties.ChestType
 *  net.minecraft.world.level.block.state.properties.Property
 */
package org.millenaire.building;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.Property;
import org.millenaire.block.FirePitBlockEntity;

public class BuildingInventory {
    private final List<BlockPos> chestPositions;
    private final List<BlockPos> furnacePositions;
    private final List<BlockPos> firepitPositions;
    private static final int FURNACE_RESULT_SLOT = 2;
    @Nullable
    private Map<Item, Integer> cachedContents = null;

    public BuildingInventory(List<BlockPos> chestPositions) {
        this.chestPositions = List.copyOf(chestPositions);
        this.furnacePositions = List.of();
        this.firepitPositions = List.of();
    }

    public BuildingInventory(List<BlockPos> chestPositions, List<BlockPos> furnacePositions) {
        this.chestPositions = List.copyOf(chestPositions);
        this.furnacePositions = List.copyOf(furnacePositions);
        this.firepitPositions = List.of();
    }

    public BuildingInventory(List<BlockPos> chestPositions, List<BlockPos> furnacePositions, List<BlockPos> firepitPositions) {
        this.chestPositions = List.copyOf(chestPositions);
        this.furnacePositions = List.copyOf(furnacePositions);
        this.firepitPositions = List.copyOf(firepitPositions);
    }

    public Map<Item, Integer> scanChests(Level level) {
        HashMap<Item, Integer> contents = new HashMap<Item, Integer>();
        for (Container container : this.resolveContainers(level)) {
            for (int slot = 0; slot < container.getContainerSize(); ++slot) {
                ItemStack stack = container.getItem(slot);
                if (stack.isEmpty()) continue;
                contents.merge(stack.getItem(), stack.getCount(), Integer::sum);
            }
        }
        for (BlockPos pos : this.furnacePositions) {
            AbstractFurnaceBlockEntity furnace;
            ItemStack stack;
            BlockEntity be;
            if (!level.isLoaded(pos) || !((be = level.getBlockEntity(pos)) instanceof AbstractFurnaceBlockEntity) || (stack = (furnace = (AbstractFurnaceBlockEntity)be).getItem(2)).isEmpty()) continue;
            contents.merge(stack.getItem(), stack.getCount(), Integer::sum);
        }
        for (BlockPos pos : this.firepitPositions) {
            BlockEntity be;
            if (!level.isLoaded(pos) || !((be = level.getBlockEntity(pos)) instanceof FirePitBlockEntity)) continue;
            FirePitBlockEntity firePit = (FirePitBlockEntity)be;
            for (int lane = 0; lane < 3; ++lane) {
                ItemStack stack = firePit.getOutputItem(lane);
                if (stack.isEmpty()) continue;
                contents.merge(stack.getItem(), stack.getCount(), Integer::sum);
            }
        }
        this.cachedContents = contents;
        return contents;
    }

    public int getCount(Level level, Item item) {
        Map<Item, Integer> cache = this.cachedContents;
        if (cache == null) {
            if (level instanceof ServerLevel) {
                ServerLevel serverLevel = (ServerLevel)level;
                this.scanChests((Level)serverLevel);
                cache = this.cachedContents;
            }
            if (cache == null) {
                return 0;
            }
        }
        return cache.getOrDefault(item, 0);
    }

    @Nullable
    public Map<Item, Integer> getCachedContents() {
        return this.cachedContents != null ? Collections.unmodifiableMap(this.cachedContents) : null;
    }

    public int getChestCount() {
        return this.chestPositions.size();
    }

    public List<BlockPos> getChestPositions() {
        return Collections.unmodifiableList(this.chestPositions);
    }

    public int add(Level level, Item item, int count) {
        if (count <= 0) {
            return 0;
        }
        int remaining = count;
        for (Container container : this.resolveContainers(level)) {
            if (remaining <= 0) break;
            remaining = BuildingInventory.addToContainer(container, item, remaining);
        }
        this.invalidateCache();
        return count - remaining;
    }

    public int remove(Level level, Item item, int count) {
        BlockEntity be;
        if (count <= 0) {
            return 0;
        }
        int remaining = count;
        for (Container container : this.resolveContainers(level)) {
            if (remaining <= 0) break;
            remaining = BuildingInventory.removeFromContainer(container, item, remaining);
        }
        for (BlockPos pos : this.furnacePositions) {
            AbstractFurnaceBlockEntity furnace;
            ItemStack stack;
            if (remaining <= 0) break;
            if (!level.isLoaded(pos) || !((be = level.getBlockEntity(pos)) instanceof AbstractFurnaceBlockEntity) || (stack = (furnace = (AbstractFurnaceBlockEntity)be).getItem(2)).isEmpty() || !stack.is(item)) continue;
            int toTake = Math.min(remaining, stack.getCount());
            stack.shrink(toTake);
            if (stack.isEmpty()) {
                furnace.setItem(2, ItemStack.EMPTY);
            }
            furnace.setChanged();
            remaining -= toTake;
        }
        for (BlockPos pos : this.firepitPositions) {
            if (remaining <= 0) break;
            if (!level.isLoaded(pos) || !((be = level.getBlockEntity(pos)) instanceof FirePitBlockEntity)) continue;
            FirePitBlockEntity firePit = (FirePitBlockEntity)be;
            for (int lane = 0; lane < 3 && remaining > 0; ++lane) {
                ItemStack stack = firePit.getOutputItem(lane);
                if (stack.isEmpty() || !stack.is(item)) continue;
                int toTake = Math.min(remaining, stack.getCount());
                stack.shrink(toTake);
                if (stack.isEmpty()) {
                    firePit.setItem(4 + lane, ItemStack.EMPTY);
                }
                firePit.setChanged();
                remaining -= toTake;
            }
        }
        this.invalidateCache();
        return count - remaining;
    }

    public int getCountByTag(Level level, TagKey<Item> tag) {
        Map<Item, Integer> cache = this.cachedContents;
        if (cache == null) {
            if (level instanceof ServerLevel) {
                ServerLevel serverLevel = (ServerLevel)level;
                this.scanChests((Level)serverLevel);
                cache = this.cachedContents;
            }
            if (cache == null) {
                return 0;
            }
        }
        int total = 0;
        for (Map.Entry<Item, Integer> entry : cache.entrySet()) {
            if (!new ItemStack((ItemLike)entry.getKey()).is(tag)) continue;
            total += entry.getValue().intValue();
        }
        return total;
    }

    public int getCountByTagExcluding(Level level, TagKey<Item> tag, Set<Item> excluded) {
        Map<Item, Integer> cache = this.cachedContents;
        if (cache == null) {
            if (level instanceof ServerLevel) {
                ServerLevel serverLevel = (ServerLevel)level;
                this.scanChests((Level)serverLevel);
                cache = this.cachedContents;
            }
            if (cache == null) {
                return 0;
            }
        }
        int total = 0;
        for (Map.Entry<Item, Integer> entry : cache.entrySet()) {
            if (excluded.contains(entry.getKey()) || !new ItemStack((ItemLike)entry.getKey()).is(tag)) continue;
            total += entry.getValue().intValue();
        }
        return total;
    }

    public int removeByTag(Level level, TagKey<Item> tag, int count, Set<Item> excluded) {
        if (count <= 0) {
            return 0;
        }
        int remaining = count;
        if ((remaining = this.removeByTagFromAllSources(level, tag, remaining, excluded)) > 0) {
            remaining = this.removeByTagFromAllSources(level, tag, remaining, Set.of());
        }
        this.invalidateCache();
        return count - remaining;
    }

    private int removeByTagFromAllSources(Level level, TagKey<Item> tag, int remaining, Set<Item> excluded) {
        BlockEntity be;
        for (Container container : this.resolveContainers(level)) {
            if (remaining <= 0) {
                return 0;
            }
            remaining = BuildingInventory.removeByTagFromContainer(container, tag, remaining, excluded);
        }
        for (BlockPos pos : this.furnacePositions) {
            AbstractFurnaceBlockEntity furnace;
            ItemStack stack;
            if (remaining <= 0) {
                return 0;
            }
            if (!level.isLoaded(pos) || !((be = level.getBlockEntity(pos)) instanceof AbstractFurnaceBlockEntity) || (stack = (furnace = (AbstractFurnaceBlockEntity)be).getItem(2)).isEmpty() || !stack.is(tag) || excluded.contains(stack.getItem())) continue;
            int toTake = Math.min(remaining, stack.getCount());
            stack.shrink(toTake);
            if (stack.isEmpty()) {
                furnace.setItem(2, ItemStack.EMPTY);
            }
            furnace.setChanged();
            remaining -= toTake;
        }
        for (BlockPos pos : this.firepitPositions) {
            if (remaining <= 0) {
                return 0;
            }
            if (!level.isLoaded(pos) || !((be = level.getBlockEntity(pos)) instanceof FirePitBlockEntity)) continue;
            FirePitBlockEntity firePit = (FirePitBlockEntity)be;
            for (int lane = 0; lane < 3; ++lane) {
                if (remaining <= 0) {
                    return 0;
                }
                ItemStack stack = firePit.getOutputItem(lane);
                if (stack.isEmpty() || !stack.is(tag) || excluded.contains(stack.getItem())) continue;
                int toTake = Math.min(remaining, stack.getCount());
                stack.shrink(toTake);
                if (stack.isEmpty()) {
                    firePit.setItem(4 + lane, ItemStack.EMPTY);
                }
                firePit.setChanged();
                remaining -= toTake;
            }
        }
        return remaining;
    }

    private static int removeByTagFromContainer(Container container, TagKey<Item> tag, int count, Set<Item> excluded) {
        int remaining = count;
        for (int slot = 0; slot < container.getContainerSize(); ++slot) {
            if (remaining <= 0) {
                return 0;
            }
            ItemStack stack = container.getItem(slot);
            if (stack.isEmpty() || !stack.is(tag) || excluded.contains(stack.getItem())) continue;
            int toRemove = Math.min(remaining, stack.getCount());
            stack.shrink(toRemove);
            if (stack.isEmpty()) {
                container.setItem(slot, ItemStack.EMPTY);
            }
            remaining -= toRemove;
        }
        return remaining;
    }

    public boolean addStack(Level level, ItemStack stack) {
        int slot;
        if (stack.isEmpty()) {
            return false;
        }
        ItemStack toPlace = stack.copy();
        int initialCount = toPlace.getCount();
        for (Container container : this.resolveContainers(level)) {
            for (slot = 0; slot < container.getContainerSize(); ++slot) {
                int canAdd;
                ItemStack existing = container.getItem(slot);
                if (existing.isEmpty() || !ItemStack.isSameItemSameComponents((ItemStack)existing, (ItemStack)toPlace) || (canAdd = Math.min(toPlace.getCount(), existing.getMaxStackSize() - existing.getCount())) <= 0) continue;
                existing.grow(canAdd);
                container.setChanged();
                toPlace.shrink(canAdd);
                if (!toPlace.isEmpty()) continue;
                this.invalidateCache();
                return true;
            }
        }
        for (Container container : this.resolveContainers(level)) {
            for (slot = 0; slot < container.getContainerSize(); ++slot) {
                if (!container.getItem(slot).isEmpty()) continue;
                container.setItem(slot, toPlace);
                container.setChanged();
                this.invalidateCache();
                return true;
            }
        }
        if (toPlace.getCount() < initialCount) {
            this.invalidateCache();
            return true;
        }
        return false;
    }

    public void invalidateCache() {
        this.cachedContents = null;
    }

    private List<Container> resolveContainers(Level level) {
        ArrayList<Container> containers = new ArrayList<Container>();
        HashSet<BlockPos> processedPositions = new HashSet<BlockPos>();
        for (BlockPos pos : this.chestPositions) {
            ChestBlock chestBlock;
            Container container;
            BlockState state;
            Block block;
            BlockEntity be;
            if (processedPositions.contains(pos) || !level.isLoaded(pos) || !((be = level.getBlockEntity(pos)) instanceof ChestBlockEntity) || !((block = (state = level.getBlockState(pos)).getBlock()) instanceof ChestBlock) || (container = ChestBlock.getContainer((ChestBlock)(chestBlock = (ChestBlock)block), (BlockState)state, (Level)level, (BlockPos)pos, (boolean)true)) == null) continue;
            processedPositions.add(pos);
            ChestType chestType = (ChestType)state.getValue((Property)ChestBlock.TYPE);
            if (chestType != ChestType.SINGLE) {
                BlockPos neighborPos = pos.relative(ChestBlock.getConnectedDirection((BlockState)state));
                processedPositions.add(neighborPos);
            }
            containers.add(container);
        }
        return containers;
    }

    private static int addToContainer(Container container, Item item, int count) {
        ItemStack stack;
        int slot;
        int remaining = count;
        int maxStackSize = item.getDefaultMaxStackSize();
        for (slot = 0; slot < container.getContainerSize(); ++slot) {
            int canAdd;
            if (remaining <= 0) {
                return 0;
            }
            stack = container.getItem(slot);
            if (stack.isEmpty() || !stack.is(item) || (canAdd = maxStackSize - stack.getCount()) <= 0) continue;
            int toAdd = Math.min(remaining, canAdd);
            stack.grow(toAdd);
            container.setItem(slot, stack);
            remaining -= toAdd;
        }
        for (slot = 0; slot < container.getContainerSize(); ++slot) {
            if (remaining <= 0) {
                return 0;
            }
            stack = container.getItem(slot);
            if (!stack.isEmpty()) continue;
            int toAdd = Math.min(remaining, maxStackSize);
            container.setItem(slot, new ItemStack((ItemLike)item, toAdd));
            remaining -= toAdd;
        }
        return remaining;
    }

    private static int removeFromContainer(Container container, Item item, int count) {
        int remaining = count;
        for (int slot = 0; slot < container.getContainerSize(); ++slot) {
            if (remaining <= 0) {
                return 0;
            }
            ItemStack stack = container.getItem(slot);
            if (stack.isEmpty() || !stack.is(item)) continue;
            int toRemove = Math.min(remaining, stack.getCount());
            stack.shrink(toRemove);
            if (stack.isEmpty()) {
                container.setItem(slot, ItemStack.EMPTY);
            }
            remaining -= toRemove;
        }
        return remaining;
    }
}

