/*
 * Decompiled with CFR 0.152.
 */
package org.millenaire.village.panel;

import java.util.ArrayList;
import org.millenaire.village.Village;
import org.millenaire.village.panel.PanelContent;
import org.millenaire.village.panel.PanelContentGenerator;
import org.millenaire.village.panel.PanelHelper;
import org.millenaire.village.panel.PanelLine;
import org.millenaire.village.panel.PanelType;

public final class VillageMapPanelGenerator {
    private VillageMapPanelGenerator() {
    }

    public static PanelContent generateVillageMap(Village village) {
        ArrayList<PanelLine> lines = new ArrayList<PanelLine>();
        String title = village.getVillageName();
        lines.add(PanelLine.translatable("panel.millenaire.map_legend_title"));
        lines.add(PanelLine.text(""));
        lines.add(PanelLine.translatableColored("panel.millenaire.map_blue", 0x5555FF));
        lines.add(PanelLine.translatableColored("panel.millenaire.map_purple", 0xFF55FF));
        lines.add(PanelLine.translatableColored("panel.millenaire.map_orange_upgrading", 16744512));
        lines.add(PanelLine.translatableColored("panel.millenaire.map_dark_blue", 170));
        lines.add(PanelLine.translatableColored("panel.millenaire.map_cyan", 0x55FFFF));
        lines.add(PanelLine.translatableColored("panel.millenaire.map_red", 0xAA0000));
        lines.add(PanelLine.translatableColored("panel.millenaire.map_yellow", 0xCCCC00));
        lines.add(PanelLine.translatableColored("panel.millenaire.map_white", 0));
        return new PanelContent(PanelType.VILLAGE_MAP, title, lines);
    }

    static void addVillageMap3D(PanelContentGenerator.DisplayData data, Village village) {
        data.addCenteredWithIcons(PanelHelper.t("panel.millenaire.panel_type.village_map"), "minecraft:filled_map", "minecraft:filled_map");
        data.addCentered("");
        int nbBuildings = village.getBuildings().size();
        data.addCentered(nbBuildings + " " + PanelHelper.t("panel.millenaire.buildings"));
    }
}

