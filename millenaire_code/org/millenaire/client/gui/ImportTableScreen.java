/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.gui.components.Button
 *  net.minecraft.client.gui.components.EditBox
 *  net.minecraft.client.gui.components.events.GuiEventListener
 *  net.minecraft.client.gui.screens.Screen
 *  net.minecraft.client.server.IntegratedServer
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.chat.MutableComponent
 *  net.minecraft.network.protocol.common.custom.CustomPacketPayload
 *  net.minecraft.resources.ResourceLocation
 *  net.neoforged.neoforge.network.PacketDistributor
 */
package org.millenaire.client.gui;

import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;
import org.millenaire.building.BuildingExporter;
import org.millenaire.building.BuildingPlanSet;
import org.millenaire.client.gui.AbstractMillenaireScreen;
import org.millenaire.client.gui.PanelRenderHelper;
import org.millenaire.culture.Culture;
import org.millenaire.culture.ModCultures;
import org.millenaire.network.ImportTableActionPayload;
import org.millenaire.network.ImportTableCostsPayload;
import org.millenaire.network.ImportTableSyncPayload;

public class ImportTableScreen
extends AbstractMillenaireScreen {
    private static final int PANEL_WIDTH = 256;
    private static final int PANEL_HEIGHT = 220;
    private static final int PADDING = 10;
    private static final int LINE_HEIGHT = 14;
    private static final int BUTTON_HEIGHT = 20;
    private static final int MAX_LIST_ITEMS = 7;
    private static final int COLOR_WHITE = -13421773;
    private static final int COLOR_GRAY = -10066347;
    private static final int COLOR_YELLOW = -11193600;
    private static final int COLOR_GREEN = -13408717;
    private static final String[] ORIENTATION_NAMES = new String[]{"N", "E", "S", "W"};
    private final ImportTableSyncPayload syncData;
    private String buildingId;
    private String variant;
    private String cultureKey;
    private String parentBuildingId;
    private int length;
    private int plotWidth;
    private int upgradeLevel;
    private int startingLevel;
    private int plotHeight;
    private int orientation;
    private int numberOfUpgrades = 0;
    private boolean preBuilt = false;
    private boolean clearGround;
    private boolean exportSnow;
    private boolean importMockBlocks;
    private boolean convertToPreserveGround;
    private boolean isMainTable;
    private EditBox buildingNameField;
    private EditBox lengthField;
    private EditBox plotWidthField;
    private EditBox startingLevelField;
    private EditBox plotHeightField;
    private EditBox numberOfUpgradesField;
    private String buildingName = "";
    private ScreenState currentState = ScreenState.HOME;
    private final Deque<ScreenState> previousScreens = new ArrayDeque<ScreenState>();
    private String selectedCultureKey = "";
    private String selectedCategory = "";
    private String selectedBuildingId = "";
    private String selectedVariant = "a";
    private String selectedExportBuildingId = "";
    private String selectedExportVariant = "a";
    private List<String> cultureCategories = List.of();
    private List<BuildingPlanSet> categoryBuildings = List.of();
    private List<Integer> buildingLevels = List.of();
    private List<String> exportBuildings = List.of();
    private List<Integer> exportLevels = List.of();
    private BuildingPlanSet subParentPlanSet;
    private String subParentVariant = "a";
    private BuildingPlanSet currentSubBuildingPlanSet;
    private String selectedSubVariant = "a";
    private List<BuildingPlanSet> referencedSubBuildings = List.of();
    private List<Integer> subBuildingLevels = List.of();
    private BuildingPlanSet currentBuildingPlanSet;
    private List<String> availableExportVariants = List.of();
    private List<ImportTableCostsPayload.Entry> costs = null;
    private String costsLabel = "";

    public ImportTableScreen(ImportTableSyncPayload payload) {
        super((Component)Component.translatable((String)"gui.millenaire.importtable.title"));
        this.syncData = payload;
        this.buildingId = payload.buildingId();
        this.variant = payload.variant();
        this.cultureKey = payload.cultureKey();
        this.parentBuildingId = payload.parentBuildingId();
        this.length = payload.length();
        this.plotWidth = payload.width();
        this.upgradeLevel = payload.upgradeLevel();
        this.startingLevel = payload.startingLevel();
        this.plotHeight = payload.height();
        this.orientation = payload.orientation();
        this.clearGround = payload.clearGround();
        this.exportSnow = payload.exportSnow();
        this.importMockBlocks = payload.importMockBlocks();
        this.convertToPreserveGround = payload.convertToPreserveGround();
        this.isMainTable = payload.isMainTable();
    }

    private boolean hasPlan() {
        return this.buildingId != null && !this.buildingId.isEmpty();
    }

    protected void init() {
        super.init();
        this.rebuildScreen();
    }

    private void rebuildScreen() {
        this.clearWidgets();
        int panelX = (this.width - 256) / 2;
        int panelY = (this.height - 220) / 2;
        switch (this.currentState.ordinal()) {
            case 0: {
                this.buildHomeScreen(panelX, panelY);
                break;
            }
            case 1: {
                this.buildNewBuildingScreen(panelX, panelY);
                break;
            }
            case 2: {
                this.buildSettingsScreen(panelX, panelY);
                break;
            }
            case 3: {
                this.buildImportCultureScreen(panelX, panelY);
                break;
            }
            case 4: {
                this.buildImportCultureSubdirScreen(panelX, panelY);
                break;
            }
            case 5: {
                this.buildImportCultureBuildingScreen(panelX, panelY);
                break;
            }
            case 6: {
                this.buildImportCultureSubBuildingScreen(panelX, panelY);
                break;
            }
            case 7: {
                this.buildImportExportDirScreen(panelX, panelY);
                break;
            }
            case 8: {
                this.buildImportExportDirBuildingScreen(panelX, panelY);
                break;
            }
            case 9: {
                this.buildCostsScreen(panelX, panelY);
            }
        }
        this.totalPages = this.computeTotalPages();
        if (this.currentState != ScreenState.HOME) {
            this.addRenderableWidget((GuiEventListener)Button.builder((Component)Component.translatable((String)"gui.millenaire.importtable.back"), btn -> this.navigateBack()).bounds(panelX + 10, panelY + 220 - 25, 50, 20).build());
        }
        if (this.totalPages > 1) {
            int midX = panelX + 128;
            this.prevButton = Button.builder((Component)Component.literal((String)"<"), btn -> {
                if (this.currentPage > 0) {
                    --this.currentPage;
                    this.rebuildScreen();
                }
            }).bounds(midX - 40, panelY + 220 - 25, 20, 20).build();
            this.prevButton.active = this.currentPage > 0;
            this.addRenderableWidget((GuiEventListener)this.prevButton);
            this.nextButton = Button.builder((Component)Component.literal((String)">"), btn -> {
                if (this.currentPage < this.totalPages - 1) {
                    ++this.currentPage;
                    this.rebuildScreen();
                }
            }).bounds(midX + 20, panelY + 220 - 25, 20, 20).build();
            this.nextButton.active = this.currentPage < this.totalPages - 1;
            this.addRenderableWidget((GuiEventListener)this.nextButton);
        }
        this.addRenderableWidget((GuiEventListener)Button.builder((Component)Component.translatable((String)"gui.done"), btn -> this.onClose()).bounds(panelX + 256 - 10 - 50, panelY + 220 - 25, 50, 20).build());
    }

    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, 0x60000000);
        int panelX = (this.width - 256) / 2;
        int panelY = (this.height - 220) / 2;
        PanelRenderHelper.renderTexturedBackground(graphics, PanelRenderHelper.QUEST_TEXTURE, panelX, panelY, 256, 220);
        MutableComponent title = switch (this.currentState.ordinal()) {
            default -> throw new MatchException(null, null);
            case 0 -> {
                if (this.hasPlan()) {
                    yield Component.translatable((String)"gui.millenaire.importtable.title_with_building", (Object[])new Object[]{this.buildingId});
                }
                yield Component.translatable((String)"gui.millenaire.importtable.title");
            }
            case 1 -> Component.translatable((String)"gui.millenaire.importtable.title_new_building");
            case 2 -> Component.translatable((String)"gui.millenaire.importtable.title_settings");
            case 3 -> Component.translatable((String)"gui.millenaire.importtable.title_import_culture", (Object[])new Object[]{this.selectedCultureKey});
            case 4 -> Component.translatable((String)"gui.millenaire.importtable.title_category", (Object[])new Object[]{this.selectedCategory});
            case 5 -> Component.translatable((String)"gui.millenaire.importtable.title_building", (Object[])new Object[]{this.selectedBuildingId});
            case 6 -> Component.translatable((String)"gui.millenaire.importtable.title_subbuilding", (Object[])new Object[]{this.currentSubBuildingPlanSet != null ? this.currentSubBuildingPlanSet.buildingId() : ""});
            case 7 -> Component.translatable((String)"gui.millenaire.importtable.title_import_exports");
            case 8 -> Component.translatable((String)"gui.millenaire.importtable.title_export", (Object[])new Object[]{this.selectedExportBuildingId});
            case 9 -> Component.translatable((String)"gui.millenaire.importtable.title_costs", (Object[])new Object[]{this.costsLabel});
        };
        graphics.drawCenteredString(this.font, (Component)title, panelX + 128, panelY + 6, -11193600);
        if (this.currentState == ScreenState.HOME && this.hasPlan()) {
            this.renderPlanInfo(graphics, panelX + 10, panelY + 22);
        }
        if (this.currentState == ScreenState.NEW_BUILDING) {
            this.renderNewBuildingLabels(graphics);
        }
        if (this.currentState == ScreenState.SETTINGS) {
            this.renderSettingsLabels(graphics);
        }
        if (this.currentState == ScreenState.IMPORT_EXPORT_DIR && this.exportBuildings.isEmpty()) {
            graphics.drawCenteredString(this.font, (Component)Component.translatable((String)"gui.millenaire.importtable.no_exports"), panelX + 128, panelY + 50, -10066347);
        }
        if (this.currentState == ScreenState.COSTS) {
            this.renderCosts(graphics, panelX + 10, panelY + 22);
        }
        if (this.totalPages > 1) {
            String pageStr = this.currentPage + 1 + "/" + this.totalPages;
            graphics.drawCenteredString(this.font, pageStr, panelX + 128, panelY + 220 - 38, -10066347);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void buildHomeScreen(int panelX, int panelY) {
        int y = panelY + 22;
        int buttonWidth = 236;
        if (this.hasPlan()) {
            this.addRenderableWidget((GuiEventListener)Button.builder((Component)Component.translatable((String)"gui.millenaire.importtable.reimport"), btn -> this.sendAction(ImportTableActionPayload.Action.REIMPORT, new CompoundTag())).bounds(panelX + 10, y += 70, buttonWidth / 2 - 2, 20).build());
            Button reimportAll = Button.builder((Component)Component.translatable((String)"gui.millenaire.importtable.reimport_all"), btn -> this.sendAction(ImportTableActionPayload.Action.REIMPORT_ALL, new CompoundTag())).bounds(panelX + 10 + buttonWidth / 2 + 2, y, buttonWidth / 2 - 2, 20).build();
            reimportAll.active = this.isMainTable || this.syncData.hasMainTablePos();
            this.addRenderableWidget((GuiEventListener)reimportAll);
            this.addRenderableWidget((GuiEventListener)Button.builder((Component)Component.translatable((String)"gui.millenaire.importtable.export"), btn -> this.sendAction(ImportTableActionPayload.Action.EXPORT, new CompoundTag())).bounds(panelX + 10, y += 24, buttonWidth / 2 - 2, 20).build());
            this.addRenderableWidget((GuiEventListener)Button.builder((Component)Component.translatable((String)"gui.millenaire.importtable.export_new_level"), btn -> this.sendAction(ImportTableActionPayload.Action.EXPORT_NEW_LEVEL, new CompoundTag())).bounds(panelX + 10 + buttonWidth / 2 + 2, y, buttonWidth / 2 - 2, 20).build());
            this.addRenderableWidget((GuiEventListener)Button.builder((Component)Component.translatable((String)"gui.millenaire.importtable.change_plan"), btn -> {
                this.buildingId = "";
                this.rebuildScreen();
            }).bounds(panelX + 10, y += 24, buttonWidth / 2 - 2, 20).build());
            this.addRenderableWidget((GuiEventListener)Button.builder((Component)Component.translatable((String)"gui.millenaire.importtable.settings"), btn -> this.navigateTo(ScreenState.SETTINGS)).bounds(panelX + 10 + buttonWidth / 2 + 2, y, buttonWidth / 2 - 2, 20).build());
            this.addRenderableWidget((GuiEventListener)Button.builder((Component)Component.translatable((String)"gui.millenaire.importtable.costs"), btn -> {
                this.costs = null;
                this.costsLabel = this.buildingId + " " + this.variant + " level " + this.upgradeLevel;
                this.sendAction(ImportTableActionPayload.Action.SHOW_COSTS, new CompoundTag());
                this.navigateTo(ScreenState.COSTS);
            }).bounds(panelX + 10, y += 24, buttonWidth, 20).build());
        } else {
            this.addRenderableWidget((GuiEventListener)Button.builder((Component)Component.translatable((String)"gui.millenaire.importtable.new_building"), btn -> this.navigateTo(ScreenState.NEW_BUILDING)).bounds(panelX + 10, y, buttonWidth, 20).build());
            this.addRenderableWidget((GuiEventListener)Button.builder((Component)Component.translatable((String)"gui.millenaire.importtable.import_from_exports"), btn -> {
                this.loadExportBuildings();
                this.navigateTo(ScreenState.IMPORT_EXPORT_DIR);
            }).bounds(panelX + 10, y += 24, buttonWidth, 20).build());
            y += 24;
            Map<ResourceLocation, Culture> cultures = ModCultures.getAllCultures();
            int halfWidth = (buttonWidth - 4) / 2;
            int col = 0;
            for (Map.Entry<ResourceLocation, Culture> entry : cultures.entrySet()) {
                Culture culture = entry.getValue();
                String cKey = entry.getKey().toString();
                int btnX = panelX + 10 + col * (halfWidth + 4);
                this.addRenderableWidget((GuiEventListener)Button.builder((Component)Component.literal((String)culture.displayName()), btn -> {
                    this.selectedCultureKey = cKey;
                    this.loadCultureCategories();
                    this.navigateTo(ScreenState.IMPORT_CULTURE);
                }).bounds(btnX, y, halfWidth, 20).build());
                if (++col >= 2) {
                    col = 0;
                    y += 22;
                }
                if (y <= panelY + 220 - 55) continue;
                break;
            }
            if (col > 0) {
                y += 22;
            }
            this.addRenderableWidget((GuiEventListener)Button.builder((Component)Component.translatable((String)"gui.millenaire.importtable.settings"), btn -> this.navigateTo(ScreenState.SETTINGS)).bounds(panelX + 10, y += 4, buttonWidth, 20).build());
        }
    }

    private void renderPlanInfo(GuiGraphics graphics, int x, int y) {
        MutableComponent cultureLabel = this.cultureKey.isEmpty() ? Component.translatable((String)"gui.millenaire.importtable.info.culture_custom") : Component.literal((String)this.cultureKey);
        graphics.drawString(this.font, (Component)Component.translatable((String)"gui.millenaire.importtable.info.culture", (Object[])new Object[]{cultureLabel}), x, y, -10066347);
        graphics.drawString(this.font, (Component)Component.translatable((String)"gui.millenaire.importtable.info.building", (Object[])new Object[]{this.buildingId, this.variant}), x, y += 14, -13421773);
        graphics.drawString(this.font, (Component)Component.translatable((String)"gui.millenaire.importtable.info.level_size", (Object[])new Object[]{this.upgradeLevel, this.plotWidth, this.length}), x, y += 14, -13421773);
        graphics.drawString(this.font, (Component)Component.translatable((String)"gui.millenaire.importtable.info.y_range_orientation", (Object[])new Object[]{this.startingLevel, this.startingLevel + this.plotHeight - 1, ORIENTATION_NAMES[this.orientation % 4]}), x, y += 14, -13421773);
        if (this.parentBuildingId != null && !this.parentBuildingId.isEmpty()) {
            String parentLabel = this.syncData.parentVariant().isEmpty() ? this.parentBuildingId : this.parentBuildingId + "/" + this.syncData.parentVariant();
            graphics.drawString(this.font, (Component)Component.translatable((String)"gui.millenaire.importtable.info.sub_of", (Object[])new Object[]{parentLabel, this.syncData.parentTriggerLevel()}), x, y += 14, -11193600);
        }
    }

    private void buildNewBuildingScreen(int panelX, int panelY) {
        int y = panelY + 22;
        int valueX = panelX + 10 + 120;
        int editBoxW = 40;
        if (this.currentPage == 0) {
            int nameW = 112;
            this.buildingNameField = new EditBox(this.font, valueX, y, nameW, 20, (Component)Component.literal((String)""));
            this.buildingNameField.setValue(this.buildingName);
            this.buildingNameField.setMaxLength(64);
            this.buildingNameField.setResponder(s -> {
                this.buildingName = s;
            });
            this.addRenderableWidget((GuiEventListener)this.buildingNameField);
            this.lengthField = new EditBox(this.font, valueX + 22, y += 22, editBoxW, 20, (Component)Component.literal((String)""));
            this.lengthField.setValue(String.valueOf(this.length));
            this.lengthField.setMaxLength(4);
            this.lengthField.setFilter(s -> s.isEmpty() || s.matches("\\d+"));
            this.lengthField.setResponder(s -> {
                try {
                    this.length = Math.max(1, Integer.parseInt(s));
                }
                catch (NumberFormatException numberFormatException) {
                    // empty catch block
                }
            });
            this.addRenderableWidget((GuiEventListener)this.lengthField);
            this.addRenderableWidget((GuiEventListener)Button.builder((Component)Component.literal((String)"-"), btn -> {
                this.length = Math.max(1, this.length - 1);
                this.lengthField.setValue(String.valueOf(this.length));
            }).bounds(valueX, y, 20, 20).build());
            this.addRenderableWidget((GuiEventListener)Button.builder((Component)Component.literal((String)"+"), btn -> {
                ++this.length;
                this.lengthField.setValue(String.valueOf(this.length));
            }).bounds(valueX + 22 + editBoxW + 2, y, 20, 20).build());
            this.plotWidthField = new EditBox(this.font, valueX + 22, y += 22, editBoxW, 20, (Component)Component.literal((String)""));
            this.plotWidthField.setValue(String.valueOf(this.plotWidth));
            this.plotWidthField.setMaxLength(4);
            this.plotWidthField.setFilter(s -> s.isEmpty() || s.matches("\\d+"));
            this.plotWidthField.setResponder(s -> {
                try {
                    this.plotWidth = Math.max(1, Integer.parseInt(s));
                }
                catch (NumberFormatException numberFormatException) {
                    // empty catch block
                }
            });
            this.addRenderableWidget((GuiEventListener)this.plotWidthField);
            this.addRenderableWidget((GuiEventListener)Button.builder((Component)Component.literal((String)"-"), btn -> {
                this.plotWidth = Math.max(1, this.plotWidth - 1);
                this.plotWidthField.setValue(String.valueOf(this.plotWidth));
            }).bounds(valueX, y, 20, 20).build());
            this.addRenderableWidget((GuiEventListener)Button.builder((Component)Component.literal((String)"+"), btn -> {
                ++this.plotWidth;
                this.plotWidthField.setValue(String.valueOf(this.plotWidth));
            }).bounds(valueX + 22 + editBoxW + 2, y, 20, 20).build());
            this.startingLevelField = new EditBox(this.font, valueX + 22, y += 22, editBoxW, 20, (Component)Component.literal((String)""));
            this.startingLevelField.setValue(String.valueOf(this.startingLevel));
            this.startingLevelField.setMaxLength(4);
            this.startingLevelField.setFilter(s -> s.isEmpty() || s.equals("-") || s.matches("-?\\d+"));
            this.startingLevelField.setResponder(s -> {
                try {
                    this.startingLevel = Integer.parseInt(s);
                }
                catch (NumberFormatException numberFormatException) {
                    // empty catch block
                }
            });
            this.addRenderableWidget((GuiEventListener)this.startingLevelField);
            this.addRenderableWidget((GuiEventListener)Button.builder((Component)Component.literal((String)"-"), btn -> {
                --this.startingLevel;
                this.startingLevelField.setValue(String.valueOf(this.startingLevel));
            }).bounds(valueX, y, 20, 20).build());
            this.addRenderableWidget((GuiEventListener)Button.builder((Component)Component.literal((String)"+"), btn -> {
                ++this.startingLevel;
                this.startingLevelField.setValue(String.valueOf(this.startingLevel));
            }).bounds(valueX + 22 + editBoxW + 2, y, 20, 20).build());
            this.plotHeightField = new EditBox(this.font, valueX + 22, y += 22, editBoxW, 20, (Component)Component.literal((String)""));
            this.plotHeightField.setValue(String.valueOf(this.plotHeight));
            this.plotHeightField.setMaxLength(4);
            this.plotHeightField.setFilter(s -> s.isEmpty() || s.matches("\\d+"));
            this.plotHeightField.setResponder(s -> {
                try {
                    this.plotHeight = Math.max(1, Integer.parseInt(s));
                }
                catch (NumberFormatException numberFormatException) {
                    // empty catch block
                }
            });
            this.addRenderableWidget((GuiEventListener)this.plotHeightField);
            this.addRenderableWidget((GuiEventListener)Button.builder((Component)Component.literal((String)"-"), btn -> {
                this.plotHeight = Math.max(1, this.plotHeight - 1);
                this.plotHeightField.setValue(String.valueOf(this.plotHeight));
            }).bounds(valueX, y, 20, 20).build());
            this.addRenderableWidget((GuiEventListener)Button.builder((Component)Component.literal((String)"+"), btn -> {
                ++this.plotHeight;
                this.plotHeightField.setValue(String.valueOf(this.plotHeight));
            }).bounds(valueX + 22 + editBoxW + 2, y, 20, 20).build());
        } else {
            this.numberOfUpgradesField = new EditBox(this.font, valueX + 22, y, editBoxW, 20, (Component)Component.literal((String)""));
            this.numberOfUpgradesField.setValue(String.valueOf(this.numberOfUpgrades));
            this.numberOfUpgradesField.setMaxLength(4);
            this.numberOfUpgradesField.setFilter(s -> s.isEmpty() || s.matches("\\d+"));
            this.numberOfUpgradesField.setResponder(s -> {
                try {
                    this.numberOfUpgrades = Math.max(0, Integer.parseInt(s));
                }
                catch (NumberFormatException numberFormatException) {
                    // empty catch block
                }
            });
            this.addRenderableWidget((GuiEventListener)this.numberOfUpgradesField);
            this.addRenderableWidget((GuiEventListener)Button.builder((Component)Component.literal((String)"-"), btn -> {
                this.numberOfUpgrades = Math.max(0, this.numberOfUpgrades - 1);
                this.numberOfUpgradesField.setValue(String.valueOf(this.numberOfUpgrades));
            }).bounds(valueX, y, 20, 20).build());
            this.addRenderableWidget((GuiEventListener)Button.builder((Component)Component.literal((String)"+"), btn -> {
                ++this.numberOfUpgrades;
                this.numberOfUpgradesField.setValue(String.valueOf(this.numberOfUpgrades));
            }).bounds(valueX + 22 + editBoxW + 2, y, 20, 20).build());
            this.addRenderableWidget((GuiEventListener)Button.builder((Component)Component.translatable((String)(this.clearGround ? "gui.millenaire.importtable.toggle.on" : "gui.millenaire.importtable.toggle.off")), btn -> {
                this.clearGround = !this.clearGround;
                this.rebuildScreen();
            }).bounds(valueX, y += 22, 50, 20).build());
            this.addRenderableWidget((GuiEventListener)Button.builder((Component)Component.translatable((String)(this.preBuilt ? "gui.millenaire.importtable.toggle.on" : "gui.millenaire.importtable.toggle.off")), btn -> {
                this.preBuilt = !this.preBuilt;
                this.rebuildScreen();
            }).bounds(valueX, y += 22, 50, 20).build());
            this.addRenderableWidget((GuiEventListener)Button.builder((Component)Component.translatable((String)"gui.millenaire.importtable.create"), btn -> {
                CompoundTag data = new CompoundTag();
                data.putInt("length", this.length);
                data.putInt("width", this.plotWidth);
                data.putInt("startingLevel", this.startingLevel);
                data.putInt("height", this.plotHeight);
                data.putBoolean("clearGround", this.clearGround);
                data.putInt("numberOfUpgrades", this.numberOfUpgrades);
                data.putBoolean("preBuilt", this.preBuilt);
                data.putString("buildingName", this.buildingName);
                this.sendAction(ImportTableActionPayload.Action.CREATE_NEW, data);
                this.onClose();
            }).bounds(panelX + 10, y += 28, 236, 20).build());
        }
    }

    private void renderNewBuildingLabels(GuiGraphics graphics) {
        int panelX = (this.width - 256) / 2;
        int panelY = (this.height - 220) / 2;
        int y = panelY + 22;
        int labelX = panelX + 10;
        if (this.currentPage == 0) {
            graphics.drawString(this.font, (Component)Component.translatable((String)"gui.millenaire.importtable.label.building_name"), labelX, y + 5, -13421773);
            graphics.drawString(this.font, (Component)Component.translatable((String)"gui.millenaire.importtable.label.length"), labelX, (y += 22) + 5, -13421773);
            graphics.drawString(this.font, (Component)Component.translatable((String)"gui.millenaire.importtable.label.width"), labelX, (y += 22) + 5, -13421773);
            graphics.drawString(this.font, (Component)Component.translatable((String)"gui.millenaire.importtable.label.starting_level"), labelX, (y += 22) + 5, -13421773);
            graphics.drawString(this.font, (Component)Component.translatable((String)"gui.millenaire.importtable.label.height"), labelX, (y += 22) + 5, -13421773);
        } else {
            graphics.drawString(this.font, (Component)Component.translatable((String)"gui.millenaire.importtable.label.number_of_upgrades"), labelX, y + 5, -13421773);
            graphics.drawString(this.font, (Component)Component.translatable((String)"gui.millenaire.importtable.label.clear_ground"), labelX, (y += 22) + 5, -13421773);
            graphics.drawString(this.font, (Component)Component.translatable((String)"gui.millenaire.importtable.label.pre_built"), labelX, (y += 22) + 5, -13421773);
        }
    }

    private void buildSettingsScreen(int panelX, int panelY) {
        int y = panelY + 22;
        int valueX = panelX + 10 + 160;
        int btnW = 50;
        this.addRenderableWidget((GuiEventListener)Button.builder((Component)Component.literal((String)ORIENTATION_NAMES[this.orientation % 4]), btn -> {
            this.orientation = (this.orientation + 1) % 4;
            this.sendSettingsUpdate();
            this.rebuildScreen();
        }).bounds(valueX, y, btnW, 20).build());
        this.addRenderableWidget((GuiEventListener)Button.builder((Component)Component.literal((String)"-"), btn -> {
            --this.startingLevel;
            ++this.plotHeight;
            this.sendSettingsUpdate();
            this.rebuildScreen();
        }).bounds(valueX, y += 22, 20, 20).build());
        this.addRenderableWidget((GuiEventListener)Button.builder((Component)Component.literal((String)"+"), btn -> {
            ++this.startingLevel;
            if (this.plotHeight > 1) {
                --this.plotHeight;
            }
            this.sendSettingsUpdate();
            this.rebuildScreen();
        }).bounds(valueX + 30, y, 20, 20).build());
        this.addRenderableWidget((GuiEventListener)Button.builder((Component)Component.literal((String)"-"), btn -> {
            this.plotHeight = Math.max(1, this.plotHeight - 1);
            this.sendSettingsUpdate();
            this.rebuildScreen();
        }).bounds(valueX, y += 22, 20, 20).build());
        this.addRenderableWidget((GuiEventListener)Button.builder((Component)Component.literal((String)"+"), btn -> {
            ++this.plotHeight;
            this.sendSettingsUpdate();
            this.rebuildScreen();
        }).bounds(valueX + 30, y, 20, 20).build());
        this.addRenderableWidget((GuiEventListener)Button.builder((Component)Component.translatable((String)(this.exportSnow ? "gui.millenaire.importtable.toggle.on" : "gui.millenaire.importtable.toggle.off")), btn -> {
            this.exportSnow = !this.exportSnow;
            this.sendSettingsUpdate();
            this.rebuildScreen();
        }).bounds(valueX, y += 22, btnW, 20).build());
        this.addRenderableWidget((GuiEventListener)Button.builder((Component)Component.translatable((String)(this.importMockBlocks ? "gui.millenaire.importtable.toggle.on" : "gui.millenaire.importtable.toggle.off")), btn -> {
            this.importMockBlocks = !this.importMockBlocks;
            this.sendSettingsUpdate();
            this.rebuildScreen();
        }).bounds(valueX, y += 22, btnW, 20).build());
        this.addRenderableWidget((GuiEventListener)Button.builder((Component)Component.translatable((String)(this.convertToPreserveGround ? "gui.millenaire.importtable.toggle.on" : "gui.millenaire.importtable.toggle.off")), btn -> {
            this.convertToPreserveGround = !this.convertToPreserveGround;
            this.sendSettingsUpdate();
            this.rebuildScreen();
        }).bounds(valueX, y += 22, btnW, 20).build());
    }

    private void renderSettingsLabels(GuiGraphics graphics) {
        int panelX = (this.width - 256) / 2;
        int panelY = (this.height - 220) / 2;
        int y = panelY + 22;
        int labelX = panelX + 10;
        graphics.drawString(this.font, (Component)Component.translatable((String)"gui.millenaire.importtable.label.orientation"), labelX, y + 5, -13421773);
        graphics.drawString(this.font, (Component)Component.translatable((String)"gui.millenaire.importtable.label.starting_level_value", (Object[])new Object[]{this.startingLevel}), labelX, (y += 22) + 5, -13421773);
        graphics.drawString(this.font, (Component)Component.translatable((String)"gui.millenaire.importtable.label.height_value", (Object[])new Object[]{this.plotHeight}), labelX, (y += 22) + 5, -13421773);
        graphics.drawString(this.font, (Component)Component.translatable((String)"gui.millenaire.importtable.label.export_snow"), labelX, (y += 22) + 5, -13421773);
        graphics.drawString(this.font, (Component)Component.translatable((String)"gui.millenaire.importtable.label.import_mock_blocks"), labelX, (y += 22) + 5, -13421773);
        graphics.drawString(this.font, (Component)Component.translatable((String)"gui.millenaire.importtable.label.convert_preserve_ground"), labelX, (y += 22) + 5, -13421773);
    }

    private void buildImportCultureScreen(int panelX, int panelY) {
        int y = panelY + 22;
        int buttonWidth = 236;
        for (String category : this.pageSlice(this.cultureCategories)) {
            this.addRenderableWidget((GuiEventListener)Button.builder((Component)Component.literal((String)category), btn -> {
                this.selectedCategory = category;
                this.loadCategoryBuildings();
                this.navigateTo(ScreenState.IMPORT_CULTURE_SUBDIR);
            }).bounds(panelX + 10, y, buttonWidth, 20).build());
            y += 22;
        }
    }

    private void buildImportCultureSubdirScreen(int panelX, int panelY) {
        int y = panelY + 22;
        int buttonWidth = 236;
        for (BuildingPlanSet planSet : this.pageSlice(this.categoryBuildings)) {
            String label = planSet.buildingId();
            this.addRenderableWidget((GuiEventListener)Button.builder((Component)Component.literal((String)label), btn -> {
                this.selectedBuildingId = planSet.buildingId();
                this.currentBuildingPlanSet = planSet;
                if (!planSet.variants().isEmpty()) {
                    this.selectedVariant = planSet.variants().keySet().iterator().next();
                }
                this.loadBuildingLevels(planSet);
                this.navigateTo(ScreenState.IMPORT_CULTURE_BUILDING);
            }).bounds(panelX + 10, y, buttonWidth, 20).build());
            y += 22;
        }
    }

    private void buildImportCultureBuildingScreen(int panelX, int panelY) {
        int y = panelY + 22;
        int buttonWidth = 236;
        if (this.currentBuildingPlanSet != null && this.currentBuildingPlanSet.variants().size() > 1) {
            this.addRenderableWidget((GuiEventListener)Button.builder((Component)Component.translatable((String)"gui.millenaire.importtable.variant_hint", (Object[])new Object[]{this.selectedVariant}), btn -> {
                ArrayList<String> variants = new ArrayList<String>(this.currentBuildingPlanSet.variants().keySet());
                int idx = variants.indexOf(this.selectedVariant);
                this.selectedVariant = (String)variants.get((idx + 1) % variants.size());
                this.loadBuildingLevels(this.currentBuildingPlanSet);
                this.currentPage = 0;
                this.rebuildScreen();
            }).bounds(panelX + 10, y, buttonWidth, 20).build());
            y += 24;
        }
        this.addRenderableWidget((GuiEventListener)Button.builder((Component)Component.translatable((String)"gui.millenaire.importtable.import_all"), btn -> {
            CompoundTag data = new CompoundTag();
            data.putString("cultureKey", this.selectedCultureKey);
            data.putString("buildingId", this.selectedBuildingId);
            data.putString("variant", this.selectedVariant);
            this.sendAction(ImportTableActionPayload.Action.IMPORT_ALL, data);
            this.onClose();
        }).bounds(panelX + 10, y, buttonWidth, 20).build());
        y += 24;
        for (Object entry : this.pageSlice(this.buildingScreenEntries())) {
            if (entry instanceof Integer) {
                Integer lvl = (Integer)entry;
                this.addRenderableWidget((GuiEventListener)Button.builder((Component)Component.translatable((String)"gui.millenaire.importtable.building_level_label", (Object[])new Object[]{this.selectedBuildingId, this.selectedVariant, lvl}), btn -> {
                    CompoundTag data = new CompoundTag();
                    data.putString("cultureKey", this.selectedCultureKey);
                    data.putString("buildingId", this.selectedBuildingId);
                    data.putString("variant", this.selectedVariant);
                    data.putInt("level", lvl.intValue());
                    data.putString("parentBuildingId", "");
                    data.putString("parentVariant", "");
                    this.sendAction(ImportTableActionPayload.Action.IMPORT_LEVEL, data);
                    this.onClose();
                }).bounds(panelX + 10, y, buttonWidth, 20).build());
            } else if (entry instanceof BuildingPlanSet) {
                BuildingPlanSet sub = (BuildingPlanSet)entry;
                this.addRenderableWidget((GuiEventListener)Button.builder((Component)Component.translatable((String)"gui.millenaire.importtable.subbuilding_entry", (Object[])new Object[]{sub.buildingId()}), btn -> {
                    this.subParentPlanSet = this.currentBuildingPlanSet;
                    this.subParentVariant = this.selectedVariant;
                    this.currentSubBuildingPlanSet = sub;
                    this.selectedSubVariant = sub.variants().isEmpty() ? "a" : sub.variants().keySet().iterator().next();
                    this.loadSubBuildingLevels(sub);
                    this.navigateTo(ScreenState.IMPORT_CULTURE_SUBBUILDING);
                }).bounds(panelX + 10, y, buttonWidth, 20).build());
            }
            y += 22;
        }
    }

    private List<Object> buildingScreenEntries() {
        ArrayList<Object> entries = new ArrayList<Object>(this.buildingLevels.size() + this.referencedSubBuildings.size());
        entries.addAll(this.buildingLevels);
        entries.addAll(this.referencedSubBuildings);
        return entries;
    }

    private void buildImportCultureSubBuildingScreen(int panelX, int panelY) {
        int y = panelY + 22;
        int buttonWidth = 236;
        if (this.currentSubBuildingPlanSet != null && this.currentSubBuildingPlanSet.variants().size() > 1) {
            this.addRenderableWidget((GuiEventListener)Button.builder((Component)Component.translatable((String)"gui.millenaire.importtable.variant_hint", (Object[])new Object[]{this.selectedSubVariant}), btn -> {
                ArrayList<String> variants = new ArrayList<String>(this.currentSubBuildingPlanSet.variants().keySet());
                int idx = variants.indexOf(this.selectedSubVariant);
                this.selectedSubVariant = (String)variants.get((idx + 1) % variants.size());
                this.loadSubBuildingLevels(this.currentSubBuildingPlanSet);
                this.currentPage = 0;
                this.rebuildScreen();
            }).bounds(panelX + 10, y, buttonWidth, 20).build());
            y += 24;
        }
        for (int lvl : this.pageSlice(this.subBuildingLevels)) {
            this.addRenderableWidget((GuiEventListener)Button.builder((Component)Component.translatable((String)"gui.millenaire.importtable.building_level_label", (Object[])new Object[]{this.currentSubBuildingPlanSet != null ? this.currentSubBuildingPlanSet.buildingId() : "", this.selectedSubVariant, lvl}), btn -> {
                CompoundTag data = new CompoundTag();
                data.putString("cultureKey", this.selectedCultureKey);
                data.putString("buildingId", this.currentSubBuildingPlanSet != null ? this.currentSubBuildingPlanSet.buildingId() : "");
                data.putString("variant", this.selectedSubVariant);
                data.putInt("level", lvl);
                data.putString("parentBuildingId", this.subParentPlanSet != null ? this.subParentPlanSet.buildingId() : "");
                data.putString("parentVariant", this.subParentVariant);
                this.sendAction(ImportTableActionPayload.Action.IMPORT_LEVEL, data);
                this.onClose();
            }).bounds(panelX + 10, y, buttonWidth, 20).build());
            y += 22;
        }
    }

    private void loadSubBuildingLevels(BuildingPlanSet subPlanSet) {
        List<BuildingPlanSet.LevelDef> levels = subPlanSet.variants().get(this.selectedSubVariant);
        this.subBuildingLevels = levels == null ? List.of() : levels.stream().map(BuildingPlanSet.LevelDef::level).toList();
    }

    private void buildImportExportDirScreen(int panelX, int panelY) {
        int y = panelY + 22;
        int buttonWidth = 236;
        for (String building : this.pageSlice(this.exportBuildings)) {
            this.addRenderableWidget((GuiEventListener)Button.builder((Component)Component.literal((String)building), btn -> {
                this.selectedExportBuildingId = building;
                this.loadExportVariants();
                this.loadExportLevels();
                this.navigateTo(ScreenState.IMPORT_EXPORT_DIR_BUILDING);
            }).bounds(panelX + 10, y, buttonWidth, 20).build());
            y += 22;
        }
        if (this.exportBuildings.isEmpty()) {
            // empty if block
        }
    }

    private void buildImportExportDirBuildingScreen(int panelX, int panelY) {
        int y = panelY + 22;
        int buttonWidth = 236;
        if (this.availableExportVariants.size() > 1) {
            this.addRenderableWidget((GuiEventListener)Button.builder((Component)Component.translatable((String)"gui.millenaire.importtable.variant_hint", (Object[])new Object[]{this.selectedExportVariant}), btn -> {
                int idx = this.availableExportVariants.indexOf(this.selectedExportVariant);
                this.selectedExportVariant = this.availableExportVariants.get((idx + 1) % this.availableExportVariants.size());
                this.loadExportLevels();
                this.currentPage = 0;
                this.rebuildScreen();
            }).bounds(panelX + 10, y, buttonWidth, 20).build());
            y += 24;
        }
        this.addRenderableWidget((GuiEventListener)Button.builder((Component)Component.translatable((String)"gui.millenaire.importtable.import_all"), btn -> {
            CompoundTag data = new CompoundTag();
            data.putString("buildingId", this.selectedExportBuildingId);
            data.putString("variant", this.selectedExportVariant);
            this.sendAction(ImportTableActionPayload.Action.IMPORT_ALL_EXPORT, data);
            this.onClose();
        }).bounds(panelX + 10, y, buttonWidth, 20).build());
        y += 24;
        for (int lvl : this.pageSlice(this.exportLevels)) {
            this.addRenderableWidget((GuiEventListener)Button.builder((Component)Component.translatable((String)"gui.millenaire.importtable.building_level_label", (Object[])new Object[]{this.selectedExportBuildingId, this.selectedExportVariant, lvl}), btn -> {
                CompoundTag data = new CompoundTag();
                data.putString("buildingId", this.selectedExportBuildingId);
                data.putString("variant", this.selectedExportVariant);
                data.putInt("level", lvl);
                this.sendAction(ImportTableActionPayload.Action.IMPORT_LEVEL_EXPORT, data);
                this.onClose();
            }).bounds(panelX + 10, y, buttonWidth, 20).build());
            y += 22;
        }
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.buildingNameField != null && this.buildingNameField.isFocused() && this.buildingNameField.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        if (this.lengthField != null && this.lengthField.isFocused() && this.lengthField.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        if (this.plotWidthField != null && this.plotWidthField.isFocused() && this.plotWidthField.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        if (this.startingLevelField != null && this.startingLevelField.isFocused() && this.startingLevelField.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        if (this.plotHeightField != null && this.plotHeightField.isFocused() && this.plotHeightField.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        if (this.numberOfUpgradesField != null && this.numberOfUpgradesField.isFocused() && this.numberOfUpgradesField.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void buildCostsScreen(int panelX, int panelY) {
    }

    private void renderCosts(GuiGraphics graphics, int x, int y) {
        if (this.costs == null) {
            graphics.drawCenteredString(this.font, (Component)Component.translatable((String)"gui.millenaire.importtable.computing"), x + 118, y + 30, -10066347);
            return;
        }
        if (this.costs.isEmpty()) {
            graphics.drawCenteredString(this.font, (Component)Component.translatable((String)"gui.millenaire.importtable.no_resources"), x + 118, y + 30, -10066347);
            return;
        }
        List<ImportTableCostsPayload.Entry> page = this.pageSlice(this.costs);
        int lineY = y;
        for (ImportTableCostsPayload.Entry entry : page) {
            String line = entry.itemId() + "  \u00d7" + entry.quantity();
            graphics.drawString(this.font, line, x, lineY, -13421773, false);
            lineY += 14;
        }
    }

    public static void applyCosts(ImportTableCostsPayload payload) {
        Screen screen = Minecraft.getInstance().screen;
        if (screen instanceof ImportTableScreen) {
            ImportTableScreen screen2 = (ImportTableScreen)screen;
            screen2.costs = payload.costs();
            screen2.costsLabel = payload.buildingId() + " " + payload.variant() + " level " + payload.level();
            if (screen2.currentState == ScreenState.COSTS) {
                screen2.rebuildScreen();
            }
        }
    }

    private int maxItemsPerPage() {
        int headerPx = 0;
        switch (this.currentState.ordinal()) {
            case 5: {
                if (this.currentBuildingPlanSet != null && this.currentBuildingPlanSet.variants().size() > 1) {
                    headerPx += 24;
                }
                headerPx += 24;
                break;
            }
            case 6: {
                if (this.currentSubBuildingPlanSet == null || this.currentSubBuildingPlanSet.variants().size() <= 1) break;
                headerPx += 24;
                break;
            }
            case 8: {
                if (this.availableExportVariants.size() > 1) {
                    headerPx += 24;
                }
                headerPx += 24;
                break;
            }
        }
        int titlePx = 22;
        int bottomPx = 38;
        int available = 220 - titlePx - bottomPx - headerPx;
        int itemPx = 22;
        int fit = Math.max(1, available / itemPx);
        return Math.min(fit, 7);
    }

    private int computeTotalPages() {
        if (this.currentState == ScreenState.NEW_BUILDING) {
            return 2;
        }
        int listSize = switch (this.currentState.ordinal()) {
            case 3 -> this.cultureCategories.size();
            case 4 -> this.categoryBuildings.size();
            case 5 -> this.buildingLevels.size() + this.referencedSubBuildings.size();
            case 6 -> this.subBuildingLevels.size();
            case 7 -> this.exportBuildings.size();
            case 8 -> this.exportLevels.size();
            case 9 -> {
                if (this.costs == null) {
                    yield 0;
                }
                yield this.costs.size();
            }
            default -> 0;
        };
        int max = this.maxItemsPerPage();
        if (listSize <= max) {
            return 1;
        }
        return (listSize + max - 1) / max;
    }

    private <T> List<T> pageSlice(List<T> list) {
        int max = this.maxItemsPerPage();
        int from = this.currentPage * max;
        int to = Math.min(from + max, list.size());
        if (from >= list.size()) {
            return List.of();
        }
        return list.subList(from, to);
    }

    private void navigateTo(ScreenState state) {
        this.previousScreens.push(this.currentState);
        this.currentState = state;
        this.currentPage = 0;
        this.rebuildScreen();
    }

    private void navigateBack() {
        this.currentState = !this.previousScreens.isEmpty() ? this.previousScreens.pop() : ScreenState.HOME;
        this.currentPage = 0;
        this.rebuildScreen();
    }

    private void sendAction(ImportTableActionPayload.Action action, CompoundTag data) {
        PacketDistributor.sendToServer((CustomPacketPayload)new ImportTableActionPayload(this.syncData.blockPos(), action, data), (CustomPacketPayload[])new CustomPacketPayload[0]);
    }

    private void sendSettingsUpdate() {
        CompoundTag data = new CompoundTag();
        data.putInt("orientation", this.orientation);
        data.putInt("startingLevel", this.startingLevel);
        data.putInt("height", this.plotHeight);
        data.putBoolean("exportSnow", this.exportSnow);
        data.putBoolean("importMockBlocks", this.importMockBlocks);
        data.putBoolean("convertToPreserveGround", this.convertToPreserveGround);
        this.sendAction(ImportTableActionPayload.Action.UPDATE_SETTINGS, data);
    }

    private void loadCultureCategories() {
        TreeMap<String, List> byCategory = new TreeMap<String, List>();
        for (BuildingPlanSet planSet : ModCultures.getAllBuildingPlanSets().values()) {
            if (!planSet.culture().toString().equals(this.selectedCultureKey)) continue;
            byCategory.computeIfAbsent(planSet.category(), k -> new ArrayList()).add(planSet);
        }
        this.cultureCategories = new ArrayList(byCategory.keySet());
    }

    private void loadCategoryBuildings() {
        ArrayList<BuildingPlanSet> buildings = new ArrayList<BuildingPlanSet>();
        for (BuildingPlanSet planSet : ModCultures.getAllBuildingPlanSets().values()) {
            if (!planSet.culture().toString().equals(this.selectedCultureKey) || !planSet.category().equals(this.selectedCategory)) continue;
            buildings.add(planSet);
        }
        buildings.sort((a, b) -> a.buildingId().compareToIgnoreCase(b.buildingId()));
        this.categoryBuildings = buildings;
    }

    private void loadBuildingLevels(BuildingPlanSet planSet) {
        List<BuildingPlanSet.LevelDef> levels = planSet.variants().get(this.selectedVariant);
        this.buildingLevels = levels == null ? List.of() : levels.stream().map(BuildingPlanSet.LevelDef::level).toList();
        this.referencedSubBuildings = this.referencedSubBuildingsFor(planSet);
    }

    private List<BuildingPlanSet> referencedSubBuildingsFor(BuildingPlanSet parent) {
        LinkedHashSet<String> ids = new LinkedHashSet<String>(parent.startingSubBuildings());
        for (List<BuildingPlanSet.LevelDef> levels : parent.variants().values()) {
            for (BuildingPlanSet.LevelDef ld : levels) {
                ids.addAll(ld.subBuildings());
            }
        }
        if (ids.isEmpty()) {
            return List.of();
        }
        String culturePrefix = parent.culture().toString() + "/";
        ArrayList<BuildingPlanSet> result = new ArrayList<BuildingPlanSet>();
        for (String subId : ids) {
            BuildingPlanSet subPlanSet;
            ResourceLocation subPlanId = ResourceLocation.tryParse((String)(culturePrefix + subId));
            if (subPlanId == null || (subPlanSet = ModCultures.getBuildingPlanSet(subPlanId)) == null || !subPlanSet.isSubBuilding()) continue;
            result.add(subPlanSet);
        }
        result.sort((a, b) -> a.buildingId().compareToIgnoreCase(b.buildingId()));
        return result;
    }

    private void loadExportBuildings() {
        this.exportBuildings = new ArrayList<String>();
        Path gameDir = ImportTableScreen.singleplayerGameDir();
        if (gameDir == null) {
            return;
        }
        this.exportBuildings.addAll(BuildingExporter.listExportedBuildingIds(gameDir));
    }

    private void loadExportLevels() {
        this.exportLevels = new ArrayList<Integer>();
        Path gameDir = ImportTableScreen.singleplayerGameDir();
        if (gameDir == null) {
            return;
        }
        this.exportLevels.addAll(BuildingExporter.listExportedLevels(gameDir, this.selectedExportBuildingId, this.selectedExportVariant));
    }

    private void loadExportVariants() {
        this.availableExportVariants = new ArrayList<String>();
        Path gameDir = ImportTableScreen.singleplayerGameDir();
        if (gameDir == null) {
            return;
        }
        this.availableExportVariants.addAll(BuildingExporter.listExportedVariants(gameDir, this.selectedExportBuildingId));
        if (!this.availableExportVariants.isEmpty() && !this.availableExportVariants.contains(this.selectedExportVariant)) {
            this.selectedExportVariant = this.availableExportVariants.get(0);
        }
    }

    private static Path singleplayerGameDir() {
        IntegratedServer server = Minecraft.getInstance().getSingleplayerServer();
        return server == null ? null : server.getServerDirectory();
    }

    static enum ScreenState {
        HOME,
        NEW_BUILDING,
        SETTINGS,
        IMPORT_CULTURE,
        IMPORT_CULTURE_SUBDIR,
        IMPORT_CULTURE_BUILDING,
        IMPORT_CULTURE_SUBBUILDING,
        IMPORT_EXPORT_DIR,
        IMPORT_EXPORT_DIR_BUILDING,
        COSTS;

    }
}

