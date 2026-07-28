/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.gui.components.Button
 *  net.minecraft.client.gui.components.Tooltip
 *  net.minecraft.client.gui.components.events.GuiEventListener
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.chat.MutableComponent
 */
package org.millenaire.client.gui;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.millenaire.client.gui.AbstractMillenaireScreen;
import org.millenaire.client.gui.PanelRenderHelper;
import org.millenaire.network.VillageBookPayload;
import org.millenaire.village.panel.PanelContent;
import org.millenaire.village.panel.PanelLine;

public class VillageBookScreen
extends AbstractMillenaireScreen {
    private static final int PARCHMENT_WIDTH = 204;
    private static final int PARCHMENT_HEIGHT = 220;
    private static final int TABS_AND_LABEL_HEIGHT = 30;
    private static final int LINES_PER_PAGE = VillageBookScreen.computeMaxLinesPerPage(220) - 2;
    private static final int TEXT_WIDTH = VillageBookScreen.textWidth(204);
    private static final int MAP_HEIGHT = 120;
    private static final String[] SECTION_LABEL_KEYS = new String[]{"gui.millenaire.book.section.resume", "gui.millenaire.book.section.population", "gui.millenaire.book.section.constructions", "gui.millenaire.book.section.projects", "gui.millenaire.book.section.resources", "gui.millenaire.book.section.military", "gui.millenaire.book.section.chronicle"};
    private static final String[] SECTION_TAB_KEYS = new String[]{"gui.millenaire.book.tab.resume", "gui.millenaire.book.tab.population", "gui.millenaire.book.tab.constructions", "gui.millenaire.book.tab.projects", "gui.millenaire.book.tab.resources", "gui.millenaire.book.tab.military", "gui.millenaire.book.tab.chronicle"};
    private final VillageBookPayload payload;
    private int currentSection = 0;
    private List<List<PanelLine>> wrappedSections = List.of();
    private final List<Button> sectionButtons = new ArrayList<Button>();
    @Nullable
    private String mapTooltip;

    public VillageBookScreen(VillageBookPayload payload) {
        super((Component)Component.literal((String)payload.villageName()));
        this.payload = payload;
    }

    protected void init() {
        super.init();
        ArrayList<List<PanelLine>> wrapped = new ArrayList<List<PanelLine>>();
        for (PanelContent section : this.payload.sections()) {
            wrapped.add(PanelRenderHelper.wrapLines(section.lines(), this.font, TEXT_WIDTH));
        }
        this.wrappedSections = wrapped;
        this.currentSection = 0;
        this.currentPage = 0;
        int parchX = (this.width - 204) / 2;
        int parchY = (this.height - 220) / 2;
        this.sectionButtons.clear();
        int sectionCount = Math.min(this.payload.sections().size(), SECTION_LABEL_KEYS.length);
        int tabY = parchY + 16 + 20 + 4;
        int tabTotalWidth = 172;
        int tabWidth = sectionCount > 0 ? tabTotalWidth / sectionCount : tabTotalWidth;
        for (int i = 0; i < sectionCount; ++i) {
            MutableComponent shortLabel;
            int sectionIndex = i;
            MutableComponent mutableComponent = shortLabel = i < SECTION_TAB_KEYS.length ? Component.translatable((String)SECTION_TAB_KEYS[i]) : Component.literal((String)("\u00a7" + i));
            MutableComponent fullLabel = i < SECTION_LABEL_KEYS.length ? Component.translatable((String)SECTION_LABEL_KEYS[i]) : Component.translatable((String)"gui.millenaire.book.section_fallback", (Object[])new Object[]{i});
            Button btn = Button.builder((Component)shortLabel, button -> {
                this.currentSection = sectionIndex;
                this.currentPage = 0;
                this.recalcPages();
                this.updatePaginationButtons();
            }).bounds(parchX + 16 + i * tabWidth, tabY, tabWidth, 14).tooltip(Tooltip.create((Component)fullLabel)).build();
            this.sectionButtons.add(btn);
            this.addRenderableWidget((GuiEventListener)btn);
        }
        this.initPaginationButtons(parchX, parchY, 204, 220, 16);
        this.recalcPages();
        this.updatePaginationButtons();
    }

    private void recalcPages() {
        if (this.wrappedSections.isEmpty()) {
            this.totalPages = 1;
            return;
        }
        int sectionIdx = Math.min(this.currentSection, this.wrappedSections.size() - 1);
        List<PanelLine> lines = this.wrappedSections.get(sectionIdx);
        int textPages = PanelRenderHelper.computePageCount(lines.size(), LINES_PER_PAGE);
        boolean hasMap = this.currentSection == 0 && this.payload.hasMapData();
        this.totalPages = hasMap ? 1 + textPages : textPages;
    }

    @Override
    protected void updatePaginationButtons() {
        super.updatePaginationButtons();
        for (int i = 0; i < this.sectionButtons.size(); ++i) {
            this.sectionButtons.get((int)i).active = i != this.currentSection;
        }
    }

    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, 0x60000000);
        int parchX = (this.width - 204) / 2;
        int parchY = (this.height - 220) / 2;
        PanelRenderHelper.renderTexturedBackground(graphics, PanelRenderHelper.PARCHMENT_TEXTURE, parchX, parchY, 204, 220);
        int contentX = parchX + 16;
        int y = parchY + 16;
        graphics.drawCenteredString(this.font, (Component)Component.translatable((String)"gui.millenaire.book.title", (Object[])new Object[]{this.payload.villageName()}).withStyle(s -> s.withBold(Boolean.valueOf(true))), parchX + 102, y, -11193600);
        y += 20;
        y += 18;
        if (this.payload.degraded()) {
            graphics.drawCenteredString(this.font, (Component)Component.translatable((String)"gui.millenaire.book.out_of_range"), parchX + 102, y, -3381760);
            y += 12;
        }
        if (this.currentSection < SECTION_LABEL_KEYS.length) {
            graphics.drawCenteredString(this.font, (Component)Component.translatable((String)SECTION_LABEL_KEYS[this.currentSection]), parchX + 102, y, -13421773);
            y += 12;
        }
        graphics.fill(contentX, y, parchX + 204 - 16, y + 1, -5601178);
        y += 6;
        this.mapTooltip = null;
        if (!this.wrappedSections.isEmpty()) {
            boolean hasMap;
            int sectionIdx = Math.min(this.currentSection, this.wrappedSections.size() - 1);
            boolean bl = hasMap = this.currentSection == 0 && this.payload.hasMapData();
            if (hasMap && this.currentPage == 0) {
                float playerYaw = Minecraft.getInstance().player != null ? Minecraft.getInstance().player.getYRot() : 0.0f;
                this.mapTooltip = PanelRenderHelper.renderVillageMap(graphics, this.font, this.payload.mapBuildings(), this.payload.mapVillagers(), this.payload.mapPlayerX(), this.payload.mapPlayerZ(), playerYaw, this.payload.mapCenterX(), this.payload.mapCenterZ(), this.payload.mapTerrain(), this.payload.mapPaths(), contentX, y, 172, 120, mouseX, mouseY);
            } else if (this.currentSection == 0 && !this.payload.hasMapData() && this.currentPage == 0) {
                graphics.drawCenteredString(this.font, (Component)Component.translatable((String)"gui.millenaire.book.map_unavailable"), parchX + 102, y + 60 - 4, -13421773);
            }
            if (!hasMap || this.currentPage != 0) {
                int textPage = hasMap ? this.currentPage - 1 : this.currentPage;
                List<PanelLine> lines = this.wrappedSections.get(sectionIdx);
                int startLine = textPage * LINES_PER_PAGE;
                PanelRenderHelper.renderPanelLines(graphics, this.font, lines, contentX, y, TEXT_WIDTH, startLine, LINES_PER_PAGE);
            }
        }
        this.renderPageCounter(graphics, parchX, parchY, 204, 220);
        super.render(graphics, mouseX, mouseY, partialTick);
        if (this.mapTooltip != null) {
            graphics.renderTooltip(this.font, (Component)Component.literal((String)this.mapTooltip), mouseX, mouseY);
        }
    }
}

