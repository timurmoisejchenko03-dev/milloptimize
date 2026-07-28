/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.gui.components.Button
 *  net.minecraft.client.gui.components.events.GuiEventListener
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.chat.FormattedText
 *  net.minecraft.network.chat.MutableComponent
 *  net.minecraft.network.protocol.common.custom.CustomPacketPayload
 *  net.minecraft.util.FormattedCharSequence
 *  net.neoforged.neoforge.network.PacketDistributor
 */
package org.millenaire.client.gui;

import java.util.Objects;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.network.PacketDistributor;
import org.millenaire.client.gui.AbstractMillenaireScreen;
import org.millenaire.client.gui.PanelRenderHelper;
import org.millenaire.network.NegationWandConfirmPayload;
import org.millenaire.network.NegationWandPayload;

public class NegationWandScreen
extends AbstractMillenaireScreen {
    private static final int PARCHMENT_WIDTH = 204;
    private static final int PARCHMENT_HEIGHT = 220;
    private final NegationWandPayload payload;

    public NegationWandScreen(NegationWandPayload payload) {
        super((Component)Component.translatable((String)"negationwand.title"));
        this.payload = payload;
    }

    protected void init() {
        super.init();
        int parchX = (this.width - 204) / 2;
        int parchY = (this.height - 220) / 2;
        int buttonY = parchY + 220 - 15 - 25;
        int centerX = parchX + 102;
        int buttonWidth = 80;
        int buttonGap = 10;
        this.addRenderableWidget((GuiEventListener)Button.builder((Component)Component.translatable((String)"negationwand.confirm"), btn -> {
            PacketDistributor.sendToServer((CustomPacketPayload)new NegationWandConfirmPayload(this.payload.villageId()), (CustomPacketPayload[])new CustomPacketPayload[0]);
            this.onClose();
        }).bounds(centerX - buttonWidth - buttonGap / 2, buttonY, buttonWidth, 20).build());
        this.addRenderableWidget((GuiEventListener)Button.builder((Component)Component.translatable((String)"negationwand.cancel"), btn -> this.onClose()).bounds(centerX + buttonGap / 2, buttonY, buttonWidth, 20).build());
    }

    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, 0x60000000);
        int parchX = (this.width - 204) / 2;
        int parchY = (this.height - 220) / 2;
        PanelRenderHelper.renderParchmentBackground(graphics, parchX, parchY, 204, 220);
        int y = this.renderParchmentHeader(graphics, this.getTitle(), parchX, parchY, 204);
        String displayName = this.payload.villageName().isEmpty() ? this.payload.villageTypeId() : this.payload.villageName();
        MutableComponent warningText = Component.translatable((String)"negationwand.confirmmessage", (Object[])new Object[]{displayName});
        int textW = 172;
        int textX = parchX + 16;
        for (FormattedCharSequence line : this.font.split((FormattedText)warningText, textW)) {
            graphics.drawString(this.font, line, textX, y, 4141088, false);
            Objects.requireNonNull(this.font);
            y += 9 + 2;
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }
}

