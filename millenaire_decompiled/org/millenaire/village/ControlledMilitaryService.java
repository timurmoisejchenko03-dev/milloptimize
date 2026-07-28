/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  net.minecraft.network.protocol.common.custom.CustomPacketPayload
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.neoforged.neoforge.network.PacketDistributor
 */
package org.millenaire.village;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import org.millenaire.culture.Culture;
import org.millenaire.culture.ModCultures;
import org.millenaire.network.ControlledMilitaryPayload;
import org.millenaire.village.Village;
import org.millenaire.village.VillageId;
import org.millenaire.village.VillageManager;
import org.millenaire.village.VillageRelations;
import org.millenaire.village.VillageSavedData;
import org.millenaire.village.panel.MilitaryPanelGenerator;
import org.millenaire.village.panel.PanelLine;

public final class ControlledMilitaryService {
    private ControlledMilitaryService() {
    }

    public static ControlledMilitaryPayload buildPayload(Village village, ServerLevel level) {
        VillageManager vm = VillageSavedData.get(level).getVillageManager();
        ArrayList<ControlledMilitaryPayload.RelationEntry> relations = new ArrayList<ControlledMilitaryPayload.RelationEntry>();
        for (Map.Entry<VillageId, Integer> relEntry : village.getRelations().entrySet()) {
            Village other = vm.getVillage(relEntry.getKey());
            if (other == null || village.getParentVillageId() != null && village.getParentVillageId().equals(other.getId()) || other.getParentVillageId() != null && other.getParentVillageId().equals(village.getId())) continue;
            String otherName = other.getVillageName() != null ? other.getVillageName() : other.getVillageTypeId().getPath();
            Culture otherCulture = ModCultures.getCulture(other.getCultureId());
            String otherCultureName = otherCulture != null ? otherCulture.displayName() : "?";
            int rel = relEntry.getValue();
            String relLabel = VillageRelations.getRelationKey(rel);
            relations.add(new ControlledMilitaryPayload.RelationEntry(relEntry.getKey().uuid().toString(), otherName, otherCultureName, rel, relLabel));
        }
        relations.sort(Comparator.comparing(ControlledMilitaryPayload.RelationEntry::villageName));
        String currentRaidTargetVillageId = village.getRaidTarget() != null ? village.getRaidTarget().uuid().toString() : "";
        boolean raidInProgress = village.getRaidStart() > 0L;
        ArrayList<PanelLine> militaryLines = new ArrayList<PanelLine>(MilitaryPanelGenerator.generateMilitary(village, level).lines());
        String villageName = village.getVillageName() != null ? village.getVillageName() : "";
        String ownerName = village.getOwnerName() != null ? village.getOwnerName() : "";
        return new ControlledMilitaryPayload(village.getId().uuid().toString(), villageName, ownerName, currentRaidTargetVillageId, raidInProgress, relations, militaryLines);
    }

    public static void sendRefresh(ServerPlayer player, ServerLevel level, Village village) {
        PacketDistributor.sendToPlayer((ServerPlayer)player, (CustomPacketPayload)ControlledMilitaryService.buildPayload(village, level), (CustomPacketPayload[])new CustomPacketPayload[0]);
    }
}

