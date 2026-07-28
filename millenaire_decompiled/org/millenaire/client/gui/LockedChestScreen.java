/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
 *  net.minecraft.network.chat.Component
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.entity.player.Inventory
 *  net.minecraft.world.inventory.AbstractContainerMenu
 */
package org.millenaire.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.millenaire.commerce.LockedChestMenu;

public class LockedChestScreen
extends AbstractContainerScreen<LockedChestMenu> {
    private static final ResourceLocation CHEST_TEXTURE = ResourceLocation.withDefaultNamespace((String)"textures/gui/container/generic_54.png");
    private static final int LOCKED_COLOR = -43691;
    private final int numRows;

    public LockedChestScreen(LockedChestMenu menu, Inventory playerInventory, Component title) {
        super((AbstractContainerMenu)menu, playerInventory, title);
        this.numRows = menu.getNumRows();
        this.imageHeight = 114 + this.numRows * 18;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        guiGraphics.blit(CHEST_TEXTURE, x, y, 0, 0, this.imageWidth, this.numRows * 18 + 17);
        guiGraphics.blit(CHEST_TEXTURE, x, y + this.numRows * 18 + 17, 0, 126, this.imageWidth, 96);
    }

    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
        String indicatorKey = ((LockedChestMenu)this.menu).isLocked() ? "block.millenaire.locked_chest.indicator_locked" : "block.millenaire.locked_chest.indicator_unlocked";
        int indicatorColor = ((LockedChestMenu)this.menu).isLocked() ? -43691 : -11141291;
        String lockText = Component.translatable((String)indicatorKey).getString();
        int textWidth = this.font.width(lockText);
        guiGraphics.drawString(this.font, lockText, this.leftPos + this.imageWidth - textWidth - 4, this.topPos + 5, indicatorColor, false);
    }

    public boolean isPauseScreen() {
        return false;
    }
}

