/*
 * Decompiled with CFR 0.152.
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
import org.millenaire.block.FirePitMenu;

public class FirePitScreen
extends AbstractContainerScreen<FirePitMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)"textures/gui/firepit.png");
    private static final int[][] ARROWS = new int[][]{{77, 22, 23, 31, 8}, {71, 28, 37, 14, 16}, {77, 42, 23, 31, 8}};
    private static final int[] FIRE = new int[]{81, 54};

    public FirePitScreen(FirePitMenu menu, Inventory playerInventory, Component title) {
        super((AbstractContainerMenu)menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 175;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);
        if (((FirePitMenu)this.menu).getBurnTime() > 0) {
            int burn = ((FirePitMenu)this.menu).getBurnLeftScaled(13);
            guiGraphics.blit(TEXTURE, x + FIRE[0], y + FIRE[1] + 12 - burn, this.imageWidth, 12 - burn, 14, burn + 1);
        }
        for (int i = 0; i < 3; ++i) {
            int arrowX = ARROWS[i][0];
            int arrowY = ARROWS[i][1];
            int arrowLen = ARROWS[i][2];
            int arrowTexY = ARROWS[i][3];
            int arrowHeight = ARROWS[i][4];
            int progress = ((FirePitMenu)this.menu).getCookProgressScaled(i, arrowLen);
            guiGraphics.blit(TEXTURE, x + arrowX, y + arrowY, this.imageWidth, arrowTexY, progress, arrowHeight);
        }
    }

    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, 8, 6, 0x404040, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 94, 0x404040, false);
    }

    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}

