/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  net.neoforged.fml.loading.FMLEnvironment
 *  net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent
 *  net.neoforged.neoforge.network.registration.PayloadRegistrar
 */
package org.millenaire.network;

import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.millenaire.network.BuildingForgetRequestPayload;
import org.millenaire.network.BuildingProjectCancelRequestPayload;
import org.millenaire.network.BuildingProjectListPayload;
import org.millenaire.network.BuildingProjectRequestPayload;
import org.millenaire.network.BuildingPurchasePayload;
import org.millenaire.network.BuildingUpgradeToggleRequestPayload;
import org.millenaire.network.ClientPayloadHandler;
import org.millenaire.network.ControlledMilitaryPayload;
import org.millenaire.network.ControlledProjectsPayload;
import org.millenaire.network.ControlledRelationPayload;
import org.millenaire.network.CropLearningPayload;
import org.millenaire.network.CultureControlPurchasePayload;
import org.millenaire.network.DiplomacyActionPayload;
import org.millenaire.network.FireplacePositionsPayload;
import org.millenaire.network.HireActionPayload;
import org.millenaire.network.HuntingLearningPayload;
import org.millenaire.network.ImportTableActionHandler;
import org.millenaire.network.ImportTableActionPayload;
import org.millenaire.network.ImportTableCostsPayload;
import org.millenaire.network.ImportTableSyncPayload;
import org.millenaire.network.InfoPanelContentPayload;
import org.millenaire.network.InfoPanelRequestPayload;
import org.millenaire.network.NegationWandConfirmPayload;
import org.millenaire.network.NegationWandPayload;
import org.millenaire.network.OpenHireScreenPayload;
import org.millenaire.network.PanelContentPayload;
import org.millenaire.network.QuestCompleteStepPayload;
import org.millenaire.network.QuestInstanceDestroyPayload;
import org.millenaire.network.QuestInstanceSyncPayload;
import org.millenaire.network.QuestOpenPayload;
import org.millenaire.network.QuestRefusePayload;
import org.millenaire.network.QuestResultTextPayload;
import org.millenaire.network.RaidActionPayload;
import org.millenaire.network.SpeechChatPayload;
import org.millenaire.network.ToggleStancePayload;
import org.millenaire.network.TradePayload;
import org.millenaire.network.TradeStockUpdatePayload;
import org.millenaire.network.TravelBookContentPayload;
import org.millenaire.network.TravelBookRequestPayload;
import org.millenaire.network.UnlockingToastPayload;
import org.millenaire.network.VillageBookPayload;
import org.millenaire.network.VillageChiefPayload;
import org.millenaire.network.VillageChiefUpdatePayload;
import org.millenaire.network.VillageCreationRequestPayload;
import org.millenaire.network.VillageScrollPurchasePayload;
import org.millenaire.network.VillageTypeListPayload;
import org.millenaire.network.VillagerInfoPayload;
import org.millenaire.network.WandDebugActionPayload;
import org.millenaire.network.WandDebugMenuPayload;
import org.millenaire.village.BuildingPurchaseService;
import org.millenaire.village.VillageBookService;

public final class ModPayloads {
    private ModPayloads() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("2");
        if (FMLEnvironment.dist.isClient()) {
            ClientHandlers.register(registrar);
        } else {
            registrar.playToClient(VillagerInfoPayload.TYPE, VillagerInfoPayload.STREAM_CODEC, (p, c) -> {});
            registrar.playToClient(VillageChiefPayload.TYPE, VillageChiefPayload.STREAM_CODEC, (p, c) -> {});
            registrar.playToClient(VillageChiefUpdatePayload.TYPE, VillageChiefUpdatePayload.STREAM_CODEC, (p, c) -> {});
            registrar.playToClient(TradePayload.TYPE, TradePayload.STREAM_CODEC, (p, c) -> {});
            registrar.playToClient(TradeStockUpdatePayload.TYPE, TradeStockUpdatePayload.STREAM_CODEC, (p, c) -> {});
            registrar.playToClient(PanelContentPayload.TYPE, PanelContentPayload.STREAM_CODEC, (p, c) -> {});
            registrar.playToClient(VillageBookPayload.TYPE, VillageBookPayload.STREAM_CODEC, (p, c) -> {});
            registrar.playToClient(ImportTableSyncPayload.TYPE, ImportTableSyncPayload.STREAM_CODEC, (p, c) -> {});
            registrar.playToClient(ImportTableCostsPayload.TYPE, ImportTableCostsPayload.STREAM_CODEC, (p, c) -> {});
            registrar.playToClient(TravelBookContentPayload.TYPE, TravelBookContentPayload.STREAM_CODEC, (p, c) -> {});
            registrar.playToClient(FireplacePositionsPayload.TYPE, FireplacePositionsPayload.STREAM_CODEC, (p, c) -> {});
            registrar.playToClient(SpeechChatPayload.TYPE, SpeechChatPayload.STREAM_CODEC, (p, c) -> {});
            registrar.playToClient(VillageTypeListPayload.TYPE, VillageTypeListPayload.STREAM_CODEC, (p, c) -> {});
            registrar.playToClient(InfoPanelContentPayload.TYPE, InfoPanelContentPayload.STREAM_CODEC, (p, c) -> {});
            registrar.playToClient(QuestInstanceSyncPayload.TYPE, QuestInstanceSyncPayload.STREAM_CODEC, (p, c) -> {});
            registrar.playToClient(QuestInstanceDestroyPayload.TYPE, QuestInstanceDestroyPayload.STREAM_CODEC, (p, c) -> {});
            registrar.playToClient(QuestResultTextPayload.TYPE, QuestResultTextPayload.STREAM_CODEC, (p, c) -> {});
            registrar.playToClient(QuestOpenPayload.TYPE, QuestOpenPayload.STREAM_CODEC, (p, c) -> {});
            registrar.playToClient(WandDebugMenuPayload.TYPE, WandDebugMenuPayload.STREAM_CODEC, (p, c) -> {});
            registrar.playToClient(NegationWandPayload.TYPE, NegationWandPayload.STREAM_CODEC, (p, c) -> {});
            registrar.playToClient(BuildingProjectListPayload.TYPE, BuildingProjectListPayload.STREAM_CODEC, (p, c) -> {});
            registrar.playToClient(ControlledProjectsPayload.TYPE, ControlledProjectsPayload.STREAM_CODEC, (p, c) -> {});
            registrar.playToClient(ControlledMilitaryPayload.TYPE, ControlledMilitaryPayload.STREAM_CODEC, (p, c) -> {});
            registrar.playToClient(OpenHireScreenPayload.TYPE, OpenHireScreenPayload.STREAM_CODEC, (p, c) -> {});
            registrar.playToClient(UnlockingToastPayload.TYPE, UnlockingToastPayload.STREAM_CODEC, (p, c) -> {});
        }
        registrar.playToServer(VillageScrollPurchasePayload.TYPE, VillageScrollPurchasePayload.STREAM_CODEC, VillageBookService::handleScrollPurchasePacket);
        registrar.playToServer(BuildingPurchasePayload.TYPE, BuildingPurchasePayload.STREAM_CODEC, BuildingPurchaseService::handlePurchasePacket);
        registrar.playToServer(ImportTableActionPayload.TYPE, ImportTableActionPayload.STREAM_CODEC, ImportTableActionHandler::handleAction);
        registrar.playToServer(TravelBookRequestPayload.TYPE, TravelBookRequestPayload.STREAM_CODEC, TravelBookRequestPayload::handleOnServer);
        registrar.playToServer(DiplomacyActionPayload.TYPE, DiplomacyActionPayload.STREAM_CODEC, DiplomacyActionPayload::handleOnServer);
        registrar.playToServer(VillageCreationRequestPayload.TYPE, VillageCreationRequestPayload.STREAM_CODEC, VillageCreationRequestPayload::handleOnServer);
        registrar.playToServer(CropLearningPayload.TYPE, CropLearningPayload.STREAM_CODEC, CropLearningPayload::handleOnServer);
        registrar.playToServer(HuntingLearningPayload.TYPE, HuntingLearningPayload.STREAM_CODEC, HuntingLearningPayload::handleOnServer);
        registrar.playToServer(CultureControlPurchasePayload.TYPE, CultureControlPurchasePayload.STREAM_CODEC, CultureControlPurchasePayload::handleOnServer);
        registrar.playToServer(InfoPanelRequestPayload.TYPE, InfoPanelRequestPayload.STREAM_CODEC, InfoPanelRequestPayload::handleOnServer);
        registrar.playToServer(QuestCompleteStepPayload.TYPE, QuestCompleteStepPayload.STREAM_CODEC, QuestCompleteStepPayload::handle);
        registrar.playToServer(QuestRefusePayload.TYPE, QuestRefusePayload.STREAM_CODEC, QuestRefusePayload::handleOnServer);
        registrar.playToServer(WandDebugActionPayload.TYPE, WandDebugActionPayload.STREAM_CODEC, WandDebugActionPayload::handleOnServer);
        registrar.playToServer(NegationWandConfirmPayload.TYPE, NegationWandConfirmPayload.STREAM_CODEC, NegationWandConfirmPayload::handleOnServer);
        registrar.playToServer(BuildingProjectRequestPayload.TYPE, BuildingProjectRequestPayload.STREAM_CODEC, BuildingProjectRequestPayload::handleOnServer);
        registrar.playToServer(BuildingUpgradeToggleRequestPayload.TYPE, BuildingUpgradeToggleRequestPayload.STREAM_CODEC, BuildingUpgradeToggleRequestPayload::handleOnServer);
        registrar.playToServer(BuildingProjectCancelRequestPayload.TYPE, BuildingProjectCancelRequestPayload.STREAM_CODEC, BuildingProjectCancelRequestPayload::handleOnServer);
        registrar.playToServer(BuildingForgetRequestPayload.TYPE, BuildingForgetRequestPayload.STREAM_CODEC, BuildingForgetRequestPayload::handleOnServer);
        registrar.playToServer(RaidActionPayload.TYPE, RaidActionPayload.STREAM_CODEC, RaidActionPayload::handleOnServer);
        registrar.playToServer(ControlledRelationPayload.TYPE, ControlledRelationPayload.STREAM_CODEC, ControlledRelationPayload::handleOnServer);
        registrar.playToServer(HireActionPayload.TYPE, HireActionPayload.STREAM_CODEC, HireActionPayload::handleOnServer);
        registrar.playToServer(ToggleStancePayload.TYPE, ToggleStancePayload.STREAM_CODEC, ToggleStancePayload::handleOnServer);
    }

    private static final class ClientHandlers {
        private ClientHandlers() {
        }

        static void register(PayloadRegistrar registrar) {
            registrar.playToClient(VillagerInfoPayload.TYPE, VillagerInfoPayload.STREAM_CODEC, ClientPayloadHandler::handleVillagerInfo);
            registrar.playToClient(VillageChiefPayload.TYPE, VillageChiefPayload.STREAM_CODEC, ClientPayloadHandler::handleVillageChief);
            registrar.playToClient(VillageChiefUpdatePayload.TYPE, VillageChiefUpdatePayload.STREAM_CODEC, ClientPayloadHandler::handleVillageChiefUpdate);
            registrar.playToClient(TradePayload.TYPE, TradePayload.STREAM_CODEC, ClientPayloadHandler::handleTrade);
            registrar.playToClient(TradeStockUpdatePayload.TYPE, TradeStockUpdatePayload.STREAM_CODEC, ClientPayloadHandler::handleTradeStockUpdate);
            registrar.playToClient(PanelContentPayload.TYPE, PanelContentPayload.STREAM_CODEC, ClientPayloadHandler::handlePanelContent);
            registrar.playToClient(VillageBookPayload.TYPE, VillageBookPayload.STREAM_CODEC, ClientPayloadHandler::handleVillageBook);
            registrar.playToClient(ImportTableSyncPayload.TYPE, ImportTableSyncPayload.STREAM_CODEC, ClientPayloadHandler::handleImportTableSync);
            registrar.playToClient(ImportTableCostsPayload.TYPE, ImportTableCostsPayload.STREAM_CODEC, ClientPayloadHandler::handleImportTableCosts);
            registrar.playToClient(TravelBookContentPayload.TYPE, TravelBookContentPayload.STREAM_CODEC, ClientPayloadHandler::handleTravelBook);
            registrar.playToClient(FireplacePositionsPayload.TYPE, FireplacePositionsPayload.STREAM_CODEC, ClientPayloadHandler::handleFireplacePositions);
            registrar.playToClient(SpeechChatPayload.TYPE, SpeechChatPayload.STREAM_CODEC, ClientPayloadHandler::handleSpeechChat);
            registrar.playToClient(VillageTypeListPayload.TYPE, VillageTypeListPayload.STREAM_CODEC, ClientPayloadHandler::handleVillageTypeList);
            registrar.playToClient(InfoPanelContentPayload.TYPE, InfoPanelContentPayload.STREAM_CODEC, ClientPayloadHandler::handleInfoPanel);
            registrar.playToClient(QuestInstanceSyncPayload.TYPE, QuestInstanceSyncPayload.STREAM_CODEC, ClientPayloadHandler::handleQuestSync);
            registrar.playToClient(QuestInstanceDestroyPayload.TYPE, QuestInstanceDestroyPayload.STREAM_CODEC, ClientPayloadHandler::handleQuestDestroy);
            registrar.playToClient(QuestResultTextPayload.TYPE, QuestResultTextPayload.STREAM_CODEC, ClientPayloadHandler::handleQuestResult);
            registrar.playToClient(QuestOpenPayload.TYPE, QuestOpenPayload.STREAM_CODEC, ClientPayloadHandler::handleQuestOpen);
            registrar.playToClient(WandDebugMenuPayload.TYPE, WandDebugMenuPayload.STREAM_CODEC, ClientPayloadHandler::handleWandDebugMenu);
            registrar.playToClient(NegationWandPayload.TYPE, NegationWandPayload.STREAM_CODEC, ClientPayloadHandler::handleNegationWand);
            registrar.playToClient(BuildingProjectListPayload.TYPE, BuildingProjectListPayload.STREAM_CODEC, ClientPayloadHandler::handleBuildingProjectList);
            registrar.playToClient(ControlledProjectsPayload.TYPE, ControlledProjectsPayload.STREAM_CODEC, ClientPayloadHandler::handleControlledProjects);
            registrar.playToClient(ControlledMilitaryPayload.TYPE, ControlledMilitaryPayload.STREAM_CODEC, ClientPayloadHandler::handleControlledMilitary);
            registrar.playToClient(OpenHireScreenPayload.TYPE, OpenHireScreenPayload.STREAM_CODEC, ClientPayloadHandler::handleOpenHire);
            registrar.playToClient(UnlockingToastPayload.TYPE, UnlockingToastPayload.STREAM_CODEC, ClientPayloadHandler::handleUnlockingToast);
        }
    }
}

