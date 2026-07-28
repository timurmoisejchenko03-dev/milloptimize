/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  javax.annotation.Nullable
 *  net.minecraft.core.registries.Registries
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.tags.TagKey
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.level.Level
 *  org.slf4j.Logger
 */
package org.millenaire.goal.gathering;

import com.mojang.logging.LogUtils;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import javax.annotation.Nullable;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import org.millenaire.building.BuildingId;
import org.millenaire.building.BuildingInstance;
import org.millenaire.building.BuildingInventory;
import org.millenaire.building.GoodAvailabilityHelper;
import org.millenaire.entity.MillVillager;
import org.millenaire.entity.VillagerInventory;
import org.millenaire.goal.GoalContext;
import org.millenaire.goal.GoalScheduler;
import org.millenaire.goal.GoalUtils;
import org.millenaire.goal.PerVillagerThrottle;
import org.millenaire.goal.VillagerGoal;
import org.millenaire.goal.VillagerTask;
import org.millenaire.goal.gathering.GatheringHandler;
import org.millenaire.goal.gathering.GatheringTask;
import org.millenaire.goal.gathering.GatheringType;
import org.millenaire.item.ItemHelper;
import org.slf4j.Logger;

public class GatheringGoal
implements VillagerGoal {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final PerVillagerThrottle canStartThrottle = new PerVillagerThrottle(40);
    private final GatheringType type;
    private final GatheringHandler handler;

    public GatheringGoal(GatheringType type, GatheringHandler handler) {
        this.type = type;
        this.handler = handler;
    }

    public GatheringType getGatheringType() {
        return this.type;
    }

    public GatheringHandler getHandler() {
        return this.handler;
    }

    @Override
    public ResourceLocation id() {
        return this.type.id();
    }

    @Override
    public boolean isLeisure() {
        return this.type.leisure();
    }

    @Override
    public long reoccurDelayTicks() {
        return this.type.reoccurDelay();
    }

    @Override
    public int computePriority(GoalContext context) {
        int base = this.type.priority();
        if (this.type.priorityInvPenaltyItems() != null) {
            int invCount = this.countPenaltyItemsInInventory(context);
            base = Math.max(10, this.type.priorityInvPenaltyBase() - invCount);
        }
        int random = this.type.priorityRandom() > 0 ? ThreadLocalRandom.current().nextInt(this.type.priorityRandom()) : 0;
        return base + random;
    }

    private int countPenaltyItemsInInventory(GoalContext context) {
        VillagerInventory inventory = context.villager().getInventory();
        Map<Item, Integer> all = inventory.getAll();
        int total = 0;
        for (String key : this.type.priorityInvPenaltyItems()) {
            if (key.startsWith("#")) {
                ResourceLocation tagId = ResourceLocation.parse((String)key.substring(1));
                TagKey tag = TagKey.create((ResourceKey)Registries.ITEM, (ResourceLocation)tagId);
                for (Map.Entry<Item, Integer> e : all.entrySet()) {
                    if (!e.getKey().builtInRegistryHolder().is(tag)) continue;
                    total += e.getValue().intValue();
                }
                continue;
            }
            Item item = ItemHelper.resolve(key);
            if (item == null) continue;
            total += inventory.getCount(item);
        }
        return total;
    }

    @Override
    public boolean canStart(GoalContext context) {
        long gameTime;
        int total;
        int count;
        BuildingId destId;
        BuildingInstance townhall;
        long dayTime = context.dayTime();
        if (this.type.minimumHour() >= 0 && dayTime < (long)this.type.minimumHour()) {
            return false;
        }
        if (this.type.maximumHour() >= 0 && dayTime > (long)this.type.maximumHour()) {
            return false;
        }
        if (this.type.buildingLimit() != null) {
            BuildingInstance limitTarget = this.handler.resolveBuildingLimitTarget(context, this.type);
            if (limitTarget == null) {
                limitTarget = context.resolveHomeBuilding().orElse(null);
            }
            if (limitTarget != null && !this.checkStockLimit(context, limitTarget, this.type.buildingLimit())) {
                return false;
            }
        }
        if (this.type.townhallLimit() != null && (townhall = context.village().getTownhall()) != null && !this.checkStockLimit(context, townhall, this.type.townhallLimit())) {
            return false;
        }
        if (this.type.maxSimultaneousInBuilding() > 0 && (destId = this.resolveDestBuildingId(context)) != null && (count = this.countActiveGatheringTasksForBuilding(context, destId)) >= this.type.maxSimultaneousInBuilding()) {
            return false;
        }
        if (this.type.villageLimit() != null && !this.checkVillageStockLimit(context, this.type.villageLimit())) {
            return false;
        }
        if (this.type.itemsBalance() != null && !this.checkItemsBalance(context, this.type.itemsBalance())) {
            return false;
        }
        if (this.type.maxSimultaneousTotal() > 0 && (total = GoalUtils.countSimultaneous(context, this.type.id())) >= this.type.maxSimultaneousTotal()) {
            return false;
        }
        UUID uuid = context.villager().getUUID();
        if (this.canStartThrottle.isThrottled(uuid, gameTime = context.gameTime())) {
            return false;
        }
        boolean result = this.handler.canStart(context, this.type);
        this.canStartThrottle.record(uuid, gameTime);
        return result;
    }

    private boolean checkStockLimit(GoalContext context, BuildingInstance building, Map<String, Integer> limits) {
        BuildingInventory inv = building.getInventory();
        if (inv == null) {
            return true;
        }
        for (Map.Entry<String, Integer> entry : limits.entrySet()) {
            int current;
            Item item;
            ResourceLocation tagId;
            TagKey tag;
            int total;
            String key = entry.getKey();
            int maxQty = entry.getValue();
            if (!(key.startsWith("#") ? (total = this.countTagInInventory(inv, context, (TagKey<Item>)(tag = TagKey.create((ResourceKey)Registries.ITEM, (ResourceLocation)(tagId = ResourceLocation.parse((String)key.substring(1))))))) >= maxQty : (item = ItemHelper.resolve(key)) != null && (current = inv.getCount((Level)context.level(), item)) >= maxQty)) continue;
            return false;
        }
        return true;
    }

    private int countTagInInventory(BuildingInventory inv, GoalContext context, TagKey<Item> tag) {
        Map<Item, Integer> cache = inv.getCachedContents();
        if (cache == null) {
            inv.scanChests((Level)context.level());
            cache = inv.getCachedContents();
        }
        if (cache == null) {
            return 0;
        }
        int total = 0;
        for (Map.Entry<Item, Integer> e : cache.entrySet()) {
            if (!e.getKey().builtInRegistryHolder().is(tag)) continue;
            total += e.getValue().intValue();
        }
        return total;
    }

    private boolean checkVillageStockLimit(GoalContext context, Map<String, Integer> limits) {
        ServerLevel serverLevel = context.level();
        if (!(serverLevel instanceof ServerLevel)) {
            return true;
        }
        ServerLevel sl = serverLevel;
        for (Map.Entry entry : limits.entrySet()) {
            int current;
            String key = (String)entry.getKey();
            int maxQty = (Integer)entry.getValue();
            if (key.startsWith("#")) {
                ResourceLocation tagId = ResourceLocation.parse((String)key.substring(1));
                TagKey tag = TagKey.create((ResourceKey)Registries.ITEM, (ResourceLocation)tagId);
                current = context.village().getVillageTagCount(sl, (TagKey<Item>)tag);
            } else {
                Item item = ItemHelper.resolve(key);
                if (item == null) continue;
                current = context.village().getVillageItemCount(sl, item);
            }
            if (current < maxQty) continue;
            return false;
        }
        return true;
    }

    private boolean checkItemsBalance(GoalContext context, Map<String, String> itemsBalance) {
        ServerLevel serverLevel = context.level();
        if (!(serverLevel instanceof ServerLevel)) {
            return true;
        }
        ServerLevel sl = serverLevel;
        BuildingInstance townhall = context.village().getTownhall();
        if (townhall == null) {
            return true;
        }
        ResourceLocation cultureId = context.village().getCultureId();
        for (Map.Entry<String, String> entry : itemsBalance.entrySet()) {
            int outputCount;
            int inputCount;
            Item input = ItemHelper.resolve(entry.getKey());
            Item output = ItemHelper.resolve(entry.getValue());
            if (input == null || output == null || (inputCount = GoodAvailabilityHelper.nbGoodAvailable(townhall, input, sl, context.village(), cultureId, false)) >= (outputCount = GoodAvailabilityHelper.nbGoodAvailable(townhall, output, sl, context.village(), cultureId, false))) continue;
            return false;
        }
        return true;
    }

    @Nullable
    private BuildingId resolveDestBuildingId(GoalContext context) {
        BuildingInstance dest = this.handler.resolveBuildingLimitTarget(context, this.type);
        if (dest == null) {
            dest = context.resolveHomeBuilding().orElse(null);
        }
        return dest != null ? dest.getId() : null;
    }

    private int countActiveGatheringTasksForBuilding(GoalContext context, BuildingId buildingId) {
        int count = 0;
        ResourceLocation goalId = this.type.id();
        for (UUID uuid : context.village().getVillagerUuids()) {
            GatheringTask gatheringTask;
            VillagerTask task;
            ResourceLocation otherGoalId;
            GoalScheduler scheduler;
            MillVillager other;
            Entity entity = context.level().getEntity(uuid);
            if (!(entity instanceof MillVillager) || (other = (MillVillager)entity) == context.villager() || (scheduler = other.getGoalScheduler()) == null || !goalId.equals((Object)(otherGoalId = scheduler.getCurrentGoalId())) || !((task = scheduler.getCurrentTask()) instanceof GatheringTask) || !buildingId.equals((gatheringTask = (GatheringTask)task).getTargetBuildingId())) continue;
            ++count;
        }
        return count;
    }

    @Override
    public VillagerTask start(GoalContext context) {
        LOGGER.debug("Starting goal {} for villager {}", (Object)this.type.id(), (Object)context.villager().getVillagerTypeId());
        BuildingId destId = this.resolveDestBuildingId(context);
        return new GatheringTask(this.type, this.handler, destId);
    }
}

