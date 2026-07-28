/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 *  net.minecraft.network.chat.Component
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.level.Level
 *  org.slf4j.Logger
 */
package org.millenaire.goal.impl;

import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import org.millenaire.building.BuildingId;
import org.millenaire.building.BuildingInstance;
import org.millenaire.building.BuildingInventory;
import org.millenaire.building.BuildingPlan;
import org.millenaire.culture.ModCultures;
import org.millenaire.culture.VillagerType;
import org.millenaire.entity.MillVillager;
import org.millenaire.entity.VillagerInventory;
import org.millenaire.entity.VillagerNavDriver;
import org.millenaire.goal.GoalContext;
import org.millenaire.goal.GoalScheduler;
import org.millenaire.goal.ProgressAwareTask;
import org.millenaire.goal.StopReason;
import org.millenaire.goal.TravelPhase;
import org.millenaire.goal.VillagerGoal;
import org.millenaire.goal.VillagerTask;
import org.millenaire.item.ItemHelper;
import org.millenaire.tool.ToolCategory;
import org.millenaire.tool.ToolCategoryRegistry;
import org.slf4j.Logger;

public class GetToolGoal
implements VillagerGoal {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)"get_tool");
    private static final int PRIORITY = 550;
    private static final int MAX_SIMULTANEOUS_PER_SHOP = 2;

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
        return 550;
    }

    @Override
    public boolean canStart(GoalContext context) {
        VillagerType vType = ModCultures.getVillagerType(context.villager().getVillagerTypeId());
        if (vType == null || vType.toolNeededClasses().isEmpty() && vType.itemsNeeded().isEmpty()) {
            return false;
        }
        List<ToolPickup> upgrades = this.findUpgrades(context, vType);
        if (upgrades.isEmpty()) {
            return false;
        }
        BuildingId targetShop = upgrades.getFirst().shopId;
        return this.countGetToolTasksForShop(context, targetShop) < 2;
    }

    private int countGetToolTasksForShop(GoalContext context, BuildingId shopId) {
        int count = 0;
        for (UUID uuid : context.village().getVillagerUuids()) {
            GetToolTask gtt;
            VillagerTask task;
            GoalScheduler scheduler;
            MillVillager other;
            Entity entity = context.level().getEntity(uuid);
            if (!(entity instanceof MillVillager) || (other = (MillVillager)entity) == context.villager() || (scheduler = other.getGoalScheduler()) == null || !ID.equals((Object)scheduler.getCurrentGoalId()) || !((task = scheduler.getCurrentTask()) instanceof GetToolTask) || !shopId.equals((gtt = (GetToolTask)task).getShopBuildingId())) continue;
            ++count;
        }
        return count;
    }

    @Override
    public VillagerTask start(GoalContext context) {
        List upgrades;
        VillagerType vType = ModCultures.getVillagerType(context.villager().getVillagerTypeId());
        List<Object> list = upgrades = vType != null ? this.findUpgrades(context, vType) : List.of();
        if (upgrades.isEmpty()) {
            return new GetToolTask(List.of(), null);
        }
        BuildingId firstShop = ((ToolPickup)upgrades.getFirst()).shopId;
        List<ToolPickup> fromFirstShop = upgrades.stream().filter(u -> u.shopId.equals(firstShop)).toList();
        return new GetToolTask(fromFirstShop, firstShop);
    }

    private List<ToolPickup> findUpgrades(GoalContext context, VillagerType vType) {
        VillagerInventory villagerInv = context.villager().getInventory();
        ArrayList<ToolPickup> result = new ArrayList<ToolPickup>();
        for (BuildingInstance building : context.village().getBuildings()) {
            BuildingPlan plan = ModCultures.getBuildingPlan(building.getPlanId());
            if (plan == null || plan.shopId() == null || building.getInventory() == null || !building.isOperational()) continue;
            BuildingInventory shopInv = building.getInventory();
            for (ResourceLocation neededId : vType.itemsNeeded()) {
                Item neededItem = ItemHelper.resolve(neededId);
                if (neededItem == null || villagerInv.getCount(neededItem) > 0 || shopInv.getCount((Level)context.level(), neededItem) <= 0) continue;
                result.add(new ToolPickup(building.getId(), neededItem, "itemneeded"));
            }
            for (String categoryId : vType.toolNeededClasses()) {
                ToolCategory.ToolEntry bestOwned;
                ToolCategory.ToolEntry upgrade;
                ToolCategory category = ToolCategoryRegistry.get(categoryId);
                if (category == null || (upgrade = category.findUpgrade(bestOwned = category.getBestOwned(item -> villagerInv.getCount((Item)item) > 0), item -> shopInv.getCount((Level)context.level(), (Item)item) > 0)) == null || upgrade.item() == null) continue;
                result.add(new ToolPickup(building.getId(), upgrade.item(), categoryId));
            }
            if (result.isEmpty()) continue;
            return result;
        }
        return result;
    }

    record ToolPickup(BuildingId shopId, Item item, String categoryId) {
    }

    static class GetToolTask
    extends ProgressAwareTask {
        private static final double ARRIVE_DISTANCE = 3.0;
        private static final double WALK_SPEED = 0.5;
        private static final int ACTION_DURATION = 10;
        private State state;
        private final List<ToolPickup> pickups;
        @Nullable
        private final BuildingId shopBuildingId;
        private int actionTicks;

        @Nullable
        BuildingId getShopBuildingId() {
            return this.shopBuildingId;
        }

        GetToolTask(List<ToolPickup> pickups, @Nullable BuildingId shopBuildingId) {
            this.pickups = pickups;
            this.shopBuildingId = shopBuildingId;
            this.state = pickups.isEmpty() ? State.DONE : State.WALKING_TO_SHOP;
        }

        @Override
        public ResourceLocation goalId() {
            return ID;
        }

        @Override
        public void tick(GoalContext ctx) {
            switch (this.state.ordinal()) {
                case 0: {
                    this.tickWalking(ctx);
                    break;
                }
                case 1: {
                    this.tickPickingUp(ctx);
                    break;
                }
            }
        }

        private void tickWalking(GoalContext ctx) {
            BlockPos target;
            BuildingInstance shop = this.shopBuildingId != null ? ctx.village().getBuilding(this.shopBuildingId) : null;
            BlockPos blockPos = target = shop != null ? shop.getSellingPos() : null;
            if (target == null) {
                this.state = State.DONE;
                return;
            }
            VillagerNavDriver nav = ctx.villager().getNavManager();
            if (nav.getDestination() == null) {
                nav.navigateTo(ctx.villager(), target, 0.5);
            }
            if (nav.isArrived(ctx.villager(), 3.0)) {
                nav.stop(ctx.villager());
                this.state = State.PICKING_UP;
                this.actionTicks = 0;
                this.reportProgress();
                return;
            }
            if (nav.isAbandoned()) {
                nav.stop(ctx.villager());
                this.state = State.DONE;
                return;
            }
        }

        private void tickPickingUp(GoalContext ctx) {
            BuildingInstance shop;
            ++this.actionTicks;
            if (this.actionTicks < 10) {
                return;
            }
            BuildingInstance buildingInstance = shop = this.shopBuildingId != null ? ctx.village().getBuilding(this.shopBuildingId) : null;
            if (shop == null || shop.getInventory() == null) {
                this.state = State.DONE;
                return;
            }
            VillagerInventory villagerInv = ctx.villager().getInventory();
            BuildingInventory shopInv = shop.getInventory();
            for (ToolPickup pickup : this.pickups) {
                int removed = shopInv.remove((Level)ctx.level(), pickup.item, 1);
                if (removed <= 0) continue;
                villagerInv.add(pickup.item, 1);
                LOGGER.debug("[Mill\u00e9naire] {} a r\u00e9cup\u00e9r\u00e9 un outil ({}) au shop", (Object)ctx.villager().getVillagerTypeId(), (Object)pickup.categoryId);
                this.reportProgress();
            }
            this.state = State.DONE;
        }

        @Override
        public boolean isFinished() {
            return this.state == State.DONE;
        }

        @Override
        public void stop(GoalContext ctx, StopReason reason) {
            if (ctx != null) {
                ctx.villager().getNavManager().stop(ctx.villager());
            }
        }

        @Override
        public TravelPhase getTravelPhase() {
            return this.state == State.WALKING_TO_SHOP ? TravelPhase.TRAVELLING : TravelPhase.AT_DESTINATION;
        }

        @Override
        @Nullable
        public Component getGoalLabel() {
            return switch (this.state.ordinal()) {
                default -> throw new MatchException(null, null);
                case 0 -> Component.translatable((String)"goal.millenaire.get_tool.walking");
                case 1 -> Component.translatable((String)"goal.millenaire.get_tool.picking_up");
                case 2 -> null;
            };
        }

        private static final class State
        extends Enum<State> {
            public static final /* enum */ State WALKING_TO_SHOP = new State();
            public static final /* enum */ State PICKING_UP = new State();
            public static final /* enum */ State DONE = new State();
            private static final /* synthetic */ State[] $VALUES;

            public static State[] values() {
                return (State[])$VALUES.clone();
            }

            public static State valueOf(String name) {
                return Enum.valueOf(State.class, name);
            }

            private static /* synthetic */ State[] $values() {
                return new State[]{WALKING_TO_SHOP, PICKING_UP, DONE};
            }

            static {
                $VALUES = State.$values();
            }
        }
    }
}

