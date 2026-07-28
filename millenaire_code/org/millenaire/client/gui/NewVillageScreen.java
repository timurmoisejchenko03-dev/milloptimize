/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.gui.components.Button
 *  net.minecraft.client.gui.components.events.GuiEventListener
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.protocol.common.custom.CustomPacketPayload
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.ItemLike
 *  net.neoforged.neoforge.network.PacketDistributor
 */
package org.millenaire.client.gui;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.network.PacketDistributor;
import org.millenaire.client.gui.AbstractMillenaireScreen;
import org.millenaire.client.gui.IconButton;
import org.millenaire.client.gui.PanelRenderHelper;
import org.millenaire.client.gui.TravelBookNavHelper;
import org.millenaire.item.ModItems;
import org.millenaire.network.VillageCreationRequestPayload;
import org.millenaire.network.VillageTypeListPayload;

public class NewVillageScreen
extends AbstractMillenaireScreen {
    private static final int PARCHMENT_WIDTH = 204;
    private static final int PARCHMENT_HEIGHT = 220;
    private static final int TEXT_WIDTH = NewVillageScreen.textWidth(204);
    private static final int CULTURE_HEADER_HEIGHT = 14;
    private static final int BUTTON_ROW_HEIGHT = 16;
    private static final int BUTTON_WIDTH = 160;
    private static final int BUTTON_HEIGHT = 14;
    private static final int INFO_BUTTON_WIDTH = 20;
    private static final int INFO_BUTTON_GAP = 4;
    private final VillageTypeListPayload data;
    private final Map<String, List<VillageTypeListPayload.VillageTypeEntry>> groupedByCulture;
    private List<DisplayRow> displayRows = List.of();
    private final List<Button> pageButtons = new ArrayList<Button>();
    private int rowsPerPage;

    public NewVillageScreen(VillageTypeListPayload data) {
        super((Component)Component.translatable((String)"gui.millenaire.new_village.title"));
        this.data = data;
        this.groupedByCulture = new LinkedHashMap<String, List<VillageTypeListPayload.VillageTypeEntry>>();
        for (VillageTypeListPayload.VillageTypeEntry entry : data.entries()) {
            this.groupedByCulture.computeIfAbsent(entry.cultureKey(), k -> new ArrayList()).add(entry);
        }
    }

    protected void init() {
        super.init();
        ArrayList<DisplayRow> rows = new ArrayList<DisplayRow>();
        for (Map.Entry<String, List<VillageTypeListPayload.VillageTypeEntry>> group : this.groupedByCulture.entrySet()) {
            List<VillageTypeListPayload.VillageTypeEntry> entries = group.getValue();
            if (entries.isEmpty()) continue;
            rows.add(new CultureHeaderRow(entries.getFirst().cultureName(), group.getKey()));
            for (VillageTypeListPayload.VillageTypeEntry entry : entries) {
                rows.add(new VillageTypeRow(entry));
            }
        }
        this.displayRows = rows;
        int parchY = (this.height - 220) / 2;
        int contentStartY = parchY + 42;
        int contentEndY = parchY + 220 - 15;
        int availableHeight = contentEndY - contentStartY;
        this.rowsPerPage = Math.max(1, availableHeight / 16);
        this.totalPages = Math.max(1, (this.displayRows.size() + this.rowsPerPage - 1) / this.rowsPerPage);
        this.currentPage = 0;
        int parchX = (this.width - 204) / 2;
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
        int parchX = (this.width - 204) / 2;
        int parchY = (this.height - 220) / 2;
        int contentX = parchX + 16;
        int y = parchY + 42;
        int startIdx = this.currentPage * this.rowsPerPage;
        int endIdx = Math.min(startIdx + this.rowsPerPage, this.displayRows.size());
        int mainBtnWidth = 136;
        for (int i = startIdx; i < endIdx; ++i) {
            DisplayRow row = this.displayRows.get(i);
            if (row instanceof VillageTypeRow) {
                VillageTypeRow vtRow = (VillageTypeRow)row;
                VillageTypeListPayload.VillageTypeEntry entry = vtRow.entry();
                String label = entry.requiresControl() ? entry.displayName() + " " + Component.translatable((String)"gui.millenaire.new_village.controlled_suffix").getString() : entry.displayName();
                boolean enabled = !entry.requiresControl() || entry.hasControl();
                int btnX = contentX + (TEXT_WIDTH - 160) / 2;
                Button btn = Button.builder((Component)Component.literal((String)label), button -> {
                    PacketDistributor.sendToServer((CustomPacketPayload)new VillageCreationRequestPayload(this.data.targetPos(), entry.cultureKey(), entry.typeKey()), (CustomPacketPayload[])new CustomPacketPayload[0]);
                    this.onClose();
                }).bounds(btnX, y, mainBtnWidth, 14).build();
                btn.active = enabled;
                this.addRenderableWidget((GuiEventListener)btn);
                this.pageButtons.add(btn);
                ItemStack bookIcon = new ItemStack((ItemLike)ModItems.TRAVEL_BOOK.get());
                IconButton infoBtn = new IconButton(btnX + mainBtnWidth + 4, y, 20, 14, bookIcon, (Component)Component.translatable((String)"gui.millenaire.new_village.info_tooltip"), button -> TravelBookNavHelper.openVillageType(this, entry.cultureKey(), entry.typeKey()));
                this.addRenderableWidget((GuiEventListener)infoBtn);
                this.pageButtons.add(infoBtn);
            }
            y += 16;
        }
    }

    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, 0x60000000);
        int parchX = (this.width - 204) / 2;
        int parchY = (this.height - 220) / 2;
        PanelRenderHelper.renderTexturedBackground(graphics, PanelRenderHelper.PANEL_TEXTURE, parchX, parchY, 204, 220);
        int y = this.renderParchmentHeader(graphics, (Component)Component.translatable((String)"gui.millenaire.new_village.title"), parchX, parchY, 204);
        int contentX = parchX + 16;
        int startIdx = this.currentPage * this.rowsPerPage;
        int endIdx = Math.min(startIdx + this.rowsPerPage, this.displayRows.size());
        for (int i = startIdx; i < endIdx; ++i) {
            DisplayRow row = this.displayRows.get(i);
            if (row instanceof CultureHeaderRow) {
                CultureHeaderRow header = (CultureHeaderRow)row;
                graphics.drawString(this.font, (Component)Component.literal((String)header.cultureName()).withStyle(s -> s.withBold(Boolean.valueOf(true))), contentX, y + 3, -11193600);
            }
            y += 16;
        }
        this.renderPageCounter(graphics, parchX, parchY, 204, 220);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int parchX = (this.width - 204) / 2;
            int parchY = (this.height - 220) / 2;
            int contentX = parchX + 16;
            int y = parchY + 42;
            int startIdx = this.currentPage * this.rowsPerPage;
            int endIdx = Math.min(startIdx + this.rowsPerPage, this.displayRows.size());
            for (int i = startIdx; i < endIdx; ++i) {
                DisplayRow row = this.displayRows.get(i);
                if (row instanceof CultureHeaderRow) {
                    CultureHeaderRow header = (CultureHeaderRow)row;
                    if (mouseY >= (double)y && mouseY < (double)(y + 16) && mouseX >= (double)contentX && mouseX < (double)(contentX + TEXT_WIDTH)) {
                        TravelBookNavHelper.openCulture(this, header.cultureKey());
                        return true;
                    }
                }
                y += 16;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private record CultureHeaderRow(String cultureName, String cultureKey) implements DisplayRow
    {
    }

    private record VillageTypeRow(VillageTypeListPayload.VillageTypeEntry entry) implements DisplayRow
    {
    }

    private static sealed interface DisplayRow
    permits CultureHeaderRow, VillageTypeRow {
    }
}

