/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  com.google.gson.GsonBuilder
 *  com.google.gson.JsonArray
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  com.google.gson.JsonParser
 *  com.mojang.logging.LogUtils
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.core.HolderGetter
 *  net.minecraft.core.Vec3i
 *  net.minecraft.core.registries.Registries
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.nbt.NbtAccounter
 *  net.minecraft.nbt.NbtIo
 *  net.minecraft.network.chat.Component
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.item.DyeColor
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraft.world.level.block.Rotation
 *  net.minecraft.world.level.block.StandingSignBlock
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.level.block.entity.SignBlockEntity
 *  net.minecraft.world.level.block.entity.SignText
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.properties.Property
 *  net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate
 *  org.slf4j.Logger
 */
package org.millenaire.building;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.millenaire.block.ImportTableBlock;
import org.millenaire.block.ImportTableBlockEntity;
import org.millenaire.block.ModBlocks;
import org.millenaire.building.BuildingExporter;
import org.millenaire.building.HearthLightingUtil;
import org.millenaire.building.HearthTemplateSanitizer;
import org.millenaire.building.ImportTableIdentifierValidator;
import org.millenaire.building.ImportTablePlanResolver;
import org.millenaire.building.TemplateLoader;
import org.millenaire.content.ContentFs;
import org.millenaire.content.Resource;
import org.millenaire.culture.ModCultures;
import org.millenaire.world.BuildingPlacer;
import org.slf4j.Logger;

public final class BuildingImporter {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int IMPORT_ALL_SPACING = 4;
    private static final BlockState WHITE_WOOL = Blocks.WHITE_WOOL.defaultBlockState();
    private static final BlockState ORANGE_WOOL = Blocks.ORANGE_WOOL.defaultBlockState();
    private static final BlockState YELLOW_WOOL = Blocks.YELLOW_WOOL.defaultBlockState();
    private static final int CLEAR_ABOVE = 20;

    private BuildingImporter() {
    }

    public static void importLevelFromCulture(ServerLevel level, ImportTableBlockEntity be, ServerPlayer player, String cultureKey, String buildingId, String variant, int upgradeLevel, String parentBuildingId, String parentVariant) {
        boolean processMockBlocks;
        ResourceLocation cultureRl;
        Optional<ImportTablePlanResolver.LevelView> levelOpt = ImportTablePlanResolver.resolveLevel(cultureKey, buildingId, variant, upgradeLevel);
        if (levelOpt.isEmpty()) {
            ResourceLocation planSetId = ResourceLocation.tryParse((String)(cultureKey + "/" + buildingId));
            if (planSetId == null) {
                player.sendSystemMessage((Component)Component.literal((String)("\u00a7c[ImportTable] Invalid building ID: " + buildingId)));
                return;
            }
            if (ModCultures.getBuildingPlanSet(planSetId) == null) {
                player.sendSystemMessage((Component)Component.literal((String)("\u00a7c[ImportTable] Building plan set not found: " + String.valueOf((Object)planSetId))));
                return;
            }
            player.sendSystemMessage((Component)Component.literal((String)("\u00a7c[ImportTable] Variant '" + variant + "' level " + upgradeLevel + " not found for " + buildingId)));
            return;
        }
        ImportTablePlanResolver.LevelView levelView = levelOpt.get();
        boolean realIsSub = BuildingImporter.readIsSubBuildingFlag(cultureKey, buildingId);
        int resolvedTrigger = -1;
        if (realIsSub) {
            if (parentBuildingId == null || parentBuildingId.isEmpty()) {
                player.sendSystemMessage((Component)Component.literal((String)("\u00a7c[ImportTable] '" + buildingId + "' is a sub-building \u2014 select it under a parent so a base can be placed")));
                return;
            }
            Optional<Integer> triggerOpt = BuildingImporter.resolveTriggerLevel(level, cultureKey, parentBuildingId, parentVariant, buildingId);
            if (triggerOpt.isEmpty()) {
                player.sendSystemMessage((Component)Component.literal((String)("\u00a7c[ImportTable] Parent '" + parentBuildingId + (String)(parentVariant == null || parentVariant.isEmpty() ? "" : "/" + parentVariant) + "' does not reference sub-building '" + buildingId + "'")));
                return;
            }
            resolvedTrigger = triggerOpt.get();
        }
        if ((cultureRl = ResourceLocation.tryParse((String)cultureKey)) == null) {
            player.sendSystemMessage((Component)Component.literal((String)("\u00a7c[ImportTable] Invalid culture key: " + cultureKey)));
            return;
        }
        ContentFs cultureFs = TemplateLoader.cultureFsForImport(cultureRl);
        Optional<StructureTemplate> templateOpt = TemplateLoader.loadFromPath(levelView.nbtPath(), level, cultureFs);
        if (templateOpt.isEmpty()) {
            player.sendSystemMessage((Component)Component.literal((String)("\u00a7c[ImportTable] Template not found: " + levelView.nbtPath())));
            return;
        }
        boolean isFreshLoad = !buildingId.equals(be.getBuildingId()) || !variant.equals(be.getVariant()) || !cultureKey.equals(be.getCultureKey());
        be.setBuildingId(buildingId);
        be.setVariant(variant);
        be.setCultureKey(cultureKey);
        be.setUpgradeLevel(upgradeLevel);
        be.setWidth(levelView.width());
        be.setLength(levelView.depth());
        be.setHeight(levelView.height());
        be.setStartingLevel(levelView.groundLevel());
        if (realIsSub) {
            be.setParentBuildingId(parentBuildingId);
            be.setParentVariant(parentVariant == null ? "" : parentVariant);
            be.setParentTriggerLevel(resolvedTrigger);
        } else {
            be.clearParentContext();
        }
        if (isFreshLoad) {
            be.setOrientation(levelView.buildingOrientation());
        }
        be.setImportedFromCulture(true);
        BuildingImporter.clearPlotVolume(level, be);
        BuildingImporter.placePreviousLevels(level, be, upgradeLevel, player);
        Rotation rotation = Rotation.NONE;
        boolean bl = processMockBlocks = !be.isImportMockBlocks();
        if (!be.hasParentContext() && upgradeLevel == 0) {
            BuildingImporter.placeTemplateWithFixes(level, be, templateOpt.get());
        } else {
            BlockPos origin = BuildingExporter.computeScanOrigin(be);
            BuildingPlacer.placeUpgradeFromTemplate(level, templateOpt.get(), origin, rotation, processMockBlocks);
        }
        HearthLightingUtil.lightHearthsInArea(level, BuildingExporter.computeScanOrigin(be), BuildingExporter.computeScanSize(be));
        be.captureSavedState(BuildingExporter.computeBlocksHash(level, be));
        be.setDirty(false);
        be.markImportSettlePending();
        BuildingImporter.placeConstructionBorder(level, be);
        BuildingImporter.placeImportLabel(level, be, BuildingImporter.labelFor(buildingId, variant, upgradeLevel));
        player.sendSystemMessage((Component)Component.literal((String)("\u00a7a[ImportTable] Imported " + buildingId + " " + variant + " level " + upgradeLevel)));
    }

    public static void importLevelFromExports(ServerLevel level, ImportTableBlockEntity be, ServerPlayer player, String buildingId, String variant, int upgradeLevel) {
        Optional<Path> nbtPathOpt = BuildingExporter.findExportedNbt(level, buildingId, variant, upgradeLevel);
        String nbtFile = buildingId + "_" + variant + "_" + upgradeLevel + ".nbt";
        if (nbtPathOpt.isEmpty()) {
            player.sendSystemMessage((Component)Component.literal((String)("\u00a7c[ImportTable] NBT file not found: " + nbtFile)));
            return;
        }
        Path nbtPath = nbtPathOpt.get();
        Path exportDir = nbtPath.getParent();
        if (BuildingImporter.readExportIsSubBuildingFlag(exportDir, buildingId)) {
            player.sendSystemMessage((Component)Component.literal((String)("\u00a7c[ImportTable] '" + buildingId + "' is a sub-building \u2014 edit it via its culture's parent grouping, not the exports tab")));
            return;
        }
        StructureTemplate template = new StructureTemplate();
        try (InputStream is = Files.newInputStream(nbtPath, new OpenOption[0]);){
            CompoundTag nbt = NbtIo.readCompressed((InputStream)is, (NbtAccounter)NbtAccounter.unlimitedHeap());
            HearthTemplateSanitizer.sanitize(nbt, null);
            template.load((HolderGetter)level.holderLookup(Registries.BLOCK), nbt);
        }
        catch (IOException e) {
            LOGGER.error("Failed to load NBT: {}", (Object)nbtPath, (Object)e);
            player.sendSystemMessage((Component)Component.literal((String)("\u00a7c[ImportTable] Failed to load NBT: " + e.getMessage())));
            return;
        }
        boolean isFreshLoad = !buildingId.equals(be.getBuildingId()) || !variant.equals(be.getVariant());
        BuildingImporter.loadDimensionsFromJson(exportDir, buildingId, variant, upgradeLevel, be, isFreshLoad);
        be.setBuildingId(buildingId);
        be.setVariant(variant);
        be.setUpgradeLevel(upgradeLevel);
        if (isFreshLoad) {
            be.setImportedFromCulture(false);
            be.clearParentContext();
        }
        BuildingImporter.clearPlotVolume(level, be);
        BuildingImporter.placePreviousLevels(level, be, upgradeLevel, player);
        if (upgradeLevel == 0) {
            BuildingImporter.placeTemplateWithFixes(level, be, template);
        } else {
            Rotation rotation = Rotation.NONE;
            boolean processMockBlocks = !be.isImportMockBlocks();
            BlockPos origin = BuildingExporter.computeScanOrigin(be);
            BuildingPlacer.placeUpgradeFromTemplate(level, template, origin, rotation, processMockBlocks);
        }
        HearthLightingUtil.lightHearthsInArea(level, BuildingExporter.computeScanOrigin(be), BuildingExporter.computeScanSize(be));
        be.captureSavedState(BuildingExporter.computeBlocksHash(level, be));
        be.setDirty(false);
        be.markImportSettlePending();
        BuildingImporter.placeConstructionBorder(level, be);
        BuildingImporter.placeImportLabel(level, be, BuildingImporter.labelFor(buildingId, variant, upgradeLevel));
        player.sendSystemMessage((Component)Component.literal((String)("\u00a7a[ImportTable] Imported " + buildingId + " " + variant + " level " + upgradeLevel + " from exports")));
    }

    public static void importAllFromCulture(ServerLevel level, ImportTableBlockEntity be, ServerPlayer player, String cultureKey, String buildingId, String variant) {
        ImportAabb aabb = BuildingImporter.importAllVariantsFromCulture(level, be, player, cultureKey, buildingId);
        if (aabb != null) {
            player.sendSystemMessage((Component)Component.literal((String)("\u00a7a[ImportTable] Imported all plans of " + buildingId)));
            BuildingImporter.announceFromExports(player, cultureKey, buildingId);
        }
    }

    public static void announceFromExports(ServerPlayer player, String cultureKey, String buildingId) {
        if (ImportTablePlanResolver.isServedFromExports(cultureKey, buildingId)) {
            ImportTablePlanResolver.formatFromExportsMessage("ImportTable", List.of(buildingId)).ifPresent(msg -> player.sendSystemMessage((Component)Component.literal((String)msg)));
        }
    }

    @Nullable
    public static ImportAabb importAllVariantsFromCulture(ServerLevel level, ImportTableBlockEntity be, ServerPlayer player, String cultureKey, String buildingId) {
        Optional<ImportTablePlanResolver.PlanView> planOpt = ImportTablePlanResolver.resolvePlan(cultureKey, buildingId);
        if (planOpt.isEmpty()) {
            ResourceLocation planSetId = ResourceLocation.tryParse((String)(cultureKey + "/" + buildingId));
            if (planSetId == null) {
                return null;
            }
            if (ModCultures.getBuildingPlanSet(planSetId) == null) {
                player.sendSystemMessage((Component)Component.literal((String)("\u00a7c[ImportTable] Building plan set not found: " + String.valueOf((Object)planSetId))));
            } else {
                player.sendSystemMessage((Component)Component.literal((String)("\u00a7c[ImportTable] Could not read on-disk JSON for " + buildingId)));
            }
            return null;
        }
        ImportTablePlanResolver.PlanView planView = planOpt.get();
        if (BuildingImporter.readIsSubBuildingFlag(cultureKey, buildingId)) {
            player.sendSystemMessage((Component)Component.literal((String)("\u00a7c[ImportTable] '" + buildingId + "' is a sub-building \u2014 import it under its parent (per-level), not via Import All")));
            return null;
        }
        be.setIsMainTable(true);
        BlockPos mainPos = be.getBlockPos();
        Direction facing = (Direction)level.getBlockState(mainPos).getValue(ImportTableBlock.FACING);
        int globalMinX = Integer.MAX_VALUE;
        int globalMaxX = Integer.MIN_VALUE;
        int globalMinZ = Integer.MAX_VALUE;
        int globalMaxZ = Integer.MIN_VALUE;
        int imported = 0;
        int nextVariantMinX = Integer.MIN_VALUE;
        ArrayList<String> variants = new ArrayList<String>(planView.variants().keySet());
        for (String variantKey : variants) {
            ImportTablePlanResolver.VariantView variantView = planView.variants().get(variantKey);
            if (variantView == null || variantView.levels().isEmpty()) continue;
            List sortedLevels = variantView.levels().keySet().stream().sorted().toList();
            int variantMinX = Integer.MAX_VALUE;
            int variantMaxX = Integer.MIN_VALUE;
            int nextLevelMinZ = Integer.MIN_VALUE;
            boolean variantAnchored = false;
            int importedInVariant = 0;
            Iterator iterator = sortedLevels.iterator();
            while (iterator.hasNext()) {
                ImportTableBlockEntity targetBe;
                int levelNum = (Integer)iterator.next();
                if (imported == 0) {
                    targetBe = be;
                } else {
                    int desiredMinZ;
                    int desiredMinX;
                    if (!variantAnchored) {
                        desiredMinX = nextVariantMinX;
                        desiredMinZ = mainPos.getZ();
                    } else {
                        desiredMinX = variantMinX;
                        desiredMinZ = nextLevelMinZ;
                    }
                    BlockPos tablePos = BuildingImporter.tablePosForBuildingAabb(desiredMinX, desiredMinZ, mainPos.getY());
                    level.setBlock(tablePos, (BlockState)((ImportTableBlock)((Object)ModBlocks.IMPORT_TABLE.get())).defaultBlockState().setValue(ImportTableBlock.FACING, (Comparable)facing), 2);
                    BlockEntity childBlockEntity = level.getBlockEntity(tablePos);
                    if (!(childBlockEntity instanceof ImportTableBlockEntity)) {
                        LOGGER.warn("Failed to create child ImportTable at {}", (Object)tablePos);
                        continue;
                    }
                    ImportTableBlockEntity childBe = (ImportTableBlockEntity)childBlockEntity;
                    be.copySettingsTo(childBe);
                    targetBe = childBe;
                }
                BuildingImporter.importLevelFromCulture(level, targetBe, player, cultureKey, buildingId, variantKey, levelNum, "", "");
                int actualOrient = BuildingImporter.normaliseOrientation(targetBe.getOrientation());
                int actualW = targetBe.getWidth();
                int actualL = targetBe.getLength();
                int[] aabb = BuildingImporter.buildingAabb(targetBe.getBlockPos(), actualOrient, actualW, actualL);
                int aMinX = aabb[0];
                int aMinZ = aabb[1];
                int aMaxX = aabb[2];
                int aMaxZ = aabb[3];
                if (!variantAnchored) {
                    variantMinX = aMinX;
                    variantMaxX = aMaxX;
                    variantAnchored = true;
                } else {
                    variantMinX = Math.min(variantMinX, aMinX);
                    variantMaxX = Math.max(variantMaxX, aMaxX);
                }
                nextLevelMinZ = aMaxZ + 1 + 4;
                globalMinX = Math.min(globalMinX, aMinX);
                globalMinZ = Math.min(globalMinZ, aMinZ);
                globalMaxX = Math.max(globalMaxX, aMaxX);
                globalMaxZ = Math.max(globalMaxZ, aMaxZ);
                ++imported;
                ++importedInVariant;
            }
            if (importedInVariant <= 0) continue;
            nextVariantMinX = variantMaxX + 1 + 4;
        }
        if (imported == 0) {
            return null;
        }
        return new ImportAabb(globalMinX, globalMinZ, globalMaxX, globalMaxZ);
    }

    private static int normaliseOrientation(int orient) {
        return (orient % 4 + 4) % 4;
    }

    public static int[] buildingAabb(BlockPos tablePos, int orient, int w, int l) {
        int tx = tablePos.getX();
        int tz = tablePos.getZ();
        return new int[]{tx + 1, tz + 1, tx + w, tz + l};
    }

    public static BlockPos tablePosForBuildingAabb(int worldMinX, int worldMinZ, int y) {
        return new BlockPos(worldMinX - 1, y, worldMinZ - 1);
    }

    @Deprecated
    public static BlockPos tablePosForBuildingAabb(int worldMinX, int worldMinZ, int y, int orient, int w, int l) {
        return BuildingImporter.tablePosForBuildingAabb(worldMinX, worldMinZ, y);
    }

    public static void importAllFromExports(ServerLevel level, ImportTableBlockEntity be, ServerPlayer player, String buildingId, String variant) {
        if (BuildingExporter.findExportedNbt(level, buildingId, variant, 0).isEmpty()) {
            player.sendSystemMessage((Component)Component.literal((String)("\u00a7c[ImportTable] No levels found for " + buildingId + " " + variant)));
            return;
        }
        Path exportJson = BuildingExporter.findExportedJson(level, buildingId).orElse(null);
        if (exportJson != null && BuildingImporter.readExportIsSubBuildingFlag(exportJson.getParent(), buildingId)) {
            player.sendSystemMessage((Component)Component.literal((String)("\u00a7c[ImportTable] '" + buildingId + "' is a sub-building \u2014 edit it via its culture's parent grouping, not the exports tab")));
            return;
        }
        int maxLevel = 0;
        while (BuildingExporter.findExportedNbt(level, buildingId, variant, maxLevel + 1).isPresent()) {
            ++maxLevel;
        }
        be.setIsMainTable(true);
        be.setBuildingId(buildingId);
        be.setVariant(variant);
        int totalLevels = maxLevel + 1;
        BuildingImporter.importAllLevels(level, be, player, totalLevels, (lvl, childBe) -> BuildingImporter.importLevelFromExports(level, childBe, player, buildingId, variant, lvl));
        player.sendSystemMessage((Component)Component.literal((String)("\u00a7a[ImportTable] Imported all " + totalLevels + " levels of " + buildingId + " " + variant + " from exports")));
    }

    public static void reimport(ServerLevel level, ImportTableBlockEntity be, ServerPlayer player) {
        if (!be.hasPlan()) {
            player.sendSystemMessage((Component)Component.literal((String)"\u00a7c[ImportTable] No plan loaded on this table"));
            return;
        }
        String cultureKey = be.getCultureKey();
        if (cultureKey.isEmpty() || cultureKey.startsWith("millenaire-custom")) {
            BuildingImporter.importLevelFromExports(level, be, player, be.getBuildingId(), be.getVariant(), be.getUpgradeLevel());
        } else {
            BuildingImporter.importLevelFromCulture(level, be, player, cultureKey, be.getBuildingId(), be.getVariant(), be.getUpgradeLevel(), be.getParentBuildingId(), be.getParentVariant());
        }
    }

    public static void reimportAll(ServerLevel level, ImportTableBlockEntity be, ServerPlayer player) {
        ImportTableBlockEntity mainTable = be.resolveMainTable(level);
        mainTable.propagatePlanIdentity(level);
        BuildingImporter.reimport(level, mainTable, player);
        List<ImportTableBlockEntity> children = mainTable.findChildTables(level);
        for (ImportTableBlockEntity child : children) {
            BuildingImporter.reimport(level, child, player);
        }
        player.sendSystemMessage((Component)Component.literal((String)("\u00a7a[ImportTable] Re-imported all " + (1 + children.size()) + " levels")));
        BuildingImporter.announceFromExports(player, mainTable.getCultureKey(), mainTable.getBuildingId());
    }

    public static void reimportDependentLevels(ServerLevel level, ImportTableBlockEntity exported, ServerPlayer player) {
        ImportTableBlockEntity main = exported.resolveMainTable(level);
        if (!main.isMainTable()) {
            return;
        }
        String variant = exported.getVariant();
        int exportedLevel = exported.getUpgradeLevel();
        ArrayList<Integer> skippedDirty = new ArrayList<Integer>();
        for (ImportTableBlockEntity table : main.findChildTables(level)) {
            if (table == exported || !variant.equals(table.getVariant()) || table.getUpgradeLevel() <= exportedLevel) continue;
            if (table.hasUnsavedChanges(level)) {
                skippedDirty.add(table.getUpgradeLevel());
                continue;
            }
            BuildingImporter.reimport(level, table, player);
        }
        if (!skippedDirty.isEmpty()) {
            skippedDirty.sort(Integer::compareTo);
            StringBuilder levels = new StringBuilder();
            for (int i = 0; i < skippedDirty.size(); ++i) {
                if (i > 0) {
                    levels.append(", ");
                }
                levels.append(skippedDirty.get(i));
            }
            player.sendSystemMessage((Component)Component.literal((String)("\u00a7e[ImportTable] Did not refresh level(s) " + String.valueOf(levels) + " (unsaved edits) \u2014 export them before Re-Import All")));
        }
    }

    static String sanitizeBuildingName(String input) {
        if (input == null) {
            return "";
        }
        String s = input.trim().toLowerCase(Locale.ROOT).replace(' ', '_').replaceAll("[^a-z0-9_\\-]", "");
        return s;
    }

    public static void createNewBuilding(ServerLevel level, ImportTableBlockEntity be, ServerPlayer player, int length, int width, int startingLevel, int height, boolean clearGround, int numberOfUpgrades, boolean preBuilt, String buildingName) {
        Object buildingId;
        String sanitized;
        Path exportDir = BuildingExporter.getExportDir(level, "", "lone");
        try {
            Files.createDirectories(exportDir, new FileAttribute[0]);
        }
        catch (IOException e) {
            LOGGER.error("Failed to create export dir", (Throwable)e);
            return;
        }
        if (preBuilt && numberOfUpgrades > 0) {
            int spacing = length + 4;
            BlockPos mainPos = be.getBlockPos();
            for (int lvl = 1; lvl <= numberOfUpgrades; ++lvl) {
                BlockPos childPos = mainPos.offset(0, 0, spacing * lvl);
                if (level.getBlockEntity(childPos) == null) continue;
                player.sendSystemMessage((Component)Component.literal((String)("\u00a7c[ImportTable] Level " + lvl + " table position " + childPos.toShortString() + " is obstructed (block entity present) \u2014 clear the row and retry. Expected spacing: " + spacing + " blocks (Z+).")));
                return;
            }
        }
        if ((sanitized = BuildingImporter.sanitizeBuildingName(buildingName)).isEmpty()) {
            int nextN = 0;
            while (BuildingExporter.findExportedJson(level, "export" + nextN).isPresent()) {
                ++nextN;
            }
            buildingId = "export" + nextN;
        } else if (BuildingExporter.findExportedJson(level, sanitized).isEmpty()) {
            buildingId = sanitized;
        } else {
            int suffix = 2;
            while (BuildingExporter.findExportedJson(level, sanitized + "_" + suffix).isPresent()) {
                ++suffix;
            }
            buildingId = sanitized + "_" + suffix;
        }
        Object nativeName = buildingName != null && !buildingName.isBlank() ? buildingName.trim() : buildingId;
        be.setBuildingId((String)buildingId);
        be.setVariant("a");
        be.setUpgradeLevel(0);
        be.setLength(length);
        be.setWidth(width);
        be.setStartingLevel(startingLevel);
        be.setHeight(height);
        be.setClearGround(clearGround);
        be.setCultureKey("");
        be.setOrientation(1);
        be.setImportedFromCulture(false);
        Path jsonPath = exportDir.resolve((String)buildingId + ".json");
        JsonObject root = new JsonObject();
        root.addProperty("culture", "millenaire:custom");
        root.addProperty("building_id", (String)buildingId);
        root.addProperty("category", "extra");
        root.addProperty("native_name", (String)nativeName);
        JsonArray variants = new JsonArray();
        JsonObject variantObj = new JsonObject();
        variantObj.addProperty("variant", "a");
        variantObj.addProperty("building_orientation", (Number)be.getOrientation());
        JsonArray levels = new JsonArray();
        JsonObject levelObj = new JsonObject();
        levelObj.addProperty("level", (Number)0);
        JsonObject footprint = new JsonObject();
        footprint.addProperty("width", (Number)width);
        footprint.addProperty("height", (Number)height);
        footprint.addProperty("depth", (Number)length);
        levelObj.add("footprint", (JsonElement)footprint);
        levelObj.addProperty("ground_level", (Number)startingLevel);
        levels.add((JsonElement)levelObj);
        variantObj.add("levels", (JsonElement)levels);
        variants.add((JsonElement)variantObj);
        root.add("variants", (JsonElement)variants);
        if (clearGround) {
            BuildingImporter.clearBuildingGround(level, be);
        }
        BlockPos scanOrigin = BuildingExporter.computeScanOrigin(be);
        if (preBuilt) {
            int detectedH = BuildingExporter.detectBuildingHeight(level, scanOrigin, be, 0);
            be.setHeight(detectedH);
            footprint.addProperty("height", (Number)detectedH);
        }
        Vec3i scanSize = BuildingExporter.computeScanSize(be);
        StructureTemplate template = new StructureTemplate();
        template.fillFromWorld((Level)level, scanOrigin, scanSize, false, Blocks.AIR);
        CompoundTag nbt = template.save(new CompoundTag());
        Path nbtPath = exportDir.resolve((String)buildingId + "_a_0.nbt");
        try {
            NbtIo.writeCompressed((CompoundTag)nbt, (Path)nbtPath);
        }
        catch (IOException e) {
            LOGGER.error("Failed to write building NBT", (Throwable)e);
            player.sendSystemMessage((Component)Component.literal((String)("\u00a7c[ImportTable] Failed to create NBT: " + e.getMessage())));
            return;
        }
        try {
            Files.writeString(jsonPath, (CharSequence)new GsonBuilder().setPrettyPrinting().create().toJson((JsonElement)root), new OpenOption[0]);
        }
        catch (IOException e) {
            LOGGER.error("Failed to write building JSON", (Throwable)e);
        }
        be.captureSavedState(BuildingExporter.computeBlocksHash(level, be));
        be.setDirty(false);
        BuildingImporter.placeConstructionBorder(level, be);
        if (preBuilt && numberOfUpgrades > 0) {
            be.setIsMainTable(true);
            BuildingImporter.placeImportLabel(level, be, BuildingImporter.labelFor((String)buildingId, "a", 0));
            int spacing = length + 4;
            BlockPos mainPos = be.getBlockPos();
            BlockState mainFacing = level.getBlockState(mainPos);
            int captured = 0;
            for (int lvl = 1; lvl <= numberOfUpgrades; ++lvl) {
                BlockPos childPos = mainPos.offset(0, 0, spacing * lvl);
                level.setBlock(childPos, (BlockState)((ImportTableBlock)((Object)ModBlocks.IMPORT_TABLE.get())).defaultBlockState().setValue(ImportTableBlock.FACING, (Comparable)((Direction)mainFacing.getValue(ImportTableBlock.FACING))), 2);
                ImportTableBlockEntity childBe = (ImportTableBlockEntity)level.getBlockEntity(childPos);
                if (childBe == null) {
                    LOGGER.warn("[ImportTable] Failed to create child table at {} for level {}", (Object)childPos, (Object)lvl);
                    player.sendSystemMessage((Component)Component.literal((String)("\u00a7c[ImportTable] Failed to place child table for level " + lvl + " at " + childPos.toShortString() + " \u2014 stopping at " + captured + " captured level(s).")));
                    break;
                }
                be.copySettingsTo(childBe);
                childBe.setBuildingId((String)buildingId);
                childBe.setVariant("a");
                childBe.setUpgradeLevel(lvl);
                childBe.setCultureKey("");
                childBe.setImportedFromCulture(false);
                if (!BuildingExporter.captureAndWriteLevel(level, childBe, exportDir, player)) break;
                BuildingImporter.placeConstructionBorder(level, childBe);
                BuildingImporter.placeImportLabel(level, childBe, BuildingImporter.labelFor((String)buildingId, "a", lvl));
                ++captured;
            }
            BuildingExporter.refreshOverlay();
            player.sendSystemMessage((Component)Component.literal((String)("\u00a7a[ImportTable] Created pre-built building: " + (String)buildingId + " \u2014 " + (captured + 1) + " level(s) captured (" + width + "x" + length + ")")));
        } else {
            BuildingExporter.exportAdditionalLevels(level, be, player, numberOfUpgrades);
            player.sendSystemMessage((Component)Component.literal((String)("\u00a7a[ImportTable] Created new building: " + (String)buildingId + " (" + width + "x" + length + ")")));
        }
    }

    @Nullable
    public static String createSubBuilding(ServerLevel level, ImportTableBlockEntity be, ServerPlayer player, String parentCulture, String parentId, String parentVariant, int triggerLevel, String subName) {
        if (parentCulture == null || parentCulture.isEmpty() || parentId == null || parentId.isEmpty() || parentVariant == null || parentVariant.isEmpty() || subName == null || subName.isEmpty()) {
            player.sendSystemMessage((Component)Component.literal((String)"\u00a7c[ImportTable] create-sub requires culture, parent, variant and a sub name"));
            return null;
        }
        if (triggerLevel < 0) {
            player.sendSystemMessage((Component)Component.literal((String)"\u00a7c[ImportTable] trigger level must be >= 0"));
            return null;
        }
        if (BuildingImporter.readIsSubBuildingFlag(parentCulture, parentId)) {
            player.sendSystemMessage((Component)Component.literal((String)("\u00a7c[ImportTable] '" + parentId + "' is itself a sub-building \u2014 cannot parent another sub")));
            return null;
        }
        Optional<ImportTablePlanResolver.LevelView> parentLevelOpt = ImportTablePlanResolver.resolveLevel(parentCulture, parentId, parentVariant, triggerLevel);
        if (parentLevelOpt.isEmpty()) {
            player.sendSystemMessage((Component)Component.literal((String)("\u00a7c[ImportTable] Parent '" + parentId + "/" + parentVariant + "' level " + triggerLevel + " not found")));
            return null;
        }
        ImportTablePlanResolver.LevelView parentLevel = parentLevelOpt.get();
        String subId = parentId + "_" + parentVariant + "_" + subName;
        if (!ImportTableIdentifierValidator.isValidIdentifier(subId) || !subId.equals(subId.toLowerCase(Locale.ROOT))) {
            player.sendSystemMessage((Component)Component.literal((String)("\u00a7c[ImportTable] Invalid sub id '" + subId + "' \u2014 use lowercase [a-z0-9_-]")));
            return null;
        }
        ResourceLocation subPlanId = ResourceLocation.tryParse((String)(parentCulture + "/" + subId));
        Path subExportJson = BuildingExporter.getExportDir(level, parentCulture, BuildingExporter.categoryFor(parentCulture, parentId)).resolve(subId + ".json");
        if (subPlanId != null && ModCultures.getBuildingPlanSet(subPlanId) != null || Files.exists(subExportJson, new LinkOption[0])) {
            player.sendSystemMessage((Component)Component.literal((String)("\u00a7c[ImportTable] '" + subId + "' already exists \u2014 pick another name")));
            return null;
        }
        be.setBuildingId(subId);
        be.setVariant("a");
        be.setCultureKey(parentCulture);
        be.setUpgradeLevel(0);
        be.setWidth(parentLevel.width());
        be.setLength(parentLevel.depth());
        be.setHeight(parentLevel.height());
        be.setStartingLevel(parentLevel.groundLevel());
        be.setOrientation(parentLevel.buildingOrientation());
        be.setParentBuildingId(parentId);
        be.setParentVariant(parentVariant);
        be.setParentTriggerLevel(triggerLevel);
        be.setImportMockBlocks(true);
        be.setImportedFromCulture(true);
        BuildingImporter.clearPlotVolume(level, be);
        BuildingImporter.placePreviousLevels(level, be, 0, player);
        HearthLightingUtil.lightHearthsInArea(level, BuildingExporter.computeScanOrigin(be), BuildingExporter.computeScanSize(be));
        be.captureSavedState(BuildingExporter.computeBlocksHash(level, be));
        be.setDirty(false);
        be.markImportSettlePending();
        BuildingImporter.placeConstructionBorder(level, be);
        BuildingImporter.placeImportLabel(level, be, BuildingImporter.labelFor(subId, "a", 0));
        player.sendSystemMessage((Component)Component.literal((String)("\u00a7a[ImportTable] Created sub-building '" + subId + "' on " + parentId + "/" + parentVariant + " at level " + triggerLevel + " \u2014 build on top, then Export")));
        return subId;
    }

    static List<BaseLayer> computeBaseLayers(ImportTableBlockEntity be) {
        return BuildingImporter.computeBaseLayers(be.getBuildingId(), be.getVariant(), be.getUpgradeLevel(), be.hasParentContext() ? be.getParentBuildingId() : "", be.getParentVariant(), be.getParentTriggerLevel());
    }

    static List<BaseLayer> computeBaseLayers(String selfId, String selfVariant, int selfLevel, String parentId, String parentVariant, int triggerLevel) {
        int lvl;
        boolean hasParent;
        ArrayList<BaseLayer> layers = new ArrayList<BaseLayer>();
        boolean bl = hasParent = parentId != null && !parentId.isEmpty();
        if (hasParent) {
            for (lvl = 0; lvl <= triggerLevel; ++lvl) {
                layers.add(new BaseLayer(parentId, parentVariant, lvl));
            }
        }
        for (lvl = 0; lvl < selfLevel; ++lvl) {
            layers.add(new BaseLayer(selfId, selfVariant, lvl));
        }
        return layers;
    }

    public static Optional<Integer> resolveTriggerLevel(ServerLevel level, String cultureKey, String parentId, String parentVariant, String subId) {
        JsonObject parentRoot = BuildingImporter.readBuildingJson(cultureKey, parentId).orElse(null);
        if (parentRoot == null) {
            return Optional.empty();
        }
        return BuildingImporter.resolveTriggerLevelFromJson(parentRoot, parentId, parentVariant, subId);
    }

    public static List<SubRef> listReferencedSubs(String cultureKey, String parentId) {
        JsonObject root = BuildingImporter.readBuildingJson(cultureKey, parentId).orElse(null);
        if (root == null) {
            return List.of();
        }
        LinkedHashMap<String, String> byId = new LinkedHashMap<String, String>();
        if (root.has("starting_sub_buildings")) {
            for (JsonElement e2 : root.getAsJsonArray("starting_sub_buildings")) {
                if (!e2.isJsonPrimitive()) continue;
                byId.putIfAbsent(e2.getAsString(), "");
            }
        }
        if (root.has("variants")) {
            for (JsonElement ve : root.getAsJsonArray("variants")) {
                JsonObject v = ve.getAsJsonObject();
                if (!v.has("variant") || !v.has("levels")) continue;
                String variant = v.get("variant").getAsString();
                for (JsonElement le : v.getAsJsonArray("levels")) {
                    JsonArray subs;
                    JsonArray jsonArray = subs = le.getAsJsonObject().has("sub_buildings") ? le.getAsJsonObject().getAsJsonArray("sub_buildings") : null;
                    if (subs == null) continue;
                    for (JsonElement se : subs) {
                        if (!se.isJsonPrimitive()) continue;
                        byId.putIfAbsent(se.getAsString(), variant);
                    }
                }
            }
        }
        return byId.entrySet().stream().sorted(Map.Entry.comparingByKey(String.CASE_INSENSITIVE_ORDER)).map(e -> new SubRef((String)e.getKey(), (String)e.getValue())).toList();
    }

    static Optional<Integer> resolveTriggerLevelFromJson(JsonObject parentRoot, String parentId, String parentVariant, String subId) {
        JsonArray variants;
        if (BuildingImporter.jsonArrayContains(parentRoot.has("starting_sub_buildings") ? parentRoot.getAsJsonArray("starting_sub_buildings") : null, subId)) {
            return Optional.of(0);
        }
        JsonArray jsonArray = variants = parentRoot.has("variants") ? parentRoot.getAsJsonArray("variants") : null;
        if (variants == null) {
            return Optional.empty();
        }
        Integer found = null;
        for (JsonElement ve : variants) {
            JsonArray levels;
            JsonObject v = ve.getAsJsonObject();
            if (!v.has("variant") || !parentVariant.equals(v.get("variant").getAsString())) continue;
            JsonArray jsonArray2 = levels = v.has("levels") ? v.getAsJsonArray("levels") : null;
            if (levels == null) break;
            for (JsonElement le : levels) {
                JsonArray subs;
                JsonObject l = le.getAsJsonObject();
                if (!l.has("level")) continue;
                JsonArray jsonArray3 = subs = l.has("sub_buildings") ? l.getAsJsonArray("sub_buildings") : null;
                if (!BuildingImporter.jsonArrayContains(subs, subId)) continue;
                int lvl = l.get("level").getAsInt();
                if (found == null) {
                    found = lvl;
                    continue;
                }
                LOGGER.warn("ImportTable: sub '{}' referenced at multiple levels of {} variant {} (at least {} and {}); using lowest ({})", new Object[]{subId, parentId, parentVariant, found, lvl, Math.min(found, lvl)});
                found = Math.min(found, lvl);
            }
        }
        return Optional.ofNullable(found);
    }

    private static boolean jsonArrayContains(@Nullable JsonArray array, String value) {
        if (array == null) {
            return false;
        }
        for (JsonElement e : array) {
            if (!e.isJsonPrimitive() || !value.equals(e.getAsString())) continue;
            return true;
        }
        return false;
    }

    public static boolean readIsSubBuildingFlag(String cultureKey, String buildingId) {
        return BuildingImporter.readBuildingJson(cultureKey, buildingId).map(root -> root.has("is_sub_building") && root.get("is_sub_building").getAsBoolean()).orElse(false);
    }

    private static boolean readExportIsSubBuildingFlag(Path exportDir, String buildingId) {
        if (exportDir == null) {
            return false;
        }
        Path jsonPath = exportDir.resolve(buildingId + ".json");
        if (!Files.exists(jsonPath, new LinkOption[0])) {
            return false;
        }
        try {
            JsonObject root = JsonParser.parseString((String)Files.readString(jsonPath)).getAsJsonObject();
            return root.has("is_sub_building") && root.get("is_sub_building").getAsBoolean();
        }
        catch (Exception e) {
            LOGGER.warn("ImportTable: failed to read export JSON {}: {}", (Object)jsonPath, (Object)e.getMessage());
            return false;
        }
    }

    static Optional<JsonObject> readBuildingJson(String cultureKey, String buildingId) {
        Optional<JsonObject> optional;
        block12: {
            if (cultureKey == null || cultureKey.isEmpty() || buildingId == null || buildingId.isEmpty()) {
                return Optional.empty();
            }
            ResourceLocation planSetId = ResourceLocation.tryParse((String)(cultureKey + "/" + buildingId));
            if (planSetId == null) {
                return Optional.empty();
            }
            String category = ImportTablePlanResolver.resolveCategory(cultureKey, buildingId).orElse("houses");
            ResourceLocation cultureRl = ResourceLocation.tryParse((String)cultureKey);
            if (cultureRl == null) {
                return Optional.empty();
            }
            String jsonRelPath = "buildings/" + category + "/" + buildingId + ".json";
            ContentFs cultureFs = TemplateLoader.cultureFsForImport(cultureRl);
            Optional<Resource> jsonRes = cultureFs.findFirst(jsonRelPath);
            if (jsonRes.isEmpty()) {
                return Optional.empty();
            }
            InputStream is = jsonRes.get().open();
            try {
                String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                optional = Optional.of(JsonParser.parseString((String)content).getAsJsonObject());
                if (is == null) break block12;
            }
            catch (Throwable throwable) {
                try {
                    if (is != null) {
                        try {
                            is.close();
                        }
                        catch (Throwable throwable2) {
                            throwable.addSuppressed(throwable2);
                        }
                    }
                    throw throwable;
                }
                catch (Exception e) {
                    LOGGER.warn("ImportTable: failed to read building JSON {}: {}", (Object)jsonRelPath, (Object)e.getMessage());
                    return Optional.empty();
                }
            }
            is.close();
        }
        return optional;
    }

    static Optional<ResolvedPreviousLevel> resolvePreviousLevel(ServerLevel level, String cultureKey, String buildingId, String variant, int lvl, int fallbackGroundLevel, @Nullable Path exportJsonPath) {
        ImportTablePlanResolver.VariantView variantView;
        boolean hasCulture = cultureKey != null && !cultureKey.isEmpty();
        ImportTablePlanResolver.VariantView variantView2 = variantView = hasCulture ? (ImportTablePlanResolver.VariantView)ImportTablePlanResolver.resolveVariant(cultureKey, buildingId, variant).orElse(null) : null;
        if (variantView != null && variantView.levels().containsKey(lvl)) {
            ImportTablePlanResolver.LevelView lv = variantView.levels().get(lvl);
            ContentFs cultureFs = TemplateLoader.cultureFsForImport(ResourceLocation.parse((String)cultureKey));
            StructureTemplate tmpl = TemplateLoader.loadFromPath(lv.nbtPath(), level, cultureFs).orElse(null);
            return tmpl == null ? Optional.empty() : Optional.of(new ResolvedPreviousLevel(tmpl, lv.groundLevel()));
        }
        StructureTemplate tmpl = TemplateLoader.resolve(level, cultureKey, buildingId, variant, lvl).orElse(null);
        if (tmpl == null) {
            return Optional.empty();
        }
        int gl = BuildingImporter.readGroundLevelFromJsonPath(exportJsonPath, variant, lvl, fallbackGroundLevel);
        return Optional.of(new ResolvedPreviousLevel(tmpl, gl));
    }

    private static void placePreviousLevels(ServerLevel level, ImportTableBlockEntity be, int upgradeLevel, ServerPlayer player) {
        List<BaseLayer> layers = BuildingImporter.computeBaseLayers(be);
        if (layers.isEmpty()) {
            return;
        }
        String cultureKey = be.getCultureKey();
        boolean processMockBlocks = !be.isImportMockBlocks();
        Rotation rotation = Rotation.NONE;
        BlockPos scanOrigin = BuildingExporter.computeScanOrigin(be);
        int currentGroundLevel = be.getStartingLevel();
        int baseY = be.getBlockPos().getY();
        for (int i = 0; i < layers.size(); ++i) {
            BaseLayer layer = layers.get(i);
            Path exportJsonPath = BuildingExporter.findExportedJson(level, layer.buildingId()).orElse(null);
            ResolvedPreviousLevel resolved = BuildingImporter.resolvePreviousLevel(level, cultureKey, layer.buildingId(), layer.variant(), layer.level(), currentGroundLevel, exportJsonPath).orElse(null);
            if (resolved == null) {
                if (i == 0) {
                    player.sendSystemMessage((Component)Component.literal((String)("\u00a7e[ImportTable] Warning: base level template not found (" + layer.buildingId() + " " + layer.variant() + " " + layer.level() + "), skipping previous levels")));
                    return;
                }
                player.sendSystemMessage((Component)Component.literal((String)("\u00a7e[ImportTable] Warning: base level template not found (" + layer.buildingId() + " " + layer.variant() + " " + layer.level() + "), skipping")));
                continue;
            }
            BlockPos originLvl = new BlockPos(scanOrigin.getX(), baseY + resolved.groundLevel(), scanOrigin.getZ());
            if (i == 0) {
                BuildingPlacer.placeFromTemplate(level, resolved.template(), originLvl, rotation, processMockBlocks);
                continue;
            }
            BuildingPlacer.placeUpgradeFromTemplate(level, resolved.template(), originLvl, rotation, processMockBlocks);
        }
    }

    private static int readGroundLevelFromJsonPath(@Nullable Path jsonPath, String variant, int upgradeLevel, int fallback) {
        if (jsonPath == null || !Files.exists(jsonPath, new LinkOption[0])) {
            return fallback;
        }
        try {
            JsonArray variants;
            String content = Files.readString(jsonPath);
            JsonObject root = JsonParser.parseString((String)content).getAsJsonObject();
            JsonArray jsonArray = variants = root.has("variants") ? root.getAsJsonArray("variants") : null;
            if (variants == null) {
                return fallback;
            }
            for (JsonElement ve : variants) {
                JsonArray levels;
                JsonObject v = ve.getAsJsonObject();
                if (!variant.equals(v.get("variant").getAsString())) continue;
                Integer variantGroundLevel = v.has("ground_level") ? Integer.valueOf(v.get("ground_level").getAsInt()) : null;
                JsonArray jsonArray2 = levels = v.has("levels") ? v.getAsJsonArray("levels") : null;
                if (levels != null) {
                    for (JsonElement le : levels) {
                        JsonObject l = le.getAsJsonObject();
                        if (l.get("level").getAsInt() != upgradeLevel) continue;
                        if (!l.has("ground_level")) break;
                        return l.get("ground_level").getAsInt();
                    }
                }
                return variantGroundLevel != null ? variantGroundLevel : fallback;
            }
        }
        catch (Exception e) {
            LOGGER.warn("Failed to read ground_level from export JSON: {}", (Object)jsonPath, (Object)e);
        }
        return fallback;
    }

    private static void placeTemplateWithFixes(ServerLevel level, ImportTableBlockEntity be, StructureTemplate template) {
        BlockPos origin = BuildingExporter.computeScanOrigin(be);
        Rotation rotation = Rotation.NONE;
        boolean processMockBlocks = !be.isImportMockBlocks();
        BuildingPlacer.placeFromTemplate(level, template, origin, rotation, processMockBlocks);
    }

    private static void clearPlotVolume(ServerLevel level, ImportTableBlockEntity be) {
        BlockPos origin = BuildingExporter.computeScanOrigin(be);
        BlockPos tablePos = be.getBlockPos();
        Vec3i size = BuildingExporter.computeScanSize(be);
        int w = size.getX();
        int l = size.getZ();
        int h = size.getY();
        int clearH = h + 20;
        for (int x = 0; x < w; ++x) {
            for (int z = 0; z < l; ++z) {
                BlockPos foundationPos = origin.offset(x, -1, z);
                if (!foundationPos.equals((Object)tablePos)) {
                    level.setBlock(foundationPos, Blocks.STONE.defaultBlockState(), 2);
                }
                for (int y = 0; y < clearH; ++y) {
                    BlockPos pos = origin.offset(x, y, z);
                    if (pos.equals((Object)tablePos)) continue;
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }
    }

    public static void placeConstructionBorder(ServerLevel level, ImportTableBlockEntity be) {
        BlockPos origin = BuildingExporter.computeScanOrigin(be);
        Vec3i size = BuildingExporter.computeScanSize(be);
        int w = size.getX();
        int l = size.getZ();
        int orientation = be.getOrientation();
        BlockPos tablePos = be.getBlockPos();
        int groundY = tablePos.getY() - 1;
        int minX = origin.getX() - 1;
        int maxX = origin.getX() + w;
        int minZ = origin.getZ() - 1;
        int maxZ = origin.getZ() + l;
        BlockState neutralWool = be.isDirty() ? YELLOW_WOOL : WHITE_WOOL;
        BlockState northWool = orientation == 0 ? ORANGE_WOOL : neutralWool;
        for (int x = minX; x <= maxX; ++x) {
            BuildingImporter.placeBorderBlock(level, new BlockPos(x, groundY, minZ), northWool, tablePos);
        }
        BlockState southWool = orientation == 2 ? ORANGE_WOOL : neutralWool;
        for (int x = minX; x <= maxX; ++x) {
            BuildingImporter.placeBorderBlock(level, new BlockPos(x, groundY, maxZ), southWool, tablePos);
        }
        BlockState westWool = orientation == 3 ? ORANGE_WOOL : neutralWool;
        for (int z = minZ; z <= maxZ; ++z) {
            BuildingImporter.placeBorderBlock(level, new BlockPos(minX, groundY, z), westWool, tablePos);
        }
        BlockState eastWool = orientation == 1 ? ORANGE_WOOL : neutralWool;
        for (int z = minZ; z <= maxZ; ++z) {
            BuildingImporter.placeBorderBlock(level, new BlockPos(maxX, groundY, z), eastWool, tablePos);
        }
        int margin = 2;
        int clearMinX = minX - margin;
        int clearMaxX = maxX + margin;
        int clearMinZ = minZ - margin;
        int clearMaxZ = maxZ + margin;
        int clearHeight = size.getY() + 20;
        for (int x = clearMinX; x <= clearMaxX; ++x) {
            for (int z = clearMinZ; z <= clearMaxZ; ++z) {
                boolean insidePlot;
                boolean bl = insidePlot = x >= origin.getX() && x < origin.getX() + w && z >= origin.getZ() && z < origin.getZ() + l;
                if (insidePlot) continue;
                for (int y = 1; y <= clearHeight; ++y) {
                    BlockPos pos = new BlockPos(x, groundY + y, z);
                    if (pos.equals((Object)tablePos)) continue;
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }
    }

    private static void placeBorderBlock(ServerLevel level, BlockPos pos, BlockState wool, BlockPos tablePos) {
        if (!pos.equals((Object)tablePos)) {
            level.setBlock(pos, wool, 2);
        }
    }

    private static void clearBuildingGround(ServerLevel level, ImportTableBlockEntity be) {
        BlockPos origin = BuildingExporter.computeScanOrigin(be);
        int w = be.getWidth();
        int l = be.getLength();
        int h = be.getHeight();
        int surfaceY = be.getBlockPos().getY() - 1;
        int originY = origin.getY();
        for (int x = 0; x < w; ++x) {
            for (int z = 0; z < l; ++z) {
                int y;
                int worldX = origin.getX() + x;
                int worldZ = origin.getZ() + z;
                for (y = originY; y <= surfaceY; ++y) {
                    level.setBlock(new BlockPos(worldX, y, worldZ), Blocks.DIRT.defaultBlockState(), 2);
                }
                for (y = surfaceY + 1; y < originY + h; ++y) {
                    level.setBlock(new BlockPos(worldX, y, worldZ), Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }
    }

    private static void importAllLevels(ServerLevel level, ImportTableBlockEntity mainBe, ServerPlayer player, int totalLevels, LevelImporter importer) {
        mainBe.setUpgradeLevel(0);
        importer.importLevel(0, mainBe);
        int spacing = BuildingImporter.tableZExtent(mainBe) + 4;
        for (int lvl = 1; lvl < totalLevels; ++lvl) {
            BlockPos childPos = mainBe.getBlockPos().offset(0, 0, spacing * lvl);
            level.setBlock(childPos, (BlockState)((ImportTableBlock)((Object)ModBlocks.IMPORT_TABLE.get())).defaultBlockState().setValue(ImportTableBlock.FACING, (Comparable)((Direction)level.getBlockState(mainBe.getBlockPos()).getValue(ImportTableBlock.FACING))), 2);
            ImportTableBlockEntity childBe = (ImportTableBlockEntity)level.getBlockEntity(childPos);
            if (childBe == null) {
                LOGGER.warn("Failed to create child ImportTable at {}", (Object)childPos);
                continue;
            }
            mainBe.copySettingsTo(childBe);
            childBe.setUpgradeLevel(lvl);
            importer.importLevel(lvl, childBe);
        }
    }

    private static int tableZExtent(ImportTableBlockEntity be) {
        return be.getLength();
    }

    private static String labelFor(String buildingId, String variant, int upgradeLevel) {
        if (variant == null || variant.isBlank() || "a".equals(variant)) {
            return buildingId + "_" + upgradeLevel;
        }
        return buildingId + "_" + variant + "_" + upgradeLevel;
    }

    private static void placeImportLabel(ServerLevel level, ImportTableBlockEntity be, String label) {
        BlockPos labelPos = be.getBlockPos().offset(0, 0, -1);
        BlockState state = (BlockState)Blocks.OAK_SIGN.defaultBlockState().setValue((Property)StandingSignBlock.ROTATION, (Comparable)Integer.valueOf(8));
        level.setBlock(labelPos, state, 2);
        BlockEntity blockEntity = level.getBlockEntity(labelPos);
        if (blockEntity instanceof SignBlockEntity) {
            SignBlockEntity sign = (SignBlockEntity)blockEntity;
            SignText text = new SignText().setMessage(0, (Component)Component.literal((String)label)).setColor(DyeColor.BLACK);
            sign.setText(text, true);
            sign.setText(text, false);
            sign.setChanged();
            level.sendBlockUpdated(labelPos, state, state, 3);
        }
    }

    private static void loadDimensionsFromJson(Path exportDir, String buildingId, String variant, int upgradeLevel, ImportTableBlockEntity be, boolean isFreshLoad) {
        Path jsonPath = exportDir.resolve(buildingId + ".json");
        if (!Files.exists(jsonPath, new LinkOption[0])) {
            return;
        }
        try {
            String content = Files.readString(jsonPath);
            JsonObject root = JsonParser.parseString((String)content).getAsJsonObject();
            BuildingImporter.applyJsonOverrides(root, variant, upgradeLevel, be, isFreshLoad);
        }
        catch (Exception e) {
            LOGGER.warn("Failed to read export JSON: {}", (Object)jsonPath, (Object)e);
        }
    }

    private static void applyJsonOverrides(JsonObject root, String variant, int upgradeLevel, ImportTableBlockEntity be, boolean isFreshLoad) {
        JsonArray variants = root.getAsJsonArray("variants");
        if (variants == null) {
            return;
        }
        Integer planSetOrientation = root.has("building_orientation") ? Integer.valueOf(root.get("building_orientation").getAsInt()) : null;
        for (JsonElement ve : variants) {
            JsonArray levels;
            JsonObject v = ve.getAsJsonObject();
            if (!variant.equals(v.get("variant").getAsString())) continue;
            Integer variantGroundLevel = v.has("ground_level") ? Integer.valueOf(v.get("ground_level").getAsInt()) : null;
            Integer resolvedOrientation = v.has("building_orientation") ? v.get("building_orientation").getAsInt() : planSetOrientation.intValue();
            if (resolvedOrientation != null && isFreshLoad) {
                be.setOrientation(resolvedOrientation & 3);
            }
            if ((levels = v.getAsJsonArray("levels")) == null) continue;
            for (JsonElement le : levels) {
                JsonObject l = le.getAsJsonObject();
                if (l.get("level").getAsInt() != upgradeLevel) continue;
                if (l.has("footprint")) {
                    JsonObject fp = l.getAsJsonObject("footprint");
                    be.setWidth(fp.get("width").getAsInt());
                    be.setHeight(fp.get("height").getAsInt());
                    be.setLength(fp.get("depth").getAsInt());
                }
                if (l.has("ground_level")) {
                    be.setStartingLevel(l.get("ground_level").getAsInt());
                } else if (variantGroundLevel != null) {
                    be.setStartingLevel(variantGroundLevel);
                } else {
                    be.setStartingLevel(0);
                }
                return;
            }
        }
    }

    public record ImportAabb(int minX, int minZ, int maxX, int maxZ) {
        public int xSpan() {
            return this.maxX - this.minX + 1;
        }

        public int zSpan() {
            return this.maxZ - this.minZ + 1;
        }
    }

    @FunctionalInterface
    private static interface LevelImporter {
        public void importLevel(int var1, ImportTableBlockEntity var2);
    }

    record BaseLayer(String buildingId, String variant, int level) {
    }

    record ResolvedPreviousLevel(StructureTemplate template, int groundLevel) {
    }

    public record SubRef(String subId, String parentVariant) {
    }
}

