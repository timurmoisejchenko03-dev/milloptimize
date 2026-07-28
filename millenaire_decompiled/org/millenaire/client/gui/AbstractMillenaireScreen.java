/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.gui.components.AbstractWidget
 *  net.minecraft.client.gui.components.Button$OnPress
 *  net.minecraft.client.gui.components.events.GuiEventListener
 *  net.minecraft.client.gui.narration.NarrationElementOutput
 *  net.minecraft.client.gui.screens.Screen
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.chat.FormattedText
 *  net.minecraft.network.chat.MutableComponent
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.Items
 *  net.minecraft.world.level.ItemLike
 */
package org.millenaire.client.gui;

import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import org.millenaire.client.gui.IconButton;
import org.millenaire.client.gui.PanelRenderHelper;
import org.millenaire.village.panel.PanelLine;

public abstract class AbstractMillenaireScreen
extends Screen {
    protected static final int PARCHMENT_PADDING = 16;
    protected static final int PARCHMENT_TITLE_HEIGHT = 20;
    protected static final int SEPARATOR_GAP = 6;
    protected static final int HEADER_HEIGHT = 42;
    protected static final int BOTTOM_RESERVE = 15;
    protected int currentPage = 0;
    protected int totalPages = 1;
    protected AbstractWidget prevButton;
    protected AbstractWidget nextButton;
    private static final int ARROW_WIDTH = 32;
    private static final int ARROW_HEIGHT = 13;
    private static final int ARROW_LEFT_X = 1;
    private static final int ARROW_INSET_RIGHT = 32;
    private static final int ARROW_INSET_BOTTOM = 13;

    protected AbstractMillenaireScreen(Component title) {
        super(title);
    }

    protected static int computeMaxLinesPerPage(int parchmentHeight) {
        return (parchmentHeight - 42 - 15) / 11;
    }

    protected static int textWidth(int parchmentWidth) {
        return parchmentWidth - 32;
    }

    protected int renderParchmentHeader(GuiGraphics graphics, Component title, int parchX, int parchY, int parchWidth) {
        int y = parchY + 16;
        int maxWidth = parchWidth - 32;
        MutableComponent styledTitle = title.copy().withStyle(s -> s.withBold(Boolean.valueOf(true)));
        if (this.font.width((FormattedText)styledTitle) > maxWidth) {
            String raw = styledTitle.getString();
            String ellipsis = "...";
            int ellipsisWidth = this.font.width(ellipsis);
            StringBuilder truncated = new StringBuilder();
            for (int i = 0; i < raw.length() && this.font.width(truncated.toString() + raw.charAt(i)) + ellipsisWidth <= maxWidth; ++i) {
                truncated.append(raw.charAt(i));
            }
            styledTitle = Component.literal((String)(String.valueOf(truncated) + ellipsis)).withStyle(s -> s.withBold(Boolean.valueOf(true)));
        }
        graphics.drawCenteredString(this.font, (Component)styledTitle, parchX + parchWidth / 2, y, -11193600);
        int contentX = parchX + 16;
        graphics.fill(contentX, y += 20, parchX + parchWidth - 16, y + 1, -5601178);
        return y += 6;
    }

    protected void addTravelBookButton(int parchX, int parchY, int parchWidth, Button.OnPress action) {
        int tbButtonSize = 20;
        int tbButtonX = parchX + parchWidth - 16 - tbButtonSize;
        int tbButtonY = parchY + 16 - 2;
        this.addRenderableWidget((GuiEventListener)new IconButton(tbButtonX, tbButtonY, tbButtonSize, tbButtonSize, new ItemStack((ItemLike)Items.WRITABLE_BOOK), (Component)Component.translatable((String)"gui.millenaire.travel_book.open"), action));
    }

    protected List<PanelLine> wrapLines(List<PanelLine> lines, int parchmentWidth) {
        return PanelRenderHelper.wrapLines(lines, this.font, AbstractMillenaireScreen.textWidth(parchmentWidth));
    }

    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    public boolean isPauseScreen() {
        return false;
    }

    protected void initPaginationButtons(int parchX, int parchY, int parchWidth, int parchHeight, int padding) {
        int arrowY = parchY + parchHeight - 13;
        this.prevButton = new ArrowZone(parchX + 1, arrowY, 32, 13, true, () -> {
            if (this.currentPage > 0) {
                --this.currentPage;
                this.onPageChanged();
            }
        });
        this.addRenderableWidget((GuiEventListener)this.prevButton);
        this.nextButton = new ArrowZone(parchX + parchWidth - 32, arrowY, 32, 13, false, () -> {
            if (this.currentPage < this.totalPages - 1) {
                ++this.currentPage;
                this.onPageChanged();
            }
        });
        this.addRenderableWidget((GuiEventListener)this.nextButton);
    }

    protected void updatePaginationButtons() {
        this.prevButton.visible = this.totalPages > 1;
        this.nextButton.visible = this.totalPages > 1;
        this.prevButton.active = this.currentPage > 0;
        this.nextButton.active = this.currentPage < this.totalPages - 1;
    }

    protected void onPageChanged() {
        this.updatePaginationButtons();
    }

    protected void renderPageCounter(GuiGraphics graphics, int parchX, int parchY, int parchWidth, int parchHeight) {
        if (this.totalPages > 1) {
            String pageStr = this.currentPage + 1 + "/" + this.totalPages;
            graphics.drawCenteredString(this.font, pageStr, parchX + parchWidth / 2, parchY + parchHeight - 10, -11193600);
        }
    }

    protected static class ArrowZone
    extends AbstractWidget {
        private final Runnable onClick;
        private final boolean pointsLeft;

        ArrowZone(int x, int y, int width, int height, boolean pointsLeft, Runnable onClick) {
            super(x, y, width, height, (Component)Component.empty());
            this.pointsLeft = pointsLeft;
            this.onClick = onClick;
        }

        public void onClick(double mouseX, double mouseY) {
            this.onClick.run();
        }

        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            int color = !this.active ? 0x55553300 : (this.isHovered() ? -8762112 : -11193600);
            int aw = 7;
            int ah = 11;
            int baseX = this.getX() + (this.getWidth() - aw) / 2;
            int cy = this.getY() + this.getHeight() / 2;
            for (int i = 0; i < aw; ++i) {
                int half = Math.round((float)ah / 2.0f * ((float)i / (float)(aw - 1)));
                int colX = this.pointsLeft ? baseX + i : baseX + (aw - 1 - i);
                graphics.fill(colX, cy - half, colX + 1, cy + half + 1, color);
            }
        }

        protected void updateWidgetNarration(NarrationElementOutput output) {
        }
    }
}

