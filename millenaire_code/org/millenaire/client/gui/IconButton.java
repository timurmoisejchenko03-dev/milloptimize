/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.Font
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.gui.components.Button
 *  net.minecraft.client.gui.components.Button$OnPress
 *  net.minecraft.client.gui.components.Tooltip
 *  net.minecraft.network.chat.Component
 *  net.minecraft.world.item.ItemStack
 */
package org.millenaire.client.gui;

import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public class IconButton
extends Button {
    @Nullable
    private final ItemStack leftIcon;
    @Nullable
    private final ItemStack rightIcon;
    private final boolean iconOnly;

    public IconButton(int x, int y, int width, int height, ItemStack icon, Component tooltip, Button.OnPress onPress) {
        super(x, y, width, height, (Component)Component.empty(), onPress, DEFAULT_NARRATION);
        this.leftIcon = icon;
        this.rightIcon = null;
        this.iconOnly = true;
        this.setTooltip(Tooltip.create((Component)tooltip));
    }

    public IconButton(int x, int y, int width, int height, Component text, @Nullable ItemStack leftIcon, @Nullable ItemStack rightIcon, Button.OnPress onPress) {
        super(x, y, width, height, text, onPress, DEFAULT_NARRATION);
        this.leftIcon = leftIcon;
        this.rightIcon = rightIcon;
        this.iconOnly = false;
    }

    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderWidget(graphics, mouseX, mouseY, partialTick);
        if (this.iconOnly) {
            if (this.leftIcon != null) {
                int iconX = this.getX() + (this.getWidth() - 16) / 2;
                int iconY = this.getY() + (this.getHeight() - 16) / 2;
                graphics.renderItem(this.leftIcon, iconX, iconY);
            }
        } else {
            int iconY = this.getY() + (this.getHeight() - 16) / 2;
            int leftOffset = 0;
            int rightOffset = 0;
            if (this.leftIcon != null) {
                graphics.renderItem(this.leftIcon, this.getX() + 2, iconY);
                leftOffset = 18;
            }
            if (this.rightIcon != null) {
                graphics.renderItem(this.rightIcon, this.getX() + this.getWidth() - 18, iconY);
                rightOffset = 18;
            }
            if (leftOffset > 0 || rightOffset > 0) {
                int textX = this.getX() + leftOffset;
                int textWidth = this.getWidth() - leftOffset - rightOffset;
                int textY = this.getY() + (this.getHeight() - 8) / 2;
                Font font = Minecraft.getInstance().font;
                String text = this.getMessage().getString();
                int strWidth = font.width(text);
                int drawX = textX + (textWidth - strWidth) / 2;
                graphics.drawString(font, text, drawX, textY, this.getFGColor(), false);
            }
        }
    }
}

