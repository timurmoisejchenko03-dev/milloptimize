/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.google.gson.GsonBuilder
 *  com.google.gson.JsonArray
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  com.google.gson.JsonParser
 *  com.mojang.logging.LogUtils
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.BlockPos$MutableBlockPos
 *  net.minecraft.core.HolderGetter
 *  net.minecraft.core.HolderLookup$Provider
 *  net.minecraft.core.Vec3i
 *  net.minecraft.core.registries.Registries
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.nbt.IntTag
 *  net.minecraft.nbt.ListTag
 *  net.minecraft.nbt.NbtIo
 *  net.minecraft.nbt.NbtUtils
 *  net.minecraft.nbt.Tag
 *  net.minecraft.network.chat.Component
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.item.DyeColor
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraft.world.level.block.Rotation
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings
 *  net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate
 *  org.slf4j.Logger
 */
package org.millenaire.building;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.millenaire.block.ImportTableBlockEntity;
import org.millenaire.block.mock.MockBlock;
import org.millenaire.building.BuildingImporter;
import org.millenaire.building.BuildingPlan;
import org.millenaire.building.BuildingPlanSet;
import org.millenaire.building.ClearMargins;
import org.millenaire.building.ImportTableIdentifierValidator;
import org.millenaire.building.NbtPaletteHelper;
import org.millenaire.building.TemplateLoader;
import org.millenaire.content.BuiltInCultures;
import org.millenaire.content.ContentDirectoryManager;
import org.millenaire.content.CustomContentIndex;
import org.millenaire.culture.ModCultures;
import org.millenaire.village.BrickColourTheme;
import org.slf4j.Logger;

public final class BuildingExporter {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String PRESERVE_GROUND_BLOCK = "millenaire:mock_marker[type=preserve_ground]";
    private static final Set<String> GROUND_BLOCKS = Set.of("minecraft:grass_block", "minecraft:dirt", "minecraft:sand");
    private static final String SNOW_BLOCK_PREFIX = "minecraft:snow";

    private BuildingExporter() {
    }

    public static void exportLevel(ServerLevel level, ImportTableBlockEntity be, ServerPlayer player) {
        if (BuildingExporter.rejectIfBeIdentifiersUnsafe(player, be)) {
            return;
        }
        Path exportDir = BuildingExporter.getExportDirFor(level, be);
        BuildingExporter.ensureDir(exportDir);
        String buildingId = be.getBuildingId();
        String variant = be.getVariant();
        int upgradeLevel = be.getUpgradeLevel();
        CaptureResult capture = BuildingExporter.captureLevelCore(level, be, exportDir, player);
        if (!capture.success()) {
            return;
        }
        int copiedLevels = capture.copiedLevels();
        String nbtFileName = buildingId + "_" + variant + "_" + upgradeLevel + ".nbt";
        BuildingExporter.refreshOverlay();
        BuildingExporter.maybePatchParentForCreatedSub(level, be, player);
        player.sendSystemMessage((Component)Component.literal((String)("\u00a7a[ImportTable] Exported " + buildingId + " " + variant + " level " + upgradeLevel + " to " + nbtFileName + " (height: " + capture.detectedHeight() + (String)(copiedLevels > 0 ? ", copied " + copiedLevels + " linked upgrade(s)" : "") + ")")));
        be.captureSavedState(BuildingExporter.computeBlocksHash(level, be));
        be.setDirty(false);
        BuildingImporter.reimportDependentLevels(level, be, player);
    }

    public static void exportNewLevel(ServerLevel level, ImportTableBlockEntity be, ServerPlayer player) {
        if (BuildingExporter.rejectIfBeIdentifiersUnsafe(player, be)) {
            return;
        }
        be.setUpgradeLevel(be.getUpgradeLevel() + 1);
        BuildingExporter.exportLevel(level, be, player);
    }

    private static int cloneLevel(Path exportDir, ImportTableBlockEntity be, ServerPlayer player) {
        String buildingId = be.getBuildingId();
        String variant = be.getVariant();
        int currentMax = be.getUpgradeLevel();
        String sourceFile = buildingId + "_" + variant + "_" + currentMax + ".nbt";
        int nextLevel = currentMax + 1;
        String destFile = buildingId + "_" + variant + "_" + nextLevel + ".nbt";
        Path sourcePath = exportDir.resolve(sourceFile);
        Path destPath = exportDir.resolve(destFile);
        if (!Files.exists(sourcePath, new LinkOption[0])) {
            player.sendSystemMessage((Component)Component.literal((String)("\u00a7c[ImportTable] Source NBT not found: " + sourceFile)));
            return -1;
        }
        try {
            Files.copy(sourcePath, destPath, StandardCopyOption.REPLACE_EXISTING);
        }
        catch (IOException e) {
            LOGGER.error("Failed to copy NBT for new level: {} -> {}", new Object[]{sourcePath, destPath, e});
            player.sendSystemMessage((Component)Component.literal((String)("\u00a7c[ImportTable] Failed to create new level: " + e.getMessage())));
            return -1;
        }
        be.setUpgradeLevel(nextLevel);
        BuildingExporter.updateBuildingJson(exportDir, be);
        return nextLevel;
    }

    public static void exportAdditionalLevels(ServerLevel level, ImportTableBlockEntity be, ServerPlayer player, int numberOfUpgrades) {
        int nextLevel;
        if (numberOfUpgrades <= 0) {
            return;
        }
        Path exportDir = BuildingExporter.getExportDirFor(level, be);
        BuildingExporter.ensureDir(exportDir);
        int created = 0;
        for (int i = 0; i < numberOfUpgrades && (nextLevel = BuildingExporter.cloneLevel(exportDir, be, player)) >= 0; ++i) {
            ++created;
        }
        if (created > 0) {
            BuildingExporter.refreshOverlay();
            player.sendSystemMessage((Component)Component.literal((String)("\u00a7a[ImportTable] Created " + created + " upgrade level(s) for " + be.getBuildingId() + " " + be.getVariant())));
        }
    }

    private static void postProcessNbt(CompoundTag nbt, ImportTableBlockEntity be) {
        if (!nbt.contains("palette", 9)) {
            return;
        }
        if (!nbt.contains("blocks", 9)) {
            return;
        }
        ListTag palette = nbt.getList("palette", 10);
        ListTag blocks = nbt.getList("blocks", 10);
        HashMap<Integer, String> paletteIndexToName = new HashMap<Integer, String>();
        for (int i = 0; i < palette.size(); ++i) {
            CompoundTag entry = palette.getCompound(i);
            paletteIndexToName.put(i, entry.getString("Name"));
        }
        if (!be.isExportSnow()) {
            BuildingExporter.removeSnowBlocks(blocks, paletteIndexToName);
        }
        if (be.isConvertToPreserveGround()) {
            BuildingExporter.convertToPreserveGround(palette, blocks, paletteIndexToName);
        }
    }

    private static boolean isSnowLayer(String name) {
        String baseName = name.contains("[") ? name.substring(0, name.indexOf(91)) : name;
        return SNOW_BLOCK_PREFIX.equals(baseName);
    }

    private static void removeSnowBlocks(ListTag blocks, Map<Integer, String> paletteIndexToName) {
        HashSet<Integer> snowIndices = new HashSet<Integer>();
        for (Map.Entry<Integer, String> entry : paletteIndexToName.entrySet()) {
            if (!BuildingExporter.isSnowLayer(entry.getValue())) continue;
            snowIndices.add(entry.getKey());
        }
        if (snowIndices.isEmpty()) {
            return;
        }
        for (int i = blocks.size() - 1; i >= 0; --i) {
            CompoundTag block = blocks.getCompound(i);
            int state = block.getInt("state");
            if (!snowIndices.contains(state)) continue;
            blocks.remove(i);
        }
    }

    private static void convertToPreserveGround(ListTag palette, ListTag blocks, Map<Integer, String> paletteIndexToName) {
        HashSet<Integer> groundIndices = new HashSet<Integer>();
        for (Map.Entry<Integer, String> entry : paletteIndexToName.entrySet()) {
            String name = entry.getValue();
            String baseName = name.contains("[") ? name.substring(0, name.indexOf(91)) : name;
            if (!GROUND_BLOCKS.contains(baseName)) continue;
            groundIndices.add(entry.getKey());
        }
        if (groundIndices.isEmpty()) {
            return;
        }
        int preserveIndex = -1;
        for (int i = 0; i < palette.size(); ++i) {
            CompoundTag props;
            CompoundTag entry = palette.getCompound(i);
            if (!"millenaire:mock_marker".equals(entry.getString("Name")) || !"preserve_ground".equals((props = entry.getCompound("Properties")).getString("type"))) continue;
            preserveIndex = i;
            break;
        }
        if (preserveIndex == -1) {
            CompoundTag preserveEntry = new CompoundTag();
            preserveEntry.putString("Name", "millenaire:mock_marker");
            CompoundTag properties = new CompoundTag();
            properties.putString("type", "preserve_ground");
            preserveEntry.put("Properties", (Tag)properties);
            palette.add((Object)preserveEntry);
            preserveIndex = palette.size() - 1;
        }
        for (int i = 0; i < blocks.size(); ++i) {
            ListTag pos;
            CompoundTag block = blocks.getCompound(i);
            int state = block.getInt("state");
            if (!groundIndices.contains(state) || (pos = block.getList("pos", 3)).size() < 3 || pos.getInt(1) != 0) continue;
            block.putInt("state", preserveIndex);
        }
    }

    private static void stripPreviousLevelBlocks(CompoundTag nbt, ImportTableBlockEntity be, ServerLevel level) {
        Object block;
        ListTag paletteTag;
        List<BuildingImporter.BaseLayer> baseLayers = BuildingImporter.computeBaseLayers(be);
        if (baseLayers.isEmpty()) {
            return;
        }
        String cultureKey = be.getCultureKey();
        boolean importMockBlocks = be.isImportMockBlocks();
        StructurePlaceSettings settings = new StructurePlaceSettings().setRotation(Rotation.NONE);
        int baseY = be.getBlockPos().getY();
        BlockPos scanOrigin = BuildingExporter.computeScanOrigin(be);
        int currentGroundLevel = be.getStartingLevel();
        HashMap<BlockPos, BlockState> consolidated = new HashMap<BlockPos, BlockState>();
        for (BuildingImporter.BaseLayer baseLayer : baseLayers) {
            Path path = BuildingExporter.findExportedJson(level, baseLayer.buildingId()).orElse(null);
            BuildingImporter.ResolvedPreviousLevel resolved = BuildingImporter.resolvePreviousLevel(level, cultureKey, baseLayer.buildingId(), baseLayer.variant(), baseLayer.level(), currentGroundLevel, path).orElse(null);
            if (resolved == null) continue;
            int yOffset = baseY + resolved.groundLevel() - scanOrigin.getY();
            BuildingExporter.extractBlocksFromTemplateRelative(resolved.template(), settings, yOffset, level, consolidated);
        }
        if (consolidated.isEmpty()) {
            return;
        }
        if (!importMockBlocks) {
            HashMap<BlockPos, BlockState> converted = new HashMap<BlockPos, BlockState>();
            for (Map.Entry entry : consolidated.entrySet()) {
                BlockState state = (BlockState)entry.getValue();
                Block block2 = state.getBlock();
                if (block2 instanceof MockBlock) {
                    MockBlock mockBlock = (MockBlock)block2;
                    BlockState replacement = mockBlock.getReplacementState(state);
                    converted.put((BlockPos)entry.getKey(), replacement != null ? replacement : Blocks.AIR.defaultBlockState());
                    continue;
                }
                converted.put((BlockPos)entry.getKey(), state);
            }
            consolidated = converted;
        }
        if ((paletteTag = NbtPaletteHelper.resolvePaletteTag(nbt)) == null) {
            return;
        }
        ListTag listTag = nbt.getList("blocks", 10);
        ArrayList<BlockState> arrayList = new ArrayList<BlockState>();
        for (int i = 0; i < paletteTag.size(); ++i) {
            CompoundTag stateTag = paletteTag.getCompound(i);
            BlockState state = NbtUtils.readBlockState((HolderGetter)level.holderLookup(Registries.BLOCK), (CompoundTag)stateTag);
            arrayList.add(state);
        }
        HashMap<BlockPos, Integer> exportedPositions = new HashMap<BlockPos, Integer>();
        for (int i = 0; i < listTag.size(); ++i) {
            CompoundTag block3 = listTag.getCompound(i);
            ListTag pos = block3.getList("pos", 3);
            BlockPos relPos = new BlockPos(pos.getInt(0), pos.getInt(1), pos.getInt(2));
            exportedPositions.put(relPos, i);
        }
        HashSet<Integer> toRemove = new HashSet<Integer>();
        for (int i = 0; i < listTag.size(); ++i) {
            block = listTag.getCompound(i);
            ListTag pos = block.getList("pos", 3);
            BlockPos relPos = new BlockPos(pos.getInt(0), pos.getInt(1), pos.getInt(2));
            int stateIndex = block.getInt("state");
            if (stateIndex < 0 || stateIndex >= arrayList.size()) continue;
            BlockState exportedState = (BlockState)arrayList.get(stateIndex);
            BlockState consolidatedState = (BlockState)consolidated.get((Object)relPos);
            if (consolidatedState == null || !consolidatedState.equals((Object)exportedState)) continue;
            toRemove.add(i);
        }
        ArrayList sortedRemove = new ArrayList(toRemove);
        sortedRemove.sort(Comparator.reverseOrder());
        block = sortedRemove.iterator();
        while (block.hasNext()) {
            int idx = (Integer)block.next();
            listTag.remove(idx);
        }
        int airIndex = -1;
        for (int i = 0; i < arrayList.size(); ++i) {
            if (!((BlockState)arrayList.get(i)).isAir()) continue;
            airIndex = i;
            break;
        }
        if (airIndex == -1) {
            CompoundTag airEntry = new CompoundTag();
            airEntry.putString("Name", "minecraft:air");
            paletteTag.add((Object)airEntry);
            airIndex = paletteTag.size() - 1;
        }
        for (Map.Entry entry : consolidated.entrySet()) {
            Integer existingIdx;
            BlockPos relPos = (BlockPos)entry.getKey();
            if (((BlockState)entry.getValue()).isAir() || (existingIdx = (Integer)exportedPositions.get((Object)relPos)) != null && toRemove.contains(existingIdx) || existingIdx != null) continue;
            CompoundTag airBlock = new CompoundTag();
            ListTag posTag = new ListTag();
            posTag.add((Object)IntTag.valueOf((int)relPos.getX()));
            posTag.add((Object)IntTag.valueOf((int)relPos.getY()));
            posTag.add((Object)IntTag.valueOf((int)relPos.getZ()));
            airBlock.put("pos", (Tag)posTag);
            airBlock.putInt("state", airIndex);
            listTag.add((Object)airBlock);
        }
    }

    static void compactPalette(CompoundTag nbt) {
        int i;
        if (!nbt.contains("palette", 9)) {
            return;
        }
        if (!nbt.contains("blocks", 9)) {
            return;
        }
        ListTag palette = nbt.getList("palette", 10);
        ListTag blocks = nbt.getList("blocks", 10);
        int[] refCount = new int[palette.size()];
        for (int i2 = 0; i2 < blocks.size(); ++i2) {
            CompoundTag block = blocks.getCompound(i2);
            int state = block.getInt("state");
            if (state < 0 || state >= refCount.length) continue;
            int n = state;
            refCount[n] = refCount[n] + 1;
        }
        int[] remap = new int[palette.size()];
        ListTag newPalette = new ListTag();
        for (i = 0; i < palette.size(); ++i) {
            if (refCount[i] > 0) {
                remap[i] = newPalette.size();
                newPalette.add((Object)palette.getCompound(i));
                continue;
            }
            remap[i] = -1;
        }
        if (newPalette.size() == palette.size()) {
            return;
        }
        for (i = 0; i < blocks.size(); ++i) {
            CompoundTag block = blocks.getCompound(i);
            int oldState = block.getInt("state");
            if (oldState < 0 || oldState >= remap.length || remap[oldState] < 0) continue;
            block.putInt("state", remap[oldState]);
        }
        nbt.put("palette", (Tag)newPalette);
    }

    private static void updateBuildingJson(Path exportDir, ImportTableBlockEntity be) {
        JsonObject root;
        String buildingId = be.getBuildingId();
        Path jsonPath = exportDir.resolve(buildingId + ".json");
        if (Files.exists(jsonPath, new LinkOption[0])) {
            try {
                String content = Files.readString(jsonPath);
                root = JsonParser.parseString((String)content).getAsJsonObject();
            }
            catch (Exception e) {
                LOGGER.warn("Failed to read existing JSON, creating new: {}", (Object)jsonPath, (Object)e);
                root = BuildingExporter.createNewBuildingJson(be);
            }
        } else {
            root = BuildingExporter.createNewBuildingJson(be);
        }
        BuildingExporter.updateLevelEntry(root, be);
        try {
            Files.writeString(jsonPath, (CharSequence)GSON.toJson((JsonElement)root), new OpenOption[0]);
        }
        catch (IOException e) {
            LOGGER.error("Failed to write building JSON: {}", (Object)jsonPath, (Object)e);
        }
    }

    private static int copyMissingCultureVariantLevelsToExports(ServerLevel level, Path exportDir, ImportTableBlockEntity be) {
        String cultureKey = be.getCultureKey();
        String buildingId = be.getBuildingId();
        String currentVariant = be.getVariant();
        int currentLevel = be.getUpgradeLevel();
        if (cultureKey == null || cultureKey.isEmpty()) {
            return 0;
        }
        if (ContentDirectoryManager.isInitialized() && CustomContentIndex.current().customCultureIds().contains(cultureKey)) {
            return 0;
        }
        BuildingPlanSet planSet = BuildingExporter.getPlanSet(cultureKey, buildingId);
        if (planSet == null) {
            return 0;
        }
        int copied = 0;
        for (Map.Entry<String, List<BuildingPlanSet.LevelDef>> variantEntry : planSet.variants().entrySet()) {
            String variantKey = variantEntry.getKey();
            List<BuildingPlanSet.LevelDef> levels = variantEntry.getValue();
            if (levels == null || levels.isEmpty()) continue;
            for (BuildingPlanSet.LevelDef levelDef : levels) {
                String nbtFileName;
                Path nbtPath;
                if (variantKey.equals(currentVariant) && levelDef.level() == currentLevel || Files.exists(nbtPath = exportDir.resolve(nbtFileName = buildingId + "_" + variantKey + "_" + levelDef.level() + ".nbt"), new LinkOption[0])) continue;
                StructureTemplate template = TemplateLoader.resolve(level, cultureKey, buildingId, variantKey, levelDef.level()).orElse(null);
                if (template == null) {
                    LOGGER.warn("Could not copy missing export template for {} {} level {}", new Object[]{buildingId, variantKey, levelDef.level()});
                    continue;
                }
                try {
                    NbtIo.writeCompressed((CompoundTag)template.save(new CompoundTag()), (Path)nbtPath);
                    ++copied;
                }
                catch (IOException e) {
                    LOGGER.warn("Failed to copy linked template to exports: {}", (Object)nbtPath, (Object)e);
                }
            }
        }
        return copied;
    }

    private static void ensureExportJsonHasCopiedVariantLevels(Path exportDir, ImportTableBlockEntity be) {
        JsonObject root;
        String buildingId;
        String cultureKey = be.getCultureKey();
        BuildingPlanSet planSet = BuildingExporter.getPlanSet(cultureKey, buildingId = be.getBuildingId());
        if (planSet == null) {
            return;
        }
        Path jsonPath = exportDir.resolve(buildingId + ".json");
        try {
            root = Files.exists(jsonPath, new LinkOption[0]) ? JsonParser.parseString((String)Files.readString(jsonPath)).getAsJsonObject() : BuildingExporter.createNewBuildingJson(be);
        }
        catch (Exception e) {
            LOGGER.warn("Failed to read export JSON for linked entries: {}", (Object)jsonPath, (Object)e);
            root = BuildingExporter.createNewBuildingJson(be);
        }
        BuildingExporter.applyPlanSetToJson(root, exportDir, buildingId, be.getVariant(), be.getUpgradeLevel(), planSet);
        try {
            Files.writeString(jsonPath, (CharSequence)GSON.toJson((JsonElement)root), new OpenOption[0]);
        }
        catch (IOException e) {
            LOGGER.error("Failed to write export JSON with linked entries: {}", (Object)jsonPath, (Object)e);
        }
    }

    static SubRefPatchResult patchParentJsonWithSubRef(JsonObject parentRoot, String parentVariant, int triggerLevel, String subId) {
        JsonArray variants;
        if (triggerLevel <= 0) {
            JsonArray arr;
            JsonArray jsonArray = arr = parentRoot.has("starting_sub_buildings") ? parentRoot.getAsJsonArray("starting_sub_buildings") : null;
            if (arr == null) {
                arr = new JsonArray();
                parentRoot.add("starting_sub_buildings", (JsonElement)arr);
            }
            if (BuildingExporter.jsonArrayContainsString(arr, subId)) {
                return SubRefPatchResult.ALREADY_PRESENT;
            }
            arr.add(subId);
            return SubRefPatchResult.ADDED;
        }
        JsonArray jsonArray = variants = parentRoot.has("variants") ? parentRoot.getAsJsonArray("variants") : null;
        if (variants == null) {
            return SubRefPatchResult.LEVEL_NOT_FOUND;
        }
        for (JsonElement ve : variants) {
            JsonArray levels;
            JsonObject v = ve.getAsJsonObject();
            if (!v.has("variant") || !parentVariant.equals(v.get("variant").getAsString())) continue;
            JsonArray jsonArray2 = levels = v.has("levels") ? v.getAsJsonArray("levels") : null;
            if (levels == null) {
                return SubRefPatchResult.LEVEL_NOT_FOUND;
            }
            for (JsonElement le : levels) {
                JsonArray subs;
                JsonObject l = le.getAsJsonObject();
                if (!l.has("level") || l.get("level").getAsInt() != triggerLevel) continue;
                JsonArray jsonArray3 = subs = l.has("sub_buildings") ? l.getAsJsonArray("sub_buildings") : null;
                if (subs == null) {
                    subs = new JsonArray();
                    l.add("sub_buildings", (JsonElement)subs);
                }
                if (BuildingExporter.jsonArrayContainsString(subs, subId)) {
                    return SubRefPatchResult.ALREADY_PRESENT;
                }
                subs.add(subId);
                return SubRefPatchResult.ADDED;
            }
            return SubRefPatchResult.LEVEL_NOT_FOUND;
        }
        return SubRefPatchResult.LEVEL_NOT_FOUND;
    }

    public static SubRefPatchResult addSubBuildingRef(ServerLevel level, String parentCulture, String parentId, String parentVariant, int triggerLevel, String subId) {
        JsonObject parentRoot = BuildingImporter.readBuildingJson(parentCulture, parentId).orElse(null);
        if (parentRoot == null) {
            LOGGER.warn("ImportTable: cannot patch parent '{}' for sub '{}' \u2014 parent JSON not found", (Object)parentId, (Object)subId);
            return SubRefPatchResult.PARENT_NOT_FOUND;
        }
        SubRefPatchResult result = BuildingExporter.patchParentJsonWithSubRef(parentRoot, parentVariant, triggerLevel, subId);
        if (result != SubRefPatchResult.ADDED) {
            return result;
        }
        String category = BuildingExporter.categoryFor(parentCulture, parentId);
        Path jsonPath = BuildingExporter.getExportDir(level, parentCulture, category).resolve(parentId + ".json");
        try {
            Files.createDirectories(jsonPath.getParent(), new FileAttribute[0]);
            Files.writeString(jsonPath, (CharSequence)GSON.toJson((JsonElement)parentRoot), new OpenOption[0]);
        }
        catch (IOException e) {
            LOGGER.error("Failed to write patched parent JSON: {}", (Object)jsonPath, (Object)e);
            return SubRefPatchResult.PARENT_NOT_FOUND;
        }
        BuildingExporter.refreshOverlay();
        return SubRefPatchResult.ADDED;
    }

    private static boolean jsonArrayContainsString(JsonArray arr, String value) {
        for (JsonElement e : arr) {
            if (!e.isJsonPrimitive() || !value.equals(e.getAsString())) continue;
            return true;
        }
        return false;
    }

    private static void maybePatchParentForCreatedSub(ServerLevel level, ImportTableBlockEntity be, ServerPlayer player) {
        if (!be.hasParentContext()) {
            return;
        }
        String parentCulture = be.getCultureKey();
        String parentId = be.getParentBuildingId();
        String parentVariant = be.getParentVariant();
        String subId = be.getBuildingId();
        if (parentCulture == null || parentCulture.isEmpty() || parentId == null || parentId.isEmpty()) {
            return;
        }
        if (BuildingImporter.resolveTriggerLevel(level, parentCulture, parentId, parentVariant, subId).isPresent()) {
            return;
        }
        int triggerLevel = be.getParentTriggerLevel();
        SubRefPatchResult result = BuildingExporter.addSubBuildingRef(level, parentCulture, parentId, parentVariant, triggerLevel, subId);
        switch (result.ordinal()) {
            case 0: {
                player.sendSystemMessage((Component)Component.literal((String)("\u00a7a[ImportTable] Wired sub '" + subId + "' into parent " + parentId + (String)(triggerLevel <= 0 ? "" : "/" + parentVariant) + " at trigger level " + triggerLevel)));
                break;
            }
            case 2: {
                player.sendSystemMessage((Component)Component.literal((String)("\u00a7c[ImportTable] Could not wire sub '" + subId + "' \u2014 parent " + parentId + "/" + parentVariant + " has no level " + triggerLevel)));
                break;
            }
            case 3: {
                player.sendSystemMessage((Component)Component.literal((String)("\u00a7c[ImportTable] Could not wire sub '" + subId + "' \u2014 parent '" + parentId + "' JSON not found")));
                break;
            }
        }
    }

    static void applyPlanSetToJson(JsonObject root, Path exportDir, String buildingId, String currentVariant, int currentLevel, BuildingPlanSet planSet) {
        JsonArray variants = root.has("variants") ? root.getAsJsonArray("variants") : new JsonArray();
        root.add("variants", (JsonElement)variants);
        for (Map.Entry<String, List<BuildingPlanSet.LevelDef>> variantEntry : planSet.variants().entrySet()) {
            String variantKey = variantEntry.getKey();
            List<BuildingPlanSet.LevelDef> levels = variantEntry.getValue();
            if (levels == null || levels.isEmpty()) continue;
            JsonObject variantObj = BuildingExporter.findOrCreateVariant(variants, variantKey);
            JsonArray levelArray = variantObj.has("levels") ? variantObj.getAsJsonArray("levels") : new JsonArray();
            variantObj.add("levels", (JsonElement)levelArray);
            for (BuildingPlanSet.LevelDef levelDef : levels) {
                Path nbtPath = exportDir.resolve(buildingId + "_" + variantKey + "_" + levelDef.level() + ".nbt");
                if (!Files.exists(nbtPath, new LinkOption[0])) continue;
                JsonObject levelObj = BuildingExporter.findOrCreateLevel(levelArray, levelDef.level());
                boolean isCurrent = variantKey.equals(currentVariant) && levelDef.level() == currentLevel;
                if (isCurrent) continue;
                JsonObject footprint = new JsonObject();
                footprint.addProperty("width", (Number)levelDef.width());
                footprint.addProperty("height", (Number)levelDef.height());
                footprint.addProperty("depth", (Number)levelDef.depth());
                levelObj.add("footprint", (JsonElement)footprint);
                levelObj.addProperty("ground_level", (Number)levelDef.groundLevel());
            }
        }
    }

    private static BuildingPlanSet getPlanSet(String cultureKey, String buildingId) {
        if (cultureKey == null || cultureKey.isEmpty() || buildingId == null || buildingId.isEmpty()) {
            return null;
        }
        ResourceLocation planSetId = ResourceLocation.tryParse((String)(cultureKey + "/" + buildingId));
        return planSetId != null ? ModCultures.getBuildingPlanSet(planSetId) : null;
    }

    private static JsonObject findOrCreateVariant(JsonArray variants, String variant) {
        for (JsonElement elem : variants) {
            JsonObject obj = elem.getAsJsonObject();
            if (!variant.equals(obj.get("variant").getAsString())) continue;
            return obj;
        }
        JsonObject obj = new JsonObject();
        obj.addProperty("variant", variant);
        obj.add("levels", (JsonElement)new JsonArray());
        variants.add((JsonElement)obj);
        return obj;
    }

    private static JsonObject findOrCreateLevel(JsonArray levels, int level) {
        for (JsonElement elem : levels) {
            JsonObject obj = elem.getAsJsonObject();
            if (!obj.has("level") || obj.get("level").getAsInt() != level) continue;
            return obj;
        }
        JsonObject obj = new JsonObject();
        obj.addProperty("level", (Number)level);
        levels.add((JsonElement)obj);
        return obj;
    }

    private static JsonObject createNewBuildingJson(ImportTableBlockEntity be) {
        BuildingPlanSet planSet;
        ResourceLocation planSetId;
        String cultureKey = be.getCultureKey();
        String buildingId = be.getBuildingId();
        if (!cultureKey.isEmpty() && !buildingId.isEmpty() && (planSetId = ResourceLocation.tryParse((String)(cultureKey + "/" + buildingId))) != null && (planSet = ModCultures.getBuildingPlanSet(planSetId)) != null) {
            return BuildingExporter.serializeFromExistingPlanSet(planSet);
        }
        JsonObject root = new JsonObject();
        root.addProperty("culture", cultureKey.isEmpty() ? "millenaire:custom" : cultureKey);
        root.addProperty("building_id", buildingId);
        boolean createdSub = be.hasParentContext() && !be.getParentBuildingId().isEmpty();
        root.addProperty("category", createdSub ? BuildingExporter.categoryFor(cultureKey, be.getParentBuildingId()) : "extra");
        root.addProperty("native_name", buildingId);
        root.addProperty("converter_skip", Boolean.valueOf(true));
        if (createdSub) {
            root.addProperty("is_sub_building", Boolean.valueOf(true));
            root.addProperty("max_count", (Number)0);
        }
        root.add("variants", (JsonElement)new JsonArray());
        return root;
    }

    static JsonObject serializeFromExistingPlanSet(BuildingPlanSet planSet) {
        BuildingPlan firstPlan;
        ClearMargins margins;
        JsonObject root = new JsonObject();
        root.addProperty("culture", planSet.culture().toString());
        root.addProperty("building_id", planSet.buildingId());
        if (!"houses".equals(planSet.category())) {
            root.addProperty("category", planSet.category());
        }
        root.addProperty("native_name", planSet.nativeName());
        root.addProperty("converter_skip", Boolean.valueOf(true));
        if (planSet.maxCount() != 1) {
            root.addProperty("max_count", (Number)planSet.maxCount());
        }
        if (planSet.isSubBuilding()) {
            root.addProperty("is_sub_building", Boolean.valueOf(true));
        }
        if (planSet.minDistance() != 0.0) {
            root.addProperty("min_distance", (Number)planSet.minDistance());
        }
        if (planSet.maxDistance() != 1.0) {
            root.addProperty("max_distance", (Number)planSet.maxDistance());
        }
        if (!planSet.maleResidents().isEmpty()) {
            root.add("male", (JsonElement)BuildingExporter.toStringArray(planSet.maleResidents()));
        }
        if (!planSet.femaleResidents().isEmpty()) {
            root.add("female", (JsonElement)BuildingExporter.toStringArray(planSet.femaleResidents()));
        }
        if (!planSet.visitors().isEmpty()) {
            root.add("visitors", (JsonElement)BuildingExporter.toStringArray(planSet.visitors()));
        }
        root.addProperty("priority_move_in", (Number)planSet.priorityMoveIn());
        if (!planSet.tags().isEmpty()) {
            root.add("tags", (JsonElement)BuildingExporter.toStringArray(planSet.tags()));
        }
        if (!"clear_and_flatten".equals(planSet.terrainPolicy())) {
            root.addProperty("terrain_policy", planSet.terrainPolicy());
        }
        if (!"bottom_up".equals(planSet.constructionOrder())) {
            root.addProperty("construction_order", planSet.constructionOrder());
        }
        if (planSet.icon() != null) {
            root.addProperty("icon", planSet.icon());
        }
        if ((margins = planSet.clearMargins()).lengthBefore() != 5 || margins.lengthAfter() != 5 || margins.widthBefore() != 5 || margins.widthAfter() != 5) {
            root.addProperty("area_to_clear_length_before", (Number)margins.lengthBefore());
            root.addProperty("area_to_clear_length_after", (Number)margins.lengthAfter());
            root.addProperty("area_to_clear_width_before", (Number)margins.widthBefore());
            root.addProperty("area_to_clear_width_after", (Number)margins.widthAfter());
        }
        if (planSet.price() > 0) {
            root.addProperty("price", (Number)planSet.price());
        }
        if (planSet.reputation() > 0) {
            root.addProperty("reputation", (Number)planSet.reputation());
        }
        if ((firstPlan = BuildingExporter.findFirstPlan(planSet)) != null && firstPlan.shopId() != null) {
            root.addProperty("shop", firstPlan.shopId());
        }
        Map<String, Integer> variantOrientations = BuildingExporter.collectVariantOrientations(planSet);
        if (!planSet.startingSubBuildings().isEmpty()) {
            root.add("starting_sub_buildings", (JsonElement)BuildingExporter.toStringArray(planSet.startingSubBuildings()));
        }
        if (!planSet.startingGoods().isEmpty()) {
            JsonArray sgArray = new JsonArray();
            for (BuildingPlanSet.StartingGood startingGood : planSet.startingGoods()) {
                JsonObject sgObj = new JsonObject();
                sgObj.addProperty("item", startingGood.item());
                if (startingGood.probability() != 1.0) {
                    sgObj.addProperty("probability", (Number)startingGood.probability());
                }
                if (startingGood.fixedNumber() != 0) {
                    sgObj.addProperty("fixed", (Number)startingGood.fixedNumber());
                }
                if (startingGood.randomNumber() != 0) {
                    sgObj.addProperty("random", (Number)startingGood.randomNumber());
                }
                sgArray.add((JsonElement)sgObj);
            }
            root.add("starting_goods", (JsonElement)sgArray);
        }
        if (!planSet.randomBrickColours().isEmpty()) {
            JsonObject rbcObj = new JsonObject();
            for (Map.Entry entry : planSet.randomBrickColours().entrySet()) {
                JsonArray colors = new JsonArray();
                for (BrickColourTheme.WeightedColor wc : (List)entry.getValue()) {
                    JsonObject wcObj = new JsonObject();
                    wcObj.addProperty("color", wc.color().getName());
                    wcObj.addProperty("weight", (Number)wc.weight());
                    colors.add((JsonElement)wcObj);
                }
                rbcObj.add(((DyeColor)entry.getKey()).getName(), (JsonElement)colors);
            }
            root.add("random_brick_colours", (JsonElement)rbcObj);
        }
        if (planSet.travelBookCategory() != null) {
            root.addProperty("travel_book_category", planSet.travelBookCategory());
        }
        if (!planSet.travelBookDisplay()) {
            root.addProperty("travel_book_display", Boolean.valueOf(false));
        }
        JsonArray variantsArray = new JsonArray();
        for (Map.Entry entry : planSet.variants().entrySet()) {
            JsonObject variantObj = new JsonObject();
            variantObj.addProperty("variant", (String)entry.getKey());
            Integer variantOrientation = variantOrientations.get(entry.getKey());
            variantObj.addProperty("building_orientation", (Number)(variantOrientation != null ? variantOrientation : 1));
            List levelDefs = (List)entry.getValue();
            boolean groundLevelConstant = true;
            boolean tagsConstant = true;
            int firstGround = levelDefs.isEmpty() ? 0 : ((BuildingPlanSet.LevelDef)levelDefs.get(0)).groundLevel();
            List<String> firstTags = BuildingExporter.planTagsFor(levelDefs.isEmpty() ? null : (BuildingPlanSet.LevelDef)levelDefs.get(0));
            for (int i = 1; i < levelDefs.size(); ++i) {
                if (((BuildingPlanSet.LevelDef)levelDefs.get(i)).groundLevel() != firstGround) {
                    groundLevelConstant = false;
                }
                if (BuildingExporter.planTagsFor((BuildingPlanSet.LevelDef)levelDefs.get(i)).equals(firstTags)) continue;
                tagsConstant = false;
            }
            if (groundLevelConstant && !levelDefs.isEmpty() && firstGround != 0) {
                variantObj.addProperty("ground_level", (Number)firstGround);
            }
            if (tagsConstant && !firstTags.isEmpty()) {
                variantObj.add("tags", (JsonElement)BuildingExporter.toStringArray(firstTags));
            }
            JsonArray levelsArray = new JsonArray();
            for (BuildingPlanSet.LevelDef ld : levelDefs) {
                levelsArray.add((JsonElement)BuildingExporter.serializeLevelDef(ld, groundLevelConstant, tagsConstant));
            }
            variantObj.add("levels", (JsonElement)levelsArray);
            variantsArray.add((JsonElement)variantObj);
        }
        root.add("variants", (JsonElement)variantsArray);
        return root;
    }

    private static List<String> planTagsFor(@Nullable BuildingPlanSet.LevelDef ld) {
        if (ld == null) {
            return List.of();
        }
        BuildingPlan plan = ModCultures.getBuildingPlan(ld.planId());
        return plan != null ? plan.tags() : List.of();
    }

    private static JsonObject serializeLevelDef(BuildingPlanSet.LevelDef ld, boolean groundLevelConstant, boolean tagsConstant) {
        List<String> planTags;
        JsonObject levelObj = new JsonObject();
        levelObj.addProperty("level", (Number)ld.level());
        JsonObject footprint = new JsonObject();
        footprint.addProperty("width", (Number)ld.width());
        footprint.addProperty("height", (Number)ld.height());
        footprint.addProperty("depth", (Number)ld.depth());
        levelObj.add("footprint", (JsonElement)footprint);
        if (!groundLevelConstant) {
            levelObj.addProperty("ground_level", (Number)ld.groundLevel());
        }
        if (!tagsConstant && !(planTags = BuildingExporter.planTagsFor(ld)).isEmpty()) {
            levelObj.add("tags", (JsonElement)BuildingExporter.toStringArray(planTags));
        }
        if (ld.priority() != 100) {
            levelObj.addProperty("priority", (Number)ld.priority());
        }
        if (ld.nativeName() != null) {
            levelObj.addProperty("native_name", ld.nativeName());
        }
        if (!ld.requiredTags().isEmpty()) {
            levelObj.add("required_tags", (JsonElement)BuildingExporter.toStringArray(ld.requiredTags()));
        }
        if (!ld.forbiddenTagsInVillage().isEmpty()) {
            levelObj.add("forbidden_tags_in_village", (JsonElement)BuildingExporter.toStringArray(ld.forbiddenTagsInVillage()));
        }
        if (!ld.requiredVillageTags().isEmpty()) {
            levelObj.add("required_village_tags", (JsonElement)BuildingExporter.toStringArray(ld.requiredVillageTags()));
        }
        if (!ld.parentTags().isEmpty()) {
            levelObj.add("parent_tags", (JsonElement)BuildingExporter.toStringArray(ld.parentTags()));
        }
        if (!ld.requiredParentTags().isEmpty()) {
            levelObj.add("required_parent_tags", (JsonElement)BuildingExporter.toStringArray(ld.requiredParentTags()));
        }
        if (!ld.clearTags().isEmpty()) {
            levelObj.add("clear_tags", (JsonElement)BuildingExporter.toStringArray(ld.clearTags()));
        }
        if (!ld.villageTags().isEmpty()) {
            levelObj.add("village_tags", (JsonElement)BuildingExporter.toStringArray(ld.villageTags()));
        }
        if (!ld.subBuildings().isEmpty()) {
            levelObj.add("sub_buildings", (JsonElement)BuildingExporter.toStringArray(ld.subBuildings()));
        }
        if (ld.signOrder() != null) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < ld.signOrder().length; ++i) {
                if (i > 0) {
                    sb.append(",");
                }
                sb.append(ld.signOrder()[i]);
            }
            levelObj.addProperty("signs", sb.toString());
        }
        if (ld.pathLevel() != 0) {
            levelObj.addProperty("path_level", (Number)ld.pathLevel());
        }
        if (ld.rebuildPath()) {
            levelObj.addProperty("rebuild_path", Boolean.valueOf(true));
        }
        if (ld.pathWidth() != 2) {
            levelObj.addProperty("path_width", (Number)ld.pathWidth());
        }
        if (!ld.abstractedProduction().isEmpty()) {
            JsonArray apArray = new JsonArray();
            for (Map.Entry<String, Integer> apEntry : ld.abstractedProduction().entrySet()) {
                apArray.add(apEntry.getKey() + "," + String.valueOf(apEntry.getValue()));
            }
            levelObj.add("abstracted_production", (JsonElement)apArray);
        }
        return levelObj;
    }

    @Nullable
    private static BuildingPlan findFirstPlan(BuildingPlanSet planSet) {
        for (List<BuildingPlanSet.LevelDef> levels : planSet.variants().values()) {
            if (levels.isEmpty()) continue;
            return ModCultures.getBuildingPlan(levels.get(0).planId());
        }
        return null;
    }

    static Map<String, Integer> collectVariantOrientations(BuildingPlanSet planSet) {
        LinkedHashMap<String, Integer> result = new LinkedHashMap<String, Integer>();
        for (Map.Entry<String, List<BuildingPlanSet.LevelDef>> entry : planSet.variants().entrySet()) {
            BuildingPlan plan;
            List<BuildingPlanSet.LevelDef> levels = entry.getValue();
            if (levels == null || levels.isEmpty() || (plan = ModCultures.getBuildingPlan(levels.get(0).planId())) == null) continue;
            result.put(entry.getKey(), plan.buildingOrientation());
        }
        return result;
    }

    private static JsonArray toStringArray(List<String> list) {
        JsonArray array = new JsonArray();
        for (String s : list) {
            array.add(s);
        }
        return array;
    }

    static void updateLevelEntry(JsonObject root, ImportTableBlockEntity be) {
        int variantGroundLevel;
        JsonArray variants = root.has("variants") ? root.getAsJsonArray("variants") : new JsonArray();
        JsonObject variantObj = null;
        for (JsonElement elem : variants) {
            JsonObject v = elem.getAsJsonObject();
            if (!be.getVariant().equals(v.get("variant").getAsString())) continue;
            variantObj = v;
            break;
        }
        if (variantObj == null) {
            variantObj = new JsonObject();
            variantObj.addProperty("variant", be.getVariant());
            variantObj.add("levels", (JsonElement)new JsonArray());
            variants.add((JsonElement)variantObj);
        }
        JsonArray levels = variantObj.has("levels") ? variantObj.getAsJsonArray("levels") : new JsonArray();
        JsonObject levelObj = null;
        for (JsonElement elem : levels) {
            JsonObject l = elem.getAsJsonObject();
            if (l.get("level").getAsInt() != be.getUpgradeLevel()) continue;
            levelObj = l;
            break;
        }
        if (levelObj == null) {
            levelObj = new JsonObject();
            levelObj.addProperty("level", (Number)be.getUpgradeLevel());
            levels.add((JsonElement)levelObj);
        }
        JsonObject footprint = new JsonObject();
        footprint.addProperty("width", (Number)be.getWidth());
        footprint.addProperty("height", (Number)be.getHeight());
        footprint.addProperty("depth", (Number)be.getLength());
        levelObj.add("footprint", (JsonElement)footprint);
        int levelGroundLevel = be.getStartingLevel();
        int n = variantGroundLevel = variantObj.has("ground_level") ? variantObj.get("ground_level").getAsInt() : 0;
        if (levelGroundLevel != variantGroundLevel) {
            levelObj.addProperty("ground_level", (Number)levelGroundLevel);
        } else {
            levelObj.remove("ground_level");
        }
        BuildingExporter.applyVariantOrientation(variantObj, be.getOrientation());
        variantObj.add("levels", (JsonElement)levels);
        root.add("variants", (JsonElement)variants);
    }

    static void applyVariantOrientation(JsonObject variantObj, int tableOrientation) {
        variantObj.addProperty("building_orientation", (Number)tableOrientation);
    }

    public static BlockPos computeScanOrigin(ImportTableBlockEntity be) {
        BlockPos tablePos = be.getBlockPos();
        int startingLevel = be.getStartingLevel();
        return tablePos.offset(1, startingLevel, 1);
    }

    static int detectBuildingHeight(ServerLevel level, BlockPos origin, ImportTableBlockEntity be, int upgradeLevel) {
        int scanX = be.getWidth();
        int scanZ = be.getLength();
        int minHeight = be.getHeight();
        int maxY = level.getMaxBuildHeight();
        int maxPossibleHeight = maxY - origin.getY();
        int height = 0;
        for (int dy = 0; dy < maxPossibleHeight; ++dy) {
            boolean blockFound = false;
            for (int dx = 0; dx < scanX && !blockFound; ++dx) {
                for (int dz = 0; dz < scanZ && !blockFound; ++dz) {
                    BlockPos pos = origin.offset(dx, dy, dz);
                    if (level.getBlockState(pos).isAir()) continue;
                    blockFound = true;
                }
            }
            if (blockFound) {
                height = dy + 1;
                continue;
            }
            if (dy >= minHeight) break;
        }
        return Math.max(height, 1);
    }

    public static Vec3i computeScanSize(ImportTableBlockEntity be) {
        return new Vec3i(be.getWidth(), be.getHeight(), be.getLength());
    }

    public static long computeBlocksHash(ServerLevel level, ImportTableBlockEntity be) {
        BlockPos origin = BuildingExporter.computeScanOrigin(be);
        Vec3i size = BuildingExporter.computeScanSize(be);
        long hash = 1469598103934665603L;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int sx = size.getX();
        int sy = size.getY();
        int sz = size.getZ();
        for (int dx = 0; dx < sx; ++dx) {
            for (int dy = 0; dy < sy; ++dy) {
                for (int dz = 0; dz < sz; ++dz) {
                    cursor.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    BlockState state = level.getBlockState((BlockPos)cursor);
                    BlockState hashState = state.getBlock() == Blocks.WATER || state.getBlock() == Blocks.LAVA ? (state.getFluidState().isSource() ? state.getBlock().defaultBlockState() : Blocks.AIR.defaultBlockState()) : state;
                    hash ^= (long)Block.getId((BlockState)hashState);
                    hash *= 1099511628211L;
                    BlockEntity blockEntity = level.getBlockEntity((BlockPos)cursor);
                    if (blockEntity == null) continue;
                    try {
                        CompoundTag tag = blockEntity.saveWithoutMetadata((HolderLookup.Provider)level.registryAccess());
                        hash ^= (long)tag.hashCode();
                        hash *= 1099511628211L;
                        continue;
                    }
                    catch (Exception exception) {
                        // empty catch block
                    }
                }
            }
        }
        return hash;
    }

    public static Rotation orientationToRotation(int orientation) {
        return switch (orientation) {
            case 1 -> Rotation.CLOCKWISE_90;
            case 2 -> Rotation.CLOCKWISE_180;
            case 3 -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
    }

    public static Path getExportDir(ServerLevel level, @Nullable String cultureKey, @Nullable String category) {
        return BuildingExporter.computeExportDir(level.getServer().getServerDirectory(), cultureKey, category);
    }

    static Path computeExportDir(Path gameDir, @Nullable String cultureKey, @Nullable String category) {
        String culturePath = BuildingExporter.culturePathComponent(cultureKey);
        String categoryDir = category == null || category.isEmpty() ? "lone" : category;
        return gameDir.resolve("millenaire-custom").resolve("exported").resolve("cultures").resolve(culturePath).resolve("buildings").resolve(categoryDir);
    }

    public static Path getLegacyExportDir(ServerLevel level) {
        return BuildingExporter.getLegacyExportDir(level.getServer().getServerDirectory());
    }

    public static Path getLegacyExportDir(Path gameDir) {
        return gameDir.resolve("millenaire-custom").resolve("exports");
    }

    public static Path getExportedRoot(ServerLevel level) {
        return BuildingExporter.getExportedRoot(level.getServer().getServerDirectory());
    }

    public static Path getExportedRoot(Path gameDir) {
        return gameDir.resolve("millenaire-custom").resolve("exported");
    }

    @Deprecated
    public static Path getExportDir(ServerLevel level) {
        return BuildingExporter.getLegacyExportDir(level);
    }

    static String culturePathComponent(@Nullable String cultureKey) {
        if (cultureKey == null || cultureKey.isEmpty()) {
            return "custom";
        }
        if (!ImportTableIdentifierValidator.isValidCultureKey(cultureKey)) {
            return "custom";
        }
        ResourceLocation rl = ResourceLocation.tryParse((String)cultureKey);
        if (rl != null && !rl.getPath().isEmpty()) {
            return rl.getPath();
        }
        return cultureKey;
    }

    static boolean rejectIfBeIdentifiersUnsafe(ServerPlayer player, ImportTableBlockEntity be) {
        if (!ImportTableIdentifierValidator.isValidIdentifier(be.getBuildingId()) || !ImportTableIdentifierValidator.isValidIdentifier(be.getVariant())) {
            LOGGER.warn("ImportTable export refused: BE has path-unsafe identifiers (buildingId='{}', variant='{}') \u2014 possible forged save", (Object)be.getBuildingId(), (Object)be.getVariant());
            player.sendSystemMessage((Component)Component.literal((String)"\u00a7c[ImportTable] Refusing to export: building id or variant is path-unsafe"));
            return true;
        }
        return false;
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    public static Optional<Path> findExportedNbt(ServerLevel level, String buildingId, String variant, int upgradeLevel) {
        if (!ImportTableIdentifierValidator.isValidIdentifier(buildingId)) return Optional.empty();
        if (!ImportTableIdentifierValidator.isValidIdentifier(variant)) {
            return Optional.empty();
        }
        String fileName = buildingId + "_" + variant + "_" + upgradeLevel + ".nbt";
        Path legacy = BuildingExporter.getLegacyExportDir(level).resolve(fileName);
        if (Files.exists(legacy, new LinkOption[0])) {
            return Optional.of(legacy);
        }
        Path exportedRoot = BuildingExporter.getExportedRoot(level).resolve("cultures");
        if (!Files.isDirectory(exportedRoot, new LinkOption[0])) {
            return Optional.empty();
        }
        try (Stream<Path> cultures = Files.list(exportedRoot);){
            List<Path> sortedCultures = cultures.filter(x$0 -> Files.isDirectory(x$0, new LinkOption[0])).sorted(Comparator.comparing(p -> p.getFileName().toString())).toList();
            Iterator<Path> iterator = sortedCultures.iterator();
            block14: while (iterator.hasNext()) {
                Path culturePath = iterator.next();
                Path buildingsDir = culturePath.resolve("buildings");
                if (!Files.isDirectory(buildingsDir, new LinkOption[0])) continue;
                Stream<Path> categories = Files.list(buildingsDir);
                try {
                    Path categoryPath;
                    Path candidate;
                    List<Path> sortedCategories = categories.filter(x$0 -> Files.isDirectory(x$0, new LinkOption[0])).sorted(Comparator.comparing(p -> p.getFileName().toString())).toList();
                    Iterator<Path> iterator2 = sortedCategories.iterator();
                    do {
                        if (!iterator2.hasNext()) continue block14;
                    } while (!Files.exists(candidate = (categoryPath = iterator2.next()).resolve(fileName), new LinkOption[0]));
                    Optional<Path> optional = Optional.of(candidate);
                    return optional;
                }
                finally {
                    if (categories == null) continue;
                    categories.close();
                }
            }
            return Optional.empty();
        }
        catch (IOException e) {
            LOGGER.warn("Failed to walk exported/cultures while looking for {}: {}", (Object)fileName, (Object)e.getMessage());
        }
        return Optional.empty();
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    public static Optional<Path> findExportedJson(ServerLevel level, String buildingId) {
        if (!ImportTableIdentifierValidator.isValidIdentifier(buildingId)) {
            return Optional.empty();
        }
        String fileName = buildingId + ".json";
        Path legacy = BuildingExporter.getLegacyExportDir(level).resolve(fileName);
        if (Files.exists(legacy, new LinkOption[0])) {
            return Optional.of(legacy);
        }
        Path exportedRoot = BuildingExporter.getExportedRoot(level).resolve("cultures");
        if (!Files.isDirectory(exportedRoot, new LinkOption[0])) {
            return Optional.empty();
        }
        try (Stream<Path> cultures = Files.list(exportedRoot);){
            List<Path> sortedCultures = cultures.filter(x$0 -> Files.isDirectory(x$0, new LinkOption[0])).sorted(Comparator.comparing(p -> p.getFileName().toString())).toList();
            Iterator<Path> iterator = sortedCultures.iterator();
            block14: while (iterator.hasNext()) {
                Path culturePath = iterator.next();
                Path buildingsDir = culturePath.resolve("buildings");
                if (!Files.isDirectory(buildingsDir, new LinkOption[0])) continue;
                Stream<Path> categories = Files.list(buildingsDir);
                try {
                    Path categoryPath;
                    Path candidate;
                    List<Path> sortedCategories = categories.filter(x$0 -> Files.isDirectory(x$0, new LinkOption[0])).sorted(Comparator.comparing(p -> p.getFileName().toString())).toList();
                    Iterator<Path> iterator2 = sortedCategories.iterator();
                    do {
                        if (!iterator2.hasNext()) continue block14;
                    } while (!Files.exists(candidate = (categoryPath = iterator2.next()).resolve(fileName), new LinkOption[0]));
                    Optional<Path> optional = Optional.of(candidate);
                    return optional;
                }
                finally {
                    if (categories == null) continue;
                    categories.close();
                }
            }
            return Optional.empty();
        }
        catch (IOException e) {
            LOGGER.warn("Failed to walk exported/cultures while looking for {}: {}", (Object)fileName, (Object)e.getMessage());
        }
        return Optional.empty();
    }

    public static String categoryFor(@Nullable String cultureKey, @Nullable String buildingId) {
        BuildingPlanSet planSet = BuildingExporter.getPlanSet(cultureKey, buildingId);
        if (planSet != null && planSet.category() != null && !planSet.category().isEmpty()) {
            return planSet.category();
        }
        return "lone";
    }

    public static Path getExportDirFor(ServerLevel level, ImportTableBlockEntity be) {
        Optional<Path> existing = BuildingExporter.findExportedDir(level.getServer().getServerDirectory(), be.getBuildingId());
        if (existing.isPresent()) {
            return existing.get();
        }
        String cultureKey = be.getCultureKey();
        String category = BuildingExporter.categoryFor(cultureKey, be.getBuildingId());
        if ("lone".equals(category) && be.hasParentContext() && !be.getParentBuildingId().isEmpty()) {
            category = BuildingExporter.categoryFor(cultureKey, be.getParentBuildingId());
        }
        return BuildingExporter.getExportDir(level, cultureKey, category);
    }

    public static List<String> listExportedBuildingIds(Path gameDir) {
        TreeSet<String> ids = new TreeSet<String>();
        BuildingExporter.collectJsonStems(BuildingExporter.getLegacyExportDir(gameDir), ids);
        Path culturesRoot = BuildingExporter.getExportedRoot(gameDir).resolve("cultures");
        if (Files.isDirectory(culturesRoot, new LinkOption[0])) {
            try (Stream<Path> cultures = Files.list(culturesRoot);){
                cultures.filter(x$0 -> Files.isDirectory(x$0, new LinkOption[0])).forEach(culturePath -> {
                    Path buildingsDir = culturePath.resolve("buildings");
                    if (!Files.isDirectory(buildingsDir, new LinkOption[0])) {
                        return;
                    }
                    try (Stream<Path> categories = Files.list(buildingsDir);){
                        categories.filter(x$0 -> Files.isDirectory(x$0, new LinkOption[0])).forEach(categoryPath -> BuildingExporter.collectJsonStems(categoryPath, ids));
                    }
                    catch (IOException e) {
                        LOGGER.warn("Failed to list {}: {}", (Object)buildingsDir, (Object)e.getMessage());
                    }
                });
            }
            catch (IOException e) {
                LOGGER.warn("Failed to walk exported/cultures: {}", (Object)e.getMessage());
            }
        }
        return new ArrayList<String>(ids);
    }

    private static void collectJsonStems(Path dir, Set<String> out) {
        if (!Files.isDirectory(dir, new LinkOption[0])) {
            return;
        }
        try (Stream<Path> files = Files.list(dir);){
            files.filter(x$0 -> Files.isRegularFile(x$0, new LinkOption[0])).map(p -> p.getFileName().toString()).filter(n -> n.endsWith(".json")).map(n -> n.substring(0, n.length() - 5)).forEach(out::add);
        }
        catch (IOException e) {
            LOGGER.warn("Failed to list {}: {}", (Object)dir, (Object)e.getMessage());
        }
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    public static Optional<Path> findExportedDir(Path gameDir, String buildingId) {
        String fileName = buildingId + ".json";
        Path legacy = BuildingExporter.getLegacyExportDir(gameDir);
        if (Files.isRegularFile(legacy.resolve(fileName), new LinkOption[0])) {
            return Optional.of(legacy);
        }
        Path culturesRoot = BuildingExporter.getExportedRoot(gameDir).resolve("cultures");
        if (!Files.isDirectory(culturesRoot, new LinkOption[0])) {
            return Optional.empty();
        }
        try (Stream<Path> cultures = Files.list(culturesRoot);){
            List<Path> sortedCultures = cultures.filter(x$0 -> Files.isDirectory(x$0, new LinkOption[0])).sorted(Comparator.comparing(p -> p.getFileName().toString())).toList();
            Iterator<Path> iterator = sortedCultures.iterator();
            block14: while (iterator.hasNext()) {
                Path culturePath = iterator.next();
                Path buildingsDir = culturePath.resolve("buildings");
                if (!Files.isDirectory(buildingsDir, new LinkOption[0])) continue;
                Stream<Path> categories = Files.list(buildingsDir);
                try {
                    Path categoryPath;
                    List<Path> sortedCategories = categories.filter(x$0 -> Files.isDirectory(x$0, new LinkOption[0])).sorted(Comparator.comparing(p -> p.getFileName().toString())).toList();
                    Iterator<Path> iterator2 = sortedCategories.iterator();
                    do {
                        if (!iterator2.hasNext()) continue block14;
                    } while (!Files.isRegularFile((categoryPath = iterator2.next()).resolve(fileName), new LinkOption[0]));
                    Optional<Path> optional = Optional.of(categoryPath);
                    return optional;
                }
                finally {
                    if (categories == null) continue;
                    categories.close();
                }
            }
            return Optional.empty();
        }
        catch (IOException e) {
            LOGGER.warn("Failed to walk exported/cultures looking for {}: {}", (Object)fileName, (Object)e.getMessage());
        }
        return Optional.empty();
    }

    public static List<String> listExportedVariants(Path gameDir, String buildingId) {
        Optional<Path> dir = BuildingExporter.findExportedDir(gameDir, buildingId);
        if (dir.isEmpty()) {
            return List.of();
        }
        String prefix = buildingId + "_";
        TreeSet variants = new TreeSet();
        try (Stream<Path> files = Files.list(dir.get());){
            files.filter(x$0 -> Files.isRegularFile(x$0, new LinkOption[0])).map(p -> p.getFileName().toString()).filter(n -> n.startsWith(prefix) && n.endsWith(".nbt")).forEach(name -> {
                String stripped = name.substring(prefix.length(), name.length() - 4);
                int us = stripped.lastIndexOf(95);
                if (us > 0) {
                    variants.add(stripped.substring(0, us));
                }
            });
        }
        catch (IOException e) {
            LOGGER.warn("Failed to list variants for {} in {}: {}", new Object[]{buildingId, dir.get(), e.getMessage()});
        }
        return new ArrayList<String>(variants);
    }

    public static List<Integer> listExportedLevels(Path gameDir, String buildingId, String variant) {
        Optional<Path> dir = BuildingExporter.findExportedDir(gameDir, buildingId);
        if (dir.isEmpty()) {
            return List.of();
        }
        ArrayList<Integer> levels = new ArrayList<Integer>();
        int lvl = 0;
        while (Files.isRegularFile(dir.get().resolve(buildingId + "_" + variant + "_" + lvl + ".nbt"), new LinkOption[0])) {
            levels.add(lvl);
            ++lvl;
        }
        return levels;
    }

    private static void ensureDir(Path dir) {
        try {
            Files.createDirectories(dir, new FileAttribute[0]);
        }
        catch (IOException e) {
            LOGGER.error("Failed to create export directory: {}", (Object)dir, (Object)e);
        }
    }

    static void refreshOverlay() {
        CustomContentIndex.rebuild(BuiltInCultures.IDS, true);
    }

    static CaptureResult captureLevelCore(ServerLevel level, ImportTableBlockEntity be, Path exportDir, @Nullable ServerPlayer player) {
        BlockPos origin = BuildingExporter.computeScanOrigin(be);
        int detectedHeight = BuildingExporter.detectBuildingHeight(level, origin, be, be.getUpgradeLevel());
        be.setHeight(detectedHeight);
        Vec3i size = BuildingExporter.computeScanSize(be);
        StructureTemplate template = new StructureTemplate();
        template.fillFromWorld((Level)level, origin, size, false, null);
        CompoundTag nbt = template.save(new CompoundTag());
        BuildingExporter.postProcessNbt(nbt, be);
        BuildingExporter.stripPreviousLevelBlocks(nbt, be, level);
        BuildingExporter.compactPalette(nbt);
        String nbtFileName = be.getBuildingId() + "_" + be.getVariant() + "_" + be.getUpgradeLevel() + ".nbt";
        Path nbtPath = exportDir.resolve(nbtFileName);
        try {
            NbtIo.writeCompressed((CompoundTag)nbt, (Path)nbtPath);
        }
        catch (IOException e) {
            LOGGER.error("Failed to write NBT export: {}", (Object)nbtPath, (Object)e);
            if (player != null) {
                player.sendSystemMessage((Component)Component.literal((String)("\u00a7c[ImportTable] Failed to export NBT: " + e.getMessage())));
            }
            return new CaptureResult(false, detectedHeight, 0);
        }
        BuildingExporter.updateBuildingJson(exportDir, be);
        int copiedLevels = BuildingExporter.copyMissingCultureVariantLevelsToExports(level, exportDir, be);
        BuildingExporter.ensureExportJsonHasCopiedVariantLevels(exportDir, be);
        return new CaptureResult(true, detectedHeight, copiedLevels);
    }

    static boolean captureAndWriteLevel(ServerLevel level, ImportTableBlockEntity be, Path exportDir, @Nullable ServerPlayer player) {
        if (!BuildingExporter.captureLevelCore(level, be, exportDir, player).success()) {
            return false;
        }
        be.captureSavedState(BuildingExporter.computeBlocksHash(level, be));
        be.setDirty(false);
        return true;
    }

    private static void extractBlocksFromTemplateRelative(StructureTemplate template, StructurePlaceSettings settings, int yOffset, ServerLevel level, Map<BlockPos, BlockState> combined) {
        CompoundTag nbt = template.save(new CompoundTag());
        ArrayList<BlockState> palette = new ArrayList<BlockState>();
        ListTag paletteTag = NbtPaletteHelper.resolvePaletteTag(nbt);
        if (paletteTag == null) {
            return;
        }
        for (int i = 0; i < paletteTag.size(); ++i) {
            CompoundTag stateTag = paletteTag.getCompound(i);
            BlockState state = NbtUtils.readBlockState((HolderGetter)level.holderLookup(Registries.BLOCK), (CompoundTag)stateTag);
            palette.add(state);
        }
        if (palette.isEmpty()) {
            return;
        }
        ListTag blocksTag = nbt.getList("blocks", 10);
        for (int i = 0; i < blocksTag.size(); ++i) {
            BlockState state;
            CompoundTag blockEntry = blocksTag.getCompound(i);
            ListTag posTag = blockEntry.getList("pos", 3);
            BlockPos templatePos = new BlockPos(posTag.getInt(0), posTag.getInt(1), posTag.getInt(2));
            int stateIndex = blockEntry.getInt("state");
            if (stateIndex < 0 || stateIndex >= palette.size() || (state = (BlockState)palette.get(stateIndex)).isAir()) continue;
            BlockPos rotatedPos = StructureTemplate.calculateRelativePosition((StructurePlaceSettings)settings, (BlockPos)templatePos);
            BlockState rotatedState = state.rotate(settings.getRotation());
            BlockPos relPos = rotatedPos.offset(0, yOffset, 0);
            combined.put(relPos, rotatedState);
        }
    }

    record CaptureResult(boolean success, int detectedHeight, int copiedLevels) {
    }

    public static final class SubRefPatchResult
    extends Enum<SubRefPatchResult> {
        public static final /* enum */ SubRefPatchResult ADDED = new SubRefPatchResult();
        public static final /* enum */ SubRefPatchResult ALREADY_PRESENT = new SubRefPatchResult();
        public static final /* enum */ SubRefPatchResult LEVEL_NOT_FOUND = new SubRefPatchResult();
        public static final /* enum */ SubRefPatchResult PARENT_NOT_FOUND = new SubRefPatchResult();
        private static final /* synthetic */ SubRefPatchResult[] $VALUES;

        public static SubRefPatchResult[] values() {
            return (SubRefPatchResult[])$VALUES.clone();
        }

        public static SubRefPatchResult valueOf(String name) {
            return Enum.valueOf(SubRefPatchResult.class, name);
        }

        private static /* synthetic */ SubRefPatchResult[] $values() {
            return new SubRefPatchResult[]{ADDED, ALREADY_PRESENT, LEVEL_NOT_FOUND, PARENT_NOT_FOUND};
        }

        static {
            $VALUES = SubRefPatchResult.$values();
        }
    }
}

