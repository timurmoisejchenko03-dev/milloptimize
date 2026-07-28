/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonObject
 *  com.mojang.logging.LogUtils
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Vec3i
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.util.GsonHelper
 *  net.minecraft.world.InteractionHand
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.level.Level
 *  org.slf4j.Logger
 */
package org.millenaire.goal.gathering.handler;

import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import org.millenaire.building.BuildingInstance;
import org.millenaire.building.BuildingInventory;
import org.millenaire.goal.GoalContext;
import org.millenaire.goal.gathering.GatheringTarget;
import org.millenaire.goal.gathering.GatheringType;
import org.millenaire.goal.gathering.handler.AbstractGatheringHandler;
import org.millenaire.item.ItemHelper;
import org.slf4j.Logger;

public class TakeFromBuildingHandler
extends AbstractGatheringHandler {
    private static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public String id() {
        return "take_from_building";
    }

    @Override
    public List<String> validate(GatheringType type) {
        ArrayList<String> errors = new ArrayList<String>();
        if (this.getBuildingTag(type) == null) {
            errors.add("missing required handlerParam 'buildingTag'");
        }
        if (this.resolveCollectGood(type) == null) {
            String raw = GsonHelper.getAsString((JsonObject)type.handlerParams(), (String)"collectGood", null);
            if (raw == null) {
                errors.add("missing required handlerParam 'collectGood'");
            } else {
                errors.add("unresolvable item in 'collectGood': " + raw);
            }
        }
        return errors;
    }

    @Override
    public boolean canStart(GoalContext ctx, GatheringType type) {
        String buildingTag = this.getBuildingTag(type);
        Item collectItem = this.resolveCollectGood(type);
        int minimumPickup = GsonHelper.getAsInt((JsonObject)type.handlerParams(), (String)"minimumPickup", (int)4);
        if (buildingTag == null || collectItem == null) {
            return false;
        }
        ServerLevel level = ctx.level();
        for (BuildingInstance building : this.findBuildingsWithTag(ctx.village(), buildingTag)) {
            BuildingInventory inv = building.getInventory();
            if (inv == null || inv.getCount((Level)level, collectItem) < minimumPickup) continue;
            return true;
        }
        return false;
    }

    @Override
    @Nullable
    public GatheringTarget findTarget(GoalContext ctx, GatheringType type, @Nullable GatheringTarget lastTarget) {
        String buildingTag = this.getBuildingTag(type);
        Item collectItem = this.resolveCollectGood(type);
        int minimumPickup = GsonHelper.getAsInt((JsonObject)type.handlerParams(), (String)"minimumPickup", (int)4);
        if (buildingTag == null || collectItem == null) {
            return null;
        }
        ServerLevel level = ctx.level();
        BlockPos reference = lastTarget != null ? lastTarget.navigationPos() : ctx.villager().blockPosition();
        BlockPos bestPos = null;
        double bestDistSq = Double.MAX_VALUE;
        for (BuildingInstance building : this.findBuildingsWithTag(ctx.village(), buildingTag)) {
            double distSq;
            BlockPos targetPos;
            BuildingInventory inv = building.getInventory();
            if (inv == null || inv.getCount((Level)level, collectItem) < minimumPickup || (targetPos = this.resolveNavPos(building)) == null || !((distSq = reference.distSqr((Vec3i)targetPos)) < bestDistSq)) continue;
            bestDistSq = distSq;
            bestPos = targetPos;
        }
        return bestPos != null ? new GatheringTarget.BlockTarget(bestPos) : null;
    }

    @Override
    public boolean performAction(GoalContext ctx, GatheringType type, GatheringTarget target) {
        String buildingTag = this.getBuildingTag(type);
        Item collectItem = this.resolveCollectGood(type);
        int maxCollect = GsonHelper.getAsInt((JsonObject)type.handlerParams(), (String)"maxCollect", (int)64);
        if (buildingTag == null || collectItem == null) {
            return true;
        }
        BuildingInstance targetBuilding = null;
        for (BuildingInstance building : this.findBuildingsWithTag(ctx.village(), buildingTag)) {
            BlockPos navPos = this.resolveNavPos(building);
            if (navPos == null || !navPos.equals((Object)target.navigationPos())) continue;
            targetBuilding = building;
            break;
        }
        if (targetBuilding == null) {
            return true;
        }
        BuildingInventory inv = targetBuilding.getInventory();
        if (inv == null) {
            return true;
        }
        ServerLevel level = ctx.level();
        int currentlyHeld = ctx.villager().getInventory().getCount(collectItem);
        int canTake = Math.min(maxCollect - currentlyHeld, maxCollect);
        if (canTake <= 0) {
            return true;
        }
        int taken = inv.remove((Level)level, collectItem, canTake);
        if (taken > 0) {
            ctx.villager().getInventory().add(collectItem, taken);
            ctx.villager().swing(InteractionHand.MAIN_HAND);
            LOGGER.debug("Took {} {} from building {} at {}", new Object[]{taken, collectItem, targetBuilding.getPlanId(), target.navigationPos()});
        }
        return true;
    }

    @Override
    @Nullable
    public BuildingInstance resolveBuildingLimitTarget(GoalContext ctx, GatheringType type) {
        String buildingTag = this.getBuildingTag(type);
        if (buildingTag == null) {
            return null;
        }
        List<BuildingInstance> buildings = this.findBuildingsWithTag(ctx.village(), buildingTag);
        return buildings.isEmpty() ? null : buildings.getFirst();
    }

    @Nullable
    private Item resolveCollectGood(GatheringType type) {
        String itemId = GsonHelper.getAsString((JsonObject)type.handlerParams(), (String)"collectGood", null);
        if (itemId == null) {
            return null;
        }
        return ItemHelper.resolve(itemId);
    }

    @Nullable
    private BlockPos resolveNavPos(BuildingInstance building) {
        BlockPos pos = building.getFirstPointPos("sellingPos");
        if (pos != null) {
            return pos;
        }
        pos = building.getFirstPointPos("craftingPos");
        if (pos != null) {
            return pos;
        }
        return building.getOrigin();
    }
}

