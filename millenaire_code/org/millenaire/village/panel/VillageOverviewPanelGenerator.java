/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.network.chat.Component
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.entity.Entity
 */
package org.millenaire.village.panel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.millenaire.DisplayUtils;
import org.millenaire.building.BuildingInstance;
import org.millenaire.building.BuildingPlanSet;
import org.millenaire.culture.Gender;
import org.millenaire.culture.ModCultures;
import org.millenaire.culture.VillageType;
import org.millenaire.culture.VillagerType;
import org.millenaire.entity.MillVillager;
import org.millenaire.language.BuildingNameHelper;
import org.millenaire.village.Village;
import org.millenaire.village.VillageEvent;
import org.millenaire.village.VillageId;
import org.millenaire.village.VillageManager;
import org.millenaire.village.VillageRelations;
import org.millenaire.village.VillageSavedData;
import org.millenaire.village.VillagerRecord;
import org.millenaire.village.panel.PanelContent;
import org.millenaire.village.panel.PanelContentGenerator;
import org.millenaire.village.panel.PanelHelper;
import org.millenaire.village.panel.PanelLine;
import org.millenaire.village.panel.PanelType;

public final class VillageOverviewPanelGenerator {
    private static final int SUMMARY_CHRONICLE_LIMIT = 20;

    private VillageOverviewPanelGenerator() {
    }

    public static PanelContent generateSummary(Village village, @Nullable BuildingInstance building, @Nullable ServerLevel level) {
        ArrayList<PanelLine> lines = new ArrayList<PanelLine>();
        String titleKey = "panel.millenaire.title.village_summary";
        String[] titleArgs = new String[]{village.getVillageName()};
        VillageType villageType = ModCultures.getVillageType(village.getVillageTypeId());
        if (villageType != null) {
            lines.add(PanelLine.translatableWithArgs("panel.millenaire.village_type_value", villageType.name()));
        }
        PopulationCounts population = VillageOverviewPanelGenerator.countPopulation(village);
        lines.add(PanelLine.translatableWithArgs("panel.millenaire.population_value", String.valueOf(population.total())));
        lines.add(PanelLine.translatableWithArgs("panel.millenaire.adults_value", String.valueOf(population.adults()), String.valueOf(population.men()), String.valueOf(population.women())));
        if (population.children() > 0) {
            lines.add(PanelLine.translatableWithArgs("panel.millenaire.children_value", String.valueOf(population.children())));
        }
        lines.add(PanelLine.separator());
        BuildingInstance currentProject = null;
        for (BuildingInstance b : village.getBuildings()) {
            if (!b.isBeingBuilt()) continue;
            currentProject = b;
            break;
        }
        if (currentProject != null) {
            BuildingPlanSet.LevelDef levelDef;
            BuildingPlanSet planSet;
            String projectNative = BuildingNameHelper.getServerFallbackName(currentProject);
            if (currentProject.getStatus() == BuildingInstance.Status.UPGRADING) {
                lines.add(PanelLine.translatableWithMixedArgs("panel.millenaire.building_project_upgrading", 1, "panel.millenaire.building_project", projectNative, String.valueOf(currentProject.getLevel())));
            } else {
                lines.add(PanelLine.translatableWithMixedArgs("panel.millenaire.building_project_constructing", 1, "panel.millenaire.building_project", projectNative));
            }
            lines.add(PanelLine.text(""));
            if (currentProject.getPlanSetId() != null && currentProject.getVariant() != null && (planSet = ModCultures.getBuildingPlanSet(currentProject.getPlanSetId())) != null && (levelDef = planSet.getLevel(currentProject.getVariant(), currentProject.getLevel())) != null && !levelDef.requiredResources().isEmpty()) {
                lines.add(PanelLine.translatable("panel.millenaire.resources_needed"));
                PanelContentGenerator.addResourceLines(lines, levelDef.requiredResources(), village, level);
            }
        } else {
            Village.PendingProject pending = village.getPendingProject();
            if (pending != null) {
                String pendingKey = PanelHelper.getPendingProjectKey(pending);
                lines.add(PanelLine.translatableWithMixedArgs("panel.millenaire.pending_project_value", 1, pendingKey));
                lines.add(PanelLine.translatable("panel.millenaire.awaiting_resources"));
            } else {
                lines.add(PanelLine.translatable("panel.millenaire.goals_completed"));
            }
        }
        lines.add(PanelLine.separator());
        lines.add(PanelLine.translatable("panel.millenaire.current_constructions"));
        boolean anyConstruction = false;
        for (BuildingInstance b : village.getBuildings()) {
            if (!b.isBeingBuilt()) continue;
            anyConstruction = true;
            String nameArg = BuildingNameHelper.getServerFallbackName(b);
            PanelHelper.DirectionInfo dir = PanelHelper.computeDirectionInfo(village.getCenter(), b.getOrigin());
            if (b.getStatus() == BuildingInstance.Status.UPGRADING) {
                if (dir.atCenter()) {
                    lines.add(PanelLine.translatableWithMixedArgs("panel.millenaire.construction_line_upgrading_center", 0, nameArg, String.valueOf(b.getLevel())));
                    continue;
                }
                lines.add(PanelLine.translatableWithMixedArgs("panel.millenaire.construction_line_upgrading_dir", 8, nameArg, String.valueOf(b.getLevel()), String.valueOf(dir.distance()), dir.cardinalKey()));
                continue;
            }
            if (dir.atCenter()) {
                lines.add(PanelLine.translatableWithMixedArgs("panel.millenaire.construction_line_building_center", 0, nameArg));
                continue;
            }
            lines.add(PanelLine.translatableWithMixedArgs("panel.millenaire.construction_line_building_dir", 4, nameArg, String.valueOf(dir.distance()), dir.cardinalKey()));
        }
        if (!anyConstruction) {
            lines.add(PanelLine.translatable("panel.millenaire.no_construction"));
        }
        if (!village.getRelations().isEmpty()) {
            lines.add(PanelLine.separator());
            lines.add(PanelLine.translatable("panel.millenaire.diplomacy"));
            VillageManager vm = level != null ? VillageSavedData.get(level).getVillageManager() : null;
            for (Map.Entry<VillageId, Integer> relEntry : village.getRelations().entrySet()) {
                Village other = vm != null ? vm.getVillage(relEntry.getKey()) : null;
                if (other == null || village.getParentVillageId() != null && village.getParentVillageId().equals(other.getId()) || other.getParentVillageId() != null && other.getParentVillageId().equals(village.getId())) continue;
                String otherName = other.getVillageName() != null ? other.getVillageName() : other.getVillageTypeId().getPath();
                int rel = relEntry.getValue();
                String relKey = VillageRelations.getRelationKey(rel);
                String diplomacyKey = rel >= 50 ? "panel.millenaire.diplomacy_friendly" : (rel >= 0 ? "panel.millenaire.diplomacy_neutral" : (rel > -50 ? "panel.millenaire.diplomacy_cold" : "panel.millenaire.diplomacy_hostile"));
                lines.add(PanelLine.translatableWithMixedArgs(diplomacyKey, 2, otherName, relKey));
            }
        }
        VillageOverviewPanelGenerator.appendChronicleLines(lines, village);
        return new PanelContent(PanelType.VILLAGE_SUMMARY, titleKey, lines, true, titleArgs);
    }

    public static PanelContent generatePopulation(Village village, @Nullable ServerLevel level) {
        ArrayList<PanelLine> lines = new ArrayList<PanelLine>();
        String titleKey = "panel.millenaire.title.population";
        String[] titleArgs = new String[]{village.getVillageName()};
        Map<UUID, ResourceLocation> villagerTypes = village.getVillagerTypes();
        if (villagerTypes.isEmpty()) {
            lines.add(PanelLine.translatable("panel.millenaire.no_inhabitants"));
            return new PanelContent(PanelType.POPULATION, titleKey, lines, true, titleArgs);
        }
        for (Map.Entry<UUID, ResourceLocation> entry : villagerTypes.entrySet()) {
            boolean awayHired;
            VillagerType vtype;
            UUID uuid = entry.getKey();
            ResourceLocation typeId = entry.getValue();
            String roleKey = PanelHelper.resolveRoleKey(typeId);
            Object displayName = "???";
            boolean entityLoaded = false;
            String goalLabel = null;
            boolean isMissing = false;
            boolean isAbsent = false;
            if (level != null) {
                Entity entity = level.getEntity(uuid);
                if (entity instanceof MillVillager) {
                    MillVillager mv = (MillVillager)entity;
                    entityLoaded = true;
                    String first = mv.getFirstName();
                    String family = mv.getFamilyName();
                    if (first != null && !first.isEmpty()) {
                        displayName = first + (String)(family != null && !family.isEmpty() ? " " + family : "");
                    }
                    goalLabel = mv.getGoalLabel();
                } else {
                    int missing = village.getMissingCount(uuid);
                    if (missing >= 3) {
                        isMissing = true;
                    } else {
                        isAbsent = true;
                    }
                }
            }
            String iconItem = (vtype = ModCultures.getVillagerType(typeId)) != null && vtype.icon() != null ? vtype.icon() : "";
            PanelLine.PanelNavTarget nav = PanelHelper.villagerNavTarget(typeId);
            String nameText = "\u00a70" + (String)displayName;
            if (!iconItem.isEmpty()) {
                if (nav != null) {
                    lines.add(PanelLine.clickableWithIcon(nameText, "", iconItem, nav));
                } else {
                    lines.add(PanelLine.withIcon(nameText, "", iconItem));
                }
            } else if (nav != null) {
                lines.add(PanelLine.clickableText(nameText, nav));
            } else {
                lines.add(PanelLine.text(nameText));
            }
            VillagerRecord record = village.getVillagerRecord(uuid);
            boolean bl = awayHired = record != null && record.isAwayHired();
            if (awayHired) {
                lines.add(PanelLine.translatableWithMixedArgs("panel.millenaire.role_and_activity", 1, roleKey, Component.translatable((String)"panel.millenaire.awayhired").getString()));
                continue;
            }
            if (isMissing) {
                lines.add(PanelLine.translatableWithMixedArgs("panel.millenaire.missing_villager_status", 1, roleKey));
                continue;
            }
            if (isAbsent) {
                lines.add(PanelLine.translatableWithMixedArgs("panel.millenaire.absent_status", 1, roleKey));
                continue;
            }
            if (goalLabel != null && !goalLabel.isEmpty()) {
                int argMask = DisplayUtils.isGoalTranslationKey(goalLabel) ? 3 : 1;
                lines.add(PanelLine.translatableWithMixedArgs("panel.millenaire.villager_goal", argMask, roleKey, goalLabel));
                continue;
            }
            lines.add(PanelLine.translatableWithMixedArgs("panel.millenaire.role_and_activity", 1, roleKey, ""));
        }
        return new PanelContent(PanelType.POPULATION, titleKey, lines, true, titleArgs);
    }

    public static void appendChronicleLines(List<PanelLine> lines, Village village) {
        List<VillageEvent> events = village.getChronicle();
        if (events.isEmpty()) {
            return;
        }
        lines.add(PanelLine.separator());
        lines.add(PanelLine.translatable("gui.millenaire.book.section.chronicle"));
        int start = Math.max(0, events.size() - 20);
        for (int i = events.size() - 1; i >= start; --i) {
            VillageEvent event = events.get(i);
            long dayNumber = event.gameTime() / 24000L + 1L;
            String dayStr = String.valueOf(dayNumber);
            if (event.param2() != null) {
                lines.add(PanelLine.translatableWithArgs(event.type().i18nKey(), dayStr, event.param1(), event.param2()));
                continue;
            }
            lines.add(PanelLine.translatableWithArgs(event.type().i18nKey(), dayStr, event.param1()));
        }
    }

    public static PanelContent generateChronicle(Village village) {
        ArrayList<PanelLine> lines = new ArrayList<PanelLine>();
        List<VillageEvent> events = village.getChronicle();
        if (events.isEmpty()) {
            lines.add(PanelLine.translatable("chronicle.millenaire.empty"));
        } else {
            for (int i = events.size() - 1; i >= 0; --i) {
                VillageEvent event = events.get(i);
                long dayNumber = event.gameTime() / 24000L + 1L;
                String dayStr = String.valueOf(dayNumber);
                if (event.param2() != null) {
                    lines.add(PanelLine.translatableWithArgs(event.type().i18nKey(), dayStr, event.param1(), event.param2()));
                    continue;
                }
                lines.add(PanelLine.translatableWithArgs(event.type().i18nKey(), dayStr, event.param1()));
            }
        }
        return new PanelContent(PanelType.CHRONICLE, "panel.millenaire.panel_type.chronicle", lines, true, null);
    }

    private static PopulationCounts countPopulation(Village village) {
        Map<UUID, ResourceLocation> villagerTypes = village.getVillagerTypes();
        int nbMen = 0;
        int nbWomen = 0;
        int nbChildren = 0;
        for (ResourceLocation typeId : villagerTypes.values()) {
            VillagerType vType = ModCultures.getVillagerType(typeId);
            if (vType == null) continue;
            if (vType.isChild()) {
                ++nbChildren;
                continue;
            }
            if (vType.gender() == Gender.FEMALE) {
                ++nbWomen;
                continue;
            }
            ++nbMen;
        }
        return new PopulationCounts(villagerTypes.size(), nbMen, nbWomen, nbChildren);
    }

    static void addSummary3D(PanelContentGenerator.DisplayData data, Village village) {
        String banner = "minecraft:white_banner";
        data.addCenteredWithIcons(village.getVillageName(), banner, banner);
        VillageType villageType = ModCultures.getVillageType(village.getVillageTypeId());
        if (villageType != null) {
            data.addCentered(villageType.name());
        }
        data.addCentered("");
        data.addCentered(PanelHelper.t("panel.millenaire.population") + ": " + village.getVillagerUuids().size());
        data.addCentered("");
    }

    static void addPopulation3D(PanelContentGenerator.DisplayData data, Village village) {
        data.addCenteredWithIcons(PanelHelper.t("panel.millenaire.population_title"), "minecraft:blue_orchid", "minecraft:pink_tulip");
        data.addCentered("");
        PopulationCounts population = VillageOverviewPanelGenerator.countPopulation(village);
        data.addCentered(PanelHelper.t("panel.millenaire.adults") + ": " + population.adults() + " (" + population.men() + "M / " + population.women() + "F)");
        if (population.children() > 0) {
            data.addCentered(PanelHelper.t("panel.millenaire.children_section") + ": " + population.children());
        }
    }

    private record PopulationCounts(int total, int men, int women, int children) {
        int adults() {
            return this.men + this.women;
        }
    }
}

