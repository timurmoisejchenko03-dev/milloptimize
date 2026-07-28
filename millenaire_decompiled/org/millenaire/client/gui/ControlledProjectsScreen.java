/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
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
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.PacketDistributor;
import org.millenaire.client.gui.AbstractMillenaireScreen;
import org.millenaire.client.gui.PanelRenderHelper;
import org.millenaire.network.BuildingForgetRequestPayload;
import org.millenaire.network.BuildingProjectCancelRequestPayload;
import org.millenaire.network.BuildingUpgradeToggleRequestPayload;
import org.millenaire.network.ControlledProjectsPayload;

public class ControlledProjectsScreen
extends AbstractMillenaireScreen {
    private static final int PARCHMENT_WIDTH = 204;
    private static final int PARCHMENT_HEIGHT = 220;
    private static final int ROW_HEIGHT = 44;
    private static final int TOGGLE_WIDTH = 80;
    private static final int TOGGLE_HEIGHT = 14;
    private final ControlledProjectsPayload data;
    private final List<Button> pageButtons = new ArrayList<Button>();
    private int rowsPerPage;
    private int rowCount;

    public ControlledProjectsScreen(ControlledProjectsPayload data) {
        super((Component)Component.translatable((String)"gui.millenaire.controlled_projects.title"));
        this.data = data;
    }

    protected void init() {
        int n;
        super.init();
        int parchY = (this.height - 220) / 2;
        int parchX = (this.width - 204) / 2;
        if (this.data.villageName().isEmpty()) {
            n = 0;
        } else {
            Objects.requireNonNull(this.font);
            n = 9 + 4;
        }
        int contentStartY = parchY + 42 + n;
        int contentEndY = parchY + 220 - 15;
        int availableHeight = contentEndY - contentStartY;
        this.rowsPerPage = Math.max(1, availableHeight / 44);
        this.rowCount = this.data.entries().size() + (this.data.pendingPlanName().isEmpty() ? 0 : 1);
        this.totalPages = this.rowCount == 0 ? 1 : (this.rowCount + this.rowsPerPage - 1) / this.rowsPerPage;
        this.currentPage = 0;
        this.initPaginationButtons(parchX, parchY, 204, 220, 16);
        this.rebuildPageButtons();
        this.updatePaginationButtons();
    }

    @Override
    protected void onPageChanged() {
        super.onPageChanged();
        this.rebuildPageButtons();
    }

    private void rebuildPageButtons() {
        int n;
        for (Button btn : this.pageButtons) {
            this.removeWidget((GuiEventListener)btn);
        }
        this.pageButtons.clear();
        int parchX = (this.width - 204) / 2;
        int parchY = (this.height - 220) / 2;
        int contentX = parchX + 16;
        if (this.data.villageName().isEmpty()) {
            n = 0;
        } else {
            Objects.requireNonNull(this.font);
            n = 9 + 4;
        }
        int subtitleOffset = n;
        int rowY = parchY + 42 + subtitleOffset;
        int startIdx = this.currentPage * this.rowsPerPage;
        int endIdx = Math.min(startIdx + this.rowsPerPage, this.rowCount);
        boolean hasPending = !this.data.pendingPlanName().isEmpty();
        for (int i = startIdx; i < endIdx; ++i) {
            Objects.requireNonNull(this.font);
            int btnY = rowY + 2 * (9 + 1) + 2;
            int btnX = contentX + ControlledProjectsScreen.textWidth(204) - 80;
            if (hasPending && i == 0) {
                Button cancelBtn = Button.builder((Component)Component.translatable((String)"gui.millenaire.controlled_projects.cancel"), b -> PacketDistributor.sendToServer((CustomPacketPayload)new BuildingProjectCancelRequestPayload(this.data.villageUuid()), (CustomPacketPayload[])new CustomPacketPayload[0])).bounds(btnX, btnY, 80, 14).build();
                this.addRenderableWidget((GuiEventListener)cancelBtn);
                this.pageButtons.add(cancelBtn);
            } else {
                int entryIdx = hasPending ? i - 1 : i;
                ControlledProjectsPayload.ProjectEntry entry = this.data.entries().get(entryIdx);
                if (entry.maxLevel() > 1 && entry.currentLevel() < entry.maxLevel() - 1) {
                    String labelKey = entry.upgradesAllowed() ? "gui.millenaire.controlled_projects.forbid" : "gui.millenaire.controlled_projects.allow";
                    boolean newAllow = !entry.upgradesAllowed();
                    Button toggle = Button.builder((Component)Component.translatable((String)labelKey), b -> PacketDistributor.sendToServer((CustomPacketPayload)new BuildingUpgradeToggleRequestPayload(this.data.villageUuid(), entry.buildingId(), newAllow), (CustomPacketPayload[])new CustomPacketPayload[0])).bounds(btnX, btnY, 80, 14).build();
                    this.addRenderableWidget((GuiEventListener)toggle);
                    this.pageButtons.add(toggle);
                }
                if (!entry.isTownHall()) {
                    Button forget = Button.builder((Component)Component.translatable((String)"gui.millenaire.controlled_projects.forget"), b -> PacketDistributor.sendToServer((CustomPacketPayload)new BuildingForgetRequestPayload(this.data.villageUuid(), entry.buildingId()), (CustomPacketPayload[])new CustomPacketPayload[0])).bounds(btnX, btnY + 14 + 2, 80, 14).build();
                    this.addRenderableWidget((GuiEventListener)forget);
                    this.pageButtons.add(forget);
                }
            }
            rowY += 44;
        }
    }

    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, 0x60000000);
        int parchX = (this.width - 204) / 2;
        int parchY = (this.height - 220) / 2;
        PanelRenderHelper.renderTexturedBackground(graphics, PanelRenderHelper.PANEL_TEXTURE, parchX, parchY, 204, 220);
        int y = this.renderParchmentHeader(graphics, this.getTitle(), parchX, parchY, 204);
        if (!this.data.villageName().isEmpty()) {
            int textX = parchX + 16;
            graphics.drawString(this.font, (Component)Component.literal((String)this.data.villageName()), textX, y, 5914656, false);
            Objects.requireNonNull(this.font);
            y += 9 + 4;
        }
        if (this.rowCount == 0) {
            graphics.drawString(this.font, (Component)Component.translatable((String)"gui.millenaire.controlled_projects.empty"), parchX + 16, y, 5914656, false);
        } else {
            this.renderRows(graphics, parchX, y);
        }
        this.renderPageCounter(graphics, parchX, parchY, 204, 220);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderRows(GuiGraphics graphics, int parchX, int startY) {
        int contentX = parchX + 16;
        int rowY = startY;
        int startIdx = this.currentPage * this.rowsPerPage;
        int endIdx = Math.min(startIdx + this.rowsPerPage, this.rowCount);
        boolean hasPending = !this.data.pendingPlanName().isEmpty();
        for (int i = startIdx; i < endIdx; ++i) {
            if (hasPending && i == 0) {
                graphics.drawString(this.font, (Component)Component.translatable((String)"gui.millenaire.controlled_projects.pending_label"), contentX, rowY, 6955040, false);
                MutableComponent mutableComponent = Component.literal((String)this.data.pendingPlanName());
                Objects.requireNonNull(this.font);
                graphics.drawString(this.font, (Component)mutableComponent, contentX, rowY + 9 + 1, 4202512, false);
            } else {
                int entryIdx = hasPending ? i - 1 : i;
                ControlledProjectsPayload.ProjectEntry entry = this.data.entries().get(entryIdx);
                graphics.drawString(this.font, (Component)Component.literal((String)entry.displayName()), contentX, rowY, 4202512, false);
                String levelStr = Component.translatable((String)"gui.millenaire.controlled_projects.level", (Object[])new Object[]{String.valueOf(entry.currentLevel() + 1), String.valueOf(entry.maxLevel())}).getString();
                String suffix = entry.distanceLabel().isEmpty() ? levelStr : levelStr + " \u2014 " + entry.distanceLabel();
                MutableComponent mutableComponent = Component.literal((String)suffix);
                Objects.requireNonNull(this.font);
                graphics.drawString(this.font, (Component)mutableComponent, contentX, rowY + 9 + 1, 6963232, false);
            }
            rowY += 44;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

