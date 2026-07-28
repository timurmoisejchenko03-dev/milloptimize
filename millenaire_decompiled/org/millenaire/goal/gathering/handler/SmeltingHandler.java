/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonObject
 *  com.mojang.logging.LogUtils
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Holder
 *  net.minecraft.core.HolderLookup$Provider
 *  net.minecraft.core.Vec3i
 *  net.minecraft.core.registries.BuiltInRegistries
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.tags.ItemTags
 *  net.minecraft.util.GsonHelper
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.Items
 *  net.minecraft.world.item.crafting.CraftingInput
 *  net.minecraft.world.item.crafting.CraftingRecipe
 *  net.minecraft.world.item.crafting.RecipeHolder
 *  net.minecraft.world.item.crafting.RecipeInput
 *  net.minecraft.world.item.crafting.RecipeType
 *  net.minecraft.world.level.ItemLike
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  org.slf4j.Logger
 */
package org.millenaire.goal.gathering.handler;

import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.millenaire.block.FirePitBlockEntity;
import org.millenaire.building.BuildingId;
import org.millenaire.building.BuildingInstance;
import org.millenaire.building.BuildingInventory;
import org.millenaire.goal.GoalContext;
import org.millenaire.goal.gathering.GatheringTarget;
import org.millenaire.goal.gathering.GatheringType;
import org.millenaire.goal.gathering.handler.AbstractGatheringHandler;
import org.slf4j.Logger;

public class SmeltingHandler
extends AbstractGatheringHandler {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int DEFAULT_MINIMUM_TO_COOK = 3;
    private static final int MINIMUM_FUEL = 4;
    private static final int MAX_FURNACE_INPUT = 32;
    private static final int MAX_FUEL_PER_FURNACE = 64;
    private static final Map<Item, ItemStack> LOG_TO_PLANKS = new ConcurrentHashMap<Item, ItemStack>();
    private static final Map<Item, Item> PLANK_TO_LOG = new ConcurrentHashMap<Item, Item>();
    @Nullable
    private static List<Item> fuelItemsCache;

    private int getMinimumToCook(GatheringType type) {
        return GsonHelper.getAsInt((JsonObject)type.handlerParams(), (String)"minimumToCook", (int)3);
    }

    @Override
    public String id() {
        return "smelting";
    }

    @Override
    public void onClear() {
        fuelItemsCache = null;
    }

    @Override
    public List<String> validate(GatheringType type) {
        return this.validateItemList(type, "inputs", true);
    }

    @Override
    public boolean supportsRemoteAction() {
        return true;
    }

    @Override
    public boolean canStart(GoalContext ctx, GatheringType type) {
        Map<Item, Integer> inputs = this.parseItemList(type.handlerParams(), "inputs");
        if (inputs.isEmpty()) {
            return false;
        }
        Item inputItem = inputs.keySet().iterator().next();
        boolean firepitBurnable = FirePitBlockEntity.isFirePitBurnable(new ItemStack((ItemLike)inputItem), (Level)ctx.level());
        for (BuildingInstance building : this.resolveTargetBuildings(ctx, type)) {
            BlockEntity be;
            if (building.getInventory() == null) continue;
            for (BlockPos furnacePos : building.getFurnacePositions()) {
                AbstractFurnaceBlockEntity furnace;
                be = ctx.level().getBlockEntity(furnacePos);
                if (!(be instanceof AbstractFurnaceBlockEntity) || !this.furnaceNeedsAttention(ctx, type, building, furnace = (AbstractFurnaceBlockEntity)be, inputs)) continue;
                return true;
            }
            if (!firepitBurnable) continue;
            for (BlockPos fpPos : building.getFirePitPositions()) {
                FirePitBlockEntity firePit;
                be = ctx.level().getBlockEntity(fpPos);
                if (!(be instanceof FirePitBlockEntity) || !this.firePitNeedsAttention(ctx, type, building, firePit = (FirePitBlockEntity)be, inputs)) continue;
                return true;
            }
        }
        return false;
    }

    @Override
    @Nullable
    public GatheringTarget findTarget(GoalContext ctx, GatheringType type, @Nullable GatheringTarget lastTarget) {
        Map<Item, Integer> inputs = this.parseItemList(type.handlerParams(), "inputs");
        if (inputs.isEmpty()) {
            return null;
        }
        BlockPos reference = lastTarget != null ? lastTarget.navigationPos() : ctx.villager().blockPosition();
        BlockPos bestPos = null;
        double bestDistSq = Double.MAX_VALUE;
        Item inputItem = inputs.keySet().iterator().next();
        boolean firepitBurnable = FirePitBlockEntity.isFirePitBurnable(new ItemStack((ItemLike)inputItem), (Level)ctx.level());
        for (BuildingInstance building : this.resolveTargetBuildings(ctx, type)) {
            double distSq;
            BlockEntity be;
            for (BlockPos furnacePos : building.getFurnacePositions()) {
                AbstractFurnaceBlockEntity furnace;
                be = ctx.level().getBlockEntity(furnacePos);
                if (!(be instanceof AbstractFurnaceBlockEntity) || !this.furnaceNeedsAttention(ctx, type, building, furnace = (AbstractFurnaceBlockEntity)be, inputs) || !((distSq = reference.distSqr((Vec3i)furnacePos)) < bestDistSq)) continue;
                bestDistSq = distSq;
                bestPos = furnacePos;
            }
            if (!firepitBurnable) continue;
            for (BlockPos fpPos : building.getFirePitPositions()) {
                FirePitBlockEntity firePit;
                be = ctx.level().getBlockEntity(fpPos);
                if (!(be instanceof FirePitBlockEntity) || !this.firePitNeedsAttention(ctx, type, building, firePit = (FirePitBlockEntity)be, inputs) || !((distSq = reference.distSqr((Vec3i)fpPos)) < bestDistSq)) continue;
                bestDistSq = distSq;
                bestPos = fpPos;
            }
        }
        return bestPos != null ? new GatheringTarget.BlockTarget(bestPos) : null;
    }

    @Override
    public boolean performAction(GoalContext ctx, GatheringType type, GatheringTarget target) {
        boolean firepitBurnable;
        Map<Item, Integer> inputs = this.parseItemList(type.handlerParams(), "inputs");
        int minimumToCook = this.getMinimumToCook(type);
        BlockPos pos = target.navigationPos();
        BuildingInstance ownerBuilding = this.findBuildingOwning(ctx, type, pos);
        if (ownerBuilding == null) {
            return true;
        }
        BuildingInventory inv = ownerBuilding.getInventory();
        if (inv == null) {
            return true;
        }
        ServerLevel level = ctx.level();
        for (BlockPos furnacePos : ownerBuilding.getFurnacePositions()) {
            BlockEntity be = level.getBlockEntity(furnacePos);
            if (!(be instanceof AbstractFurnaceBlockEntity)) continue;
            AbstractFurnaceBlockEntity furnace = (AbstractFurnaceBlockEntity)be;
            this.performFurnaceAction(ctx, ownerBuilding, furnace, furnacePos, inputs, minimumToCook, level);
        }
        Item inputItem = inputs.isEmpty() ? null : inputs.keySet().iterator().next();
        boolean bl = firepitBurnable = inputItem != null && FirePitBlockEntity.isFirePitBurnable(new ItemStack((ItemLike)inputItem), (Level)level);
        if (firepitBurnable) {
            for (BlockPos fpPos : ownerBuilding.getFirePitPositions()) {
                BlockEntity be = level.getBlockEntity(fpPos);
                if (!(be instanceof FirePitBlockEntity)) continue;
                FirePitBlockEntity firePit = (FirePitBlockEntity)be;
                this.performActionFirePit(ctx, ownerBuilding, inputs, minimumToCook, fpPos, firePit);
            }
        }
        return true;
    }

    private void performFurnaceAction(GoalContext ctx, BuildingInstance ownerBuilding, AbstractFurnaceBlockEntity furnace, BlockPos pos, Map<Item, Integer> inputs, int minimumToCook, ServerLevel level) {
        BuildingInventory inv = ownerBuilding.getInventory();
        if (inv == null) {
            return;
        }
        ItemStack outputSlot = furnace.getItem(2);
        if (!outputSlot.isEmpty()) {
            inv.add((Level)level, outputSlot.getItem(), outputSlot.getCount());
            furnace.setItem(2, ItemStack.EMPTY);
            furnace.setChanged();
            LOGGER.debug("Retrieved {} {} from furnace at {}", new Object[]{outputSlot.getCount(), outputSlot.getItem(), pos});
        }
        if (!inputs.isEmpty()) {
            Item inputItem = inputs.keySet().iterator().next();
            ItemStack inputSlot = furnace.getItem(0);
            int totalAvailable = this.countInputAvailable(ctx, ownerBuilding, inputs);
            if (totalAvailable >= minimumToCook) {
                if (inputSlot.isEmpty()) {
                    int toPlace = Math.min(64, totalAvailable);
                    furnace.setItem(0, new ItemStack((ItemLike)inputItem, toPlace));
                    this.removeInputFromSources(ctx, ownerBuilding, inputItem, toPlace, level);
                    furnace.setChanged();
                } else if (inputSlot.is(inputItem) && inputSlot.getCount() < 64 && totalAvailable > 0) {
                    int toAdd = Math.min(64 - inputSlot.getCount(), totalAvailable);
                    inputSlot.grow(toAdd);
                    furnace.setItem(0, inputSlot);
                    this.removeInputFromSources(ctx, ownerBuilding, inputItem, toAdd, level);
                    furnace.setChanged();
                }
            }
        }
        this.addFuelIfNeeded(ctx, ownerBuilding, furnace, level);
    }

    private void performActionFirePit(GoalContext ctx, BuildingInstance ownerBuilding, Map<Item, Integer> inputs, int minimumToCook, BlockPos pos, FirePitBlockEntity firePit) {
        int lane;
        Item inputItem;
        BuildingInventory inv = ownerBuilding.getInventory();
        if (inv == null) {
            return;
        }
        ServerLevel level = ctx.level();
        Item item = inputItem = inputs.isEmpty() ? null : inputs.keySet().iterator().next();
        if (inputItem != null) {
            for (lane = 0; lane < 3; ++lane) {
                int nb;
                ItemStack laneInput = firePit.getInputItem(lane);
                int countGoods = this.countInputAvailable(ctx, ownerBuilding, inputs);
                if (laneInput.isEmpty() && countGoods >= minimumToCook) {
                    nb = Math.min(64, countGoods);
                    firePit.setItem(0 + lane, new ItemStack((ItemLike)inputItem, nb));
                    this.removeInputFromSources(ctx, ownerBuilding, inputItem, nb, level);
                    continue;
                }
                if (laneInput.isEmpty() || !laneInput.is(inputItem) || laneInput.getCount() >= 64 || countGoods <= 0) continue;
                nb = Math.min(64 - laneInput.getCount(), countGoods);
                laneInput.grow(nb);
                firePit.setItem(0 + lane, laneInput);
                this.removeInputFromSources(ctx, ownerBuilding, inputItem, nb, level);
            }
        }
        for (lane = 0; lane < 3; ++lane) {
            ItemStack laneOutput = firePit.getOutputItem(lane);
            if (laneOutput.isEmpty()) continue;
            inv.add((Level)level, laneOutput.getItem(), laneOutput.getCount());
            firePit.setItem(4 + lane, ItemStack.EMPTY);
            LOGGER.debug("Retrieved {} {} from fire pit lane {} at {}", new Object[]{laneOutput.getCount(), laneOutput.getItem(), lane, pos});
        }
        this.addFuelToFirePitIfNeeded(ctx, ownerBuilding, firePit, level);
        firePit.setChanged();
    }

    @Override
    @Nullable
    public BuildingInstance resolveBuildingLimitTarget(GoalContext ctx, GatheringType type) {
        List<BuildingInstance> buildings = this.resolveTargetBuildings(ctx, type);
        return buildings.isEmpty() ? null : buildings.getFirst();
    }

    private int countInputAvailable(GoalContext ctx, BuildingInstance smeltingBuilding, Map<Item, Integer> inputs) {
        BuildingInventory homeInv;
        int total = 0;
        ServerLevel level = ctx.level();
        BuildingInventory inv = smeltingBuilding.getInventory();
        if (inv != null) {
            for (Item item : inputs.keySet()) {
                total += inv.getCount((Level)level, item);
            }
        }
        for (Item item : inputs.keySet()) {
            total += ctx.villager().getInventory().getCount(item);
        }
        BuildingInstance home = this.getVillagerHome(ctx);
        if (home != null && home != smeltingBuilding && (homeInv = home.getInventory()) != null) {
            for (Item item : inputs.keySet()) {
                total += homeInv.getCount((Level)level, item);
            }
        }
        return total;
    }

    private int removeInputFromSources(GoalContext ctx, BuildingInstance smeltingBuilding, Item inputItem, int amount, ServerLevel level) {
        BuildingInventory homeInv;
        BuildingInstance home;
        int remaining = amount;
        BuildingInventory inv = smeltingBuilding.getInventory();
        if (inv != null && remaining > 0) {
            remaining -= inv.remove((Level)level, inputItem, remaining);
        }
        if (remaining > 0) {
            int removed = Math.min(remaining, ctx.villager().getInventory().getCount(inputItem));
            ctx.villager().getInventory().remove(inputItem, removed);
            remaining -= removed;
        }
        if (remaining > 0 && (home = this.getVillagerHome(ctx)) != null && home != smeltingBuilding && (homeInv = home.getInventory()) != null) {
            remaining -= homeInv.remove((Level)level, inputItem, remaining);
        }
        return amount - remaining;
    }

    private boolean furnaceNeedsAttention(GoalContext ctx, GatheringType type, BuildingInstance building, AbstractFurnaceBlockEntity furnace, Map<Item, Integer> inputs) {
        ItemStack fuelSlot;
        BuildingInventory inv = building.getInventory();
        if (inv == null) {
            return false;
        }
        int minimumToCook = this.getMinimumToCook(type);
        Item inputItem = inputs.isEmpty() ? null : inputs.keySet().iterator().next();
        ItemStack outputSlot = furnace.getItem(2);
        if (!outputSlot.isEmpty() && outputSlot.getCount() >= minimumToCook) {
            return true;
        }
        if (inputItem != null) {
            int totalAvailable = this.countInputAvailable(ctx, building, Map.of(inputItem, 1));
            ItemStack inputSlot = furnace.getItem(0);
            if (totalAvailable >= minimumToCook && (inputSlot.isEmpty() || inputSlot.is(inputItem) && inputSlot.getCount() < 32)) {
                return true;
            }
        }
        ItemStack inputSlot = furnace.getItem(0);
        return inputItem != null && !inputSlot.isEmpty() && inputSlot.is(inputItem) && ((fuelSlot = furnace.getItem(1)).isEmpty() || fuelSlot.getCount() < 64) && this.countFuelAvailable(ctx, building) >= 4;
    }

    private int countFuelAvailable(GoalContext ctx, BuildingInstance building) {
        int count = 0;
        for (Item fuel : SmeltingHandler.getAllFuelItems()) {
            count += this.countFuelInSource(ctx, building, fuel);
        }
        return count;
    }

    private int countFuelInSource(GoalContext ctx, BuildingInstance smeltingBuilding, Item fuel) {
        BuildingInventory homeInv;
        int count = 0;
        ServerLevel level = ctx.level();
        BuildingInventory inv = smeltingBuilding.getInventory();
        if (inv != null) {
            count += inv.getCount((Level)level, fuel);
        }
        count += ctx.villager().getInventory().getCount(fuel);
        BuildingInstance home = this.getVillagerHome(ctx);
        if (home != null && home != smeltingBuilding && (homeInv = home.getInventory()) != null) {
            count += homeInv.getCount((Level)level, fuel);
        }
        return count;
    }

    private static boolean isLog(Item item) {
        return new ItemStack((ItemLike)item).is(ItemTags.LOGS);
    }

    @Nullable
    private static ItemStack resolveLogPlanks(ServerLevel level, Item log) {
        ItemStack cached = LOG_TO_PLANKS.get((Object)log);
        if (cached != null) {
            return cached;
        }
        CraftingInput input = CraftingInput.of((int)1, (int)1, List.of(new ItemStack((ItemLike)log)));
        RecipeHolder holder = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, (RecipeInput)input, (Level)level).orElse(null);
        if (holder == null) {
            return null;
        }
        ItemStack result = ((CraftingRecipe)holder.value()).getResultItem((HolderLookup.Provider)level.registryAccess());
        if (result.isEmpty() || !result.is(ItemTags.PLANKS)) {
            return null;
        }
        ItemStack planks = result.copy();
        LOG_TO_PLANKS.put(log, planks);
        PLANK_TO_LOG.put(planks.getItem(), log);
        return planks;
    }

    @Nullable
    private FuelOp planFueling(GoalContext ctx, BuildingInstance building, ItemStack currentFuel, ServerLevel level) {
        int ratio;
        Item placedItem;
        Item sourceItem;
        int current = currentFuel.getCount();
        int capacity = 64 - current;
        if (capacity <= 0) {
            return null;
        }
        if (currentFuel.isEmpty()) {
            ItemStack planks;
            Item best = this.findBestFuel(ctx, building);
            if (best == null) {
                return null;
            }
            ItemStack itemStack = planks = SmeltingHandler.isLog(best) ? SmeltingHandler.resolveLogPlanks(level, best) : null;
            if (planks != null) {
                sourceItem = best;
                placedItem = planks.getItem();
                ratio = planks.getCount();
            } else {
                sourceItem = best;
                placedItem = best;
                ratio = 1;
            }
        } else {
            placedItem = currentFuel.getItem();
            Item log = PLANK_TO_LOG.get((Object)placedItem);
            if (log != null && this.countFuelInSource(ctx, building, log) > 0) {
                sourceItem = log;
                ratio = LOG_TO_PLANKS.get((Object)log).getCount();
            } else {
                sourceItem = placedItem;
                ratio = 1;
            }
        }
        int availableSource = this.countFuelInSource(ctx, building, sourceItem);
        if (availableSource <= 0) {
            return null;
        }
        int placeCount = Math.min(capacity, availableSource * ratio);
        int sourceConsumed = placeCount / ratio;
        if (sourceConsumed <= 0) {
            return null;
        }
        placeCount = sourceConsumed * ratio;
        return new FuelOp(new ItemStack((ItemLike)placedItem, current + placeCount), sourceItem, sourceConsumed);
    }

    private void addFuelIfNeeded(GoalContext ctx, BuildingInstance building, AbstractFurnaceBlockEntity furnace, ServerLevel level) {
        FuelOp op = this.planFueling(ctx, building, furnace.getItem(1), level);
        if (op == null) {
            return;
        }
        furnace.setItem(1, op.slotStack());
        this.removeFuelFromSources(ctx, building, op.sourceItem(), op.sourceConsumed(), level);
        furnace.setChanged();
    }

    private void removeFuelFromSources(GoalContext ctx, BuildingInstance smeltingBuilding, Item fuel, int amount, ServerLevel level) {
        BuildingInventory homeInv;
        BuildingInstance home;
        int remaining = amount;
        BuildingInventory inv = smeltingBuilding.getInventory();
        if (inv != null && remaining > 0) {
            remaining -= inv.remove((Level)level, fuel, remaining);
        }
        if (remaining > 0) {
            int removed = Math.min(remaining, ctx.villager().getInventory().getCount(fuel));
            ctx.villager().getInventory().remove(fuel, removed);
            remaining -= removed;
        }
        if (remaining > 0 && (home = this.getVillagerHome(ctx)) != null && home != smeltingBuilding && (homeInv = home.getInventory()) != null) {
            homeInv.remove((Level)level, fuel, remaining);
        }
    }

    private static List<Item> getAllFuelItems() {
        if (fuelItemsCache != null) {
            return fuelItemsCache;
        }
        ArrayList<Item> list = new ArrayList<Item>();
        list.add(Items.COAL);
        list.add(Items.CHARCOAL);
        for (Holder holder : BuiltInRegistries.ITEM.getTagOrEmpty(ItemTags.PLANKS)) {
            list.add((Item)holder.value());
        }
        for (Holder holder : BuiltInRegistries.ITEM.getTagOrEmpty(ItemTags.LOGS)) {
            list.add((Item)holder.value());
        }
        fuelItemsCache = List.copyOf(list);
        return fuelItemsCache;
    }

    @Nullable
    private Item findBestFuel(GoalContext ctx, BuildingInstance building) {
        Item best = null;
        int bestCount = 0;
        for (Item fuel : SmeltingHandler.getAllFuelItems()) {
            int count = this.countFuelInSource(ctx, building, fuel);
            if (count <= bestCount) continue;
            bestCount = count;
            best = fuel;
        }
        return bestCount >= 4 ? best : null;
    }

    @Nullable
    private BuildingInstance findBuildingOwning(GoalContext ctx, GatheringType type, BlockPos furnacePos) {
        for (BuildingInstance building : this.resolveTargetBuildings(ctx, type)) {
            for (BlockPos pos : building.getFurnacePositions()) {
                if (!pos.equals((Object)furnacePos)) continue;
                return building;
            }
            for (BlockPos pos : building.getFirePitPositions()) {
                if (!pos.equals((Object)furnacePos)) continue;
                return building;
            }
        }
        return null;
    }

    @Nullable
    private BuildingInstance getVillagerHome(GoalContext ctx) {
        BuildingId homeId = ctx.villager().getHomeBuilding();
        if (homeId == null) {
            return null;
        }
        return ctx.village().getBuilding(homeId);
    }

    private boolean firePitNeedsAttention(GoalContext ctx, GatheringType type, BuildingInstance building, FirePitBlockEntity firePit, Map<Item, Integer> inputs) {
        int minimumToCook = this.getMinimumToCook(type);
        Item inputItem = inputs.isEmpty() ? null : inputs.keySet().iterator().next();
        for (int lane = 0; lane < 3; ++lane) {
            ItemStack laneOutput = firePit.getOutputItem(lane);
            if (!laneOutput.isEmpty() && laneOutput.getCount() >= minimumToCook) {
                return true;
            }
            if (inputItem == null) continue;
            int totalAvailable = this.countInputAvailable(ctx, building, Map.of(inputItem, 1));
            ItemStack laneInput = firePit.getInputItem(lane);
            if (totalAvailable < minimumToCook || !laneInput.isEmpty() && (!laneInput.is(inputItem) || laneInput.getCount() >= 32)) continue;
            return true;
        }
        return this.hasFuelNeedForFirePit(firePit, ctx, building);
    }

    private boolean hasFuelNeedForFirePit(FirePitBlockEntity firePit, GoalContext ctx, BuildingInstance building) {
        boolean hasInput = false;
        for (int lane = 0; lane < 3; ++lane) {
            if (firePit.getInputItem(lane).isEmpty()) continue;
            hasInput = true;
            break;
        }
        if (!hasInput) {
            return false;
        }
        ItemStack fuelSlot = firePit.getFuelItem();
        if (fuelSlot.isEmpty() && this.countFuelAvailable(ctx, building) > 4) {
            return true;
        }
        return !fuelSlot.isEmpty() && fuelSlot.getCount() < 64 && this.countFuelAvailable(ctx, building) >= 4;
    }

    private void addFuelToFirePitIfNeeded(GoalContext ctx, BuildingInstance building, FirePitBlockEntity firePit, ServerLevel level) {
        FuelOp op = this.planFueling(ctx, building, firePit.getFuelItem(), level);
        if (op == null) {
            return;
        }
        firePit.setItem(3, op.slotStack());
        this.removeFuelFromSources(ctx, building, op.sourceItem(), op.sourceConsumed(), level);
    }

    private record FuelOp(ItemStack slotStack, Item sourceItem, int sourceConsumed) {
    }
}

