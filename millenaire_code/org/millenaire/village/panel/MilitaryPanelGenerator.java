/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.core.registries.BuiltInRegistries
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.ItemStack
 */
package org.millenaire.village.panel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.millenaire.culture.ModCultures;
import org.millenaire.culture.VillagerType;
import org.millenaire.entity.MillVillager;
import org.millenaire.village.Village;
import org.millenaire.village.panel.PanelContent;
import org.millenaire.village.panel.PanelContentGenerator;
import org.millenaire.village.panel.PanelHelper;
import org.millenaire.village.panel.PanelLine;
import org.millenaire.village.panel.PanelType;

public final class MilitaryPanelGenerator {
    private MilitaryPanelGenerator() {
    }

    public static PanelContent generateMilitary(Village village, @Nullable ServerLevel level) {
        ArrayList<PanelLine> lines = new ArrayList<PanelLine>();
        String titleKey = "panel.millenaire.title.military";
        String[] titleArgs = new String[]{village.getVillageName()};
        lines.add(MilitaryPanelGenerator.raidStatusLine(village));
        lines.add(PanelLine.separator());
        int fighters = 0;
        ArrayList<PanelLine> fighterLines = new ArrayList<PanelLine>();
        for (Map.Entry<UUID, ResourceLocation> entry : village.getVillagerTypes().entrySet()) {
            Entity entity;
            VillagerType vType = ModCultures.getVillagerType(entry.getValue());
            if (vType == null) continue;
            boolean isDefender = vType.hasTag("helpInAttacks");
            boolean isRaider = vType.hasTag("isRaider");
            if (!isDefender && !isRaider) continue;
            ++fighters;
            String roleKey = PanelHelper.resolveRoleKey(entry.getValue());
            String statusKey = isDefender && isRaider ? "panel.millenaire.fighter_status_both" : (isDefender ? "panel.millenaire.fighter_status_defender" : "panel.millenaire.fighter_status_raider");
            Object displayName = "???";
            ArrayList<PanelLine> statsLines = new ArrayList<PanelLine>();
            if (level != null && (entity = level.getEntity(entry.getKey())) instanceof MillVillager) {
                MillVillager mv = (MillVillager)entity;
                String first = mv.getFirstName();
                String family = mv.getFamilyName();
                if (first != null && !first.isEmpty()) {
                    displayName = first + (String)(family != null && !family.isEmpty() ? " " + family : "");
                }
                statsLines.add(PanelLine.translatableWithArgsColored("panel.millenaire.health_value", 0x555555, String.valueOf(Math.round(mv.getMaxHealth()))));
                int armorValue = mv.getArmorValue();
                statsLines.add(PanelLine.translatableWithArgsColored("panel.millenaire.armour_value", 0x555555, String.valueOf(armorValue)));
                ItemStack mainHand = mv.getMainHandItem();
                if (!mainHand.isEmpty()) {
                    String weaponDescId = mainHand.getItem().getDescriptionId();
                    statsLines.add(PanelLine.translatableWithMixedArgsColored("panel.millenaire.weapons_value", 0x555555, 1, weaponDescId));
                }
            }
            fighterLines.add(PanelLine.translatableWithMixedArgs("panel.millenaire.fighter_name_role", 2, new String[]{displayName, roleKey}));
            fighterLines.add(PanelLine.translatableColored(statusKey, 170));
            fighterLines.addAll(statsLines);
            fighterLines.add(PanelLine.text(""));
        }
        lines.add(PanelLine.translatableWithArgs("panel.millenaire.offensive_value", String.valueOf(village.getVillageRaidingStrength())));
        lines.add(PanelLine.translatableWithArgs("panel.millenaire.defensive_value", String.valueOf(village.getVillageDefendingStrength())));
        lines.add(PanelLine.text(""));
        lines.add(PanelLine.translatableWithArgs("panel.millenaire.defenders_value", String.valueOf(fighters)));
        lines.add(PanelLine.text(""));
        if (fighters == 0) {
            lines.add(PanelLine.translatable("panel.millenaire.no_defenders"));
        } else {
            lines.addAll(fighterLines);
        }
        MilitaryPanelGenerator.appendRaidHistory(lines, village.getRaidsPerformed(), "panel.millenaire.raids_performed", 170, "panel.millenaire.raid_history_success", 30464, "panel.millenaire.raid_history_failure", 0xAA0000);
        MilitaryPanelGenerator.appendRaidHistory(lines, village.getRaidsSuffered(), "panel.millenaire.raids_suffered", 0xAA0000, "panel.millenaire.raid_history_raided", 0xAA0000, "panel.millenaire.raid_history_defended", 30464);
        return new PanelContent(PanelType.MILITARY, titleKey, lines, true, titleArgs);
    }

    private static void appendRaidHistory(List<PanelLine> lines, List<String> history, String titleKey, int titleColor, String successKey, int successColor, String failureKey, int failureColor) {
        if (history == null || history.isEmpty()) {
            return;
        }
        lines.add(PanelLine.separator());
        lines.add(PanelLine.translatableColored(titleKey, titleColor));
        lines.add(PanelLine.text(""));
        for (int i = history.size() - 1; i >= 0; --i) {
            String[] parts = history.get(i).split(";");
            if (parts.length < 2) continue;
            boolean success = "success".equals(parts[0]);
            String otherName = parts[1];
            if (!success) {
                lines.add(PanelLine.translatableWithArgsColored(failureKey, failureColor, otherName));
                lines.add(PanelLine.text(""));
                continue;
            }
            lines.add(PanelLine.translatableWithArgsColored(successKey, successColor, otherName));
            boolean anyLoot = false;
            for (int j = 2; j < parts.length; ++j) {
                int slash = parts[j].lastIndexOf(47);
                if (slash <= 0 || slash == parts[j].length() - 1) continue;
                String id = parts[j].substring(0, slash);
                String qty = parts[j].substring(slash + 1);
                Item item = BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse((String)id)).orElse(null);
                if (item == null) continue;
                lines.add(PanelLine.withIconTranslatable(item.getDescriptionId(), "x" + qty, id, 0x555555));
                anyLoot = true;
            }
            if (!anyLoot) {
                lines.add(PanelLine.translatableColored("panel.millenaire.raid_nothing", 0x555555));
            }
            lines.add(PanelLine.text(""));
        }
    }

    static void addMilitary3D(PanelContentGenerator.DisplayData data, Village village) {
        data.addCenteredWithIcons(PanelHelper.t("panel.millenaire.military_title"), "minecraft:iron_sword", "minecraft:iron_sword");
        data.addCentered("");
        data.addCentered(PanelHelper.t(MilitaryPanelGenerator.raidStatusKey(village)));
        data.addCenteredWithIcons(PanelHelper.t("panel.millenaire.offensive_strength") + ": " + village.getVillageRaidingStrength(), "minecraft:iron_axe", "minecraft:iron_axe");
        data.addCenteredWithIcons(PanelHelper.t("panel.millenaire.defensive_strength") + ": " + village.getVillageDefendingStrength(), "minecraft:iron_chestplate", "minecraft:iron_chestplate");
    }

    private static String raidStatusKey(Village village) {
        if (village.isUnderAttack()) {
            return "panel.millenaire.village_under_attack";
        }
        if (village.getRaidTarget() != null) {
            return village.getRaidStart() > 0L ? "panel.millenaire.village_raiding" : "panel.millenaire.village_raid_planned";
        }
        return "panel.millenaire.village_at_peace";
    }

    private static PanelLine raidStatusLine(Village village) {
        return PanelLine.translatable(MilitaryPanelGenerator.raidStatusKey(village));
    }
}

