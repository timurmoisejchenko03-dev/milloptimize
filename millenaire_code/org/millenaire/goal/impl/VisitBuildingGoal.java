/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Vec3i
 *  net.minecraft.network.chat.Component
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.ItemLike
 */
package org.millenaire.goal.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import org.millenaire.building.BuildingId;
import org.millenaire.building.BuildingInstance;
import org.millenaire.building.BuildingPlan;
import org.millenaire.culture.ModCultures;
import org.millenaire.entity.MillVillager;
import org.millenaire.goal.GoalContext;
import org.millenaire.goal.GoalScheduler;
import org.millenaire.goal.PerVillagerThrottle;
import org.millenaire.goal.TaskLabels;
import org.millenaire.goal.TravelPhase;
import org.millenaire.goal.VillagerGoal;
import org.millenaire.goal.VillagerTask;
import org.millenaire.goal.impl.WalkAndWaitTask;
import org.millenaire.goal.visit.VisitGoalSchema;
import org.millenaire.item.ItemHelper;

public class VisitBuildingGoal
implements VillagerGoal {
    private final ResourceLocation id;
    private final String buildingTag;
    private final int basePriority;
    private final int priorityRandom;
    private final int durationTicks;
    private final int reoccurDelayTicks;
    private final boolean allowRandomMoves;
    @Nullable
    private final String targetPosType;
    @Nullable
    private final String goalKey;
    private final int minimumHour;
    private final int maximumHour;
    private final int maxSimultaneousInBuilding;
    @Nullable
    private final String requiredTag;
    private final boolean leisure;
    @Nullable
    private final List<String> heldItems;
    @Nullable
    private final List<String> heldItemsDestination;
    @Nullable
    private final PerVillagerThrottle reoccurThrottle;

    public VisitBuildingGoal(ResourceLocation id, String buildingTag, int basePriority, int priorityRandom, int durationTicks, int reoccurDelayTicks, boolean allowRandomMoves, @Nullable String targetPosType, @Nullable String goalKey) {
        this(id, buildingTag, basePriority, priorityRandom, durationTicks, reoccurDelayTicks, allowRandomMoves, targetPosType, goalKey, -1, -1, 0, null, null, null, true);
    }

    public VisitBuildingGoal(ResourceLocation id, String buildingTag, int basePriority, int priorityRandom, int durationTicks, int reoccurDelayTicks, boolean allowRandomMoves, @Nullable String targetPosType, @Nullable String goalKey, int minimumHour, int maximumHour, int maxSimultaneousInBuilding, @Nullable String requiredTag, @Nullable List<String> heldItems, @Nullable List<String> heldItemsDestination, boolean leisure) {
        this.id = id;
        this.buildingTag = buildingTag;
        this.basePriority = basePriority;
        this.priorityRandom = priorityRandom;
        this.durationTicks = durationTicks;
        this.reoccurDelayTicks = reoccurDelayTicks;
        this.allowRandomMoves = allowRandomMoves;
        this.targetPosType = targetPosType;
        this.goalKey = goalKey != null ? goalKey : id.getPath();
        this.minimumHour = minimumHour;
        this.maximumHour = maximumHour;
        this.maxSimultaneousInBuilding = maxSimultaneousInBuilding;
        this.requiredTag = requiredTag;
        this.leisure = leisure;
        this.heldItems = heldItems;
        this.heldItemsDestination = heldItemsDestination;
        this.reoccurThrottle = reoccurDelayTicks > 0 ? new PerVillagerThrottle(reoccurDelayTicks, 24000L, 6000L) : null;
    }

    public static VisitBuildingGoal fromSchema(VisitGoalSchema.VisitBuilding s) {
        return new VisitBuildingGoal(s.id(), s.buildingTag(), s.basePriority(), s.priorityRandom(), s.durationTicks(), s.reoccurDelayTicks(), s.allowRandomMoves(), s.targetPosType(), s.goalKey(), s.minimumHour(), s.maximumHour(), s.maxSimultaneousInBuilding(), s.requiredTag(), s.heldItems(), s.heldItemsDestination(), s.leisure());
    }

    @Override
    public ResourceLocation id() {
        return this.id;
    }

    @Override
    public boolean isLeisure() {
        return this.leisure;
    }

    public String buildingTag() {
        return this.buildingTag;
    }

    @Nullable
    public List<String> heldItems() {
        return this.heldItems;
    }

    @Nullable
    public List<String> heldItemsDestination() {
        return this.heldItemsDestination;
    }

    @Override
    public int computePriority(GoalContext context) {
        int random = this.priorityRandom > 0 ? ThreadLocalRandom.current().nextInt(this.priorityRandom) : 0;
        return this.basePriority + random;
    }

    @Override
    public long reoccurDelayTicks() {
        return this.reoccurDelayTicks;
    }

    @Override
    public boolean canStart(GoalContext context) {
        if (this.reoccurThrottle != null) {
            long currentTick = context.level().getServer().getTickCount();
            if (this.reoccurThrottle.isThrottled(context.villager().getUUID(), currentTick)) {
                return false;
            }
        }
        if (this.minimumHour >= 0 || this.maximumHour >= 0) {
            long dayTime = context.level().getDayTime() % 24000L;
            if (this.minimumHour >= 0 && dayTime < (long)this.minimumHour) {
                return false;
            }
            if (this.maximumHour >= 0 && dayTime > (long)this.maximumHour) {
                return false;
            }
        }
        return this.findTarget(context) != null;
    }

    @Override
    public VillagerTask start(GoalContext context) {
        TargetInfo target;
        if (this.reoccurThrottle != null) {
            this.reoccurThrottle.record(context.villager().getUUID(), context.level().getServer().getTickCount());
        }
        return new VisitTask((target = this.findTarget(context)) != null ? target.pos() : null, target != null ? target.building() : null, this.durationTicks, this.allowRandomMoves, this.goalKey, this.heldItems, this.heldItemsDestination);
    }

    @Nullable
    private TargetInfo findTarget(GoalContext ctx) {
        if (this.buildingTag.isEmpty()) {
            BuildingPlan plan;
            BuildingInstance home;
            BuildingId homeId = ctx.villager().getHomeBuilding();
            BuildingInstance buildingInstance = home = homeId != null ? ctx.village().getBuilding(homeId) : null;
            if (home == null || !home.isOperational()) {
                return null;
            }
            if (!(this.requiredTag == null || (plan = ModCultures.getBuildingPlan(home.getPlanId())) != null && plan.hasTag(this.requiredTag))) {
                return null;
            }
            BlockPos pos = this.resolveTargetPos(home);
            return pos != null ? new TargetInfo(home, pos) : null;
        }
        ArrayList<TargetInfo> candidates = new ArrayList<TargetInfo>();
        for (BuildingInstance building : ctx.village().getOperationalBuildingsWithTag(this.buildingTag)) {
            BlockPos pos;
            int count;
            double dist;
            BlockPos craftingPos;
            BuildingPlan plan;
            if (this.requiredTag != null && ((plan = ModCultures.getBuildingPlan(building.getPlanId())) == null || !plan.hasTag(this.requiredTag)) || (craftingPos = building.getFirstPointPos("craftingPos")) != null && (dist = ctx.villager().blockPosition().distSqr((Vec3i)craftingPos)) <= 25.0 || this.maxSimultaneousInBuilding > 0 && (count = this.countVillagersWithGoalInBuilding(ctx, building)) >= this.maxSimultaneousInBuilding || (pos = this.resolveTargetPos(building)) == null) continue;
            candidates.add(new TargetInfo(building, pos));
        }
        if (candidates.isEmpty()) {
            return null;
        }
        return (TargetInfo)candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
    }

    private int countVillagersWithGoalInBuilding(GoalContext ctx, BuildingInstance building) {
        int count = 0;
        for (UUID uuid : ctx.village().getVillagerUuids()) {
            VillagerTask villagerTask;
            ResourceLocation currentGoalId;
            GoalScheduler scheduler;
            MillVillager other;
            Entity entity = ctx.level().getEntity(uuid);
            if (!(entity instanceof MillVillager) || (other = (MillVillager)entity) == ctx.villager() || (scheduler = other.getGoalScheduler()) == null || !this.id.equals((Object)(currentGoalId = scheduler.getCurrentGoalId())) || !((villagerTask = scheduler.getCurrentTask()) instanceof VisitTask)) continue;
            VisitTask visitTask = (VisitTask)villagerTask;
            if (visitTask.targetBuilding != building) continue;
            ++count;
        }
        return count;
    }

    @Nullable
    private BlockPos resolveTargetPos(BuildingInstance building) {
        BlockPos pos;
        if (this.targetPosType != null && (pos = building.getFirstPointPos(this.targetPosType)) != null) {
            return pos;
        }
        pos = building.getFirstPointPos("sleepingPos");
        if (pos != null) {
            return pos;
        }
        return building.getOrigin();
    }

    private record TargetInfo(BuildingInstance building, BlockPos pos) {
    }

    static class VisitTask
    extends WalkAndWaitTask {
        @Nullable
        final BuildingInstance targetBuilding;
        private final boolean randomMovesEnabled;
        private final String goalKey;
        private final List<ItemStack> travelHeldItems;
        private final List<ItemStack> destHeldItems;

        VisitTask(@Nullable BlockPos target, @Nullable BuildingInstance targetBuilding, int maxWaitTicks, boolean allowRandomMoves, String goalKey, @Nullable List<String> heldItems, @Nullable List<String> heldItemsDestination) {
            super(target, maxWaitTicks);
            this.targetBuilding = targetBuilding;
            this.randomMovesEnabled = allowRandomMoves;
            this.goalKey = goalKey;
            this.travelHeldItems = VisitTask.resolveItems(heldItems);
            this.destHeldItems = heldItemsDestination != null ? VisitTask.resolveItems(heldItemsDestination) : this.travelHeldItems;
        }

        private static List<ItemStack> resolveItems(@Nullable List<String> itemIds) {
            if (itemIds == null || itemIds.isEmpty()) {
                return List.of();
            }
            ArrayList<ItemStack> result = new ArrayList<ItemStack>();
            for (String itemId : itemIds) {
                Item item = ItemHelper.resolve(itemId);
                if (item == null) continue;
                result.add(new ItemStack((ItemLike)item));
            }
            return List.copyOf(result);
        }

        @Override
        protected boolean allowRandomMoves() {
            return this.randomMovesEnabled;
        }

        @Override
        public ResourceLocation goalId() {
            return ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)this.goalKey);
        }

        @Override
        public List<ItemStack> getHeldItems(TravelPhase phase) {
            return phase == TravelPhase.AT_DESTINATION ? this.destHeldItems : this.travelHeldItems;
        }

        @Override
        @Nullable
        public Component getGoalLabel() {
            return TaskLabels.labelForPhase(this.arrived, this.goalKey);
        }
    }
}

