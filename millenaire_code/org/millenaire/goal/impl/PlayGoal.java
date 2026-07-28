/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 *  net.minecraft.network.chat.Component
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.Items
 *  net.minecraft.world.level.ItemLike
 *  net.minecraft.world.level.Level
 */
package org.millenaire.goal.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import org.millenaire.building.BuildingInstance;
import org.millenaire.entity.MillVillager;
import org.millenaire.entity.VillagerNavDriver;
import org.millenaire.goal.GoalContext;
import org.millenaire.goal.GoalScheduler;
import org.millenaire.goal.StopReason;
import org.millenaire.goal.TaskLabels;
import org.millenaire.goal.TravelPhase;
import org.millenaire.goal.VillagerGoal;
import org.millenaire.goal.VillagerTask;
import org.millenaire.goal.impl.SocialiseGoal;
import org.millenaire.goal.impl.WalkAndWaitTask;
import org.millenaire.goal.visit.VisitGoalSchema;

public class PlayGoal
implements VillagerGoal {
    private final ResourceLocation id;
    private final boolean withFriends;
    private final long reoccurDelay;

    public PlayGoal(boolean withFriends) {
        this.withFriends = withFriends;
        this.id = ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)(withFriends ? "play_with_friends" : "play"));
        this.reoccurDelay = withFriends ? 1200L : 600L;
    }

    public static PlayGoal fromSchema(VisitGoalSchema.Play s) {
        return new PlayGoal(s.withFriends());
    }

    @Override
    public boolean showInTravelBook() {
        return false;
    }

    @Override
    public ResourceLocation id() {
        return this.id;
    }

    @Override
    public boolean isLeisure() {
        return true;
    }

    @Override
    public long reoccurDelayTicks() {
        return this.reoccurDelay;
    }

    @Override
    public int computePriority(GoalContext context) {
        return 5 + ThreadLocalRandom.current().nextInt(10);
    }

    @Override
    public boolean canStart(GoalContext context) {
        if (this.withFriends) {
            return this.findPlayingFriend(context) != null;
        }
        return this.findLeisurePos(context) != null;
    }

    @Override
    public VillagerTask start(GoalContext context) {
        if (this.withFriends) {
            MillVillager friend = this.findPlayingFriend(context);
            return new PlayWithFriendTask(friend);
        }
        BlockPos target = this.findLeisurePos(context);
        return new PlayTask(target);
    }

    @Nullable
    private MillVillager findPlayingFriend(GoalContext ctx) {
        ArrayList<MillVillager> candidates = new ArrayList<MillVillager>();
        for (UUID uuid : ctx.village().getVillagerUuids()) {
            String path;
            ResourceLocation otherGoalId;
            GoalScheduler scheduler;
            MillVillager other;
            Entity entity = ctx.level().getEntity(uuid);
            if (!(entity instanceof MillVillager) || (other = (MillVillager)entity) == ctx.villager() || (scheduler = other.getGoalScheduler()) == null || (otherGoalId = scheduler.getCurrentGoalId()) == null || !"play".equals(path = otherGoalId.getPath()) && !"play_with_friends".equals(path)) continue;
            candidates.add(other);
        }
        if (candidates.isEmpty()) {
            return null;
        }
        return (MillVillager)((Object)candidates.get(ThreadLocalRandom.current().nextInt(candidates.size())));
    }

    @Nullable
    private BlockPos findLeisurePos(GoalContext ctx) {
        List<BuildingInstance> leisureBuildings = ctx.village().getOperationalBuildingsWithTag("leisure");
        if (leisureBuildings.isEmpty()) {
            return null;
        }
        BuildingInstance chosen = leisureBuildings.get(ThreadLocalRandom.current().nextInt(leisureBuildings.size()));
        BlockPos basePos = chosen.resolveNavigationTarget("leisurePos", "sellingPos", "sleepingPos");
        return SocialiseGoal.findRandomSafePosition((Level)ctx.level(), basePos);
    }

    static class PlayWithFriendTask
    implements VillagerTask {
        private static final double WALK_SPEED = 0.5;
        private static final int MAX_WAIT_TICKS = 200;
        @Nullable
        private final MillVillager friend;
        private boolean arrived;
        private int waitTicks;

        PlayWithFriendTask(@Nullable MillVillager friend) {
            this.friend = friend;
            if (friend == null) {
                this.arrived = true;
            }
        }

        @Override
        public ResourceLocation goalId() {
            return ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)"play_with_friends");
        }

        @Override
        public void tick(GoalContext ctx) {
            if (this.friend != null && !this.friend.isAlive()) {
                this.arrived = true;
                this.waitTicks = 200;
                return;
            }
            if (!this.arrived && this.friend != null) {
                VillagerNavDriver nav = ctx.villager().getNavManager();
                nav.navigateTo(ctx.villager(), this.friend.blockPosition(), 0.5);
                if (nav.isArrived(ctx.villager(), 4.0)) {
                    this.arrived = true;
                    nav.stop(ctx.villager());
                    ctx.villager().getLookControl().setLookAt((Entity)this.friend);
                } else if (nav.isAbandoned()) {
                    this.arrived = true;
                    nav.stop(ctx.villager());
                }
            }
            if (this.arrived) {
                ++this.waitTicks;
                if (this.friend != null && this.friend.isAlive()) {
                    ctx.villager().getLookControl().setLookAt((Entity)this.friend);
                }
            }
        }

        @Override
        public boolean isFinished() {
            return this.arrived && this.waitTicks >= 200;
        }

        @Override
        public void stop(GoalContext ctx, StopReason reason) {
            if (ctx == null) {
                return;
            }
            ctx.villager().getNavManager().stop(ctx.villager());
        }

        @Override
        public List<ItemStack> getHeldItems(TravelPhase phase) {
            return List.of(new ItemStack((ItemLike)Items.STICK));
        }

        @Override
        public TravelPhase getTravelPhase() {
            return TaskLabels.phaseFor(this.arrived);
        }

        @Override
        @Nullable
        public Component getGoalLabel() {
            return TaskLabels.labelForPhase(this.arrived, "play");
        }
    }

    static class PlayTask
    extends WalkAndWaitTask {
        private static final int MAX_WAIT_TICKS = 200;

        PlayTask(@Nullable BlockPos target) {
            super(target, 200);
        }

        @Override
        protected boolean allowRandomMoves() {
            return true;
        }

        @Override
        public ResourceLocation goalId() {
            return ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)"play");
        }

        @Override
        public List<ItemStack> getHeldItems(TravelPhase phase) {
            return List.of(new ItemStack((ItemLike)Items.STICK));
        }

        @Override
        @Nullable
        public Component getGoalLabel() {
            return TaskLabels.labelForPhase(this.arrived, "play");
        }
    }
}

