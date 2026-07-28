/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.components.toasts.Toast
 *  net.minecraft.client.gui.screens.Screen
 *  net.minecraft.client.resources.language.I18n
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.chat.MutableComponent
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.inventory.AbstractContainerMenu
 *  net.minecraft.world.item.ItemStack
 *  net.neoforged.neoforge.network.handling.IPayloadContext
 *  org.slf4j.Logger
 */
package org.millenaire.network;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.millenaire.client.ClientLanguageCache;
import org.millenaire.client.ClientQuestCache;
import org.millenaire.client.FireplaceSmokeHandler;
import org.millenaire.client.gui.ControlledMilitaryScreen;
import org.millenaire.client.gui.ControlledProjectsScreen;
import org.millenaire.client.gui.HireScreen;
import org.millenaire.client.gui.ImportTableScreen;
import org.millenaire.client.gui.InfoPanelScreen;
import org.millenaire.client.gui.NegationWandScreen;
import org.millenaire.client.gui.NewBuildingProjectScreen;
import org.millenaire.client.gui.NewVillageScreen;
import org.millenaire.client.gui.PanelRenderHelper;
import org.millenaire.client.gui.QuestScreen;
import org.millenaire.client.gui.TravelBookScreen;
import org.millenaire.client.gui.UnlockingToast;
import org.millenaire.client.gui.VillageBookScreen;
import org.millenaire.client.gui.VillageChiefScreen;
import org.millenaire.client.gui.VillagePanelScreen;
import org.millenaire.client.gui.VillagerInfoScreen;
import org.millenaire.client.gui.WandDebugScreen;
import org.millenaire.commerce.TradeMenu;
import org.millenaire.language.DisplayNameResolver;
import org.millenaire.language.SentenceRenderer;
import org.millenaire.language.SpeechResolver;
import org.millenaire.network.BuildingProjectListPayload;
import org.millenaire.network.ControlledMilitaryPayload;
import org.millenaire.network.ControlledProjectsPayload;
import org.millenaire.network.FireplacePositionsPayload;
import org.millenaire.network.ImportTableCostsPayload;
import org.millenaire.network.ImportTableSyncPayload;
import org.millenaire.network.InfoPanelContentPayload;
import org.millenaire.network.NegationWandPayload;
import org.millenaire.network.OpenHireScreenPayload;
import org.millenaire.network.PanelContentPayload;
import org.millenaire.network.QuestInstanceDestroyPayload;
import org.millenaire.network.QuestInstanceSyncPayload;
import org.millenaire.network.QuestOpenPayload;
import org.millenaire.network.QuestResultTextPayload;
import org.millenaire.network.SpeechChatPayload;
import org.millenaire.network.TradePayload;
import org.millenaire.network.TradeStockUpdatePayload;
import org.millenaire.network.TravelBookContentPayload;
import org.millenaire.network.UnlockingToastPayload;
import org.millenaire.network.VillageBookPayload;
import org.millenaire.network.VillageChiefPayload;
import org.millenaire.network.VillageChiefUpdatePayload;
import org.millenaire.network.VillageTypeListPayload;
import org.millenaire.network.VillagerInfoPayload;
import org.millenaire.network.WandDebugMenuPayload;
import org.slf4j.Logger;

public final class ClientPayloadHandler {
    private static final Logger LOGGER = LogUtils.getLogger();

    private ClientPayloadHandler() {
    }

    public static void handleVillagerInfo(VillagerInfoPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ResourceLocation cultureId = ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)payload.cultureKey());
            ClientLanguageCache.update(cultureId, payload.languageScore());
            Minecraft.getInstance().setScreen((Screen)new VillagerInfoScreen(payload));
        });
    }

    public static void handleUnlockingToast(UnlockingToastPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            MutableComponent title = Component.literal((String)payload.title());
            MutableComponent message = Component.translatable((String)("travelbook." + ClientPayloadHandler.descKey(payload.category())), (Object[])new Object[]{payload.cultureName(), payload.nbUnlocked(), payload.nbTotal()});
            ItemStack icon = payload.iconId() == null || payload.iconId().isEmpty() ? ItemStack.EMPTY : PanelRenderHelper.resolveItemIcon(payload.iconId());
            Minecraft.getInstance().getToasts().addToast((Toast)new UnlockingToast((Component)title, (Component)message, icon));
        });
    }

    private static String descKey(String category) {
        return switch (category) {
            case "village" -> "unlockedvillage";
            case "villager" -> "unlockedvillager";
            case "tradegood" -> "unlockedtradegood";
            default -> "unlockedbuilding";
        };
    }

    public static void handleOpenHire(OpenHireScreenPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> Minecraft.getInstance().setScreen((Screen)new HireScreen(payload)));
    }

    public static void handleVillageChief(VillageChiefPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> Minecraft.getInstance().setScreen((Screen)new VillageChiefScreen(payload)));
    }

    public static void handleVillageChiefUpdate(VillageChiefUpdatePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Screen patt0$temp = Minecraft.getInstance().screen;
            if (patt0$temp instanceof VillageChiefScreen) {
                VillageChiefScreen screen = (VillageChiefScreen)patt0$temp;
                screen.applyUpdate(payload.dynamic());
            }
        });
    }

    public static void handleTrade(TradePayload payload, IPayloadContext context) {
        LOGGER.debug("TradePayload received (legacy stub, ignored) \u2014 trade uses menu system");
    }

    public static void handleTradeStockUpdate(TradeStockUpdatePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            AbstractContainerMenu patt0$temp;
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && (patt0$temp = mc.player.containerMenu) instanceof TradeMenu) {
                TradeMenu tradeMenu = (TradeMenu)patt0$temp;
                if (tradeMenu.containerId == payload.containerId()) {
                    tradeMenu.updateStocks(payload.stocks(), payload.donationMode());
                }
            }
        });
    }

    public static void handleVillageBook(VillageBookPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> Minecraft.getInstance().setScreen((Screen)new VillageBookScreen(payload)));
    }

    public static void handleImportTableSync(ImportTableSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> Minecraft.getInstance().setScreen((Screen)new ImportTableScreen(payload)));
    }

    public static void handleImportTableCosts(ImportTableCostsPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ImportTableScreen.applyCosts(payload));
    }

    public static void handleTravelBook(TravelBookContentPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> TravelBookScreen.applyContent(payload));
    }

    public static void handleFireplacePositions(FireplacePositionsPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> FireplaceSmokeHandler.updatePositions(payload.villageId(), payload.villageCenter(), payload.positions()));
    }

    public static void handleSpeechChat(SpeechChatPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            String prefix = payload.villagerName() + ": ";
            ResourceLocation payloadCultureId = ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)payload.cultureKey());
            ClientLanguageCache.update(payloadCultureId, payload.languageScore());
            String[] speechParts = SpeechResolver.resolve(payload.speechRef(), payloadCultureId, payload.languageScore());
            String nativeText = speechParts[0];
            String translation = speechParts[1];
            if (translation == null && payload.vanillaFallbackKey() != null && !payload.vanillaFallbackKey().isEmpty()) {
                String cultureSpecificKey = payload.vanillaFallbackKey() + "." + payload.cultureKey();
                Object resolvedKey = I18n.exists((String)cultureSpecificKey) ? cultureSpecificKey : payload.vanillaFallbackKey();
                String vanilla = Component.translatable((String)resolvedKey, (Object[])new Object[]{payload.villagerName()}).getString();
                double ratio = SentenceRenderer.languageRatio(payload.languageScore());
                if (ratio >= 1.0) {
                    translation = vanilla;
                } else if (ratio > 0.0) {
                    translation = SentenceRenderer.maskTranslation(vanilla, ratio);
                }
            }
            if (DisplayNameResolver.equivalent(nativeText, translation)) {
                translation = null;
            }
            if (nativeText != null) {
                MutableComponent msg = Component.empty().append((Component)Component.literal((String)prefix).withStyle(s -> s.withColor(0xFFFFFF))).append((Component)Component.literal((String)nativeText).withStyle(s -> s.withColor(0x5555FF)));
                if (translation != null) {
                    msg.append((Component)Component.literal((String)" ").withStyle(s -> s.withColor(0xFFFFFF))).append((Component)Component.literal((String)translation).withStyle(s -> s.withColor(0xAA0000)));
                }
                mc.player.sendSystemMessage((Component)msg);
            }
        });
    }

    public static void handleVillageTypeList(VillageTypeListPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> Minecraft.getInstance().setScreen((Screen)new NewVillageScreen(payload)));
    }

    public static void handleInfoPanel(InfoPanelContentPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> Minecraft.getInstance().setScreen((Screen)new InfoPanelScreen(payload)));
    }

    public static void handlePanelContent(PanelContentPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (payload.hasMapData()) {
                Minecraft.getInstance().setScreen((Screen)new VillagePanelScreen(payload.toContent(), payload));
            } else {
                Minecraft.getInstance().setScreen((Screen)new VillagePanelScreen(payload.toContent()));
            }
        });
    }

    public static void handleQuestSync(QuestInstanceSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientQuestCache.update(payload));
    }

    public static void handleQuestDestroy(QuestInstanceDestroyPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientQuestCache.remove(payload.uniqueId()));
    }

    public static void handleQuestResult(QuestResultTextPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            Screen patt0$temp = mc.screen;
            if (patt0$temp instanceof QuestScreen) {
                QuestScreen questScreen = (QuestScreen)patt0$temp;
                questScreen.onQuestResult(payload.resultText(), payload.isSuccess());
            } else {
                LOGGER.debug("Quest result received but no QuestScreen open: questId={}, success={}", (Object)payload.questUniqueId(), (Object)payload.isSuccess());
            }
        });
    }

    public static void handleQuestOpen(QuestOpenPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> Minecraft.getInstance().setScreen((Screen)new QuestScreen(payload)));
    }

    public static void handleNegationWand(NegationWandPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> Minecraft.getInstance().setScreen((Screen)new NegationWandScreen(payload)));
    }

    public static void handleBuildingProjectList(BuildingProjectListPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> Minecraft.getInstance().setScreen((Screen)new NewBuildingProjectScreen(payload)));
    }

    public static void handleControlledProjects(ControlledProjectsPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> Minecraft.getInstance().setScreen((Screen)new ControlledProjectsScreen(payload)));
    }

    public static void handleControlledMilitary(ControlledMilitaryPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> Minecraft.getInstance().setScreen((Screen)new ControlledMilitaryScreen(payload)));
    }

    public static void handleWandDebugMenu(WandDebugMenuPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> Minecraft.getInstance().setScreen((Screen)new WandDebugScreen(payload)));
    }
}

