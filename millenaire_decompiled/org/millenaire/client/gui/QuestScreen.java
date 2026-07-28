/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  javax.annotation.Nullable
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.gui.components.Button
 *  net.minecraft.client.gui.components.events.GuiEventListener
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.chat.MutableComponent
 *  net.minecraft.network.protocol.common.custom.CustomPacketPayload
 *  net.minecraft.world.entity.Entity
 *  net.neoforged.neoforge.network.PacketDistributor
 *  org.slf4j.Logger
 */
package org.millenaire.client.gui;

import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.PacketDistributor;
import org.millenaire.client.ClientQuestCache;
import org.millenaire.client.gui.AbstractMillenaireScreen;
import org.millenaire.client.gui.PanelRenderHelper;
import org.millenaire.client.gui.TravelBookNavHelper;
import org.millenaire.network.QuestCompleteStepPayload;
import org.millenaire.network.QuestOpenPayload;
import org.millenaire.network.QuestRefusePayload;
import org.millenaire.village.panel.PanelLine;
import org.slf4j.Logger;

public class QuestScreen
extends AbstractMillenaireScreen {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int PARCHMENT_WIDTH = 256;
    private static final int PARCHMENT_HEIGHT = 200;
    private static final int LINES_PER_PAGE = QuestScreen.computeMaxLinesPerPage(200);
    private static final int TEXT_WIDTH = QuestScreen.textWidth(256);
    private static final int BUTTON_WIDTH = 80;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_SPACING = 8;
    private static final int BUTTON_TIMEOUT_TICKS = 100;
    private final QuestOpenPayload data;
    private List<PanelLine> allLines = List.of();
    @Nullable
    private Button acceptButton;
    @Nullable
    private Button refuseButton;
    @Nullable
    private Button continueButton;
    @Nullable
    private Button closeButton;
    private boolean pendingResponse = false;
    private int pendingTicks = 0;
    @Nullable
    private String resultText = null;
    private boolean resultSuccess = false;

    public QuestScreen(QuestOpenPayload data) {
        super((Component)Component.literal((String)data.villagerDisplayName()));
        this.data = data;
    }

    protected void init() {
        super.init();
        this.allLines = this.buildContentLines();
        this.totalPages = PanelRenderHelper.computePageCount(this.allLines.size(), LINES_PER_PAGE);
        this.currentPage = 0;
        int parchX = (this.width - 256) / 2;
        int parchY = (this.height - 200) / 2;
        this.initPaginationButtons(parchX, parchY, 256, 200, 16);
        if (!this.data.villagerTypeKey().isEmpty()) {
            this.addTravelBookButton(parchX, parchY, 256, button -> TravelBookNavHelper.openVillagerDetail(this, this.data.cultureKey(), this.data.villagerTypeKey()));
        }
        int buttonY = parchY + 200 + 4;
        int centerX = this.width / 2;
        if (this.resultText != null) {
            this.closeButton = Button.builder((Component)Component.translatable((String)"gui.millenaire.quest.close"), b -> this.onClose()).bounds(centerX - 40, buttonY, 80, 20).build();
            this.addRenderableWidget((GuiEventListener)this.closeButton);
        } else if (this.data.isFirstStep()) {
            if (this.data.conditionsMet()) {
                this.refuseButton = Button.builder((Component)Component.translatable((String)"gui.millenaire.quest.refuse"), b -> this.onRefuse()).bounds(centerX - 80 - 4, buttonY, 80, 20).build();
                this.addRenderableWidget((GuiEventListener)this.refuseButton);
                this.acceptButton = Button.builder((Component)Component.translatable((String)"gui.millenaire.quest.accept"), b -> this.onAccept()).bounds(centerX + 4, buttonY, 80, 20).build();
                this.addRenderableWidget((GuiEventListener)this.acceptButton);
            } else {
                this.closeButton = Button.builder((Component)Component.translatable((String)"gui.millenaire.quest.close"), b -> this.onClose()).bounds(centerX - 40, buttonY, 80, 20).build();
                this.addRenderableWidget((GuiEventListener)this.closeButton);
            }
        } else if (this.data.conditionsMet()) {
            this.continueButton = Button.builder((Component)Component.translatable((String)"gui.millenaire.quest.continue"), b -> this.onContinue()).bounds(centerX - 40, buttonY, 80, 20).build();
            this.addRenderableWidget((GuiEventListener)this.continueButton);
        } else {
            this.closeButton = Button.builder((Component)Component.translatable((String)"gui.millenaire.quest.close"), b -> this.onClose()).bounds(centerX - 40, buttonY, 80, 20).build();
            this.addRenderableWidget((GuiEventListener)this.closeButton);
        }
        this.updatePaginationButtons();
    }

    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, 0x60000000);
        int parchX = (this.width - 256) / 2;
        int parchY = (this.height - 200) / 2;
        PanelRenderHelper.renderTexturedBackground(graphics, PanelRenderHelper.QUEST_TEXTURE, parchX, parchY, 256, 200);
        MutableComponent headerTitle = Component.literal((String)this.data.villagerDisplayName());
        int contentY = this.renderParchmentHeader(graphics, (Component)headerTitle, parchX, parchY, 256);
        Object occupations = this.data.villagerGameOccupation();
        if (!this.data.villagerNativeOccupation().isEmpty()) {
            occupations = !((String)occupations).isEmpty() ? this.data.villagerNativeOccupation() + " \u2014 " + (String)occupations : this.data.villagerNativeOccupation();
        }
        if (!((String)occupations).isEmpty()) {
            graphics.drawCenteredString(this.font, (Component)Component.literal((String)occupations).withStyle(s -> s.withItalic(Boolean.valueOf(true))), parchX + 128, contentY, -13421773);
            contentY += 11;
        }
        int startLine = this.currentPage * LINES_PER_PAGE;
        int maxLines = LINES_PER_PAGE;
        if (!((String)occupations).isEmpty() && this.currentPage == 0) {
            --maxLines;
        }
        int contentX = parchX + 16;
        PanelRenderHelper.renderPanelLines(graphics, this.font, this.allLines, contentX, contentY, TEXT_WIDTH, startLine, maxLines);
        this.renderPageCounter(graphics, parchX, parchY, 256, 200);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    public void tick() {
        super.tick();
        if (this.pendingResponse) {
            ++this.pendingTicks;
            if (this.pendingTicks >= 100) {
                this.pendingResponse = false;
                this.setButtonsEnabled(true);
            }
        }
    }

    private void onAccept() {
        String villagerUuid;
        this.setPendingState();
        ClientQuestCache.CachedQuest cq = ClientQuestCache.getQuest(this.data.questUniqueId());
        if (cq != null && (villagerUuid = this.resolveCurrentStepVillagerUuid(cq)) != null) {
            PacketDistributor.sendToServer((CustomPacketPayload)new QuestCompleteStepPayload(this.data.questUniqueId(), villagerUuid), (CustomPacketPayload[])new CustomPacketPayload[0]);
        }
    }

    private void onRefuse() {
        this.setPendingState();
        PacketDistributor.sendToServer((CustomPacketPayload)new QuestRefusePayload(this.data.questUniqueId()), (CustomPacketPayload[])new CustomPacketPayload[0]);
    }

    private void onContinue() {
        String villagerUuid;
        this.setPendingState();
        ClientQuestCache.CachedQuest cq = ClientQuestCache.getQuest(this.data.questUniqueId());
        if (cq != null && (villagerUuid = this.resolveCurrentStepVillagerUuid(cq)) != null) {
            PacketDistributor.sendToServer((CustomPacketPayload)new QuestCompleteStepPayload(this.data.questUniqueId(), villagerUuid), (CustomPacketPayload[])new CustomPacketPayload[0]);
        }
    }

    private void setPendingState() {
        this.pendingResponse = true;
        this.pendingTicks = 0;
        this.setButtonsEnabled(false);
    }

    private void setButtonsEnabled(boolean enabled) {
        if (this.acceptButton != null) {
            this.acceptButton.active = enabled;
        }
        if (this.refuseButton != null) {
            this.refuseButton.active = enabled;
        }
        if (this.continueButton != null) {
            this.continueButton.active = enabled;
        }
    }

    public void onQuestResult(String text, boolean success) {
        this.resultText = text;
        this.resultSuccess = success;
        this.pendingResponse = false;
        this.rebuildWidgets();
    }

    private List<PanelLine> buildContentLines() {
        ArrayList<PanelLine> lines = new ArrayList<PanelLine>();
        if (!this.data.descriptionText().isEmpty()) {
            QuestScreen.addSplitLines(lines, this.data.descriptionText(), null);
        }
        if (!this.data.conditionsMet() && !this.data.conditionText().isEmpty()) {
            lines.add(PanelLine.separator());
            QuestScreen.addSplitLines(lines, this.data.conditionText(), "\u00a7c");
        }
        if (this.resultText != null && !this.resultText.isEmpty()) {
            lines.add(PanelLine.separator());
            String prefix = this.resultSuccess ? "\u00a7a" : "\u00a7c";
            QuestScreen.addSplitLines(lines, this.resultText, prefix);
        }
        return PanelRenderHelper.wrapLines(lines, this.font, TEXT_WIDTH);
    }

    private static void addSplitLines(List<PanelLine> lines, String text, @Nullable String colorPrefix) {
        String normalized = text.replace("\n", "<ret>");
        String[] segments = normalized.split("<ret>");
        for (int i = 0; i < segments.length; ++i) {
            String segment = segments[i].trim();
            if (segment.isEmpty()) {
                lines.add(PanelLine.empty());
                continue;
            }
            lines.add(PanelLine.text((String)(colorPrefix != null ? colorPrefix + segment : segment)));
        }
    }

    @Nullable
    private String resolveCurrentStepVillagerUuid(ClientQuestCache.CachedQuest cq) {
        Entity entity;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && (entity = mc.level.getEntity(this.data.villagerEntityId())) != null) {
            return entity.getUUID().toString();
        }
        return null;
    }
}

