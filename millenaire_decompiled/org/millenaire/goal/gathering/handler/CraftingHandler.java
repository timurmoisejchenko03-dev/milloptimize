/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.level.Level
 *  org.slf4j.Logger
 */
package org.millenaire.goal.gathering.handler;

import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import org.millenaire.building.BuildingInstance;
import org.millenaire.building.BuildingInventory;
import org.millenaire.entity.VillagerInventory;
import org.millenaire.goal.GoalContext;
import org.millenaire.goal.gathering.GatheringTarget;
import org.millenaire.goal.gathering.GatheringType;
import org.millenaire.goal.gathering.handler.AbstractGatheringHandler;
import org.slf4j.Logger;

public class CraftingHandler
extends AbstractGatheringHandler {
    private static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public String id() {
        return "crafting";
    }

    @Override
    public List<String> validate(GatheringType type) {
        ArrayList<String> errors = new ArrayList<String>();
        errors.addAll(this.validateItemList(type, "inputs", false));
        errors.addAll(this.validateItemList(type, "outputs", false));
        return errors;
    }

    @Override
    public boolean canStart(GoalContext ctx, GatheringType type) {
        Map<Item, Integer> inputs = this.parseItemList(type.handlerParams(), "inputs");
        boolean freeRecipe = inputs.isEmpty();
        ServerLevel level = ctx.level();
        VillagerInventory villagerInv = ctx.villager().getInventory();
        for (BuildingInstance building : this.resolveTargetBuildings(ctx, type)) {
            if (!this.hasCraftingPos(building) || !freeRecipe && !this.hasAllInputsCombined(building, level, villagerInv, inputs)) continue;
            return true;
        }
        return false;
    }

    @Override
    @Nullable
    public GatheringTarget findTarget(GoalContext ctx, GatheringType type, @Nullable GatheringTarget lastTarget) {
        Map<Item, Integer> inputs = this.parseItemList(type.handlerParams(), "inputs");
        boolean freeRecipe = inputs.isEmpty();
        ServerLevel level = ctx.level();
        BlockPos reference = lastTarget != null ? lastTarget.navigationPos() : ctx.villager().blockPosition();
        VillagerInventory villagerInv = ctx.villager().getInventory();
        ArrayList<BlockPos> candidates = new ArrayList<BlockPos>();
        for (BuildingInstance building : this.resolveTargetBuildings(ctx, type)) {
            BlockPos craftingPos = this.getCraftingPos(building);
            if (craftingPos == null || !freeRecipe && !this.hasAllInputsCombined(building, level, villagerInv, inputs)) continue;
            candidates.add(craftingPos);
        }
        BlockPos bestPos = CraftingHandler.findClosestBlock(candidates, reference, lastTarget, type.batchRadius());
        return bestPos != null ? new GatheringTarget.BlockTarget(bestPos) : null;
    }

    @Override
    public boolean performAction(GoalContext ctx, GatheringType type, GatheringTarget target) {
        Map<Item, Integer> inputs = this.parseItemList(type.handlerParams(), "inputs");
        Map<Item, Integer> outputs = this.parseItemList(type.handlerParams(), "outputs");
        BuildingInstance craftingBuilding = null;
        for (BuildingInstance building : this.resolveTargetBuildings(ctx, type)) {
            BlockPos craftingPos = this.getCraftingPos(building);
            if (craftingPos == null || !craftingPos.equals((Object)target.navigationPos())) continue;
            craftingBuilding = building;
            break;
        }
        if (craftingBuilding == null) {
            return true;
        }
        BuildingInventory inv = craftingBuilding.getInventory();
        if (inv == null) {
            return true;
        }
        ServerLevel level = ctx.level();
        VillagerInventory villagerInv = ctx.villager().getInventory();
        if (!inputs.isEmpty() && !this.hasAllInputsCombined(craftingBuilding, level, villagerInv, inputs)) {
            return true;
        }
        if (!this.hasSpaceForOutputs(inv, level, outputs)) {
            return true;
        }
        for (Map.Entry<Item, Integer> entry : inputs.entrySet()) {
            int takenFromVillager;
            int needed = entry.getValue();
            int remaining = needed - (takenFromVillager = villagerInv.remove(entry.getKey(), needed));
            if (remaining <= 0) continue;
            inv.remove((Level)level, entry.getKey(), remaining);
        }
        for (Map.Entry<Item, Integer> entry : outputs.entrySet()) {
            inv.add((Level)level, entry.getKey(), entry.getValue());
        }
        LOGGER.debug("Crafting performed at building {} at {}", (Object)craftingBuilding.getPlanId(), (Object)target.navigationPos());
        return true;
    }

    @Override
    @Nullable
    public BuildingInstance resolveBuildingLimitTarget(GoalContext ctx, GatheringType type) {
        List<BuildingInstance> buildings = this.resolveTargetBuildings(ctx, type);
        return buildings.isEmpty() ? null : buildings.getFirst();
    }

    private boolean hasCraftingPos(BuildingInstance building) {
        return this.getCraftingPos(building) != null;
    }

    @Nullable
    private BlockPos getCraftingPos(BuildingInstance building) {
        BlockPos pos = building.getFirstPointPos("craftingPos");
        if (pos != null) {
            return pos;
        }
        pos = building.getFirstPointPos("sellingPos");
        if (pos != null) {
            return pos;
        }
        return building.getFirstPointPos("sleepingPos");
    }

    private boolean hasSpaceForOutputs(BuildingInventory inv, ServerLevel level, Map<Item, Integer> outputs) {
        int totalOutputCount;
        Map<Item, Integer> cache = inv.getCachedContents();
        if (cache == null) {
            inv.scanChests((Level)level);
            cache = inv.getCachedContents();
        }
        if (cache == null) {
            return false;
        }
        int totalItems = cache.values().stream().mapToInt(Integer::intValue).sum();
        int totalSlots = inv.getChestCount() * 27;
        int freeSpace = totalSlots * 64 - totalItems;
        return freeSpace >= (totalOutputCount = outputs.values().stream().mapToInt(Integer::intValue).sum());
    }

    private boolean hasAllInputsCombined(BuildingInstance building, ServerLevel level, VillagerInventory villagerInv, Map<Item, Integer> inputs) {
        BuildingInventory buildingInv = building.getInventory();
        if (buildingInv == null) {
            return false;
        }
        for (Map.Entry<Item, Integer> entry : inputs.entrySet()) {
            int available = villagerInv.getCount(entry.getKey()) + buildingInv.getCount((Level)level, entry.getKey());
            if (available >= entry.getValue()) continue;
            return false;
        }
        return true;
    }
}

