/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.gui.components.Button
 *  net.minecraft.client.gui.components.events.GuiEventListener
 *  net.minecraft.network.chat.Component
 */
package org.millenaire.client.gui;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;
import org.millenaire.client.gui.AbstractMillenaireScreen;
import org.millenaire.client.gui.PanelRenderHelper;
import org.millenaire.client.gui.TravelBookNavHelper;
import org.millenaire.network.PanelContentPayload;
import org.millenaire.village.panel.PanelContent;
import org.millenaire.village.panel.PanelLine;

public class VillagePanelScreen
extends AbstractMillenaireScreen {
    private static final int PARCHMENT_WIDTH = 204;
    private static final int PARCHMENT_HEIGHT = 220;
    private static final int LINES_PER_PAGE = VillagePanelScreen.computeMaxLinesPerPage(220);
    private static final int TEXT_WIDTH = VillagePanelScreen.textWidth(204);
    private final PanelContent content;
    @Nullable
    private final PanelContentPayload mapPayload;
    private List<PanelLine> wrappedLines = List.of();
    private boolean showChunkMap = false;
    @Nullable
    private Button chunkMapToggle;

    public VillagePanelScreen(PanelContent content) {
        this(content, null);
    }

    public VillagePanelScreen(PanelContent content, @Nullable PanelContentPayload mapPayload) {
        super(VillagePanelScreen.resolveTitleComponent(content));
        this.content = content;
        this.mapPayload = mapPayload;
    }

    private static Component resolveTitleComponent(PanelContent content) {
        if (content.titleTranslatable()) {
            return content.titleArgs() != null ? Component.translatable((String)content.title(), (Object[])content.titleArgs()) : Component.translatable((String)content.title());
        }
        return Component.literal((String)content.title());
    }

    protected void init() {
        boolean hasChunks;
        super.init();
        this.wrappedLines = PanelRenderHelper.wrapLines(this.content.lines(), this.font, TEXT_WIDTH);
        boolean hasMap = this.mapPayload != null && this.mapPayload.hasMapData();
        int textPages = PanelRenderHelper.computePageCount(this.wrappedLines.size(), LINES_PER_PAGE);
        this.totalPages = hasMap ? 1 + textPages : textPages;
        this.currentPage = 0;
        int parchX = (this.width - 204) / 2;
        int parchY = (this.height - 220) / 2;
        this.initPaginationButtons(parchX, parchY, 204, 220, 16);
        this.addTravelBookButton(parchX, parchY, 204, button -> TravelBookNavHelper.openHome(this));
        this.addRenderableWidget((GuiEventListener)Button.builder((Component)Component.translatable((String)"gui.millenaire.common.help_btn"), button -> TravelBookNavHelper.openHelp(this)).bounds(parchX, parchY, 16, 16).build());
        boolean bl = hasChunks = this.mapPayload != null && !this.mapPayload.forceLoadedChunks().isEmpty();
        if (hasMap && hasChunks) {
            int toggleWidth = 60;
            int toggleX = parchX + 16;
            int toggleY = parchY + 220 - 15 - 16;
            this.chunkMapToggle = Button.builder((Component)Component.translatable((String)"gui.millenaire.chunk_map_toggle"), btn -> {
                this.showChunkMap = !this.showChunkMap;
                btn.setMessage((Component)Component.translatable((String)(this.showChunkMap ? "gui.millenaire.village_map_toggle" : "gui.millenaire.chunk_map_toggle")));
            }).bounds(toggleX, toggleY, toggleWidth, 14).build();
            this.addRenderableWidget((GuiEventListener)this.chunkMapToggle);
        }
        this.updatePaginationButtons();
    }

    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        boolean hasMap;
        graphics.fill(0, 0, this.width, this.height, 0x60000000);
        int parchX = (this.width - 204) / 2;
        int parchY = (this.height - 220) / 2;
        PanelRenderHelper.renderTexturedBackground(graphics, PanelRenderHelper.PANEL_TEXTURE, parchX, parchY, 204, 220);
        int contentX = parchX + 16;
        int y = this.renderParchmentHeader(graphics, VillagePanelScreen.resolveTitleComponent(this.content), parchX, parchY, 204);
        boolean bl = hasMap = this.mapPayload != null && this.mapPayload.hasMapData();
        if (hasMap && this.currentPage == 0) {
            int mapAreaWidth = 172;
            int mapAreaHeight = 124;
            if (this.showChunkMap && !this.mapPayload.forceLoadedChunks().isEmpty()) {
                String tooltip = PanelRenderHelper.renderChunkMap(graphics, this.font, this.mapPayload.forceLoadedChunks(), this.mapPayload.mapBuildings(), this.mapPayload.mapCenterX(), this.mapPayload.mapCenterZ(), contentX, y, mapAreaWidth, mapAreaHeight, mouseX, mouseY);
                if (tooltip != null) {
                    graphics.renderTooltip(this.font, (Component)Component.literal((String)tooltip), mouseX, mouseY);
                }
            } else {
                float playerYaw = Minecraft.getInstance().player != null ? Minecraft.getInstance().player.getYRot() : 0.0f;
                String tooltip = PanelRenderHelper.renderVillageMap(graphics, this.font, this.mapPayload.mapBuildings(), this.mapPayload.mapVillagers(), this.mapPayload.mapPlayerX(), this.mapPayload.mapPlayerZ(), playerYaw, this.mapPayload.mapCenterX(), this.mapPayload.mapCenterZ(), this.mapPayload.mapTerrain(), this.mapPayload.mapPaths(), contentX, y, mapAreaWidth, mapAreaHeight, mouseX, mouseY);
                if (tooltip != null) {
                    graphics.renderTooltip(this.font, (Component)Component.literal((String)tooltip), mouseX, mouseY);
                }
            }
        } else {
            int textPage = hasMap ? this.currentPage - 1 : this.currentPage;
            int startLine = textPage * LINES_PER_PAGE;
            PanelRenderHelper.renderPanelLines(graphics, this.font, this.wrappedLines, contentX, y, TEXT_WIDTH, startLine, LINES_PER_PAGE, mouseX, mouseY);
        }
        this.renderPageCounter(graphics, parchX, parchY, 204, 220);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    protected void onPageChanged() {
        super.onPageChanged();
        if (this.chunkMapToggle != null) {
            boolean hasMap = this.mapPayload != null && this.mapPayload.hasMapData();
            this.chunkMapToggle.visible = hasMap && this.currentPage == 0;
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && !this.wrappedLines.isEmpty()) {
            int textPage;
            int startLine;
            int parchY;
            int contentY;
            int parchX;
            int contentX;
            PanelLine.PanelNavTarget target;
            boolean isMapPage;
            boolean hasMap = this.mapPayload != null && this.mapPayload.hasMapData();
            boolean bl = isMapPage = hasMap && this.currentPage == 0;
            if (!isMapPage && (target = PanelRenderHelper.getClickedNavTarget(this.wrappedLines, contentX = (parchX = (this.width - 204) / 2) + 16, contentY = (parchY = (this.height - 220) / 2) + 42, TEXT_WIDTH, startLine = (textPage = hasMap ? this.currentPage - 1 : this.currentPage) * LINES_PER_PAGE, LINES_PER_PAGE, mouseX, mouseY)) != null) {
                TravelBookNavHelper.openFromNavTarget(this, target);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}

