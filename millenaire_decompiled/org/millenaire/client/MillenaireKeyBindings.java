/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.platform.InputConstants$Type
 *  net.minecraft.client.KeyMapping
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.screens.Screen
 *  net.minecraft.network.protocol.common.custom.CustomPacketPayload
 *  net.neoforged.api.distmarker.Dist
 *  net.neoforged.bus.api.SubscribeEvent
 *  net.neoforged.fml.common.EventBusSubscriber
 *  net.neoforged.neoforge.client.event.ClientTickEvent$Post
 *  net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent
 *  net.neoforged.neoforge.common.NeoForge
 *  net.neoforged.neoforge.network.PacketDistributor
 */
package org.millenaire.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import org.millenaire.network.InfoPanelRequestPayload;
import org.millenaire.network.ToggleStancePayload;

@EventBusSubscriber(modid="millenaire", value={Dist.CLIENT})
public final class MillenaireKeyBindings {
    private static final String CATEGORY = "key.categories.millenaire";
    public static final KeyMapping KEY_VILLAGES = new KeyMapping("key.millenaire.villages", InputConstants.Type.KEYSYM, 86, "key.categories.millenaire");
    public static final KeyMapping KEY_INFO_PANEL = new KeyMapping("key.millenaire.info_panel", InputConstants.Type.KEYSYM, 77, "key.categories.millenaire");
    public static final KeyMapping KEY_STANCE = new KeyMapping("key.millenaire.escorts", InputConstants.Type.KEYSYM, 71, "key.categories.millenaire");

    private MillenaireKeyBindings() {
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(KEY_VILLAGES);
        event.register(KEY_INFO_PANEL);
        event.register(KEY_STANCE);
        NeoForge.EVENT_BUS.addListener(MillenaireKeyBindings::onClientTick);
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) {
            return;
        }
        while (KEY_VILLAGES.consumeClick()) {
            mc.player.connection.sendCommand("millenaire villages");
        }
        while (KEY_INFO_PANEL.consumeClick()) {
            PacketDistributor.sendToServer((CustomPacketPayload)new InfoPanelRequestPayload(), (CustomPacketPayload[])new CustomPacketPayload[0]);
        }
        while (KEY_STANCE.consumeClick()) {
            boolean aggressive = !Screen.hasShiftDown();
            PacketDistributor.sendToServer((CustomPacketPayload)new ToggleStancePayload(aggressive), (CustomPacketPayload[])new CustomPacketPayload[0]);
        }
    }
}

