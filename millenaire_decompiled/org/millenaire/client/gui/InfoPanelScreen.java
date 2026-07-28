/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.gui.components.Button
 *  net.minecraft.client.gui.components.events.GuiEventListener
 *  net.minecraft.client.gui.screens.Screen
 *  net.minecraft.client.resources.language.I18n
 *  net.minecraft.network.chat.Component
 */
package org.millenaire.client.gui;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import org.millenaire.client.gui.AbstractMillenaireScreen;
import org.millenaire.client.gui.HelpScreen;
import org.millenaire.client.gui.MillenaireScreenUtils;
import org.millenaire.client.gui.PanelRenderHelper;
import org.millenaire.client.gui.TravelBookNavHelper;
import org.millenaire.network.InfoPanelContentPayload;
import org.millenaire.village.panel.PanelLine;

public class InfoPanelScreen
extends AbstractMillenaireScreen {
    private static final int PARCHMENT_WIDTH = 256;
    private static final int PARCHMENT_HEIGHT = 220;
    private static final int LINES_PER_PAGE = InfoPanelScreen.computeMaxLinesPerPage(220);
    private static final int COLOR_DARK_BLUE = -16777046;
    private static final int COLOR_DARK_GREEN = -16733696;
    private static final int COLOR_DARK_RED = -5636096;
    private final InfoPanelContentPayload payload;
    private List<PanelLine> contentLines = List.of();
    private static final int BUTTON_AREA_HEIGHT = 50;

    public InfoPanelScreen(InfoPanelContentPayload payload) {
        super((Component)Component.translatable((String)"gui.millenaire.info_panel.title"));
        this.payload = payload;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    protected void init() {
        super.init();
        int parchX = (this.width - 256) / 2;
        int parchY = (this.height - 220) / 2;
        int contentX = parchX + 16;
        int buttonWidth = 220;
        int buttonY = parchY + 42;
        this.addRenderableWidget((GuiEventListener)Button.builder((Component)Component.translatable((String)"gui.millenaire.info_panel.help"), btn -> this.minecraft.setScreen((Screen)new HelpScreen(this))).bounds(contentX, buttonY, buttonWidth, 20).build());
        this.addRenderableWidget((GuiEventListener)Button.builder((Component)Component.translatable((String)"gui.millenaire.info_panel.travel_book"), btn -> TravelBookNavHelper.openHome(this)).bounds(contentX, buttonY += 22, buttonWidth, 20).build());
        this.buildContentLines((buttonY += 24) - parchY);
        int availableHeight = 220 - (buttonY - parchY) - 15;
        int linesPerPage = availableHeight / 11;
        this.totalPages = PanelRenderHelper.computePageCount(this.contentLines.size(), Math.max(1, linesPerPage));
        this.currentPage = 0;
        this.initPaginationButtons(parchX, parchY, 256, 220, 16);
        this.updatePaginationButtons();
    }

    private void buildContentLines(int buttonAreaHeight) {
        ArrayList<PanelLine> lines = new ArrayList<PanelLine>();
        lines.add(PanelLine.separator());
        lines.add(PanelLine.colored(I18n.get((String)"gui.millenaire.info_panel.cultures", (Object[])new Object[0]), -16777046));
        lines.add(PanelLine.empty());
        for (InfoPanelContentPayload.CultureEntry entry : this.payload.cultures()) {
            String cultureName = I18n.get((String)entry.cultureNameKey(), (Object[])new Object[0]);
            lines.add(PanelLine.text(I18n.get((String)"gui.millenaire.info_panel.culture", (Object[])new Object[]{cultureName})));
            String repLabelResolved = MillenaireScreenUtils.resolveReputationLabel(entry.reputationLabel());
            String repText = I18n.get((String)"gui.millenaire.info_panel.reputation", (Object[])new Object[]{repLabelResolved});
            int repColor = entry.reputation() > 0 ? -16733696 : (entry.reputation() < 0 ? -5636096 : -13421773);
            lines.add(PanelLine.colored(repText, repColor));
            String langLevel = InfoPanelScreen.resolveLanguageLevel(entry.languageScore());
            lines.add(PanelLine.text(I18n.get((String)"gui.millenaire.info_panel.language_level", (Object[])new Object[]{langLevel})));
            lines.add(PanelLine.empty());
        }
        this.contentLines = lines;
    }

    private static String resolveLanguageLevel(int score) {
        if (score >= 500) {
            return I18n.get((String)"gui.millenaire.info_panel.lang_fluent", (Object[])new Object[0]);
        }
        if (score >= 200) {
            return I18n.get((String)"gui.millenaire.info_panel.lang_moderate", (Object[])new Object[0]);
        }
        if (score >= 100) {
            return I18n.get((String)"gui.millenaire.info_panel.lang_beginner", (Object[])new Object[0]);
        }
        return I18n.get((String)"gui.millenaire.info_panel.lang_minimal", (Object[])new Object[0]);
    }

    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, 0x60000000);
        int parchX = (this.width - 256) / 2;
        int parchY = (this.height - 220) / 2;
        PanelRenderHelper.renderTexturedBackground(graphics, PanelRenderHelper.QUEST_TEXTURE, parchX, parchY, 256, 220);
        this.renderParchmentHeader(graphics, (Component)Component.translatable((String)"gui.millenaire.info_panel.title"), parchX, parchY, 256);
        if (!this.contentLines.isEmpty()) {
            int contentX = parchX + 16;
            int contentY = parchY + 42 + 50;
            int availableHeight = 113;
            int linesPerPage = Math.max(1, availableHeight / 11);
            int startLine = this.currentPage * linesPerPage;
            PanelRenderHelper.renderPanelLines(graphics, this.font, this.contentLines, contentX, contentY, InfoPanelScreen.textWidth(256), startLine, linesPerPage);
        }
        this.renderPageCounter(graphics, parchX, parchY, 256, 220);
        super.render(graphics, mouseX, mouseY, partialTick);
    }
}

