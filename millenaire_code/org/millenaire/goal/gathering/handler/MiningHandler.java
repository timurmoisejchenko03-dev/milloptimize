/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonObject
 *  com.mojang.logging.LogUtils
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 *  net.minecraft.util.GsonHelper
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.Items
 *  net.minecraft.world.level.block.Blocks
 *  org.slf4j.Logger
 */
package org.millenaire.goal.gathering.handler;

import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import org.millenaire.building.BuildingInstance;
import org.millenaire.building.SpecialPoint;
import org.millenaire.goal.GoalContext;
import org.millenaire.goal.gathering.GatheringTarget;
import org.millenaire.goal.gathering.GatheringType;
import org.millenaire.goal.gathering.handler.AbstractGatheringHandler;
import org.millenaire.tool.ToolCategory;
import org.millenaire.tool.ToolCategoryRegistry;
import org.millenaire.village.Village;
import org.slf4j.Logger;

public class MiningHandler
extends AbstractGatheringHandler {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Set<String> HARD_SUBTYPES = Set.of("stone", "sandstone", "red_sandstone", "diorite", "iron");
    private static final Set<String> SOFT_SUBTYPES = Set.of("sand", "clay", "gravel");
    private static final Set<String> FROZEN_SUBTYPES = Set.of("snow", "ice");

    @Override
    public String id() {
        return "mining";
    }

    @Override
    public List<String> validate(GatheringType type) {
        return this.validateItemList(type, "loot", false);
    }

    @Override
    public int getActionCooldown(GoalContext ctx, GatheringType type) {
        float efficiency;
        String subtype = this.getSourceSubtype(type);
        if (subtype == null) {
            return type.actionCooldown();
        }
        if (FROZEN_SUBTYPES.contains(subtype)) {
            return 70;
        }
        if (SOFT_SUBTYPES.contains(subtype)) {
            ToolCategory category = ToolCategoryRegistry.get("toolsshovel");
            if (category == null) {
                return type.actionCooldown();
            }
            efficiency = category.getBestDestroySpeed(item -> ctx.villager().getInventory().getCount((Item)item) > 0, Blocks.SAND.defaultBlockState(), Items.WOODEN_SHOVEL);
        } else if (HARD_SUBTYPES.contains(subtype)) {
            ToolCategory category = ToolCategoryRegistry.get("toolspickaxe");
            if (category == null) {
                return type.actionCooldown();
            }
            efficiency = category.getBestDestroySpeed(item -> ctx.villager().getInventory().getCount((Item)item) > 0, Blocks.SANDSTONE.defaultBlockState(), Items.WOODEN_PICKAXE);
        } else {
            return type.actionCooldown();
        }
        return 140 - 4 * (int)efficiency;
    }

    @Override
    public String getHeldToolCategoryId(GatheringType type) {
        String subtype = this.getSourceSubtype(type);
        if (subtype == null) {
            return "toolspickaxe";
        }
        if (SOFT_SUBTYPES.contains(subtype)) {
            return "toolsshovel";
        }
        if (HARD_SUBTYPES.contains(subtype)) {
            return "toolspickaxe";
        }
        return null;
    }

    @Override
    public boolean supportsRemoteAction() {
        return false;
    }

    @Override
    public boolean canStart(GoalContext ctx, GatheringType type) {
        List<BlockPos> sources = this.findSourcePositions(ctx.village(), type);
        return !sources.isEmpty();
    }

    @Override
    @Nullable
    public GatheringTarget findTarget(GoalContext ctx, GatheringType type, @Nullable GatheringTarget lastTarget) {
        List<BlockPos> sources = this.findSourcePositions(ctx.village(), type);
        if (sources.isEmpty()) {
            return null;
        }
        BlockPos reference = lastTarget != null ? lastTarget.navigationPos() : ctx.villager().blockPosition();
        BlockPos best = MiningHandler.findClosestBlock(sources, reference, lastTarget, type.batchRadius());
        return best != null ? new GatheringTarget.BlockTarget(best) : null;
    }

    @Override
    public boolean performAction(GoalContext ctx, GatheringType type, GatheringTarget target) {
        Map<Item, Integer> loot = this.parseItemList(type.handlerParams(), "loot");
        for (Map.Entry<Item, Integer> entry : loot.entrySet()) {
            ctx.villager().getInventory().add(entry.getKey(), entry.getValue());
        }
        LOGGER.debug("Mining: loot added to villager inventory at {}", (Object)target.navigationPos());
        return true;
    }

    private List<BlockPos> findSourcePositions(Village village, GatheringType type) {
        String sourceSubtype = this.getSourceSubtype(type);
        String buildingTag = this.getBuildingTag(type);
        ArrayList<BlockPos> positions = new ArrayList<BlockPos>();
        List<BuildingInstance> buildings = buildingTag != null ? village.getOperationalBuildingsWithTag(buildingTag) : village.getBuildings();
        for (BuildingInstance building : buildings) {
            if (!building.isOperational()) continue;
            for (SpecialPoint sp : building.getPointsByType("source")) {
                if (sourceSubtype != null && !sourceSubtype.equals(sp.subtype())) continue;
                positions.add(sp.pos());
            }
        }
        return positions;
    }

    @Nullable
    private String getSourceSubtype(GatheringType type) {
        return GsonHelper.getAsString((JsonObject)type.handlerParams(), (String)"sourceSubtype", null);
    }
}

