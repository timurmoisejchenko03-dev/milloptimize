/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.screens.Screen
 *  net.minecraft.network.protocol.common.custom.CustomPacketPayload
 *  net.neoforged.neoforge.network.PacketDistributor
 */
package org.millenaire.client.gui;

import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.PacketDistributor;
import org.millenaire.client.gui.HelpScreen;
import org.millenaire.client.gui.TravelBookScreen;
import org.millenaire.network.TravelBookRequestPayload;
import org.millenaire.village.TravelBookScreenState;
import org.millenaire.village.panel.PanelLine;

public final class TravelBookNavHelper {
    private TravelBookNavHelper() {
    }

    public static void openHelp(@Nullable Screen callingScreen) {
        Minecraft.getInstance().setScreen((Screen)new HelpScreen(callingScreen));
    }

    public static void openCulture(@Nullable Screen callingScreen, String cultureKey) {
        TravelBookScreen.setCallingScreen(callingScreen);
        PacketDistributor.sendToServer((CustomPacketPayload)new TravelBookRequestPayload(TravelBookScreenState.CULTURE, cultureKey, "", "", 0), (CustomPacketPayload[])new CustomPacketPayload[0]);
    }

    public static void openHome(@Nullable Screen callingScreen) {
        TravelBookScreen.setCallingScreen(callingScreen);
        PacketDistributor.sendToServer((CustomPacketPayload)new TravelBookRequestPayload(TravelBookScreenState.HOME, "", "", "", 0), (CustomPacketPayload[])new CustomPacketPayload[0]);
    }

    public static void openVillagerDetail(@Nullable Screen callingScreen, String cultureKey, String villagerTypeKey) {
        TravelBookScreen.setCallingScreen(callingScreen);
        PacketDistributor.sendToServer((CustomPacketPayload)new TravelBookRequestPayload(TravelBookScreenState.VILLAGER_DETAIL, cultureKey, "villagers", villagerTypeKey, 0), (CustomPacketPayload[])new CustomPacketPayload[0]);
    }

    public static void openBuilding(@Nullable Screen callingScreen, String cultureKey, String buildingKey) {
        TravelBookScreen.setCallingScreen(callingScreen);
        PacketDistributor.sendToServer((CustomPacketPayload)new TravelBookRequestPayload(TravelBookScreenState.BUILDING_DETAIL, cultureKey, "", buildingKey, 0), (CustomPacketPayload[])new CustomPacketPayload[0]);
    }

    public static void openVillageType(@Nullable Screen callingScreen, String cultureKey, String villageTypeKey) {
        TravelBookScreen.setCallingScreen(callingScreen);
        PacketDistributor.sendToServer((CustomPacketPayload)new TravelBookRequestPayload(TravelBookScreenState.VILLAGE_DETAIL, cultureKey, "", villageTypeKey, 0), (CustomPacketPayload[])new CustomPacketPayload[0]);
    }

    public static void openFromNavTarget(@Nullable Screen callingScreen, PanelLine.PanelNavTarget target) {
        TravelBookScreenState state;
        TravelBookScreen.setCallingScreen(callingScreen);
        try {
            state = TravelBookScreenState.valueOf(target.targetStateName());
        }
        catch (IllegalArgumentException e) {
            state = TravelBookScreenState.HOME;
        }
        PacketDistributor.sendToServer((CustomPacketPayload)new TravelBookRequestPayload(state, target.cultureKey(), target.categoryKey(), target.itemKey(), 0), (CustomPacketPayload[])new CustomPacketPayload[0]);
    }
}

