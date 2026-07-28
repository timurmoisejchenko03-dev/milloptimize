/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.ChatFormatting
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.gui.components.Button
 *  net.minecraft.client.gui.components.events.GuiEventListener
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.chat.MutableComponent
 *  net.minecraft.network.protocol.common.custom.CustomPacketPayload
 *  net.neoforged.neoforge.network.PacketDistributor
 */
package org.millenaire.client.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.PacketDistributor;
import org.millenaire.client.gui.AbstractMillenaireScreen;
import org.millenaire.client.gui.PanelRenderHelper;
import org.millenaire.combat.raid.RaidButtonState;
import org.millenaire.network.ControlledMilitaryPayload;
import org.millenaire.network.ControlledRelationPayload;
import org.millenaire.network.RaidActionPayload;
import org.millenaire.village.VillageRelations;
import org.millenaire.village.panel.PanelLine;

public class ControlledMilitaryScreen
extends AbstractMillenaireScreen {
    private static final int PARCHMENT_WIDTH = 204;
    private static final int PARCHMENT_HEIGHT = 220;
    private static final int ROW_HEIGHT = 58;
    private static final int REL_BUTTON_WIDTH = 52;
    private static final int RAID_BUTTON_WIDTH = 90;
    private static final int BUTTON_HEIGHT = 14;
    private static final int BUTTON_GAP = 2;
    private final ControlledMilitaryPayload data;
    private final List<Button> pageButtons = new ArrayList<Button>();
    private List<PanelLine> wrappedMilitaryLines = List.of();
    private int rowsPerPage;
    private int rowCount;

    public ControlledMilitaryScreen(ControlledMilitaryPayload data) {
        super((Component)Component.translatable((String)"panel.millenaire.panel_type.controlled_military"));
        this.data = data;
    }

    private ControlledMilitaryPayload data() {
        return this.data;
    }

    protected void init() {
        super.init();
        int parchY = (this.height - 220) / 2;
        int parchX = (this.width - 204) / 2;
        int contentStartY = parchY + 42 + this.subtitleOffset();
        int contentEndY = parchY + 220 - 15;
        int availableHeight = contentEndY - contentStartY;
        this.rowsPerPage = Math.max(1, availableHeight / 58);
        this.wrappedMilitaryLines = this.wrapLines(this.data().militaryLines(), 204);
        int relationRows = this.data().relations().size();
        int relationPages = relationRows == 0 ? 0 : (relationRows + this.rowsPerPage - 1) / this.rowsPerPage;
        this.rowCount = relationRows;
        this.totalPages = Math.max(1, relationPages + 1);
        this.currentPage = 0;
        this.initPaginationButtons(parchX, parchY, 204, 220, 16);
        this.rebuildPageButtons();
        this.updatePaginationButtons();
    }

    private int subtitleOffset() {
        int n;
        if (this.data().villageName().isEmpty()) {
            n = 0;
        } else {
            Objects.requireNonNull(this.font);
            n = 9 + 4;
        }
        return n;
    }

    private int militaryPageIndex() {
        return this.totalPages - 1;
    }

    @Override
    protected void onPageChanged() {
        super.onPageChanged();
        this.rebuildPageButtons();
    }

    private void rebuildPageButtons() {
        for (Button btn : this.pageButtons) {
            this.removeWidget((GuiEventListener)btn);
        }
        this.pageButtons.clear();
        if (this.currentPage == this.militaryPageIndex()) {
            return;
        }
        int parchX = (this.width - 204) / 2;
        int parchY = (this.height - 220) / 2;
        int contentX = parchX + 16;
        int rowY = parchY + 42 + this.subtitleOffset();
        int startIdx = this.currentPage * this.rowsPerPage;
        int endIdx = Math.min(startIdx + this.rowsPerPage, this.rowCount);
        String currentRaidTarget = this.data().currentRaidTargetVillageId().isEmpty() ? null : this.data().currentRaidTargetVillageId();
        for (int i = startIdx; i < endIdx; ++i) {
            ControlledMilitaryPayload.RelationEntry re = this.data().relations().get(i);
            Objects.requireNonNull(this.font);
            int btnRow1Y = rowY + 2 * (9 + 1) + 2;
            int gx = contentX;
            this.addPageButton(Button.builder((Component)Component.translatable((String)"gui.millenaire.controlled_military.relgood"), b -> PacketDistributor.sendToServer((CustomPacketPayload)new ControlledRelationPayload(this.data().villageUuid(), re.villageId(), 100), (CustomPacketPayload[])new CustomPacketPayload[0])).bounds(gx, btnRow1Y, 52, 14).build());
            this.addPageButton(Button.builder((Component)Component.translatable((String)"gui.millenaire.controlled_military.relneutral"), b -> PacketDistributor.sendToServer((CustomPacketPayload)new ControlledRelationPayload(this.data().villageUuid(), re.villageId(), 0), (CustomPacketPayload[])new CustomPacketPayload[0])).bounds(gx += 54, btnRow1Y, 52, 14).build());
            this.addPageButton(Button.builder((Component)Component.translatable((String)"gui.millenaire.controlled_military.relbad"), b -> PacketDistributor.sendToServer((CustomPacketPayload)new ControlledRelationPayload(this.data().villageUuid(), re.villageId(), -100), (CustomPacketPayload[])new CustomPacketPayload[0])).bounds(gx += 54, btnRow1Y, 52, 14).build());
            int btnRow2Y = btnRow1Y + 14 + 2;
            RaidButtonState state = RaidButtonState.compute(currentRaidTarget, this.data().raidInProgress(), re.villageId());
            switch (state) {
                case PLAN: 
                case PLANNED_OTHER: {
                    this.addPageButton(Button.builder((Component)Component.translatable((String)"gui.millenaire.controlled_military.raid"), b -> PacketDistributor.sendToServer((CustomPacketPayload)new RaidActionPayload(this.data().villageUuid(), re.villageId(), 0, true), (CustomPacketPayload[])new CustomPacketPayload[0])).bounds(contentX, btnRow2Y, 90, 14).build());
                    break;
                }
                case CANCEL: {
                    this.addPageButton(Button.builder((Component)Component.translatable((String)"gui.millenaire.controlled_military.raidcancel"), b -> PacketDistributor.sendToServer((CustomPacketPayload)new RaidActionPayload(this.data().villageUuid(), re.villageId(), 1, true), (CustomPacketPayload[])new CustomPacketPayload[0])).bounds(contentX, btnRow2Y, 90, 14).build());
                    break;
                }
            }
            rowY += 58;
        }
    }

    private void addPageButton(Button btn) {
        this.addRenderableWidget((GuiEventListener)btn);
        this.pageButtons.add(btn);
    }

    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, 0x60000000);
        int parchX = (this.width - 204) / 2;
        int parchY = (this.height - 220) / 2;
        PanelRenderHelper.renderTexturedBackground(graphics, PanelRenderHelper.PANEL_TEXTURE, parchX, parchY, 204, 220);
        int y = this.renderParchmentHeader(graphics, this.getTitle(), parchX, parchY, 204);
        if (!this.data().villageName().isEmpty()) {
            graphics.drawString(this.font, (Component)Component.literal((String)this.data().villageName()), parchX + 16, y, 5914656, false);
            Objects.requireNonNull(this.font);
            y += 9 + 4;
        }
        if (this.currentPage == this.militaryPageIndex()) {
            this.renderMilitaryPage(graphics, parchX, parchY, y);
        } else if (this.rowCount == 0) {
            graphics.drawString(this.font, (Component)Component.translatable((String)"gui.millenaire.chief.diplomacy_none"), parchX + 16, y, 5914656, false);
        } else {
            this.renderRelationRows(graphics, parchX, y);
        }
        this.renderPageCounter(graphics, parchX, parchY, 204, 220);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderRelationRows(GuiGraphics graphics, int parchX, int startY) {
        int contentX = parchX + 16;
        int rowY = startY;
        int startIdx = this.currentPage * this.rowsPerPage;
        int endIdx = Math.min(startIdx + this.rowsPerPage, this.rowCount);
        String currentRaidTarget = this.data().currentRaidTargetVillageId().isEmpty() ? null : this.data().currentRaidTargetVillageId();
        for (int i = startIdx; i < endIdx; ++i) {
            ControlledMilitaryPayload.RelationEntry re = this.data().relations().get(i);
            graphics.drawString(this.font, (Component)Component.literal((String)(re.villageName() + " (" + re.cultureName() + ")")), contentX, rowY, 4202512, false);
            ChatFormatting cf = VillageRelations.getRelationColor(re.relation());
            int relColor = cf.getColor() != null ? 0xFF000000 | cf.getColor() : 6963232;
            String relLabel = Component.translatable((String)re.relationLabel()).getString();
            MutableComponent mutableComponent = Component.literal((String)(relLabel + " (" + re.relation() + ")"));
            Objects.requireNonNull(this.font);
            graphics.drawString(this.font, (Component)mutableComponent, contentX, rowY + 9 + 1, relColor, false);
            RaidButtonState state = RaidButtonState.compute(currentRaidTarget, this.data().raidInProgress(), re.villageId());
            Objects.requireNonNull(this.font);
            int btnRow2Y = rowY + 2 * (9 + 1) + 2 + 14 + 2;
            switch (state) {
                case CANCEL: {
                    this.drawStatus(graphics, contentX, btnRow2Y, "gui.millenaire.chief.raid_planned", 0xFF5555);
                    break;
                }
                case PLANNED_OTHER: {
                    this.drawStatus(graphics, contentX, btnRow2Y, "gui.millenaire.chief.other_raid_planned", 0xFF5555);
                    break;
                }
                case IN_PROGRESS_HERE: {
                    this.drawStatus(graphics, contentX, btnRow2Y, "gui.millenaire.chief.raid_in_progress", 0xAA0000);
                    break;
                }
                case IN_PROGRESS_OTHER: {
                    this.drawStatus(graphics, contentX, btnRow2Y, "gui.millenaire.chief.other_raid_in_progress", 0xAA0000);
                    break;
                }
            }
            rowY += 58;
        }
    }

    private void drawStatus(GuiGraphics graphics, int x, int y, String key, int color) {
        graphics.drawString(this.font, (Component)Component.translatable((String)key), x + 90 + 6, y + 3, 0xFF000000 | color, false);
    }

    private void renderMilitaryPage(GuiGraphics graphics, int parchX, int parchY, int startY) {
        graphics.drawString(this.font, (Component)Component.translatable((String)"gui.millenaire.chief.military_title"), parchX + 16, startY, -11193600, false);
        Objects.requireNonNull(this.font);
        int linesStartY = startY + 9 + 4;
        int contentEndY = parchY + 220 - 15;
        int maxLines = Math.max(0, (contentEndY - linesStartY) / 11);
        PanelRenderHelper.renderPanelLines(graphics, this.font, this.wrappedMilitaryLines, parchX + 16, linesStartY, ControlledMilitaryScreen.textWidth(204), 0, maxLines);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

