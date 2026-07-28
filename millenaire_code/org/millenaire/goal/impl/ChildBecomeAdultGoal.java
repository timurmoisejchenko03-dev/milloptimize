/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 *  net.minecraft.network.chat.Component
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.ai.attributes.Attributes
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.level.levelgen.Heightmap$Types
 *  org.slf4j.Logger
 */
package org.millenaire.goal.impl;

import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.levelgen.Heightmap;
import org.millenaire.Millenaire;
import org.millenaire.building.BuildingId;
import org.millenaire.building.BuildingInstance;
import org.millenaire.building.BuildingPlanSet;
import org.millenaire.culture.Gender;
import org.millenaire.culture.ModCultures;
import org.millenaire.culture.VillagerType;
import org.millenaire.entity.MillVillager;
import org.millenaire.entity.VillagerAppearanceFactory;
import org.millenaire.entity.VillagerNavDriver;
import org.millenaire.goal.GoalContext;
import org.millenaire.goal.GoalRegistry;
import org.millenaire.goal.StopReason;
import org.millenaire.goal.TaskLabels;
import org.millenaire.goal.TravelPhase;
import org.millenaire.goal.VillagerGoal;
import org.millenaire.goal.VillagerTask;
import org.millenaire.item.ItemHelper;
import org.millenaire.village.Village;
import org.millenaire.village.VillageEventType;
import org.slf4j.Logger;

public class ChildBecomeAdultGoal
implements VillagerGoal {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)"child_become_adult");

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
        return 100;
    }

    @Override
    public boolean canStart(GoalContext context) {
        MillVillager villager = context.villager();
        VillagerType vType = ModCultures.getVillagerType(villager.getVillagerTypeId());
        if (vType == null || !vType.isChild()) {
            return false;
        }
        if (villager.getChildSize() < 20) {
            return false;
        }
        return this.findTargetBuilding(context, vType.gender()) != null;
    }

    @Override
    public VillagerTask start(GoalContext context) {
        MillVillager villager = context.villager();
        VillagerType vType = ModCultures.getVillagerType(villager.getVillagerTypeId());
        Gender gender = vType != null ? vType.gender() : Gender.MALE;
        TargetInfo target = this.findTargetBuilding(context, gender);
        if (target == null) {
            return new FailedTask();
        }
        String adultType = context.village().reserveSlot(target.buildingId, gender, villager.getUUID());
        if (adultType == null) {
            return new FailedTask();
        }
        LOGGER.debug("[Millenaire] Teenager {} reserves a slot {} in {}", new Object[]{villager.getVillagerDisplayName(), adultType, target.buildingId});
        return new BecomeAdultTask(target.buildingId, target.planSetId, gender, adultType);
    }

    @Nullable
    private TargetInfo findTargetBuilding(GoalContext ctx, Gender gender) {
        Village village = ctx.village();
        MillVillager villager = ctx.villager();
        String familyName = villager.getFamilyName();
        record Candidate(BuildingId buildingId, ResourceLocation planSetId, int priority, boolean completesCouple) {
        }
        ArrayList<Candidate> candidates = new ArrayList<Candidate>();
        for (BuildingInstance b : village.getBuildings()) {
            BuildingPlanSet planSet;
            if (!b.isOperational() || b.getPlanSetId() == null || (planSet = ModCultures.getBuildingPlanSet(b.getPlanSetId())) == null || !village.hasFreeSlot(b.getId(), gender) || this.hasRelativeOfOppositeGender(village, ctx.level(), b.getId(), gender, familyName)) continue;
            Gender oppositeGender = gender == Gender.MALE ? Gender.FEMALE : Gender.MALE;
            boolean completesCouple = this.buildingHasAdultOfGender(ctx, b.getId(), oppositeGender);
            candidates.add(new Candidate(b.getId(), b.getPlanSetId(), planSet.priorityMoveIn(), completesCouple));
        }
        if (candidates.isEmpty()) {
            return null;
        }
        List<Candidate> preferred = candidates.stream().filter(Candidate::completesCouple).toList();
        List<Object> pool = preferred.isEmpty() ? candidates : preferred;
        int maxPriority = pool.stream().mapToInt(Candidate::priority).max().getAsInt();
        ArrayList<Candidate> best = new ArrayList<Candidate>();
        for (Candidate candidate : pool) {
            if (candidate.priority() != maxPriority) continue;
            best.add(candidate);
        }
        Candidate chosen = (Candidate)best.get(ctx.villager().getRandom().nextInt(best.size()));
        return new TargetInfo(chosen.buildingId(), chosen.planSetId());
    }

    private boolean hasRelativeOfOppositeGender(Village village, ServerLevel level, BuildingId buildingId, Gender teenagerGender, String familyName) {
        if (familyName == null || familyName.isEmpty()) {
            return false;
        }
        Gender oppositeGender = teenagerGender == Gender.MALE ? Gender.FEMALE : Gender.MALE;
        for (Map.Entry<UUID, ResourceLocation> entry : village.getVillagerTypes().entrySet()) {
            MillVillager mv;
            Entity entity;
            VillagerType vType;
            BuildingId home = village.getVillagerHome(entry.getKey());
            if (home == null || !home.equals(buildingId) || (vType = ModCultures.getVillagerType(entry.getValue())) == null || vType.isChild() || vType.gender() != oppositeGender || !((entity = level.getEntity(entry.getKey())) instanceof MillVillager) || !(mv = (MillVillager)entity).isAlive() || !familyName.equals(mv.getFamilyName())) continue;
            return true;
        }
        return false;
    }

    private boolean buildingHasAdultOfGender(GoalContext ctx, BuildingId buildingId, Gender targetGender) {
        Village village = ctx.village();
        for (Map.Entry<UUID, ResourceLocation> entry : village.getVillagerTypes().entrySet()) {
            MillVillager mv;
            Entity entity;
            VillagerType vType;
            BuildingId home = village.getVillagerHome(entry.getKey());
            if (home == null || !home.equals(buildingId) || (vType = ModCultures.getVillagerType(entry.getValue())) == null || vType.isChild() || vType.gender() != targetGender || !((entity = ctx.level().getEntity(entry.getKey())) instanceof MillVillager) || !(mv = (MillVillager)entity).isAlive()) continue;
            return true;
        }
        return false;
    }

    private record TargetInfo(BuildingId buildingId, ResourceLocation planSetId) {
    }

    private static class FailedTask
    implements VillagerTask {
        private FailedTask() {
        }

        @Override
        public ResourceLocation goalId() {
            return ID;
        }

        @Override
        public void tick(GoalContext context) {
        }

        @Override
        public boolean isFinished() {
            return true;
        }

        @Override
        public void stop(GoalContext context, StopReason reason) {
        }
    }

    static class BecomeAdultTask
    implements VillagerTask {
        private static final double ARRIVE_DISTANCE = 3.0;
        private static final double WALK_SPEED = 0.5;
        private final BuildingId targetBuildingId;
        private final ResourceLocation planSetId;
        private final Gender gender;
        private final String adultTypeSuffix;
        private boolean arrived;
        private boolean finished;
        private int tickCount;

        BecomeAdultTask(BuildingId targetBuildingId, ResourceLocation planSetId, Gender gender, String adultTypeSuffix) {
            this.targetBuildingId = targetBuildingId;
            this.planSetId = planSetId;
            this.gender = gender;
            this.adultTypeSuffix = adultTypeSuffix;
        }

        @Override
        public ResourceLocation goalId() {
            return ID;
        }

        @Override
        public void tick(GoalContext ctx) {
            ++this.tickCount;
            if (this.arrived) {
                this.performTransformation(ctx);
                this.finished = true;
                return;
            }
            BuildingInstance target = ctx.village().getBuilding(this.targetBuildingId);
            if (target == null) {
                this.finished = true;
                return;
            }
            BlockPos targetPos = target.getOrigin();
            int surfaceY = ctx.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, targetPos.getX(), targetPos.getZ());
            BlockPos walkTarget = new BlockPos(targetPos.getX(), surfaceY, targetPos.getZ());
            VillagerNavDriver nav = ctx.villager().getNavManager();
            if (nav.getDestination() == null) {
                nav.navigateTo(ctx.villager(), walkTarget, 0.5);
            }
            if (nav.isArrived(ctx.villager(), 3.0)) {
                nav.stop(ctx.villager());
                this.arrived = true;
            } else if (nav.isAbandoned()) {
                nav.stop(ctx.villager());
                this.arrived = true;
            }
        }

        private void performTransformation(GoalContext ctx) {
            MillVillager villager = ctx.villager();
            Village village = ctx.village();
            String culturePath = village.getCultureId().getPath();
            ResourceLocation adultTypeId = ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)(culturePath + "/" + this.adultTypeSuffix));
            VillagerType adultType = ModCultures.getVillagerType(adultTypeId);
            if (adultType == null) {
                LOGGER.warn("[Millenaire] Adult type not found: {}", (Object)adultTypeId);
                return;
            }
            String firstName = villager.getFirstName();
            String familyName = villager.getFamilyName();
            String fathersName = villager.getFathersName();
            String mothersName = villager.getMothersName();
            this.handleMarriage(ctx, villager, adultType);
            villager.setVillagerTypeId(adultTypeId);
            villager.setChildSize(-1);
            villager.setHomeBuilding(this.targetBuildingId);
            GoalRegistry registry = Millenaire.getGoalRegistry();
            if (registry != null) {
                villager.initGoals(registry, adultType);
            }
            VillagerAppearanceFactory.randomizeAppearance(villager, adultType);
            villager.setFirstName(firstName);
            villager.setFathersName(fathersName);
            villager.setMothersName(mothersName);
            for (Map.Entry<ResourceLocation, Integer> entry : adultType.initialInventory().entrySet()) {
                Item item = ItemHelper.resolve(entry.getKey());
                if (item == null) continue;
                villager.getInventory().add(item, entry.getValue());
            }
            villager.getAttribute(Attributes.MAX_HEALTH).setBaseValue(20.0);
            villager.setHealth(villager.getMaxHealth());
            village.updateVillagerType(villager.getUUID(), adultTypeId);
            village.releaseAllSlots(villager.getUUID());
            LOGGER.info("[Millenaire] Teenager {} {} became adult: {} in {}", new Object[]{villager.getFirstName(), villager.getFamilyName(), adultTypeId.getPath(), this.targetBuildingId});
            village.recordEvent(ctx.level(), Component.translatable((String)"event.millenaire.teenager_became_adult", (Object[])new Object[]{villager.getFirstName() + " " + villager.getFamilyName(), adultTypeId.getPath()}).getString());
            village.recordChronicleEvent(ctx.level(), VillageEventType.CAME_OF_AGE, villager.getFirstName() + " " + villager.getFamilyName(), adultTypeId.getPath());
        }

        private void handleMarriage(GoalContext ctx, MillVillager teenager, VillagerType adultType) {
            Village village = ctx.village();
            Gender oppositeGender = this.gender == Gender.MALE ? Gender.FEMALE : Gender.MALE;
            MillVillager spouse = null;
            for (Map.Entry<UUID, ResourceLocation> entry : village.getVillagerTypes().entrySet()) {
                MillVillager mv;
                Entity entity;
                VillagerType vType;
                BuildingId home = village.getVillagerHome(entry.getKey());
                if (home == null || !home.equals(this.targetBuildingId) || (vType = ModCultures.getVillagerType(entry.getValue())) == null || vType.isChild() || vType.gender() != oppositeGender || !((entity = ctx.level().getEntity(entry.getKey())) instanceof MillVillager) || !(mv = (MillVillager)entity).isAlive()) continue;
                spouse = mv;
                break;
            }
            if (spouse == null) {
                return;
            }
            if (this.gender == Gender.FEMALE) {
                teenager.setMaidenName(teenager.getFamilyName());
                teenager.setFamilyName(spouse.getFamilyName());
                teenager.setSpousesName(spouse.getFirstName() + " " + spouse.getFamilyName());
                spouse.setSpousesName(teenager.getFirstName() + " " + teenager.getMaidenName());
            } else {
                spouse.setMaidenName(spouse.getFamilyName());
                spouse.setFamilyName(teenager.getFamilyName());
                spouse.setSpousesName(teenager.getFirstName() + " " + teenager.getFamilyName());
                teenager.setSpousesName(spouse.getFirstName() + " " + spouse.getMaidenName());
            }
            LOGGER.info("[Millenaire] Marriage: {} & {}", (Object)teenager.getVillagerDisplayName(), (Object)spouse.getVillagerDisplayName());
        }

        @Override
        public boolean isFinished() {
            return this.finished;
        }

        @Override
        public void stop(GoalContext ctx, StopReason reason) {
            if (ctx == null) {
                return;
            }
            ctx.villager().getNavManager().stop(ctx.villager());
            if (reason != StopReason.COMPLETED) {
                ctx.village().releaseAllSlots(ctx.villager().getUUID());
            }
        }

        @Override
        public TravelPhase getTravelPhase() {
            return TaskLabels.phaseFor(this.arrived);
        }

        @Override
        @Nullable
        public Component getGoalLabel() {
            return TaskLabels.labelForPhase(this.arrived, "become_adult");
        }
    }
}

