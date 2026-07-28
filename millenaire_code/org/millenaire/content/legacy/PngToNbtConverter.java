/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.SharedConstants
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.nbt.IntTag
 *  net.minecraft.nbt.ListTag
 *  net.minecraft.nbt.NbtIo
 *  net.minecraft.nbt.Tag
 */
package org.millenaire.content.legacy;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import javax.imageio.ImageIO;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import org.millenaire.content.legacy.ConversionMode;

public class PngToNbtConverter {
    private static final int DATA_VERSION = PngToNbtConverter.computeDataVersion();
    static final int DATA_VERSION_FALLBACK = 3955;
    private static final int WHITE = PngToNbtConverter.rgb(255, 255, 255);
    private static final int GREEN_PRESERVE = PngToNbtConverter.rgb(0, 200, 0);
    private static final int ALLBUTTREES = PngToNbtConverter.rgb(150, 255, 150);
    private static final int GRASS_SPECIAL = PngToNbtConverter.rgb(0, 128, 0);
    private final Map<Integer, BlockState> colorMap = new HashMap<Integer, BlockState>();
    private final Set<Integer> specialColors = new HashSet<Integer>();
    private final Map<Integer, SpecialPointInfo> specialPointMap = new HashMap<Integer, SpecialPointInfo>();
    private final ConversionMode mode;
    private final Set<String> unmappedSpecialPointSignatures = new LinkedHashSet<String>();
    private final Set<Integer> unmappedColourSet = new LinkedHashSet<Integer>();

    static int computeDataVersion() {
        try {
            return SharedConstants.getCurrentVersion().getDataVersion().getVersion();
        }
        catch (ExceptionInInitializerError | IllegalStateException | NoClassDefFoundError | NullPointerException e) {
            return 3955;
        }
    }

    public PngToNbtConverter(ConversionMode mode) {
        this.mode = mode;
    }

    @Deprecated
    public PngToNbtConverter() {
        this(ConversionMode.AUTO);
    }

    public Set<String> drainUnmappedSpecialPoints() {
        LinkedHashSet<String> out = new LinkedHashSet<String>(this.unmappedSpecialPointSignatures);
        this.unmappedSpecialPointSignatures.clear();
        return out;
    }

    public Set<Integer> drainUnmappedColours() {
        LinkedHashSet<Integer> copy = new LinkedHashSet<Integer>(this.unmappedColourSet);
        this.unmappedColourSet.clear();
        return copy;
    }

    public void clearPerCultureState() {
        this.unmappedColourSet.clear();
        this.unmappedSpecialPointSignatures.clear();
    }

    public void loadBlocklist(Path blocklistPath, Set<Integer> usedColors) throws IOException {
        List<BlocklistEntry> entries = this.parseBlocklist(blocklistPath);
        this.applyBlocklist(entries, usedColors);
    }

    public void loadBlocklist(InputStream blocklistStream) throws IOException {
        this.loadBlocklist(blocklistStream, null);
    }

    public void loadBlocklist(InputStream blocklistStream, Set<Integer> usedColors) throws IOException {
        List<BlocklistEntry> entries = this.parseBlocklist(blocklistStream);
        this.applyBlocklist(entries, usedColors);
    }

    private void applyBlocklist(List<BlocklistEntry> entries, Set<Integer> usedColors) {
        this.specialColors.add(WHITE);
        this.addSpecialPoint(GREEN_PRESERVE, "preserve_ground", "surface", null, null);
        this.addSpecialPoint(PngToNbtConverter.rgb(150, 255, 150), "preserve_ground", "allbuttrees", null, null);
        this.addSpecialPoint(PngToNbtConverter.rgb(178, 255, 177), "preserve_ground", "allbuttrees", null, null);
        this.addSpecialPoint(GRASS_SPECIAL, "preserve_ground", "grass", null, null);
        this.addSpecialPoint(PngToNbtConverter.rgb(255, 255, 0), "torchGuess", null, "guess", null);
        this.addSpecialPoint(PngToNbtConverter.rgb(0, 128, 255), "sleepingPos", null, null, null);
        this.addSpecialPoint(PngToNbtConverter.rgb(200, 0, 0), "sellingPos", null, null, null);
        this.addSpecialPoint(PngToNbtConverter.rgb(200, 125, 0), "craftingPos", null, null, null);
        this.addSpecialPoint(PngToNbtConverter.rgb(200, 150, 0), "defendingPos", null, null, null);
        this.addSpecialPoint(PngToNbtConverter.rgb(200, 175, 0), "shelterPos", null, null, null);
        this.addSpecialPoint(PngToNbtConverter.rgb(200, 250, 0), "pathStartPos", null, null, null);
        this.addSpecialPoint(PngToNbtConverter.rgb(50, 50, 250), "leisurePos", null, null, null);
        LinkedHashMap<Integer, BlocklistEntry> rgbToEntry = new LinkedHashMap<Integer, BlocklistEntry>();
        for (BlocklistEntry entry : entries) {
            rgbToEntry.put(entry.rgb, entry);
        }
        for (BlocklistEntry entry : entries) {
            SpecialPointInfo spInfo = this.mapBlocklistToSpecialPoint(entry);
            if (spInfo == null) continue;
            this.addSpecialPoint(entry.rgb, spInfo.type, spInfo.subtype, spInfo.orientation, spInfo.placement);
        }
        Set<Integer> colorsToProcess = usedColors != null ? usedColors : rgbToEntry.keySet();
        Iterator iterator = colorsToProcess.iterator();
        while (iterator.hasNext()) {
            SpecialPointInfo spInfo;
            BlocklistEntry entry;
            int rgb = (Integer)iterator.next();
            if (this.specialColors.contains(rgb) && !this.specialPointMap.containsKey(rgb) || this.colorMap.containsKey(rgb) || (entry = (BlocklistEntry)rgbToEntry.get(rgb)) == null || (spInfo = this.mapBlocklistToSpecialPoint(entry)) != null && this.specialPointToMockBlockState(spInfo) != null) continue;
            BlockState special = this.mapSpecialEntry(entry);
            if (special != null) {
                this.colorMap.put(rgb, special);
                continue;
            }
            BlockState state = this.mapTo121(entry);
            if (state == null) continue;
            this.colorMap.put(rgb, state);
        }
    }

    private void addSpecialPoint(int rgb, String type, String subtype, String orientation, String placement) {
        this.specialColors.add(rgb);
        this.specialPointMap.put(rgb, new SpecialPointInfo(type, subtype, orientation, placement));
    }

    private SpecialPointInfo mapBlocklistToSpecialPoint(BlocklistEntry entry) {
        String name = entry.name.toLowerCase();
        if (name.startsWith("mainchest")) {
            String orientation = this.extractLegacyOrientation(name, "mainchest");
            return new SpecialPointInfo("chest", "locked", orientation, null);
        }
        if (name.startsWith("lockedchest")) {
            String orientation = this.extractLegacyOrientation(name, "lockedchest");
            return new SpecialPointInfo("chest", "locked", orientation, null);
        }
        if (name.equals("soil")) {
            return new SpecialPointInfo("soil", "wheat", null, null);
        }
        if (name.equals("ricesoil")) {
            return new SpecialPointInfo("soil", "rice", null, null);
        }
        if (name.equals("turmericsoil")) {
            return new SpecialPointInfo("soil", "turmeric", null, null);
        }
        if (name.equals("sugarcanesoil")) {
            return new SpecialPointInfo("soil", "sugarcane", null, null);
        }
        if (name.equals("potatosoil")) {
            return new SpecialPointInfo("soil", "potato", null, null);
        }
        if (name.equals("netherwartsoil")) {
            return new SpecialPointInfo("soil", "netherwart", null, null);
        }
        if (name.equals("vinesoil")) {
            return new SpecialPointInfo("soil", "vine", null, null);
        }
        if (name.equals("maizesoil")) {
            return new SpecialPointInfo("soil", "maize", null, null);
        }
        if (name.equals("carrotsoil")) {
            return new SpecialPointInfo("soil", "carrot", null, null);
        }
        if (name.equals("flowersoil")) {
            return new SpecialPointInfo("soil", "flower", null, null);
        }
        if (name.equals("cottonsoil")) {
            return new SpecialPointInfo("soil", "cotton", null, null);
        }
        if (name.equals("oakspawn")) {
            return new SpecialPointInfo("treeSpawn", "oak", null, null);
        }
        if (name.equals("pinespawn")) {
            return new SpecialPointInfo("treeSpawn", "pine", null, null);
        }
        if (name.equals("birchspawn")) {
            return new SpecialPointInfo("treeSpawn", "birch", null, null);
        }
        if (name.equals("junglespawn")) {
            return new SpecialPointInfo("treeSpawn", "jungle", null, null);
        }
        if (name.equals("acaciaspawn")) {
            return new SpecialPointInfo("treeSpawn", "acacia", null, null);
        }
        if (name.equals("darkoakspawn")) {
            return new SpecialPointInfo("treeSpawn", "dark_oak", null, null);
        }
        if (name.equals("appletreespawn")) {
            return new SpecialPointInfo("treeSpawn", "apple", null, null);
        }
        if (name.equals("olivetreespawn")) {
            return new SpecialPointInfo("treeSpawn", "olive", null, null);
        }
        if (name.equals("pistachiotreespawn")) {
            return new SpecialPointInfo("treeSpawn", "pistachio", null, null);
        }
        if (name.equals("cowspawn")) {
            return new SpecialPointInfo("animalSpawn", "cow", null, null);
        }
        if (name.equals("pigspawn")) {
            return new SpecialPointInfo("animalSpawn", "pig", null, null);
        }
        if (name.equals("sheepspawn")) {
            return new SpecialPointInfo("animalSpawn", "sheep", null, null);
        }
        if (name.equals("chickenspawn")) {
            return new SpecialPointInfo("animalSpawn", "chicken", null, null);
        }
        if (name.equals("squidspawn")) {
            return new SpecialPointInfo("animalSpawn", "squid", null, null);
        }
        if (name.equals("wolfspawn")) {
            return new SpecialPointInfo("animalSpawn", "wolf", null, null);
        }
        if (name.equals("polarbearspawn")) {
            return new SpecialPointInfo("animalSpawn", "polar_bear", null, null);
        }
        if (name.equals("stonesource")) {
            return new SpecialPointInfo("source", "stone", null, null);
        }
        if (name.equals("sandsource")) {
            return new SpecialPointInfo("source", "sand", null, null);
        }
        if (name.equals("sandstonesource")) {
            return new SpecialPointInfo("source", "sandstone", null, null);
        }
        if (name.equals("claysource")) {
            return new SpecialPointInfo("source", "clay", null, null);
        }
        if (name.equals("gravelsource")) {
            return new SpecialPointInfo("source", "gravel", null, null);
        }
        if (name.equals("granitesource")) {
            return new SpecialPointInfo("source", "granite", null, null);
        }
        if (name.equals("dioritesource")) {
            return new SpecialPointInfo("source", "diorite", null, null);
        }
        if (name.equals("andesitesource")) {
            return new SpecialPointInfo("source", "andesite", null, null);
        }
        if (name.equals("snowsource")) {
            return new SpecialPointInfo("source", "snow", null, null);
        }
        if (name.equals("icesource")) {
            return new SpecialPointInfo("source", "ice", null, null);
        }
        if (name.equals("redsandstonesource")) {
            return new SpecialPointInfo("source", "red_sandstone", null, null);
        }
        if (name.equals("quartzsource")) {
            return new SpecialPointInfo("source", "quartz", null, null);
        }
        if (name.equals("freestone")) {
            return new SpecialPointInfo("freeBlock", "stone", null, null);
        }
        if (name.equals("freesand")) {
            return new SpecialPointInfo("freeBlock", "sand", null, null);
        }
        if (name.equals("freegravel")) {
            return new SpecialPointInfo("freeBlock", "gravel", null, null);
        }
        if (name.equals("freewool")) {
            return new SpecialPointInfo("freeBlock", "wool", null, null);
        }
        if (name.equals("freesandstone")) {
            return new SpecialPointInfo("freeBlock", "sandstone", null, null);
        }
        if (name.equals("freecobblestone")) {
            return new SpecialPointInfo("freeBlock", "cobblestone", null, null);
        }
        if (name.equals("freestonebrick")) {
            return new SpecialPointInfo("freeBlock", "stone_brick", null, null);
        }
        if (name.equals("freepaintedbrick")) {
            return new SpecialPointInfo("freeBlock", "painted_brick", null, null);
        }
        if (name.equals("freegrass_block")) {
            return new SpecialPointInfo("freeBlock", "grass_block", null, null);
        }
        if (name.startsWith("furnace")) {
            String orientation = this.extractVariant(entry.variant, "facing");
            if (orientation == null) {
                orientation = this.extractLegacyOrientation(name, "furnace");
            }
            return new SpecialPointInfo("furnace", null, orientation, null);
        }
        if (name.equals("stall")) {
            return new SpecialPointInfo("stall", null, null, null);
        }
        if (name.equals("fishingspot")) {
            return new SpecialPointInfo("fishingSpot", null, null, null);
        }
        if (name.equals("brickspot")) {
            return new SpecialPointInfo("brickSpot", null, null, null);
        }
        if (name.equals("cacaospot")) {
            return new SpecialPointInfo("cacaoSpot", null, null, null);
        }
        if (name.equals("silkwormblock")) {
            return new SpecialPointInfo("silkwormBlock", null, null, null);
        }
        if (name.equals("snailsoilblock")) {
            return new SpecialPointInfo("snailSoilBlock", null, null, null);
        }
        if (name.equals("healingspot")) {
            return new SpecialPointInfo("healingSpot", null, null, null);
        }
        if (name.equals("brewingstand")) {
            return new SpecialPointInfo("brewingStand", null, null, null);
        }
        if (name.equals("dispenserunknownpowder")) {
            return new SpecialPointInfo("dispenserUnknownPowder", null, null, null);
        }
        if (name.equals("plainsignguess")) {
            return new SpecialPointInfo("plainSign", null, null, null);
        }
        if (name.startsWith("signwall")) {
            String wallDir = this.extractLegacyOrientation(name, "signwall");
            String faceDir = "guess".equals(wallDir) ? "guess" : this.oppositeDirection(wallDir);
            return new SpecialPointInfo("signPos", null, faceDir, null);
        }
        if (name.startsWith("spawner")) {
            String mob = name.substring("spawner".length());
            if (mob.equals("cavespider")) {
                mob = "cave_spider";
            }
            return new SpecialPointInfo("spawner", mob, null, null);
        }
        if (name.equals("tapestry")) {
            return new SpecialPointInfo("decorative", "tapestry", null, null);
        }
        if (name.equals("indianstatue")) {
            return new SpecialPointInfo("decorative", "indian_statue", null, null);
        }
        if (name.equals("mayanstatue")) {
            return new SpecialPointInfo("decorative", "mayan_statue", null, null);
        }
        if (name.equals("byzantineiconsmall")) {
            return new SpecialPointInfo("decorative", "byzantine_icon_small", null, null);
        }
        if (name.equals("byzantineiconmedium")) {
            return new SpecialPointInfo("decorative", "byzantine_icon_medium", null, null);
        }
        if (name.equals("byzantineiconlarge")) {
            return new SpecialPointInfo("decorative", "byzantine_icon_large", null, null);
        }
        if (name.equals("hidehanging")) {
            return new SpecialPointInfo("decorative", "hide_hanging", null, null);
        }
        if (name.equals("wallcarpetsmall")) {
            return new SpecialPointInfo("decorative", "wall_carpet_small", null, null);
        }
        if (name.equals("wallcarpetmedium")) {
            return new SpecialPointInfo("decorative", "wall_carpet_medium", null, null);
        }
        if (name.equals("wallcarpetlarge")) {
            return new SpecialPointInfo("decorative", "wall_carpet_large", null, null);
        }
        if (name.startsWith("villagebanner")) {
            String placement = this.extractBannerPlacement(name, "villagebanner");
            return new SpecialPointInfo("banner", "village", null, placement);
        }
        if (name.startsWith("culturebanner")) {
            String placement = this.extractBannerPlacement(name, "culturebanner");
            return new SpecialPointInfo("banner", "culture", null, placement);
        }
        return null;
    }

    private String extractLegacyOrientation(String name, String prefix) {
        if (name.contains("top")) {
            return "west";
        }
        if (name.contains("bottom")) {
            return "east";
        }
        if (name.contains("left")) {
            return "south";
        }
        if (name.contains("right")) {
            return "north";
        }
        return "guess";
    }

    private String oppositeDirection(String dir) {
        return switch (dir) {
            case "north" -> "south";
            case "south" -> "north";
            case "east" -> "west";
            case "west" -> "east";
            default -> dir;
        };
    }

    private String extractBannerPlacement(String name, String prefix) {
        String suffix = name.substring(prefix.length());
        if (suffix.equals("wallnorth")) {
            return "wall_north";
        }
        if (suffix.equals("walleast")) {
            return "wall_east";
        }
        if (suffix.equals("wallsouth")) {
            return "wall_south";
        }
        if (suffix.equals("wallwest")) {
            return "wall_west";
        }
        if (suffix.startsWith("standing")) {
            String num = suffix.substring("standing".length());
            return "standing_" + num;
        }
        return null;
    }

    private List<BlocklistEntry> parseBlocklist(Path path) throws IOException {
        return this.parseBlocklistLines(Files.readAllLines(path));
    }

    private List<BlocklistEntry> parseBlocklist(InputStream stream) throws IOException {
        ArrayList<String> lines = new ArrayList<String>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));){
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        }
        return this.parseBlocklistLines(lines);
    }

    private List<BlocklistEntry> parseBlocklistLines(List<String> rawLines) {
        ArrayList<BlocklistEntry> entries = new ArrayList<BlocklistEntry>();
        for (String line : rawLines) {
            String[] colorParts;
            String[] parts;
            if ((line = line.trim()).isEmpty() || line.startsWith("//") || (parts = line.split(";", -1)).length < 5) continue;
            String name = parts[0].trim();
            String blockId = parts[1].trim();
            String variant = parts[2].trim();
            String setAfterStr = parts[3].trim();
            String colorStr = parts[4].trim();
            if (colorStr.isEmpty() || (colorParts = colorStr.split("/")).length != 3) continue;
            try {
                int r = Integer.parseInt(colorParts[0].trim());
                int g = Integer.parseInt(colorParts[1].trim());
                int b = Integer.parseInt(colorParts[2].trim());
                int color = (r << 16) + (g << 8) + b;
                boolean setAfter = "true".equalsIgnoreCase(setAfterStr) || setAfterStr.startsWith("true");
                entries.add(new BlocklistEntry(name, blockId, variant, setAfter, color));
            }
            catch (NumberFormatException numberFormatException) {}
        }
        return entries;
    }

    private BlockState mapSpecialEntry(BlocklistEntry entry) {
        String name = entry.name.toLowerCase();
        if (name.startsWith("lockedchest")) {
            HashMap<String, String> props = new HashMap<String, String>();
            if (name.contains("top")) {
                props.put("facing", "west");
            } else if (name.contains("bottom")) {
                props.put("facing", "east");
            } else if (name.contains("left")) {
                props.put("facing", "south");
            } else if (name.contains("right")) {
                props.put("facing", "north");
            }
            return new BlockState("minecraft:chest", props);
        }
        if (name.startsWith("mainchest")) {
            HashMap<String, String> props = new HashMap<String, String>();
            if (name.contains("top")) {
                props.put("facing", "west");
            } else if (name.contains("bottom")) {
                props.put("facing", "east");
            } else if (name.contains("left")) {
                props.put("facing", "south");
            } else if (name.contains("right")) {
                props.put("facing", "north");
            }
            return new BlockState("minecraft:chest", props);
        }
        if (name.equals("soil") || name.equals("potatosoil") || name.equals("carrotsoil") || name.equals("maizesoil") || name.equals("flowersoil") || name.equals("ricesoil") || name.equals("turmericsoil") || name.equals("sugarcanesoil") || name.equals("netherwartsoil") || name.equals("vinesoil") || name.equals("cottonsoil")) {
            return new BlockState("minecraft:farmland");
        }
        if (name.equals("freestone")) {
            return new BlockState("minecraft:stone");
        }
        if (name.equals("freesand")) {
            return new BlockState("minecraft:sand");
        }
        if (name.equals("freegravel")) {
            return new BlockState("minecraft:gravel");
        }
        if (name.equals("freewool")) {
            return new BlockState("minecraft:white_wool");
        }
        if (name.equals("freesandstone")) {
            return new BlockState("minecraft:sandstone");
        }
        if (name.equals("freecobblestone")) {
            return new BlockState("minecraft:cobblestone");
        }
        if (name.equals("freestonebrick")) {
            return new BlockState("minecraft:stone_bricks");
        }
        if (name.equals("freepaintedbrick")) {
            return new BlockState("minecraft:stone_bricks");
        }
        if (name.equals("furnaceguess")) {
            return new BlockState("minecraft:furnace");
        }
        return null;
    }

    private BlockState mapTo121(BlocklistEntry entry) {
        String id = entry.blockId;
        String variant = entry.variant;
        if (id.isEmpty() || id.equals("0")) {
            return null;
        }
        if ("minecraft:air".equals(id)) {
            return new BlockState("minecraft:air");
        }
        if (id.startsWith("millenaire:")) {
            return this.mapMillenaireBlock(id, variant);
        }
        return this.flattenVanillaBlock(id, variant);
    }

    private BlockState mapMillenaireBlock(String id, String variant) {
        return switch (id) {
            case "millenaire:earth_deco" -> new BlockState("millenaire:dirt_wall");
            case "millenaire:wood_deco" -> {
                if (variant.contains("2") || variant.contains("thatch")) {
                    yield new BlockState("millenaire:thatch");
                }
                if (variant.contains("1") || variant.contains("cross")) {
                    yield new BlockState("millenaire:timber_frame_cross");
                }
                yield new BlockState("millenaire:timber_frame_plain");
            }
            case "millenaire:stone_deco" -> {
                if (variant.equals("2")) {
                    yield new BlockState("millenaire:mayan_gold_block");
                }
                if (variant.contains("byzantine_mosaic_red")) {
                    yield new BlockState("millenaire:byzantine_mosaic_red");
                }
                if (variant.contains("byzantine_mosaic_blue")) {
                    yield new BlockState("millenaire:byzantine_mosaic_blue");
                }
                if (variant.contains("byzantine")) {
                    yield new BlockState("millenaire:byzantine_mosaic_red");
                }
                yield new BlockState("millenaire:mud_brick");
            }
            case "millenaire:panel" -> {
                Map<String, String> props = this.parseFacingProps(variant);
                yield new BlockState("minecraft:oak_wall_sign", props);
            }
            case "millenaire:stairs_timberframe" -> {
                Map<String, String> props = this.parseFacingProps(variant);
                yield new BlockState("millenaire:timber_frame_stairs", props);
            }
            case "millenaire:stairs_thatch" -> {
                Map<String, String> props = this.parseFacingProps(variant);
                yield new BlockState("millenaire:thatch_stairs", props);
            }
            case "millenaire:stairs_mudbrick" -> {
                Map<String, String> props = this.parseFacingProps(variant);
                yield new BlockState("millenaire:mud_brick_stairs", props);
            }
            case "millenaire:slab_wood_deco" -> {
                Map<String, String> props = this.parseSlabProps(variant);
                if (variant.contains("thatch")) {
                    yield new BlockState("millenaire:thatch_slab", props);
                }
                yield new BlockState("millenaire:timber_frame_slab", props);
            }
            case "millenaire:slab_stone_deco" -> {
                Map<String, String> props = this.parseSlabProps(variant);
                if (variant.contains("cookedbrick")) {
                    yield new BlockState("millenaire:painted_brick_white_slab", props);
                }
                yield new BlockState("millenaire:mud_brick_slab", props);
            }
            case "millenaire:wall_mud_brick" -> new BlockState("millenaire:mud_brick_wall");
            case "millenaire:extended_mud_brick" -> {
                if (variant.contains("mudbrick_seljuk_ornamented")) {
                    yield new BlockState("millenaire:mud_brick_seljuk_ornamented");
                }
                if (variant.contains("mudbrick_seljuk_decorated")) {
                    yield new BlockState("millenaire:mud_brick_seljuk_decorated");
                }
                if (variant.contains("mudbrick_smooth")) {
                    yield new BlockState("millenaire:mud_brick_smooth");
                }
                yield new BlockState("millenaire:mud_brick");
            }
            case "millenaire:wooden_bars" -> new BlockState("millenaire:wooden_bars");
            case "millenaire:wooden_bars_indian" -> new BlockState("millenaire:wooden_bars_indian");
            case "millenaire:wooden_bars_rosette" -> {
                Map<String, String> props = this.parseRosetteProps(variant);
                yield new BlockState("millenaire:wooden_bars_rosette", props);
            }
            case "millenaire:rosette" -> new BlockState("millenaire:rosette");
            case "millenaire:sandstone_carved" -> new BlockState("millenaire:sandstone_carved");
            case "millenaire:sandstone_red_carved" -> new BlockState("millenaire:red_sandstone_carved");
            case "millenaire:sandstone_ochre_carved" -> new BlockState("millenaire:ochre_sandstone_carved");
            case "millenaire:stairs_sandstone_carved" -> {
                Map<String, String> props = this.parseFacingProps(variant);
                yield new BlockState("millenaire:sandstone_carved_stairs", props);
            }
            case "millenaire:stairs_sandstone_red_carved" -> {
                Map<String, String> props = this.parseFacingProps(variant);
                yield new BlockState("millenaire:red_sandstone_carved_stairs", props);
            }
            case "millenaire:stairs_sandstone_ochre_carved" -> {
                Map<String, String> props = this.parseFacingProps(variant);
                yield new BlockState("millenaire:ochre_sandstone_carved_stairs", props);
            }
            case "millenaire:slab_sandstone_carved" -> {
                Map<String, String> props = this.parseSlabProps(variant);
                yield new BlockState("millenaire:sandstone_carved_slab", props);
            }
            case "millenaire:slab_sandstone_red_carved" -> {
                Map<String, String> props = this.parseSlabProps(variant);
                yield new BlockState("millenaire:red_sandstone_carved_slab", props);
            }
            case "millenaire:slab_sandstone_ochre_carved" -> {
                Map<String, String> props = this.parseSlabProps(variant);
                yield new BlockState("millenaire:ochre_sandstone_carved_slab", props);
            }
            case "millenaire:wall_sandstone_carved" -> new BlockState("millenaire:sandstone_carved_wall");
            case "millenaire:wall_sandstone_red_carved" -> new BlockState("millenaire:red_sandstone_carved_wall");
            case "millenaire:wall_sandstone_ochre_carved" -> new BlockState("millenaire:ochre_sandstone_carved_wall");
            case "millenaire:bed_charpoy" -> {
                Map<String, String> props = this.parseFacingProps(variant);
                props.put("part", "head");
                yield new BlockState("millenaire:charpoy", props);
            }
            case "millenaire:paper_wall" -> new BlockState("millenaire:paper_wall");
            case "millenaire:japanese_tiles" -> {
                HashMap<String, String> props = new HashMap<String, String>();
                if (variant.contains("axis=z")) {
                    props.put("axis", "z");
                } else if (variant.contains("axis=x")) {
                    props.put("axis", "x");
                }
                yield new BlockState("millenaire:japanese_tiles", props.isEmpty() ? null : props);
            }
            case "millenaire:japanese_stone_tiles" -> {
                HashMap<String, String> props = new HashMap<String, String>();
                if (variant.contains("axis=z")) {
                    props.put("axis", "z");
                } else if (variant.contains("axis=x")) {
                    props.put("axis", "x");
                }
                yield new BlockState("millenaire:japanese_stone_tiles", props.isEmpty() ? null : props);
            }
            case "millenaire:japanese_tiles_stairs" -> {
                Map<String, String> props = this.parseFacingProps(variant);
                yield new BlockState("millenaire:japanese_tiles_stairs", props);
            }
            case "millenaire:japanese_tiles_slab" -> {
                Map<String, String> props = this.parseSlabProps(variant);
                yield new BlockState("millenaire:japanese_tiles_slab", props);
            }
            case "millenaire:bed_futon" -> {
                Map<String, String> props = this.parseFacingProps(variant);
                props.put("part", "head");
                yield new BlockState("millenaire:futon", props);
            }
            case "millenaire:wooden_sliding_door" -> {
                Map<String, String> props = this.parseFacingProps(variant);
                yield new BlockState("millenaire:wooden_sliding_door", props);
            }
            case "millenaire:japanese_sliding_door" -> {
                Map<String, String> props = this.parseFacingProps(variant);
                yield new BlockState("millenaire:japanese_sliding_door", props);
            }
            case "millenaire:pathdirt" -> new BlockState("millenaire:path_dirt");
            case "millenaire:pathgravel" -> new BlockState("millenaire:path_gravel");
            case "millenaire:pathslabs" -> new BlockState("millenaire:path_slabs");
            case "millenaire:pathsandstone" -> new BlockState("millenaire:path_sandstone");
            case "millenaire:pathochretiles" -> new BlockState("millenaire:path_ochre_tiles");
            case "millenaire:pathgravelslabs" -> new BlockState("millenaire:path_gravel");
            case "millenaire:pathsnow" -> new BlockState("millenaire:path_snow");
            case "millenaire:pathdirt_slab" -> new BlockState("millenaire:path_dirt_slab", Map.of("type", "bottom"));
            case "millenaire:pathgravel_slab" -> new BlockState("millenaire:path_gravel_slab", Map.of("type", "bottom"));
            case "millenaire:pathslabs_slab" -> new BlockState("millenaire:path_slabs_slab", Map.of("type", "bottom"));
            case "millenaire:pathsandstone_slab" -> new BlockState("millenaire:path_sandstone");
            case "millenaire:pathochretiles_slab" -> new BlockState("millenaire:path_ochre_tiles");
            case "millenaire:pathgravelslabs_slab" -> new BlockState("millenaire:path_gravel");
            case "millenaire:pathsnow_slab" -> new BlockState("millenaire:path_snow_slab", Map.of("type", "bottom"));
            case "millenaire:stained_glass" -> {
                String color = this.extractVariant(variant, "variant");
                yield new BlockState("millenaire:stained_glass_" + (color != null ? color : "white"));
            }
            case "millenaire:bed_straw" -> {
                Map<String, String> props = this.parseFacingProps(variant);
                props.put("part", "head");
                yield new BlockState("millenaire:straw_bed", props);
            }
            case "millenaire:byzantine_tiles" -> {
                HashMap<String, String> props = new HashMap<String, String>();
                if (variant.contains("axis=z")) {
                    props.put("axis", "z");
                } else if (variant.contains("axis=x")) {
                    props.put("axis", "x");
                }
                yield new BlockState("millenaire:byzantine_tiles", props.isEmpty() ? null : props);
            }
            case "millenaire:byzantine_stone_tiles" -> {
                HashMap<String, String> props = new HashMap<String, String>();
                if (variant.contains("axis=z")) {
                    props.put("axis", "z");
                } else if (variant.contains("axis=x")) {
                    props.put("axis", "x");
                }
                yield new BlockState("millenaire:byzantine_stone_tiles", props.isEmpty() ? null : props);
            }
            case "millenaire:byzantine_sandstone_tiles" -> {
                HashMap<String, String> props = new HashMap<String, String>();
                if (variant.contains("axis=z")) {
                    props.put("axis", "z");
                } else if (variant.contains("axis=x")) {
                    props.put("axis", "x");
                }
                yield new BlockState("millenaire:byzantine_sandstone_tiles", props.isEmpty() ? null : props);
            }
            case "millenaire:byzantine_sandstone_ornament" -> new BlockState("millenaire:byzantine_sandstone_ornament");
            case "millenaire:byzantine_stone_ornament" -> new BlockState("millenaire:byzantine_stone_ornament");
            case "millenaire:stairs_byzantine_tiles" -> {
                Map<String, String> props = this.parseFacingProps(variant);
                yield new BlockState("millenaire:byzantine_tiles_stairs", props);
            }
            case "millenaire:byzantine_tiles_slab" -> {
                Map<String, String> props = this.parseSlabProps(variant);
                yield new BlockState("millenaire:byzantine_tiles_slab", props);
            }
            case "millenaire:leaves_olivetree" -> new BlockState("millenaire:olive_tree_leaves");
            case "millenaire:crop_rice" -> new BlockState("millenaire:crop_rice");
            case "millenaire:crop_turmeric" -> new BlockState("millenaire:crop_turmeric");
            case "millenaire:crop_cotton" -> new BlockState("millenaire:crop_cotton");
            case "millenaire:crop_maize" -> new BlockState("millenaire:crop_maize");
            case "millenaire:sod" -> {
                String sodVariant = this.extractVariant(variant, "variant");
                yield new BlockState("millenaire:sod_" + (sodVariant != null ? sodVariant : "spruce"));
            }
            case "millenaire:snowbrick" -> new BlockState("millenaire:snow_brick");
            case "millenaire:icebrick" -> new BlockState("millenaire:ice_brick");
            case "millenaire:snowwall" -> new BlockState("millenaire:snow_wall");
            case "millenaire:inuitcarving" -> new BlockState("millenaire:inuit_carving");
            case "millenaire:fire_pit" -> {
                String alignment = this.extractVariant(variant, "alignment");
                yield new BlockState("millenaire:fire_pit", Map.of("alignment", alignment != null ? alignment : "x", "lit", "false"));
            }
            default -> {
                if (id.startsWith("millenaire:painted_brick_decorated_")) {
                    String color = this.remapLegacyColor(id.substring("millenaire:painted_brick_decorated_".length()));
                    yield new BlockState("millenaire:decorated_brick_" + color);
                }
                if (id.startsWith("millenaire:painted_brick_")) {
                    String color = this.remapLegacyColor(id.substring("millenaire:painted_brick_".length()));
                    yield new BlockState("millenaire:painted_brick_" + color);
                }
                if (id.startsWith("millenaire:stairs_painted_brick_")) {
                    String color = this.remapLegacyColor(id.substring("millenaire:stairs_painted_brick_".length()));
                    Map<String, String> props = this.parseFacingProps(variant);
                    yield new BlockState("millenaire:painted_brick_" + color + "_stairs", props);
                }
                if (id.startsWith("millenaire:slab_painted_brick_")) {
                    String color = this.remapLegacyColor(id.substring("millenaire:slab_painted_brick_".length()));
                    Map<String, String> props = this.parseSlabProps(variant);
                    yield new BlockState("millenaire:painted_brick_" + color + "_slab", props);
                }
                if (id.startsWith("millenaire:wall_painted_brick_")) {
                    String color = this.remapLegacyColor(id.substring("millenaire:wall_painted_brick_".length()));
                    yield new BlockState("millenaire:painted_brick_" + color + "_wall");
                }
                System.out.println("  WARNING: unknown modded block \u2192 cobblestone: " + id);
                yield new BlockState("minecraft:cobblestone");
            }
        };
    }

    private String remapLegacyColor(String color) {
        return "silver".equals(color) ? "light_gray" : color;
    }

    BlockState flattenVanillaBlock(String id, String variant) {
        if ("minecraft:planks".equals(id)) {
            String woodType = this.extractVariant(variant, "variant");
            return new BlockState("minecraft:" + (woodType != null ? woodType : "oak") + "_planks");
        }
        if ("minecraft:stone".equals(id)) {
            String v = this.extractVariant(variant, "variant");
            if (v == null) {
                v = "stone";
            }
            return switch (v) {
                case "stone" -> new BlockState("minecraft:stone");
                case "granite" -> new BlockState("minecraft:granite");
                case "smooth_granite" -> new BlockState("minecraft:polished_granite");
                case "diorite" -> new BlockState("minecraft:diorite");
                case "smooth_diorite" -> new BlockState("minecraft:polished_diorite");
                case "andesite" -> new BlockState("minecraft:andesite");
                case "smooth_andesite" -> new BlockState("minecraft:polished_andesite");
                default -> new BlockState("minecraft:stone");
            };
        }
        if ("minecraft:dirt".equals(id)) {
            String v = this.extractVariant(variant, "variant");
            if ("coarse_dirt".equals(v)) {
                return new BlockState("minecraft:coarse_dirt");
            }
            if ("podzol".equals(v)) {
                return new BlockState("minecraft:podzol");
            }
            return new BlockState("minecraft:dirt");
        }
        if ("minecraft:cobblestone".equals(id)) {
            return new BlockState("minecraft:cobblestone");
        }
        if ("minecraft:mossy_cobblestone".equals(id)) {
            return new BlockState("minecraft:mossy_cobblestone");
        }
        if ("minecraft:stonebrick".equals(id)) {
            return switch (variant) {
                case "0" -> new BlockState("minecraft:stone_bricks");
                case "1" -> new BlockState("minecraft:mossy_stone_bricks");
                case "2" -> new BlockState("minecraft:cracked_stone_bricks");
                case "3" -> new BlockState("minecraft:chiseled_stone_bricks");
                default -> new BlockState("minecraft:stone_bricks");
            };
        }
        if ("minecraft:log".equals(id) || "minecraft:log2".equals(id)) {
            String woodVariant = this.extractVariant(variant, "variant");
            String axis = this.extractVariant(variant, "axis");
            if (woodVariant == null) {
                woodVariant = "oak";
            }
            HashMap<String, String> props = new HashMap<String, String>();
            if (axis != null) {
                props.put("axis", axis);
            }
            return new BlockState("minecraft:" + woodVariant + "_log", props);
        }
        if (id.endsWith("_stairs")) {
            Map<String, String> props = this.parseFacingProps(variant);
            String remapped = "minecraft:stone_stairs".equals(id) ? "minecraft:cobblestone_stairs" : id;
            return new BlockState(remapped, props);
        }
        if ("minecraft:wooden_slab".equals(id)) {
            boolean top;
            String[] woods = new String[]{"oak", "spruce", "birch", "jungle", "acacia", "dark_oak"};
            int meta = this.parseIntSafe(variant);
            int idx = meta % 8;
            boolean bl = top = meta >= 8;
            if (idx >= 0 && idx < woods.length) {
                HashMap<String, String> props = new HashMap<String, String>();
                props.put("type", top ? "top" : "bottom");
                return new BlockState("minecraft:" + woods[idx] + "_slab", props);
            }
            return new BlockState("minecraft:oak_slab");
        }
        if ("minecraft:stone_slab".equals(id) || "minecraft:stone_slab2".equals(id)) {
            return this.mapStoneSlab(id, variant);
        }
        if ("minecraft:double_stone_slab".equals(id)) {
            return new BlockState("minecraft:smooth_stone");
        }
        if ("minecraft:torch".equals(id)) {
            String facing = this.extractVariant(variant, "facing");
            if (facing == null || "up".equals(facing)) {
                return new BlockState("minecraft:torch");
            }
            HashMap<String, String> props = new HashMap<String, String>();
            props.put("facing", facing);
            return new BlockState("minecraft:wall_torch", props);
        }
        if ("minecraft:furnace".equals(id)) {
            Map<String, String> props = this.parseFacingProps(variant);
            return new BlockState("minecraft:furnace", props);
        }
        if ("minecraft:glass".equals(id)) {
            return new BlockState("minecraft:glass");
        }
        if ("minecraft:glass_pane".equals(id)) {
            return new BlockState("minecraft:glass_pane");
        }
        if ("minecraft:water".equals(id) || "minecraft:flowing_water".equals(id)) {
            return new BlockState("minecraft:water");
        }
        if ("minecraft:ladder".equals(id)) {
            Map<String, String> props = this.parseFacingProps(variant);
            return new BlockState("minecraft:ladder", props);
        }
        if (id.contains("_door") || "minecraft:wooden_door".equals(id)) {
            return this.mapDoor(id, variant);
        }
        if ("minecraft:trapdoor".equals(id)) {
            Map<String, String> props = this.parseFacingProps(variant);
            return new BlockState("minecraft:oak_trapdoor", props);
        }
        if ("minecraft:fence".equals(id)) {
            return new BlockState("minecraft:oak_fence");
        }
        if ("minecraft:spruce_fence".equals(id)) {
            return new BlockState("minecraft:spruce_fence");
        }
        if ("minecraft:birch_fence".equals(id)) {
            return new BlockState("minecraft:birch_fence");
        }
        if ("minecraft:acacia_fence".equals(id)) {
            return new BlockState("minecraft:acacia_fence");
        }
        if ("minecraft:jungle_fence".equals(id)) {
            return new BlockState("minecraft:jungle_fence");
        }
        if ("minecraft:dark_oak_fence".equals(id)) {
            return new BlockState("minecraft:dark_oak_fence");
        }
        if (id.contains("fence_gate")) {
            Map<String, String> props = this.parseFacingProps(variant);
            if (id.contains("spruce")) {
                return new BlockState("minecraft:spruce_fence_gate", props);
            }
            if (id.contains("birch")) {
                return new BlockState("minecraft:birch_fence_gate", props);
            }
            if (id.contains("acacia")) {
                return new BlockState("minecraft:acacia_fence_gate", props);
            }
            if (id.contains("jungle")) {
                return new BlockState("minecraft:jungle_fence_gate", props);
            }
            if (id.contains("dark_oak")) {
                return new BlockState("minecraft:dark_oak_fence_gate", props);
            }
            return new BlockState("minecraft:oak_fence_gate", props);
        }
        if ("minecraft:chest".equals(id)) {
            Map<String, String> props = this.parseFacingProps(variant);
            return new BlockState("minecraft:chest", props);
        }
        if ("minecraft:crafting_table".equals(id)) {
            return new BlockState("minecraft:crafting_table");
        }
        if ("minecraft:wool".equals(id)) {
            return this.mapColoredBlock(variant, "_wool", "minecraft:white_wool");
        }
        if ("minecraft:carpet".equals(id)) {
            return this.mapCarpet(variant);
        }
        if ("minecraft:hardened_clay".equals(id)) {
            return new BlockState("minecraft:terracotta");
        }
        if ("minecraft:stained_hardened_clay".equals(id)) {
            return this.mapColoredTerracotta(variant);
        }
        if ("minecraft:leaves".equals(id) || "minecraft:leaves2".equals(id)) {
            String v = this.extractVariant(variant, "variant");
            if (v == null) {
                v = "oak";
            }
            HashMap<String, String> props = new HashMap<String, String>();
            props.put("persistent", "true");
            return new BlockState("minecraft:" + v + "_leaves", props);
        }
        if ("minecraft:brick_block".equals(id)) {
            return new BlockState("minecraft:bricks");
        }
        if ("minecraft:sand".equals(id)) {
            return new BlockState("minecraft:sand");
        }
        if ("minecraft:gravel".equals(id)) {
            return new BlockState("minecraft:gravel");
        }
        if ("minecraft:sandstone".equals(id)) {
            return switch (variant) {
                case "1" -> new BlockState("minecraft:chiseled_sandstone");
                case "2" -> new BlockState("minecraft:smooth_sandstone");
                default -> new BlockState("minecraft:sandstone");
            };
        }
        if ("minecraft:red_sandstone".equals(id)) {
            return switch (variant) {
                case "1" -> new BlockState("minecraft:chiseled_red_sandstone");
                case "2" -> new BlockState("minecraft:smooth_red_sandstone");
                default -> new BlockState("minecraft:red_sandstone");
            };
        }
        if ("minecraft:purpur_slab".equals(id)) {
            int meta = this.parseIntSafe(variant);
            HashMap<String, String> props = new HashMap<String, String>();
            props.put("type", meta >= 8 ? "top" : "bottom");
            return new BlockState("minecraft:purpur_slab", props);
        }
        if ("minecraft:sponge".equals(id)) {
            return "1".equals(variant) ? new BlockState("minecraft:wet_sponge") : new BlockState("minecraft:sponge");
        }
        if ("minecraft:hay_block".equals(id)) {
            String axis = this.extractVariant(variant, "axis");
            HashMap<String, String> props = new HashMap<String, String>();
            if (axis != null) {
                props.put("axis", axis);
            }
            return new BlockState("minecraft:hay_block", props);
        }
        if ("minecraft:bookshelf".equals(id)) {
            return new BlockState("minecraft:bookshelf");
        }
        if ("minecraft:bed".equals(id)) {
            Map<String, String> props = this.parseFacingProps(variant);
            String part = this.extractVariant(variant, "part");
            if (part != null) {
                props.put("part", part);
            }
            return new BlockState("minecraft:white_bed", props);
        }
        if ("minecraft:iron_bars".equals(id)) {
            return new BlockState("minecraft:iron_bars");
        }
        if ("minecraft:standing_sign".equals(id)) {
            String rotation = this.extractVariant(variant, "rotation");
            HashMap<String, String> props = new HashMap<String, String>();
            if (rotation != null) {
                props.put("rotation", rotation);
            }
            return new BlockState("minecraft:oak_sign", props);
        }
        if ("minecraft:redstone_wire".equals(id)) {
            return new BlockState("minecraft:redstone_wire");
        }
        if ("minecraft:redstone_torch".equals(id)) {
            return new BlockState("minecraft:redstone_torch");
        }
        if ("minecraft:stone_pressure_plate".equals(id)) {
            return new BlockState("minecraft:stone_pressure_plate");
        }
        if ("minecraft:wooden_pressure_plate".equals(id)) {
            return new BlockState("minecraft:oak_pressure_plate");
        }
        if ("minecraft:stone_button".equals(id)) {
            return new BlockState("minecraft:stone_button");
        }
        if ("minecraft:wooden_button".equals(id)) {
            Map<String, String> props = this.parseFacingProps(variant);
            return new BlockState("minecraft:oak_button", props);
        }
        if ("minecraft:wall_sign".equals(id)) {
            Map<String, String> props = this.parseFacingProps(variant);
            return new BlockState("minecraft:oak_wall_sign", props);
        }
        if ("minecraft:brewing_stand".equals(id)) {
            return new BlockState("minecraft:brewing_stand");
        }
        if ("minecraft:grass_path".equals(id)) {
            return new BlockState("minecraft:dirt_path");
        }
        if ("minecraft:grass".equals(id)) {
            return new BlockState("minecraft:grass_block");
        }
        if ("minecraft:tallgrass".equals(id)) {
            return new BlockState("minecraft:short_grass");
        }
        if ("minecraft:vine".equals(id)) {
            return new BlockState("minecraft:vine");
        }
        if ("minecraft:snow_layer".equals(id)) {
            return new BlockState("minecraft:snow");
        }
        if ("minecraft:snow".equals(id)) {
            return new BlockState("minecraft:snow_block");
        }
        if ("minecraft:ice".equals(id)) {
            return new BlockState("minecraft:ice");
        }
        if ("minecraft:pumpkin".equals(id)) {
            return new BlockState("minecraft:pumpkin");
        }
        if ("minecraft:lit_pumpkin".equals(id)) {
            return new BlockState("minecraft:jack_o_lantern");
        }
        if ("minecraft:melon_block".equals(id)) {
            return new BlockState("minecraft:melon");
        }
        if ("minecraft:nether_brick".equals(id)) {
            return new BlockState("minecraft:nether_bricks");
        }
        if ("minecraft:nether_brick_fence".equals(id)) {
            return new BlockState("minecraft:nether_brick_fence");
        }
        if ("minecraft:quartz_block".equals(id)) {
            String v = this.extractVariant(variant, "variant");
            if ("chiseled".equals(v)) {
                return new BlockState("minecraft:chiseled_quartz_block");
            }
            if (v != null && v.startsWith("lines_")) {
                HashMap<String, String> props = new HashMap<String, String>();
                props.put("axis", v.substring(6));
                return new BlockState("minecraft:quartz_pillar", props);
            }
            return new BlockState("minecraft:quartz_block");
        }
        if ("minecraft:bone_block".equals(id)) {
            String axis = this.extractVariant(variant, "axis");
            HashMap<String, String> props = new HashMap<String, String>();
            if (axis != null) {
                props.put("axis", axis);
            }
            return new BlockState("minecraft:bone_block", props);
        }
        if ("minecraft:clay".equals(id)) {
            return new BlockState("minecraft:clay");
        }
        if ("minecraft:obsidian".equals(id)) {
            return new BlockState("minecraft:obsidian");
        }
        if ("minecraft:tnt".equals(id)) {
            return new BlockState("minecraft:tnt");
        }
        if ("minecraft:netherrack".equals(id)) {
            return new BlockState("minecraft:netherrack");
        }
        if ("minecraft:soul_sand".equals(id)) {
            return new BlockState("minecraft:soul_sand");
        }
        if ("minecraft:glowstone".equals(id)) {
            return new BlockState("minecraft:glowstone");
        }
        if ("minecraft:lapis_block".equals(id)) {
            return new BlockState("minecraft:lapis_block");
        }
        if ("minecraft:iron_block".equals(id)) {
            return new BlockState("minecraft:iron_block");
        }
        if ("minecraft:diamond_block".equals(id)) {
            return new BlockState("minecraft:diamond_block");
        }
        if ("minecraft:emerald_block".equals(id)) {
            return new BlockState("minecraft:emerald_block");
        }
        if ("minecraft:gold_block".equals(id)) {
            return new BlockState("minecraft:gold_block");
        }
        if ("minecraft:redstone_block".equals(id)) {
            return new BlockState("minecraft:redstone_block");
        }
        if ("minecraft:redstone_lamp".equals(id)) {
            return new BlockState("minecraft:redstone_lamp");
        }
        if ("minecraft:lit_redstone_lamp".equals(id)) {
            return new BlockState("minecraft:redstone_lamp");
        }
        if ("minecraft:jukebox".equals(id)) {
            return new BlockState("minecraft:jukebox");
        }
        if ("minecraft:noteblock".equals(id)) {
            return new BlockState("minecraft:note_block");
        }
        if ("minecraft:bedrock".equals(id)) {
            return new BlockState("minecraft:bedrock");
        }
        if ("minecraft:rail".equals(id)) {
            return new BlockState("minecraft:rail");
        }
        if ("minecraft:sea_lantern".equals(id)) {
            return new BlockState("minecraft:sea_lantern");
        }
        if ("minecraft:cobblestone_wall".equals(id)) {
            return new BlockState("minecraft:cobblestone_wall");
        }
        if ("minecraft:cauldron".equals(id)) {
            return new BlockState("minecraft:cauldron");
        }
        if ("minecraft:anvil".equals(id)) {
            Map<String, String> props = this.parseFacingProps(variant);
            return new BlockState("minecraft:anvil", props);
        }
        if ("minecraft:sapling".equals(id)) {
            String type = this.extractVariant(variant, "type");
            if (type == null) {
                type = "oak";
            }
            return new BlockState("minecraft:" + type + "_sapling");
        }
        if ("minecraft:yellow_flower".equals(id)) {
            return new BlockState("minecraft:dandelion");
        }
        if ("minecraft:red_flower".equals(id)) {
            int meta = this.parseIntSafe(variant);
            return switch (meta) {
                case 0 -> new BlockState("minecraft:poppy");
                case 1 -> new BlockState("minecraft:blue_orchid");
                case 2 -> new BlockState("minecraft:allium");
                case 3 -> new BlockState("minecraft:azure_bluet");
                case 4 -> new BlockState("minecraft:red_tulip");
                case 5 -> new BlockState("minecraft:orange_tulip");
                case 6 -> new BlockState("minecraft:white_tulip");
                case 7 -> new BlockState("minecraft:pink_tulip");
                case 8 -> new BlockState("minecraft:oxeye_daisy");
                default -> new BlockState("minecraft:poppy");
            };
        }
        if ("minecraft:flower_pot".equals(id)) {
            return new BlockState("minecraft:flower_pot");
        }
        if ("minecraft:farmland".equals(id)) {
            return new BlockState("minecraft:farmland");
        }
        if ("minecraft:wheat".equals(id)) {
            return new BlockState("minecraft:wheat");
        }
        if ("minecraft:nether_wart".equals(id)) {
            return new BlockState("minecraft:nether_wart");
        }
        if ("minecraft:waterlily".equals(id)) {
            return new BlockState("minecraft:lily_pad");
        }
        if ("minecraft:brown_mushroom_block".equals(id)) {
            return new BlockState("minecraft:brown_mushroom_block");
        }
        if ("minecraft:red_mushroom_block".equals(id)) {
            return new BlockState("minecraft:red_mushroom_block");
        }
        if ("minecraft:brown_mushroom".equals(id)) {
            return new BlockState("minecraft:brown_mushroom");
        }
        if ("minecraft:red_mushroom".equals(id)) {
            return new BlockState("minecraft:red_mushroom");
        }
        if ("minecraft:stained_glass_pane".equals(id)) {
            String[] colors = new String[]{"white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"};
            int meta = this.parseIntSafe(variant);
            if (meta >= 0 && meta < colors.length) {
                return new BlockState("minecraft:" + colors[meta] + "_stained_glass_pane");
            }
            return new BlockState("minecraft:glass_pane");
        }
        if ("minecraft:concrete".equals(id)) {
            String[] colors = new String[]{"white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"};
            int meta = this.parseIntSafe(variant);
            if (meta >= 0 && meta < colors.length) {
                return new BlockState("minecraft:" + colors[meta] + "_concrete");
            }
            return new BlockState("minecraft:white_concrete");
        }
        if (id.contains("glazed_terracotta")) {
            Map<String, String> props = this.parseFacingProps(variant);
            return new BlockState(id, props);
        }
        if ("minecraft:purpur_block".equals(id)) {
            return new BlockState("minecraft:purpur_block");
        }
        if ("minecraft:purpur_pillar".equals(id)) {
            String axis = this.extractVariant(variant, "axis");
            HashMap<String, String> props = new HashMap<String, String>();
            if (axis != null) {
                props.put("axis", axis);
            }
            return new BlockState("minecraft:purpur_pillar", props);
        }
        if ("minecraft:end_rod".equals(id)) {
            Map<String, String> props = this.parseFacingProps(variant);
            return new BlockState("minecraft:end_rod", props);
        }
        if ("minecraft:dispenser".equals(id)) {
            return new BlockState("minecraft:dispenser");
        }
        if ("minecraft:cake".equals(id)) {
            return new BlockState("minecraft:cake");
        }
        if ("minecraft:flowing_lava".equals(id) || "minecraft:lava".equals(id)) {
            return new BlockState("minecraft:lava");
        }
        if ("minecraft:portal".equals(id)) {
            return new BlockState("minecraft:nether_portal");
        }
        if ("minecraft:dead_bush".equals(id)) {
            return new BlockState("minecraft:dead_bush");
        }
        if ("minecraft:double_plant".equals(id)) {
            String plantVariant = this.extractVariant(variant, "variant");
            String half = this.extractVariant(variant, "half");
            String blockName = switch (plantVariant != null ? plantVariant : "") {
                case "double_rose" -> "minecraft:rose_bush";
                case "double_grass" -> "minecraft:tall_grass";
                case "double_fern" -> "minecraft:large_fern";
                case "paeonia" -> "minecraft:peony";
                case "sunflower" -> "minecraft:sunflower";
                case "syringa" -> "minecraft:lilac";
                default -> "minecraft:tall_grass";
            };
            HashMap<String, String> props = new HashMap<String, String>();
            props.put("half", "lower".equals(half) ? "lower" : "upper");
            return new BlockState(blockName, props);
        }
        if (id.startsWith("minecraft:") && (id.endsWith("_ore") || id.equals("minecraft:heavy_weighted_pressure_plate") || id.equals("minecraft:light_weighted_pressure_plate"))) {
            return new BlockState(id);
        }
        System.out.println("  [WARN] No explicit mapping for: " + id + ";" + variant + " \u2192 using directly");
        return new BlockState(id);
    }

    private void fixDoorPairs(ListTag blocksList, List<BlockState> palette, Map<BlockState, Integer> paletteIndex) {
        CompoundTag tag;
        int i;
        HashMap<Long, Integer> posIndex = new HashMap<Long, Integer>();
        for (i = 0; i < blocksList.size(); ++i) {
            tag = blocksList.getCompound(i);
            ListTag pos = tag.getList("pos", 3);
            long key = PngToNbtConverter.packPos(pos.getInt(0), pos.getInt(1), pos.getInt(2));
            posIndex.put(key, i);
        }
        for (i = 0; i < blocksList.size(); ++i) {
            String hinge;
            tag = blocksList.getCompound(i);
            int stateIdx = tag.getInt("state");
            BlockState state = palette.get(stateIdx);
            if (!PngToNbtConverter.isDoorBlock(state.name) || !"lower".equals(state.properties.get("half"))) continue;
            ListTag pos = tag.getList("pos", 3);
            int x = pos.getInt(0);
            int y = pos.getInt(1);
            int z = pos.getInt(2);
            String facing = state.properties.getOrDefault("facing", "north");
            long upperKey = PngToNbtConverter.packPos(x, y + 1, z);
            Integer upperIdx = (Integer)posIndex.get(upperKey);
            if (upperIdx != null) {
                CompoundTag upperTag = blocksList.getCompound(upperIdx.intValue());
                BlockState upperState = palette.get(upperTag.getInt("state"));
                if (PngToNbtConverter.isDoorBlock(upperState.name) && "upper".equals(upperState.properties.get("half"))) {
                    hinge = upperState.properties.getOrDefault("hinge", "left");
                    BlockState fixedUpper = new BlockState(upperState.name, Map.of("facing", facing, "half", "upper", "hinge", hinge, "open", "false"));
                    int fixedUpperIdx = paletteIndex.computeIfAbsent(fixedUpper, s -> {
                        int idx = palette.size();
                        palette.add((BlockState)s);
                        return idx;
                    });
                    upperTag.putInt("state", fixedUpperIdx);
                } else {
                    hinge = "left";
                    BlockState generatedUpper = new BlockState(state.name, Map.of("facing", facing, "half", "upper", "hinge", hinge, "open", "false"));
                    int generatedUpperIdx = paletteIndex.computeIfAbsent(generatedUpper, s -> {
                        int idx = palette.size();
                        palette.add((BlockState)s);
                        return idx;
                    });
                    upperTag.putInt("state", generatedUpperIdx);
                    System.out.println("  [WARN] Door at (" + x + "," + y + "," + z + "): replaced non-door block above with auto-generated upper half (iso-legacy)");
                }
            } else {
                hinge = "left";
                BlockState generatedUpper = new BlockState(state.name, Map.of("facing", facing, "half", "upper", "hinge", hinge, "open", "false"));
                int generatedUpperIdx = paletteIndex.computeIfAbsent(generatedUpper, s -> {
                    int idx = palette.size();
                    palette.add((BlockState)s);
                    return idx;
                });
                CompoundTag newUpperTag = new CompoundTag();
                ListTag upperPos = new ListTag();
                upperPos.add((Object)IntTag.valueOf((int)x));
                upperPos.add((Object)IntTag.valueOf((int)(y + 1)));
                upperPos.add((Object)IntTag.valueOf((int)z));
                newUpperTag.put("pos", (Tag)upperPos);
                newUpperTag.putInt("state", generatedUpperIdx);
                blocksList.add((Object)newUpperTag);
                posIndex.put(upperKey, blocksList.size() - 1);
                System.out.println("  [WARN] Door at (" + x + "," + y + "," + z + "): auto-generated missing upper half (iso-legacy)");
            }
            BlockState fixedLower = new BlockState(state.name, Map.of("facing", facing, "half", "lower", "hinge", hinge, "open", "false"));
            int fixedLowerIdx = paletteIndex.computeIfAbsent(fixedLower, s -> {
                int idx = palette.size();
                palette.add((BlockState)s);
                return idx;
            });
            tag.putInt("state", fixedLowerIdx);
        }
    }

    private static long packPos(int x, int y, int z) {
        return (long)x & 0xFFFFFL | ((long)y & 0xFFFFFL) << 20 | ((long)z & 0xFFFFFL) << 40;
    }

    private static boolean isDoorBlock(String name) {
        return name.endsWith("_door");
    }

    void fixDoubleDoorHinges(ListTag blocksList, List<BlockState> palette, Map<BlockState, Integer> paletteIndex, int sizeX, int sizeZ) {
        HashMap<Long, Integer> posIndex = new HashMap<Long, Integer>();
        HashMap<Long, BlockState> posState = new HashMap<Long, BlockState>();
        for (int i = 0; i < blocksList.size(); ++i) {
            CompoundTag tag = blocksList.getCompound(i);
            ListTag pos = tag.getList("pos", 3);
            long key = PngToNbtConverter.packPos(pos.getInt(0), pos.getInt(1), pos.getInt(2));
            posIndex.put(key, i);
            posState.put(key, palette.get(tag.getInt("state")));
        }
        int fixed = 0;
        block13: for (int i = 0; i < blocksList.size(); ++i) {
            CompoundTag tag = blocksList.getCompound(i);
            BlockState state = palette.get(tag.getInt("state"));
            if (!PngToNbtConverter.isDoorBlock(state.name) || !"lower".equals(state.properties.get("half"))) continue;
            ListTag pos = tag.getList("pos", 3);
            int x = pos.getInt(0);
            int y = pos.getInt(1);
            int z = pos.getInt(2);
            String facing = state.properties.getOrDefault("facing", "north");
            String doorType = state.name;
            int openX = x;
            int openZ = z;
            int presentX = x;
            int presentZ = z;
            switch (facing) {
                case "north": {
                    openX = x - 1;
                    presentX = x + 1;
                    break;
                }
                case "east": {
                    openZ = z - 1;
                    presentZ = z + 1;
                    break;
                }
                case "south": {
                    openX = x + 1;
                    presentX = x - 1;
                    break;
                }
                case "west": {
                    openZ = z + 1;
                    presentZ = z - 1;
                    break;
                }
                default: {
                    continue block13;
                }
            }
            long openKey = PngToNbtConverter.packPos(openX, y, openZ);
            BlockState openState = (BlockState)posState.get(openKey);
            boolean openSideOpen = openState == null ? true : "minecraft:air".equals(openState.name) || doorType.equals(openState.name);
            boolean presentSideInBounds = presentX >= 0 && presentX < sizeX && presentZ >= 0 && presentZ < sizeZ;
            String targetHinge = openSideOpen && presentSideInBounds ? "right" : "left";
            String currentHinge = state.properties.getOrDefault("hinge", "left");
            if (targetHinge.equals(currentHinge)) continue;
            HashMap<String, String> lowerProps = new HashMap<String, String>(state.properties);
            lowerProps.put("hinge", targetHinge);
            BlockState fixedLower = new BlockState(state.name, lowerProps);
            int fixedLowerIdx = paletteIndex.computeIfAbsent(fixedLower, s -> {
                int idx = palette.size();
                palette.add((BlockState)s);
                return idx;
            });
            tag.putInt("state", fixedLowerIdx);
            long upperKey = PngToNbtConverter.packPos(x, y + 1, z);
            Integer upperIdx = (Integer)posIndex.get(upperKey);
            if (upperIdx != null) {
                CompoundTag upperTag = blocksList.getCompound(upperIdx.intValue());
                BlockState upperState = palette.get(upperTag.getInt("state"));
                if (doorType.equals(upperState.name) && "upper".equals(upperState.properties.get("half"))) {
                    HashMap<String, String> upperProps = new HashMap<String, String>(upperState.properties);
                    upperProps.put("hinge", targetHinge);
                    BlockState fixedUpper = new BlockState(upperState.name, upperProps);
                    int fixedUpperIdx = paletteIndex.computeIfAbsent(fixedUpper, s -> {
                        int idx = palette.size();
                        palette.add((BlockState)s);
                        return idx;
                    });
                    upperTag.putInt("state", fixedUpperIdx);
                }
            }
            ++fixed;
            System.out.println("  [DOOR] Fixed hinge at (" + x + "," + y + "," + z + ") " + doorType + " facing=" + facing + ": " + currentHinge + " -> " + targetHinge);
        }
        if (fixed > 0) {
            System.out.println("  [DOOR] Fixed " + fixed + " double-door hinge(s)");
        }
    }

    private BlockState mapDoor(String id, String variant) {
        HashMap<String, String> props = new HashMap<String, String>();
        String facing = this.extractVariant(variant, "facing");
        String half = this.extractVariant(variant, "half");
        String hinge = this.extractVariant(variant, "hinge");
        props.put("facing", facing != null ? facing : "north");
        props.put("half", half != null ? half : "lower");
        props.put("hinge", hinge != null ? hinge : "left");
        props.put("open", "false");
        String doorBlock = switch (id) {
            case "minecraft:wooden_door" -> "minecraft:oak_door";
            case "minecraft:spruce_door" -> "minecraft:spruce_door";
            case "minecraft:birch_door" -> "minecraft:birch_door";
            case "minecraft:jungle_door" -> "minecraft:jungle_door";
            case "minecraft:acacia_door" -> "minecraft:acacia_door";
            case "minecraft:dark_oak_door" -> "minecraft:dark_oak_door";
            case "minecraft:iron_door" -> "minecraft:iron_door";
            default -> "minecraft:oak_door";
        };
        return new BlockState(doorBlock, props);
    }

    private BlockState mapStoneSlab(String id, String variant) {
        boolean top;
        HashMap<String, String> props = new HashMap<String, String>();
        int meta = this.parseIntSafe(variant);
        boolean bl = top = meta >= 8;
        if (top) {
            props.put("type", "top");
            meta -= 8;
        } else if (variant.contains("half=top")) {
            props.put("type", "top");
        } else {
            props.put("type", "bottom");
        }
        if (variant.contains("variant=quartz")) {
            return new BlockState("minecraft:quartz_slab", props);
        }
        if ("minecraft:stone_slab2".equals(id)) {
            return new BlockState("minecraft:red_sandstone_slab", props);
        }
        return switch (meta) {
            case 0 -> new BlockState("minecraft:smooth_stone_slab", props);
            case 1 -> new BlockState("minecraft:sandstone_slab", props);
            case 3 -> new BlockState("minecraft:cobblestone_slab", props);
            case 4 -> new BlockState("minecraft:brick_slab", props);
            case 5 -> new BlockState("minecraft:stone_brick_slab", props);
            default -> new BlockState("minecraft:smooth_stone_slab", props);
        };
    }

    private BlockState mapColoredBlock(String variant, String suffix, String defaultBlock) {
        String[] colors = new String[]{"white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"};
        int meta = this.parseIntSafe(variant);
        if (meta >= 0 && meta < colors.length) {
            return new BlockState("minecraft:" + colors[meta] + suffix);
        }
        return new BlockState(defaultBlock);
    }

    private BlockState mapCarpet(String variant) {
        String[] colors = new String[]{"white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"};
        int meta = this.parseIntSafe(variant);
        if (meta >= 0 && meta < colors.length) {
            return new BlockState("minecraft:" + colors[meta] + "_carpet");
        }
        return new BlockState("minecraft:white_carpet");
    }

    private BlockState mapColoredTerracotta(String variant) {
        String color = this.extractVariant(variant, "color");
        if (color == null) {
            int meta = this.parseIntSafe(variant);
            String[] colors = new String[]{"white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"};
            if (meta >= 0 && meta < colors.length) {
                color = colors[meta];
            }
        }
        if (color == null) {
            return new BlockState("minecraft:terracotta");
        }
        if ("silver".equals(color)) {
            color = "light_gray";
        }
        return new BlockState("minecraft:" + color + "_terracotta");
    }

    private Map<String, String> parseFacingProps(String variant) {
        HashMap<String, String> props = new HashMap<String, String>();
        if (variant == null || variant.isEmpty()) {
            return props;
        }
        for (String part : variant.split(",")) {
            String[] kv = part.split("=", 2);
            if (kv.length != 2) continue;
            String key = kv[0].trim();
            String value = kv[1].trim();
            if (!"facing".equals(key) && !"half".equals(key) && !"axis".equals(key) && !"part".equals(key) && !"open".equals(key) && !"hinge".equals(key)) continue;
            props.put(key, value);
        }
        return props;
    }

    private Map<String, String> parseRosetteProps(String variant) {
        HashMap<String, String> props = new HashMap<String, String>();
        if (variant == null || variant.isEmpty()) {
            return props;
        }
        block8: for (String part : variant.split(",")) {
            String[] kv = part.split("=", 2);
            if (kv.length != 2) continue;
            String key = kv[0].trim();
            String value = kv[1].trim();
            switch (key) {
                case "facing": {
                    props.put("facing", value);
                    continue block8;
                }
                case "topbottom": {
                    props.put("half", value);
                }
            }
        }
        return props;
    }

    private Map<String, String> parseSlabProps(String variant) {
        HashMap<String, String> props = new HashMap<String, String>();
        if (variant.contains("half=top")) {
            props.put("type", "top");
        } else {
            props.put("type", "bottom");
        }
        return props;
    }

    private String extractVariant(String variant, String key) {
        if (variant == null || variant.isEmpty()) {
            return null;
        }
        for (String part : variant.split(",")) {
            String[] kv = part.split("=", 2);
            if (kv.length != 2 || !kv[0].trim().equals(key)) continue;
            return kv[1].trim();
        }
        return null;
    }

    private int parseIntSafe(String s) {
        if (s == null || s.isEmpty()) {
            return -1;
        }
        try {
            return Integer.parseInt(s.trim());
        }
        catch (NumberFormatException e) {
            return -1;
        }
    }

    static int rgb(int r, int g, int b) {
        return r << 16 | g << 8 | b;
    }

    public int[][][] decodePng(Path pngPath, int width, int length) throws IOException {
        BufferedImage img;
        try (InputStream is = Files.newInputStream(pngPath, new OpenOption[0]);){
            img = ImageIO.read(is);
        }
        int pngWidth = img.getWidth();
        int pngHeight = img.getHeight();
        int nbFloors = (pngWidth + 1) / (width + 1);
        System.out.println("  PNG: " + pngWidth + "x" + pngHeight + ", building: " + width + "x" + length + ", floors: " + nbFloors);
        if (pngHeight != length) {
            System.out.println("  [WARN] PNG height (" + pngHeight + ") != building length (" + length + ")");
        }
        int[][][] grid = new int[nbFloors][width][length];
        for (int floor = 0; floor < nbFloors; ++floor) {
            for (int widthPos = 0; widthPos < width; ++widthPos) {
                int px = floor * (width + 1) + (width - widthPos - 1);
                for (int z = 0; z < length; ++z) {
                    int py = z;
                    if (px < pngWidth && py < pngHeight) {
                        int argb = img.getRGB(px, py);
                        if ((argb >> 24 & 0xFF) != 255) {
                            grid[floor][widthPos][z] = WHITE;
                            continue;
                        }
                        int r = argb >> 16 & 0xFF;
                        int g = argb >> 8 & 0xFF;
                        int b = argb & 0xFF;
                        grid[floor][widthPos][z] = PngToNbtConverter.rgb(r, g, b);
                        continue;
                    }
                    grid[floor][widthPos][z] = WHITE;
                }
            }
        }
        return grid;
    }

    public Set<Integer> collectColors(Path pngPath, int width, int length) throws IOException {
        int[][][] grid = this.decodePng(pngPath, width, length);
        HashSet<Integer> colors = new HashSet<Integer>();
        int[][][] nArray = grid;
        int n = nArray.length;
        for (int i = 0; i < n; ++i) {
            int[][] floor;
            int[][] nArray2 = floor = nArray[i];
            int n2 = nArray2.length;
            for (int j = 0; j < n2; ++j) {
                int[] row;
                for (int c : row = nArray2[j]) {
                    colors.add(c);
                }
            }
        }
        return colors;
    }

    public ConversionResult convert(Path pngPath, int width, int length, Path outputPath, String name) throws IOException {
        int nbFloors;
        System.out.println("Converting " + name + "...");
        int[][][] grid = this.decodePng(pngPath, width, length);
        int height = nbFloors = grid.length;
        ArrayList<BlockState> palette = new ArrayList<BlockState>();
        LinkedHashMap<BlockState, Integer> paletteIndex = new LinkedHashMap<BlockState, Integer>();
        ListTag blocksList = new ListTag();
        int airSkipped = 0;
        LinkedHashSet<Integer> unmappedColors = new LinkedHashSet<Integer>();
        TreeMap<String, Integer> blockCounts = new TreeMap<String, Integer>();
        ArrayList<SpecialPoint> specialPoints = new ArrayList<SpecialPoint>();
        for (int floor = 0; floor < nbFloors; ++floor) {
            for (int z = 0; z < length; ++z) {
                for (int x = 0; x < width; ++x) {
                    BlockState state;
                    ListTag posTag;
                    CompoundTag blockTag;
                    int idx;
                    int color = grid[floor][x][z];
                    int templateX = z;
                    int templateY = floor;
                    int templateZ = x;
                    SpecialPointInfo spInfo = this.specialPointMap.get(color);
                    if (spInfo != null) {
                        specialPoints.add(new SpecialPoint(spInfo.type, spInfo.subtype, spInfo.orientation, spInfo.placement, templateX, templateY, templateZ));
                        BlockState mockState = this.specialPointToMockBlockState(spInfo);
                        if (mockState != null) {
                            idx = paletteIndex.computeIfAbsent(mockState, s -> {
                                int i = palette.size();
                                palette.add((BlockState)s);
                                return i;
                            });
                            blockTag = new CompoundTag();
                            posTag = new ListTag();
                            posTag.add((Object)IntTag.valueOf((int)templateX));
                            posTag.add((Object)IntTag.valueOf((int)templateY));
                            posTag.add((Object)IntTag.valueOf((int)templateZ));
                            blockTag.put("pos", (Tag)posTag);
                            blockTag.putInt("state", idx);
                            blocksList.add((Object)blockTag);
                            blockCounts.merge(mockState.name, 1, Integer::sum);
                            continue;
                        }
                    }
                    if ((state = this.colorMap.get(color)) != null) {
                        idx = paletteIndex.computeIfAbsent(state, s -> {
                            int i = palette.size();
                            palette.add((BlockState)s);
                            return i;
                        });
                        blockTag = new CompoundTag();
                        posTag = new ListTag();
                        posTag.add((Object)IntTag.valueOf((int)templateX));
                        posTag.add((Object)IntTag.valueOf((int)templateY));
                        posTag.add((Object)IntTag.valueOf((int)templateZ));
                        blockTag.put("pos", (Tag)posTag);
                        blockTag.putInt("state", idx);
                        blocksList.add((Object)blockTag);
                        blockCounts.merge(state.name, 1, Integer::sum);
                        continue;
                    }
                    if (this.isAirColor(color)) {
                        ++airSkipped;
                        continue;
                    }
                    unmappedColors.add(color);
                    this.unmappedColourSet.add(color);
                    ++airSkipped;
                }
            }
        }
        this.fixDoorPairs(blocksList, palette, paletteIndex);
        this.fixDoubleDoorHinges(blocksList, palette, paletteIndex, length, width);
        CompoundTag root = new CompoundTag();
        root.putInt("DataVersion", DATA_VERSION);
        ListTag sizeTag = new ListTag();
        sizeTag.add((Object)IntTag.valueOf((int)length));
        sizeTag.add((Object)IntTag.valueOf((int)height));
        sizeTag.add((Object)IntTag.valueOf((int)width));
        root.put("size", (Tag)sizeTag);
        ListTag paletteTag = new ListTag();
        for (BlockState state : palette) {
            paletteTag.add((Object)state.toNbt());
        }
        root.put("palette", (Tag)paletteTag);
        root.put("blocks", (Tag)blocksList);
        root.put("entities", (Tag)new ListTag());
        Files.createDirectories(outputPath.getParent(), new FileAttribute[0]);
        NbtIo.writeCompressed((CompoundTag)root, (Path)outputPath);
        int totalBlocks = blocksList.size();
        System.out.println("  Size: " + width + "x" + height + "x" + length);
        System.out.println("  Blocks placed: " + totalBlocks);
        System.out.println("  Air/skip: " + airSkipped);
        System.out.println("  Special points: " + specialPoints.size());
        System.out.println("  Palette: " + palette.size() + " entries");
        if (!unmappedColors.isEmpty()) {
            System.out.println("  Unmapped colors: " + unmappedColors.size());
            Iterator iterator = unmappedColors.iterator();
            while (iterator.hasNext()) {
                int c = (Integer)iterator.next();
                int r = c >> 16 & 0xFF;
                int g = c >> 8 & 0xFF;
                int b = c & 0xFF;
                System.out.println("    RGB(" + r + "," + g + "," + b + ")");
            }
        }
        System.out.println("  File: " + String.valueOf(outputPath));
        return new ConversionResult(name, width, height, length, totalBlocks, airSkipped, specialPoints.size(), unmappedColors, blockCounts, root);
    }

    private BlockState specialPointToMockBlockState(SpecialPointInfo info) {
        return switch (info.type) {
            case "sleepingPos" -> this.mockMarkerState("sleeping_pos");
            case "sellingPos" -> this.mockMarkerState("selling_pos");
            case "craftingPos" -> this.mockMarkerState("crafting_pos");
            case "defendingPos" -> this.mockMarkerState("defending_pos");
            case "shelterPos" -> this.mockMarkerState("shelter_pos");
            case "pathStartPos" -> this.mockMarkerState("path_start_pos");
            case "leisurePos" -> this.mockMarkerState("leisure_pos");
            case "stall" -> this.mockMarkerState("stall");
            case "fishingSpot" -> this.mockMarkerState("fishing_spot");
            case "preserve_ground" -> {
                String v1 = switch (info.subtype) {
                    case "surface" -> "preserve_ground";
                    case "depth" -> "preserve_ground_depth";
                    case "allbuttrees" -> "preserve_ground_allbuttrees";
                    case "grass" -> "preserve_ground_grass";
                    default -> "preserve_ground";
                };
                String markerType = v1;
                yield this.mockMarkerState(markerType);
            }
            case "torchGuess" -> this.mockMarkerState("torch");
            case "furnace", "signPos" -> {
                String mockType = info.type.equals("furnace") ? "furnace" : "sign_pos";
                HashMap<String, String> props = new HashMap<String, String>();
                props.put("type", mockType);
                if (info.orientation != null && !"guess".equals(info.orientation)) {
                    props.put("facing", info.orientation);
                    props.put("guess", "false");
                } else {
                    props.put("facing", "north");
                    props.put("guess", "true");
                }
                yield new BlockState("millenaire:mock_facing_marker", props);
            }
            case "healingSpot" -> this.mockMarkerState("healing_spot");
            case "brickSpot" -> this.mockMarkerState("brick_spot");
            case "silkwormBlock" -> this.mockMarkerState("silkworm_block");
            case "snailSoilBlock" -> this.mockMarkerState("snail_soil_block");
            case "cacaoSpot" -> this.mockMarkerState("cacao_spot");
            case "plainSign" -> {
                HashMap<String, String> props = new HashMap<String, String>();
                props.put("type", "sign_pos");
                props.put("facing", "north");
                props.put("guess", "true");
                yield new BlockState("millenaire:mock_facing_marker", props);
            }
            case "decorative" -> this.mockBlockState("millenaire:mock_decor", "decor_type", info.subtype);
            case "banner" -> {
                String placement = info.placement;
                if (placement == null) {
                    yield null;
                }
                HashMap<String, String> props = new HashMap<String, String>();
                props.put("subtype", info.subtype);
                if (placement.startsWith("wall_")) {
                    props.put("facing", placement.substring("wall_".length()));
                    yield new BlockState("millenaire:mock_banner_wall", props);
                }
                if (placement.startsWith("standing_")) {
                    props.put("rotation", placement.substring("standing_".length()));
                    yield new BlockState("millenaire:mock_banner_standing", props);
                }
                yield null;
            }
            case "soil" -> this.mockBlockState("millenaire:mock_soil", "crop", info.subtype);
            case "source" -> this.mockBlockState("millenaire:mock_source", "material", info.subtype);
            case "freeBlock" -> this.mockBlockState("millenaire:mock_free", "material", info.subtype);
            case "treeSpawn" -> this.mockBlockState("millenaire:mock_tree_spawn", "tree", info.subtype);
            case "animalSpawn" -> this.mockBlockState("millenaire:mock_animal_spawn", "animal", info.subtype);
            case "chest" -> {
                HashMap<String, String> props = new HashMap<String, String>();
                props.put("chest_type", info.subtype);
                if (info.orientation != null && !"guess".equals(info.orientation)) {
                    props.put("facing", info.orientation);
                    props.put("guess", "false");
                } else {
                    props.put("facing", "north");
                    props.put("guess", "true");
                }
                yield new BlockState("millenaire:mock_chest", props);
            }
            default -> {
                String sig = info.type + "/" + info.subtype;
                if (this.unmappedSpecialPointSignatures.add(sig)) {
                    System.out.println("  WARNING: special point type not converted to mock block: " + sig);
                }
                yield null;
            }
        };
    }

    private BlockState mockMarkerState(String type) {
        return this.mockBlockState("millenaire:mock_marker", "type", type);
    }

    private BlockState mockBlockState(String name, String propKey, String propValue) {
        HashMap<String, String> props = new HashMap<String, String>();
        props.put(propKey, propValue);
        return new BlockState(name, props);
    }

    private boolean isAirColor(int color) {
        if (color == WHITE || this.specialColors.contains(color)) {
            return true;
        }
        int r = color >> 16 & 0xFF;
        int g = color >> 8 & 0xFF;
        int b = color & 0xFF;
        return r > 245 && g > 245 && b > 245;
    }

    public Map<Integer, BlockState> getColorMap() {
        return Collections.unmodifiableMap(this.colorMap);
    }

    public Set<Integer> getSpecialColors() {
        return Collections.unmodifiableSet(this.specialColors);
    }

    private record BlocklistEntry(String name, String blockId, String variant, boolean setAfter, int rgb) {
    }

    private record SpecialPointInfo(String type, String subtype, String orientation, String placement) {
    }

    public record BlockState(String name, Map<String, String> properties) {
        public BlockState(String name) {
            this(name, Map.of());
        }

        public CompoundTag toNbt() {
            CompoundTag tag = new CompoundTag();
            tag.putString("Name", this.name);
            if (!this.properties.isEmpty()) {
                CompoundTag props = new CompoundTag();
                this.properties.forEach((arg_0, arg_1) -> ((CompoundTag)props).putString(arg_0, arg_1));
                tag.put("Properties", (Tag)props);
            }
            return tag;
        }
    }

    public record SpecialPoint(String type, String subtype, String orientation, String placement, int x, int y, int z) {
    }

    public record ConversionResult(String buildingName, int width, int height, int depth, int totalBlocks, int airSkipped, int specialPointCount, Set<Integer> unmappedColors, Map<String, Integer> blockCounts, CompoundTag templateNbt) {
    }
}

