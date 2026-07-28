/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.registries.BuiltInRegistries
 *  net.minecraft.network.chat.Component
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.item.ItemEntity
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.phys.AABB
 *  org.slf4j.Logger
 */
package org.millenaire.goal.impl;

import com.mojang.logging.LogUtils;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.millenaire.TickConstants;
import org.millenaire.culture.ModCultures;
import org.millenaire.culture.VillagerType;
import org.millenaire.entity.VillagerNavDriver;
import org.millenaire.goal.GoalContext;
import org.millenaire.goal.PerVillagerThrottle;
import org.millenaire.goal.ProgressAwareTask;
import org.millenaire.goal.StopReason;
import org.millenaire.goal.TravelPhase;
import org.millenaire.goal.VillagerGoal;
import org.millenaire.goal.VillagerTask;
import org.slf4j.Logger;

public class GatherGoodsGoal
implements VillagerGoal {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)"gather_goods");
    private static final int PRIORITY = 500;
    static final double SCAN_RADIUS_H = 20.0;
    static final double SCAN_RADIUS_V = 10.0;
    private final PerVillagerThrottle throttle = new PerVillagerThrottle(100);

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
        return 500;
    }

    @Override
    public boolean canStart(GoalContext context) {
        VillagerType vtype = ModCultures.getVillagerType(context.villager().getVillagerTypeId());
        if (vtype == null || vtype.resolvedCollectGoods().isEmpty()) {
            return false;
        }
        if (TickConstants.isNight((Level)context.level())) {
            return false;
        }
        long currentTick = context.level().getServer().getTickCount();
        if (!this.throttle.shouldEvaluate(context.villager().getUUID(), currentTick)) {
            return false;
        }
        return GatherGoodsGoal.findClosestItem(context, vtype) != null;
    }

    @Override
    public VillagerTask start(GoalContext context) {
        VillagerType vtype = ModCultures.getVillagerType(context.villager().getVillagerTypeId());
        ItemEntity target = vtype != null ? GatherGoodsGoal.findClosestItem(context, vtype) : null;
        return new GatherGoodsTask(target);
    }

    @Nullable
    static ItemEntity findClosestItem(GoalContext context, VillagerType vtype) {
        AABB scanBox = context.villager().getBoundingBox().inflate(20.0, 10.0, 20.0);
        List items = context.level().getEntitiesOfClass(ItemEntity.class, scanBox);
        ItemEntity closest = null;
        double closestDistSq = Double.MAX_VALUE;
        for (ItemEntity itemEntity : items) {
            double distSq;
            if (itemEntity.isRemoved()) continue;
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey((Object)itemEntity.getItem().getItem());
            if (!vtype.resolvedCollectGoods().contains(itemId) || !((distSq = context.villager().distanceToSqr((Entity)itemEntity)) < closestDistSq)) continue;
            closestDistSq = distSq;
            closest = itemEntity;
        }
        return closest;
    }

    static class GatherGoodsTask
    extends ProgressAwareTask {
        private static final double ARRIVE_DISTANCE = 5.0;
        private static final double WALK_SPEED = 0.5;
        private static final int WAIT_DURATION = 40;
        private static final int STUCK_ITEM_TELEPORT_TICKS = 200;
        private State state;
        @Nullable
        private ItemEntity targetItem;
        private int waitTicks;
        private int totalTicks;

        GatherGoodsTask(@Nullable ItemEntity targetItem) {
            this.targetItem = targetItem;
            this.state = targetItem != null ? State.WALKING : State.DONE;
        }

        @Override
        public ResourceLocation goalId() {
            return ID;
        }

        @Override
        public void tick(GoalContext ctx) {
            ++this.totalTicks;
            switch (this.state.ordinal()) {
                case 0: {
                    this.tickWalking(ctx);
                    break;
                }
                case 1: {
                    this.tickWaiting(ctx);
                    break;
                }
            }
        }

        private void tickWalking(GoalContext ctx) {
            if (this.targetItem == null || this.targetItem.isRemoved()) {
                VillagerType vtype = ModCultures.getVillagerType(ctx.villager().getVillagerTypeId());
                if (vtype != null) {
                    this.targetItem = GatherGoodsGoal.findClosestItem(ctx, vtype);
                }
                if (this.targetItem == null || this.targetItem.isRemoved()) {
                    this.state = State.DONE;
                    return;
                }
                ctx.villager().getNavManager().stop(ctx.villager());
            }
            BlockPos targetPos = this.targetItem.blockPosition();
            VillagerNavDriver nav = ctx.villager().getNavManager();
            if (nav.getDestination() == null) {
                nav.navigateTo(ctx.villager(), targetPos, 0.5);
            }
            if (nav.isArrived(ctx.villager(), 5.0)) {
                nav.stop(ctx.villager());
                this.state = State.WAITING;
                this.waitTicks = 0;
                this.reportProgress();
                return;
            }
            if (nav.isAbandoned() || this.totalTicks >= 200) {
                nav.stop(ctx.villager());
                this.doStuckItemTeleport(ctx);
                return;
            }
        }

        private void tickWaiting(GoalContext ctx) {
            ++this.waitTicks;
            if (this.waitTicks >= 40) {
                this.state = State.DONE;
                this.reportProgress();
            }
        }

        private void doStuckItemTeleport(GoalContext ctx) {
            if (this.targetItem != null && !this.targetItem.isRemoved()) {
                ResourceLocation itemId = BuiltInRegistries.ITEM.getKey((Object)this.targetItem.getItem().getItem());
                ctx.villager().getInventory().add(this.targetItem.getItem().getItem(), 1);
                this.targetItem.discard();
                LOGGER.debug("[Millenaire] {} stuck \u2014 force-collected 1x {} (item teleport)", (Object)ctx.villager().getVillagerTypeId(), (Object)itemId);
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
            return this.state == State.WALKING ? TravelPhase.TRAVELLING : TravelPhase.AT_DESTINATION;
        }

        @Override
        @Nullable
        public Component getGoalLabel() {
            return Component.translatable((String)"goal.millenaire.gather_goods");
        }

        private static enum State {
            WALKING,
            WAITING,
            DONE;

        }
    }
}

