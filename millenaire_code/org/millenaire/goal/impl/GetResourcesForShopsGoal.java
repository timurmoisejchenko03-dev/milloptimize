/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 *  net.minecraft.network.chat.Component
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.level.Level
 *  org.slf4j.Logger
 */
package org.millenaire.goal.impl;

import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import org.millenaire.building.BuildingId;
import org.millenaire.building.BuildingInstance;
import org.millenaire.building.BuildingInventory;
import org.millenaire.building.BuildingPlan;
import org.millenaire.building.GoodAvailabilityHelper;
import org.millenaire.commerce.ShopProfile;
import org.millenaire.commerce.ShopProfileLoader;
import org.millenaire.culture.ModCultures;
import org.millenaire.entity.VillagerInventory;
import org.millenaire.goal.GoalContext;
import org.millenaire.goal.PerVillagerThrottle;
import org.millenaire.goal.VillagerGoal;
import org.millenaire.goal.VillagerTask;
import org.millenaire.goal.impl.WalkThenActTask;
import org.millenaire.item.ItemHelper;
import org.slf4j.Logger;

public class GetResourcesForShopsGoal
implements VillagerGoal {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)"get_resources_for_shops");
    private static final int STANDARD_DELAY = 2000;
    private static final int IMMEDIATE_THRESHOLD = 16;
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
        return this.computeRawPriorityCount(context) * 5;
    }

    private int computeRawPriorityCount(GoalContext context) {
        ResourceLocation cultureId = context.village().getCultureId();
        BuildingInstance home = context.resolveHomeBuilding().orElse(null);
        BuildingInstance townhall = context.village().getTownhall();
        int total = 0;
        for (BuildingInstance shop : context.village().getBuildings()) {
            ShopProfile profile;
            BuildingPlan plan;
            if (!shop.isOperational() || (plan = ModCultures.getBuildingPlan(shop.getPlanId())) == null || plan.shopId() == null || (profile = ShopProfileLoader.getProfile(cultureId, plan.shopId())) == null || profile.deliverTo().isEmpty()) continue;
            for (String itemName : profile.deliverTo()) {
                Item item = ItemHelper.resolve(itemName);
                if (item == null) continue;
                if (home != null && home.getInventory() != null && !shop.getId().equals(home.getId())) {
                    total += home.getInventory().getCount((Level)context.level(), item);
                }
                if (townhall == null || townhall.getInventory() == null || shop.getId().equals(townhall.getId()) || home != null && townhall.getId().equals(home.getId())) continue;
                total += townhall.getInventory().getCount((Level)context.level(), item);
            }
        }
        return total;
    }

    @Override
    public boolean canStart(GoalContext context) {
        if (context.resolveHomeBuilding().isEmpty()) {
            return false;
        }
        if (this.isAlreadyCarryingShopGoods(context)) {
            return false;
        }
        List<PickupRequest> requests = this.computePickupRequests(context);
        if (requests.isEmpty()) {
            return false;
        }
        int totalAvailable = 0;
        for (PickupRequest req : requests) {
            totalAvailable += req.availableCount;
        }
        if (totalAvailable <= 0) {
            return false;
        }
        long currentTick = context.gameTime();
        if (totalAvailable > 16) {
            this.throttle.record(context.villager().getUUID(), currentTick);
            return true;
        }
        return this.throttle.shouldEvaluate(context.villager().getUUID(), currentTick);
    }

    @Override
    public VillagerTask start(GoalContext context) {
        List<PickupRequest> requests = this.computePickupRequests(context);
        if (requests.isEmpty()) {
            return new PickupTask(null, List.of());
        }
        PickupRequest first = requests.get(0);
        return new PickupTask(first.sourceBuilding, first.items);
    }

    private boolean isAlreadyCarryingShopGoods(GoalContext context) {
        VillagerInventory inv = context.villager().getInventory();
        ResourceLocation cultureId = context.village().getCultureId();
        for (BuildingInstance shop : context.village().getBuildings()) {
            ShopProfile profile;
            BuildingPlan plan;
            if (!shop.isOperational() || (plan = ModCultures.getBuildingPlan(shop.getPlanId())) == null || plan.shopId() == null || (profile = ShopProfileLoader.getProfile(cultureId, plan.shopId())) == null || profile.deliverTo().isEmpty()) continue;
            for (String itemName : profile.deliverTo()) {
                Item item = ItemHelper.resolve(itemName);
                if (item == null || inv.getCount(item) <= 0) continue;
                return true;
            }
        }
        return false;
    }

    List<PickupRequest> computePickupRequests(GoalContext context) {
        ArrayList<PickupRequest> requests = new ArrayList<PickupRequest>();
        ResourceLocation cultureId = context.village().getCultureId();
        BuildingInstance home = context.resolveHomeBuilding().orElse(null);
        BuildingInstance townhall = context.village().getTownhall();
        ArrayList<ShopItemNeed> allNeeds = new ArrayList<ShopItemNeed>();
        for (BuildingInstance shop : context.village().getBuildings()) {
            ShopProfile profile;
            BuildingPlan plan;
            if (!shop.isOperational() || (plan = ModCultures.getBuildingPlan(shop.getPlanId())) == null || plan.shopId() == null || (profile = ShopProfileLoader.getProfile(cultureId, plan.shopId())) == null || profile.deliverTo().isEmpty()) continue;
            for (String itemName : profile.deliverTo()) {
                Item item = ItemHelper.resolve(itemName);
                if (item == null) continue;
                allNeeds.add(new ShopItemNeed(item, shop.getId()));
            }
        }
        if (allNeeds.isEmpty()) {
            return requests;
        }
        this.checkSource(home, allNeeds, context, cultureId, townhall, requests);
        this.checkSource(townhall, allNeeds, context, cultureId, townhall, requests);
        return requests;
    }

    private void checkSource(@Nullable BuildingInstance source, List<ShopItemNeed> allNeeds, GoalContext context, ResourceLocation cultureId, @Nullable BuildingInstance townhall, List<PickupRequest> requests) {
        ShopProfile sourceProfile;
        if (source == null || source.getInventory() == null) {
            return;
        }
        boolean sourceIsTownhall = townhall != null && source.getId().equals(townhall.getId());
        BuildingPlan sourcePlan = ModCultures.getBuildingPlan(source.getPlanId());
        boolean sourceIsShop = sourcePlan != null && sourcePlan.shopId() != null;
        List<Item> sourceDeliverTo = List.of();
        if (sourceIsShop && !sourceIsTownhall && (sourceProfile = ShopProfileLoader.getProfile(cultureId, sourcePlan.shopId())) != null && !sourceProfile.deliverTo().isEmpty()) {
            sourceDeliverTo = new ArrayList();
            for (String name : sourceProfile.deliverTo()) {
                Item item = ItemHelper.resolve(name);
                if (item == null) continue;
                sourceDeliverTo.add(item);
            }
        }
        ArrayList<ItemAmount> available = new ArrayList<ItemAmount>();
        int totalAvailable = 0;
        for (ShopItemNeed need : allNeeds) {
            int count;
            if (source.getId().equals(need.shopId) || sourceIsShop && sourceDeliverTo.contains(need.item) || (count = GoodAvailabilityHelper.nbGoodAvailable(source, need.item, context.level(), context.village(), cultureId, true)) <= 0) continue;
            available.add(new ItemAmount(need.item, count));
            totalAvailable += count;
        }
        if (totalAvailable > 0) {
            requests.add(new PickupRequest(source.getId(), available, totalAvailable));
        }
    }

    record PickupRequest(BuildingId sourceBuilding, List<ItemAmount> items, int availableCount) {
    }

    static class PickupTask
    extends WalkThenActTask {
        @Nullable
        private final BuildingId sourceId;
        private final List<ItemAmount> itemsToPickup;

        PickupTask(@Nullable BuildingId sourceId, List<ItemAmount> itemsToPickup) {
            this.sourceId = sourceId;
            this.itemsToPickup = itemsToPickup;
            if (sourceId == null || itemsToPickup.isEmpty()) {
                this.phase = WalkThenActTask.Phase.DONE;
            }
        }

        @Override
        public ResourceLocation goalId() {
            return ID;
        }

        @Override
        @Nullable
        protected BlockPos resolveTarget(GoalContext ctx) {
            BuildingInstance source = this.sourceId != null ? ctx.village().getBuilding(this.sourceId) : null;
            return source != null ? source.getSellingPos() : null;
        }

        @Override
        protected void performAction(GoalContext ctx) {
            BuildingInstance source;
            BuildingInstance buildingInstance = source = this.sourceId != null ? ctx.village().getBuilding(this.sourceId) : null;
            if (source == null || source.getInventory() == null) {
                return;
            }
            VillagerInventory villagerInv = ctx.villager().getInventory();
            BuildingInventory sourceInv = source.getInventory();
            int picked = 0;
            for (ItemAmount ia : this.itemsToPickup) {
                int removed;
                int available = GoodAvailabilityHelper.nbGoodAvailable(source, ia.item, ctx.level(), ctx.village(), ctx.village().getCultureId(), true);
                int toTake = Math.min(ia.count, available);
                if (toTake <= 0 || (removed = sourceInv.remove((Level)ctx.level(), ia.item, toTake)) <= 0) continue;
                villagerInv.add(ia.item, removed);
                picked += removed;
            }
            if (picked > 0) {
                LOGGER.debug("[Millenaire] {} picked up {} items for shops", (Object)ctx.villager().getVillagerTypeId(), (Object)picked);
                this.reportProgress();
            }
        }

        @Override
        @Nullable
        public Component getGoalLabel() {
            return switch (this.phase) {
                default -> throw new MatchException(null, null);
                case WalkThenActTask.Phase.WALKING -> Component.translatable((String)"goal.millenaire.transport.to_source");
                case WalkThenActTask.Phase.ACTING -> Component.translatable((String)"goal.millenaire.transport.picking_up");
                case WalkThenActTask.Phase.DONE -> null;
            };
        }
    }

    private record ShopItemNeed(Item item, BuildingId shopId) {
    }

    record ItemAmount(Item item, int count) {
    }
}

