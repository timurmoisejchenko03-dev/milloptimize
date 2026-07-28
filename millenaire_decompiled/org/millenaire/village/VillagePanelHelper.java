/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Vec3i
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.level.block.state.BlockState
 */
package org.millenaire.village;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import org.millenaire.block.VillagePanelBlockEntity;
import org.millenaire.building.BuildingInstance;
import org.millenaire.building.SpecialPoint;
import org.millenaire.village.Village;
import org.millenaire.village.panel.PanelContentGenerator;
import org.millenaire.village.panel.PanelPlacer;
import org.millenaire.village.panel.PanelType;

public final class VillagePanelHelper {
    private VillagePanelHelper() {
    }

    public static void updatePanelDisplayLines(ServerLevel level, Village village) {
        for (BuildingInstance building : village.getBuildings()) {
            if (!building.isOperational()) continue;
            for (SpecialPoint sp : building.getPointsByType("signPos")) {
                PanelContentGenerator.DisplayData displayData;
                VillagePanelBlockEntity panel;
                Player player2;
                BlockPos panelPos = sp.pos();
                if (!level.isLoaded(panelPos)) continue;
                boolean playerNearby = false;
                for (Player player2 : level.players()) {
                    if (!player2.blockPosition().closerThan((Vec3i)panelPos, 48.0)) continue;
                    playerNearby = true;
                    break;
                }
                if (!playerNearby) continue;
                if (level.getBlockState(panelPos).isAir()) {
                    PanelPlacer.recreatePanel(level, village, building, panelPos);
                }
                if (!((player2 = level.getBlockEntity(panelPos)) instanceof VillagePanelBlockEntity) || (panel = (VillagePanelBlockEntity)player2).getPanelType() == PanelType.HALL_OF_FAME || (displayData = PanelContentGenerator.generateDisplayLines(panel.getPanelType(), village, building, level, panel.getSignIndex())).displayLines().equals(panel.getDisplayLines())) continue;
                panel.updateDisplayLines(displayData.displayLines());
                BlockState state = level.getBlockState(panelPos);
                level.sendBlockUpdated(panelPos, state, state, 3);
            }
        }
    }
}

