/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.core.registries.BuiltInRegistries
 *  net.minecraft.network.chat.Component
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.level.Level
 */
package org.millenaire.village.panel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.annotation.Nullable;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import org.millenaire.building.AnywoodHelper;
import org.millenaire.building.BuildingId;
import org.millenaire.building.BuildingInstance;
import org.millenaire.building.BuildingInventory;
import org.millenaire.building.BuildingPlanSet;
import org.millenaire.building.ConstructionTask;
import org.millenaire.culture.ModCultures;
import org.millenaire.culture.VillageType;
import org.millenaire.item.ItemHelper;
import org.millenaire.language.BuildingNameHelper;
import org.millenaire.language.LanguageHelper;
import org.millenaire.village.Village;
import org.millenaire.village.panel.PanelContent;
import org.millenaire.village.panel.PanelContentGenerator;
import org.millenaire.village.panel.PanelHelper;
import org.millenaire.village.panel.PanelLine;
import org.millenaire.village.panel.PanelType;

public final class ConstructionPanelGenerator {
    private ConstructionPanelGenerator() {
    }

    public static PanelContent generateConstructions(Village village, @Nullable ServerPlayer player) {
        ArrayList<PanelLine> lines = new ArrayList<PanelLine>();
        String titleKey = "panel.millenaire.title.constructions";
        String[] titleArgs = new String[]{village.getVillageName()};
        boolean canRead = player != null && LanguageHelper.canReadBuildingNames(player, village.getCultureId());
        boolean found = false;
        for (BuildingInstance b : village.getBuildings()) {
            BuildingPlanSet activePlanSet;
            if (!b.isBeingBuilt()) continue;
            found = true;
            int progress = 0;
            ConstructionTask task = b.getConstructionTask();
            if (task != null) {
                progress = Math.round(task.progress() * 100.0f);
            }
            PanelHelper.DirectionInfo dir = PanelHelper.computeDirectionInfo(village.getCenter(), b.getOrigin());
            PanelLine.PanelNavTarget nav = PanelHelper.buildingNavTarget(b);
            BuildingPlanSet buildingPlanSet = activePlanSet = b.getPlanSetId() != null ? ModCultures.getBuildingPlanSet(b.getPlanSetId()) : null;
            if (activePlanSet != null) {
                lines.add(ConstructionPanelGenerator.buildingNameLine(activePlanSet, canRead, nav, 170));
            } else {
                String fallbackName = b.getPlanId().getPath();
                if (nav != null) {
                    lines.add(new PanelLine(fallbackName, false, null, null, null, false, nav, null, 170, false, 0));
                } else {
                    lines.add(PanelLine.colored(fallbackName, 170));
                }
            }
            if (b.getStatus() == BuildingInstance.Status.UPGRADING) {
                if (dir.atCenter()) {
                    lines.add(PanelLine.translatableWithArgs("panel.millenaire.construction_detail_upgrading_center", String.valueOf(b.getLevel()), String.valueOf(progress)));
                    continue;
                }
                lines.add(PanelLine.translatableWithMixedArgs("panel.millenaire.construction_detail_upgrading_dir", 8, String.valueOf(b.getLevel()), String.valueOf(progress), String.valueOf(dir.distance()), dir.cardinalKey()));
                continue;
            }
            if (dir.atCenter()) {
                lines.add(PanelLine.translatableWithArgs("panel.millenaire.construction_detail_building_center", String.valueOf(progress)));
                continue;
            }
            lines.add(PanelLine.translatableWithMixedArgs("panel.millenaire.construction_detail_building_dir", 4, String.valueOf(progress), String.valueOf(dir.distance()), dir.cardinalKey()));
        }
        if (!found) {
            lines.add(PanelLine.translatable("panel.millenaire.no_construction"));
        }
        lines.add(PanelLine.separator());
        lines.add(PanelLine.translatable("panel.millenaire.existing_buildings"));
        for (BuildingInstance b : village.getBuildings()) {
            BuildingPlanSet allBps;
            if (b.isSubBuilding()) continue;
            String nameKey = PanelHelper.getBuildingTranslationKey(b);
            BuildingPlanSet buildingPlanSet = allBps = b.getPlanSetId() != null ? ModCultures.getBuildingPlanSet(b.getPlanSetId()) : null;
            String nameArg = canRead ? nameKey : (allBps != null ? allBps.nativeName() : b.getPlanId().getPath());
            int centerMask = canRead ? 3 : 2;
            int dirMask = canRead ? 19 : 18;
            PanelHelper.DirectionInfo dir = PanelHelper.computeDirectionInfo(village.getCenter(), b.getOrigin());
            PanelLine.PanelNavTarget bNav = PanelHelper.buildingNavTarget(b);
            if (dir.atCenter()) {
                if (bNav != null) {
                    lines.add(PanelLine.clickableTranslatableWithMixedArgs("panel.millenaire.building_with_level_center", bNav, centerMask, nameArg, "panel.millenaire.level", String.valueOf(b.getLevel())));
                    continue;
                }
                lines.add(PanelLine.translatableWithMixedArgs("panel.millenaire.building_with_level_center", centerMask, nameArg, "panel.millenaire.level", String.valueOf(b.getLevel())));
                continue;
            }
            if (bNav != null) {
                lines.add(PanelLine.clickableTranslatableWithMixedArgs("panel.millenaire.building_with_level_dir", bNav, dirMask, nameArg, "panel.millenaire.level", String.valueOf(b.getLevel()), String.valueOf(dir.distance()), dir.cardinalKey()));
                continue;
            }
            lines.add(PanelLine.translatableWithMixedArgs("panel.millenaire.building_with_level_dir", dirMask, nameArg, "panel.millenaire.level", String.valueOf(b.getLevel()), String.valueOf(dir.distance()), dir.cardinalKey()));
        }
        return new PanelContent(PanelType.CONSTRUCTIONS, titleKey, lines, true, titleArgs);
    }

    public static PanelContent generateProjects(Village village, @Nullable ServerPlayer player) {
        ArrayList<PanelLine> lines = new ArrayList<PanelLine>();
        String titleKey = "panel.millenaire.title.projects";
        String[] titleArgs = new String[]{village.getVillageName()};
        boolean canRead = player != null && LanguageHelper.canReadBuildingNames(player, village.getCultureId());
        VillageType villageType = ModCultures.getVillageType(village.getVillageTypeId());
        if (villageType == null) {
            lines.add(PanelLine.translatable("panel.millenaire.unknown_village_type"));
            return new PanelContent(PanelType.PROJECTS, titleKey, lines, true, titleArgs);
        }
        Village.PendingProject pending = village.getPendingProject();
        if (pending != null) {
            String pendingKey = PanelHelper.getPendingProjectKey(pending);
            lines.add(PanelLine.translatableWithMixedArgs("panel.millenaire.pending_project_value", 1, pendingKey));
            lines.add(PanelLine.translatable("panel.millenaire.awaiting_resources"));
            lines.add(PanelLine.separator());
        }
        LinkedHashMap<String, List> slotsByRole = new LinkedHashMap<String, List>();
        for (VillageType.LayoutSlot slot : villageType.layout()) {
            slotsByRole.computeIfAbsent(slot.role(), k -> new ArrayList()).add(slot);
        }
        HashSet<BuildingId> claimedInstances = new HashSet<BuildingId>();
        for (Map.Entry roleEntry : slotsByRole.entrySet()) {
            String role = (String)roleEntry.getKey();
            List slots = (List)roleEntry.getValue();
            String roleDisplay = role.substring(0, 1).toUpperCase() + role.substring(1);
            lines.add(PanelLine.text("\u00a71" + roleDisplay));
            for (VillageType.LayoutSlot slot : slots) {
                BuildingPlanSet planSet = ModCultures.getBuildingPlanSet(slot.plan());
                if (planSet == null) continue;
                BuildingInstance existing = null;
                for (BuildingInstance b : village.getBuildings()) {
                    if (!slot.plan().equals((Object)b.getPlanSetId()) || claimedInstances.contains(b.getId())) continue;
                    existing = b;
                    break;
                }
                if (existing != null) {
                    claimedInstances.add(existing.getId());
                }
                PanelLine.PanelNavTarget psNav = PanelHelper.planSetNavTarget(slot.plan());
                if (existing == null) {
                    lines.add(ConstructionPanelGenerator.buildingNameLine(planSet, canRead, psNav, 0x5555FF));
                    lines.add(PanelLine.translatable("panel.millenaire.not_yet_built"));
                    continue;
                }
                if (!planSet.hasNextLevel(existing.getVariant(), existing.getLevel())) {
                    PanelHelper.DirectionInfo dir = PanelHelper.computeDirectionInfo(village.getCenter(), existing.getOrigin());
                    lines.add(ConstructionPanelGenerator.buildingNameLine(planSet, canRead, psNav, 43520));
                    if (dir.atCenter()) {
                        lines.add(PanelLine.translatable("panel.millenaire.finished_center"));
                        continue;
                    }
                    lines.add(PanelLine.translatableWithMixedArgs("panel.millenaire.finished_dir", 2, String.valueOf(dir.distance()), dir.cardinalKey()));
                    continue;
                }
                int maxLevel = planSet.getLevelCount(existing.getVariant()) - 1;
                int remaining = maxLevel - existing.getLevel();
                PanelHelper.DirectionInfo dir = PanelHelper.computeDirectionInfo(village.getCenter(), existing.getOrigin());
                lines.add(ConstructionPanelGenerator.buildingNameLine(planSet, canRead, psNav, 0));
                if (dir.atCenter()) {
                    lines.add(PanelLine.translatableWithArgs("panel.millenaire.upgrades_left_center", String.valueOf(remaining)));
                    continue;
                }
                lines.add(PanelLine.translatableWithMixedArgs("panel.millenaire.upgrades_left_dir", 4, String.valueOf(remaining), String.valueOf(dir.distance()), dir.cardinalKey()));
            }
        }
        return new PanelContent(PanelType.PROJECTS, titleKey, lines, true, titleArgs);
    }

    private static PanelLine buildingNameLine(BuildingPlanSet planSet, boolean canRead, @Nullable PanelLine.PanelNavTarget nav, int color) {
        if (canRead) {
            String key = BuildingNameHelper.getTranslationKey(planSet);
            return nav != null ? PanelLine.clickableWithTranslationColored(planSet.nativeName(), key, nav, color) : PanelLine.withTranslationColored(planSet.nativeName(), key, color);
        }
        return nav != null ? new PanelLine(planSet.nativeName(), false, null, null, null, false, nav, null, color, false, 0) : PanelLine.colored(planSet.nativeName(), color);
    }

    public static PanelContent generateResources(Village village, @Nullable ServerLevel level) {
        ArrayList<PanelLine> lines = new ArrayList<PanelLine>();
        String titleKey = "panel.millenaire.title.resources";
        String[] titleArgs = new String[]{village.getVillageName()};
        PanelContentGenerator.ResourcePanelData data = PanelContentGenerator.collectResourcePanelData(village);
        if (data.projectNameKey() != null) {
            String headerKey = data.inProgress() ? "panel.millenaire.in_construction" : "panel.millenaire.resources_needed";
            lines.add(PanelLine.translatable(headerKey));
            if (data.isUpgrade()) {
                lines.add(PanelLine.translatableWithMixedArgs("panel.millenaire.project_name_upgrade", 0, data.projectNameKey(), String.valueOf(data.level())));
            } else {
                lines.add(PanelLine.translatableWithMixedArgs("panel.millenaire.project_name_construction", 0, data.projectNameKey()));
            }
            lines.add(PanelLine.text(""));
            if (!data.resources().isEmpty()) {
                if (data.inProgress()) {
                    ConstructionPanelGenerator.addResourceLinesAllMet(lines, data.resources());
                } else {
                    PanelContentGenerator.addResourceLines(lines, data.resources(), village, level, false);
                }
            } else {
                lines.add(PanelLine.translatable("panel.millenaire.no_resources_needed"));
            }
        } else {
            lines.add(PanelLine.translatable("panel.millenaire.no_project"));
        }
        lines.add(PanelLine.separator());
        lines.add(PanelLine.translatable("panel.millenaire.resources_available"));
        lines.add(PanelLine.text(""));
        BuildingInstance townhall = village.getTownhall();
        if (townhall != null && townhall.getInventory() != null && level != null) {
            BuildingInventory inv = townhall.getInventory();
            Map<Item, Integer> contents = inv.scanChests((Level)level);
            record InvEntry(String descId, String itemId, int count) {
            }
            TreeMap<String, InvEntry> sorted = new TreeMap<String, InvEntry>();
            for (Map.Entry<Item, Integer> entry : contents.entrySet()) {
                if (entry.getValue() <= 0) continue;
                String descId = entry.getKey().getDescriptionId();
                String sortName = Component.translatable((String)descId).getString();
                ResourceLocation itemKey = BuiltInRegistries.ITEM.getKey((Object)entry.getKey());
                sorted.put(sortName, new InvEntry(descId, itemKey.toString(), entry.getValue()));
            }
            if (sorted.isEmpty()) {
                lines.add(PanelLine.translatableColored("panel.millenaire.no_chests", 0x555555));
            } else {
                for (Map.Entry<Object, Integer> entry : sorted.entrySet()) {
                    InvEntry entry2 = (InvEntry)((Object)entry.getValue());
                    lines.add(PanelLine.withIconTranslatable(entry2.descId, "\u00a71" + entry2.count, entry2.itemId, 0));
                }
            }
        } else if (level == null) {
            lines.add(PanelLine.translatable("panel.millenaire.resources_unavailable_offline"));
        } else {
            lines.add(PanelLine.translatable("panel.millenaire.no_chests"));
        }
        return new PanelContent(PanelType.RESOURCES, titleKey, lines, true, titleArgs);
    }

    private static void addResourceLinesAllMet(List<PanelLine> lines, Map<ResourceLocation, Integer> required) {
        for (Map.Entry<ResourceLocation, Integer> entry : required.entrySet()) {
            String descId;
            String itemId;
            if (entry.getKey().getPath().startsWith("mock_")) continue;
            int cost = entry.getValue();
            if (AnywoodHelper.isAnywood(entry.getKey())) {
                itemId = "minecraft:oak_log";
                descId = "item.millenaire.anywood_log";
            } else {
                itemId = entry.getKey().toString();
                Item item = ItemHelper.resolve(entry.getKey());
                descId = item != null ? item.getDescriptionId() : null;
            }
            String right = "\u00a72" + cost + "/" + cost;
            if (descId != null) {
                lines.add(PanelLine.withIconTranslatable(descId, right, itemId, 43520));
                continue;
            }
            String name = entry.getKey().getPath().replace('_', ' ');
            lines.add(PanelLine.withIcon("  \u00a72" + name, right, itemId));
        }
    }

    static void addConstructions3D(PanelContentGenerator.DisplayData data, Village village) {
        int nbActive = 0;
        for (BuildingInstance b : village.getBuildings()) {
            if (!b.isBeingBuilt()) continue;
            ++nbActive;
        }
        if (nbActive == 1) {
            for (BuildingInstance b : village.getBuildings()) {
                if (!b.isBeingBuilt()) continue;
                String bldIcon = PanelHelper.resolveBuildingIcon(b);
                data.addCenteredBuildingNameWithIcons(PanelHelper.getBuildingTranslationKey(b), bldIcon, bldIcon, BuildingNameHelper.getServerFallbackName(b));
                data.addCentered("");
                int progress = 0;
                ConstructionTask task = b.getConstructionTask();
                if (task != null) {
                    progress = Math.round(task.progress() * 100.0f);
                }
                data.addCentered(PanelHelper.t("panel.millenaire.in_construction") + " " + progress + "%");
                BuildingInstance th = village.getTownhall();
                if (th != null) {
                    data.addCentered(PanelHelper.computeDirection(th.getOrigin(), b.getOrigin()));
                }
                break;
            }
        } else if (nbActive > 1) {
            data.addCentered(nbActive + " " + PanelHelper.t("panel.millenaire.constructions_title"));
            data.addCentered("");
            for (BuildingInstance b : village.getBuildings()) {
                if (!b.isBeingBuilt()) continue;
                String bldIcon = PanelHelper.resolveBuildingIcon(b);
                data.addCenteredBuildingNameWithIcons(PanelHelper.getBuildingTranslationKey(b), bldIcon, bldIcon, BuildingNameHelper.getServerFallbackName(b));
                if (data.displayLines().size() < 7) continue;
                break;
            }
        } else {
            data.addCentered(PanelHelper.t("panel.millenaire.no_construction"));
            data.addCentered("");
        }
    }

    static void addProjects3D(PanelContentGenerator.DisplayData data, Village village) {
        BuildingInstance activeBuild = null;
        for (BuildingInstance b : village.getBuildings()) {
            if (!b.isBeingBuilt()) continue;
            activeBuild = b;
            break;
        }
        Village.PendingProject pending = village.getPendingProject();
        if (activeBuild != null) {
            goalSet = activeBuild.getPlanSetId() != null ? ModCultures.getBuildingPlanSet(activeBuild.getPlanSetId()) : null;
            String goalIcon = goalSet != null && goalSet.icon() != null ? goalSet.icon() : "";
            data.addCenteredWithIcons(PanelHelper.t("panel.millenaire.projects_title"), goalIcon, goalIcon);
            data.addCentered("");
            data.addCenteredBuildingName(PanelHelper.getBuildingTranslationKey(activeBuild), BuildingNameHelper.getServerFallbackName(activeBuild));
            data.addCentered("");
            data.addCentered(PanelHelper.t("panel.millenaire.in_construction"));
        } else if (pending != null) {
            goalSet = ModCultures.getBuildingPlanSet(pending.planSetId());
            String goalIcon = goalSet != null && goalSet.icon() != null ? goalSet.icon() : "";
            data.addCenteredWithIcons(PanelHelper.t("panel.millenaire.projects_title"), goalIcon, goalIcon);
            data.addCentered("");
            data.addCenteredTranslatable(PanelHelper.getPendingProjectKey(pending));
            data.addCentered("");
            data.addCentered(PanelHelper.t("panel.millenaire.awaiting_resources"));
        } else {
            data.addCentered("");
            data.addCentered("");
            data.addCentered(PanelHelper.t("panel.millenaire.goals_completed"));
        }
    }

    static void addResources3D(PanelContentGenerator.DisplayData data, Village village, @Nullable ServerLevel level) {
        PanelContentGenerator.ResourcePanelData rpd = PanelContentGenerator.collectResourcePanelData(village);
        data.addCenteredWithIcons(PanelHelper.t("panel.millenaire.resources_title"), "minecraft:chest", "minecraft:chest");
        if (rpd.projectNameKey() != null && !rpd.resources().isEmpty()) {
            BuildingInstance townhall;
            if (rpd.resources().size() < 6) {
                data.addCentered("");
            }
            BuildingInventory inv = (townhall = village.getTownhall()) != null ? townhall.getInventory() : null;
            for (Map.Entry<ResourceLocation, Integer> req : rpd.resources().entrySet()) {
                String itemId;
                int has;
                if (req.getKey().getPath().startsWith("mock_")) continue;
                int cost = req.getValue();
                if (rpd.inProgress()) {
                    has = cost;
                    itemId = AnywoodHelper.isAnywood(req.getKey()) ? "minecraft:oak_log" : req.getKey().toString();
                } else if (AnywoodHelper.isAnywood(req.getKey())) {
                    if (inv != null && level != null) {
                        ServerLevel lvl = level;
                        BuildingInventory thInv = inv;
                        int rawCount = thInv.getCountByTag((Level)lvl, AnywoodHelper.LOGS_TAG);
                        has = Math.min(AnywoodHelper.anywoodAvailable(rawCount, rpd.resources(), key -> {
                            Item logItem = ItemHelper.resolve(key);
                            return logItem == null ? 0 : thInv.getCount((Level)lvl, logItem);
                        }), cost);
                    } else {
                        has = 0;
                    }
                    itemId = "minecraft:oak_log";
                } else {
                    Item item;
                    has = 0;
                    if (inv != null && level != null && (item = ItemHelper.resolve(req.getKey())) != null) {
                        has = Math.min(inv.getCount((Level)level, item), cost);
                    }
                    itemId = req.getKey().toString();
                }
                data.addLeftWithIcon(has + "/" + cost, itemId);
                if (data.displayLines().size() < 8) continue;
                break;
            }
        } else if (rpd.projectNameKey() != null) {
            data.addCentered("");
            data.addCentered(PanelHelper.t("panel.millenaire.no_resources_needed"));
        } else {
            data.addCentered("");
            data.addCentered(PanelHelper.t("panel.millenaire.no_project"));
        }
    }
}

