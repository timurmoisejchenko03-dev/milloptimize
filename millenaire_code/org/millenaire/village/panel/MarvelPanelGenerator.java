/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.item.Item
 */
package org.millenaire.village.panel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import org.millenaire.building.AnywoodHelper;
import org.millenaire.building.BuildingInstance;
import org.millenaire.building.BuildingPlanSet;
import org.millenaire.culture.ModCultures;
import org.millenaire.item.ItemHelper;
import org.millenaire.quest.MarvelManager;
import org.millenaire.village.Village;
import org.millenaire.village.panel.PanelContent;
import org.millenaire.village.panel.PanelContentGenerator;
import org.millenaire.village.panel.PanelHelper;
import org.millenaire.village.panel.PanelLine;
import org.millenaire.village.panel.PanelType;

public final class MarvelPanelGenerator {
    private MarvelPanelGenerator() {
    }

    public static PanelContent generateMarvelProjects(Village village) {
        ArrayList<PanelLine> lines = new ArrayList<PanelLine>();
        int[] progress = MarvelPanelGenerator.countMarvelProgress(village);
        int doneLevels = progress[0];
        int totalLevels = progress[1];
        lines.add(PanelLine.translatableWithArgs("panel.millenaire.marvel_projects_done", String.valueOf(doneLevels)));
        lines.add(PanelLine.translatableWithArgs("panel.millenaire.marvel_projects_total", String.valueOf(totalLevels)));
        return new PanelContent(PanelType.MARVEL_PROJECTS, "panel.millenaire.panel_type.marvel_projects", lines, true, null);
    }

    public static PanelContent generateMarvelDonations(Village village) {
        ArrayList<PanelLine> lines = new ArrayList<PanelLine>();
        MarvelManager mm = village.getMarvelManager();
        if (mm == null) {
            lines.add(PanelLine.translatable("panel.millenaire.no_marvel"));
            return new PanelContent(PanelType.MARVEL_DONATIONS, "panel.millenaire.panel_type.marvel_donations", lines, true, null);
        }
        List<String> donations = mm.getDonationList();
        if (donations.isEmpty()) {
            lines.add(PanelLine.translatable("panel.millenaire.no_donations_yet"));
        } else {
            for (int i = donations.size() - 1; i >= 0; --i) {
                String s = donations.get(i);
                DonationDisplay donation = MarvelPanelGenerator.parseDonationDisplay(village, s);
                if (donation != null) {
                    lines.add(PanelLine.translatableWithArgs("panel.millenaire.marvel_donation", donation.villageName(), donation.itemsDescription()));
                } else {
                    lines.add(PanelLine.text(s));
                }
                lines.add(PanelLine.empty());
            }
        }
        return new PanelContent(PanelType.MARVEL_DONATIONS, "panel.millenaire.panel_type.marvel_donations", lines, true, null);
    }

    public static PanelContent generateMarvelResources(Village village, @Nullable ServerLevel level) {
        ArrayList<PanelLine> lines = new ArrayList<PanelLine>();
        MarvelManager mm = village.getMarvelManager();
        if (mm == null || level == null) {
            lines.add(PanelLine.translatable("panel.millenaire.no_marvel"));
            return new PanelContent(PanelType.MARVEL_RESOURCES, "panel.millenaire.panel_type.marvel_resources", lines, true, null);
        }
        if (mm.isMarvelComplete()) {
            lines.add(PanelLine.translatable("panel.millenaire.marvel_complete"));
        } else {
            Map<ResourceLocation, Integer> remaining = mm.getRemainingNeeds(village, level);
            Map<ResourceLocation, Integer> total = mm.getTotalNeeds(village);
            if (remaining.isEmpty()) {
                lines.add(PanelLine.translatable("panel.millenaire.marvel_resources_met"));
            } else {
                for (Map.Entry<ResourceLocation, Integer> entry : total.entrySet()) {
                    String descId;
                    String itemId;
                    int needed = entry.getValue();
                    int left = remaining.getOrDefault(entry.getKey(), 0);
                    int gathered = needed - left;
                    int color = left <= 0 ? 43520 : 0;
                    String ratio = gathered + "/" + needed;
                    if (AnywoodHelper.isAnywood(entry.getKey())) {
                        itemId = "minecraft:oak_log";
                        descId = "item.millenaire.anywood_log";
                    } else {
                        itemId = entry.getKey().toString();
                        Item item = ItemHelper.resolve(entry.getKey());
                        String string = descId = item != null ? item.getDescriptionId() : null;
                    }
                    if (descId != null) {
                        lines.add(PanelLine.withIconTranslatable(descId, ratio, itemId, color));
                        continue;
                    }
                    String itemName = entry.getKey().getPath().replace('_', ' ');
                    lines.add(PanelLine.colored("  " + itemName + ": " + ratio, color));
                }
            }
        }
        return new PanelContent(PanelType.MARVEL_RESOURCES, "panel.millenaire.panel_type.marvel_resources", lines, true, null);
    }

    private static int[] countMarvelProgress(Village village) {
        int totalLevels = 0;
        int doneLevels = 0;
        for (BuildingInstance building : village.getBuildings()) {
            BuildingPlanSet planSet = building.getPlanSetId() != null ? ModCultures.getBuildingPlanSet(building.getPlanSetId()) : null;
            if (planSet == null) continue;
            boolean isMarvelBuilding = planSet.hasTag("marvel");
            if (!isMarvelBuilding) {
                BuildingInstance parent;
                BuildingInstance buildingInstance = parent = building.getParentBuildingId() != null ? village.getBuilding(building.getParentBuildingId()) : null;
                if (parent != null) {
                    BuildingPlanSet parentPlanSet;
                    BuildingPlanSet buildingPlanSet = parentPlanSet = parent.getPlanSetId() != null ? ModCultures.getBuildingPlanSet(parent.getPlanSetId()) : null;
                    if (parentPlanSet != null && parentPlanSet.hasTag("marvel")) {
                        isMarvelBuilding = true;
                    }
                }
            }
            if (!isMarvelBuilding) continue;
            String variant = building.getVariant();
            if (variant == null) {
                variant = planSet.pickRandomVariant(new Random(0L));
            }
            int maxLevel = planSet.getLevelCount(variant);
            totalLevels += maxLevel;
            if (building.getLevel() < 0) continue;
            doneLevels += Math.min(building.getLevel() + 1, maxLevel);
        }
        return new int[]{doneLevels, totalLevels};
    }

    @Nullable
    private static DonationDisplay parseDonationDisplay(Village village, String rawDonation) {
        String[] parts = rawDonation.split(";");
        if (parts.length <= 2 || !parts[0].equals("donation")) {
            return null;
        }
        StringBuilder itemsDesc = new StringBuilder();
        for (int j = 2; j < parts.length; ++j) {
            String[] itemParts;
            if (itemsDesc.length() > 0) {
                itemsDesc.append(", ");
            }
            if ((itemParts = parts[j].split("/")).length == 2) {
                String itemName = PanelHelper.resolveTradeGoodName(village.getCultureId(), itemParts[0]);
                itemsDesc.append(itemParts[1]).append(" ").append(itemName);
                continue;
            }
            itemsDesc.append(parts[j]);
        }
        return new DonationDisplay(parts[1], itemsDesc.toString());
    }

    static void addMarvelProjects3D(PanelContentGenerator.DisplayData data, Village village) {
        data.addCenteredWithIcons(PanelHelper.t("panel.millenaire.panel_type.marvel_projects"), "minecraft:iron_shovel", "minecraft:iron_shovel");
        data.addCentered("");
        int[] progress = MarvelPanelGenerator.countMarvelProgress(village);
        data.addCentered(PanelHelper.t("panel.millenaire.marvel_projects_done") + " " + progress[0]);
        data.addCentered(PanelHelper.t("panel.millenaire.marvel_projects_total") + " " + progress[1]);
    }

    static void addMarvelDonations3D(PanelContentGenerator.DisplayData data, Village village) {
        data.addCenteredWithIcons(PanelHelper.t("panel.millenaire.panel_type.marvel_donations"), "millenaire:denier_or", "millenaire:denier_or");
        data.addCentered("");
        MarvelManager mmDon = village.getMarvelManager();
        if (mmDon == null) {
            data.addCentered(PanelHelper.t("panel.millenaire.no_marvel"));
        } else if (mmDon.getDonationList().isEmpty()) {
            data.addCentered(PanelHelper.t("panel.millenaire.no_donations_yet"));
        } else {
            int donCount = Math.min(mmDon.getDonationList().size(), 3);
            for (int i = mmDon.getDonationList().size() - 1; i >= mmDon.getDonationList().size() - donCount; --i) {
                String s = mmDon.getDonationList().get(i);
                DonationDisplay donation = MarvelPanelGenerator.parseDonationDisplay(village, s);
                if (donation != null) {
                    data.addCentered(PanelHelper.t("panel.millenaire.marvel_donation", donation.villageName(), donation.itemsDescription()));
                    continue;
                }
                data.addCentered(s);
            }
        }
    }

    static void addMarvelResources3D(PanelContentGenerator.DisplayData data, Village village, @Nullable ServerLevel level) {
        data.addCenteredWithIcons(PanelHelper.t("panel.millenaire.panel_type.marvel_resources"), "minecraft:iron_shovel", "minecraft:iron_shovel");
        data.addCentered("");
        MarvelManager mmRes = village.getMarvelManager();
        if (mmRes == null) {
            data.addCentered(PanelHelper.t("panel.millenaire.no_marvel"));
        } else if (mmRes.isMarvelComplete()) {
            data.addCentered(PanelHelper.t("panel.millenaire.marvel_complete"));
        } else if (level != null) {
            Map<ResourceLocation, Integer> remaining = mmRes.getRemainingNeeds(village, level);
            Map<ResourceLocation, Integer> total = mmRes.getTotalNeeds(village);
            if (remaining.isEmpty()) {
                data.addCentered(PanelHelper.t("panel.millenaire.marvel_resources_met"));
            } else {
                int shown = 0;
                for (Map.Entry<ResourceLocation, Integer> entry : total.entrySet()) {
                    Item item;
                    if (shown >= 5) break;
                    int needed = entry.getValue();
                    int left = remaining.getOrDefault(entry.getKey(), 0);
                    int gathered = needed - left;
                    String itemName = AnywoodHelper.isAnywood(entry.getKey()) ? PanelHelper.t("item.millenaire.anywood_log") : ((item = ItemHelper.resolve(entry.getKey())) != null ? item.getDescription().getString() : entry.getKey().getPath().replace('_', ' '));
                    data.addCentered(itemName + ": " + gathered + "/" + needed);
                    ++shown;
                }
            }
        }
    }

    private record DonationDisplay(String villageName, String itemsDescription) {
    }
}

