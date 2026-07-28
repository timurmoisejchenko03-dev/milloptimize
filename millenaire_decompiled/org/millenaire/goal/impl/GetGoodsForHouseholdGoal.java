/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  javax.annotation.Nullable
 *  net.minecraft.network.chat.Component
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.level.Level
 *  org.slf4j.Logger
 */
package org.millenaire.goal.impl;

import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import org.millenaire.building.BuildingId;
import org.millenaire.building.BuildingInstance;
import org.millenaire.building.BuildingInventory;
import org.millenaire.building.GoodAvailabilityHelper;
import org.millenaire.culture.ModCultures;
import org.millenaire.culture.VillagerType;
import org.millenaire.entity.VillagerInventory;
import org.millenaire.goal.GoalContext;
import org.millenaire.goal.PerVillagerThrottle;
import org.millenaire.goal.VillagerGoal;
import org.millenaire.goal.VillagerTask;
import org.millenaire.goal.impl.AbstractTransportTask;
import org.millenaire.village.VillagerRecord;
import org.slf4j.Logger;

public class GetGoodsForHouseholdGoal
implements VillagerGoal {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)"get_goods_for_household");
    private static final int STANDARD_DELAY = 2000;
    private final PerVillagerThrottle throttle = new PerVillagerThrottle(2000);

    @Override
    public boolean showInTravelBook() {
        return false;
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public int computePriority(GoalContext context) {
        Map<Item, Integer> allGoods = GetGoodsForHouseholdGoal.collectHouseholdRequiredGoods(context);
        int nbMissing = this.countMissingGoods(context, allGoods);
        return nbMissing * 20;
    }

    @Override
    public boolean canStart(GoalContext context) {
        if (context.villager().getHomeBuilding() == null) {
            return false;
        }
        Map<Item, Integer> allRequiredGoods = GetGoodsForHouseholdGoal.collectHouseholdRequiredGoods(context);
        if (allRequiredGoods.isEmpty()) {
            return false;
        }
        int nbMissing = this.countMissingGoods(context, allRequiredGoods);
        long currentTick = context.gameTime();
        if (nbMissing <= 16 && !this.throttle.shouldEvaluate(context.villager().getUUID(), currentTick)) {
            return false;
        }
        if (nbMissing > 0) {
            return true;
        }
        return this.isCarryingNeededHouseholdGoods(context, allRequiredGoods);
    }

    private boolean isCarryingNeededHouseholdGoods(GoalContext context, Map<Item, Integer> requiredGoods) {
        BuildingInstance home;
        BuildingId homeId = context.villager().getHomeBuilding();
        BuildingInstance buildingInstance = home = homeId != null ? context.village().getBuilding(homeId) : null;
        if (home == null || home.getInventory() == null) {
            return false;
        }
        VillagerInventory villagerInv = context.villager().getInventory();
        BuildingInventory homeInv = home.getInventory();
        for (Map.Entry<Item, Integer> entry : requiredGoods.entrySet()) {
            int inHome = homeInv.getCount((Level)context.level(), entry.getKey());
            if (inHome >= entry.getValue() || villagerInv.getCount(entry.getKey()) <= 0) continue;
            return true;
        }
        return false;
    }

    static Map<Item, Integer> collectHouseholdRequiredGoods(GoalContext context) {
        BuildingId homeId = context.villager().getHomeBuilding();
        if (homeId == null) {
            return Map.of();
        }
        HashMap<Item, Integer> allGoods = new HashMap<Item, Integer>();
        for (VillagerRecord record : context.village().getVillagerRecords().values()) {
            VillagerType vtype;
            BuildingId vHome = record.getHomeBuilding();
            if (vHome == null || !vHome.equals(homeId) || (vtype = ModCultures.getVillagerType(record.getVillagerTypeId())) == null) continue;
            for (Map.Entry<Item, Integer> entry : vtype.resolvedRequiredGoods().entrySet()) {
                allGoods.merge(entry.getKey(), entry.getValue(), Math::max);
            }
        }
        return allGoods;
    }

    @Override
    public VillagerTask start(GoalContext context) {
        List<ItemAmount> itemsToGet = this.computeItemsToGet(context);
        return new GetGoodsForHouseholdTask(itemsToGet);
    }

    private int countMissingGoods(GoalContext context, Map<Item, Integer> requiredGoods) {
        BuildingInstance home;
        BuildingId homeId = context.villager().getHomeBuilding();
        BuildingInstance buildingInstance = home = homeId != null ? context.village().getBuilding(homeId) : null;
        if (home == null || home.getInventory() == null) {
            return 0;
        }
        BuildingInventory homeInv = home.getInventory();
        int missing = 0;
        block0: for (Map.Entry<Item, Integer> entry : requiredGoods.entrySet()) {
            Item item = entry.getKey();
            int required = entry.getValue();
            int inHome = homeInv.getCount((Level)context.level(), item);
            if (inHome >= required / 2) continue;
            for (BuildingInstance building : context.village().getBuildings()) {
                int available;
                if (!building.isOperational() || building.getId().equals(homeId) || building.getInventory() == null || (available = GoodAvailabilityHelper.nbGoodAvailable(building, item, context.level(), context.village(), context.village().getCultureId(), false)) <= 0) continue;
                missing += Math.min(required - inHome, available);
                continue block0;
            }
        }
        return missing;
    }

    private List<ItemAmount> computeItemsToGet(GoalContext context) {
        BuildingInstance home;
        Map<Item, Integer> allGoods = GetGoodsForHouseholdGoal.collectHouseholdRequiredGoods(context);
        if (allGoods.isEmpty()) {
            return List.of();
        }
        BuildingId homeId = context.villager().getHomeBuilding();
        BuildingInstance buildingInstance = home = homeId != null ? context.village().getBuilding(homeId) : null;
        if (home == null || home.getInventory() == null) {
            return List.of();
        }
        BuildingInventory homeInv = home.getInventory();
        VillagerInventory villagerInv = context.villager().getInventory();
        LinkedHashMap<Item, Integer> needed = new LinkedHashMap<Item, Integer>();
        for (Map.Entry<Item, Integer> entry : allGoods.entrySet()) {
            int deficit;
            Item item = entry.getKey();
            int required = entry.getValue();
            int inHome = homeInv.getCount((Level)context.level(), item);
            if (inHome >= required / 2 || (deficit = required - inHome - villagerInv.getCount(item)) <= 0) continue;
            needed.put(item, deficit);
        }
        if (needed.isEmpty()) {
            return List.of();
        }
        for (BuildingInstance building : context.village().getBuildings()) {
            if (!building.isOperational() || building.getId().equals(homeId) || building.getInventory() == null) continue;
            ArrayList<ItemAmount> fromThisBuilding = new ArrayList<ItemAmount>();
            for (Map.Entry ne : needed.entrySet()) {
                int available = GoodAvailabilityHelper.nbGoodAvailable(building, (Item)ne.getKey(), context.level(), context.village(), context.village().getCultureId(), false);
                if (available <= 0) continue;
                fromThisBuilding.add(new ItemAmount((Item)ne.getKey(), Math.min((Integer)ne.getValue(), available), building.getId()));
            }
            if (fromThisBuilding.isEmpty()) continue;
            return fromThisBuilding;
        }
        return List.of();
    }

    static class GetGoodsForHouseholdTask
    extends AbstractTransportTask {
        private static final int HOUSEHOLD_ACTION_DURATION = 10;
        private final List<ItemAmount> itemsToGet;
        @Nullable
        private final BuildingId sourceBuildingId;

        GetGoodsForHouseholdTask(List<ItemAmount> itemsToGet) {
            this.itemsToGet = itemsToGet;
            BuildingId buildingId = this.sourceBuildingId = itemsToGet.isEmpty() ? null : itemsToGet.getFirst().sourceBuilding();
            if (itemsToGet.isEmpty()) {
                this.state = AbstractTransportTask.State.WALKING_TO_DEST;
            }
        }

        @Override
        public ResourceLocation goalId() {
            return ID;
        }

        @Override
        @Nullable
        protected BuildingInstance resolveSourceBuilding(GoalContext ctx) {
            return this.sourceBuildingId != null ? ctx.village().getBuilding(this.sourceBuildingId) : null;
        }

        @Override
        @Nullable
        protected BuildingInstance resolveDestBuilding(GoalContext ctx) {
            BuildingId homeId = ctx.villager().getHomeBuilding();
            return homeId != null ? ctx.village().getBuilding(homeId) : null;
        }

        protected List<ItemAmount> getItemsToTransfer() {
            return this.itemsToGet;
        }

        @Override
        protected String pickupLogLabel() {
            return "for household";
        }

        @Override
        protected String deliveryLogLabel() {
            return "to household";
        }

        @Override
        protected void tickPickingUp(GoalContext ctx) {
            ++this.actionTicks;
            if (this.actionTicks < 10) {
                return;
            }
            VillagerInventory villagerInv = ctx.villager().getInventory();
            int picked = 0;
            for (ItemAmount ia : this.itemsToGet) {
                int removed;
                int available;
                int toTake;
                BuildingInstance source = ctx.village().getBuilding(ia.sourceBuilding());
                if (source == null || source.getInventory() == null || (toTake = Math.min(ia.count, available = GoodAvailabilityHelper.nbGoodAvailable(source, ia.item, ctx.level(), ctx.village(), ctx.village().getCultureId(), false))) <= 0 || (removed = source.getInventory().remove((Level)ctx.level(), ia.item, toTake)) <= 0) continue;
                villagerInv.add(ia.item, removed);
                picked += removed;
            }
            if (picked > 0) {
                this.hasPickedUpGoods = true;
                this.reportProgress();
            }
            this.state = AbstractTransportTask.State.WALKING_TO_DEST;
            this.actionTicks = 0;
        }

        @Override
        protected void tickDelivering(GoalContext ctx) {
            ++this.actionTicks;
            if (this.actionTicks < 10) {
                return;
            }
            BuildingInstance dest = this.resolveDestBuilding(ctx);
            if (dest == null || dest.getInventory() == null) {
                this.state = AbstractTransportTask.State.DONE;
                return;
            }
            VillagerInventory villagerInv = ctx.villager().getInventory();
            BuildingInventory destInv = dest.getInventory();
            int delivered = 0;
            for (ItemAmount ia : this.itemsToGet) {
                int added;
                int has = villagerInv.getCount(ia.item);
                if (has <= 0 || (added = destInv.add((Level)ctx.level(), ia.item, has)) <= 0) continue;
                villagerInv.remove(ia.item, added);
                delivered += added;
            }
            Map<Item, Integer> allGoods = this.collectHouseholdRequiredGoods(ctx);
            for (Map.Entry<Item, Integer> entry : allGoods.entrySet()) {
                int added;
                int has;
                Item item = entry.getKey();
                boolean alreadyHandled = false;
                for (ItemAmount ia : this.itemsToGet) {
                    if (!ia.item.equals((Object)item)) continue;
                    alreadyHandled = true;
                    break;
                }
                if (alreadyHandled || (has = villagerInv.getCount(item)) <= 0 || (added = destInv.add((Level)ctx.level(), item, has)) <= 0) continue;
                villagerInv.remove(item, added);
                delivered += added;
            }
            if (delivered > 0) {
                LOGGER.debug("[Millenaire] {} delivered {} items to household", (Object)ctx.villager().getVillagerTypeId(), (Object)delivered);
                this.reportProgress();
            }
            this.hasPickedUpGoods = false;
            this.state = AbstractTransportTask.State.DONE;
        }

        private Map<Item, Integer> collectHouseholdRequiredGoods(GoalContext context) {
            return GetGoodsForHouseholdGoal.collectHouseholdRequiredGoods(context);
        }

        @Override
        @Nullable
        public Component getGoalLabel() {
            return switch (this.state) {
                default -> throw new MatchException(null, null);
                case AbstractTransportTask.State.WALKING_TO_SOURCE -> Component.translatable((String)"goal.millenaire.get_goods_household.to_townhall");
                case AbstractTransportTask.State.PICKING_UP -> Component.translatable((String)"goal.millenaire.get_goods_household.picking_up");
                case AbstractTransportTask.State.WALKING_TO_DEST -> Component.translatable((String)"goal.millenaire.get_goods_household.to_home");
                case AbstractTransportTask.State.DELIVERING -> Component.translatable((String)"goal.millenaire.get_goods_household.delivering");
                case AbstractTransportTask.State.DONE -> null;
            };
        }
    }

    record ItemAmount(Item item, int count, BuildingId sourceBuilding) {
    }
}

