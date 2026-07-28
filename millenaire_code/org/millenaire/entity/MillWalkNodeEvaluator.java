/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.core.Direction$Plane
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.tags.BlockTags
 *  net.minecraft.util.Mth
 *  net.minecraft.world.entity.Mob
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraft.world.level.block.CampfireBlock
 *  net.minecraft.world.level.block.FenceGateBlock
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.properties.Property
 *  net.minecraft.world.level.pathfinder.Node
 *  net.minecraft.world.level.pathfinder.PathType
 *  net.minecraft.world.level.pathfinder.PathfindingContext
 *  net.minecraft.world.level.pathfinder.WalkNodeEvaluator
 *  org.jetbrains.annotations.Nullable
 */
package org.millenaire.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.PathfindingContext;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import org.jetbrains.annotations.Nullable;
import org.millenaire.block.MillPathBlock;
import org.millenaire.block.MillPathSlabBlock;
import org.millenaire.block.PathTier;
import org.millenaire.block.RicePaddyBlock;
import org.millenaire.culture.ModCultures;
import org.millenaire.culture.VillagerType;
import org.millenaire.entity.MillVillager;
import org.millenaire.goal.NavigationHelperUtils;
import org.millenaire.village.Village;

public class MillWalkNodeEvaluator
extends WalkNodeEvaluator {
    static final float ON_TOP_OF_BARRIER_MALUS = 16.0f;
    @Nullable
    private final Mob mob;
    @Nullable
    private Village cachedVillage;
    private long cachedVillageTick = Long.MIN_VALUE;
    private boolean cachedNoLeafClearing;
    private final Node[] millReusableNeighbors = new Node[Direction.Plane.HORIZONTAL.length()];

    public MillWalkNodeEvaluator() {
        this(null);
    }

    public MillWalkNodeEvaluator(@Nullable Mob mob) {
        this.mob = mob;
    }

    public PathType getPathType(PathfindingContext context, int x, int y, int z) {
        BlockState state;
        PathType base = super.getPathType(context, x, y, z);
        BlockState here = context.level().getBlockState(new BlockPos(x, y, z));
        if (MillWalkNodeEvaluator.isLitCampfire(here)) {
            return PathType.BLOCKED;
        }
        if (base == PathType.FENCE) {
            BlockState state2 = context.level().getBlockState(new BlockPos(x, y, z));
            if (state2.getBlock() instanceof FenceGateBlock && !((Boolean)state2.getValue((Property)FenceGateBlock.OPEN)).booleanValue() && this.canOpenDoors()) {
                return PathType.DOOR_WOOD_CLOSED;
            }
            return PathType.BLOCKED;
        }
        if (base == PathType.LEAVES) {
            if (this.isLeafTraversalBlocked(x, y, z)) {
                return PathType.BLOCKED;
            }
            return PathType.OPEN;
        }
        if ((base == PathType.WATER || base == PathType.WATER_BORDER) && (state = context.level().getBlockState(new BlockPos(x, y, z))).getBlock() instanceof RicePaddyBlock) {
            return PathType.OPEN;
        }
        if (base == PathType.WALKABLE) {
            BlockState belowState;
            BlockState hereState = context.level().getBlockState(new BlockPos(x, y, z));
            if (hereState.getBlock() instanceof MillPathBlock || hereState.getBlock() instanceof MillPathSlabBlock) {
                return PathType.COCOA;
            }
            if (y > context.level().getMinBuildHeight() && ((belowState = context.level().getBlockState(new BlockPos(x, y - 1, z))).getBlock() instanceof MillPathBlock || belowState.getBlock() instanceof MillPathSlabBlock)) {
                return PathType.COCOA;
            }
        }
        return base;
    }

    public int getNeighbors(Node[] outputArray, Node node) {
        int count = 0;
        int verticalDeltaLimit = 0;
        Mob pathMob = ((WalkNodeEvaluator)this).mob;
        PathType above = this.getCachedPathType(node.x, node.y + 1, node.z);
        PathType here = this.getCachedPathType(node.x, node.y, node.z);
        if (pathMob.getPathfindingMalus(above) >= 0.0f && here != PathType.STICKY_HONEY) {
            verticalDeltaLimit = Mth.floor((float)Math.max(1.0f, pathMob.maxUpStep()));
            if (this.isOnPathTile(node.x, node.y, node.z)) {
                ++verticalDeltaLimit;
            }
        }
        double floor = this.getFloorLevel(new BlockPos(node.x, node.y, node.z));
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            Node neighbor;
            this.millReusableNeighbors[direction.get2DDataValue()] = neighbor = this.findAcceptedNode(node.x + direction.getStepX(), node.y, node.z + direction.getStepZ(), verticalDeltaLimit, floor, direction, here);
            if (!this.isNeighborValid(neighbor, node)) continue;
            outputArray[count++] = neighbor;
        }
        for (Direction direction1 : Direction.Plane.HORIZONTAL) {
            Node diagonal;
            Direction direction2 = direction1.getClockWise();
            if (!this.isDiagonalValid(node, this.millReusableNeighbors[direction1.get2DDataValue()], this.millReusableNeighbors[direction2.get2DDataValue()]) || !this.isDiagonalValid(diagonal = this.findAcceptedNode(node.x + direction1.getStepX() + direction2.getStepX(), node.y, node.z + direction1.getStepZ() + direction2.getStepZ(), verticalDeltaLimit, floor, direction1, here))) continue;
            outputArray[count++] = diagonal;
        }
        return count;
    }

    private boolean isOnPathTile(int x, int y, int z) {
        if (this.currentContext == null) {
            return false;
        }
        BlockState hereState = this.currentContext.getBlockState(new BlockPos(x, y, z));
        if (hereState.getBlock() instanceof MillPathBlock || hereState.getBlock() instanceof MillPathSlabBlock) {
            return true;
        }
        if (y > this.currentContext.level().getMinBuildHeight()) {
            BlockState belowState = this.currentContext.getBlockState(new BlockPos(x, y - 1, z));
            return belowState.getBlock() instanceof MillPathBlock || belowState.getBlock() instanceof MillPathSlabBlock;
        }
        return false;
    }

    protected Node findAcceptedNode(int x, int y, int z, int verticalDeltaLimit, double nodeFloorLevel, Direction direction, PathType pathType) {
        PathTier tier;
        Node pathLanding;
        Node node = super.findAcceptedNode(x, y, z, verticalDeltaLimit, nodeFloorLevel, direction, pathType);
        if ((node == null || node.costMalus < 0.0f) && this.getCachedPathType(x, y, z) == PathType.OPEN && (pathLanding = this.tryPathLandingBelow(x, y, z)) != null) {
            node = pathLanding;
        }
        if (node != null && node.type == PathType.COCOA && (tier = this.resolvePathTier(node.x, node.y, node.z)) != null && tier.preferenceMalus() > node.costMalus) {
            node.costMalus = tier.preferenceMalus();
        }
        if (node != null && this.isOnTopOfBarrier(node.x, node.y, node.z)) {
            node.costMalus = Math.max(node.costMalus, 16.0f);
        }
        return node;
    }

    @Nullable
    private Node tryPathLandingBelow(int x, int y, int z) {
        int maxFall = ((WalkNodeEvaluator)this).mob.getMaxFallDistance() + 1;
        int minY = this.currentContext.level().getMinBuildHeight();
        for (int i = y - 1; i >= minY; --i) {
            if (y - i > maxFall) {
                return null;
            }
            PathType belowType = this.getCachedPathType(x, i, z);
            if (belowType == PathType.OPEN) continue;
            float malus = ((WalkNodeEvaluator)this).mob.getPathfindingMalus(belowType);
            if (malus >= 0.0f && this.isOnPathTile(x, i, z)) {
                Node node = this.getNode(x, i, z);
                node.type = belowType;
                node.costMalus = Math.max(node.costMalus, malus);
                return node;
            }
            return null;
        }
        return null;
    }

    protected double getFloorLevel(BlockPos pos) {
        double maxY;
        BlockState here;
        if (this.currentContext != null && !(here = this.currentContext.getBlockState(pos)).isAir() && here.getFluidState().isEmpty() && (maxY = NavigationHelperUtils.collisionTopOffset((BlockGetter)this.currentContext.level(), pos, here)) > 0.0 && maxY < 1.0) {
            return (double)pos.getY() + maxY;
        }
        return super.getFloorLevel(pos);
    }

    private PathTier resolvePathTier(int x, int y, int z) {
        BlockState hereState = this.currentContext.level().getBlockState(new BlockPos(x, y, z));
        PathTier tier = MillWalkNodeEvaluator.tierOf(hereState.getBlock());
        if (tier != null) {
            return tier;
        }
        if (y > this.currentContext.level().getMinBuildHeight()) {
            BlockState belowState = this.currentContext.level().getBlockState(new BlockPos(x, y - 1, z));
            return MillWalkNodeEvaluator.tierOf(belowState.getBlock());
        }
        return null;
    }

    private boolean isOnTopOfBarrier(int x, int y, int z) {
        if (this.currentContext == null) {
            return false;
        }
        if (y <= this.currentContext.level().getMinBuildHeight()) {
            return false;
        }
        BlockState belowState = this.currentContext.level().getBlockState(new BlockPos(x, y - 1, z));
        return belowState.is(BlockTags.FENCES) || belowState.is(BlockTags.WALLS) || belowState.is(Blocks.IRON_BARS);
    }

    private static PathTier tierOf(Block block) {
        if (block instanceof MillPathBlock) {
            MillPathBlock mpb = (MillPathBlock)block;
            return mpb.tier();
        }
        if (block instanceof MillPathSlabBlock) {
            MillPathSlabBlock msb = (MillPathSlabBlock)block;
            return msb.tier();
        }
        return null;
    }

    private boolean isLeafTraversalBlocked(int x, int y, int z) {
        Mob mob = this.mob;
        if (!(mob instanceof MillVillager)) {
            return false;
        }
        MillVillager v = (MillVillager)mob;
        Level level = this.mob.level();
        if (!(level instanceof ServerLevel)) {
            return false;
        }
        ServerLevel sl = (ServerLevel)level;
        long tick = sl.getGameTime();
        if (tick != this.cachedVillageTick) {
            VillagerType vt;
            this.cachedVillage = v.getVillageId() != null ? Village.resolve(sl, v.getVillageId()) : null;
            ResourceLocation typeId = v.getVillagerTypeId();
            this.cachedNoLeafClearing = typeId == null ? true : (vt = ModCultures.getVillagerType(typeId)) != null && vt.hasTag("noleafclearing");
            this.cachedVillageTick = tick;
        }
        if (this.cachedNoLeafClearing) {
            return true;
        }
        return this.cachedVillage != null && this.cachedVillage.getOperationalBuildingAt(new BlockPos(x, y, z)) != null;
    }

    static boolean isLitCampfire(BlockState state) {
        return state.getBlock() instanceof CampfireBlock && state.hasProperty((Property)CampfireBlock.LIT) && (Boolean)state.getValue((Property)CampfireBlock.LIT) != false;
    }
}

