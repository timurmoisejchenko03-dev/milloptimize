/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.core.Direction$Axis
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
 *  net.minecraft.world.phys.shapes.VoxelShape
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
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.millenaire.block.MillPathBlock;
import org.millenaire.block.MillPathSlabBlock;
import org.millenaire.block.PathTier;
import org.millenaire.block.RicePaddyBlock;
import org.millenaire.culture.ModCultures;
import org.millenaire.culture.VillagerType;
import org.millenaire.entity.MillVillager;
import org.millenaire.village.Village;

public class MillWalkNodeEvaluatorMC
extends WalkNodeEvaluator {
    @Nullable
    private final Mob mob;
    @Nullable
    private Village cachedVillage;
    private long cachedVillageTick = Long.MIN_VALUE;
    private boolean cachedNoLeafClearing;
    private final Node[] reusableNeighbors;

    public MillWalkNodeEvaluatorMC() {
        this(null);
    }

    public MillWalkNodeEvaluatorMC(@Nullable Mob mob) {
        this.mob = mob;
        this.reusableNeighbors = new Node[Direction.Plane.HORIZONTAL.length()];
    }

    public PathType getPathType(PathfindingContext context, int x, int y, int z) {
        BlockState state;
        BlockState belowState;
        PathType base = super.getPathType(context, x, y, z);
        BlockState here = context.level().getBlockState(new BlockPos(x, y, z));
        if (MillWalkNodeEvaluatorMC.isLitCampfire(here)) {
            return PathType.BLOCKED;
        }
        if (y > context.level().getMinBuildHeight() && ((belowState = context.level().getBlockState(new BlockPos(x, y - 1, z))).is(BlockTags.FENCES) || belowState.is(BlockTags.WALLS) || belowState.is(Blocks.IRON_BARS))) {
            return PathType.BLOCKED;
        }
        if (base == PathType.FENCE) {
            state = context.level().getBlockState(new BlockPos(x, y, z));
            if (state.getBlock() instanceof FenceGateBlock && !((Boolean)state.getValue((Property)FenceGateBlock.OPEN)).booleanValue() && this.canOpenDoors()) {
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
            BlockState belowState2;
            BlockState hereState = context.level().getBlockState(new BlockPos(x, y, z));
            if (hereState.getBlock() instanceof MillPathBlock || hereState.getBlock() instanceof MillPathSlabBlock) {
                return PathType.COCOA;
            }
            if (y > context.level().getMinBuildHeight() && ((belowState2 = context.level().getBlockState(new BlockPos(x, y - 1, z))).getBlock() instanceof MillPathBlock || belowState2.getBlock() instanceof MillPathSlabBlock)) {
                return PathType.COCOA;
            }
        }
        return base;
    }

    public int getNeighbors(Node[] outputArray, Node p_node) {
        int i = 0;
        int j = 0;
        PathType pathtype = this.getCachedPathType(p_node.x, p_node.y + 1, p_node.z);
        PathType pathtype1 = this.getCachedPathType(p_node.x, p_node.y, p_node.z);
        if (this.mob.getPathfindingMalus(pathtype) >= 0.0f && pathtype1 != PathType.STICKY_HONEY) {
            j = Mth.floor((float)Math.max(1.0f, this.mob.maxUpStep()));
        }
        double d0 = this.getFloorLevel(new BlockPos(p_node.x, p_node.y, p_node.z));
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            Node node;
            this.reusableNeighbors[direction.get2DDataValue()] = node = this.findAcceptedNode(p_node.x + direction.getStepX(), p_node.y, p_node.z + direction.getStepZ(), j, d0, direction, pathtype1);
            if (!this.isNeighborValid(node, p_node)) continue;
            outputArray[i++] = node;
        }
        for (Direction direction1 : Direction.Plane.HORIZONTAL) {
            Node node1;
            Direction direction2 = direction1.getClockWise();
            if (!this.isDiagonalValid(p_node, this.reusableNeighbors[direction1.get2DDataValue()], this.reusableNeighbors[direction2.get2DDataValue()]) || !this.isDiagonalValid(node1 = this.findAcceptedNode(p_node.x + direction1.getStepX() + direction2.getStepX(), p_node.y, p_node.z + direction1.getStepZ() + direction2.getStepZ(), j, d0, direction1, pathtype1))) continue;
            outputArray[i++] = node1;
        }
        return i;
    }

    protected Node findAcceptedNode(int x, int y, int z, int verticalDeltaLimit, double nodeFloorLevel, Direction direction, PathType pathType) {
        PathTier tier;
        Node node = super.findAcceptedNode(x, y, z, verticalDeltaLimit, nodeFloorLevel, direction, pathType);
        if (node != null && node.type == PathType.COCOA && (tier = this.resolvePathTier(node.x, node.y, node.z)) != null && tier.preferenceMalus() > node.costMalus) {
            node.costMalus = tier.preferenceMalus();
        }
        return node;
    }

    protected double getFloorLevel(BlockPos pos) {
        double maxY;
        VoxelShape shape;
        BlockState here;
        if (this.currentContext != null && !(here = this.currentContext.getBlockState(pos)).isAir() && here.getFluidState().isEmpty() && !(shape = here.getCollisionShape((BlockGetter)this.currentContext.level(), pos)).isEmpty() && (maxY = shape.max(Direction.Axis.Y)) > 0.0 && maxY < 1.0) {
            return (double)pos.getY() + maxY;
        }
        return super.getFloorLevel(pos);
    }

    private PathTier resolvePathTier(int x, int y, int z) {
        BlockState hereState = this.currentContext.level().getBlockState(new BlockPos(x, y, z));
        PathTier tier = MillWalkNodeEvaluatorMC.tierOf(hereState.getBlock());
        if (tier != null) {
            return tier;
        }
        if (y > this.currentContext.level().getMinBuildHeight()) {
            BlockState belowState = this.currentContext.level().getBlockState(new BlockPos(x, y - 1, z));
            return MillWalkNodeEvaluatorMC.tierOf(belowState.getBlock());
        }
        return null;
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
        return this.cachedVillage != null && this.cachedVillage.getBuildingAt(new BlockPos(x, y, z)) != null;
    }

    static boolean isLitCampfire(BlockState state) {
        return state.getBlock() instanceof CampfireBlock && state.hasProperty((Property)CampfireBlock.LIT) && (Boolean)state.getValue((Property)CampfireBlock.LIT) != false;
    }
}

