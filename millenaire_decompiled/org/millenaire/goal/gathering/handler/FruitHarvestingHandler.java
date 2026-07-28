/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonObject
 *  com.mojang.logging.LogUtils
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.util.GsonHelper
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.properties.IntegerProperty
 *  net.minecraft.world.level.block.state.properties.Property
 *  org.slf4j.Logger
 */
package org.millenaire.goal.gathering.handler;

import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import org.millenaire.building.BuildingInstance;
import org.millenaire.building.SpecialPoint;
import org.millenaire.goal.GoalContext;
import org.millenaire.goal.gathering.GatheringTarget;
import org.millenaire.goal.gathering.GatheringType;
import org.millenaire.goal.gathering.handler.AbstractGatheringHandler;
import org.millenaire.item.BlockHelper;
import org.millenaire.item.ItemHelper;
import org.millenaire.village.Village;
import org.slf4j.Logger;

public class FruitHarvestingHandler
extends AbstractGatheringHandler {
    private static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public String id() {
        return "fruit_harvesting";
    }

    @Override
    public List<String> validate(GatheringType type) {
        ArrayList<String> errors = new ArrayList<String>();
        errors.addAll(FruitHarvestingHandler.validateBlockParam(type, "targetBlock", true));
        errors.addAll(FruitHarvestingHandler.validateItemParam(type, "harvestItem", true));
        return errors;
    }

    @Override
    public boolean canStart(GoalContext ctx, GatheringType type) {
        return !this.collectRipeFruits(ctx.level(), ctx.village(), type).isEmpty();
    }

    @Override
    @Nullable
    public GatheringTarget findTarget(GoalContext ctx, GatheringType type, @Nullable GatheringTarget lastTarget) {
        List<BlockPos> candidates = this.collectRipeFruits(ctx.level(), ctx.village(), type);
        BlockPos reference = lastTarget != null ? lastTarget.navigationPos() : ctx.villager().blockPosition();
        BlockPos best = FruitHarvestingHandler.findClosestBlock(candidates, reference, lastTarget, type.batchRadius());
        return best != null ? new GatheringTarget.BlockTarget(best) : null;
    }

    @Override
    public boolean performAction(GoalContext ctx, GatheringType type, GatheringTarget target) {
        if (!(target instanceof GatheringTarget.BlockTarget)) {
            return true;
        }
        GatheringTarget.BlockTarget blockTarget = (GatheringTarget.BlockTarget)target;
        BlockPos pos = blockTarget.pos();
        ServerLevel level = ctx.level();
        BlockState state = level.getBlockState(pos);
        JsonObject params = type.handlerParams();
        Block targetBlock = BlockHelper.resolve(GsonHelper.getAsString((JsonObject)params, (String)"targetBlock", (String)""));
        if (targetBlock == null || !state.is(targetBlock)) {
            return true;
        }
        int ripeAge = GsonHelper.getAsInt((JsonObject)params, (String)"ripeAge", (int)3);
        String agePropertyName = GsonHelper.getAsString((JsonObject)params, (String)"ageProperty", (String)"age");
        Property prop = state.getBlock().getStateDefinition().getProperty(agePropertyName);
        if (!(prop instanceof IntegerProperty)) {
            return true;
        }
        IntegerProperty ageProp = (IntegerProperty)prop;
        int currentAge = (Integer)state.getValue((Property)ageProp);
        if (currentAge != ripeAge) {
            return true;
        }
        int resetAge = GsonHelper.getAsInt((JsonObject)params, (String)"resetAge", (int)0);
        level.setBlock(pos, (BlockState)state.setValue((Property)ageProp, (Comparable)Integer.valueOf(resetAge)), 2);
        String harvestItemId = GsonHelper.getAsString((JsonObject)params, (String)"harvestItem", (String)"");
        Item harvestItem = ItemHelper.resolve(harvestItemId);
        if (harvestItem != null) {
            int harvestCount = GsonHelper.getAsInt((JsonObject)params, (String)"harvestCount", (int)1);
            ctx.villager().getInventory().add(harvestItem, harvestCount);
        }
        return true;
    }

    private List<BlockPos> collectRipeFruits(ServerLevel level, Village village, GatheringType type) {
        JsonObject params = type.handlerParams();
        Block targetBlock = BlockHelper.resolve(GsonHelper.getAsString((JsonObject)params, (String)"targetBlock", (String)""));
        if (targetBlock == null) {
            return List.of();
        }
        int ripeAge = GsonHelper.getAsInt((JsonObject)params, (String)"ripeAge", (int)3);
        String agePropertyName = GsonHelper.getAsString((JsonObject)params, (String)"ageProperty", (String)"age");
        String buildingTag = GsonHelper.getAsString((JsonObject)params, (String)"buildingTag", (String)"grove");
        ArrayList<BlockPos> candidates = new ArrayList<BlockPos>();
        for (BuildingInstance building : village.getOperationalBuildingsWithTag(buildingTag)) {
            for (SpecialPoint sp : building.getPointsByType("treeSpawn")) {
                BlockPos center = sp.pos();
                int scanRadius = 5;
                for (int dx = -scanRadius; dx <= scanRadius; ++dx) {
                    for (int dy = -2; dy <= 8; ++dy) {
                        for (int dz = -scanRadius; dz <= scanRadius; ++dz) {
                            IntegerProperty ageProp;
                            Property prop;
                            BlockState state;
                            BlockPos checkPos = center.offset(dx, dy, dz);
                            if (!level.isLoaded(checkPos) || !(state = level.getBlockState(checkPos)).is(targetBlock) || !((prop = state.getBlock().getStateDefinition().getProperty(agePropertyName)) instanceof IntegerProperty) || (Integer)state.getValue((Property)(ageProp = (IntegerProperty)prop)) != ripeAge) continue;
                            candidates.add(checkPos);
                        }
                    }
                }
            }
        }
        return candidates;
    }
}

