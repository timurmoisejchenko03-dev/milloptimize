/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  javax.annotation.Nullable
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.Font
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.gui.components.Button
 *  net.minecraft.client.gui.components.events.GuiEventListener
 *  net.minecraft.client.gui.screens.Screen
 *  net.minecraft.client.resources.language.I18n
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.chat.MutableComponent
 *  net.minecraft.network.protocol.common.custom.CustomPacketPayload
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.entity.EntityType
 *  net.minecraft.world.entity.EquipmentSlot
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.ItemLike
 *  net.minecraft.world.level.Level
 *  net.neoforged.neoforge.network.PacketDistributor
 *  org.slf4j.Logger
 */
package org.millenaire.client.gui;

import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import org.millenaire.client.gui.AbstractMillenaireScreen;
import org.millenaire.client.gui.MillenaireScreenUtils;
import org.millenaire.client.gui.PanelRenderHelper;
import org.millenaire.entity.MillVillager;
import org.millenaire.entity.ModEntities;
import org.millenaire.entity.ModelType;
import org.millenaire.item.ItemHelper;
import org.millenaire.language.DisplayNameResolver;
import org.millenaire.network.TravelBookContentPayload;
import org.millenaire.network.TravelBookRequestPayload;
import org.millenaire.village.TravelBookLine;
import org.millenaire.village.TravelBookScreenState;
import org.slf4j.Logger;

public class TravelBookScreen
extends AbstractMillenaireScreen {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int PARCHMENT_WIDTH = 256;
    private static final int PARCHMENT_HEIGHT = 220;
    private static final int LINES_PER_PAGE = TravelBookScreen.computeMaxLinesPerPage(220);
    private static final int LINES_PER_PAGE_WITH_BACK = LINES_PER_PAGE - 2;
    private static final int TEXT_WIDTH = 230;
    static final int LINK_COLOR = -15645526;
    static final int LINK_HOVER_COLOR = -14522659;
    @Nullable
    private static Screen pendingCallingScreen;
    @Nullable
    private Screen callingScreen;
    private TravelBookContentPayload payload;
    private List<TravelBookLine> wrappedLines = List.of();
    private int effectiveLinesPerPage = LINES_PER_PAGE;
    private static final int VILLAGER_PICTURE_OFFSET = 80;
    @Nullable
    private MillVillager mockVillager;
    @Nullable
    private Button backButton;
    @Nullable
    private Button prevItemButton;
    @Nullable
    private Button nextItemButton;

    @Override
    public boolean isPauseScreen() {
        return true;
    }

    public TravelBookScreen(TravelBookContentPayload payload) {
        super((Component)(payload.titleTranslatable() ? Component.translatable((String)payload.pageTitle()) : Component.literal((String)payload.pageTitle())));
        this.payload = payload;
    }

    public static void setCallingScreen(@Nullable Screen screen) {
        pendingCallingScreen = screen;
    }

    public static void applyContent(TravelBookContentPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        Screen screen = mc.screen;
        if (screen instanceof TravelBookScreen) {
            TravelBookScreen existing = (TravelBookScreen)screen;
            existing.updateContent(payload);
        } else {
            TravelBookScreen newScreen = new TravelBookScreen(payload);
            newScreen.callingScreen = pendingCallingScreen;
            pendingCallingScreen = null;
            mc.setScreen((Screen)newScreen);
        }
    }

    public void onClose() {
        if (this.callingScreen != null) {
            this.minecraft.setScreen(this.callingScreen);
        } else {
            super.onClose();
        }
    }

    private void updateContent(TravelBookContentPayload newPayload) {
        this.payload = newPayload;
        this.currentPage = 0;
        this.mockVillager = null;
        this.rebuildWidgets();
    }

    @Nullable
    private MillVillager createMockVillager() {
        if (!this.payload.hasMockVillager()) {
            return null;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return null;
        }
        try {
            Item item;
            MillVillager villager = new MillVillager((EntityType<? extends MillVillager>)((EntityType)ModEntities.MILL_VILLAGER.get()), (Level)mc.level);
            ModelType modelType = ModelType.fromByte(this.payload.mockModelType());
            ResourceLocation texture = this.payload.mockTexture().isEmpty() ? null : ResourceLocation.parse((String)this.payload.mockTexture());
            ResourceLocation cloth0 = this.payload.mockCloth0().isEmpty() ? null : ResourceLocation.parse((String)this.payload.mockCloth0());
            ResourceLocation cloth1 = this.payload.mockCloth1().isEmpty() ? null : ResourceLocation.parse((String)this.payload.mockCloth1());
            villager.initAppearance(modelType, texture, cloth0, cloth1, this.payload.mockScale(), "", "", "");
            if (!this.payload.mockHeldItem().isEmpty() && (item = ItemHelper.resolve(this.payload.mockHeldItem())) != null) {
                villager.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack((ItemLike)item));
            }
            if (!this.payload.mockHeldItemOffHand().isEmpty() && (item = ItemHelper.resolve(this.payload.mockHeldItemOffHand())) != null) {
                villager.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack((ItemLike)item));
            }
            return villager;
        }
        catch (Exception e) {
            LOGGER.warn("Failed to create mock villager for travel book", (Throwable)e);
            return null;
        }
    }

    protected void init() {
        super.init();
        boolean hasMock = this.payload.hasMockVillager();
        int effectiveTextWidth = hasMock ? 150 : 230;
        this.wrappedLines = TravelBookScreen.wrapTravelBookLines(this.payload.lines(), this.font, effectiveTextWidth);
        this.mockVillager = hasMock ? this.createMockVillager() : null;
        this.effectiveLinesPerPage = this.payload.hasBack() ? LINES_PER_PAGE_WITH_BACK : LINES_PER_PAGE;
        this.currentPage = 0;
        this.totalPages = PanelRenderHelper.computePageCount(this.wrappedLines.size(), this.effectiveLinesPerPage);
        int parchX = (this.width - 256) / 2;
        int parchY = (this.height - 220) / 2;
        this.initPaginationButtons(parchX, parchY, 256, 220, 16);
        if (this.payload.hasBack()) {
            int backW = 95;
            int backY = parchY + 220 - 13 - 22;
            this.backButton = Button.builder((Component)Component.translatable((String)"gui.millenaire.travel_book.back"), button -> this.sendNavRequest(TravelBookScreenState.HOME, "", "", "", 1)).bounds(parchX + (256 - backW) / 2, backY, backW, 20).build();
            this.addRenderableWidget((GuiEventListener)this.backButton);
        } else {
            this.backButton = null;
        }
        int itemNavY = parchY + 16 - 2;
        if (this.payload.hasPrev()) {
            this.prevItemButton = Button.builder((Component)Component.literal((String)"<"), button -> this.sendNavRequest(this.payload.currentState(), "", "", "", 3)).bounds(parchX + 1, itemNavY, 15, 20).build();
            this.addRenderableWidget((GuiEventListener)this.prevItemButton);
        } else {
            this.prevItemButton = null;
        }
        if (this.payload.hasNext()) {
            this.nextItemButton = Button.builder((Component)Component.literal((String)">"), button -> this.sendNavRequest(this.payload.currentState(), "", "", "", 2)).bounds(parchX + 256 - 15, itemNavY, 15, 20).build();
            this.addRenderableWidget((GuiEventListener)this.nextItemButton);
        } else {
            this.nextItemButton = null;
        }
        this.updatePaginationButtons();
    }

    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, 0x60000000);
        int parchX = (this.width - 256) / 2;
        int parchY = (this.height - 220) / 2;
        PanelRenderHelper.renderTexturedBackground(graphics, PanelRenderHelper.QUEST_TEXTURE, parchX, parchY, 256, 220);
        int contentX = parchX + 16;
        MutableComponent titleComponent = this.payload.titleTranslatable() ? Component.translatable((String)this.payload.pageTitle()) : Component.literal((String)this.payload.pageTitle());
        int y = this.renderParchmentHeader(graphics, (Component)titleComponent, parchX, parchY, 256);
        if (this.mockVillager != null && this.currentPage == 0) {
            int villagerCenterX = parchX + 256 - 16 - 35;
            int villagerTopY = y - 5;
            int villagerBottomY = y + 85;
            MillenaireScreenUtils.renderStaticVillagerPreview(graphics, villagerCenterX, villagerTopY, villagerBottomY, 30, (LivingEntity)this.mockVillager);
        }
        if (this.wrappedLines.isEmpty()) {
            graphics.drawCenteredString(this.font, (Component)Component.translatable((String)"gui.millenaire.travel_book.loading"), parchX + 128, y + 20, -13421773);
        } else {
            int startLine = this.currentPage * this.effectiveLinesPerPage;
            int renderWidth = this.mockVillager != null && this.currentPage == 0 ? 150 : 230;
            this.renderTravelBookLines(graphics, this.font, this.wrappedLines, contentX, y, renderWidth, startLine, this.effectiveLinesPerPage, mouseX, mouseY);
        }
        this.renderPageCounter(graphics, parchX, parchY, 256, 220);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        TravelBookLine.TravelBookNavTarget target;
        if (button == 0 && (target = this.getClickedNavTarget(mouseX, mouseY)) != null) {
            this.sendNavRequest(target.targetState(), target.cultureKey() != null ? target.cultureKey() : "", target.categoryKey() != null ? target.categoryKey() : "", target.itemKey() != null ? target.itemKey() : "", 0);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Nullable
    private TravelBookLine.TravelBookNavTarget getClickedNavTarget(double mouseX, double mouseY) {
        if (this.wrappedLines.isEmpty()) {
            return null;
        }
        int parchX = (this.width - 256) / 2;
        int parchY = (this.height - 220) / 2;
        int contentX = parchX + 16;
        int y = parchY + 42;
        int startLine = this.currentPage * this.effectiveLinesPerPage;
        int endLine = Math.min(startLine + this.effectiveLinesPerPage, this.wrappedLines.size());
        int currentY = y;
        for (int i = startLine; i < endLine; ++i) {
            TravelBookLine line = this.wrappedLines.get(i);
            if (line.isClickable() && mouseX >= (double)contentX && mouseX < (double)(contentX + 230) && mouseY >= (double)currentY && mouseY < (double)(currentY + 11)) {
                return line.navTarget();
            }
            currentY += 11;
        }
        return null;
    }

    private void sendNavRequest(TravelBookScreenState targetState, String cultureKey, String categoryKey, String itemKey, int navAction) {
        PacketDistributor.sendToServer((CustomPacketPayload)new TravelBookRequestPayload(targetState, cultureKey, categoryKey, itemKey, navAction), (CustomPacketPayload[])new CustomPacketPayload[0]);
    }

    private void renderTravelBookLines(GuiGraphics graphics, Font font, List<TravelBookLine> lines, int x, int y, int width, int startLine, int maxLines, int mouseX, int mouseY) {
        int endLine = Math.min(startLine + maxLines, lines.size());
        int rightEdge = x + width;
        int currentY = y;
        for (int i = startLine; i < endLine; ++i) {
            TravelBookLine line = lines.get(i);
            if (line.isSeparator()) {
                graphics.fill(x, currentY + 4, rightEdge, currentY + 5, -5601178);
            } else if (line.isColumns()) {
                ItemStack iconStack;
                int iconOffset = 0;
                if (line.leftIcon() != null && !(iconStack = PanelRenderHelper.resolveItemIcon(line.leftIcon())).isEmpty()) {
                    graphics.renderItem(iconStack, x, currentY - 2);
                    iconOffset = 18;
                }
                int textColor = this.getLineColor(line, x, currentY, width, mouseX, mouseY);
                graphics.drawString(font, line.leftColumn(), x + iconOffset, currentY, textColor, false);
                int rightWidth = font.width(line.rightColumn());
                int rightX = rightEdge - rightWidth;
                graphics.drawString(font, line.rightColumn(), rightX, currentY, textColor, false);
            } else {
                String displayText;
                int textColor = this.getLineColor(line, x, currentY, width, mouseX, mouseY);
                if (line.translatable() && !line.text().isEmpty()) {
                    String resolved = I18n.get((String)line.text(), (Object[])new Object[0]);
                    displayText = DisplayNameResolver.resolve(resolved, true, line.nativePrefix(), line.text());
                } else {
                    displayText = line.text();
                }
                graphics.drawString(font, displayText, x, currentY, textColor, false);
            }
            currentY += 11;
        }
    }

    private int getLineColor(TravelBookLine line, int x, int lineY, int width, int mouseX, int mouseY) {
        if (!line.isClickable()) {
            return -13421773;
        }
        if (mouseX >= x && mouseX < x + width && mouseY >= lineY && mouseY < lineY + 11) {
            return -14522659;
        }
        return -15645526;
    }

    static List<TravelBookLine> wrapTravelBookLines(List<TravelBookLine> source, Font font, int maxWidth) {
        ArrayList<TravelBookLine> result = new ArrayList<TravelBookLine>();
        for (TravelBookLine line : source) {
            String text;
            if (line.isSeparator() || line.isColumns()) {
                result.add(line);
                continue;
            }
            if (line.translatable() && !line.text().isEmpty()) {
                String resolved = I18n.get((String)line.text(), (Object[])new Object[0]);
                text = DisplayNameResolver.resolve(resolved, true, line.nativePrefix(), line.text());
            } else {
                text = line.text();
            }
            if (text.isEmpty()) {
                result.add(line);
                continue;
            }
            if (font.width(text) <= maxWidth) {
                result.add(line);
                continue;
            }
            String[] words = text.split(" ");
            StringBuilder current = new StringBuilder();
            boolean isFirst = true;
            for (String word : words) {
                String test;
                String string = test = current.isEmpty() ? word : String.valueOf(current) + " " + word;
                if (font.width(test) > maxWidth && !current.isEmpty()) {
                    String activeColor = PanelRenderHelper.extractLastColorCode(current.toString());
                    if (isFirst) {
                        result.add(new TravelBookLine(current.toString(), false, null, null, null, false, null, line.navTarget()));
                        isFirst = false;
                    } else {
                        result.add(TravelBookLine.text(current.toString()));
                    }
                    current = new StringBuilder(activeColor + word);
                    continue;
                }
                current = new StringBuilder(test);
            }
            if (current.isEmpty()) continue;
            if (isFirst) {
                result.add(new TravelBookLine(current.toString(), false, null, null, null, false, null, line.navTarget()));
                continue;
            }
            result.add(TravelBookLine.text(current.toString()));
        }
        return result;
    }
}

