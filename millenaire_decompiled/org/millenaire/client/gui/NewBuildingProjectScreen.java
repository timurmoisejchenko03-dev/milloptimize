/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.gui.components.Button
 *  net.minecraft.client.gui.components.events.GuiEventListener
 *  net.minecraft.network.chat.Component
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
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.PacketDistributor;
import org.millenaire.client.gui.AbstractMillenaireScreen;
import org.millenaire.client.gui.PanelRenderHelper;
import org.millenaire.network.BuildingProjectListPayload;
import org.millenaire.network.BuildingProjectRequestPayload;

public class NewBuildingProjectScreen
extends AbstractMillenaireScreen {
    private static final int PARCHMENT_WIDTH = 204;
    private static final int PARCHMENT_HEIGHT = 220;
    private static final int BUTTON_ROW_HEIGHT = 18;
    private static final int BUTTON_WIDTH = 170;
    private static final int BUTTON_HEIGHT = 16;
    private final BuildingProjectListPayload data;
    private final List<Button> pageButtons = new ArrayList<Button>();
    private int rowsPerPage;

    public NewBuildingProjectScreen(BuildingProjectListPayload data) {
        super((Component)Component.translatable((String)"gui.millenaire.building_project.title"));
        this.data = data;
    }

    protected void init() {
        super.init();
        int parchY = (this.height - 220) / 2;
        int parchX = (this.width - 204) / 2;
        int contentStartY = parchY + 42;
        int contentEndY = parchY + 220 - 15;
        int availableHeight = contentEndY - contentStartY;
        this.rowsPerPage = Math.max(1, availableHeight / 18);
        this.totalPages = this.data.entries().isEmpty() ? 1 : (this.data.entries().size() + this.rowsPerPage - 1) / this.rowsPerPage;
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
        for (Button btn : this.pageButtons) {
            this.removeWidget((GuiEventListener)btn);
        }
        this.pageButtons.clear();
        if (this.data.entries().isEmpty()) {
            return;
        }
        int parchX = (this.width - 204) / 2;
        int parchY = (this.height - 220) / 2;
        int contentX = parchX + 16;
        int y = parchY + 42;
        int startIdx = this.currentPage * this.rowsPerPage;
        int endIdx = Math.min(startIdx + this.rowsPerPage, this.data.entries().size());
        for (int i = startIdx; i < endIdx; ++i) {
            BuildingProjectListPayload.BuildingEntry entry = this.data.entries().get(i);
            Button btn = Button.builder((Component)Component.literal((String)entry.displayName()), button -> {
                PacketDistributor.sendToServer((CustomPacketPayload)new BuildingProjectRequestPayload(this.data.villageUuid(), entry.planSetId(), this.data.clickedPos()), (CustomPacketPayload[])new CustomPacketPayload[0]);
                this.onClose();
            }).bounds(contentX + (NewBuildingProjectScreen.textWidth(204) - 170) / 2, y, 170, 16).build();
            this.addRenderableWidget((GuiEventListener)btn);
            this.pageButtons.add(btn);
            y += 18;
        }
    }

    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, 0x60000000);
        int parchX = (this.width - 204) / 2;
        int parchY = (this.height - 220) / 2;
        PanelRenderHelper.renderParchmentBackground(graphics, parchX, parchY, 204, 220);
        int y = this.renderParchmentHeader(graphics, this.getTitle(), parchX, parchY, 204);
        if (!this.data.villageName().isEmpty()) {
            int textX = parchX + 16;
            graphics.drawString(this.font, (Component)Component.literal((String)this.data.villageName()), textX, y, 5914656, false);
            Objects.requireNonNull(this.font);
            y += 9 + 2;
        }
        if (this.data.entries().isEmpty()) {
            graphics.drawString(this.font, (Component)Component.translatable((String)"gui.millenaire.building_project.empty"), parchX + 16, y, 5914656, false);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

