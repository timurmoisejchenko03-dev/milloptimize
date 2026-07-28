/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  net.minecraft.resources.ResourceLocation
 *  org.slf4j.Logger
 */
package org.millenaire.culture;

import com.mojang.logging.LogUtils;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import org.millenaire.building.BuildingPlanSet;
import org.millenaire.culture.ModCultures;
import org.millenaire.culture.VillageType;
import org.millenaire.culture.VillagerType;
import org.slf4j.Logger;

final class TravelBookCategoryAssigner {
    private static final Logger LOGGER = LogUtils.getLogger();

    private TravelBookCategoryAssigner() {
    }

    static void autoAssign(ResourceLocation cultureId) {
        String autoCategory;
        for (Map.Entry<ResourceLocation, BuildingPlanSet> entry : ModCultures.getAllBuildingPlanSets().entrySet()) {
            BuildingPlanSet set = entry.getValue();
            if (!set.culture().equals((Object)cultureId) || set.travelBookCategory() != null) continue;
            autoCategory = TravelBookCategoryAssigner.autoAssignBuildingCategory(set);
            ModCultures.registerBuildingPlanSet(new BuildingPlanSet(set.id(), set.culture(), set.buildingId(), set.category(), set.nativeName(), set.maxCount(), set.minDistance(), set.maxDistance(), set.maleResidents(), set.femaleResidents(), set.priorityMoveIn(), set.tags(), set.terrainPolicy(), set.constructionOrder(), set.variants(), set.startingSubBuildings(), set.icon(), set.clearMargins(), set.price(), set.reputation(), set.randomBrickColours(), set.startingGoods(), autoCategory, set.travelBookDisplay(), set.isSubBuilding(), set.isTownHall(), set.farFromTags(), set.closeToTags(), set.fixedOrientation(), set.isWallSegment(), set.isBorderBuilding(), set.extraWallConstructionSlots(), set.isGift(), set.visitors()));
        }
        for (Map.Entry<ResourceLocation, Record> entry : ModCultures.getAllVillagerTypes().entrySet()) {
            VillagerType vt = (VillagerType)entry.getValue();
            if (!vt.culture().equals((Object)cultureId) || vt.travelBookCategory() != null) continue;
            autoCategory = TravelBookCategoryAssigner.autoAssignVillagerCategory(vt, cultureId);
            ModCultures.registerVillagerType(new VillagerType(vt.id(), vt.culture(), vt.model(), vt.textures(), vt.clothes(), vt.baseScale(), vt.isChild(), vt.goals(), vt.tags(), vt.spawnWeight(), vt.initialInventory(), vt.gender(), vt.firstNameList(), vt.familyNameList(), vt.maleChild(), vt.femaleChild(), vt.bringBackHomeGoods(), vt.collectGoods(), vt.requiredGoods(), vt.icon(), vt.toolNeededClasses(), vt.itemsNeeded(), vt.maxHealth(), vt.villagerConfigKey(), autoCategory, vt.travelBookDisplay(), vt.nativeName(), vt.foreignMerchantStock(), vt.hiringCost(), vt.travelBookHeldItem(), vt.travelBookHeldItemOffHand(), vt.altNativeName(), vt.altKey(), vt.travelBookMainCultureVillager(), vt.defaultWeapon(), vt.baseAttackStrength(), vt.resolvedBringBackHomeGoods(), vt.resolvedCollectGoods(), vt.resolvedRequiredGoods()));
        }
    }

    private static String autoAssignBuildingCategory(BuildingPlanSet set) {
        if (set.price() > 0) {
            return "playerbuilding";
        }
        boolean foundInLone = false;
        boolean foundInNonLone = false;
        for (VillageType vt : ModCultures.getAllVillageTypes().values()) {
            if (!vt.culture().equals((Object)set.culture())) continue;
            for (VillageType.LayoutSlot slot : vt.layout()) {
                if (!slot.plan().equals((Object)set.id())) continue;
                if (vt.loneBuilding()) {
                    foundInLone = true;
                    break;
                }
                foundInNonLone = true;
                break;
            }
            if (!foundInNonLone) continue;
            break;
        }
        if (foundInLone && !foundInNonLone) {
            return "lonebuilding";
        }
        return switch (set.category()) {
            case "townhalls" -> "townhall";
            case "houses" -> "house";
            default -> "othervillage";
        };
    }

    private static String autoAssignVillagerCategory(VillagerType vt, ResourceLocation cultureId) {
        if (vt.hasTag("chief")) {
            return "leader";
        }
        if (vt.hasTag("visitor")) {
            return "visitor";
        }
        for (BuildingPlanSet set : ModCultures.getAllBuildingPlanSets().values()) {
            boolean residesHere;
            if (!set.culture().equals((Object)cultureId)) continue;
            String culturePrefix = set.culture().getPath() + "_";
            boolean bl = residesHere = set.maleResidents().stream().anyMatch(r -> vt.id().getPath().equals(culturePrefix + r)) || set.femaleResidents().stream().anyMatch(r -> vt.id().getPath().equals(culturePrefix + r));
            if (!residesHere) continue;
            for (VillageType villageType : ModCultures.getAllVillageTypes().values()) {
                if (!villageType.culture().equals((Object)cultureId) || !villageType.loneBuilding()) continue;
                for (VillageType.LayoutSlot slot : villageType.layout()) {
                    if (!slot.plan().equals((Object)set.id())) continue;
                    return "lonevillager";
                }
            }
        }
        return "villager";
    }
}

