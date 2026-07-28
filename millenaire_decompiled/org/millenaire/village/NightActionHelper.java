/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.registries.BuiltInRegistries
 *  net.minecraft.network.chat.Component
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.Items
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.phys.AABB
 *  org.slf4j.Logger
 */
package org.millenaire.village;

import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.millenaire.building.BuildingId;
import org.millenaire.building.BuildingInstance;
import org.millenaire.building.BuildingInventory;
import org.millenaire.building.BuildingPlanSet;
import org.millenaire.config.MillenaireServerConfig;
import org.millenaire.config.VillagerConfig;
import org.millenaire.culture.Gender;
import org.millenaire.culture.ModCultures;
import org.millenaire.culture.VillageType;
import org.millenaire.culture.VillagerType;
import org.millenaire.entity.MillVillager;
import org.millenaire.entity.VillagerSpawnFactory;
import org.millenaire.goal.GoalContext;
import org.millenaire.village.Village;
import org.millenaire.village.VillageEventType;
import org.millenaire.village.VillageId;
import org.millenaire.village.VillageManager;
import org.millenaire.village.VillageSavedData;
import org.millenaire.village.VillagerRecord;
import org.slf4j.Logger;

public final class NightActionHelper {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int CONCEPTION_CHANCE = 2;
    private static final int MAX_CHILDREN_PER_HOME = 2;
    private static final int GROWTH_PER_NIGHT = 2;
    private static final int VISITOR_NB_NIGHTS_BEFORE_LEAVING = 5;
    private static final int MAX_GROWTH_PER_NIGHT = 10;

    private NightActionHelper() {
    }

    public static boolean perform(GoalContext ctx) {
        VillageType villageType = ModCultures.getVillageType(ctx.village().getVillageTypeId());
        if (villageType != null && villageType.loneBuilding()) {
            return true;
        }
        MillVillager villager = ctx.villager();
        VillagerType vType = ModCultures.getVillagerType(villager.getVillagerTypeId());
        if (vType == null) {
            return true;
        }
        if (vType.hasTag("visitor")) {
            NightActionHelper.performVisitorNightAction(ctx, villager, vType);
            return true;
        }
        if (vType.isChild()) {
            NightActionHelper.handleChildGrowth(ctx, villager, vType);
            return true;
        }
        if (vType.maleChild() != null && vType.femaleChild() != null) {
            return NightActionHelper.attemptConception(ctx, villager, vType);
        }
        return true;
    }

    private static void performVisitorNightAction(GoalContext ctx, MillVillager villager, VillagerType vType) {
        int nights = villager.getVisitorNbNights() + 1;
        villager.setVisitorNbNights(nights);
        if (nights > 5) {
            NightActionHelper.leaveVillage(ctx, villager, vType);
            return;
        }
        if (vType.hasTag("foreignmerchant") && !vType.foreignMerchantStock().isEmpty()) {
            BuildingInstance home;
            Village village = ctx.village();
            BuildingInstance buildingInstance = home = villager.getHomeBuilding() != null ? village.getBuilding(villager.getHomeBuilding()) : null;
            if (home != null && home.getInventory() != null) {
                boolean hasItems = false;
                for (Map.Entry<ResourceLocation, Integer> entry : vType.foreignMerchantStock().entrySet()) {
                    Item item = BuiltInRegistries.ITEM.getOptional(entry.getKey()).orElse(null);
                    if (item == null || home.getInventory().getCount((Level)ctx.level(), item) <= 0) continue;
                    hasItems = true;
                    break;
                }
                if (!hasItems) {
                    LOGGER.debug("[Millenaire] Foreign merchant {} leaving \u2014 stock depleted", (Object)villager.getVillagerTypeId());
                    NightActionHelper.leaveVillage(ctx, villager, vType);
                }
            }
        }
    }

    private static void leaveVillage(GoalContext ctx, MillVillager villager, VillagerType vType) {
        Village village = ctx.village();
        if (vType.hasTag("foreignmerchant") && !vType.foreignMerchantStock().isEmpty()) {
            BuildingInstance home;
            BuildingInstance buildingInstance = home = villager.getHomeBuilding() != null ? village.getBuilding(villager.getHomeBuilding()) : null;
            if (home != null && home.getInventory() != null) {
                for (Map.Entry<ResourceLocation, Integer> entry : vType.foreignMerchantStock().entrySet()) {
                    Item item = BuiltInRegistries.ITEM.getOptional(entry.getKey()).orElse(null);
                    if (item == null) continue;
                    home.getInventory().remove((Level)ctx.level(), item, entry.getValue());
                }
            }
        }
        village.removeVillagerRecord(villager.getUUID());
        village.markDirty();
        villager.discard();
        LOGGER.debug("[Millenaire] Visitor {} left village {} after {} nights", new Object[]{villager.getVillagerTypeId(), village.getVillageName(), villager.getVisitorNbNights()});
    }

    private static void handleChildGrowth(GoalContext ctx, MillVillager villager, VillagerType vType) {
        int currentSize = villager.getChildSize();
        if (currentSize < 0) {
            currentSize = 0;
        }
        if (currentSize >= 20) {
            NightActionHelper.attemptTeenagerMigration(ctx, villager, vType);
            return;
        }
        int growth = 2;
        BuildingInstance home = NightActionHelper.resolveHome(ctx.village(), villager);
        if (home != null && home.getInventory() != null) {
            BuildingInventory inv = home.getInventory();
            if (inv.getCount((Level)ctx.level(), Items.EGG) > 0) {
                inv.remove((Level)ctx.level(), Items.EGG, 1);
                growth += 1 + ThreadLocalRandom.current().nextInt(5);
            }
            VillagerConfig config = VillagerConfig.get(vType.villagerConfigKey());
            for (Map.Entry<Item, Integer> food : config.getFoodGrowth().entrySet()) {
                if (growth >= 10 || currentSize + growth >= 20) break;
                if (inv.getCount((Level)ctx.level(), food.getKey()) <= 0) continue;
                inv.remove((Level)ctx.level(), food.getKey(), 1);
                int value = food.getValue();
                growth += value + ThreadLocalRandom.current().nextInt(value);
            }
        }
        growth = Math.min(growth, 10);
        int newSize = Math.min(currentSize + growth, 20);
        villager.setChildSize(newSize);
        LOGGER.debug("[Millenaire] Child {} grows: {} \u2192 {} (+{}, scale={})", new Object[]{villager.getVillagerDisplayName(), currentSize, newSize, growth, Float.valueOf(villager.getVillagerScale())});
    }

    private static boolean attemptConception(GoalContext ctx, MillVillager mother, VillagerType motherType) {
        BlockPos pos;
        AABB searchBox;
        List nearby;
        Village village = ctx.village();
        ServerLevel level = ctx.level();
        BuildingId homeId = mother.getHomeBuilding();
        if (homeId == null) {
            return false;
        }
        int childrenInHome = village.countChildrenInBuilding(homeId);
        if (childrenInHome >= 2) {
            return true;
        }
        int totalChildren = village.countChildren();
        if (totalChildren >= MillenaireServerConfig.SERVER.maxChildren.getAsInt()) {
            return true;
        }
        if (totalChildren > 5) {
            boolean anyMaleSlot = village.hasAnyFreeSlot(Gender.MALE);
            boolean anyFemaleSlot = village.hasAnyFreeSlot(Gender.FEMALE);
            if (!anyMaleSlot && !anyFemaleSlot) {
                return true;
            }
        }
        if ((nearby = level.getEntitiesOfClass(MillVillager.class, searchBox = new AABB(pos = mother.blockPosition()).inflate(4.0), mv -> {
            if (mv == mother) {
                return false;
            }
            if (!mv.isAlive()) {
                return false;
            }
            VillagerType vt = ModCultures.getVillagerType(mv.getVillagerTypeId());
            return vt != null && !vt.isChild() && vt.gender() == Gender.MALE;
        })).isEmpty()) {
            return false;
        }
        MillVillager father = (MillVillager)((Object)nearby.get(0));
        int conceptionChance = 2;
        BuildingInstance home = NightActionHelper.resolveHome(village, mother);
        if (home != null && home.getInventory() != null) {
            BuildingInventory inv = home.getInventory();
            VillagerConfig config = VillagerConfig.get(motherType.villagerConfigKey());
            for (Map.Entry<Item, Integer> food : config.getFoodConception().entrySet()) {
                if (inv.getCount((Level)level, food.getKey()) <= 0) continue;
                inv.remove((Level)level, food.getKey(), 1);
                conceptionChance += food.getValue().intValue();
                break;
            }
        }
        if (ThreadLocalRandom.current().nextInt(10) >= conceptionChance) {
            return true;
        }
        NightActionHelper.spawnChild(ctx, mother, motherType, father);
        return true;
    }

    private static void spawnChild(GoalContext ctx, MillVillager mother, VillagerType motherType, MillVillager presentMale) {
        MillVillager child = NightActionHelper.spawnChildCore(ctx.level(), ctx.village(), mother, motherType, false);
        if (child == null) {
            return;
        }
        Object fatherName = mother.getSpousesName();
        if (fatherName == null || ((String)fatherName).isBlank()) {
            fatherName = presentMale.getFirstName() + " " + presentMale.getFamilyName();
        }
        child.setMothersName(mother.getFirstName() + " " + mother.getFamilyName());
        child.setFathersName((String)fatherName);
        VillagerType childType = ModCultures.getVillagerType(child.getVillagerTypeId());
        Gender childGender = childType != null ? childType.gender() : Gender.MALE;
        LOGGER.info("[Millenaire] Birth! {} {} ({}), parents: {} & {}", new Object[]{child.getFirstName(), child.getFamilyName(), childGender, mother.getVillagerDisplayName(), fatherName});
        ctx.village().recordEvent(ctx.level(), Component.translatable((String)"event.millenaire.birth", (Object[])new Object[]{child.getFirstName() + " " + child.getFamilyName(), childGender.toString()}).getString());
        ctx.village().recordChronicleEvent(ctx.level(), VillageEventType.BIRTH, child.getFirstName() + " " + child.getFamilyName(), mother.getVillagerDisplayName() + " & " + (String)fatherName);
    }

    public static void forceSpawnChild(ServerLevel level, Village village, MillVillager parent, VillagerType parentType) {
        MillVillager child = NightActionHelper.spawnChildCore(level, village, parent, parentType, true);
        if (child == null) {
            return;
        }
        VillagerType childType = ModCultures.getVillagerType(child.getVillagerTypeId());
        Gender childGender = childType != null ? childType.gender() : Gender.MALE;
        LOGGER.info("[Millenaire] [Wand] Forced child: {} {} ({})", new Object[]{child.getFirstName(), child.getFamilyName(), childGender});
        village.recordEvent(level, Component.translatable((String)"event.millenaire.birth_forced", (Object[])new Object[]{child.getFirstName() + " " + child.getFamilyName(), childGender.toString()}).getString());
    }

    @Nullable
    private static MillVillager spawnChildCore(ServerLevel level, Village village, MillVillager parent, VillagerType parentType, boolean forced) {
        String childTypeSuffix;
        Gender childGender = NightActionHelper.chooseChildGender(village, parent.getHomeBuilding());
        String string = childTypeSuffix = childGender == Gender.MALE ? parentType.maleChild() : parentType.femaleChild();
        if (childTypeSuffix == null) {
            if (forced) {
                LOGGER.warn("[Millenaire] forceSpawnChild: no child type for {}", (Object)parentType.id());
            }
            return null;
        }
        String culturePath = village.getCultureId().getPath();
        ResourceLocation childTypeId = ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)(culturePath + "/" + childTypeSuffix));
        VillagerType childType = ModCultures.getVillagerType(childTypeId);
        if (childType == null) {
            LOGGER.warn("[Millenaire] Child type not found: {}", (Object)childTypeId);
            return null;
        }
        BlockPos spawnPos = parent.blockPosition();
        MillVillager child = VillagerSpawnFactory.spawnInVillage(level, village, childTypeId, spawnPos, parent.getHomeBuilding(), false);
        if (child == null) {
            return null;
        }
        child.setFamilyName(parent.getFamilyName());
        VillagerRecord record = village.getVillagerRecord(child.getUUID());
        if (record != null) {
            record.updateFromEntity(child);
        }
        return child;
    }

    private static BuildingInstance resolveHome(Village village, MillVillager villager) {
        BuildingId homeId = villager.getHomeBuilding();
        if (homeId == null) {
            return null;
        }
        return village.getBuilding(homeId);
    }

    private static Gender chooseChildGender(Village village, BuildingId homeId) {
        int nbMales = village.countAdultsInBuilding(homeId, Gender.MALE);
        int nbFemales = village.countAdultsInBuilding(homeId, Gender.FEMALE);
        int maleChance = 3 + nbFemales - nbMales;
        maleChance = Math.max(0, Math.min(6, maleChance));
        return ThreadLocalRandom.current().nextInt(6) < maleChance ? Gender.MALE : Gender.FEMALE;
    }

    private static void attemptTeenagerMigration(GoalContext ctx, MillVillager villager, VillagerType vType) {
        Village village = ctx.village();
        Gender gender = vType.gender();
        String familyName = villager.getFamilyName();
        VillageManager vm = VillageSavedData.get(ctx.level()).getVillageManager();
        for (Map.Entry<VillageId, Integer> rel : village.getRelations().entrySet()) {
            Village other;
            if (rel.getValue() <= 90 || (other = vm.getVillage(rel.getKey())) == null || other.getId().equals(village.getId()) || !other.getCultureId().equals((Object)village.getCultureId())) continue;
            BuildingId suitableHouse = null;
            BuildingId availableInn = null;
            for (BuildingInstance b : other.getBuildings()) {
                int residentCount;
                BuildingPlanSet planSet;
                if (!b.isOperational() || b.getPlanSetId() == null || (planSet = ModCultures.getBuildingPlanSet(b.getPlanSetId())) == null) continue;
                if (suitableHouse == null && planSet.isResidential()) {
                    if (other.hasFreeSlot(b.getId(), gender) && !other.hasRelativeOfOppositeGenderInRecords(b.getId(), gender, familyName)) {
                        suitableHouse = b.getId();
                    }
                } else if (availableInn == null && planSet.hasTag("inn") && (residentCount = other.countResidentsInBuilding(b.getId())) < 2) {
                    availableInn = b.getId();
                }
                if (suitableHouse == null || availableInn == null) continue;
                break;
            }
            if (suitableHouse == null || availableInn == null) continue;
            village.transferVillagerPermanently(ctx.level(), villager.getUUID(), other, availableInn);
            BuildingInstance destInn = other.getBuilding(availableInn);
            if (destInn != null) {
                String originName = village.getVillageName() != null ? village.getVillageName() : village.getVillageTypeId().getPath();
                destInn.addVisitorLog("panels.childarrived;" + villager.getFirstName() + " " + villager.getFamilyName() + ";" + originName);
            }
            String otherName = other.getVillageName() != null ? other.getVillageName() : other.getVillageTypeId().getPath();
            String thisName = village.getVillageName() != null ? village.getVillageName() : village.getVillageTypeId().getPath();
            village.recordChronicleEvent(ctx.level(), VillageEventType.MIGRATION, villager.getFirstName() + " " + villager.getFamilyName(), otherName);
            other.recordChronicleEvent(ctx.level(), VillageEventType.MIGRATION, villager.getFirstName() + " " + villager.getFamilyName(), thisName);
            LOGGER.info("[Millenaire] Teenager {} migrated from {} to {}", new Object[]{villager.getVillagerDisplayName(), village.getVillageName(), other.getVillageName()});
            return;
        }
    }
}

