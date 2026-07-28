/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
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
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import javax.annotation.Nullable;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import org.millenaire.building.BuildingInstance;
import org.millenaire.building.BuildingPlan;
import org.millenaire.culture.ModCultures;
import org.millenaire.entity.MillVillager;
import org.millenaire.entity.VillagerNavDriver;
import org.millenaire.goal.GoalContext;
import org.millenaire.goal.GoalScheduler;
import org.millenaire.goal.PerVillagerThrottle;
import org.millenaire.goal.StopReason;
import org.millenaire.goal.TaskLabels;
import org.millenaire.goal.TravelPhase;
import org.millenaire.goal.VillagerGoal;
import org.millenaire.goal.VillagerTask;
import org.millenaire.goal.gathering.GatheringGoal;
import org.millenaire.goal.visit.VisitGoalSchema;
import org.millenaire.item.ItemHelper;

public class ObserveVillagerGoal
implements VillagerGoal {
    private final ResourceLocation id;
    @Nullable
    private final String targetGoalTag;
    @Nullable
    private final Set<ResourceLocation> targetGoalIds;
    private final int basePriority;
    private final int priorityRandom;
    private final int durationTicks;
    private final int reoccurDelayTicks;
    private final int minimumHour;
    private final int maximumHour;
    @Nullable
    private final String buildingTag;
    @Nullable
    private final String requiredTag;
    @Nullable
    private final List<String> heldItems;
    @Nullable
    private final PerVillagerThrottle reoccurThrottle;

    public ObserveVillagerGoal(ResourceLocation id, String targetGoalTag, int basePriority, int priorityRandom, int durationTicks, int reoccurDelayTicks) {
        this(id, targetGoalTag, null, basePriority, priorityRandom, durationTicks, reoccurDelayTicks, -1, -1, null, null, null);
    }

    public ObserveVillagerGoal(ResourceLocation id, String targetGoalTag, int basePriority, int priorityRandom, int durationTicks, int reoccurDelayTicks, @Nullable List<String> heldItems) {
        this(id, targetGoalTag, null, basePriority, priorityRandom, durationTicks, reoccurDelayTicks, -1, -1, null, null, heldItems);
    }

    public ObserveVillagerGoal(ResourceLocation id, @Nullable String targetGoalTag, @Nullable Set<ResourceLocation> targetGoalIds, int basePriority, int priorityRandom, int durationTicks, int reoccurDelayTicks, int minimumHour, int maximumHour, @Nullable String buildingTag, @Nullable String requiredTag, @Nullable List<String> heldItems) {
        this.id = id;
        this.targetGoalTag = targetGoalTag;
        this.targetGoalIds = targetGoalIds;
        this.basePriority = basePriority;
        this.priorityRandom = priorityRandom;
        this.durationTicks = durationTicks;
        this.reoccurDelayTicks = reoccurDelayTicks;
        this.minimumHour = minimumHour;
        this.maximumHour = maximumHour;
        this.buildingTag = buildingTag;
        this.requiredTag = requiredTag;
        this.heldItems = heldItems;
        this.reoccurThrottle = reoccurDelayTicks > 0 ? new PerVillagerThrottle(reoccurDelayTicks, 24000L, 6000L) : null;
    }

    public static ObserveVillagerGoal fromSchema(VisitGoalSchema.ObserveVillager s) {
        return new ObserveVillagerGoal(s.id(), s.targetGoalTag(), s.targetGoalIds(), s.basePriority(), s.priorityRandom(), s.durationTicks(), s.reoccurDelayTicks(), s.minimumHour(), s.maximumHour(), s.buildingTag(), s.requiredTag(), s.heldItems());
    }

    @Override
    public ResourceLocation id() {
        return this.id;
    }

    @Override
    public boolean isLeisure() {
        return true;
    }

    public String buildingTag() {
        return this.buildingTag;
    }

    public Set<ResourceLocation> targetGoalIds() {
        return this.targetGoalIds;
    }

    @Nullable
    public List<String> heldItems() {
        return this.heldItems;
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
        if (this.buildingTag != null && !this.villageHasBuildingWithTags(context)) {
            return false;
        }
        return this.findTargetVillager(context) != null;
    }

    @Override
    public VillagerTask start(GoalContext context) {
        if (this.reoccurThrottle != null) {
            this.reoccurThrottle.record(context.villager().getUUID(), context.level().getServer().getTickCount());
        }
        MillVillager target = this.findTargetVillager(context);
        return new ObserveTask(target, this.durationTicks, this.id.getPath(), this.heldItems);
    }

    @Nullable
    private MillVillager findTargetVillager(GoalContext ctx) {
        ArrayList<MillVillager> candidates = new ArrayList<MillVillager>();
        for (UUID uuid : ctx.village().getVillagerUuids()) {
            ResourceLocation currentGoalId;
            GoalScheduler scheduler;
            MillVillager other;
            Entity entity = ctx.level().getEntity(uuid);
            if (!(entity instanceof MillVillager) || (other = (MillVillager)entity) == ctx.villager() || (scheduler = other.getGoalScheduler()) == null || (currentGoalId = scheduler.getCurrentGoalId()) == null || !this.goalMatches(currentGoalId, scheduler)) continue;
            candidates.add(other);
        }
        if (candidates.isEmpty()) {
            return null;
        }
        return (MillVillager)((Object)candidates.get(ThreadLocalRandom.current().nextInt(candidates.size())));
    }

    private boolean goalMatches(ResourceLocation goalId, GoalScheduler scheduler) {
        if (this.targetGoalIds != null && !this.targetGoalIds.isEmpty()) {
            return this.targetGoalIds.contains((Object)goalId);
        }
        if (this.targetGoalTag != null) {
            return this.goalMatchesTag(goalId, scheduler);
        }
        return false;
    }

    private boolean villageHasBuildingWithTags(GoalContext ctx) {
        List<BuildingInstance> tagged = ctx.village().getOperationalBuildingsWithTag(this.buildingTag);
        if (tagged.isEmpty()) {
            return false;
        }
        if (this.requiredTag == null) {
            return true;
        }
        for (BuildingInstance building : tagged) {
            BuildingPlan plan = ModCultures.getBuildingPlan(building.getPlanId());
            if (plan == null || !plan.hasTag(this.requiredTag)) continue;
            return true;
        }
        return false;
    }

    private boolean goalMatchesTag(ResourceLocation goalId, GoalScheduler scheduler) {
        String typeTag = ObserveVillagerGoal.resolveGatheringTag(scheduler);
        if (typeTag != null) {
            if ("tag_producefoodalcohol".equals(this.targetGoalTag)) {
                return typeTag.equals("tag_producefood") || typeTag.equals("tag_producealcohol");
            }
            return typeTag.equals(this.targetGoalTag);
        }
        String path = goalId.getPath();
        return switch (this.targetGoalTag) {
            case "tag_agriculture" -> {
                if (path.contains("harvest") || path.contains("plant") || path.contains("breed") || path.contains("shear") || path.contains("slaughter")) {
                    yield true;
                }
                yield false;
            }
            case "tag_construction" -> path.equals("build");
            default -> false;
        };
    }

    @Nullable
    private static String resolveGatheringTag(GoalScheduler scheduler) {
        VillagerGoal current = scheduler.getCurrentGoal();
        if (current instanceof GatheringGoal) {
            GatheringGoal gathering = (GatheringGoal)current;
            return gathering.getGatheringType().tag();
        }
        return null;
    }

    static class ObserveTask
    implements VillagerTask {
        private static final double WALK_SPEED = 0.5;
        private static final double OBSERVE_RANGE = 8.0;
        @Nullable
        private final MillVillager targetVillager;
        private final int maxWaitTicks;
        private final String goalKey;
        private boolean arrived;
        private boolean abandonedFollow;
        private int waitTicks;
        private final List<ItemStack> resolvedHeldItems;

        ObserveTask(@Nullable MillVillager targetVillager, int maxWaitTicks, String goalKey, @Nullable List<String> heldItems) {
            this.targetVillager = targetVillager;
            this.maxWaitTicks = maxWaitTicks;
            this.goalKey = goalKey;
            if (targetVillager == null) {
                this.arrived = true;
            }
            this.resolvedHeldItems = ObserveTask.resolveItems(heldItems);
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
        public ResourceLocation goalId() {
            return ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)this.goalKey);
        }

        @Override
        public void tick(GoalContext ctx) {
            if (this.targetVillager != null && !this.targetVillager.isAlive()) {
                this.arrived = true;
                this.waitTicks = this.maxWaitTicks;
                return;
            }
            if (this.targetVillager == null) {
                this.arrived = true;
                ++this.waitTicks;
                return;
            }
            if (this.abandonedFollow) {
                ++this.waitTicks;
                ctx.villager().getLookControl().setLookAt((Entity)this.targetVillager);
                return;
            }
            VillagerNavDriver nav = ctx.villager().getNavManager();
            double distSq = ctx.villager().blockPosition().distSqr((Vec3i)this.targetVillager.blockPosition());
            if (distSq <= 64.0) {
                if (nav.getDestination() != null) {
                    nav.stop(ctx.villager());
                }
                this.arrived = true;
                ctx.villager().getLookControl().setLookAt((Entity)this.targetVillager);
                ++this.waitTicks;
            } else {
                nav.navigateTo(ctx.villager(), this.targetVillager.blockPosition(), 0.5);
                if (nav.isAbandoned()) {
                    this.abandonedFollow = true;
                    this.arrived = true;
                    nav.stop(ctx.villager());
                }
            }
        }

        @Override
        public boolean isFinished() {
            return this.arrived && this.waitTicks >= this.maxWaitTicks;
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
            return this.resolvedHeldItems;
        }

        @Override
        public TravelPhase getTravelPhase() {
            return TaskLabels.phaseFor(this.arrived);
        }

        @Override
        @Nullable
        public Component getGoalLabel() {
            return TaskLabels.labelForPhase(this.arrived, this.goalKey);
        }
    }
}

