/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.level.Level
 *  org.slf4j.Logger
 */
package org.millenaire.goal.impl;

import com.mojang.logging.LogUtils;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import org.millenaire.building.BuildingInstance;
import org.millenaire.building.BuildingInventory;
import org.millenaire.entity.VillagerInventory;
import org.millenaire.entity.VillagerNavDriver;
import org.millenaire.goal.GoalContext;
import org.millenaire.goal.ProgressAwareTask;
import org.millenaire.goal.StopReason;
import org.millenaire.goal.TravelPhase;
import org.slf4j.Logger;

abstract class AbstractTransportTask
extends ProgressAwareTask {
    private static final Logger LOGGER = LogUtils.getLogger();
    static final double ARRIVE_DISTANCE = 3.0;
    static final double WALK_SPEED = 0.5;
    static final int ACTION_DURATION = 40;
    protected State state = State.WALKING_TO_SOURCE;
    protected int actionTicks;
    protected boolean hasPickedUpGoods;

    AbstractTransportTask() {
    }

    @Nullable
    protected abstract BuildingInstance resolveSourceBuilding(GoalContext var1);

    @Nullable
    protected abstract BuildingInstance resolveDestBuilding(GoalContext var1);

    protected abstract List<? extends ItemRef> getItemsToTransfer();

    protected abstract String pickupLogLabel();

    protected abstract String deliveryLogLabel();

    @Override
    public void tick(GoalContext ctx) {
        switch (this.state.ordinal()) {
            case 0: {
                this.tickWalkingToSource(ctx);
                break;
            }
            case 1: {
                this.tickPickingUp(ctx);
                break;
            }
            case 2: {
                this.tickWalkingToDest(ctx);
                break;
            }
            case 3: {
                this.tickDelivering(ctx);
                break;
            }
        }
    }

    private void tickWalkingToSource(GoalContext ctx) {
        BlockPos target;
        BuildingInstance source = this.resolveSourceBuilding(ctx);
        BlockPos blockPos = target = source != null ? source.getSellingPos() : null;
        if (target == null) {
            this.state = State.DONE;
            return;
        }
        VillagerNavDriver nav = ctx.villager().getNavManager();
        if (nav.getDestination() == null) {
            nav.navigateTo(ctx.villager(), target, 0.5);
        }
        if (nav.isArrivedSameFloor(ctx.villager(), 3.0)) {
            nav.stop(ctx.villager());
            this.state = State.PICKING_UP;
            this.actionTicks = 0;
            this.reportProgress();
            return;
        }
        if (nav.isAbandoned()) {
            this.state = State.DONE;
            return;
        }
    }

    protected void tickPickingUp(GoalContext ctx) {
        ++this.actionTicks;
        if (this.actionTicks < 40) {
            return;
        }
        BuildingInstance source = this.resolveSourceBuilding(ctx);
        if (source == null || source.getInventory() == null) {
            this.state = State.DONE;
            return;
        }
        VillagerInventory villagerInv = ctx.villager().getInventory();
        BuildingInventory sourceInv = source.getInventory();
        int picked = 0;
        for (ItemRef itemRef : this.getItemsToTransfer()) {
            int removed = sourceInv.remove((Level)ctx.level(), itemRef.item(), itemRef.count());
            if (removed <= 0) continue;
            villagerInv.add(itemRef.item(), removed);
            picked += removed;
        }
        if (picked > 0) {
            this.hasPickedUpGoods = true;
            LOGGER.debug("[Millenaire] {} picked up {} items {}", new Object[]{ctx.villager().getVillagerTypeId(), picked, this.pickupLogLabel()});
            this.reportProgress();
        }
        this.state = State.WALKING_TO_DEST;
        this.actionTicks = 0;
    }

    private void tickWalkingToDest(GoalContext ctx) {
        BlockPos target;
        BuildingInstance dest = this.resolveDestBuilding(ctx);
        BlockPos blockPos = target = dest != null ? dest.getSellingPos() : null;
        if (target == null) {
            this.state = State.DONE;
            return;
        }
        VillagerNavDriver nav = ctx.villager().getNavManager();
        if (nav.getDestination() == null) {
            nav.navigateTo(ctx.villager(), target, 0.5);
        }
        if (nav.isArrivedSameFloor(ctx.villager(), 3.0)) {
            nav.stop(ctx.villager());
            this.state = State.DELIVERING;
            this.actionTicks = 0;
            this.reportProgress();
            return;
        }
        if (nav.isAbandoned()) {
            this.state = State.DONE;
            return;
        }
    }

    protected void tickDelivering(GoalContext ctx) {
        ++this.actionTicks;
        if (this.actionTicks < 40) {
            return;
        }
        BuildingInstance dest = this.resolveDestBuilding(ctx);
        if (dest == null || dest.getInventory() == null) {
            this.state = State.DONE;
            return;
        }
        VillagerInventory villagerInv = ctx.villager().getInventory();
        BuildingInventory destInv = dest.getInventory();
        int delivered = 0;
        for (ItemRef itemRef : this.getItemsToTransfer()) {
            int added;
            int has = villagerInv.getCount(itemRef.item());
            if (has <= 0 || (added = destInv.add((Level)ctx.level(), itemRef.item(), has)) <= 0) continue;
            villagerInv.remove(itemRef.item(), added);
            delivered += added;
        }
        if (delivered > 0) {
            LOGGER.debug("[Mill\u00e9naire] {} a livr\u00e9 {} items {}", new Object[]{ctx.villager().getVillagerTypeId(), delivered, this.deliveryLogLabel()});
            this.reportProgress();
        }
        this.hasPickedUpGoods = false;
        this.state = State.DONE;
    }

    @Override
    public boolean isFinished() {
        return this.state == State.DONE;
    }

    @Override
    public void stop(GoalContext ctx, StopReason reason) {
        BuildingInstance source;
        if (ctx == null) {
            return;
        }
        ctx.villager().getNavManager().stop(ctx.villager());
        if (this.hasPickedUpGoods && (source = this.resolveSourceBuilding(ctx)) != null && source.getInventory() != null) {
            VillagerInventory villagerInv = ctx.villager().getInventory();
            BuildingInventory sourceInv = source.getInventory();
            int returned = 0;
            for (ItemRef itemRef : this.getItemsToTransfer()) {
                int added;
                int has = villagerInv.getCount(itemRef.item());
                if (has <= 0 || (added = sourceInv.add((Level)ctx.level(), itemRef.item(), has)) <= 0) continue;
                villagerInv.remove(itemRef.item(), added);
                returned += added;
            }
            if (returned > 0) {
                LOGGER.debug("[Millenaire] {} interrupted \u2014 {} items returned to source building", (Object)ctx.villager().getVillagerTypeId(), (Object)returned);
            }
        }
    }

    @Override
    public TravelPhase getTravelPhase() {
        return switch (this.state.ordinal()) {
            case 0, 2 -> TravelPhase.TRAVELLING;
            default -> TravelPhase.AT_DESTINATION;
        };
    }

    protected static final class State
    extends Enum<State> {
        public static final /* enum */ State WALKING_TO_SOURCE = new State();
        public static final /* enum */ State PICKING_UP = new State();
        public static final /* enum */ State WALKING_TO_DEST = new State();
        public static final /* enum */ State DELIVERING = new State();
        public static final /* enum */ State DONE = new State();
        private static final /* synthetic */ State[] $VALUES;

        public static State[] values() {
            return (State[])$VALUES.clone();
        }

        public static State valueOf(String name) {
            return Enum.valueOf(State.class, name);
        }

        private static /* synthetic */ State[] $values() {
            return new State[]{WALKING_TO_SOURCE, PICKING_UP, WALKING_TO_DEST, DELIVERING, DONE};
        }

        static {
            $VALUES = State.$values();
        }
    }

    static interface ItemRef {
        public Item item();

        public int count();
    }
}

