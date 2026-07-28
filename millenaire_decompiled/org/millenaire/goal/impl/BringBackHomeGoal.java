/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 *  net.minecraft.network.chat.Component
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.level.Level
 *  org.slf4j.Logger
 */
package org.millenaire.goal.impl;

import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import org.millenaire.building.BuildingId;
import org.millenaire.building.BuildingInstance;
import org.millenaire.building.BuildingInventory;
import org.millenaire.culture.ModCultures;
import org.millenaire.culture.VillagerType;
import org.millenaire.entity.VillagerInventory;
import org.millenaire.goal.GoalContext;
import org.millenaire.goal.PerVillagerThrottle;
import org.millenaire.goal.TaskLabels;
import org.millenaire.goal.VillagerGoal;
import org.millenaire.goal.VillagerTask;
import org.millenaire.goal.impl.WalkThenActTask;
import org.slf4j.Logger;

public class BringBackHomeGoal
implements VillagerGoal {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)"bring_back_home");
    private static final int STANDARD_DELAY = 2000;
    private static final int IMMEDIATE_THRESHOLD = 16;
    private final PerVillagerThrottle throttle = new PerVillagerThrottle(2000);

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
        int nbGoods = BringBackHomeGoal.countBringBackGoods(context);
        return 10 + nbGoods * 3;
    }

    @Override
    public boolean canStart(GoalContext context) {
        VillagerType vtype = ModCultures.getVillagerType(context.villager().getVillagerTypeId());
        if (vtype == null || vtype.bringBackHomeGoods().isEmpty()) {
            return false;
        }
        if (context.villager().getHomeBuilding() == null) {
            return false;
        }
        BuildingInstance home = context.village().getBuilding(context.villager().getHomeBuilding());
        if (home == null || home.getInventory() == null) {
            return false;
        }
        int nbGoods = BringBackHomeGoal.countBringBackGoods(context);
        if (nbGoods <= 0) {
            return false;
        }
        if (nbGoods > 16) {
            return true;
        }
        long now = context.level().getGameTime();
        return this.throttle.shouldEvaluate(context.villager().getUUID(), now);
    }

    @Override
    public VillagerTask start(GoalContext context) {
        return new BringBackHomeTask();
    }

    private static int countBringBackGoods(GoalContext context) {
        VillagerType vtype = ModCultures.getVillagerType(context.villager().getVillagerTypeId());
        if (vtype == null) {
            return 0;
        }
        VillagerInventory inv = context.villager().getInventory();
        int total = 0;
        for (Map.Entry<Item, Integer> entry : inv.getAll().entrySet()) {
            if (!BringBackHomeGoal.isBringBackGood(entry.getKey(), vtype)) continue;
            total += entry.getValue().intValue();
        }
        return total;
    }

    static boolean isBringBackGood(Item item, VillagerType vtype) {
        return vtype.resolvedBringBackHomeGoods().contains((Object)item);
    }

    static class BringBackHomeTask
    extends WalkThenActTask {
        BringBackHomeTask() {
        }

        @Override
        public ResourceLocation goalId() {
            return ID;
        }

        @Override
        @Nullable
        protected BlockPos resolveTarget(GoalContext ctx) {
            BuildingInstance home = this.resolveHomeBuilding(ctx);
            return home != null ? home.getPathStartPos() : null;
        }

        @Override
        protected void performAction(GoalContext ctx) {
            BuildingInstance home = this.resolveHomeBuilding(ctx);
            if (home == null || home.getInventory() == null) {
                LOGGER.debug("[Millenaire] BringBackHome \u2014 no inventory in building, abandoning");
                return;
            }
            VillagerType vtype = ModCultures.getVillagerType(ctx.villager().getVillagerTypeId());
            if (vtype == null) {
                return;
            }
            VillagerInventory villagerInv = ctx.villager().getInventory();
            BuildingInventory buildingInv = home.getInventory();
            int transferred = 0;
            for (Map.Entry<Item, Integer> entry : new ArrayList<Map.Entry<Item, Integer>>(villagerInv.getAll().entrySet())) {
                if (!BringBackHomeGoal.isBringBackGood(entry.getKey(), vtype)) continue;
                int count = entry.getValue();
                int added = buildingInv.add((Level)ctx.level(), entry.getKey(), count);
                if (added <= 0) continue;
                villagerInv.remove(entry.getKey(), added);
                transferred += added;
            }
            if (transferred > 0) {
                this.reportProgress();
                LOGGER.debug("[Millenaire] {} delivered {} items to home", (Object)ctx.villager().getVillagerTypeId(), (Object)transferred);
            }
        }

        @Nullable
        private BuildingInstance resolveHomeBuilding(GoalContext ctx) {
            BuildingId homeId = ctx.villager().getHomeBuilding();
            if (homeId == null) {
                return null;
            }
            return ctx.village().getBuilding(homeId);
        }

        @Override
        @Nullable
        public Component getGoalLabel() {
            return this.phase == WalkThenActTask.Phase.DONE ? null : TaskLabels.labelForPhase(this.phase != WalkThenActTask.Phase.WALKING, "bring_back_home");
        }
    }
}

