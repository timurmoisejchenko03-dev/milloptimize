/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.ChatFormatting
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.gui.components.Button
 *  net.minecraft.client.gui.components.events.GuiEventListener
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.chat.MutableComponent
 *  net.minecraft.network.protocol.common.custom.CustomPacketPayload
 *  net.neoforged.neoforge.network.PacketDistributor
 */
package org.millenaire.client.gui;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.PacketDistributor;
import org.millenaire.client.gui.AbstractMillenaireScreen;
import org.millenaire.client.gui.MillenaireScreenUtils;
import org.millenaire.client.gui.PanelRenderHelper;
import org.millenaire.client.gui.TravelBookNavHelper;
import org.millenaire.combat.raid.RaidButtonState;
import org.millenaire.item.MoneyHelper;
import org.millenaire.network.BuildingPurchasePayload;
import org.millenaire.network.CropLearningPayload;
import org.millenaire.network.CultureControlPurchasePayload;
import org.millenaire.network.DiplomacyActionPayload;
import org.millenaire.network.HuntingLearningPayload;
import org.millenaire.network.RaidActionPayload;
import org.millenaire.network.VillageChiefPayload;
import org.millenaire.network.VillageScrollPurchasePayload;
import org.millenaire.village.VillageRelations;
import org.millenaire.village.panel.PanelLine;

public class VillageChiefScreen
extends AbstractMillenaireScreen {
    private static final int PARCHMENT_WIDTH = 256;
    private static final int PARCHMENT_HEIGHT = 200;
    private static final int LINES_PER_PAGE = VillageChiefScreen.computeMaxLinesPerPage(200);
    private static final int PREVIEW_LINES_COST = 5;
    private static final int TEXT_WIDTH = VillageChiefScreen.textWidth(256);
    private static final int COLOR_WARN_DARK_ORANGE = 0xCC6600;
    private VillageChiefPayload data;
    private List<PanelLine> contentLines = List.of();
    private int scrollButtonPage = -1;
    private int scrollButtonLineIndex = -1;
    private final List<PurchaseButtonEntry> purchaseButtonEntries = new ArrayList<PurchaseButtonEntry>();
    private final List<Button> purchaseButtons = new ArrayList<Button>();
    private final List<DiplomacyButtonEntry> diplomacyButtonEntries = new ArrayList<DiplomacyButtonEntry>();
    private final List<Button> diplomacyButtons = new ArrayList<Button>();
    private final List<LearningButtonEntry> learningButtonEntries = new ArrayList<LearningButtonEntry>();
    private final List<Button> learningButtons = new ArrayList<Button>();
    private int cultureControlButtonLineIndex = -1;
    private int cultureControlButtonPage = -1;
    @Nullable
    private Button cultureControlButton;
    private final List<RaidButtonEntry> raidButtonEntries = new ArrayList<RaidButtonEntry>();
    private final List<Button> raidButtons = new ArrayList<Button>();

    private static String playerBuildingDisplayName(VillageChiefPayload.PlayerBuildingEntry pb) {
        String key = pb.translationKey();
        if (key == null || key.isEmpty()) {
            return pb.nativeName();
        }
        return Component.translatableWithFallback((String)key, (String)pb.nativeName()).getString();
    }

    public VillageChiefScreen(VillageChiefPayload data) {
        super((Component)Component.translatable((String)"gui.millenaire.chief.title"));
        this.data = data;
    }

    protected void init() {
        MutableComponent label;
        int buttonWidth;
        ButtonPlacement placement;
        Object placement2;
        super.init();
        this.purchaseButtonEntries.clear();
        this.purchaseButtons.clear();
        this.diplomacyButtonEntries.clear();
        this.diplomacyButtons.clear();
        this.learningButtonEntries.clear();
        this.learningButtons.clear();
        this.raidButtonEntries.clear();
        this.raidButtons.clear();
        this.scrollButtonLineIndex = -1;
        this.cultureControlButtonLineIndex = -1;
        this.cultureControlButton = null;
        List<PanelLine> rawLines = this.buildContentLines();
        this.contentLines = PanelRenderHelper.wrapLines(rawLines, this.font, TEXT_WIDTH);
        this.recalcButtonIndices(rawLines);
        this.currentPage = 0;
        int page0Lines = LINES_PER_PAGE - 5;
        int remaining = Math.max(0, this.contentLines.size() - page0Lines);
        this.totalPages = 1 + (remaining > 0 ? PanelRenderHelper.computePageCount(remaining, LINES_PER_PAGE) : 0);
        int parchX = (this.width - 256) / 2;
        int parchY = (this.height - 200) / 2;
        this.initPaginationButtons(parchX, parchY, 256, 200, 16);
        if (this.scrollButtonLineIndex >= 0) {
            placement2 = VillageChiefScreen.placeButton(this.scrollButtonLineIndex, parchY);
            this.scrollButtonPage = ((ButtonPlacement)placement2).page();
            this.addScrollButton(parchX, ((ButtonPlacement)placement2).y());
        }
        for (PurchaseButtonEntry pbe : this.purchaseButtonEntries) {
            placement = VillageChiefScreen.placeButton(pbe.lineIndex, parchY);
            buttonWidth = TEXT_WIDTH;
            label = "GIFT_AVAILABLE".equals(pbe.entry.status()) ? Component.translatable((String)"gui.millenaire.chief.building_gift_buy", (Object[])new Object[]{VillageChiefScreen.playerBuildingDisplayName(pbe.entry)}) : Component.translatable((String)"gui.millenaire.chief.building_buy", (Object[])new Object[]{VillageChiefScreen.playerBuildingDisplayName(pbe.entry), pbe.entry.price()});
            int finalBtnPage = placement.page();
            String planSetId = pbe.entry.planSetId();
            Button purchaseBtn = Button.builder((Component)label, button -> PacketDistributor.sendToServer((CustomPacketPayload)new BuildingPurchasePayload(this.data.villageId(), planSetId), (CustomPacketPayload[])new CustomPacketPayload[0])).bounds(parchX + 16, placement.y(), buttonWidth, 16).build();
            purchaseBtn.visible = this.currentPage == finalBtnPage;
            this.addRenderableWidget((GuiEventListener)purchaseBtn);
            this.purchaseButtons.add(purchaseBtn);
        }
        for (DiplomacyButtonEntry dbe : this.diplomacyButtonEntries) {
            placement = VillageChiefScreen.placeButton(dbe.lineIndex, parchY);
            buttonWidth = TEXT_WIDTH;
            label = dbe.isPraise ? Component.translatable((String)"gui.millenaire.chief.praise_btn") : Component.translatable((String)"gui.millenaire.chief.slander_btn");
            int finalBtnPage2 = placement.page();
            String targetId = dbe.targetVillageId;
            boolean praise = dbe.isPraise;
            Button dipBtn = Button.builder((Component)label, button -> PacketDistributor.sendToServer((CustomPacketPayload)new DiplomacyActionPayload(this.data.villageId(), targetId, praise), (CustomPacketPayload[])new CustomPacketPayload[0])).bounds(parchX + 16, placement.y(), buttonWidth, 16).build();
            dipBtn.visible = this.currentPage == finalBtnPage2;
            this.addRenderableWidget((GuiEventListener)dipBtn);
            this.diplomacyButtons.add(dipBtn);
        }
        for (LearningButtonEntry lbe : this.learningButtonEntries) {
            placement = VillageChiefScreen.placeButton(lbe.lineIndex, parchY);
            buttonWidth = TEXT_WIDTH;
            String priceStr = MoneyHelper.formatPrice(512);
            MutableComponent label2 = lbe.isCrop ? Component.translatable((String)"gui.millenaire.chief.crop_learn", (Object[])new Object[]{priceStr}) : Component.translatable((String)"gui.millenaire.chief.hunting_learn", (Object[])new Object[]{priceStr});
            int finalBtnPage3 = placement.page();
            String key = lbe.key;
            boolean isCrop = lbe.isCrop;
            Button learnBtn = Button.builder((Component)label2, button -> {
                if (isCrop) {
                    PacketDistributor.sendToServer((CustomPacketPayload)new CropLearningPayload(this.data.villageId(), key), (CustomPacketPayload[])new CustomPacketPayload[0]);
                } else {
                    PacketDistributor.sendToServer((CustomPacketPayload)new HuntingLearningPayload(this.data.villageId(), key), (CustomPacketPayload[])new CustomPacketPayload[0]);
                }
            }).bounds(parchX + 16, placement.y(), buttonWidth, 16).build();
            learnBtn.visible = this.currentPage == finalBtnPage3;
            this.addRenderableWidget((GuiEventListener)learnBtn);
            this.learningButtons.add(learnBtn);
        }
        for (RaidButtonEntry rbe : this.raidButtonEntries) {
            placement = VillageChiefScreen.placeButton(rbe.lineIndex, parchY);
            buttonWidth = TEXT_WIDTH;
            label = rbe.action == 1 ? Component.translatable((String)"gui.millenaire.chief.raid_cancel_btn") : Component.translatable((String)"gui.millenaire.chief.raid_plan_btn");
            int raidBtnPage = placement.page();
            String targetId = rbe.targetVillageId;
            int action = rbe.action;
            Button raidBtn = Button.builder((Component)label, button -> PacketDistributor.sendToServer((CustomPacketPayload)new RaidActionPayload(this.data.villageId(), targetId, action), (CustomPacketPayload[])new CustomPacketPayload[0])).bounds(parchX + 16, placement.y(), buttonWidth, 16).build();
            raidBtn.visible = this.currentPage == raidBtnPage;
            this.addRenderableWidget((GuiEventListener)raidBtn);
            this.raidButtons.add(raidBtn);
        }
        if (this.cultureControlButtonLineIndex >= 0) {
            placement2 = VillageChiefScreen.placeButton(this.cultureControlButtonLineIndex, parchY);
            this.cultureControlButtonPage = ((ButtonPlacement)placement2).page();
            int buttonWidth2 = TEXT_WIDTH;
            MutableComponent label3 = Component.translatable((String)"gui.millenaire.chief.control_get");
            this.cultureControlButton = Button.builder((Component)label3, button -> PacketDistributor.sendToServer((CustomPacketPayload)new CultureControlPurchasePayload(this.data.villageId()), (CustomPacketPayload[])new CustomPacketPayload[0])).bounds(parchX + 16, ((ButtonPlacement)placement2).y(), buttonWidth2, 16).build();
            this.cultureControlButton.visible = this.currentPage == this.cultureControlButtonPage;
            this.addRenderableWidget((GuiEventListener)this.cultureControlButton);
        }
        this.updatePaginationButtons();
    }

    public void applyUpdate(VillageChiefPayload.ChiefDynamic dynamic) {
        int savedPage = this.currentPage;
        this.data = new VillageChiefPayload(this.data.identity(), dynamic);
        this.rebuildWidgets();
        this.currentPage = Math.max(0, Math.min(savedPage, this.totalPages - 1));
        this.updatePaginationButtons();
    }

    private static ButtonPlacement placeButton(int lineIndex, int parchY) {
        int page0Lines = LINES_PER_PAGE - 5;
        if (lineIndex < page0Lines) {
            int contentY = parchY + 42 + 55;
            return new ButtonPlacement(0, contentY + lineIndex * 11);
        }
        int adjustedIdx = lineIndex - page0Lines;
        int lineOnPage = adjustedIdx % LINES_PER_PAGE;
        int contentY = parchY + 42;
        return new ButtonPlacement(1 + adjustedIdx / LINES_PER_PAGE, contentY + lineOnPage * 11);
    }

    private static int pageForLine(int lineIndex) {
        int page0Lines = LINES_PER_PAGE - 5;
        if (lineIndex < page0Lines) {
            return 0;
        }
        return 1 + (lineIndex - page0Lines) / LINES_PER_PAGE;
    }

    private void addScrollButton(int parchX, int buttonY) {
        int buttonWidth = TEXT_WIDTH;
        MutableComponent label = Component.translatable((String)"gui.millenaire.chief.scroll_buy", (Object[])new Object[]{128});
        Button scrollBtn = Button.builder((Component)label, button -> PacketDistributor.sendToServer((CustomPacketPayload)new VillageScrollPurchasePayload(this.data.villageId()), (CustomPacketPayload[])new CustomPacketPayload[0])).bounds(parchX + 16, buttonY, buttonWidth, 16).build();
        this.addRenderableWidget((GuiEventListener)scrollBtn);
        scrollBtn.visible = this.currentPage == this.scrollButtonPage;
    }

    @Override
    protected void updatePaginationButtons() {
        int i;
        super.updatePaginationButtons();
        for (GuiEventListener w : this.children()) {
            String msg;
            Button btn;
            if (!(w instanceof Button) || (btn = (Button)w) == this.prevButton || btn == this.nextButton || !(msg = btn.getMessage().getString()).contains(String.valueOf(128))) continue;
            btn.visible = this.currentPage == this.scrollButtonPage;
        }
        for (i = 0; i < this.purchaseButtons.size(); ++i) {
            if (i >= this.purchaseButtonEntries.size()) continue;
            int lineIdx = this.purchaseButtonEntries.get((int)i).lineIndex;
            this.purchaseButtons.get((int)i).visible = this.currentPage == VillageChiefScreen.pageForLine(lineIdx);
        }
        for (i = 0; i < this.diplomacyButtons.size(); ++i) {
            if (i >= this.diplomacyButtonEntries.size()) continue;
            int lineIdx = this.diplomacyButtonEntries.get((int)i).lineIndex;
            this.diplomacyButtons.get((int)i).visible = this.currentPage == VillageChiefScreen.pageForLine(lineIdx);
        }
        for (i = 0; i < this.learningButtons.size(); ++i) {
            if (i >= this.learningButtonEntries.size()) continue;
            int lineIdx = this.learningButtonEntries.get((int)i).lineIndex;
            this.learningButtons.get((int)i).visible = this.currentPage == VillageChiefScreen.pageForLine(lineIdx);
        }
        if (this.cultureControlButton != null) {
            this.cultureControlButton.visible = this.currentPage == this.cultureControlButtonPage;
        }
        for (i = 0; i < this.raidButtons.size(); ++i) {
            if (i >= this.raidButtonEntries.size()) continue;
            int lineIdx = this.raidButtonEntries.get((int)i).lineIndex;
            this.raidButtons.get((int)i).visible = this.currentPage == VillageChiefScreen.pageForLine(lineIdx);
        }
    }

    private List<PanelLine> buildContentLines() {
        String itemName;
        String roleStr;
        ArrayList<PanelLine> lines = new ArrayList<PanelLine>();
        String string = roleStr = this.data.roleName().startsWith("role.") ? Component.translatable((String)this.data.roleName()).getString() : this.data.roleName();
        if (!this.data.chiefTypeKey().isEmpty()) {
            String cultureKey = this.data.cultureId();
            PanelLine.PanelNavTarget chiefTarget = new PanelLine.PanelNavTarget("VILLAGER_DETAIL", cultureKey, "villagers", this.data.chiefTypeKey());
            lines.add(PanelLine.clickableText(this.data.chiefName() + ", " + roleStr, chiefTarget));
        } else {
            lines.add(PanelLine.bold(this.data.chiefName() + ", " + roleStr));
        }
        lines.add(PanelLine.empty());
        lines.add(PanelLine.text(Component.translatable((String)"gui.millenaire.village").getString() + " : " + this.data.villageName()));
        lines.add(PanelLine.text(Component.translatable((String)"gui.millenaire.culture").getString() + " : " + this.data.cultureName()));
        lines.add(PanelLine.empty());
        lines.add(PanelLine.text(Component.translatable((String)"gui.millenaire.chief.villagers").getString() + " : " + this.data.totalVillagers()));
        lines.add(PanelLine.text(Component.translatable((String)"gui.millenaire.chief.buildings").getString() + " : " + this.data.totalBuildings() + " (" + this.data.completeBuildings() + " " + Component.translatable((String)"gui.millenaire.chief.complete").getString() + ", " + this.data.underConstructionBuildings() + " " + Component.translatable((String)"gui.millenaire.chief.under_construction").getString() + ")"));
        lines.add(PanelLine.empty());
        String repLabel = MillenaireScreenUtils.resolveReputationLabel(this.data.reputationLabel());
        int repColor = MillenaireScreenUtils.getReputationColor(this.data.reputation());
        lines.add(PanelLine.colored(Component.translatable((String)"gui.millenaire.reputation").getString() + " : " + this.data.reputation() + " (" + repLabel + ")", repColor));
        lines.add(PanelLine.separator());
        if (this.data.playerReputation() < 8192) {
            lines.add(PanelLine.colored(Component.translatable((String)"gui.millenaire.chief.scroll_no_rep").getString(), -13421773));
        } else if (this.data.playerMoney() < 128) {
            int manque = 128 - this.data.playerMoney();
            lines.add(PanelLine.colored(Component.translatable((String)"gui.millenaire.chief.scroll_no_money", (Object[])new Object[]{manque}).getString(), 0xCC6600));
        } else {
            this.scrollButtonLineIndex = lines.size();
            PanelLine.addButtonPlaceholder(lines);
        }
        lines.add(PanelLine.separator());
        List<VillageChiefPayload.PlayerBuildingEntry> pbs = this.data.playerBuildings();
        if (!pbs.isEmpty()) {
            lines.add(PanelLine.colored(Component.translatable((String)"gui.millenaire.chief.building_purchase").getString(), -11193600));
            for (VillageChiefPayload.PlayerBuildingEntry pb : pbs) {
                String pbName = VillageChiefScreen.playerBuildingDisplayName(pb);
                switch (pb.status()) {
                    case "BUILT": {
                        lines.add(PanelLine.colored("  " + pbName + " \u2014 " + Component.translatable((String)"gui.millenaire.chief.building_already_built").getString(), 0x555555));
                        break;
                    }
                    case "BOUGHT": {
                        lines.add(PanelLine.colored("  " + pbName + " \u2014 " + Component.translatable((String)"gui.millenaire.chief.building_already_bought").getString(), 0xCCCC00));
                        break;
                    }
                    case "NO_REP": {
                        lines.add(PanelLine.colored("  " + pbName + " \u2014 " + Component.translatable((String)"gui.millenaire.chief.building_no_rep", (Object[])new Object[]{pb.reputation()}).getString(), 0xCC6600));
                        break;
                    }
                    case "NO_MONEY": {
                        int manque = pb.price() - this.data.playerMoney();
                        lines.add(PanelLine.colored("  " + pbName + " \u2014 " + Component.translatable((String)"gui.millenaire.chief.building_no_money", (Object[])new Object[]{manque}).getString(), 0xCC6600));
                        break;
                    }
                    case "AVAILABLE": 
                    case "GIFT_AVAILABLE": {
                        this.purchaseButtonEntries.add(new PurchaseButtonEntry(lines.size(), pb));
                        PanelLine.addButtonPlaceholder(lines);
                    }
                }
            }
            lines.add(PanelLine.separator());
        }
        if (!this.data.cropOffers().isEmpty()) {
            lines.add(PanelLine.colored(Component.translatable((String)"gui.millenaire.chief.crops_known").getString(), -11193600));
            lines.add(PanelLine.empty());
            for (VillageChiefPayload.LearningOffer offer : this.data.cropOffers()) {
                itemName = Component.translatable((String)offer.itemName()).getString();
                switch (offer.status()) {
                    case "LEARNED": {
                        lines.add(PanelLine.colored("  " + Component.translatable((String)"gui.millenaire.chief.crop_already_learned", (Object[])new Object[]{itemName}).getString(), 0x555555));
                        break;
                    }
                    case "NO_REP": {
                        lines.add(PanelLine.colored("  " + Component.translatable((String)"gui.millenaire.chief.crop_no_rep", (Object[])new Object[]{itemName}).getString(), 0xCC6600));
                        break;
                    }
                    case "NO_MONEY": {
                        lines.add(PanelLine.colored("  " + Component.translatable((String)"gui.millenaire.chief.crop_no_money", (Object[])new Object[]{itemName}).getString(), 0xCC6600));
                        break;
                    }
                    case "AVAILABLE": {
                        lines.add(PanelLine.colored("  " + Component.translatable((String)"gui.millenaire.chief.crop_available", (Object[])new Object[]{itemName}).getString(), -13421773));
                        this.learningButtonEntries.add(new LearningButtonEntry(lines.size(), offer.key(), true));
                        PanelLine.addButtonPlaceholder(lines);
                    }
                }
            }
            lines.add(PanelLine.empty());
        }
        if (!this.data.huntingOffers().isEmpty()) {
            lines.add(PanelLine.colored(Component.translatable((String)"gui.millenaire.chief.hunting_known").getString(), -11193600));
            lines.add(PanelLine.empty());
            for (VillageChiefPayload.LearningOffer offer : this.data.huntingOffers()) {
                itemName = Component.translatable((String)offer.itemName()).getString();
                switch (offer.status()) {
                    case "LEARNED": {
                        lines.add(PanelLine.colored("  " + Component.translatable((String)"gui.millenaire.chief.hunting_already_learned", (Object[])new Object[]{itemName}).getString(), 0x555555));
                        break;
                    }
                    case "NO_REP": {
                        lines.add(PanelLine.colored("  " + Component.translatable((String)"gui.millenaire.chief.hunting_no_rep", (Object[])new Object[]{itemName}).getString(), 0xCC6600));
                        break;
                    }
                    case "NO_MONEY": {
                        lines.add(PanelLine.colored("  " + Component.translatable((String)"gui.millenaire.chief.hunting_no_money", (Object[])new Object[]{itemName}).getString(), 0xCC6600));
                        break;
                    }
                    case "AVAILABLE": {
                        lines.add(PanelLine.colored("  " + Component.translatable((String)"gui.millenaire.chief.hunting_available", (Object[])new Object[]{itemName}).getString(), -13421773));
                        this.learningButtonEntries.add(new LearningButtonEntry(lines.size(), offer.key(), false));
                        PanelLine.addButtonPlaceholder(lines);
                    }
                }
            }
            lines.add(PanelLine.empty());
        }
        if (this.data.hasCultureControl()) {
            lines.add(PanelLine.colored(Component.translatable((String)"gui.millenaire.chief.control_already", (Object[])new Object[]{this.data.cultureName()}).getString(), 0x555555));
        } else if (this.data.cultureControlAvailable()) {
            lines.add(PanelLine.colored(Component.translatable((String)"gui.millenaire.chief.control_available", (Object[])new Object[]{this.data.cultureName()}).getString(), -13421773));
            this.cultureControlButtonLineIndex = lines.size();
            PanelLine.addButtonPlaceholder(lines);
        } else {
            lines.add(PanelLine.colored(Component.translatable((String)"gui.millenaire.chief.control_no_rep", (Object[])new Object[]{this.data.cultureName()}).getString(), 0xCC6600));
        }
        lines.add(PanelLine.separator());
        lines.add(PanelLine.colored(Component.translatable((String)"gui.millenaire.chief.diplomacy").getString(), -11193600));
        if (this.data.relationEntries().isEmpty()) {
            lines.add(PanelLine.colored(Component.translatable((String)"gui.millenaire.chief.diplomacy_none").getString(), -13421773));
        } else {
            lines.add(PanelLine.colored(Component.translatable((String)"gui.millenaire.chief.diplomacy_points", (Object[])new Object[]{this.data.diplomacyPoints()}).getString(), -13421773));
            boolean canAct = this.data.diplomacyPoints() > 0 && this.data.playerReputation() > 0;
            for (VillageChiefPayload.RelationEntry re : this.data.relationEntries()) {
                ChatFormatting cf = VillageRelations.getRelationColor(re.relation());
                int relColor = cf.getColor() != null ? cf.getColor() : -13421773;
                String relLabel = Component.translatable((String)re.relationLabel()).getString();
                lines.add(PanelLine.colored("  " + re.villageName() + " (" + re.cultureName() + ") \u2014 " + relLabel, relColor));
                if (!canAct) continue;
                if (re.relation() < 100) {
                    this.diplomacyButtonEntries.add(new DiplomacyButtonEntry(lines.size(), re.villageId(), true));
                    PanelLine.addButtonPlaceholder(lines);
                }
                if (re.relation() <= -100) continue;
                this.diplomacyButtonEntries.add(new DiplomacyButtonEntry(lines.size(), re.villageId(), false));
                PanelLine.addButtonPlaceholder(lines);
            }
        }
        lines.add(PanelLine.separator());
        lines.add(PanelLine.colored(Component.translatable((String)"gui.millenaire.chief.military_title").getString(), -11193600));
        lines.add(PanelLine.empty());
        if (this.data.controlledByPlayer() && !this.data.relationEntries().isEmpty()) {
            String currentRaidTarget = this.data.currentRaidTargetVillageId().isEmpty() ? null : this.data.currentRaidTargetVillageId();
            for (VillageChiefPayload.RelationEntry re : this.data.relationEntries()) {
                RaidButtonState state = RaidButtonState.compute(currentRaidTarget, this.data.raidInProgress(), re.villageId());
                String relLabel = Component.translatable((String)re.relationLabel()).getString();
                lines.add(PanelLine.colored("  " + re.villageName() + " (" + re.cultureName() + ") \u2014 " + relLabel, -13421773));
                switch (state) {
                    case PLAN: {
                        this.raidButtonEntries.add(new RaidButtonEntry(lines.size(), re.villageId(), 0));
                        PanelLine.addButtonPlaceholder(lines);
                        break;
                    }
                    case CANCEL: {
                        this.raidButtonEntries.add(new RaidButtonEntry(lines.size(), re.villageId(), 1));
                        PanelLine.addButtonPlaceholder(lines);
                        lines.add(PanelLine.colored(Component.translatable((String)"gui.millenaire.chief.raid_planned").getString(), 0xFF5555));
                        break;
                    }
                    case IN_PROGRESS_HERE: {
                        lines.add(PanelLine.colored(Component.translatable((String)"gui.millenaire.chief.raid_in_progress").getString(), 0xAA0000));
                        break;
                    }
                    case IN_PROGRESS_OTHER: {
                        lines.add(PanelLine.colored(Component.translatable((String)"gui.millenaire.chief.other_raid_in_progress").getString(), 0xAA0000));
                        break;
                    }
                    case PLANNED_OTHER: {
                        this.raidButtonEntries.add(new RaidButtonEntry(lines.size(), re.villageId(), 0));
                        PanelLine.addButtonPlaceholder(lines);
                        lines.add(PanelLine.colored(Component.translatable((String)"gui.millenaire.chief.other_raid_planned").getString(), 0xFF5555));
                    }
                }
                lines.add(PanelLine.empty());
            }
        }
        lines.addAll(this.data.militaryLines());
        lines.add(PanelLine.separator());
        lines.add(PanelLine.colored(Component.translatable((String)"gui.millenaire.chief.help_title").getString(), -11193600));
        lines.add(PanelLine.empty());
        lines.add(PanelLine.text(Component.translatable((String)"gui.millenaire.chief.relation_help").getString()));
        return lines;
    }

    private void recalcButtonIndices(List<PanelLine> rawLines) {
        List<PanelLine> wrapped = this.contentLines;
        int[] rawToWrapped = new int[rawLines.size()];
        int wrappedIdx = 0;
        for (int rawIdx = 0; rawIdx < rawLines.size(); ++rawIdx) {
            rawToWrapped[rawIdx] = wrappedIdx++;
            PanelLine rawLine = rawLines.get(rawIdx);
            if (rawLine.isSeparator() || rawLine.text().isEmpty() || rawLine.isColumns()) {
                ++wrappedIdx;
                continue;
            }
            String string = PanelRenderHelper.resolveDisplayText(rawLine);
            if (this.font.width(string) <= TEXT_WIDTH) continue;
            String[] arrstring = string.split(" ");
            StringBuilder current = new StringBuilder();
            int segments = 0;
            for (String word : arrstring) {
                String test;
                String string2 = test = current.isEmpty() ? word : String.valueOf(current) + " " + word;
                if (this.font.width(test) > TEXT_WIDTH && !current.isEmpty()) {
                    ++segments;
                    current = new StringBuilder(word);
                    continue;
                }
                current = new StringBuilder(test);
            }
            if (!current.isEmpty()) {
                ++segments;
            }
            wrappedIdx += segments;
        }
        if (this.scrollButtonLineIndex >= 0 && this.scrollButtonLineIndex < rawToWrapped.length) {
            this.scrollButtonLineIndex = rawToWrapped[this.scrollButtonLineIndex];
        }
        ArrayList<PurchaseButtonEntry> remapped = new ArrayList<PurchaseButtonEntry>();
        for (PurchaseButtonEntry purchaseButtonEntry : this.purchaseButtonEntries) {
            if (purchaseButtonEntry.lineIndex >= rawToWrapped.length) continue;
            remapped.add(new PurchaseButtonEntry(rawToWrapped[purchaseButtonEntry.lineIndex], purchaseButtonEntry.entry));
        }
        this.purchaseButtonEntries.clear();
        this.purchaseButtonEntries.addAll(remapped);
        ArrayList<DiplomacyButtonEntry> remappedDip = new ArrayList<DiplomacyButtonEntry>();
        for (DiplomacyButtonEntry diplomacyButtonEntry : this.diplomacyButtonEntries) {
            if (diplomacyButtonEntry.lineIndex >= rawToWrapped.length) continue;
            remappedDip.add(new DiplomacyButtonEntry(rawToWrapped[diplomacyButtonEntry.lineIndex], diplomacyButtonEntry.targetVillageId, diplomacyButtonEntry.isPraise));
        }
        this.diplomacyButtonEntries.clear();
        this.diplomacyButtonEntries.addAll(remappedDip);
        ArrayList<LearningButtonEntry> arrayList = new ArrayList<LearningButtonEntry>();
        for (LearningButtonEntry lbe : this.learningButtonEntries) {
            if (lbe.lineIndex >= rawToWrapped.length) continue;
            arrayList.add(new LearningButtonEntry(rawToWrapped[lbe.lineIndex], lbe.key, lbe.isCrop));
        }
        this.learningButtonEntries.clear();
        this.learningButtonEntries.addAll(arrayList);
        if (this.cultureControlButtonLineIndex >= 0 && this.cultureControlButtonLineIndex < rawToWrapped.length) {
            this.cultureControlButtonLineIndex = rawToWrapped[this.cultureControlButtonLineIndex];
        }
        ArrayList<RaidButtonEntry> arrayList2 = new ArrayList<RaidButtonEntry>();
        for (RaidButtonEntry rbe : this.raidButtonEntries) {
            if (rbe.lineIndex >= rawToWrapped.length) continue;
            arrayList2.add(new RaidButtonEntry(rawToWrapped[rbe.lineIndex], rbe.targetVillageId, rbe.action));
        }
        this.raidButtonEntries.clear();
        this.raidButtonEntries.addAll(arrayList2);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int contentY;
            int maxLines;
            int startLine;
            int parchX = (this.width - 256) / 2;
            int parchY = (this.height - 200) / 2;
            int contentX = parchX + 16;
            int page0Lines = LINES_PER_PAGE - 5;
            if (this.currentPage == 0) {
                startLine = 0;
                maxLines = page0Lines;
                contentY = parchY + 42 + 55;
            } else {
                startLine = page0Lines + (this.currentPage - 1) * LINES_PER_PAGE;
                maxLines = LINES_PER_PAGE;
                contentY = parchY + 42;
            }
            PanelLine.PanelNavTarget target = PanelRenderHelper.getClickedNavTarget(this.contentLines, contentX, contentY, TEXT_WIDTH, startLine, maxLines, mouseX, mouseY);
            if (target != null) {
                TravelBookNavHelper.openFromNavTarget(this, target);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int maxLines;
        int startLine;
        graphics.fill(0, 0, this.width, this.height, 0x60000000);
        int parchX = (this.width - 256) / 2;
        int parchY = (this.height - 200) / 2;
        PanelRenderHelper.renderTexturedBackground(graphics, PanelRenderHelper.VILLAGE_CHIEF_TEXTURE, parchX, parchY, 256, 200);
        int y = this.renderParchmentHeader(graphics, (Component)Component.translatable((String)"gui.millenaire.chief.title"), parchX, parchY, 256);
        if (this.currentPage == 0) {
            y = MillenaireScreenUtils.renderVillagerPreview(graphics, parchX, parchY, 256, 16, y, mouseX, mouseY, this.data.entityId());
        }
        int page0Lines = LINES_PER_PAGE - 5;
        if (this.currentPage == 0) {
            startLine = 0;
            maxLines = page0Lines;
        } else {
            startLine = page0Lines + (this.currentPage - 1) * LINES_PER_PAGE;
            maxLines = LINES_PER_PAGE;
        }
        PanelRenderHelper.renderPanelLines(graphics, this.font, this.contentLines, parchX + 16, y, TEXT_WIDTH, startLine, maxLines);
        this.renderPageCounter(graphics, parchX, parchY, 256, 200);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private record ButtonPlacement(int page, int y) {
    }

    private record PurchaseButtonEntry(int lineIndex, VillageChiefPayload.PlayerBuildingEntry entry) {
    }

    private record DiplomacyButtonEntry(int lineIndex, String targetVillageId, boolean isPraise) {
    }

    private record LearningButtonEntry(int lineIndex, String key, boolean isCrop) {
    }

    private record RaidButtonEntry(int lineIndex, String targetVillageId, int action) {
    }
}

