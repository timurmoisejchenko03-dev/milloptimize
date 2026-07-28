/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.BlockPos$MutableBlockPos
 *  net.minecraft.core.Direction$Axis
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.levelgen.Heightmap$Types
 *  net.minecraft.world.phys.shapes.VoxelShape
 *  org.slf4j.Logger
 */
package org.millenaire.goal;

import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.IntPredicate;
import java.util.function.IntToDoubleFunction;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.millenaire.entity.BlockHazards;
import org.millenaire.entity.MillVillager;
import org.slf4j.Logger;

public final class NavigationHelperUtils {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int TELEPORT_FACTOR = 4;
    private static final int RANDOM_SAFE_ATTEMPTS = 20;
    private static final int RANDOM_SAFE_RADIUS = 2;

    private NavigationHelperUtils() {
    }

    public static double horizontalDistSq(BlockPos a, BlockPos b) {
        double dx = a.getX() - b.getX();
        double dz = a.getZ() - b.getZ();
        return dx * dx + dz * dz;
    }

    public static boolean isSafeLanding(Level level, BlockPos pos) {
        if (level.getBlockState(pos).isSuffocating((BlockGetter)level, pos)) {
            return false;
        }
        if (level.getBlockState(pos.above()).isSuffocating((BlockGetter)level, pos.above())) {
            return false;
        }
        return !BlockHazards.isHazardousAt((BlockGetter)level, pos);
    }

    public static double collisionTopOffset(BlockGetter level, BlockPos pos, BlockState state) {
        if (state.isAir() || !state.getFluidState().isEmpty()) {
            return 0.0;
        }
        VoxelShape shape = state.getCollisionShape(level, pos);
        if (shape.isEmpty()) {
            return 0.0;
        }
        double maxY = shape.max(Direction.Axis.Y);
        return maxY < 0.0 ? 0.0 : maxY;
    }

    public static int villagerStandY(Level level, int x, int z) {
        int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        int minY = level.getMinBuildHeight();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        return NavigationHelperUtils.resolveStandY(top, minY, y -> {
            cursor.set(x, y, z);
            return level.getBlockState((BlockPos)cursor).isAir();
        }, y -> {
            cursor.set(x, y, z);
            return NavigationHelperUtils.collisionTopOffset((BlockGetter)level, (BlockPos)cursor, level.getBlockState((BlockPos)cursor));
        });
    }

    public static int villagerStandYAt(Level level, BlockPos pos) {
        return NavigationHelperUtils.villagerStandY(level, pos.getX(), pos.getZ());
    }

    static int resolveStandY(int topY, int minY, IntPredicate isAir, IntToDoubleFunction collisionTopAt) {
        for (int y = topY; y > minY; --y) {
            if (isAir.test(y) || isAir.test(y - 1)) continue;
            return collisionTopAt.applyAsDouble(y) >= 1.0 ? y + 1 : y;
        }
        return topY;
    }

    public static void teleportToSafe(MillVillager villager, BlockPos target) {
        Level level = villager.level();
        if (NavigationHelperUtils.isSafeLanding(level, target)) {
            villager.teleportTo((double)target.getX() + 0.5, target.getY(), (double)target.getZ() + 0.5);
            villager.getNavigation().stop();
            return;
        }
        if (NavigationHelperUtils.tryTeleportTowards(villager, target)) {
            return;
        }
        BlockPos outdoorPos = villager.getLastOutdoorPos();
        if (outdoorPos != null && NavigationHelperUtils.isSafeLanding(level, outdoorPos)) {
            LOGGER.debug("[Millenaire] NavHelperUtils \u2014 TP to lastOutdoorPos {}", (Object)outdoorPos.toShortString());
            villager.teleportTo((double)outdoorPos.getX() + 0.5, outdoorPos.getY(), (double)outdoorPos.getZ() + 0.5);
            villager.getNavigation().stop();
            return;
        }
        BlockPos safePos = NavigationHelperUtils.findRandomSafePos(level, villager.blockPosition());
        if (safePos != null) {
            LOGGER.debug("[Millenaire] NavHelperUtils \u2014 TP to random safe pos {}", (Object)safePos.toShortString());
            villager.teleportTo((double)safePos.getX() + 0.5, safePos.getY(), (double)safePos.getZ() + 0.5);
            villager.getNavigation().stop();
            return;
        }
        int surfaceY = NavigationHelperUtils.villagerStandY(level, target.getX(), target.getZ());
        BlockPos surfacePos = new BlockPos(target.getX(), surfaceY, target.getZ());
        for (int nudge = 0; nudge < 5; ++nudge) {
            BlockPos candidate = surfacePos.above(nudge);
            if (!NavigationHelperUtils.isSafeLanding(level, candidate)) continue;
            villager.teleportTo((double)candidate.getX() + 0.5, candidate.getY(), (double)candidate.getZ() + 0.5);
            villager.getNavigation().stop();
            return;
        }
        LOGGER.warn("[Millenaire] NavHelperUtils \u2014 Step 5 fallback may suffocate at {}", (Object)surfacePos.toShortString());
        villager.teleportTo((double)surfacePos.getX() + 0.5, surfacePos.getY(), (double)surfacePos.getZ() + 0.5);
        villager.getNavigation().stop();
    }

    public static void teleportToSafeNearTarget(MillVillager villager, BlockPos target) {
        NavigationHelperUtils.teleportToSafeNearTarget(villager, target, p -> false);
    }

    public static void teleportToSafeNearTarget(MillVillager villager, BlockPos target, Predicate<BlockPos> excluded) {
        Level level = villager.level();
        if (!excluded.test(target) && NavigationHelperUtils.isSafeLanding(level, target)) {
            villager.teleportTo((double)target.getX() + 0.5, target.getY(), (double)target.getZ() + 0.5);
            villager.getNavigation().stop();
            return;
        }
        if (NavigationHelperUtils.tryTeleportTowards(villager, target)) {
            return;
        }
        BlockPos safePos = NavigationHelperUtils.findRandomSafePos(level, target, excluded);
        if (safePos != null) {
            LOGGER.debug("[Millenaire] NavHelperUtils \u2014 TP safe near target: {}", (Object)safePos.toShortString());
            villager.teleportTo((double)safePos.getX() + 0.5, safePos.getY(), (double)safePos.getZ() + 0.5);
            villager.getNavigation().stop();
            return;
        }
        int surfaceY = NavigationHelperUtils.villagerStandY(level, target.getX(), target.getZ());
        BlockPos surfacePos = new BlockPos(target.getX(), surfaceY, target.getZ());
        for (int nudge = 0; nudge < 5; ++nudge) {
            BlockPos candidate = surfacePos.above(nudge);
            if (!NavigationHelperUtils.isSafeLanding(level, candidate)) continue;
            villager.teleportTo((double)candidate.getX() + 0.5, candidate.getY(), (double)candidate.getZ() + 0.5);
            villager.getNavigation().stop();
            return;
        }
        LOGGER.warn("[Millenaire] NavHelperUtils \u2014 target fallback unsafe at {}", (Object)surfacePos.toShortString());
        villager.teleportTo((double)surfacePos.getX() + 0.5, surfacePos.getY(), (double)surfacePos.getZ() + 0.5);
        villager.getNavigation().stop();
    }

    public static boolean tryTeleportTowards(MillVillager villager, BlockPos dest) {
        BlockPos current = villager.blockPosition();
        int dx = dest.getX() - current.getX();
        int dz = dest.getZ() - current.getZ();
        if (Math.abs(dx) <= 4 && Math.abs(dz) <= 4) {
            return false;
        }
        int xDir = Integer.signum(dx);
        int zDir = Integer.signum(dz);
        int targetX = current.getX() + xDir * 4;
        int targetZ = current.getZ() + zDir * 4;
        int surfaceY = NavigationHelperUtils.villagerStandY(villager.level(), targetX, targetZ);
        Level level = villager.level();
        for (int nudge = 0; nudge <= 1; ++nudge) {
            BlockPos candidate = new BlockPos(targetX, surfaceY + nudge, targetZ);
            if (!NavigationHelperUtils.isSafeLanding(level, candidate)) continue;
            LOGGER.debug("[Millenaire] NavHelperUtils \u2014 partial TP towards dest: {}", (Object)candidate.toShortString());
            villager.teleportTo((double)candidate.getX() + 0.5, candidate.getY(), (double)candidate.getZ() + 0.5);
            villager.getNavigation().stop();
            return true;
        }
        return false;
    }

    @Nullable
    public static BlockPos findRandomSafePos(Level level, BlockPos origin) {
        return NavigationHelperUtils.findRandomSafePos(level, origin, p -> false);
    }

    @Nullable
    public static BlockPos findRandomSafePos(Level level, BlockPos origin, Predicate<BlockPos> excluded) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        ArrayList<BlockPos> candidates = new ArrayList<BlockPos>(20);
        for (int i = 0; i < 20; ++i) {
            BlockPos below;
            BlockPos test;
            int rdx = random.nextInt(-2, 3);
            int dy = random.nextInt(-1, 2);
            int rdz = random.nextInt(-2, 3);
            if (rdx == 0 && dy == 0 && rdz == 0 || excluded.test(test = origin.offset(rdx, dy, rdz)) || !level.getBlockState(below = test.below()).isSolidRender((BlockGetter)level, below) || !NavigationHelperUtils.isSafeLanding(level, test)) continue;
            candidates.add(test);
        }
        if (candidates.isEmpty()) {
            return null;
        }
        return (BlockPos)candidates.get(random.nextInt(candidates.size()));
    }
}

