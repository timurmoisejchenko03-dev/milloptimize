/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Vec3i
 *  net.minecraft.network.chat.Component
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.sounds.SoundEvent
 *  net.minecraft.sounds.SoundEvents
 *  net.minecraft.world.InteractionHand
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.Items
 *  net.minecraft.world.level.ItemLike
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.levelgen.Heightmap$Types
 *  org.slf4j.Logger
 */
package org.millenaire.goal.gathering;

import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import org.millenaire.building.BuildingId;
import org.millenaire.culture.ModCultures;
import org.millenaire.culture.VillagerType;
import org.millenaire.diagnostics.NavEvent;
import org.millenaire.diagnostics.NavigationCounters;
import org.millenaire.diagnostics.NavigationEventLog;
import org.millenaire.entity.VillagerInventory;
import org.millenaire.entity.VillagerNavDriver;
import org.millenaire.goal.GoalContext;
import org.millenaire.goal.NavigationHelperUtils;
import org.millenaire.goal.StopReason;
import org.millenaire.goal.TaskLabels;
import org.millenaire.goal.TravelPhase;
import org.millenaire.goal.VillagerTask;
import org.millenaire.goal.gathering.GatheringHandler;
import org.millenaire.goal.gathering.GatheringTarget;
import org.millenaire.goal.gathering.GatheringTaskHelper;
import org.millenaire.goal.gathering.GatheringType;
import org.millenaire.goal.gathering.InvalidationWindow;
import org.millenaire.item.ItemHelper;
import org.millenaire.tool.ToolCategory;
import org.millenaire.tool.ToolCategoryRegistry;
import org.slf4j.Logger;

public class GatheringTask
implements VillagerTask {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int DEFAULT_ARRIVAL_RANGE = 3;
    private static final double REMOTE_ACTION_HORIZONTAL_DIST_SQ = 64.0;
    private static final double STUCK_THRESHOLD_SQ = 0.25;
    private static final int MAX_STUCK_TELEPORTS = 3;
    private static final int INVALID_WINDOW_TICKS = 400;
    private static final int MAX_INVALIDS_IN_WINDOW = 3;
    private final GatheringType type;
    private final GatheringHandler handler;
    @Nullable
    private final BuildingId targetBuildingId;
    private State state = State.WALKING_TO_TARGET;
    @Nullable
    private GatheringTarget currentTarget;
    private int actionsPerformed;
    private int actionCooldown;
    private int cachedActionCooldownValue = -1;
    private int actionCooldownModCount = -1;
    private int stuckTicks;
    @Nullable
    private BlockPos lastPos;
    private boolean progressFlag;
    private int stuckTeleports;
    private final InvalidationWindow invalidationWindow = new InvalidationWindow(400, 3);
    private int actingTicksSinceTarget;
    private boolean remoteActing;
    @Nullable
    private VillagerInventory villagerInventory;
    @Nullable
    private final List<ItemStack> cachedTravelHeldItems;
    @Nullable
    private final List<ItemStack> cachedDestHeldItems;
    private static final Map<String, SoundEvent> SOUND_MAP = Map.of("wood", SoundEvents.WOOD_PLACE, "stone", SoundEvents.ANVIL_USE, "metal", SoundEvents.ANVIL_USE, "glass", SoundEvents.GLASS_PLACE, "cloth", SoundEvents.WOOL_PLACE);

    GatheringTask(GatheringType type, GatheringHandler handler, @Nullable BuildingId targetBuildingId) {
        this.type = type;
        this.handler = handler;
        this.targetBuildingId = targetBuildingId;
        this.cachedTravelHeldItems = GatheringTask.resolveItemIds(type.heldItems());
        List<String> destIds = type.heldItemsDestination();
        this.cachedDestHeldItems = destIds != null ? GatheringTask.resolveItemIds(destIds) : this.cachedTravelHeldItems;
    }

    @Nullable
    private static List<ItemStack> resolveItemIds(@Nullable List<String> itemIds) {
        if (itemIds == null || itemIds.isEmpty()) {
            return null;
        }
        ArrayList<ItemStack> stacks = new ArrayList<ItemStack>();
        for (String itemId : itemIds) {
            Item item = ItemHelper.resolve(itemId);
            if (item == null) continue;
            stacks.add(new ItemStack((ItemLike)item));
        }
        return stacks.isEmpty() ? null : List.copyOf(stacks);
    }

    @Override
    public ResourceLocation goalId() {
        return this.type.id();
    }

    @Nullable
    public BuildingId getTargetBuildingId() {
        return this.targetBuildingId;
    }

    @Override
    public void reportProgress() {
        this.progressFlag = true;
    }

    @Override
    public boolean consumeProgress() {
        if (this.progressFlag) {
            this.progressFlag = false;
            return true;
        }
        return false;
    }

    @Override
    public void tick(GoalContext ctx) {
        if (this.villagerInventory == null) {
            this.villagerInventory = ctx.villager().getInventory();
        }
        switch (this.state.ordinal()) {
            case 0: {
                this.tickWalking(ctx);
                break;
            }
            case 1: {
                this.tickActing(ctx);
                break;
            }
        }
    }

    @Override
    public boolean isFinished() {
        return this.state == State.DONE;
    }

    @Override
    public void stop(GoalContext ctx, StopReason reason) {
        if (ctx == null) {
            return;
        }
        ctx.villager().getNavManager().stop(ctx.villager());
        if (reason == StopReason.COMPLETED) {
            LOGGER.debug("Gathering {} completed \u2014 {} actions performed", (Object)this.type.id(), (Object)this.actionsPerformed);
        }
    }

    @Override
    public TravelPhase getTravelPhase() {
        return this.state == State.ACTING ? TravelPhase.AT_DESTINATION : TravelPhase.TRAVELLING;
    }

    @Override
    @Nullable
    public Component getGoalLabel() {
        return TaskLabels.labelForPhase(this.state == State.ACTING, this.type.id().getPath());
    }

    @Override
    public List<ItemStack> getHeldItems(TravelPhase phase) {
        List<ItemStack> cached;
        List<ItemStack> list = cached = phase == TravelPhase.AT_DESTINATION ? this.cachedDestHeldItems : this.cachedTravelHeldItems;
        if (cached != null) {
            return cached;
        }
        String toolCat = this.handler.getHeldToolCategoryId(this.type);
        if (toolCat != null) {
            Item fallback = GatheringTask.toolFallback(toolCat);
            return List.of(this.getBestToolStack(toolCat, fallback));
        }
        Item item = this.handler.getDefaultHeldItem(this.type);
        if (item != null) {
            return List.of(new ItemStack((ItemLike)item));
        }
        return List.of();
    }

    private static Item toolFallback(String categoryId) {
        if (categoryId.contains("pickaxe")) {
            return Items.WOODEN_PICKAXE;
        }
        if (categoryId.contains("axe")) {
            return Items.WOODEN_AXE;
        }
        if (categoryId.contains("shovel")) {
            return Items.WOODEN_SHOVEL;
        }
        if (categoryId.contains("hoe")) {
            return Items.WOODEN_HOE;
        }
        return Items.WOODEN_PICKAXE;
    }

    private ItemStack getBestToolStack(String categoryId, Item fallback) {
        if (this.villagerInventory == null) {
            return new ItemStack((ItemLike)fallback);
        }
        ToolCategory category = ToolCategoryRegistry.get(categoryId);
        if (category == null) {
            return new ItemStack((ItemLike)fallback);
        }
        ToolCategory.ToolEntry best = category.getBestOwned(item -> this.villagerInventory.getCount((Item)item) > 0);
        if (best != null && best.item() != null) {
            return new ItemStack((ItemLike)best.item());
        }
        return new ItemStack((ItemLike)fallback);
    }

    private void tickWalking(GoalContext ctx) {
        double entityMovedSq;
        double arrivalDistSq;
        BlockPos targetPos;
        BlockPos currentPos;
        double hDistSq;
        if (this.currentTarget == null) {
            this.currentTarget = this.handler.findTarget(ctx, this.type, null);
            if (this.currentTarget == null) {
                LOGGER.debug("Gathering {} \u2014 no target found, stopping", (Object)this.type.id());
                this.state = State.DONE;
                return;
            }
        }
        if ((hDistSq = GatheringTask.horizontalDistSqr(currentPos = ctx.villager().blockPosition(), targetPos = this.currentTarget.navigationPos())) <= (arrivalDistSq = (double)(this.type.arrivalRange() * this.type.arrivalRange()))) {
            ctx.villager().getNavManager().stop(ctx.villager());
            this.state = State.ACTING;
            this.actionCooldown = 0;
            this.actingTicksSinceTarget = 0;
            this.reportProgress();
            return;
        }
        int navY = GatheringTask.getNavigationY(ctx, targetPos);
        BlockPos navDest = new BlockPos(targetPos.getX(), navY, targetPos.getZ());
        VillagerNavDriver nav = ctx.villager().getNavManager();
        if (nav.getDestination() == null) {
            nav.navigateTo(ctx.villager(), navDest, this.type.walkSpeed());
        } else if (this.currentTarget instanceof GatheringTarget.EntityTarget && (entityMovedSq = nav.getDestination().distSqr((Vec3i)navDest)) > 4.0) {
            nav.navigateTo(ctx.villager(), navDest, this.type.walkSpeed());
        }
        if (this.lastPos == null) {
            this.lastPos = currentPos;
        }
        if (GatheringTaskHelper.isStuck(currentPos, this.lastPos, 0.25)) {
            ++this.stuckTicks;
        } else {
            this.stuckTicks = 0;
            this.lastPos = currentPos;
        }
        if (this.stuckTicks > this.type.stuckTimeout()) {
            double remoteDistSq;
            int safeY;
            boolean canTeleport;
            VillagerType vType = ModCultures.getVillagerType(ctx.villager().getVillagerTypeId());
            boolean bl = canTeleport = vType == null || !vType.hasTag("noteleport");
            if (canTeleport && this.stuckTeleports < 3 && Math.abs((safeY = NavigationHelperUtils.isSafeLanding((Level)ctx.level(), targetPos) ? targetPos.getY() : NavigationHelperUtils.villagerStandY((Level)ctx.level(), targetPos.getX(), targetPos.getZ())) - targetPos.getY()) < 4) {
                ++this.stuckTeleports;
                LOGGER.debug("Gathering {} \u2014 stuck TP to {},{},{} ({}/{})", new Object[]{this.type.id(), targetPos.getX(), safeY, targetPos.getZ(), this.stuckTeleports, 3});
                ctx.villager().teleportTo((double)targetPos.getX() + 0.5, safeY, (double)targetPos.getZ() + 0.5);
                ctx.villager().getNavManager().stop(ctx.villager());
                if (this.currentTarget != null) {
                    this.handler.onStuckTeleport(ctx, this.type, this.currentTarget);
                }
                this.stuckTicks = 0;
                this.lastPos = null;
                this.reportProgress();
                return;
            }
            if (this.handler.supportsRemoteAction() && (remoteDistSq = GatheringTask.horizontalDistSqr(currentPos, targetPos)) <= 64.0) {
                LOGGER.debug("Gathering {} \u2014 remote action (stuck {} ticks, horiz dist {}) at {}", new Object[]{this.type.id(), this.stuckTicks, Math.sqrt(remoteDistSq), targetPos});
                this.state = State.ACTING;
                this.remoteActing = true;
                this.actionCooldown = 0;
                this.actingTicksSinceTarget = 0;
                this.reportProgress();
                return;
            }
            LOGGER.debug("Gathering {} \u2014 villager stuck for {} ticks, giving up", (Object)this.type.id(), (Object)this.type.stuckTimeout());
            this.state = State.DONE;
            return;
        }
    }

    private void tickActing(GoalContext ctx) {
        double maxDistSq;
        if (this.currentTarget == null) {
            this.state = State.DONE;
            return;
        }
        if (!this.handler.isTargetStillValid(ctx, this.type, this.currentTarget)) {
            this.handleTargetInvalidated(ctx);
            return;
        }
        ++this.actingTicksSinceTarget;
        int watchdog = this.handler.actingWatchdogTicks(this.type);
        if (this.actingTicksSinceTarget > watchdog) {
            long now = ctx.gameTime();
            LOGGER.debug("Gathering {} \u2014 ACTING watchdog fired after {} ticks (budget {}), abandoning", new Object[]{this.type.id(), this.actingTicksSinceTarget, watchdog});
            NavigationEventLog log = ctx.villager().getNavEventLog();
            log.record(now, NavEvent.Layer.GATHERING, NavEvent.Type.ACTING_WATCHDOG_FIRED, "handler=" + this.handler.id() + " elapsed=" + this.actingTicksSinceTarget + " budget=" + watchdog);
            log.record(now, NavEvent.Layer.GATHERING, NavEvent.Type.GOAL_ABANDONED, "acting_watchdog handler=" + this.handler.id());
            NavigationCounters.incGoalAbandoned();
            this.state = State.DONE;
            return;
        }
        BlockPos targetPos = this.currentTarget.navigationPos();
        double hDistSq = GatheringTask.horizontalDistSqr(ctx.villager().blockPosition(), targetPos);
        double arrivalDistSq = this.type.arrivalRange() * this.type.arrivalRange();
        double d = maxDistSq = this.remoteActing ? 64.0 : arrivalDistSq;
        if (hDistSq > maxDistSq) {
            this.state = State.WALKING_TO_TARGET;
            this.remoteActing = false;
            this.stuckTicks = 0;
            this.lastPos = null;
            return;
        }
        ++this.actionCooldown;
        int currentModCount = ctx.villager().getInventory().modCount();
        if (this.cachedActionCooldownValue < 0 || currentModCount != this.actionCooldownModCount) {
            this.cachedActionCooldownValue = this.handler.getActionCooldown(ctx, this.type);
            this.actionCooldownModCount = currentModCount;
        }
        if (this.actionCooldown < this.cachedActionCooldownValue) {
            if (this.actionCooldown % 20 == 0) {
                ctx.villager().swing(InteractionHand.MAIN_HAND);
            }
            return;
        }
        this.actionCooldown = 0;
        ctx.villager().getLookControl().setLookAt((double)targetPos.getX() + 0.5, (double)targetPos.getY() + 0.5, (double)targetPos.getZ() + 0.5);
        boolean actionDone = this.handler.performAction(ctx, this.type, this.currentTarget);
        if (actionDone) {
            ctx.villager().swing(InteractionHand.MAIN_HAND);
            this.playGatheringSound(ctx);
            ++this.actionsPerformed;
            this.reportProgress();
            if (this.actionsPerformed >= this.type.maxActionsPerTask()) {
                LOGGER.debug("Gathering {} \u2014 action limit reached ({})", (Object)this.type.id(), (Object)this.type.maxActionsPerTask());
                this.state = State.DONE;
                return;
            }
            GatheringTarget nextTarget = this.handler.findTarget(ctx, this.type, this.currentTarget);
            if (nextTarget == null) {
                LOGGER.debug("Gathering {} \u2014 no more targets, stopping after {} actions", (Object)this.type.id(), (Object)this.actionsPerformed);
                this.state = State.DONE;
            } else {
                this.currentTarget = nextTarget;
                ctx.villager().getNavManager().stop(ctx.villager());
                this.state = State.WALKING_TO_TARGET;
                this.remoteActing = false;
                this.stuckTicks = 0;
                this.lastPos = null;
            }
        } else {
            ctx.villager().swing(InteractionHand.MAIN_HAND);
            this.reportProgress();
        }
    }

    private void handleTargetInvalidated(GoalContext ctx) {
        long now = ctx.gameTime();
        boolean exhausted = this.invalidationWindow.record(now);
        NavigationCounters.incTargetInvalid();
        ctx.villager().getNavEventLog().record(now, NavEvent.Layer.GATHERING, NavEvent.Type.TARGET_INVALID, "handler=" + this.handler.id() + " count=" + this.invalidationWindow.count() + "/" + this.invalidationWindow.maxCount());
        if (exhausted) {
            LOGGER.debug("Gathering {} \u2014 {} target invalidations in {} ticks, abandoning goal", new Object[]{this.type.id(), this.invalidationWindow.count(), 400});
            ctx.villager().getNavEventLog().record(now, NavEvent.Layer.GATHERING, NavEvent.Type.GOAL_ABANDONED, "target_invalid livelock handler=" + this.handler.id());
            NavigationCounters.incGoalAbandoned();
            this.state = State.DONE;
            return;
        }
        GatheringTarget next = this.handler.findTarget(ctx, this.type, null);
        if (next == null) {
            this.state = State.DONE;
            return;
        }
        this.currentTarget = next;
        ctx.villager().getNavManager().stop(ctx.villager());
        this.state = State.WALKING_TO_TARGET;
        this.remoteActing = false;
        this.actionCooldown = 0;
        this.stuckTicks = 0;
        this.lastPos = null;
    }

    private void playGatheringSound(GoalContext ctx) {
        String sound = this.type.sound();
        if (sound == null) {
            return;
        }
        SoundEvent soundEvent = SOUND_MAP.get(sound);
        if (soundEvent != null) {
            ctx.villager().playSound(soundEvent, 0.5f, 0.9f + ctx.level().random.nextFloat() * 0.2f);
        }
    }

    private static double horizontalDistSqr(BlockPos a, BlockPos b) {
        return NavigationHelperUtils.horizontalDistSq(a, b);
    }

    private static int getNavigationY(GoalContext ctx, BlockPos targetPos) {
        if (ctx.level().isLoaded(targetPos)) {
            int groundY = ctx.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, targetPos.getX(), targetPos.getZ());
            if (targetPos.getY() <= groundY) {
                return targetPos.getY();
            }
            return groundY;
        }
        return targetPos.getY();
    }

    @Override
    public Map<String, String> getNavDebugInfo() {
        LinkedHashMap<String, String> info = new LinkedHashMap<String, String>();
        info.put("gatherState", this.state.name());
        info.put("type", this.type.id().getPath());
        info.put("target", this.currentTarget != null ? this.currentTarget.navigationPos().toShortString() : "null");
        info.put("actions", this.actionsPerformed + "/" + this.type.maxActionsPerTask());
        info.put("stuckTicks", this.stuckTicks + "/" + this.type.stuckTimeout());
        if (this.remoteActing) {
            info.put("remoteActing", "true");
        }
        return info;
    }

    static final class State
    extends Enum<State> {
        public static final /* enum */ State WALKING_TO_TARGET = new State();
        public static final /* enum */ State ACTING = new State();
        public static final /* enum */ State DONE = new State();
        private static final /* synthetic */ State[] $VALUES;

        public static State[] values() {
            return (State[])$VALUES.clone();
        }

        public static State valueOf(String name) {
            return Enum.valueOf(State.class, name);
        }

        private static /* synthetic */ State[] $values() {
            return new State[]{WALKING_TO_TARGET, ACTING, DONE};
        }

        static {
            $VALUES = State.$values();
        }
    }
}

