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
 *  net.minecraft.core.Vec3i
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.tags.BlockTags
 *  net.minecraft.tags.ItemTags
 *  net.minecraft.tags.TagKey
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.Items
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraft.world.level.block.state.BlockState
 *  org.slf4j.Logger
 */
package org.millenaire.goal.gathering.handler;

import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.millenaire.building.BuildingInstance;
import org.millenaire.building.BuildingPlan;
import org.millenaire.culture.ModCultures;
import org.millenaire.entity.VillagerInventory;
import org.millenaire.goal.GoalContext;
import org.millenaire.goal.gathering.GatheringTarget;
import org.millenaire.goal.gathering.GatheringType;
import org.millenaire.goal.gathering.handler.AbstractGatheringHandler;
import org.millenaire.tool.ToolCategory;
import org.millenaire.tool.ToolCategoryRegistry;
import org.millenaire.village.Village;
import org.slf4j.Logger;

public class ChoppingHandler
extends AbstractGatheringHandler {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int GROVE_MARGIN = 3;
    private static final int GROVE_HEIGHT = 20;
    private static final int MAX_LOGS_IN_INVENTORY = 64;
    private static final int MAX_LEAVES_PER_ACTION = 3;
    private static final int SAPLING_DROP_CHANCE = 4;

    @Override
    public String id() {
        return "chopping";
    }

    @Override
    public String getHeldToolCategoryId(GatheringType type) {
        return "toolsaxe";
    }

    @Override
    public int getActionCooldown(GoalContext ctx, GatheringType type) {
        ToolCategory category = ToolCategoryRegistry.get("toolsaxe");
        if (category == null) {
            return type.actionCooldown();
        }
        float efficiency = category.getBestDestroySpeed(item -> ctx.villager().getInventory().getCount((Item)item) > 0, Blocks.OAK_LOG.defaultBlockState(), Items.WOODEN_AXE);
        return 20 - (int)efficiency * 2;
    }

    @Override
    public boolean canStart(GoalContext ctx, GatheringType type) {
        if (this.countLogsInInventory(ctx.villager().getInventory()) >= 64) {
            return false;
        }
        List<BuildingInstance> groves = this.resolveTargetBuildings(ctx, type);
        if (groves.isEmpty()) {
            return false;
        }
        ServerLevel level = ctx.level();
        List<BlockBounds> otherBounds = this.computeOtherBuildingBounds(ctx.village(), groves);
        for (BuildingInstance grove : groves) {
            if (this.findLogInGrove(level, grove, otherBounds) == null) continue;
            return true;
        }
        return false;
    }

    @Override
    @Nullable
    public GatheringTarget findTarget(GoalContext ctx, GatheringType type, @Nullable GatheringTarget lastTarget) {
        ServerLevel level = ctx.level();
        List<BuildingInstance> groves = this.resolveTargetBuildings(ctx, type);
        if (groves.isEmpty()) {
            return null;
        }
        List<BlockBounds> otherBounds = this.computeOtherBuildingBounds(ctx.village(), groves);
        if (lastTarget != null) {
            BlockPos found;
            BlockPos lastPos = lastTarget.navigationPos();
            BlockPos below = this.findLogBelow(level, lastPos);
            if (below != null && this.isInAnyGrove(below, groves) && !this.isInAnyOtherBuilding(below, otherBounds)) {
                return new GatheringTarget.BlockTarget(below);
            }
            BuildingInstance currentGrove = this.findContainingGrove(lastPos, groves);
            if (currentGrove != null && (found = this.findLogInGrove(level, currentGrove, otherBounds)) != null) {
                return new GatheringTarget.BlockTarget(found);
            }
        }
        BlockPos villagerPos = ctx.villager().blockPosition();
        double bestDistSq = Double.MAX_VALUE;
        BlockPos bestLog = null;
        for (BuildingInstance grove : groves) {
            double distSq;
            BlockPos log = this.findLogInGrove(level, grove, otherBounds);
            if (log == null || !((distSq = villagerPos.distSqr((Vec3i)grove.getOrigin())) < bestDistSq)) continue;
            bestDistSq = distSq;
            bestLog = log;
        }
        return bestLog != null ? new GatheringTarget.BlockTarget(bestLog) : null;
    }

    @Override
    public boolean performAction(GoalContext ctx, GatheringType type, GatheringTarget target) {
        if (!(target instanceof GatheringTarget.BlockTarget)) {
            return true;
        }
        GatheringTarget.BlockTarget blockTarget = (GatheringTarget.BlockTarget)target;
        BlockPos pos = blockTarget.pos();
        ServerLevel level = ctx.level();
        BlockState state = level.getBlockState(pos);
        if (!state.is(BlockTags.LOGS)) {
            return true;
        }
        List drops = Block.getDrops((BlockState)state, (ServerLevel)level, (BlockPos)pos, null);
        level.destroyBlock(pos, false);
        VillagerInventory inventory = ctx.villager().getInventory();
        for (ItemStack drop : drops) {
            inventory.add(drop.getItem(), drop.getCount());
        }
        this.cutNearbyLeaves(level, pos, inventory);
        this.breakNearbyBeeNests(level, pos);
        return true;
    }

    private void breakNearbyBeeNests(ServerLevel level, BlockPos logPos) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dy = -1; dy <= 1; ++dy) {
            for (Direction facing : Direction.Plane.HORIZONTAL) {
                BlockState state;
                cursor.set(logPos.getX() + facing.getStepX(), logPos.getY() + dy, logPos.getZ() + facing.getStepZ());
                if (!level.isLoaded((BlockPos)cursor) || !(state = level.getBlockState((BlockPos)cursor)).is(Blocks.BEE_NEST)) continue;
                BlockPos nestPos = cursor.immutable();
                level.removeBlock(nestPos, false);
                LOGGER.debug("Lumberjack removed bee_nest at {} (chopped log at {})", (Object)nestPos, (Object)logPos);
            }
        }
    }

    private void cutNearbyLeaves(ServerLevel level, BlockPos logPos, VillagerInventory inventory) {
        int leavesCut = 0;
        Random random = new Random();
        for (int dy = 0; dy <= 3; ++dy) {
            for (int dx = -1; dx <= 1; ++dx) {
                for (int dz = -1; dz <= 1; ++dz) {
                    Item sapling;
                    BlockState leafState;
                    BlockPos leafPos;
                    if (leavesCut >= 3) {
                        return;
                    }
                    if (dx == 0 && dy == 0 && dz == 0 || !level.isLoaded(leafPos = logPos.offset(dx, dy, dz)) || !(leafState = level.getBlockState(leafPos)).is(BlockTags.LEAVES)) continue;
                    level.destroyBlock(leafPos, false);
                    ++leavesCut;
                    if (random.nextInt(4) != 0 || (sapling = this.resolveSaplingFromLeaf(leafState)) == null) continue;
                    inventory.add(sapling, 1);
                }
            }
        }
    }

    @Nullable
    private Item resolveSaplingFromLeaf(BlockState leafState) {
        Block block = leafState.getBlock();
        if (block == Blocks.OAK_LEAVES) {
            return Items.OAK_SAPLING;
        }
        if (block == Blocks.SPRUCE_LEAVES) {
            return Items.SPRUCE_SAPLING;
        }
        if (block == Blocks.BIRCH_LEAVES) {
            return Items.BIRCH_SAPLING;
        }
        if (block == Blocks.JUNGLE_LEAVES) {
            return Items.JUNGLE_SAPLING;
        }
        if (block == Blocks.ACACIA_LEAVES) {
            return Items.ACACIA_SAPLING;
        }
        if (block == Blocks.DARK_OAK_LEAVES) {
            return Items.DARK_OAK_SAPLING;
        }
        if (block == Blocks.CHERRY_LEAVES) {
            return Items.CHERRY_SAPLING;
        }
        if (block == Blocks.MANGROVE_LEAVES) {
            return Items.MANGROVE_PROPAGULE;
        }
        return Items.OAK_SAPLING;
    }

    private int countLogsInInventory(VillagerInventory inventory) {
        return inventory.getCountByTag((TagKey<Item>)ItemTags.LOGS);
    }

    @Nullable
    private BlockPos findLogInGrove(ServerLevel level, BuildingInstance grove, List<BlockBounds> otherBounds) {
        BlockBounds bounds = this.computeGroveBounds(grove);
        if (bounds == null) {
            return null;
        }
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        BlockPos best = null;
        int bestY = Integer.MIN_VALUE;
        for (int x = bounds.minX; x <= bounds.maxX; ++x) {
            block1: for (int z = bounds.minZ; z <= bounds.maxZ; ++z) {
                for (int y = bounds.maxY; y >= bounds.minY; --y) {
                    mutable.set(x, y, z);
                    if (!level.isLoaded((BlockPos)mutable) || !level.getBlockState((BlockPos)mutable).is(BlockTags.LOGS)) continue;
                    if (this.isInAnyOtherBuilding((BlockPos)mutable, otherBounds) || y <= bestY) continue block1;
                    bestY = y;
                    best = mutable.immutable();
                    continue block1;
                }
            }
        }
        return best;
    }

    @Nullable
    private BlockPos findLogBelow(ServerLevel level, BlockPos pos) {
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos(pos.getX(), pos.getY() - 1, pos.getZ());
        for (int dy = 1; dy <= 20; ++dy) {
            mutable.setY(pos.getY() - dy);
            if (!level.isLoaded((BlockPos)mutable)) break;
            BlockState state = level.getBlockState((BlockPos)mutable);
            if (state.is(BlockTags.LOGS)) {
                return mutable.immutable();
            }
            if (!state.isAir() && !state.is(BlockTags.LEAVES)) break;
        }
        return null;
    }

    private boolean isInAnyGrove(BlockPos pos, List<BuildingInstance> groves) {
        for (BuildingInstance grove : groves) {
            BlockBounds bounds = this.computeGroveBounds(grove);
            if (bounds == null || !bounds.contains(pos)) continue;
            return true;
        }
        return false;
    }

    @Nullable
    private BuildingInstance findContainingGrove(BlockPos pos, List<BuildingInstance> groves) {
        for (BuildingInstance grove : groves) {
            BlockBounds bounds = this.computeGroveBounds(grove);
            if (bounds == null || !bounds.contains(pos)) continue;
            return grove;
        }
        return null;
    }

    @Nullable
    private BlockBounds computeGroveBounds(BuildingInstance grove) {
        BuildingPlan plan = ModCultures.getBuildingPlan(grove.getPlanId());
        if (plan == null) {
            return null;
        }
        int[] rect = ChoppingHandler.computeFootprintRect(grove, plan, 3);
        return new BlockBounds(rect[0], grove.getOrigin().getY(), rect[1], rect[0] + rect[2] - 1, grove.getOrigin().getY() + 20, rect[1] + rect[3] - 1);
    }

    private List<BlockBounds> computeOtherBuildingBounds(Village village, List<BuildingInstance> groves) {
        ArrayList<BlockBounds> result = new ArrayList<BlockBounds>();
        for (BuildingInstance b : village.getBuildings()) {
            BuildingPlan plan;
            if (!b.isOperational() || groves.contains(b) || (plan = ModCultures.getBuildingPlan(b.getPlanId())) == null) continue;
            int[] rect = ChoppingHandler.computeFootprintRect(b, plan, 0);
            result.add(new BlockBounds(rect[0], b.getOrigin().getY(), rect[1], rect[0] + rect[2] - 1, b.getOrigin().getY() + plan.height() - 1, rect[1] + rect[3] - 1));
        }
        return result;
    }

    private static int[] computeFootprintRect(BuildingInstance building, BuildingPlan plan, int margin) {
        int[] arrn;
        int ox = building.getOrigin().getX();
        int oz = building.getOrigin().getZ();
        int w = plan.width();
        int d = plan.depth();
        switch (building.getRotation()) {
            case CLOCKWISE_90: {
                int[] arrn2 = new int[4];
                arrn2[0] = ox - d + 1 - margin;
                arrn2[1] = oz - margin;
                arrn2[2] = d + 2 * margin;
                arrn = arrn2;
                arrn2[3] = w + 2 * margin;
                break;
            }
            case CLOCKWISE_180: {
                int[] arrn3 = new int[4];
                arrn3[0] = ox - w + 1 - margin;
                arrn3[1] = oz - d + 1 - margin;
                arrn3[2] = w + 2 * margin;
                arrn = arrn3;
                arrn3[3] = d + 2 * margin;
                break;
            }
            case COUNTERCLOCKWISE_90: {
                int[] arrn4 = new int[4];
                arrn4[0] = ox - margin;
                arrn4[1] = oz - w + 1 - margin;
                arrn4[2] = d + 2 * margin;
                arrn = arrn4;
                arrn4[3] = w + 2 * margin;
                break;
            }
            default: {
                int[] arrn5 = new int[4];
                arrn5[0] = ox - margin;
                arrn5[1] = oz - margin;
                arrn5[2] = w + 2 * margin;
                arrn = arrn5;
                arrn5[3] = d + 2 * margin;
            }
        }
        return arrn;
    }

    private boolean isInAnyOtherBuilding(BlockPos pos, List<BlockBounds> otherBounds) {
        for (BlockBounds bounds : otherBounds) {
            if (!bounds.contains(pos)) continue;
            return true;
        }
        return false;
    }

    private record BlockBounds(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        boolean contains(BlockPos pos) {
            return pos.getX() >= this.minX && pos.getX() <= this.maxX && pos.getY() >= this.minY && pos.getY() <= this.maxY && pos.getZ() >= this.minZ && pos.getZ() <= this.maxZ;
        }
    }
}

