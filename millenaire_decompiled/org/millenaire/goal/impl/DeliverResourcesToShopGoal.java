/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 *  net.minecraft.network.chat.Component
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.ItemLike
 *  net.minecraft.world.level.Level
 *  org.slf4j.Logger
 */
package org.millenaire.goal.impl;

import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import org.millenaire.building.BuildingId;
import org.millenaire.building.BuildingInstance;
import org.millenaire.building.BuildingInventory;
import org.millenaire.building.BuildingPlan;
import org.millenaire.commerce.ShopProfile;
import org.millenaire.commerce.ShopProfileLoader;
import org.millenaire.culture.ModCultures;
import org.millenaire.entity.VillagerInventory;
import org.millenaire.goal.GoalContext;
import org.millenaire.goal.PerVillagerThrottle;
import org.millenaire.goal.TravelPhase;
import org.millenaire.goal.VillagerGoal;
import org.millenaire.goal.VillagerTask;
import org.millenaire.goal.impl.WalkThenActTask;
import org.millenaire.item.ItemHelper;
import org.slf4j.Logger;

public class DeliverResourcesToShopGoal
implements VillagerGoal {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)"deliver_resources_shop");
    private static final int STANDARD_DELAY = 2000;
    private static final int IMMEDIATE_THRESHOLD = 16;
    private final PerVillagerThrottle throttle = new PerVillagerThrottle(2000);

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public int computePriority(GoalContext context) {
        VillagerInventory inv = context.villager().getInventory();
        int priority = 0;
        ResourceLocation cultureId = context.village().getCultureId();
        for (BuildingInstance shop : context.village().getBuildings()) {
            ShopProfile profile;
            BuildingPlan plan;
            if (!shop.isOperational() || (plan = ModCultures.getBuildingPlan(shop.getPlanId())) == null || plan.shopId() == null || (profile = ShopProfileLoader.getProfile(cultureId, plan.shopId())) == null || profile.deliverTo().isEmpty()) continue;
            for (String itemName : profile.deliverTo()) {
                Item item = ItemHelper.resolve(itemName);
                if (item == null) continue;
                priority += inv.getCount(item) * 10;
            }
        }
        return priority;
    }

    @Override
    public boolean canStart(GoalContext context) {
        if (context.resolveHomeBuilding().isEmpty()) {
            return false;
        }
        DeliveryTarget target = this.findDeliveryTarget(context);
        if (target == null) {
            return false;
        }
        long currentTick = context.gameTime();
        if (target.totalCount > 16) {
            this.throttle.record(context.villager().getUUID(), currentTick);
            return true;
        }
        return this.throttle.shouldEvaluate(context.villager().getUUID(), currentTick);
    }

    @Override
    public VillagerTask start(GoalContext context) {
        DeliveryTarget target = this.findDeliveryTarget(context);
        if (target == null) {
            return new DeliverTask(null, List.of());
        }
        return new DeliverTask(target.shopId, target.items);
    }

    @Nullable
    private DeliveryTarget findDeliveryTarget(GoalContext context) {
        VillagerInventory inv = context.villager().getInventory();
        ResourceLocation cultureId = context.village().getCultureId();
        BuildingInstance townhall = context.village().getTownhall();
        BuildingId townhallId = townhall != null ? townhall.getId() : null;
        ArrayList<DeliveryTarget> candidates = new ArrayList<DeliveryTarget>();
        for (BuildingInstance shop : context.village().getBuildings()) {
            ShopProfile profile;
            BuildingPlan plan;
            if (!shop.isOperational() || (plan = ModCultures.getBuildingPlan(shop.getPlanId())) == null || plan.shopId() == null || (profile = ShopProfileLoader.getProfile(cultureId, plan.shopId())) == null || profile.deliverTo().isEmpty()) continue;
            ArrayList<ItemCount> carriedItems = new ArrayList<ItemCount>();
            int totalCount = 0;
            for (String itemName : profile.deliverTo()) {
                int count;
                Item item = ItemHelper.resolve(itemName);
                if (item == null || (count = inv.getCount(item)) <= 0) continue;
                carriedItems.add(new ItemCount(item, count));
                totalCount += count;
            }
            if (carriedItems.isEmpty()) continue;
            candidates.add(new DeliveryTarget(shop.getId(), carriedItems, totalCount));
        }
        return DeliverResourcesToShopGoal.preferNonTownhall(candidates, t -> townhallId != null && townhallId.equals(t.shopId()));
    }

    @Nullable
    static <T> T preferNonTownhall(List<T> candidates, Predicate<T> isTownhall) {
        T townhallFallback = null;
        for (T c : candidates) {
            if (isTownhall.test(c)) {
                if (townhallFallback != null) continue;
                townhallFallback = c;
                continue;
            }
            return c;
        }
        return townhallFallback;
    }

    private record DeliveryTarget(BuildingId shopId, List<ItemCount> items, int totalCount) {
    }

    static class DeliverTask
    extends WalkThenActTask {
        @Nullable
        private final BuildingId shopId;
        private final List<ItemCount> itemsToDeliver;

        DeliverTask(@Nullable BuildingId shopId, List<ItemCount> itemsToDeliver) {
            this.shopId = shopId;
            this.itemsToDeliver = itemsToDeliver;
            if (shopId == null || itemsToDeliver.isEmpty()) {
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
            BuildingInstance shop = this.shopId != null ? ctx.village().getBuilding(this.shopId) : null;
            return shop != null ? shop.getSellingPos() : null;
        }

        @Override
        protected void performAction(GoalContext ctx) {
            BuildingInstance shop;
            BuildingInstance buildingInstance = shop = this.shopId != null ? ctx.village().getBuilding(this.shopId) : null;
            if (shop == null || shop.getInventory() == null) {
                return;
            }
            VillagerInventory villagerInv = ctx.villager().getInventory();
            BuildingInventory shopInv = shop.getInventory();
            int delivered = 0;
            for (ItemCount ic : this.itemsToDeliver) {
                int has = villagerInv.getCount(ic.item);
                if (has <= 0) continue;
                int toDeliver = Math.min(has, 256);
                int added = shopInv.add((Level)ctx.level(), ic.item, toDeliver);
                if (added <= 0) continue;
                villagerInv.remove(ic.item, added);
                delivered += added;
            }
            if (delivered > 0) {
                LOGGER.debug("[Millenaire] {} delivered {} items to shop", (Object)ctx.villager().getVillagerTypeId(), (Object)delivered);
                this.reportProgress();
            }
        }

        @Override
        public List<ItemStack> getHeldItems(TravelPhase phase) {
            if (phase != TravelPhase.TRAVELLING) {
                return List.of();
            }
            ArrayList<ItemStack> items = new ArrayList<ItemStack>();
            for (ItemCount ic : this.itemsToDeliver) {
                items.add(new ItemStack((ItemLike)ic.item, 1));
            }
            return items;
        }

        @Override
        @Nullable
        public Component getGoalLabel() {
            return switch (this.phase) {
                default -> throw new MatchException(null, null);
                case WalkThenActTask.Phase.WALKING -> Component.translatable((String)"goal.millenaire.transport.to_dest");
                case WalkThenActTask.Phase.ACTING -> Component.translatable((String)"goal.millenaire.transport.delivering");
                case WalkThenActTask.Phase.DONE -> null;
            };
        }
    }

    private record ItemCount(Item item, int count) {
    }
}

