/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.gui.components.Button
 *  net.minecraft.client.gui.components.events.GuiEventListener
 *  net.minecraft.client.gui.screens.Screen
 *  net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
 *  net.minecraft.core.registries.Registries
 *  net.minecraft.network.chat.Component
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.tags.TagKey
 *  net.minecraft.world.entity.player.Inventory
 *  net.minecraft.world.inventory.AbstractContainerMenu
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.ItemLike
 */
package org.millenaire.client.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import org.millenaire.client.gui.TravelBookNavHelper;
import org.millenaire.commerce.TradeAction;
import org.millenaire.commerce.TradeMenu;
import org.millenaire.item.MoneyHelper;

public class TradeScreen
extends AbstractContainerScreen<TradeMenu> {
    private static final ResourceLocation TRADE_TEXTURE = ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)"textures/gui/trade.png");
    private static final int LABEL_COLOR = 0x404040;
    private static final int COLOR_WHITE = 0xFFFFFF;
    private static final int COLOR_GRAY = 0x555555;
    private static final int COLOR_RED = 0xFF5555;
    private static final int COLOR_GREEN = 0x55FF55;
    private static final int COLOR_YELLOW = 0xCCCC00;
    private static final int DARK_OVERLAY = Integer.MIN_VALUE;
    private static final int GRID_COLS = 13;
    private static final int CELL_SPACING = 18;
    private static final int VISIBLE_ROWS = 2;
    private static final int SELL_GRID_Y = 32;
    private static final int BUY_GRID_Y = 86;
    private static final int GRID_X = 8;
    private static final int SELL_ARROW_Y = 68;
    private static final int BUY_ARROW_Y = 122;
    private static final int ARROW_UP_X = 216;
    private static final int ARROW_DOWN_X = 230;
    private static final int ARROW_W = 11;
    private static final int ARROW_H = 7;
    private static final int ARROW_DISABLED_U = 5;
    private static final int ARROW_DISABLED_V = 5;
    private static final int DONATION_X = 8;
    private static final int DONATION_Y = 122;
    private static final int DONATION_ICON_SIZE = 16;
    private int sellRowOffset = 0;
    private int buyRowOffset = 0;
    private List<TradeMenu.ClientGoodEntry> sellGoods = List.of();
    private List<TradeMenu.ClientGoodEntry> buyGoods = List.of();
    private int knownGoodsVersion = -1;

    public TradeScreen(TradeMenu menu, Inventory playerInventory, Component title) {
        super((AbstractContainerMenu)menu, playerInventory, title);
        this.imageWidth = 248;
        this.imageHeight = 222;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
        this.inventoryLabelX = 44;
    }

    protected void init() {
        super.init();
        this.refreshGoodLists();
        this.addRenderableWidget((GuiEventListener)Button.builder((Component)Component.translatable((String)"gui.millenaire.common.help_btn"), button -> TravelBookNavHelper.openCulture((Screen)this, ((TradeMenu)this.menu).getCultureKey())).bounds(this.leftPos + this.imageWidth - 20, this.topPos + 4, 16, 16).build());
    }

    private void refreshGoodLists() {
        this.knownGoodsVersion = ((TradeMenu)this.menu).getGoodsVersion();
        this.sellGoods = new ArrayList<TradeMenu.ClientGoodEntry>();
        this.buyGoods = new ArrayList<TradeMenu.ClientGoodEntry>();
        for (TradeMenu.ClientGoodEntry entry : ((TradeMenu)this.menu).getClientGoods()) {
            if (entry.isSelling()) {
                this.sellGoods.add(entry);
                continue;
            }
            this.buyGoods.add(entry);
        }
    }

    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        boolean donating;
        int buyTotalRows;
        int sellTotalRows;
        int x = this.leftPos;
        int y = this.topPos;
        graphics.blit(TRADE_TEXTURE, x, y, 0.0f, 0.0f, this.imageWidth, this.imageHeight, 256, 256);
        if (this.sellRowOffset == 0) {
            graphics.blit(TRADE_TEXTURE, x + 216, y + 68, 5.0f, 5.0f, 11, 7, 256, 256);
        }
        if (this.sellRowOffset >= (sellTotalRows = (this.sellGoods.size() + 13 - 1) / 13) - 2) {
            graphics.blit(TRADE_TEXTURE, x + 230, y + 68, 5.0f, 5.0f, 11, 7, 256, 256);
        }
        if (this.buyRowOffset == 0) {
            graphics.blit(TRADE_TEXTURE, x + 216, y + 122, 5.0f, 5.0f, 11, 7, 256, 256);
        }
        if (this.buyRowOffset >= (buyTotalRows = (this.buyGoods.size() + 13 - 1) / 13) - 2) {
            graphics.blit(TRADE_TEXTURE, x + 230, y + 122, 5.0f, 5.0f, 11, 7, 256, 256);
        }
        if (!(donating = ((TradeMenu)this.menu).isDonationMode())) {
            graphics.blit(TRADE_TEXTURE, x + 8, y + 122, 0.0f, 238.0f, 16, 16, 256, 256);
            graphics.blit(TRADE_TEXTURE, x + 8 + 16, y + 122, 16.0f, 222.0f, 16, 16, 256, 256);
        } else {
            graphics.blit(TRADE_TEXTURE, x + 8, y + 122, 0.0f, 222.0f, 16, 16, 256, 256);
            graphics.blit(TRADE_TEXTURE, x + 8 + 16, y + 122, 16.0f, 238.0f, 16, 16, 256, 256);
        }
    }

    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        String titleText = ((TradeMenu)this.menu).getBuildingName() + " - " + ((TradeMenu)this.menu).getVillageName();
        int maxTitleWidth = this.imageWidth - 16 - 20;
        if (this.font.width(titleText) > maxTitleWidth) {
            String ellipsis = "...";
            int ellipsisWidth = this.font.width(ellipsis);
            StringBuilder truncated = new StringBuilder();
            for (int i = 0; i < titleText.length() && this.font.width(truncated.toString() + titleText.charAt(i)) + ellipsisWidth <= maxTitleWidth; ++i) {
                truncated.append(titleText.charAt(i));
            }
            titleText = String.valueOf(truncated) + ellipsis;
        }
        graphics.drawString(this.font, titleText, 8, 6, 0x404040, false);
        if (!this.sellGoods.isEmpty()) {
            String sellHeader = Component.translatable((String)"gui.millenaire.trade.selling").getString();
            graphics.drawString(this.font, sellHeader, 8, 22, 0x404040, false);
        }
        if (!this.buyGoods.isEmpty()) {
            String buyHeader = Component.translatable((String)"gui.millenaire.trade.buying").getString();
            graphics.drawString(this.font, buyHeader, 8, 76, 0x404040, false);
        }
        String invLabel = Component.translatable((String)"container.inventory").getString();
        graphics.drawString(this.font, invLabel, 44, this.imageHeight - 96 + 2, 0x404040, false);
    }

    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (((TradeMenu)this.menu).getGoodsVersion() != this.knownGoodsVersion) {
            this.refreshGoodLists();
        }
        super.render(graphics, mouseX, mouseY, partialTick);
        int x = this.leftPos;
        int y = this.topPos;
        if (!this.sellGoods.isEmpty()) {
            this.renderGoodGrid(graphics, this.sellGoods, x + 8, y + 32, mouseX, mouseY, true, this.sellRowOffset);
        }
        if (!this.buyGoods.isEmpty()) {
            this.renderGoodGrid(graphics, this.buyGoods, x + 8, y + 86, mouseX, mouseY, false, this.buyRowOffset);
        }
        this.renderGoodTooltips(graphics, mouseX, mouseY);
        this.renderDonationTooltips(graphics, mouseX, mouseY);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    private void renderGoodGrid(GuiGraphics graphics, List<TradeMenu.ClientGoodEntry> goods, int startX, int startY, int mouseX, int mouseY, boolean isSellSection, int rowOffset) {
        int playerRep = ((TradeMenu)this.menu).getPlayerReputation();
        int balance = MoneyHelper.getTotalDeniers(this.minecraft.player.getInventory());
        int startIndex = rowOffset * 13;
        int endIndex = Math.min(goods.size(), startIndex + 26);
        for (int i = startIndex; i < endIndex; ++i) {
            int localIdx = i - startIndex;
            int col = localIdx % 13;
            int row = localIdx / 13;
            int cellX = startX + col * 18;
            int cellY = startY + row * 18;
            TradeMenu.ClientGoodEntry entry = goods.get(i);
            ItemStack displayStack = new ItemStack((ItemLike)entry.item());
            boolean unavailable = false;
            if (isSellSection) {
                if (playerRep < entry.minReputation()) {
                    unavailable = true;
                }
                if (balance < entry.sellingPrice()) {
                    unavailable = true;
                }
                if (!entry.autoGenerate() && entry.stock() <= 0) {
                    unavailable = true;
                }
            } else {
                if (!this.playerHasItem(entry)) {
                    unavailable = true;
                }
                if (entry.targetQuantity() > 0 && entry.stock() >= entry.targetQuantity()) {
                    unavailable = true;
                }
            }
            graphics.renderItem(displayStack, cellX + 1, cellY + 1);
            int displayCount = isSellSection ? (entry.autoGenerate() ? 99 : Math.max(Math.min(entry.stock(), 99), 1)) : Math.max(Math.min(this.countPlayerItem(entry), 99), 1);
            if (displayCount > 1) {
                ItemStack countStack = new ItemStack((ItemLike)entry.item(), displayCount);
                graphics.renderItemDecorations(this.font, countStack, cellX + 1, cellY + 1);
            }
            if (!unavailable) continue;
            graphics.fill(cellX + 1, cellY + 1, cellX + 17, cellY + 17, Integer.MIN_VALUE);
        }
    }

    private void renderGoodTooltips(GuiGraphics graphics, int mouseX, int mouseY) {
        int idx;
        int x = this.leftPos;
        int y = this.topPos;
        int playerRep = ((TradeMenu)this.menu).getPlayerReputation();
        int balance = MoneyHelper.getTotalDeniers(this.minecraft.player.getInventory());
        if (!this.sellGoods.isEmpty() && (idx = this.findIndexInVisibleGrid(this.sellGoods, x + 8, y + 32, mouseX, mouseY, this.sellRowOffset)) >= 0) {
            List<Component> tooltip = this.buildTooltip(this.sellGoods.get(idx), true, playerRep, balance);
            graphics.renderTooltip(this.font, tooltip, Optional.empty(), mouseX, mouseY);
            return;
        }
        if (!this.buyGoods.isEmpty() && (idx = this.findIndexInVisibleGrid(this.buyGoods, x + 8, y + 86, mouseX, mouseY, this.buyRowOffset)) >= 0) {
            List<Component> tooltip = this.buildTooltip(this.buyGoods.get(idx), false, playerRep, balance);
            graphics.renderTooltip(this.font, tooltip, Optional.empty(), mouseX, mouseY);
            return;
        }
    }

    private void renderDonationTooltips(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        int dx = mouseX - x;
        int dy = mouseY - y;
        if (dy >= 122 && dy <= 138) {
            if (dx >= 8 && dx <= 24) {
                graphics.renderTooltip(this.font, (Component)Component.translatable((String)"ui.trade_buying"), mouseX, mouseY);
            } else if (dx >= 24 && dx <= 40) {
                graphics.renderTooltip(this.font, (Component)Component.translatable((String)"ui.trade_donation"), mouseX, mouseY);
            }
        }
    }

    private int findIndexInVisibleGrid(List<TradeMenu.ClientGoodEntry> goods, int startX, int startY, int mouseX, int mouseY, int rowOffset) {
        int startIndex = rowOffset * 13;
        int endIndex = Math.min(goods.size(), startIndex + 26);
        for (int i = startIndex; i < endIndex; ++i) {
            int localIdx = i - startIndex;
            int col = localIdx % 13;
            int row = localIdx / 13;
            int cellX = startX + col * 18;
            int cellY = startY + row * 18;
            if (mouseX < cellX || mouseX >= cellX + 18 || mouseY < cellY || mouseY >= cellY + 18) continue;
            return i;
        }
        return -1;
    }

    private List<Component> buildTooltip(TradeMenu.ClientGoodEntry entry, boolean isSellSection, int playerRep, int balance) {
        ArrayList<Component> lines = new ArrayList<Component>();
        ItemStack stack = new ItemStack((ItemLike)entry.item());
        lines.add(stack.getHoverName());
        if (isSellSection) {
            lines.add((Component)Component.translatable((String)"gui.millenaire.trade.price_line", (Object[])new Object[]{MoneyHelper.formatPrice(entry.sellingPrice())}).withStyle(s -> s.withColor(0xFFFFFF)));
            if (!entry.autoGenerate()) {
                int availableStock = Math.min(Math.max(0, entry.stock()), 99);
                int stockColor = availableStock > 0 ? 0x55FF55 : 0xFF5555;
                lines.add((Component)Component.translatable((String)"gui.millenaire.trade.stock", (Object[])new Object[]{availableStock}).withStyle(s -> s.withColor(stockColor)));
                if (availableStock <= 0) {
                    lines.add((Component)Component.translatable((String)"gui.millenaire.trade.out_of_stock").withStyle(s -> s.withColor(0xFF5555)));
                }
            }
            if (balance < entry.sellingPrice()) {
                int missing = entry.sellingPrice() - balance;
                lines.add((Component)Component.translatable((String)"gui.millenaire.trade.not_enough_money", (Object[])new Object[]{MoneyHelper.formatPrice(missing)}).withStyle(s -> s.withColor(0xFF5555)));
            }
            if (playerRep < entry.minReputation()) {
                lines.add((Component)Component.translatable((String)"gui.millenaire.trade.reputation_too_low", (Object[])new Object[]{String.valueOf(entry.minReputation())}).withStyle(s -> s.withColor(0xFF5555)));
            }
        } else {
            if (((TradeMenu)this.menu).isDonationMode()) {
                lines.add((Component)Component.translatable((String)"gui.millenaire.trade.donating").withStyle(s -> s.withColor(0xCCCC00)));
                lines.add((Component)Component.translatable((String)"gui.millenaire.trade.donation_rep_bonus").withStyle(s -> s.withColor(0x55FF55)));
            } else {
                lines.add((Component)Component.translatable((String)"gui.millenaire.trade.price_line", (Object[])new Object[]{MoneyHelper.formatPrice(entry.buyingPrice())}).withStyle(s -> s.withColor(0xFFFFFF)));
            }
            if (entry.targetQuantity() > 0 && entry.stock() >= entry.targetQuantity()) {
                lines.add((Component)Component.translatable((String)"gui.millenaire.trade.village_has_enough").withStyle(s -> s.withColor(0xFF5555)));
            }
        }
        if (isSellSection) {
            repGain = entry.sellingPrice();
            lines.add((Component)Component.translatable((String)"gui.millenaire.trade.rep_gain", (Object[])new Object[]{String.valueOf(repGain)}).withStyle(s -> s.withColor(0x55FF55)));
        } else {
            repGain = entry.buyingPrice();
            if (((TradeMenu)this.menu).isDonationMode()) {
                repGain *= 4;
            }
            lines.add((Component)Component.translatable((String)"gui.millenaire.trade.rep_gain", (Object[])new Object[]{String.valueOf(repGain)}).withStyle(s -> s.withColor(0x55FF55)));
        }
        lines.add((Component)Component.translatable((String)"gui.millenaire.trade.click_hint").withStyle(s -> s.withColor(0x555555).withItalic(Boolean.valueOf(true))));
        return lines;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int mx = (int)mouseX;
        int my = (int)mouseY;
        int x = this.leftPos;
        int y = this.topPos;
        if (!this.sellGoods.isEmpty()) {
            int sellStartY = y + 32;
            int sellEndY = sellStartY + 36;
            if (mx >= x + 8 && mx < x + 8 + 234 && my >= sellStartY && my < sellEndY) {
                int maxRow = Math.max(0, (this.sellGoods.size() + 13 - 1) / 13 - 2);
                this.sellRowOffset = Math.max(0, Math.min(maxRow, this.sellRowOffset - (int)Math.signum(scrollY)));
                return true;
            }
        }
        if (!this.buyGoods.isEmpty()) {
            int buyStartY = y + 86;
            int buyEndY = buyStartY + 36;
            if (mx >= x + 8 && mx < x + 8 + 234 && my >= buyStartY && my < buyEndY) {
                int maxRow = Math.max(0, (this.buyGoods.size() + 13 - 1) / 13 - 2);
                this.buyRowOffset = Math.max(0, Math.min(maxRow, this.buyRowOffset - (int)Math.signum(scrollY)));
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int[] result;
        if (button == 0) {
            int maxRow;
            int dx = (int)mouseX - this.leftPos;
            int dy = (int)mouseY - this.topPos;
            if (dy >= 68 && dy <= 75) {
                if (dx >= 216 && dx <= 227) {
                    if (this.sellRowOffset > 0) {
                        --this.sellRowOffset;
                        return true;
                    }
                } else if (dx >= 230 && dx <= 241 && this.sellRowOffset < (maxRow = Math.max(0, (this.sellGoods.size() + 13 - 1) / 13 - 2))) {
                    ++this.sellRowOffset;
                    return true;
                }
            }
            if (dy >= 122 && dy <= 129) {
                if (dx >= 216 && dx <= 227) {
                    if (this.buyRowOffset > 0) {
                        --this.buyRowOffset;
                        return true;
                    }
                } else if (dx >= 230 && dx <= 241 && this.buyRowOffset < (maxRow = Math.max(0, (this.buyGoods.size() + 13 - 1) / 13 - 2))) {
                    ++this.buyRowOffset;
                    return true;
                }
            }
            if (dy >= 122 && dy <= 138) {
                if (dx >= 8 && dx <= 24) {
                    if (((TradeMenu)this.menu).isDonationMode()) {
                        ((TradeMenu)this.menu).toggleDonationModeClient();
                        this.sendButtonClick(TradeMenu.getToggleDonationButtonId());
                        return true;
                    }
                } else if (dx >= 24 && dx <= 40 && !((TradeMenu)this.menu).isDonationMode()) {
                    ((TradeMenu)this.menu).toggleDonationModeClient();
                    this.sendButtonClick(TradeMenu.getToggleDonationButtonId());
                    return true;
                }
            }
        }
        if ((result = this.findGoodAndSection((int)mouseX, (int)mouseY)) != null) {
            boolean selling = result[0] == 1;
            int globalIndex = result[1];
            TradeMenu.ClientGoodEntry entry = selling ? this.sellGoods.get(globalIndex) : this.buyGoods.get(globalIndex);
            int menuIndex = this.findGlobalIndex(entry);
            if (menuIndex >= 0) {
                int qty = this.getClickQuantity(button);
                TradeAction action = TradeAction.fromDirectionAndQuantity(selling, qty);
                if (action == null) {
                    return super.mouseClicked(mouseX, mouseY, button);
                }
                this.sendButtonClick(action.toButtonId(menuIndex));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void sendButtonClick(int buttonId) {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(((TradeMenu)this.menu).containerId, buttonId);
        }
    }

    private int getClickQuantity(int button) {
        boolean shift = TradeScreen.hasShiftDown();
        if (shift) {
            return 64;
        }
        if (button == 1) {
            return 8;
        }
        return 1;
    }

    private int[] findGoodAndSection(int mouseX, int mouseY) {
        int idx;
        int x = this.leftPos;
        int y = this.topPos;
        if (!this.sellGoods.isEmpty() && (idx = this.findIndexInVisibleGrid(this.sellGoods, x + 8, y + 32, mouseX, mouseY, this.sellRowOffset)) >= 0) {
            return new int[]{1, idx};
        }
        if (!this.buyGoods.isEmpty() && (idx = this.findIndexInVisibleGrid(this.buyGoods, x + 8, y + 86, mouseX, mouseY, this.buyRowOffset)) >= 0) {
            return new int[]{0, idx};
        }
        return null;
    }

    private int findGlobalIndex(TradeMenu.ClientGoodEntry entry) {
        List<TradeMenu.ClientGoodEntry> allGoods = ((TradeMenu)this.menu).getClientGoods();
        for (int i = 0; i < allGoods.size(); ++i) {
            TradeMenu.ClientGoodEntry candidate = allGoods.get(i);
            if (!candidate.id().equals(entry.id()) || candidate.isSelling() != entry.isSelling()) continue;
            return i;
        }
        return -1;
    }

    private int countPlayerItem(TradeMenu.ClientGoodEntry entry) {
        if (this.minecraft == null || this.minecraft.player == null) {
            return 0;
        }
        Inventory inventory = this.minecraft.player.getInventory();
        int total = 0;
        TagKey<Item> tag = this.resolveTag(entry);
        for (int i = 0; i < inventory.getContainerSize(); ++i) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty() || !this.matchesEntry(stack, entry, tag) || TradeScreen.isDamaged(stack)) continue;
            total += stack.getCount();
        }
        return total;
    }

    private boolean playerHasItem(TradeMenu.ClientGoodEntry entry) {
        if (this.minecraft == null || this.minecraft.player == null) {
            return false;
        }
        Inventory inventory = this.minecraft.player.getInventory();
        TagKey<Item> tag = this.resolveTag(entry);
        for (int i = 0; i < inventory.getContainerSize(); ++i) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty() || !this.matchesEntry(stack, entry, tag) || TradeScreen.isDamaged(stack)) continue;
            return true;
        }
        return false;
    }

    private static boolean isDamaged(ItemStack stack) {
        return stack.isDamageableItem() && stack.isDamaged();
    }

    @Nullable
    private TagKey<Item> resolveTag(TradeMenu.ClientGoodEntry entry) {
        if (entry.tagId() == null) {
            return null;
        }
        String raw = entry.tagId();
        String tagPath = raw.startsWith("#") ? raw.substring(1) : raw;
        return TagKey.create((ResourceKey)Registries.ITEM, (ResourceLocation)ResourceLocation.parse((String)tagPath));
    }

    private boolean matchesEntry(ItemStack stack, TradeMenu.ClientGoodEntry entry, @Nullable TagKey<Item> tag) {
        if (tag != null) {
            return stack.is(tag);
        }
        return stack.is(entry.item());
    }

    public boolean isPauseScreen() {
        return false;
    }
}

