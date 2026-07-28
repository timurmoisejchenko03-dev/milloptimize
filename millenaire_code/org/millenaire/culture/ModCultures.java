/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.resources.ResourceLocation
 */
package org.millenaire.culture;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import org.millenaire.building.BuildingPlan;
import org.millenaire.building.BuildingPlanSet;
import org.millenaire.culture.Culture;
import org.millenaire.culture.NameLists;
import org.millenaire.culture.ReputationLabel;
import org.millenaire.culture.VillageType;
import org.millenaire.culture.VillagerType;
import org.millenaire.culture.WallType;

public final class ModCultures {
    private static final Map<ResourceLocation, Culture> cultures = new ConcurrentHashMap<ResourceLocation, Culture>();
    private static final Map<ResourceLocation, BuildingPlan> buildingPlans = new ConcurrentHashMap<ResourceLocation, BuildingPlan>();
    private static final Map<ResourceLocation, BuildingPlanSet> buildingPlanSets = new ConcurrentHashMap<ResourceLocation, BuildingPlanSet>();
    private static final Map<ResourceLocation, VillagerType> villagerTypes = new ConcurrentHashMap<ResourceLocation, VillagerType>();
    private static final Map<ResourceLocation, VillageType> villageTypes = new ConcurrentHashMap<ResourceLocation, VillageType>();
    private static final Map<ResourceLocation, List<ReputationLabel>> reputationLabels = new ConcurrentHashMap<ResourceLocation, List<ReputationLabel>>();
    private static final Map<ResourceLocation, List<ReputationLabel>> cultureReputationLabels = new ConcurrentHashMap<ResourceLocation, List<ReputationLabel>>();
    private static final Map<ResourceLocation, NameLists> nameLists = new ConcurrentHashMap<ResourceLocation, NameLists>();
    private static final Map<ResourceLocation, WallType> wallTypes = new ConcurrentHashMap<ResourceLocation, WallType>();

    private ModCultures() {
    }

    @Nullable
    public static Culture getCulture(ResourceLocation id) {
        return cultures.get(id);
    }

    @Nullable
    public static BuildingPlan getBuildingPlan(ResourceLocation id) {
        if (id == null) {
            return null;
        }
        return buildingPlans.get(id);
    }

    @Nullable
    public static BuildingPlanSet getBuildingPlanSet(ResourceLocation id) {
        if (id == null) {
            return null;
        }
        return buildingPlanSets.get(id);
    }

    public static Map<ResourceLocation, BuildingPlanSet> getAllBuildingPlanSets() {
        return Collections.unmodifiableMap(buildingPlanSets);
    }

    @Nullable
    public static VillagerType getVillagerType(ResourceLocation id) {
        return villagerTypes.get(id);
    }

    @Nullable
    public static VillageType getVillageType(ResourceLocation id) {
        return villageTypes.get(id);
    }

    public static Map<ResourceLocation, Culture> getAllCultures() {
        return Collections.unmodifiableMap(cultures);
    }

    public static Map<ResourceLocation, BuildingPlan> getAllBuildingPlans() {
        return Collections.unmodifiableMap(buildingPlans);
    }

    public static Map<ResourceLocation, VillagerType> getAllVillagerTypes() {
        return Collections.unmodifiableMap(villagerTypes);
    }

    public static Map<ResourceLocation, VillageType> getAllVillageTypes() {
        return Collections.unmodifiableMap(villageTypes);
    }

    @Nullable
    public static WallType getWallType(ResourceLocation id) {
        if (id == null) {
            return null;
        }
        return wallTypes.get(id);
    }

    public static Map<ResourceLocation, WallType> getAllWallTypes() {
        return Collections.unmodifiableMap(wallTypes);
    }

    public static void registerWallType(WallType wallType) {
        wallTypes.put(wallType.id(), wallType);
    }

    @Nullable
    public static List<ReputationLabel> getReputationLabels(ResourceLocation cultureId) {
        return reputationLabels.get(cultureId);
    }

    @Nullable
    public static List<ReputationLabel> getCultureReputationLabels(ResourceLocation cultureId) {
        return cultureReputationLabels.get(cultureId);
    }

    @Nullable
    public static NameLists getNameLists(ResourceLocation cultureId) {
        return nameLists.get(cultureId);
    }

    public static ResourceLocation extractCultureId(ResourceLocation villagerTypeId) {
        String path = villagerTypeId.getPath();
        int slashIdx = path.indexOf(47);
        String culturePath = slashIdx > 0 ? path.substring(0, slashIdx) : path;
        return ResourceLocation.fromNamespaceAndPath((String)villagerTypeId.getNamespace(), (String)culturePath);
    }

    public static void registerCulture(Culture culture) {
        cultures.put(culture.id(), culture);
    }

    public static void registerBuildingPlan(BuildingPlan plan) {
        buildingPlans.put(plan.id(), plan);
    }

    public static void registerBuildingPlanSet(BuildingPlanSet set) {
        buildingPlanSets.put(set.id(), set);
    }

    public static void registerVillagerType(VillagerType type) {
        villagerTypes.put(type.id(), type);
    }

    public static void registerVillageType(VillageType type) {
        villageTypes.put(type.id(), type);
    }

    public static void registerReputationLabels(ResourceLocation cultureId, List<ReputationLabel> labels) {
        reputationLabels.put(cultureId, labels);
    }

    public static void registerCultureReputationLabels(ResourceLocation cultureId, List<ReputationLabel> labels) {
        cultureReputationLabels.put(cultureId, labels);
    }

    public static void registerNameLists(ResourceLocation cultureId, NameLists lists) {
        nameLists.put(cultureId, lists);
    }

    public static void unregisterBuildingPlan(ResourceLocation planId) {
        if (planId == null) {
            return;
        }
        buildingPlans.remove(planId);
    }

    public static void unregisterVillageType(ResourceLocation villageTypeId) {
        if (villageTypeId == null) {
            return;
        }
        villageTypes.remove(villageTypeId);
    }

    public static void unregisterCultureContent(ResourceLocation cultureId) {
        if (cultureId == null) {
            return;
        }
        buildingPlans.values().removeIf(p -> cultureId.equals((Object)p.culture()));
        buildingPlanSets.values().removeIf(s -> cultureId.equals((Object)s.culture()));
        villagerTypes.values().removeIf(v -> cultureId.equals((Object)v.culture()));
        villageTypes.values().removeIf(v -> cultureId.equals((Object)v.culture()));
    }

    public static void unregisterCulture(ResourceLocation cultureId) {
        if (cultureId == null) {
            return;
        }
        ModCultures.unregisterCultureContent(cultureId);
        cultures.remove(cultureId);
        reputationLabels.remove(cultureId);
        cultureReputationLabels.remove(cultureId);
        nameLists.remove(cultureId);
    }

    public static void clear() {
        cultures.clear();
        buildingPlans.clear();
        buildingPlanSets.clear();
        villagerTypes.clear();
        villageTypes.clear();
        wallTypes.clear();
        reputationLabels.clear();
        cultureReputationLabels.clear();
        nameLists.clear();
    }
}

