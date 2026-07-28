/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.network.chat.Component
 *  net.minecraft.world.item.Item
 *  org.slf4j.Logger
 */
package org.millenaire.client.gui;

import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import org.millenaire.DisplayUtils;
import org.millenaire.client.gui.AbstractMillenaireScreen;
import org.millenaire.client.gui.MillenaireScreenUtils;
import org.millenaire.client.gui.PanelRenderHelper;
import org.millenaire.client.gui.TravelBookNavHelper;
import org.millenaire.item.ItemHelper;
import org.millenaire.language.DisplayNameResolver;
import org.millenaire.network.VillagerInfoPayload;
import org.millenaire.village.panel.PanelLine;
import org.slf4j.Logger;

public class VillagerInfoScreen
extends AbstractMillenaireScreen {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int PARCHMENT_WIDTH = 256;
    private static final int PARCHMENT_HEIGHT = 200;
    private static final int LINES_PER_PAGE = VillagerInfoScreen.computeMaxLinesPerPage(200);
    private static final int PREVIEW_LINES_COST = 5;
    private static final int TEXT_WIDTH = VillagerInfoScreen.textWidth(256);
    private static final int LANG_BEGINNER = 100;
    private static final int LANG_MODERATE = 200;
    private static final int LANG_FLUENT = 500;
    private static final int COLOR_HEALTH_BG = -11197918;
    private static final int COLOR_HEALTH_FG = 0xFF5555;
    private final VillagerInfoPayload data;
    private List<PanelLine> allLines = List.of();

    public VillagerInfoScreen(VillagerInfoPayload data) {
        super((Component)Component.literal((String)data.displayName()));
        this.data = data;
    }

    protected void init() {
        super.init();
        List<PanelLine> infoLines = this.buildInfoLines();
        List<PanelLine> overflowLines = this.buildOverflowLines();
        ArrayList<PanelLine> combined = new ArrayList<PanelLine>(PanelRenderHelper.wrapLines(infoLines, this.font, TEXT_WIDTH));
        combined.addAll(PanelRenderHelper.wrapLines(overflowLines, this.font, TEXT_WIDTH));
        this.allLines = combined;
        int page0Lines = LINES_PER_PAGE - 5;
        int totalOverflow = Math.max(0, this.allLines.size() - page0Lines);
        this.totalPages = 1 + (totalOverflow > 0 ? PanelRenderHelper.computePageCount(totalOverflow, LINES_PER_PAGE) : 0);
        this.currentPage = 0;
        int parchX = (this.width - 256) / 2;
        int parchY = (this.height - 200) / 2;
        this.initPaginationButtons(parchX, parchY, 256, 200, 16);
        if (this.data.travelBookVisible()) {
            this.addTravelBookButton(parchX, parchY, 256, button -> TravelBookNavHelper.openVillagerDetail(this, this.data.cultureKey(), this.data.villagerTypeKey()));
        }
        this.updatePaginationButtons();
    }

    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, 0x60000000);
        int parchX = (this.width - 256) / 2;
        int parchY = (this.height - 200) / 2;
        PanelRenderHelper.renderTexturedBackground(graphics, PanelRenderHelper.VILLAGE_CHIEF_TEXTURE, parchX, parchY, 256, 200);
        if (this.currentPage == 0) {
            this.renderPage0(graphics, parchX, parchY, mouseX, mouseY);
        } else {
            this.renderPageN(graphics, parchX, parchY);
        }
        this.renderPageCounter(graphics, parchX, parchY, 256, 200);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderPage0(GuiGraphics graphics, int parchX, int parchY, int mouseX, int mouseY) {
        int previewEndY = MillenaireScreenUtils.renderVillagerPreview(graphics, parchX, parchY, 256, 16, parchY + 16, mouseX, mouseY, this.data.entityId());
        int y = Math.max(previewEndY, parchY + 16 + 55);
        graphics.drawCenteredString(this.font, (Component)Component.literal((String)this.resolveDisplayName()).withStyle(s -> s.withBold(Boolean.valueOf(true))), parchX + 128, y, -11193600);
        graphics.fill(parchX + 16, y += 20, parchX + 256 - 16, y + 1, -5601178);
        int page0Lines = LINES_PER_PAGE - 5;
        int healthLineY = (y += 6) + 33;
        PanelRenderHelper.renderPanelLines(graphics, this.font, this.allLines, parchX + 16, y, TEXT_WIDTH, 0, page0Lines);
        this.renderHealthBar(graphics, parchX + 16, healthLineY);
    }

    private void renderPageN(GuiGraphics graphics, int parchX, int parchY) {
        int y = this.renderParchmentHeader(graphics, (Component)Component.literal((String)this.resolveDisplayName()), parchX, parchY, 256);
        int page0Lines = LINES_PER_PAGE - 5;
        int startLine = page0Lines + (this.currentPage - 1) * LINES_PER_PAGE;
        PanelRenderHelper.renderPanelLines(graphics, this.font, this.allLines, parchX + 16, y, TEXT_WIDTH, startLine, LINES_PER_PAGE);
    }

    private List<PanelLine> buildInfoLines() {
        ArrayList<PanelLine> lines = new ArrayList<PanelLine>();
        lines.add(PanelLine.text(Component.translatable((String)"gui.millenaire.village").getString() + " : " + this.data.villageName()));
        lines.add(PanelLine.text(Component.translatable((String)"gui.millenaire.culture").getString() + " : " + this.data.cultureName()));
        String resolvedGoal = DisplayUtils.resolveGoalLabel(this.data.goalLabel());
        lines.add(PanelLine.text(Component.translatable((String)"gui.millenaire.activity").getString() + " : " + resolvedGoal));
        lines.add(PanelLine.text(Component.translatable((String)"gui.millenaire.health").getString() + String.format(" : %.0f / %.0f", Float.valueOf(this.data.health()), Float.valueOf(this.data.maxHealth()))));
        lines.add(PanelLine.text(""));
        String repCode = this.data.reputation() > 0 ? "a" : (this.data.reputation() < 0 ? "c" : "e");
        String repLabel = MillenaireScreenUtils.resolveReputationLabel(this.data.reputationLabel());
        lines.add(PanelLine.text("\u00a7" + repCode + Component.translatable((String)"gui.millenaire.reputation").getString() + " : " + this.data.reputation() + " (" + repLabel + ")\u00a7r"));
        lines.add(PanelLine.text("\u00a79" + Component.translatable((String)"gui.millenaire.language").getString() + " : " + this.getLangLevelString() + "\u00a7r"));
        return lines;
    }

    private List<PanelLine> buildOverflowLines() {
        ArrayList<PanelLine> lines = new ArrayList<PanelLine>();
        if (!this.data.inventory().isEmpty()) {
            lines.add(PanelLine.separator());
            lines.add(PanelLine.text(Component.translatable((String)"gui.millenaire.inventory").getString() + " :"));
            for (VillagerInfoPayload.InvEntry entry : this.data.inventory()) {
                String itemName = VillagerInfoScreen.resolveItemName(entry.itemId());
                lines.add(PanelLine.withIcon(itemName, "x" + entry.count(), entry.itemId()));
            }
        }
        if (!this.data.possibleGoals().isEmpty()) {
            lines.add(PanelLine.separator());
            lines.add(PanelLine.text(Component.translatable((String)"gui.millenaire.possible_goals").getString() + " :"));
            for (String goalKey : this.data.possibleGoals()) {
                lines.add(PanelLine.text("- " + Component.translatable((String)goalKey).getString()));
            }
        }
        return lines;
    }

    private void renderHealthBar(GuiGraphics graphics, int x, int healthTextY) {
        int barY = healthTextY + 10;
        int barWidth = TEXT_WIDTH;
        int barHeight = 4;
        graphics.fill(x, barY, x + barWidth, barY + barHeight, -11197918);
        float ratio = this.data.maxHealth() > 0.0f ? this.data.health() / this.data.maxHealth() : 0.0f;
        int filledWidth = (int)((float)barWidth * ratio);
        if (filledWidth > 0) {
            graphics.fill(x, barY, x + filledWidth, barY + barHeight, -43691);
        }
    }

    private String resolveDisplayName() {
        String nameStr = this.data.displayName().contains(".") ? Component.translatable((String)this.data.displayName()).getString() : this.data.displayName();
        String nativeRole = this.data.nativeOccupation();
        if (nativeRole == null || nativeRole.isEmpty()) {
            String string = nativeRole = this.data.roleName().startsWith("role.") ? Component.translatable((String)this.data.roleName()).getString() : this.data.roleName();
        }
        if (this.data.languageScore() >= 200) {
            String translatedRole;
            String string = translatedRole = this.data.roleName().startsWith("role.") ? Component.translatable((String)this.data.roleName()).getString() : this.data.roleName();
            if (!DisplayNameResolver.equivalent(nativeRole, translatedRole)) {
                return nameStr + ", " + nativeRole + " (" + translatedRole + ")";
            }
        }
        return nameStr + ", " + nativeRole;
    }

    private String getLangLevelString() {
        int score = this.data.languageScore();
        if (score >= 500) {
            return Component.translatable((String)"gui.millenaire.lang.fluent").getString();
        }
        if (score >= 200) {
            return Component.translatable((String)"gui.millenaire.lang.moderate").getString();
        }
        if (score >= 100) {
            return Component.translatable((String)"gui.millenaire.lang.beginner").getString();
        }
        return Component.translatable((String)"gui.millenaire.lang.minimal").getString();
    }

    private static String resolveItemName(String itemId) {
        try {
            Item item = ItemHelper.resolve(itemId);
            return item != null ? Component.translatable((String)item.getDescriptionId()).getString() : itemId;
        }
        catch (Exception e) {
            LOGGER.debug("Failed to resolve item name for {}", (Object)itemId, (Object)e);
            return itemId;
        }
    }
}

