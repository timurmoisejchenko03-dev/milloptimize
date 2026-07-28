/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  net.minecraft.core.BlockPos
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.damagesource.DamageSource
 *  net.minecraft.world.damagesource.DamageTypes
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.level.Level
 *  net.neoforged.neoforge.event.entity.living.LivingDamageEvent$Pre
 *  org.slf4j.Logger
 */
package org.millenaire.entity;

import com.mojang.logging.LogUtils;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import org.millenaire.building.BuildingId;
import org.millenaire.building.BuildingInstance;
import org.millenaire.building.ConstructionTask;
import org.millenaire.entity.MillVillager;
import org.millenaire.goal.GoalScheduler;
import org.millenaire.goal.NavigationHelperUtils;
import org.millenaire.goal.VillagerTask;
import org.millenaire.goal.impl.BuildGoal;
import org.millenaire.village.Village;
import org.slf4j.Logger;

public final class BuilderSafetyHandler {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int COOLDOWN_TICKS = 200;
    private static final int ON_SITE_MARGIN = 4;
    private static final int UPCOMING_STEPS_EXCLUSION = 10;
    private static final Map<UUID, Long> LAST_RESCUE_TICK = new ConcurrentHashMap<UUID, Long>();

    private static boolean isStuckRelatedDamage(DamageSource source) {
        return source.is(DamageTypes.IN_WALL) || source.is(DamageTypes.FALL) || source.is(DamageTypes.FALLING_BLOCK) || source.is(DamageTypes.LAVA) || source.is(DamageTypes.IN_FIRE) || source.is(DamageTypes.ON_FIRE) || source.is(DamageTypes.HOT_FLOOR) || source.is(DamageTypes.CRAMMING) || source.is(DamageTypes.DROWN);
    }

    private BuilderSafetyHandler() {
    }

    private static void pruneStaleEntries(long now) {
        LAST_RESCUE_TICK.entrySet().removeIf(e -> now - (Long)e.getValue() >= 200L);
    }

    private static boolean isInsideSiteFootprint(BlockPos pos, BuildingInstance site) {
        int w = site.getEffectiveWidth();
        int d = site.getEffectiveDepth();
        if (w <= 0 || d <= 0) {
            return true;
        }
        BlockPos origin = site.getOrigin();
        int minX = origin.getX() + site.getCachedMinX() - 4;
        int maxX = origin.getX() + site.getCachedMaxX() + 4;
        int minZ = origin.getZ() + site.getCachedMinZ() - 4;
        int maxZ = origin.getZ() + site.getCachedMaxZ() + 4;
        return pos.getX() >= minX && pos.getX() <= maxX && pos.getZ() >= minZ && pos.getZ() <= maxZ;
    }

    public static void onLivingDamage(LivingDamageEvent.Pre event) {
        LivingEntity livingEntity = event.getEntity();
        if (!(livingEntity instanceof MillVillager)) {
            return;
        }
        MillVillager villager = (MillVillager)livingEntity;
        Level level = villager.level();
        if (!(level instanceof ServerLevel)) {
            return;
        }
        ServerLevel level2 = (ServerLevel)level;
        if (!BuilderSafetyHandler.isStuckRelatedDamage(event.getSource())) {
            return;
        }
        BuildingId siteId = villager.getConstructionBuildingId();
        if (siteId == null) {
            return;
        }
        if (villager.getVillageId() == null) {
            return;
        }
        GoalScheduler scheduler = villager.getGoalScheduler();
        if (scheduler == null) {
            return;
        }
        VillagerTask currentTask = scheduler.getCurrentTask();
        if (currentTask == null || !BuildGoal.ID.equals((Object)currentTask.goalId())) {
            return;
        }
        long now = level2.getGameTime();
        Long last = LAST_RESCUE_TICK.get(villager.getUUID());
        if (last != null && now - last < 200L) {
            return;
        }
        Village village = Village.resolve(level2, villager.getVillageId());
        if (village == null) {
            return;
        }
        BuildingInstance site = village.getBuilding(siteId);
        if (site == null) {
            return;
        }
        BlockPos before = villager.blockPosition();
        if (!BuilderSafetyHandler.isInsideSiteFootprint(before, site)) {
            return;
        }
        BlockPos rescue = site.getFirstPointPos("pathStartPos");
        if (rescue == null) {
            rescue = site.getOrigin();
        }
        NavigationHelperUtils.teleportToSafeNearTarget(villager, rescue, BuilderSafetyHandler.buildExcludedTilesPredicate(site));
        villager.fallDistance = 0.0f;
        villager.clearFire();
        BuilderSafetyHandler.pruneStaleEntries(now);
        LAST_RESCUE_TICK.put(villager.getUUID(), now);
        event.setNewDamage(0.0f);
        LOGGER.info("[Millenaire] Builder {} rescued from damage at {} -> TP target {} (site {})", new Object[]{villager.getVillagerDisplayName(), before.toShortString(), rescue.toShortString(), siteId});
    }

    public static Predicate<BlockPos> buildExcludedTilesPredicate(BuildingInstance site) {
        ConstructionTask task = site.getConstructionTask();
        if (task == null) {
            return p -> false;
        }
        List<BlockPos> upcoming = task.upcomingAbsolutePositions(site.getOrigin(), 10);
        if (upcoming.isEmpty()) {
            return p -> false;
        }
        HashSet<BlockPos> excluded = new HashSet<BlockPos>(upcoming);
        return excluded::contains;
    }
}

