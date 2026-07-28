/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.BlockPos$MutableBlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.core.Direction$Plane
 *  net.minecraft.network.chat.Component
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.sounds.SoundEvents
 *  net.minecraft.sounds.SoundSource
 *  net.minecraft.world.InteractionHand
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.BedBlock
 *  net.minecraft.world.level.block.CampfireBlock
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.properties.BedPart
 *  net.minecraft.world.level.block.state.properties.Property
 *  net.minecraft.world.phys.AABB
 *  org.slf4j.Logger
 */
package org.millenaire.goal.impl;

import com.mojang.logging.LogUtils;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.AABB;
import org.millenaire.building.BedManager;
import org.millenaire.building.BuildingInstance;
import org.millenaire.building.HearthApproach;
import org.millenaire.diagnostics.NavEvent;
import org.millenaire.diagnostics.NavigationCounters;
import org.millenaire.entity.BlockHazards;
import org.millenaire.entity.MillVillager;
import org.millenaire.entity.VillagerNavDriver;
import org.millenaire.goal.GoalContext;
import org.millenaire.goal.NavigationHelperUtils;
import org.millenaire.goal.ProgressAwareTask;
import org.millenaire.goal.StopReason;
import org.millenaire.goal.TaskLabels;
import org.millenaire.goal.TravelPhase;
import org.millenaire.goal.VillagerGoal;
import org.millenaire.goal.VillagerTask;
import org.millenaire.village.HearthResidentResolver;
import org.millenaire.village.NightActionHelper;
import org.slf4j.Logger;

public class RestGoal
implements VillagerGoal {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)"rest");
    private static final int DEBT_PRIORITY_DIVISOR = 30;
    private static final int DEBT_PRIORITY_CAP = 200;

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
        int debt = context.villager().getSleepDebtTicks();
        int boost = Math.min(200, debt / 30);
        return 50 + boost;
    }

    @Override
    public boolean canStart(GoalContext context) {
        return true;
    }

    @Override
    public boolean canBeDoneAtNight() {
        return true;
    }

    @Override
    public boolean canBeDoneInDayTime() {
        return false;
    }

    @Override
    public VillagerTask start(GoalContext context) {
        return new RestTask();
    }

    static class RestTask
    extends ProgressAwareTask {
        private static final double ARRIVE_DISTANCE = 3.0;
        private static final double WALK_SPEED = 0.5;
        private static final int MAX_REST_TICKS = 12000;
        private static final int SEARCH_RADIUS = 6;
        private static final float GROUND_FLOATING_HEIGHT = 0.2f;
        private static final int HEARTH_ACTION_DURATION = 20;
        private static final double HEARTH_ARRIVE_DISTANCE = 2.0;
        private boolean arrived;
        private boolean positioned;
        private boolean nightActionPerformed;
        private boolean abandonRescueTried;
        private boolean lastWasDay = false;
        private boolean ticked = false;
        private int tickCount;
        @Nullable
        private BlockPos sleepTarget;
        @Nullable
        private BlockPos sleepBedPos;
        private boolean hearthPhaseDone;
        @Nullable
        private BlockPos hearthTarget;
        @Nullable
        private BlockPos hearthStandPos;
        private boolean hearthArrived;
        private int hearthActionTicks;

        RestTask() {
        }

        @Override
        public ResourceLocation goalId() {
            return ID;
        }

        @Override
        public void tick(GoalContext ctx) {
            this.lastWasDay = ctx.level().isDay();
            this.ticked = true;
            ++this.tickCount;
            this.reportProgress();
            if (!this.hearthPhaseDone) {
                this.tickPreRestExtinguishHearth(ctx);
                if (!this.hearthPhaseDone) {
                    return;
                }
            }
            if (this.arrived) {
                if (!this.positioned) {
                    this.positioned = true;
                    this.positionForSleep(ctx);
                }
                if (!ctx.villager().isSleeping() && !ctx.villager().isVillagerSleeping()) {
                    this.startSleeping(ctx);
                    ctx.villager().getNavigation().stop();
                }
                if (!this.nightActionPerformed) {
                    this.nightActionPerformed = NightActionHelper.perform(ctx);
                }
                return;
            }
            if (this.sleepTarget == null) {
                this.sleepTarget = this.resolveSleepTarget(ctx);
            }
            if (this.sleepTarget == null) {
                this.arrived = true;
                return;
            }
            VillagerNavDriver nav = ctx.villager().getNavManager();
            if (nav.getDestination() == null) {
                nav.navigateTo(ctx.villager(), this.sleepTarget, 0.5);
            }
            if (nav.isArrived(ctx.villager(), 3.0)) {
                this.arrived = true;
                nav.stop(ctx.villager());
            } else if (nav.isAbandoned()) {
                if (RestTask.isAtTerrainGround(ctx) || this.abandonRescueTried) {
                    this.arrived = true;
                    nav.stop(ctx.villager());
                } else {
                    this.abandonRescueTried = true;
                    NavigationHelperUtils.teleportToSafe(ctx.villager(), this.sleepTarget);
                }
            }
        }

        private static boolean isAtTerrainGround(GoalContext ctx) {
            MillVillager v = ctx.villager();
            Level level = v.level();
            if (!(level instanceof ServerLevel)) {
                return true;
            }
            ServerLevel level2 = (ServerLevel)level;
            BlockPos feet = v.blockPosition();
            int groundY = NavigationHelperUtils.villagerStandY((Level)level2, feet.getX(), feet.getZ());
            return feet.getY() <= groundY;
        }

        private void tickPreRestExtinguishHearth(GoalContext ctx) {
            if (this.hearthTarget == null && !this.hearthArrived) {
                BlockPos target = RestTask.findLitHearthToExtinguish(ctx);
                if (target == null) {
                    this.hearthPhaseDone = true;
                    return;
                }
                this.hearthTarget = target;
                this.hearthStandPos = HearthApproach.findStandPosition((BlockGetter)ctx.level(), this.hearthTarget);
            }
            if (!this.hearthArrived) {
                VillagerNavDriver nav = ctx.villager().getNavManager();
                if (nav.getDestination() == null) {
                    nav.navigateTo(ctx.villager(), this.hearthStandPos, 0.5);
                }
                if (nav.isAbandoned()) {
                    nav.stop(ctx.villager());
                    this.hearthPhaseDone = true;
                    return;
                }
                if (nav.isArrivedHorizontal(ctx.villager(), 2.0)) {
                    nav.stop(ctx.villager());
                    this.hearthArrived = true;
                    this.hearthActionTicks = 0;
                }
                return;
            }
            ctx.villager().swing(InteractionHand.MAIN_HAND);
            ++this.hearthActionTicks;
            if (this.hearthActionTicks < 20) {
                return;
            }
            ServerLevel level = ctx.level();
            BlockState current = level.getBlockState(this.hearthTarget);
            if (current.getBlock() instanceof CampfireBlock && current.hasProperty((Property)CampfireBlock.LIT) && ((Boolean)current.getValue((Property)CampfireBlock.LIT)).booleanValue()) {
                level.setBlock(this.hearthTarget, (BlockState)current.setValue((Property)CampfireBlock.LIT, (Comparable)Boolean.valueOf(false)), 3);
                level.playSound(null, this.hearthTarget, SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.BLOCKS, 0.8f, level.getRandom().nextFloat() * 0.4f + 0.8f);
            }
            this.hearthPhaseDone = true;
        }

        @Nullable
        static BlockPos findLitHearthToExtinguish(GoalContext ctx) {
            BuildingInstance home = ctx.resolveHomeBuilding().orElse(null);
            if (home == null) {
                return null;
            }
            if (home.getHearthPositions().isEmpty()) {
                return null;
            }
            if (!HearthResidentResolver.isDesignatedResident(ctx.village(), home, ctx.villager().getUUID())) {
                return null;
            }
            for (BlockPos pos : home.getHearthPositions()) {
                BlockState state = ctx.level().getBlockState(pos);
                if (!(state.getBlock() instanceof CampfireBlock) || !state.hasProperty((Property)CampfireBlock.LIT) || !((Boolean)state.getValue((Property)CampfireBlock.LIT)).booleanValue()) continue;
                return pos;
            }
            return null;
        }

        @Nullable
        private BlockPos resolveSleepTarget(GoalContext ctx) {
            BuildingInstance home = ctx.resolveHomeBuilding().orElse(null);
            if (home != null) {
                return home.getSleepingPos();
            }
            BuildingInstance th = ctx.village().getTownhall();
            if (th != null) {
                return th.getSleepingPos();
            }
            return ctx.villager().blockPosition();
        }

        private void positionForSleep(GoalContext ctx) {
            MillVillager villager = ctx.villager();
            Level level = villager.level();
            if (!(level instanceof ServerLevel)) {
                return;
            }
            ServerLevel level2 = (ServerLevel)level;
            BlockPos searchCenter = this.sleepTarget != null ? this.sleepTarget : villager.blockPosition();
            BlockPos claimedBed = this.findBedViaClaiming(ctx, level2);
            if (claimedBed != null) {
                this.sleepBedPos = claimedBed;
                return;
            }
            BuildingInstance home = ctx.resolveHomeBuilding().orElse(null);
            BedManager bedMgr = home != null && home.hasBedManager() ? home.getBedManager() : null;
            BlockPos legacyBed = this.findBedLegacy(level2, searchCenter, bedMgr, level2.getGameTime());
            if (legacyBed != null) {
                this.sleepBedPos = legacyBed;
                return;
            }
            BlockPos groundPos = this.findSheltered(level2, searchCenter);
            if (groundPos != null) {
                this.positionOnSurface(villager, level2, groundPos, 1.2f);
                return;
            }
            villager.setPos(villager.getX(), villager.getY() + (double)0.2f, villager.getZ());
            RestTask.nudgeOffHazard(villager, level2);
        }

        private static void nudgeOffHazard(MillVillager villager, ServerLevel level) {
            BlockPos feet = villager.blockPosition();
            if (!BlockHazards.isHazardousAt((BlockGetter)level, feet)) {
                return;
            }
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                BlockState below;
                BlockPos shifted = feet.relative(dir);
                if (BlockHazards.isHazardousAt((BlockGetter)level, shifted) || !(below = level.getBlockState(shifted.below())).isSolidRender((BlockGetter)level, shifted.below()) || !level.getBlockState(shifted).isAir() || !level.getBlockState(shifted.above()).isAir()) continue;
                LOGGER.info("[Millenaire] Nudged {} off hazard at {} \u2192 {}", new Object[]{villager.getVillagerDisplayName(), feet.toShortString(), shifted.toShortString()});
                villager.setPos((double)shifted.getX() + 0.5, (float)shifted.getY() + 0.2f, (double)shifted.getZ() + 0.5);
                return;
            }
            LOGGER.warn("[Millenaire] {} stuck on hazard at {} with no safe neighbor", (Object)villager.getVillagerDisplayName(), (Object)feet.toShortString());
        }

        private void startSleeping(GoalContext ctx) {
            Level level;
            MillVillager villager = ctx.villager();
            if (this.sleepBedPos != null && (level = villager.level()) instanceof ServerLevel) {
                ServerLevel level2 = (ServerLevel)level;
                BlockPos headPos = this.resolveHeadPos(level2, this.sleepBedPos);
                if (!this.wouldSuffocate(level2, headPos)) {
                    villager.startSleeping(headPos);
                    return;
                }
                this.recordBedSuffocation(ctx, villager, this.sleepBedPos);
                BlockPos alt = this.tryOneAlternativeBed(ctx, level2, villager, this.sleepBedPos);
                if (alt != null) {
                    BlockPos altHeadPos = this.resolveHeadPos(level2, alt);
                    if (!this.wouldSuffocate(level2, altHeadPos)) {
                        this.sleepBedPos = alt;
                        this.positionOnSurface(villager, level2, alt, 0.7f);
                        villager.startSleeping(altHeadPos);
                        return;
                    }
                    this.recordBedSuffocation(ctx, villager, alt);
                }
                villager.setVillagerSleeping(true);
            } else {
                villager.setVillagerSleeping(true);
            }
        }

        private void recordBedSuffocation(GoalContext ctx, MillVillager villager, BlockPos bedPos) {
            LOGGER.info("[Millenaire] BED_SUFFOCATION \u2014 {} at bed {} (flagged for 1 MC day)", (Object)villager.getVillagerDisplayName(), (Object)bedPos.toShortString());
            villager.getNavEventLog().record(villager.level().getGameTime(), NavEvent.Layer.REST, NavEvent.Type.BED_SUFFOCATION, "bed=" + bedPos.toShortString());
            NavigationCounters.incBedSuffocation();
            BuildingInstance home = ctx.resolveHomeBuilding().orElse(null);
            if (home != null && home.hasBedManager()) {
                BedManager bm = home.getBedManager();
                bm.markSuffocating(bedPos, villager.level().getGameTime());
                bm.releaseBed(bedPos);
                ctx.village().markDirty();
            }
        }

        @Nullable
        private BlockPos tryOneAlternativeBed(GoalContext ctx, ServerLevel level, MillVillager villager, BlockPos excluded) {
            BlockPos searchCenter;
            BuildingInstance home = ctx.resolveHomeBuilding().orElse(null);
            if (home == null || !home.hasBedManager()) {
                return null;
            }
            BedManager bm = home.getBedManager();
            Optional<BlockPos> alt = bm.findNearestUnclaimedBed(searchCenter = this.sleepTarget != null ? this.sleepTarget : villager.blockPosition(), 6.0, villager.level().getGameTime());
            if (alt.isPresent() && !alt.get().equals((Object)excluded) && bm.claimBed(alt.get(), villager.getUUID())) {
                ctx.village().markDirty();
                return alt.get();
            }
            return null;
        }

        private boolean wouldSuffocate(ServerLevel level, BlockPos pos) {
            return level.getBlockState(pos).isSuffocating((BlockGetter)level, pos) || level.getBlockState(pos.above()).isSuffocating((BlockGetter)level, pos.above());
        }

        private void positionOnSurface(MillVillager villager, ServerLevel level, BlockPos pos, float yOffset) {
            float angle = this.determineSleepAngle(level, pos);
            double dx = 0.5;
            double dz = 0.5;
            if (angle == 0.0f) {
                dx = 0.95;
            } else if (angle == 90.0f) {
                dz = 0.95;
            } else if (angle == 180.0f) {
                dx = 0.05;
            } else if (angle == 270.0f) {
                dz = 0.05;
            }
            villager.setPos((double)pos.getX() + dx, (float)pos.getY() + yOffset, (double)pos.getZ() + dz);
            villager.setYRot((angle + 90.0f) % 360.0f);
        }

        private BlockPos resolveHeadPos(ServerLevel level, BlockPos footPos) {
            BlockState state = level.getBlockState(footPos);
            if (state.getBlock() instanceof BedBlock && state.hasProperty((Property)BedBlock.PART) && state.getValue((Property)BedBlock.PART) == BedPart.FOOT && state.hasProperty((Property)BedBlock.FACING)) {
                return footPos.relative((Direction)state.getValue((Property)BedBlock.FACING));
            }
            return footPos;
        }

        @Nullable
        private BlockPos findBedViaClaiming(GoalContext ctx, ServerLevel level) {
            BlockPos pos;
            BlockPos searchCenter;
            Optional<BlockPos> unclaimed;
            UUID villagerUuid;
            BuildingInstance home = ctx.resolveHomeBuilding().orElse(null);
            if (home == null || !home.hasBedManager()) {
                return null;
            }
            BedManager bedManager = home.getBedManager();
            Optional<BlockPos> existing = bedManager.getClaimedBed(villagerUuid = ctx.villager().getUUID());
            if (existing.isPresent()) {
                BlockPos pos2 = existing.get();
                BlockState state = level.getBlockState(pos2);
                if (state.getBlock() instanceof BedBlock) {
                    return pos2;
                }
                bedManager.releaseBedByVillager(villagerUuid);
                ctx.village().markDirty();
            }
            if ((unclaimed = bedManager.findNearestUnclaimedBed(searchCenter = this.sleepTarget != null ? this.sleepTarget : ctx.villager().blockPosition(), 6.0, level.getGameTime())).isPresent() && bedManager.claimBed(pos = unclaimed.get(), villagerUuid)) {
                ctx.village().markDirty();
                return pos;
            }
            return null;
        }

        @Nullable
        private BlockPos findBedLegacy(ServerLevel level, BlockPos center, @Nullable BedManager bedManager, long gameTime) {
            BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
            for (int dx = -6; dx <= 6; ++dx) {
                for (int dy = -6; dy <= 6; ++dy) {
                    for (int dz = -6; dz <= 6; ++dz) {
                        mutable.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                        BlockState state = level.getBlockState((BlockPos)mutable);
                        if (!(state.getBlock() instanceof BedBlock) || state.hasProperty((Property)BedBlock.PART) && state.getValue((Property)BedBlock.PART) == BedPart.HEAD || this.isBedOccupied(level, (BlockPos)mutable) || bedManager != null && bedManager.isSuffocatingMarked(mutable.immutable(), gameTime)) continue;
                        return mutable.immutable();
                    }
                }
            }
            return null;
        }

        @Nullable
        private BlockPos findSheltered(ServerLevel level, BlockPos center) {
            BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
            for (int xDelta = 0; xDelta < 6; ++xDelta) {
                for (int yDelta = 0; yDelta < 6; ++yDelta) {
                    for (int zDelta = 0; zDelta < 6; ++zDelta) {
                        for (int l = 0; l < 8; ++l) {
                            int dx = xDelta * (1 - (l & 1) * 2);
                            int dy = yDelta * (1 - (l & 2));
                            int dz = zDelta * (1 - (l & 4) / 2);
                            mutable.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                            if (!this.isLieableBlock(level, (BlockPos)mutable)) continue;
                            float angle = this.determineSleepAngle(level, (BlockPos)mutable);
                            BlockPos feet = mutable.immutable();
                            BlockPos body = feet.relative(RestTask.sleepAngleDirection(angle));
                            if (!this.isLieableBlock(level, body) || this.isGroundSpotTaken(level, feet)) continue;
                            return feet;
                        }
                    }
                }
            }
            return null;
        }

        private boolean isLieableBlock(ServerLevel level, BlockPos pos) {
            if (!level.getBlockState(pos).isSolidRender((BlockGetter)level, pos)) {
                return false;
            }
            if (!level.getBlockState(pos.above()).isAir()) {
                return false;
            }
            if (!level.getBlockState(pos.above(2)).isAir()) {
                return false;
            }
            int maxY = level.getMaxBuildHeight();
            for (int checkY = pos.getY() + 3; checkY <= maxY; ++checkY) {
                if (level.getBlockState(pos.atY(checkY)).isAir()) continue;
                return true;
            }
            return false;
        }

        private static Direction sleepAngleDirection(float angle) {
            if (angle == 90.0f) {
                return Direction.SOUTH;
            }
            if (angle == 180.0f) {
                return Direction.WEST;
            }
            if (angle == 270.0f) {
                return Direction.NORTH;
            }
            return Direction.EAST;
        }

        private boolean isGroundSpotTaken(ServerLevel level, BlockPos feet) {
            AABB box = new AABB(feet.above()).inflate(1.0, 0.5, 1.0);
            for (MillVillager mv : level.getEntitiesOfClass(MillVillager.class, box)) {
                if (!mv.isSleeping() && !mv.isVillagerSleeping()) continue;
                return true;
            }
            return false;
        }

        private float determineSleepAngle(ServerLevel level, BlockPos pos) {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof BedBlock && state.hasProperty((Property)BedBlock.FACING)) {
                return switch ((Direction)state.getValue((Property)BedBlock.FACING)) {
                    case Direction.SOUTH -> 0.0f;
                    case Direction.WEST -> 90.0f;
                    case Direction.NORTH -> 180.0f;
                    case Direction.EAST -> 270.0f;
                    default -> 0.0f;
                };
            }
            BlockPos above = pos.above();
            if (level.getBlockState(above.south()).isAir()) {
                return 0.0f;
            }
            if (level.getBlockState(above.west()).isAir()) {
                return 90.0f;
            }
            if (level.getBlockState(above.north()).isAir()) {
                return 180.0f;
            }
            if (level.getBlockState(above.east()).isAir()) {
                return 270.0f;
            }
            return 0.0f;
        }

        private boolean isBedOccupied(ServerLevel level, BlockPos bedPos) {
            AABB box = new AABB(bedPos).inflate(0.5);
            for (MillVillager mv : level.getEntitiesOfClass(MillVillager.class, box)) {
                if (!mv.isSleeping() && !mv.isVillagerSleeping()) continue;
                return true;
            }
            return false;
        }

        @Override
        public boolean isFinished() {
            if (!this.ticked) {
                return false;
            }
            return this.lastWasDay || this.tickCount >= 12000;
        }

        @Override
        public void stop(GoalContext ctx, StopReason reason) {
            if (ctx == null) {
                return;
            }
            ctx.villager().getNavManager().stop(ctx.villager());
            if (ctx.villager().isSleeping()) {
                ctx.villager().stopSleeping();
            }
            ctx.villager().setVillagerSleeping(false);
        }

        @Override
        public TravelPhase getTravelPhase() {
            return TaskLabels.phaseFor(this.arrived);
        }

        @Override
        @Nullable
        public Component getGoalLabel() {
            return TaskLabels.labelForPhase(this.arrived, "rest");
        }
    }
}

