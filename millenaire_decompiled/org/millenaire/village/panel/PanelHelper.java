/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.item.Item
 */
package org.millenaire.village.panel;

import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import org.millenaire.DisplayUtils;
import org.millenaire.building.BuildingInstance;
import org.millenaire.building.BuildingPlanSet;
import org.millenaire.commerce.TradeGood;
import org.millenaire.commerce.TradeGoodsLoader;
import org.millenaire.culture.ModCultures;
import org.millenaire.culture.VillagerType;
import org.millenaire.entity.MillVillager;
import org.millenaire.language.BuildingNameHelper;
import org.millenaire.village.Village;
import org.millenaire.village.panel.PanelLine;

public final class PanelHelper {
    private PanelHelper() {
    }

    static String t(String key) {
        return DisplayUtils.t(key);
    }

    static String t(String key, Object ... args) {
        return DisplayUtils.t(key, args);
    }

    static String resolveRoleName(ResourceLocation typeId) {
        return DisplayUtils.resolveRoleName(typeId);
    }

    static String resolveRoleKey(ResourceLocation typeId) {
        return DisplayUtils.resolveRoleKey(typeId);
    }

    static String resolveTradeGoodName(ResourceLocation cultureId, String tradeGoodKey) {
        Item item;
        TradeGood good = TradeGoodsLoader.getGoodById(cultureId, tradeGoodKey);
        if (good != null && (item = good.resolveItem()) != null) {
            return item.getDescription().getString();
        }
        return tradeGoodKey.replace('_', ' ');
    }

    static DirectionInfo computeDirectionInfo(BlockPos from, BlockPos to) {
        int dz;
        int dx = to.getX() - from.getX();
        double dist = Math.sqrt(dx * dx + (dz = to.getZ() - from.getZ()) * dz);
        if (dist < 5.0) {
            return new DirectionInfo(true, (int)dist, null);
        }
        double angle = Math.toDegrees(Math.atan2(-dx, dz));
        if (angle < 0.0) {
            angle += 360.0;
        }
        String dirKey = angle < 22.5 || angle >= 337.5 ? "panel.millenaire.dir.south" : (angle < 67.5 ? "panel.millenaire.dir.southwest" : (angle < 112.5 ? "panel.millenaire.dir.west" : (angle < 157.5 ? "panel.millenaire.dir.northwest" : (angle < 202.5 ? "panel.millenaire.dir.north" : (angle < 247.5 ? "panel.millenaire.dir.northeast" : (angle < 292.5 ? "panel.millenaire.dir.east" : "panel.millenaire.dir.southeast"))))));
        return new DirectionInfo(false, (int)dist, dirKey);
    }

    static String computeDirection(BlockPos from, BlockPos to) {
        int dz;
        int dx = to.getX() - from.getX();
        double dist = Math.sqrt(dx * dx + (dz = to.getZ() - from.getZ()) * dz);
        if (dist < 5.0) {
            return PanelHelper.t("panel.millenaire.at_center");
        }
        double angle = Math.toDegrees(Math.atan2(-dx, dz));
        if (angle < 0.0) {
            angle += 360.0;
        }
        String dirKey = angle < 22.5 || angle >= 337.5 ? "panel.millenaire.dir.south" : (angle < 67.5 ? "panel.millenaire.dir.southwest" : (angle < 112.5 ? "panel.millenaire.dir.west" : (angle < 157.5 ? "panel.millenaire.dir.northwest" : (angle < 202.5 ? "panel.millenaire.dir.north" : (angle < 247.5 ? "panel.millenaire.dir.northeast" : (angle < 292.5 ? "panel.millenaire.dir.east" : "panel.millenaire.dir.southeast"))))));
        return (int)dist + " " + PanelHelper.t("panel.millenaire.blocks") + " " + PanelHelper.t("panel.millenaire.to_the") + " " + PanelHelper.t(dirKey);
    }

    static String getBuildingDisplayName(BuildingInstance building) {
        BuildingPlanSet planSet;
        if (building.getPlanSetId() != null && (planSet = ModCultures.getBuildingPlanSet(building.getPlanSetId())) != null) {
            BuildingPlanSet.LevelDef levelDef = planSet.getLevel(building.getVariant(), building.getLevel());
            if (levelDef != null && levelDef.nativeName() != null) {
                return levelDef.nativeName();
            }
            return planSet.nativeName();
        }
        return building.getPlanId().getPath();
    }

    static String getPendingProjectName(Village.PendingProject pending) {
        BuildingPlanSet planSet = ModCultures.getBuildingPlanSet(pending.planSetId());
        if (planSet != null) {
            BuildingPlanSet.LevelDef levelDef = planSet.getLevel(pending.variant(), pending.level());
            if (levelDef != null && levelDef.nativeName() != null) {
                return levelDef.nativeName();
            }
            return planSet.nativeName();
        }
        return pending.planSetId().getPath();
    }

    static String getBuildingTranslationKey(BuildingInstance building) {
        return BuildingNameHelper.getTranslationKey(building);
    }

    static String getPendingProjectKey(Village.PendingProject pending) {
        String key = BuildingNameHelper.getPendingProjectTranslationKey(pending.planSetId());
        return key != null ? key : pending.planSetId().getPath();
    }

    static String resolveBuildingIcon(@Nullable BuildingInstance building) {
        if (building == null || building.getPlanSetId() == null) {
            return "";
        }
        BuildingPlanSet planSet = ModCultures.getBuildingPlanSet(building.getPlanSetId());
        return planSet != null && planSet.icon() != null ? planSet.icon() : "";
    }

    @Nullable
    static MillVillager findVillager(ServerLevel level, UUID uuid) {
        MillVillager mv;
        Entity entity = level.getEntity(uuid);
        return entity instanceof MillVillager ? (mv = (MillVillager)entity) : null;
    }

    static ResourceLocation findTypeId(Village village, MillVillager mv) {
        ResourceLocation typeId = village.getVillagerTypes().get(mv.getUUID());
        return typeId != null ? typeId : ResourceLocation.withDefaultNamespace((String)"unknown");
    }

    @Nullable
    static PanelLine.PanelNavTarget buildingNavTarget(BuildingInstance b) {
        ResourceLocation planSetId = b.getPlanSetId();
        if (planSetId == null) {
            return null;
        }
        String cultureKey = ModCultures.extractCultureId(planSetId).getPath();
        return new PanelLine.PanelNavTarget("BUILDING_DETAIL", cultureKey, "", planSetId.getPath());
    }

    @Nullable
    static PanelLine.PanelNavTarget planSetNavTarget(ResourceLocation planSetId) {
        if (planSetId == null) {
            return null;
        }
        String cultureKey = ModCultures.extractCultureId(planSetId).getPath();
        return new PanelLine.PanelNavTarget("BUILDING_DETAIL", cultureKey, "", planSetId.getPath());
    }

    @Nullable
    static PanelLine.PanelNavTarget villagerNavTarget(ResourceLocation typeId) {
        VillagerType vtype = ModCultures.getVillagerType(typeId);
        if (vtype == null || !vtype.travelBookDisplay()) {
            return null;
        }
        String cultureKey = ModCultures.extractCultureId(typeId).getPath();
        return new PanelLine.PanelNavTarget("VILLAGER_DETAIL", cultureKey, "villagers", typeId.getPath());
    }

    @Nullable
    static PanelLine.PanelNavTarget tradeGoodNavTarget(TradeGood good, String cultureKey) {
        if (!good.travelBookDisplay()) {
            return null;
        }
        return new PanelLine.PanelNavTarget("TRADE_GOOD_DETAIL", cultureKey, good.category(), good.id());
    }

    record DirectionInfo(boolean atCenter, int distance, @Nullable String cardinalKey) {
    }
}

