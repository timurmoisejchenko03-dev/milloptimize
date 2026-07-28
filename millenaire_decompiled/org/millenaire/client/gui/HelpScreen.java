/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.StringSplitter
 *  net.minecraft.client.gui.Font
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.gui.screens.Screen
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.chat.FormattedText
 *  net.minecraft.network.chat.Style
 *  net.minecraft.resources.ResourceLocation
 */
package org.millenaire.client.gui;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.StringSplitter;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import org.millenaire.client.gui.AbstractMillenaireScreen;
import org.millenaire.client.gui.HelpContentLoader;

public class HelpScreen
extends AbstractMillenaireScreen {
    private static final ResourceLocation HELP_TEXTURE = ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)"textures/gui/help.png");
    private static final int X_SIZE = 256;
    private static final int Y_SIZE = 224;
    private static final int NUM_CHAPTERS = 13;
    private static final int TAB_WIDTH = 32;
    private static final int TAB_HEIGHT = 32;
    private static final int LEFT_TABS = 7;
    private static final int RIGHT_TABS = 6;
    private static final int TEXT_X_OFFSET = 36;
    private static final int TEXT_WIDTH = 180;
    private static final int TEXT_Y_START = 14;
    private static final int TEXT_Y_END = 206;
    private static final int LINE_HEIGHT = 10;
    private static final int ARROW_Y = 210;
    private static final int ARROW_LEFT_X = 36;
    private static final int ARROW_RIGHT_X = 192;
    private static final int TAB_OVERLAY_COLOR = -1610612736;
    private static final int TEXT_COLOR = 0;
    private int xStart;
    private int yStart;
    @Nullable
    private final Screen callingScreen;
    private int currentChapter = 1;
    private int currentPage = 0;
    private List<List<String>> pages = List.of();
    private int linesPerPage;

    public HelpScreen(@Nullable Screen callingScreen) {
        super((Component)Component.translatable((String)"help.tab_1"));
        this.callingScreen = callingScreen;
    }

    public void onClose() {
        Minecraft.getInstance().setScreen(this.callingScreen);
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }

    protected void init() {
        super.init();
        this.xStart = (this.width - 256) / 2;
        this.yStart = (this.height - 224) / 2;
        this.linesPerPage = 19;
        this.loadChapter(this.currentChapter);
    }

    private void loadChapter(int chapter) {
        this.currentChapter = chapter;
        this.currentPage = 0;
        List<List<String>> rawPages = HelpContentLoader.loadChapter(chapter);
        this.pages = new ArrayList<List<String>>();
        for (List<String> rawPage : rawPages) {
            ArrayList<String> wrappedLines = new ArrayList<String>();
            for (String line : rawPage) {
                if (line.isEmpty()) {
                    wrappedLines.add("");
                    continue;
                }
                wrappedLines.addAll(this.wordWrap(line, 180));
            }
            for (int i = 0; i < wrappedLines.size(); i += this.linesPerPage) {
                int end = Math.min(i + this.linesPerPage, wrappedLines.size());
                this.pages.add(new ArrayList(wrappedLines.subList(i, end)));
            }
        }
        if (this.pages.isEmpty()) {
            this.pages = List.of(List.of("No content available."));
        }
    }

    private List<String> wordWrap(String text, int maxWidth) {
        Font font = this.font;
        if (font == null) {
            return List.of(text);
        }
        List split = font.split(FormattedText.of((String)text), maxWidth);
        ArrayList<String> wrapped = new ArrayList<String>();
        StringSplitter splitter = font.getSplitter();
        List parts = splitter.splitLines(text, maxWidth, Style.EMPTY);
        String lastColor = null;
        for (FormattedText part : parts) {
            Object line = part.getString();
            if (lastColor != null && !((String)line).startsWith("\u00a7")) {
                line = lastColor + (String)line;
            }
            wrapped.add((String)line);
            String color = HelpScreen.getLastColorCode((String)line);
            if (color == null) continue;
            lastColor = color;
        }
        return wrapped;
    }

    private static String getLastColorCode(String text) {
        int lastIdx = text.lastIndexOf(167);
        if (lastIdx >= 0 && lastIdx + 1 < text.length()) {
            return text.substring(lastIdx, lastIdx + 2);
        }
        return null;
    }

    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, 0x60000000);
        graphics.blit(HELP_TEXTURE, this.xStart, this.yStart, 0.0f, 0.0f, 256, 224, 256, 256);
        this.renderTabOverlays(graphics);
        this.renderText(graphics);
        int totalPages = this.pages.size();
        if (totalPages > 1) {
            String pageStr = this.currentPage + 1 + "/" + totalPages;
            int pageWidth = this.font.width(pageStr);
            graphics.drawString(this.font, pageStr, this.xStart + 128 - pageWidth / 2, this.yStart + 210 + 2, 0, false);
        }
        this.renderTabTooltip(graphics, mouseX, mouseY);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderTabOverlays(GuiGraphics graphics) {
        int tabY;
        int i;
        for (i = 0; i < 7; ++i) {
            if (i + 1 == this.currentChapter) continue;
            tabY = this.yStart + i * 32;
            graphics.fill(this.xStart, tabY, this.xStart + 32, tabY + 32, -1610612736);
        }
        for (i = 0; i < 6; ++i) {
            if (i + 8 == this.currentChapter) continue;
            tabY = this.yStart + i * 32;
            graphics.fill(this.xStart + 256 - 32, tabY, this.xStart + 256, tabY + 32, -1610612736);
        }
    }

    private void renderText(GuiGraphics graphics) {
        if (this.currentPage >= this.pages.size()) {
            return;
        }
        List<String> pageLines = this.pages.get(this.currentPage);
        int textX = this.xStart + 36;
        int y = this.yStart + 14;
        for (String line : pageLines) {
            if (!line.isEmpty()) {
                graphics.drawString(this.font, (Component)Component.literal((String)line), textX, y, 0, false);
            }
            y += 10;
        }
    }

    private void renderTabTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        int chapter;
        int relX = mouseX - this.xStart;
        int relY = mouseY - this.yStart;
        if (relY < 0 || relY >= 224) {
            return;
        }
        int tabIndex = relY / 32;
        if (relX >= 0 && relX < 32 && tabIndex < 7) {
            chapter = tabIndex + 1;
            graphics.renderTooltip(this.font, (Component)Component.translatable((String)("help.tab_" + chapter)), mouseX, mouseY);
        }
        if (relX >= 224 && relX < 256 && tabIndex < 6) {
            chapter = tabIndex + 8;
            graphics.renderTooltip(this.font, (Component)Component.translatable((String)("help.tab_" + chapter)), mouseX, mouseY);
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int relX = (int)mouseX - this.xStart;
        int relY = (int)mouseY - this.yStart;
        if (relY < 0 || relY >= 224 || relX < 0 || relX >= 256) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        int tabIndex = relY / 32;
        if (relX < 32 && tabIndex < 7) {
            this.loadChapter(tabIndex + 1);
            return true;
        }
        if (relX >= 224 && tabIndex < 6) {
            this.loadChapter(tabIndex + 8);
            return true;
        }
        if (relY >= 210 && relY <= 224) {
            if (relX >= 36 && relX < 64) {
                if (this.currentPage > 0) {
                    --this.currentPage;
                }
                return true;
            }
            if (relX >= 192 && relX < 220) {
                if (this.currentPage < this.pages.size() - 1) {
                    ++this.currentPage;
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}

