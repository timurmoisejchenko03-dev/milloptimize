/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonArray
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Vec3i
 *  net.minecraft.util.GsonHelper
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.Items
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.phys.AABB
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 */
package org.millenaire.goal.gathering.handler;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import org.millenaire.building.BuildingId;
import org.millenaire.building.BuildingInstance;
import org.millenaire.building.BuildingInventory;
import org.millenaire.building.BuildingPlan;
import org.millenaire.culture.ModCultures;
import org.millenaire.goal.GoalContext;
import org.millenaire.goal.gathering.GatheringHandler;
import org.millenaire.goal.gathering.GatheringTarget;
import org.millenaire.goal.gathering.GatheringType;
import org.millenaire.item.BlockHelper;
import org.millenaire.item.ItemHelper;
import org.millenaire.village.Village;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractGatheringHandler
implements GatheringHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractGatheringHandler.class);

    @Nullable
    protected String getBuildingTag(GatheringType type) {
        return GsonHelper.getAsString((JsonObject)type.handlerParams(), (String)"buildingTag", null);
    }

    @Nullable
    protected String getRequiredTag(GatheringType type) {
        return GsonHelper.getAsString((JsonObject)type.handlerParams(), (String)"requiredTag", null);
    }

    protected List<BuildingInstance> findBuildingsWithTag(Village village, String tag) {
        return village.getOperationalBuildingsWithTag(tag);
    }

    protected List<BlockPos> collectSoilPositions(Village village) {
        ArrayList<BlockPos> positions = new ArrayList<BlockPos>();
        for (BuildingInstance building : village.getBuildings()) {
            if (!building.isOperational()) continue;
            positions.addAll(building.getSoilPositions());
        }
        return positions;
    }

    protected List<BlockPos> collectSoilPositions(GoalContext ctx, GatheringType type) {
        String soilSubtype = GsonHelper.getAsString((JsonObject)type.handlerParams(), (String)"soilSubtype", null);
        List<BuildingInstance> candidates = this.resolveTargetBuildings(ctx, type);
        ArrayList<BlockPos> positions = new ArrayList<BlockPos>();
        for (BuildingInstance building : candidates) {
            if (!building.isOperational()) continue;
            positions.addAll(building.getSoilPositions(soilSubtype));
        }
        return positions;
    }

    protected Map<Item, Integer> parseItemList(JsonObject params, String key) {
        HashMap<Item, Integer> items = new HashMap<Item, Integer>();
        if (!params.has(key)) {
            return items;
        }
        JsonArray array = params.getAsJsonArray(key);
        for (JsonElement elem : array) {
            JsonObject obj = elem.getAsJsonObject();
            String itemId = GsonHelper.getAsString((JsonObject)obj, (String)"item");
            int count = GsonHelper.getAsInt((JsonObject)obj, (String)"count", (int)1);
            Item item = ItemHelper.resolve(itemId);
            if (item == null || item == Items.AIR) continue;
            items.merge(item, count, Integer::sum);
        }
        return items;
    }

    @Nullable
    protected Item resolveSeedItem(GatheringType type) {
        JsonObject params = type.handlerParams();
        String seedItemId = GsonHelper.getAsString((JsonObject)params, (String)"seedItem", null);
        if (seedItemId == null) {
            return null;
        }
        return ItemHelper.resolve(seedItemId);
    }

    protected boolean homeBuildingHasItem(GoalContext ctx, Item item) {
        return ctx.resolveHomeInventory().map(inv -> inv.getCount((Level)ctx.level(), item) > 0).orElse(false);
    }

    protected boolean takeFromHomeBuilding(GoalContext ctx, Item item, int count) {
        BuildingInventory inv = ctx.resolveHomeInventory().orElse(null);
        if (inv == null) {
            return false;
        }
        int removed = inv.remove((Level)ctx.level(), item, count);
        if (removed > 0) {
            ctx.villager().getInventory().add(item, removed);
            return true;
        }
        return false;
    }

    protected List<BuildingInstance> resolveTargetBuildings(GoalContext ctx, GatheringType type) {
        List<BuildingInstance> candidates;
        String buildingTag = this.getBuildingTag(type);
        if (buildingTag != null) {
            candidates = this.findBuildingsWithTag(ctx.village(), buildingTag);
        } else {
            BuildingId homeId = ctx.villager().getHomeBuilding();
            if (homeId == null) {
                return List.of();
            }
            BuildingInstance home = ctx.village().getBuilding(homeId);
            if (home == null || !home.isOperational()) {
                return List.of();
            }
            candidates = List.of(home);
        }
        String requiredTag = this.getRequiredTag(type);
        if (requiredTag != null) {
            ArrayList<BuildingInstance> filtered = new ArrayList<BuildingInstance>();
            for (BuildingInstance b : candidates) {
                BuildingPlan plan = ModCultures.getBuildingPlan(b.getPlanId());
                if (plan == null || !plan.hasTag(requiredTag)) continue;
                filtered.add(b);
            }
            return filtered;
        }
        return candidates;
    }

    protected List<String> validateItemList(GatheringType type, String paramName, boolean required) {
        ArrayList<String> errors = new ArrayList<String>();
        JsonObject params = type.handlerParams();
        if (!params.has(paramName)) {
            if (required) {
                errors.add("missing required handlerParam '" + paramName + "'");
            }
            return errors;
        }
        JsonArray array = params.getAsJsonArray(paramName);
        for (JsonElement elem : array) {
            JsonObject obj = elem.getAsJsonObject();
            String itemId = GsonHelper.getAsString((JsonObject)obj, (String)"item");
            Item item = ItemHelper.resolve(itemId);
            if (item != null && item != Items.AIR) continue;
            errors.add("unresolvable item '" + itemId + "' in '" + paramName + "'");
        }
        return errors;
    }

    protected static List<String> validateItemParam(GatheringType type, String paramName, boolean required) {
        ArrayList<String> errors = new ArrayList<String>();
        String itemId = GsonHelper.getAsString((JsonObject)type.handlerParams(), (String)paramName, null);
        if (itemId == null) {
            if (required) {
                errors.add("missing required handlerParam '" + paramName + "'");
            }
            return errors;
        }
        Item item = ItemHelper.resolve(itemId);
        if (item == null || item == Items.AIR) {
            errors.add("unresolvable item '" + itemId + "' in '" + paramName + "'");
        }
        return errors;
    }

    protected static List<String> validateBlockParam(GatheringType type, String paramName, boolean required) {
        ArrayList<String> errors = new ArrayList<String>();
        String blockId = GsonHelper.getAsString((JsonObject)type.handlerParams(), (String)paramName, null);
        if (blockId == null || blockId.isEmpty()) {
            if (required) {
                errors.add("missing required handlerParam '" + paramName + "'");
            }
            return errors;
        }
        Block block = BlockHelper.resolve(blockId);
        if (block == null) {
            errors.add("unresolvable block '" + blockId + "' in '" + paramName + "'");
        }
        return errors;
    }

    protected static List<String> validateBlockList(GatheringType type, String paramName, boolean required) {
        ArrayList<String> errors = new ArrayList<String>();
        JsonObject params = type.handlerParams();
        if (!params.has(paramName)) {
            if (required) {
                errors.add("missing required handlerParam '" + paramName + "'");
            }
            return errors;
        }
        JsonArray array = params.getAsJsonArray(paramName);
        if (array.isEmpty() && required) {
            errors.add("empty '" + paramName + "' array");
            return errors;
        }
        for (JsonElement elem : array) {
            String blockId = elem.getAsString();
            Block block = BlockHelper.resolve(blockId);
            if (block != null) continue;
            errors.add("unresolvable block '" + blockId + "' in '" + paramName + "'");
        }
        return errors;
    }

    protected static List<String> validateItemIdList(GatheringType type, String paramName, boolean required) {
        ArrayList<String> errors = new ArrayList<String>();
        JsonObject params = type.handlerParams();
        if (!params.has(paramName)) {
            if (required) {
                errors.add("missing required handlerParam '" + paramName + "'");
            }
            return errors;
        }
        JsonArray array = params.getAsJsonArray(paramName);
        if (array.isEmpty() && required) {
            errors.add("empty '" + paramName + "' array");
            return errors;
        }
        for (JsonElement elem : array) {
            String itemId = elem.getAsString();
            Item item = ItemHelper.resolve(itemId);
            if (item != null && item != Items.AIR) continue;
            errors.add("unresolvable item '" + itemId + "' in '" + paramName + "'");
        }
        return errors;
    }

    protected static int countAnimalSpawnPoints(BuildingInstance building) {
        return building.getPointsByType("animalSpawn").size();
    }

    protected static AABB scanBoxAround(BlockPos origin, int radiusXZ, int radiusY) {
        return new AABB((double)(origin.getX() - radiusXZ), (double)(origin.getY() - radiusY), (double)(origin.getZ() - radiusXZ), (double)(origin.getX() + radiusXZ), (double)(origin.getY() + radiusY), (double)(origin.getZ() + radiusXZ));
    }

    @Nullable
    public static BlockPos findClosestBlock(List<BlockPos> candidates, Predicate<BlockPos> filter, BlockPos reference, @Nullable GatheringTarget lastTarget, int batchRadius) {
        BlockPos best = null;
        double bestDistSq = Double.MAX_VALUE;
        for (BlockPos pos : candidates) {
            if (!filter.test(pos)) continue;
            double distSq = reference.distSqr((Vec3i)pos);
            if (lastTarget != null && distSq > (double)batchRadius * (double)batchRadius || !(distSq < bestDistSq)) continue;
            bestDistSq = distSq;
            best = pos;
        }
        return best;
    }

    @Nullable
    public static BlockPos findClosestBlock(List<BlockPos> candidates, BlockPos reference, @Nullable GatheringTarget lastTarget, int batchRadius) {
        BlockPos best = null;
        double bestDistSq = Double.MAX_VALUE;
        for (BlockPos pos : candidates) {
            double distSq = reference.distSqr((Vec3i)pos);
            if (lastTarget != null && distSq > (double)batchRadius * (double)batchRadius || !(distSq < bestDistSq)) continue;
            bestDistSq = distSq;
            best = pos;
        }
        return best;
    }

    @Nullable
    protected static <T extends Entity> T findClosestEntity(List<T> candidates, BlockPos reference, @Nullable GatheringTarget lastTarget, int batchRadius) {
        Entity best = null;
        double bestDistSq = Double.MAX_VALUE;
        for (Entity entity : candidates) {
            double distSq = reference.distSqr((Vec3i)entity.blockPosition());
            if (lastTarget != null && distSq > (double)batchRadius * (double)batchRadius || !(distSq < bestDistSq)) continue;
            bestDistSq = distSq;
            best = entity;
        }
        return (T)best;
    }

    protected void applyIrrigationBonus(GoalContext ctx, GatheringType type) {
        Item item;
        String bonusId = GsonHelper.getAsString((JsonObject)type.handlerParams(), (String)"irrigationBonusCrop", (String)"");
        if (bonusId.isEmpty()) {
            return;
        }
        int irrigation = ctx.village().getVillageIrrigation();
        if (irrigation <= 0) {
            return;
        }
        if (Math.random() < (double)irrigation / 100.0 && (item = ItemHelper.resolve(bonusId)) != null) {
            ctx.villager().getInventory().add(item, 1);
        }
    }
}

