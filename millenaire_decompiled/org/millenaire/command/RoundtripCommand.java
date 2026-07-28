/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.google.gson.GsonBuilder
 *  com.google.gson.JsonArray
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  com.mojang.brigadier.arguments.ArgumentType
 *  com.mojang.brigadier.arguments.IntegerArgumentType
 *  com.mojang.brigadier.arguments.StringArgumentType
 *  com.mojang.brigadier.builder.LiteralArgumentBuilder
 *  com.mojang.brigadier.builder.RequiredArgumentBuilder
 *  com.mojang.brigadier.context.CommandContext
 *  com.mojang.brigadier.suggestion.SuggestionProvider
 *  com.mojang.brigadier.suggestion.SuggestionsBuilder
 *  com.mojang.logging.LogUtils
 *  net.minecraft.commands.CommandSourceStack
 *  net.minecraft.commands.Commands
 *  net.minecraft.commands.SharedSuggestionProvider
 *  net.minecraft.commands.arguments.ResourceLocationArgument
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Vec3i
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.nbt.ListTag
 *  net.minecraft.nbt.NbtIo
 *  net.minecraft.nbt.Tag
 *  net.minecraft.network.chat.Component
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraft.world.level.block.Rotation
 *  net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate
 *  org.slf4j.Logger
 */
package org.millenaire.command;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.millenaire.building.BuildingPlan;
import org.millenaire.building.BuildingPlanSet;
import org.millenaire.building.TemplateLoader;
import org.millenaire.culture.ModCultures;
import org.millenaire.world.BuildingPlacer;
import org.slf4j.Logger;

public final class RoundtripCommand {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path OUTPUT_DIR = Path.of("millenaire-export", "roundtrip");
    private static final Random RANDOM = new Random();
    private static final int PLACE_X = 500;
    private static final int PLACE_Z = 500;
    private static final int BASE_Y = 100;
    private static final SuggestionProvider<CommandSourceStack> PLAN_SET_SUGGESTIONS = (ctx, builder) -> SharedSuggestionProvider.suggest(ModCultures.getAllBuildingPlanSets().keySet().stream().map(ResourceLocation::getPath), (SuggestionsBuilder)builder);
    private static final SuggestionProvider<CommandSourceStack> VARIANT_SUGGESTIONS = (ctx, builder) -> {
        BuildingPlanSet planSet = ModCultures.getBuildingPlanSet(RoundtripCommand.resolvePlanSetId((CommandContext<CommandSourceStack>)ctx));
        if (planSet != null) {
            return SharedSuggestionProvider.suggest(planSet.variants().keySet().stream(), (SuggestionsBuilder)builder);
        }
        return builder.buildFuture();
    };
    private static final String AIR = "minecraft:air";

    private RoundtripCommand() {
    }

    public static void registerUnder(LiteralArgumentBuilder<CommandSourceStack> parent) {
        parent.then(Commands.literal((String)"roundtrip").then(((RequiredArgumentBuilder)Commands.argument((String)"plan_set", (ArgumentType)ResourceLocationArgument.id()).suggests(PLAN_SET_SUGGESTIONS).executes(ctx -> RoundtripCommand.execute((CommandContext<CommandSourceStack>)ctx, null, 0))).then(((RequiredArgumentBuilder)Commands.argument((String)"variant", (ArgumentType)StringArgumentType.string()).suggests(VARIANT_SUGGESTIONS).executes(ctx -> RoundtripCommand.execute((CommandContext<CommandSourceStack>)ctx, StringArgumentType.getString((CommandContext)ctx, (String)"variant"), 0))).then(Commands.argument((String)"level", (ArgumentType)IntegerArgumentType.integer((int)0)).executes(ctx -> RoundtripCommand.execute((CommandContext<CommandSourceStack>)ctx, StringArgumentType.getString((CommandContext)ctx, (String)"variant"), IntegerArgumentType.getInteger((CommandContext)ctx, (String)"level")))))));
    }

    private static int execute(CommandContext<CommandSourceStack> ctx, String variant, int level) {
        CompoundTag referenceNbt;
        CommandSourceStack source = (CommandSourceStack)ctx.getSource();
        ServerLevel serverLevel = source.getServer().getLevel(Level.OVERWORLD);
        if (serverLevel == null) {
            source.sendFailure((Component)Component.literal((String)"No Overworld available."));
            return 0;
        }
        ResourceLocation planSetId = RoundtripCommand.resolvePlanSetId(ctx);
        BuildingPlanSet planSet = ModCultures.getBuildingPlanSet(planSetId);
        if (planSet == null) {
            source.sendFailure((Component)Component.literal((String)("Unknown BuildingPlanSet: " + String.valueOf((Object)planSetId))));
            return 0;
        }
        if (variant == null) {
            variant = planSet.pickRandomVariant(RANDOM);
        }
        if (!planSet.variants().containsKey(variant)) {
            source.sendFailure((Component)Component.literal((String)("Unknown variant: " + variant)));
            return 0;
        }
        BuildingPlan[] plans = new BuildingPlan[level + 1];
        for (int i = 0; i <= level; ++i) {
            BuildingPlanSet.LevelDef def = planSet.getLevel(variant, i);
            if (def == null) {
                source.sendFailure((Component)Component.literal((String)("Level " + i + " does not exist.")));
                return 0;
            }
            plans[i] = ModCultures.getBuildingPlan(def.planId());
            if (plans[i] != null) continue;
            source.sendFailure((Component)Component.literal((String)("BuildingPlan not found: " + String.valueOf((Object)def.planId()))));
            return 0;
        }
        BuildingPlan level0Plan = plans[0];
        BuildingPlan targetPlan = plans[level];
        int maxWidth = 0;
        int maxHeight = 0;
        int maxDepth = 0;
        int minGroundLevel = 0;
        for (BuildingPlan p : plans) {
            maxWidth = Math.max(maxWidth, p.width());
            maxHeight = Math.max(maxHeight, p.height());
            maxDepth = Math.max(maxDepth, p.depth());
            minGroundLevel = Math.min(minGroundLevel, p.groundLevel());
        }
        int margin = 5;
        int foundationDepth = Math.abs(minGroundLevel) + 5;
        for (int x = 500 - margin; x < 500 + maxWidth + margin; ++x) {
            for (int z = 500 - margin; z < 500 + maxDepth + margin; ++z) {
                int y;
                for (y = 100 - foundationDepth; y < 100; ++y) {
                    serverLevel.setBlock(new BlockPos(x, y, z), Blocks.STONE.defaultBlockState(), 2);
                }
                serverLevel.setBlock(new BlockPos(x, 100, z), Blocks.GRASS_BLOCK.defaultBlockState(), 2);
                for (y = 101; y < 100 + maxHeight + 20; ++y) {
                    serverLevel.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }
        BlockPos origin = new BlockPos(500, 100 + level0Plan.groundLevel(), 500);
        if (!BuildingPlacer.placeInstantly(serverLevel, level0Plan, origin, Rotation.NONE, false)) {
            source.sendFailure((Component)Component.literal((String)"Failed to place level 0."));
            return 0;
        }
        for (int lvl = 1; lvl <= level; ++lvl) {
            BlockPos upgOrigin = new BlockPos(500, 100 + plans[lvl].groundLevel(), 500);
            BuildingPlacer.placeUpgradeInstantly(serverLevel, plans[lvl], upgOrigin, Rotation.NONE, false);
        }
        Vec3i captureSize = new Vec3i(maxWidth, maxHeight, maxDepth);
        BlockPos captureOrigin = new BlockPos(500, 100 + minGroundLevel, 500);
        StructureTemplate recaptured = new StructureTemplate();
        recaptured.fillFromWorld((Level)serverLevel, captureOrigin, captureSize, false, null);
        CompoundTag recapturedNbt = recaptured.save(new CompoundTag());
        if (level == 0) {
            templateOpt = TemplateLoader.load(level0Plan, serverLevel, TemplateLoader.cultureFsForImport(level0Plan.culture()));
            referenceNbt = templateOpt.map(t -> t.save(new CompoundTag())).orElse(new CompoundTag());
        } else {
            templateOpt = TemplateLoader.load(targetPlan, serverLevel, TemplateLoader.cultureFsForImport(targetPlan.culture()));
            referenceNbt = templateOpt.map(t -> t.save(new CompoundTag())).orElse(new CompoundTag());
        }
        JsonObject diff = RoundtripCommand.analyzeNbtDiff(referenceNbt, recapturedNbt, planSetId, variant, level);
        try {
            RoundtripCommand.ensureDir(OUTPUT_DIR);
            String baseName = planSetId.getPath().replace("/", "_") + "_" + variant + "_" + level;
            NbtIo.writeCompressed((CompoundTag)referenceNbt, (Path)OUTPUT_DIR.resolve(baseName + "_original.nbt"));
            NbtIo.writeCompressed((CompoundTag)recapturedNbt, (Path)OUTPUT_DIR.resolve(baseName + "_roundtrip.nbt"));
            Path diffPath = OUTPUT_DIR.resolve(baseName + "_diff.json");
            Files.writeString(diffPath, (CharSequence)GSON.toJson((JsonElement)diff), new OpenOption[0]);
            int paletteOriginal = diff.get("palette_original_count").getAsInt();
            int paletteRoundtrip = diff.get("palette_roundtrip_count").getAsInt();
            int blocksOriginal = diff.get("blocks_original_count").getAsInt();
            int blocksRoundtrip = diff.get("blocks_roundtrip_count").getAsInt();
            int addedProps = diff.getAsJsonObject("categories").getAsJsonArray("default_properties_added").size();
            int addedBlocks = diff.getAsJsonObject("categories").getAsJsonArray("blocks_added").size();
            int removedBlocks = diff.getAsJsonObject("categories").getAsJsonArray("blocks_removed").size();
            int changedBlocks = diff.getAsJsonObject("categories").getAsJsonArray("blocks_changed").size();
            String summary = String.format("Roundtrip %s %s L%d: palette %d\u2192%d, blocks %d\u2192%d, +props=%d, +blocks=%d, -blocks=%d, changed=%d \u2192 %s", planSetId.getPath(), variant, level, paletteOriginal, paletteRoundtrip, blocksOriginal, blocksRoundtrip, addedProps, addedBlocks, removedBlocks, changedBlocks, diffPath);
            source.sendSuccess(() -> Component.literal((String)summary), false);
            LOGGER.info(summary);
            return 1;
        }
        catch (IOException e) {
            source.sendFailure((Component)Component.literal((String)("IO error: " + e.getMessage())));
            LOGGER.error("Roundtrip IO error", (Throwable)e);
            return 0;
        }
    }

    private static JsonObject analyzeNbtDiff(CompoundTag original, CompoundTag roundtrip, ResourceLocation planSetId, String variant, int lvl) {
        JsonObject result = new JsonObject();
        result.addProperty("plan_set", planSetId.toString());
        result.addProperty("variant", variant);
        result.addProperty("level", (Number)lvl);
        ListTag origPalette = original.getList("palette", 10);
        ListTag rtPalette = roundtrip.getList("palette", 10);
        result.addProperty("palette_original_count", (Number)origPalette.size());
        result.addProperty("palette_roundtrip_count", (Number)rtPalette.size());
        Map<String, BlockEntry> origBlocks = RoundtripCommand.parseBlocks(original);
        Map<String, BlockEntry> rtBlocks = RoundtripCommand.parseBlocks(roundtrip);
        result.addProperty("blocks_original_count", (Number)origBlocks.size());
        result.addProperty("blocks_roundtrip_count", (Number)rtBlocks.size());
        JsonArray defaultPropsAdded = new JsonArray();
        JsonArray blocksAdded = new JsonArray();
        JsonArray blocksRemoved = new JsonArray();
        JsonArray blocksChanged = new JsonArray();
        JsonArray nbtAdded = new JsonArray();
        JsonArray nbtChanged = new JsonArray();
        for (Map.Entry<String, BlockEntry> entry : rtBlocks.entrySet()) {
            BlockEntry origBlock;
            String pos = entry.getKey();
            BlockEntry rtBlock = entry.getValue();
            if (RoundtripCommand.isAir(rtBlock.stateKey) && ((origBlock = origBlocks.get(pos)) == null || RoundtripCommand.isAir(origBlock.stateKey))) continue;
            origBlock = origBlocks.get(pos);
            if (origBlock == null) {
                if (RoundtripCommand.isAir(rtBlock.stateKey)) continue;
                JsonObject added = new JsonObject();
                added.addProperty("pos", pos);
                added.addProperty("state", rtBlock.stateKey);
                blocksAdded.add((JsonElement)added);
                continue;
            }
            if (!origBlock.stateKey.equals(rtBlock.stateKey)) {
                String rtBase;
                if (RoundtripCommand.isAir(origBlock.stateKey) && RoundtripCommand.isAir(rtBlock.stateKey)) continue;
                String origBase = RoundtripCommand.extractBlockName(origBlock.stateKey);
                if (origBase.equals(rtBase = RoundtripCommand.extractBlockName(rtBlock.stateKey))) {
                    Map.Entry<String, String> prop2;
                    Map<String, String> origProps = RoundtripCommand.extractProperties(origBlock.stateKey);
                    Map<String, String> rtProps = RoundtripCommand.extractProperties(rtBlock.stateKey);
                    LinkedHashMap<String, String> addedProperties = new LinkedHashMap<String, String>();
                    for (Map.Entry<String, String> prop2 : rtProps.entrySet()) {
                        if (origProps.containsKey(prop2.getKey())) continue;
                        addedProperties.put((String)prop2.getKey(), (String)prop2.getValue());
                    }
                    LinkedHashMap<String, Object> changedProperties = new LinkedHashMap<String, Object>();
                    prop2 = rtProps.entrySet().iterator();
                    while (prop2.hasNext()) {
                        Map.Entry prop3 = (Map.Entry)prop2.next();
                        String origVal = origProps.get(prop3.getKey());
                        if (origVal == null || origVal.equals(prop3.getValue())) continue;
                        changedProperties.put((String)prop3.getKey(), origVal + " \u2192 " + (String)prop3.getValue());
                    }
                    if (!addedProperties.isEmpty() && changedProperties.isEmpty()) {
                        JsonObject propDiff = new JsonObject();
                        propDiff.addProperty("pos", pos);
                        propDiff.addProperty("block", origBase);
                        propsObj = new JsonObject();
                        addedProperties.forEach((arg_0, arg_1) -> ((JsonObject)propsObj).addProperty(arg_0, arg_1));
                        propDiff.add("added_properties", (JsonElement)propsObj);
                        defaultPropsAdded.add((JsonElement)propDiff);
                    } else {
                        JsonObject change = new JsonObject();
                        change.addProperty("pos", pos);
                        change.addProperty("original", origBlock.stateKey);
                        change.addProperty("roundtrip", rtBlock.stateKey);
                        if (!addedProperties.isEmpty()) {
                            propsObj = new JsonObject();
                            addedProperties.forEach((arg_0, arg_1) -> ((JsonObject)propsObj).addProperty(arg_0, arg_1));
                            change.add("added_properties", (JsonElement)propsObj);
                        }
                        if (!changedProperties.isEmpty()) {
                            propsObj = new JsonObject();
                            changedProperties.forEach((arg_0, arg_1) -> ((JsonObject)propsObj).addProperty(arg_0, arg_1));
                            change.add("changed_properties", (JsonElement)propsObj);
                        }
                        blocksChanged.add((JsonElement)change);
                    }
                } else {
                    JsonObject change = new JsonObject();
                    change.addProperty("pos", pos);
                    change.addProperty("original", origBlock.stateKey);
                    change.addProperty("roundtrip", rtBlock.stateKey);
                    blocksChanged.add((JsonElement)change);
                }
            }
            if (rtBlock.nbt != null && origBlock.nbt == null) {
                JsonObject nbtAdd = new JsonObject();
                nbtAdd.addProperty("pos", pos);
                nbtAdd.addProperty("block", RoundtripCommand.extractBlockName(rtBlock.stateKey));
                nbtAdd.addProperty("nbt_keys", rtBlock.nbt.getAllKeys().toString());
                nbtAdded.add((JsonElement)nbtAdd);
                continue;
            }
            if (rtBlock.nbt == null || origBlock.nbt == null || rtBlock.nbt.equals((Object)origBlock.nbt)) continue;
            JsonObject nbtDiff = new JsonObject();
            nbtDiff.addProperty("pos", pos);
            nbtDiff.addProperty("block", RoundtripCommand.extractBlockName(rtBlock.stateKey));
            JsonObject keyDiffs = new JsonObject();
            for (String key : rtBlock.nbt.getAllKeys()) {
                Tag origTag = origBlock.nbt.get(key);
                Tag rtTag = rtBlock.nbt.get(key);
                if (origTag == null) {
                    keyDiffs.addProperty(key, "ADDED: " + String.valueOf((Object)rtTag));
                    continue;
                }
                if (origTag.equals((Object)rtTag)) continue;
                keyDiffs.addProperty(key, String.valueOf((Object)origTag) + " \u2192 " + String.valueOf((Object)rtTag));
            }
            for (String key : origBlock.nbt.getAllKeys()) {
                if (rtBlock.nbt.contains(key)) continue;
                keyDiffs.addProperty(key, "REMOVED");
            }
            nbtDiff.add("diffs", (JsonElement)keyDiffs);
            nbtChanged.add((JsonElement)nbtDiff);
        }
        for (Map.Entry<String, BlockEntry> entry : origBlocks.entrySet()) {
            if (rtBlocks.containsKey(entry.getKey()) || RoundtripCommand.isAir(entry.getValue().stateKey)) continue;
            JsonObject removed = new JsonObject();
            removed.addProperty("pos", entry.getKey());
            removed.addProperty("state", entry.getValue().stateKey);
            blocksRemoved.add((JsonElement)removed);
        }
        JsonObject categories = new JsonObject();
        categories.add("default_properties_added", (JsonElement)defaultPropsAdded);
        categories.add("blocks_added", (JsonElement)blocksAdded);
        categories.add("blocks_removed", (JsonElement)blocksRemoved);
        categories.add("blocks_changed", (JsonElement)blocksChanged);
        categories.add("nbt_added", (JsonElement)nbtAdded);
        categories.add("nbt_changed", (JsonElement)nbtChanged);
        result.add("categories", (JsonElement)categories);
        JsonObject propStats = new JsonObject();
        HashMap<Object, Integer> propCounts = new HashMap<Object, Integer>();
        for (int i = 0; i < defaultPropsAdded.size(); ++i) {
            JsonObject entry = defaultPropsAdded.get(i).getAsJsonObject();
            JsonObject props = entry.getAsJsonObject("added_properties");
            for (String key : props.keySet()) {
                String propKey = key + "=" + props.get(key).getAsString();
                propCounts.merge(propKey, 1, Integer::sum);
            }
        }
        propCounts.forEach((arg_0, arg_1) -> ((JsonObject)propStats).addProperty(arg_0, arg_1));
        result.add("default_property_stats", (JsonElement)propStats);
        return result;
    }

    private static boolean isAir(String stateKey) {
        return stateKey.equals(AIR) || stateKey.equals("minecraft:cave_air") || stateKey.equals("minecraft:void_air");
    }

    private static Map<String, BlockEntry> parseBlocks(CompoundTag nbt) {
        int i;
        LinkedHashMap<String, BlockEntry> result = new LinkedHashMap<String, BlockEntry>();
        ListTag palette = nbt.getList("palette", 10);
        ListTag blocks = nbt.getList("blocks", 10);
        String[] paletteKeys = new String[palette.size()];
        for (i = 0; i < palette.size(); ++i) {
            paletteKeys[i] = RoundtripCommand.blockStateToString(palette.getCompound(i));
        }
        for (i = 0; i < blocks.size(); ++i) {
            CompoundTag block = blocks.getCompound(i);
            int stateIdx = block.getInt("state");
            ListTag posList = block.getList("pos", 3);
            String posKey = posList.getInt(0) + "," + posList.getInt(1) + "," + posList.getInt(2);
            Object stateKey = stateIdx < paletteKeys.length ? paletteKeys[stateIdx] : "UNKNOWN_" + stateIdx;
            CompoundTag blockNbt = block.contains("nbt", 10) ? block.getCompound("nbt") : null;
            result.put(posKey, new BlockEntry((String)stateKey, blockNbt));
        }
        return result;
    }

    private static String blockStateToString(CompoundTag paletteEntry) {
        String name = paletteEntry.getString("Name");
        if (!paletteEntry.contains("Properties", 10)) {
            return name;
        }
        CompoundTag props = paletteEntry.getCompound("Properties");
        if (props.isEmpty()) {
            return name;
        }
        StringBuilder sb = new StringBuilder(name).append('[');
        boolean first = true;
        List sortedKeys = props.getAllKeys().stream().sorted().toList();
        for (String key : sortedKeys) {
            if (!first) {
                sb.append(',');
            }
            sb.append(key).append('=').append(props.getString(key));
            first = false;
        }
        sb.append(']');
        return sb.toString();
    }

    private static String extractBlockName(String stateKey) {
        int bracket = stateKey.indexOf(91);
        return bracket < 0 ? stateKey : stateKey.substring(0, bracket);
    }

    private static Map<String, String> extractProperties(String stateKey) {
        LinkedHashMap<String, String> result = new LinkedHashMap<String, String>();
        int start = stateKey.indexOf(91);
        int end = stateKey.lastIndexOf(93);
        if (start < 0 || end < 0) {
            return result;
        }
        String propsStr = stateKey.substring(start + 1, end);
        if (propsStr.isEmpty()) {
            return result;
        }
        for (String pair : propsStr.split(",")) {
            String[] kv = pair.split("=", 2);
            if (kv.length != 2) continue;
            result.put(kv[0], kv[1]);
        }
        return result;
    }

    private static ResourceLocation resolvePlanSetId(CommandContext<CommandSourceStack> ctx) {
        ResourceLocation raw = ResourceLocationArgument.getId(ctx, (String)"plan_set");
        return raw.getNamespace().equals("minecraft") ? ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)raw.getPath()) : raw;
    }

    private static void ensureDir(Path dir) throws IOException {
        if (!Files.exists(dir, new LinkOption[0])) {
            Files.createDirectories(dir, new FileAttribute[0]);
        }
    }

    private record BlockEntry(String stateKey, CompoundTag nbt) {
    }
}

