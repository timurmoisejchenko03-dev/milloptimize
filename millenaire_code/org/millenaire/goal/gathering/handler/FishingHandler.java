/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.Items
 *  org.slf4j.Logger
 */
package org.millenaire.goal.gathering.handler;

import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.millenaire.building.BuildingInstance;
import org.millenaire.building.SpecialPoint;
import org.millenaire.goal.GoalContext;
import org.millenaire.goal.gathering.GatheringTarget;
import org.millenaire.goal.gathering.GatheringType;
import org.millenaire.goal.gathering.handler.AbstractGatheringHandler;
import org.millenaire.village.Village;
import org.slf4j.Logger;

public class FishingHandler
extends AbstractGatheringHandler {
    private static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public String id() {
        return "fishing";
    }

    @Override
    public Item getDefaultHeldItem(GatheringType type) {
        return Items.FISHING_ROD;
    }

    @Override
    public List<String> validate(GatheringType type) {
        return this.validateItemList(type, "loot", false);
    }

    @Override
    public boolean supportsRemoteAction() {
        return true;
    }

    @Override
    public boolean canStart(GoalContext ctx, GatheringType type) {
        List<BlockPos> spots = this.findFishingSpots(ctx.village(), type);
        return !spots.isEmpty();
    }

    @Override
    @Nullable
    public GatheringTarget findTarget(GoalContext ctx, GatheringType type, @Nullable GatheringTarget lastTarget) {
        List<BlockPos> spots = this.findFishingSpots(ctx.village(), type);
        if (spots.isEmpty()) {
            return null;
        }
        BlockPos reference = lastTarget != null ? lastTarget.navigationPos() : ctx.villager().blockPosition();
        BlockPos best = FishingHandler.findClosestBlock(spots, reference, lastTarget, type.batchRadius());
        return best != null ? new GatheringTarget.BlockTarget(best) : null;
    }

    @Override
    public boolean performAction(GoalContext ctx, GatheringType type, GatheringTarget target) {
        Map<Item, Integer> loot = this.parseItemList(type.handlerParams(), "loot");
        if (!loot.isEmpty()) {
            for (Map.Entry<Item, Integer> entry : loot.entrySet()) {
                ctx.villager().getInventory().add(entry.getKey(), entry.getValue());
            }
        } else {
            ctx.villager().getInventory().add(Items.COD, 1);
        }
        LOGGER.debug("Fishing: fish added to villager inventory at {}", (Object)target.navigationPos());
        return true;
    }

    private List<BlockPos> findFishingSpots(Village village, GatheringType type) {
        String buildingTag = this.getBuildingTag(type);
        ArrayList<BlockPos> positions = new ArrayList<BlockPos>();
        List<BuildingInstance> buildings = buildingTag != null ? village.getOperationalBuildingsWithTag(buildingTag) : village.getBuildings();
        for (BuildingInstance building : buildings) {
            if (!building.isOperational()) continue;
            for (SpecialPoint sp : building.getPointsByType("fishingSpot")) {
                positions.add(sp.pos());
            }
        }
        return positions;
    }
}

