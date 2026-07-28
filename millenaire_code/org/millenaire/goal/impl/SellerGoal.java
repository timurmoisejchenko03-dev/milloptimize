/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 *  net.minecraft.network.chat.Component
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.ItemLike
 *  net.minecraft.world.level.Level
 *  org.slf4j.Logger
 */
package org.millenaire.goal.impl;

import com.mojang.logging.LogUtils;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import org.millenaire.TickConstants;
import org.millenaire.building.BuildingId;
import org.millenaire.entity.VillagerNavDriver;
import org.millenaire.goal.GoalContext;
import org.millenaire.goal.StopReason;
import org.millenaire.goal.TaskLabels;
import org.millenaire.goal.TravelPhase;
import org.millenaire.goal.VillagerGoal;
import org.millenaire.goal.VillagerTask;
import org.millenaire.item.ModItems;
import org.millenaire.village.VillagerAnnouncementHelper;
import org.slf4j.Logger;

public class SellerGoal
implements VillagerGoal {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)"be_seller");

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public int computePriority(GoalContext context) {
        return 0;
    }

    @Override
    public boolean canStart(GoalContext context) {
        return false;
    }

    @Override
    public VillagerTask start(GoalContext context) {
        LOGGER.warn("[Millenaire] SellerGoal.start() called via scheduler \u2014 this should be forced via forceTask()");
        return new SellerTask(BuildingId.random(), context.villager().blockPosition());
    }

    public static class SellerTask
    implements VillagerTask {
        private static final double ARRIVE_DISTANCE = 2.0;
        private static final double WALK_SPEED = 0.5;
        private static final double SELLING_RADIUS = 7.0;
        private final BuildingId shopBuildingId;
        private final BlockPos sellingPos;
        private State state = State.WALKING_TO_SELLING_POS;
        private boolean hasTraded = false;

        public SellerTask(BuildingId shopBuildingId, BlockPos sellingPos) {
            this.shopBuildingId = shopBuildingId;
            this.sellingPos = sellingPos;
        }

        public void markTraded() {
            this.hasTraded = true;
        }

        @Override
        public ResourceLocation goalId() {
            return ID;
        }

        @Override
        public void tick(GoalContext ctx) {
            if (SellerTask.shouldAbort(this.state == State.DONE, TickConstants.isNight((Level)ctx.level()), this.isPlayerInRange(ctx))) {
                this.state = State.DONE;
                return;
            }
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

        static boolean shouldAbort(boolean alreadyFinished, boolean isNight, boolean playerInRange) {
            return !alreadyFinished && (isNight || !playerInRange);
        }

        private boolean isPlayerInRange(GoalContext ctx) {
            return ctx.level().getNearestPlayer((double)this.sellingPos.getX() + 0.5, (double)this.sellingPos.getY() + 0.5, (double)this.sellingPos.getZ() + 0.5, 7.0, false) != null;
        }

        private void tickWalking(GoalContext ctx) {
            VillagerNavDriver nav = ctx.villager().getNavManager();
            if (nav.getDestination() == null) {
                nav.navigateTo(ctx.villager(), this.sellingPos, 0.5);
            }
            if (nav.isArrivedSameFloor(ctx.villager(), 2.0)) {
                nav.stop(ctx.villager());
                this.state = State.WAITING_FOR_PLAYER;
                ctx.villager().setSelling(true);
                LOGGER.debug("[Millenaire] Seller {} arrived at counter at {}", (Object)ctx.villager().getVillagerTypeId(), (Object)this.sellingPos.toShortString());
                return;
            }
            if (nav.isAbandoned()) {
                nav.stop(ctx.villager());
                this.state = State.DONE;
                return;
            }
        }

        private void tickWaiting(GoalContext ctx) {
            Player nearest = ctx.level().getNearestPlayer((double)this.sellingPos.getX() + 0.5, (double)this.sellingPos.getY() + 0.5, (double)this.sellingPos.getZ() + 0.5, 7.0, false);
            if (nearest != null) {
                ctx.villager().getLookControl().setLookAt((Entity)nearest, 30.0f, 30.0f);
            }
        }

        @Override
        public boolean isFinished() {
            return this.state == State.DONE;
        }

        @Override
        public void stop(GoalContext ctx, StopReason reason) {
            Player nearest;
            if (ctx == null) {
                return;
            }
            ctx.villager().setSelling(false);
            ctx.villager().getNavManager().stop(ctx.villager());
            if (this.hasTraded && (nearest = ctx.level().getNearestPlayer((double)this.sellingPos.getX() + 0.5, (double)this.sellingPos.getY() + 0.5, (double)this.sellingPos.getZ() + 0.5, 14.0, false)) instanceof ServerPlayer) {
                ServerPlayer sp = (ServerPlayer)nearest;
                VillagerAnnouncementHelper.sendAnnouncement(ctx.villager(), sp, "tradecomplete", "message.millenaire.trade_complete");
            }
            LOGGER.debug("[Millenaire] Seller {} finished trading (hasTraded={})", (Object)ctx.villager().getVillagerTypeId(), (Object)this.hasTraded);
        }

        @Override
        public List<ItemStack> getHeldItems(TravelPhase phase) {
            if (phase == TravelPhase.AT_DESTINATION) {
                return List.of(new ItemStack((ItemLike)ModItems.DENIER.get()));
            }
            return List.of();
        }

        @Override
        public List<ItemStack> getOffHandItems(TravelPhase phase) {
            if (phase == TravelPhase.AT_DESTINATION) {
                return List.of(new ItemStack((ItemLike)ModItems.PURSE.get()));
            }
            return List.of();
        }

        @Override
        public TravelPhase getTravelPhase() {
            return TaskLabels.phaseFor(this.state == State.WAITING_FOR_PLAYER);
        }

        @Override
        @Nullable
        public Component getGoalLabel() {
            return this.state == State.DONE ? null : TaskLabels.labelForPhase(this.state == State.WAITING_FOR_PLAYER, "be_seller");
        }

        public boolean isWaiting() {
            return this.state == State.WAITING_FOR_PLAYER;
        }

        public BuildingId getShopBuildingId() {
            return this.shopBuildingId;
        }

        public BlockPos getSellingPos() {
            return this.sellingPos;
        }

        private static enum State {
            WALKING_TO_SELLING_POS,
            WAITING_FOR_PLAYER,
            DONE;

        }
    }
}

