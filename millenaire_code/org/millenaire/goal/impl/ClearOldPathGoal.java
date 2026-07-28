/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.Items
 *  net.minecraft.world.level.ItemLike
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraft.world.level.block.state.BlockState
 */
package org.millenaire.goal.impl;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.millenaire.config.MillenaireServerConfig;
import org.millenaire.entity.VillagerNavDriver;
import org.millenaire.goal.GoalContext;
import org.millenaire.goal.GoalUtils;
import org.millenaire.goal.ProgressAwareTask;
import org.millenaire.goal.StopReason;
import org.millenaire.goal.TravelPhase;
import org.millenaire.goal.VillagerGoal;
import org.millenaire.goal.VillagerTask;
import org.millenaire.tool.ToolCategory;
import org.millenaire.tool.ToolCategoryRegistry;
import org.millenaire.village.path.VillagePathManager;

public class ClearOldPathGoal
implements VillagerGoal {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)"clear_old_path");

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public int computePriority(GoalContext context) {
        if (GoalUtils.countSimultaneous(context, ID) > 0) {
            return 0;
        }
        return 40;
    }

    @Override
    public boolean canStart(GoalContext context) {
        if (!((Boolean)MillenaireServerConfig.SERVER.buildPaths.get()).booleanValue()) {
            return false;
        }
        if (GoalUtils.countSimultaneous(context, ID) > 0) {
            return false;
        }
        return context.village().getPathManager().hasPathsToClear();
    }

    @Override
    public VillagerTask start(GoalContext context) {
        return new Task(this);
    }

    private class Task
    extends ProgressAwareTask {
        private State state = State.WALKING;
        @Nullable
        private BlockPos target;
        private int clearTimer;
        private float cachedShovelEfficiency = 2.0f;
        private boolean efficiencyComputed = false;
        private List<ItemStack> heldShovel = List.of(new ItemStack((ItemLike)Items.WOODEN_SHOVEL));

        private Task(ClearOldPathGoal clearOldPathGoal) {
        }

        @Override
        public ResourceLocation goalId() {
            return ID;
        }

        @Override
        public List<ItemStack> getHeldItems(TravelPhase phase) {
            return this.heldShovel;
        }

        @Override
        public void tick(GoalContext ctx) {
            if (!this.efficiencyComputed) {
                this.efficiencyComputed = true;
                ToolCategory category = ToolCategoryRegistry.get("toolsshovel");
                if (category != null) {
                    this.cachedShovelEfficiency = category.getBestDestroySpeed(item -> ctx.villager().getInventory().getCount((Item)item) > 0, Blocks.DIRT.defaultBlockState(), Items.WOODEN_SHOVEL);
                    ToolCategory.ToolEntry best = category.getBestOwned(item -> ctx.villager().getInventory().getCount((Item)item) > 0);
                    if (best != null && best.item() != null) {
                        this.heldShovel = List.of(new ItemStack((ItemLike)best.item()));
                    }
                }
            }
            VillagePathManager pm = ctx.village().getPathManager();
            VillagerNavDriver nav = ctx.villager().getNavManager();
            switch (this.state.ordinal()) {
                case 0: {
                    this.target = pm.getNextClearPos();
                    if (this.target == null) {
                        this.state = State.DONE;
                        return;
                    }
                    ServerLevel level = ctx.level();
                    BlockState current = level.getBlockState(this.target);
                    BlockState replacement = VillagePathManager.pathClearReplacement(current, level.getBlockState(this.target.below()));
                    if (current.getBlock() == replacement.getBlock()) {
                        pm.advanceClear();
                        ctx.village().markDirty();
                        if (!pm.hasPathsToClear()) {
                            this.state = State.DONE;
                        }
                        return;
                    }
                    nav.navigateTo(ctx.villager(), this.target, 0.5);
                    if (!nav.isArrivedHorizontal(ctx.villager(), 2.0)) break;
                    nav.stop(ctx.villager());
                    this.state = State.CLEARING;
                    this.clearTimer = 10 - (int)this.cachedShovelEfficiency;
                    return;
                }
                case 1: {
                    --this.clearTimer;
                    if (this.clearTimer > 0) break;
                    ServerLevel level = ctx.level();
                    BlockState current = level.getBlockState(this.target);
                    BlockState replacement = VillagePathManager.pathClearReplacement(current, level.getBlockState(this.target.below()));
                    level.setBlock(this.target, replacement, 3);
                    pm.advanceClear();
                    ctx.village().markDirty();
                    ctx.village().markWaypointGraphDirty();
                    this.reportProgress();
                    if (!pm.hasPathsToClear()) {
                        this.state = State.DONE;
                        break;
                    }
                    this.state = State.WALKING;
                    break;
                }
            }
        }

        @Override
        public boolean isFinished() {
            return this.state == State.DONE;
        }

        @Override
        public void stop(GoalContext context, StopReason reason) {
            if (context == null) {
                return;
            }
            context.villager().getNavManager().stop(context.villager());
        }

        private static enum State {
            WALKING,
            CLEARING,
            DONE;

        }
    }
}

