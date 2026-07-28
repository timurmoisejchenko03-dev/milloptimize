/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  net.minecraft.core.BlockPos
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.nbt.ListTag
 *  org.slf4j.Logger
 */
package org.millenaire.building;

import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import org.millenaire.building.NbtPaletteHelper;
import org.millenaire.building.SpecialPoint;
import org.slf4j.Logger;

public final class MockBlockExtractor {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String MOD_PREFIX = "millenaire:mock_";

    private MockBlockExtractor() {
    }

    public static List<SpecialPoint> extract(CompoundTag nbt) {
        ListTag paletteTag = NbtPaletteHelper.resolvePaletteTag(nbt);
        if (paletteTag == null) {
            return Collections.emptyList();
        }
        SpecialPointTemplate[] mockInfos = new SpecialPointTemplate[paletteTag.size()];
        boolean hasSpecialPoints = false;
        for (int i = 0; i < paletteTag.size(); ++i) {
            CompoundTag entry = paletteTag.getCompound(i);
            String name = entry.getString("Name");
            if (name.startsWith(MOD_PREFIX)) {
                CompoundTag props = entry.contains("Properties", 10) ? entry.getCompound("Properties") : new CompoundTag();
                SpecialPointTemplate info = MockBlockExtractor.parseEntry(name, props);
                if (info == null) continue;
                mockInfos[i] = info;
                hasSpecialPoints = true;
                continue;
            }
            if (!"minecraft:campfire".equals(name)) continue;
            mockInfos[i] = new SpecialPointTemplate("hearth", null, null);
            hasSpecialPoints = true;
        }
        if (!hasSpecialPoints) {
            return Collections.emptyList();
        }
        ListTag blocksTag = nbt.getList("blocks", 10);
        ArrayList<SpecialPoint> points = new ArrayList<SpecialPoint>();
        for (int i = 0; i < blocksTag.size(); ++i) {
            SpecialPointTemplate info;
            CompoundTag blockEntry = blocksTag.getCompound(i);
            int stateIndex = blockEntry.getInt("state");
            if (stateIndex < 0 || stateIndex >= mockInfos.length || (info = mockInfos[stateIndex]) == null) continue;
            ListTag posTag = blockEntry.getList("pos", 3);
            BlockPos pos = new BlockPos(posTag.getInt(0), posTag.getInt(1), posTag.getInt(2));
            points.add(new SpecialPoint(info.type, info.subtype, info.orientation, info.placement, pos));
        }
        LOGGER.debug("Extracted {} special points from NBT", (Object)points.size());
        return Collections.unmodifiableList(points);
    }

    private static SpecialPointTemplate parseEntry(String blockName, CompoundTag props) {
        String suffix;
        return switch (suffix = blockName.substring(MOD_PREFIX.length())) {
            case "marker" -> MockBlockExtractor.parseMarker(props.getString("type"));
            case "facing_marker" -> MockBlockExtractor.parseFacingMarker(props);
            case "soil" -> new SpecialPointTemplate("soil", props.getString("crop"), null);
            case "source" -> new SpecialPointTemplate("source", props.getString("material"), null);
            case "free" -> new SpecialPointTemplate("freeBlock", props.getString("material"), null);
            case "tree_spawn" -> new SpecialPointTemplate("treeSpawn", props.getString("tree"), null);
            case "animal_spawn" -> new SpecialPointTemplate("animalSpawn", props.getString("animal"), null);
            case "chest" -> {
                String chestType = props.getString("chest_type");
                if ("main".equals(chestType)) {
                    chestType = "locked";
                }
                yield new SpecialPointTemplate("chest", chestType, props.getString("facing"));
            }
            case "decor" -> new SpecialPointTemplate("wall_decoration", props.getString("decor_type"), null);
            case "banner_wall" -> {
                String facing = props.getString("facing");
                String subtype = props.contains("subtype", 8) ? props.getString("subtype") : "village";
                yield new SpecialPointTemplate("banner", subtype, facing, "wall_" + facing);
            }
            case "banner_standing" -> {
                String rotation = props.getString("rotation");
                String subtype = props.contains("subtype", 8) ? props.getString("subtype") : "village";
                yield new SpecialPointTemplate("banner", subtype, rotation, "standing_" + rotation);
            }
            default -> {
                LOGGER.warn("Unknown mock block: {}", (Object)blockName);
                yield null;
            }
        };
    }

    private static SpecialPointTemplate parseFacingMarker(CompoundTag props) {
        String typeStr = props.getString("type");
        String facing = props.getString("facing");
        boolean guess = "true".equals(props.getString("guess"));
        String orientation = guess ? "guess" : facing;
        return switch (typeStr) {
            case "furnace" -> new SpecialPointTemplate("furnace", null, orientation);
            case "sign_pos" -> new SpecialPointTemplate("signPos", null, orientation);
            default -> null;
        };
    }

    private static SpecialPointTemplate parseMarker(String typeStr) {
        return switch (typeStr) {
            case "sleeping_pos" -> new SpecialPointTemplate("sleepingPos", null, null);
            case "selling_pos" -> new SpecialPointTemplate("sellingPos", null, null);
            case "crafting_pos" -> new SpecialPointTemplate("craftingPos", null, null);
            case "defending_pos" -> new SpecialPointTemplate("defendingPos", null, null);
            case "shelter_pos" -> new SpecialPointTemplate("shelterPos", null, null);
            case "path_start_pos" -> new SpecialPointTemplate("pathStartPos", null, null);
            case "leisure_pos" -> new SpecialPointTemplate("leisurePos", null, null);
            case "stall" -> new SpecialPointTemplate("stall", null, null);
            case "fishing_spot" -> new SpecialPointTemplate("fishingSpot", null, null);
            case "preserve_ground" -> new SpecialPointTemplate("preserve_ground", "surface", null);
            case "preserve_ground_depth" -> new SpecialPointTemplate("preserve_ground", "depth", null);
            case "preserve_ground_allbuttrees" -> new SpecialPointTemplate("preserve_ground", "allbuttrees", null);
            case "preserve_ground_grass" -> new SpecialPointTemplate("preserve_ground", "grass", null);
            case "torch" -> new SpecialPointTemplate("torchGuess", null, "guess");
            case "healing_spot" -> new SpecialPointTemplate("healingSpot", null, null);
            case "brick_spot" -> new SpecialPointTemplate("brick_spot", null, null);
            case "silkworm_block" -> new SpecialPointTemplate("silkwormBlock", null, null);
            case "snail_soil_block" -> new SpecialPointTemplate("snailSoilBlock", null, null);
            case "fireplace" -> new SpecialPointTemplate("fireplace", null, null);
            default -> null;
        };
    }

    private record SpecialPointTemplate(String type, String subtype, String orientation, String placement) {
        SpecialPointTemplate(String type, String subtype, String orientation) {
            this(type, subtype, orientation, null);
        }
    }
}

