/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.gui.components.Button
 *  net.minecraft.client.gui.components.events.GuiEventListener
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.protocol.common.custom.CustomPacketPayload
 *  net.neoforged.neoforge.network.PacketDistributor
 */
package org.millenaire.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.PacketDistributor;
import org.millenaire.client.gui.AbstractMillenaireScreen;
import org.millenaire.client.gui.PanelRenderHelper;
import org.millenaire.network.WandDebugActionPayload;
import org.millenaire.network.WandDebugMenuPayload;

public class WandDebugScreen
extends AbstractMillenaireScreen {
    private static final int PARCHMENT_WIDTH = 204;
    private static final int PARCHMENT_HEIGHT = 220;
    private static final int BUTTON_WIDTH = 160;
    private static final int BUTTON_HEIGHT = 16;
    private static final int BUTTON_SPACING = 2;
    private final WandDebugMenuPayload data;

    public WandDebugScreen(WandDebugMenuPayload data) {
        super((Component)Component.translatable((String)"gui.millenaire.wand_debug.title"));
        this.data = data;
    }

    protected void init() {
        super.init();
        int parchX = (this.width - 204) / 2;
        int parchY = (this.height - 220) / 2;
        int buttonX = parchX + 22;
        int buttonY = parchY + 42 + 4;
        for (WandDebugMenuPayload.ActionEntry action : this.data.actions()) {
            this.addRenderableWidget((GuiEventListener)Button.builder((Component)Component.translatable((String)action.translationKey()), btn -> {
                PacketDistributor.sendToServer((CustomPacketPayload)new WandDebugActionPayload(action.id(), this.data.targetEntityId(), this.data.targetPos()), (CustomPacketPayload[])new CustomPacketPayload[0]);
                this.onClose();
            }).bounds(buttonX, buttonY, 160, 16).build());
            buttonY += 18;
        }
    }

    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, 0x60000000);
        int parchX = (this.width - 204) / 2;
        int parchY = (this.height - 220) / 2;
        PanelRenderHelper.renderParchmentBackground(graphics, parchX, parchY, 204, 220);
        this.renderParchmentHeader(graphics, (Component)Component.literal((String)this.data.headerTitle()), parchX, parchY, 204);
        super.render(graphics, mouseX, mouseY, partialTick);
    }
}

