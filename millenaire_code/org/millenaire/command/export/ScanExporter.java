/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.registries.BuiltInRegistries
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraft.world.level.levelgen.Heightmap$Types
 */
package org.millenaire.command.export;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import org.millenaire.village.Village;

public final class ScanExporter {
    private static final Map<Block, Character> BLOCK_CHAR_MAP = new LinkedHashMap<Block, Character>();

    private ScanExporter() {
    }

    public static Path export(ServerLevel level, Village village, int radius, Path dir) throws IOException {
        Path file = dir.resolve("village-scan.txt");
        BlockPos center = village.getCenter();
        int centerX = center.getX();
        int centerZ = center.getZ();
        int surfaceY = level.getHeight(Heightmap.Types.WORLD_SURFACE, centerX, centerZ) - 1;
        int[] layers = new int[]{surfaceY - 1, surfaceY, surfaceY + 1, surfaceY + 2, surfaceY + 3, surfaceY + 4};
        String[] layerLabels = new String[]{"foundation", "surface", "surface+1", "surface+2", "surface+3", "surface+4"};
        int minX = centerX - radius;
        int maxX = centerX + radius;
        int minZ = centerZ - radius;
        int maxZ = centerZ + radius;
        LinkedHashSet<Block> seenBlocks = new LinkedHashSet<Block>();
        StringBuilder sb = new StringBuilder();
        sb.append("=== Scan @ center (%d, %d, %d) radius=%d ===\n".formatted(centerX, surfaceY, centerZ, radius));
        sb.append("Village: %s\n".formatted(village.getId()));
        sb.append("Surface Y: %d\n".formatted(surfaceY));
        int zLabelWidth = Math.max(String.valueOf(minZ).length(), String.valueOf(maxZ).length());
        for (int layerIdx = 0; layerIdx < layers.length; ++layerIdx) {
            int y = layers[layerIdx];
            sb.append("\n--- Y=%d (%s) ---\n".formatted(y, layerLabels[layerIdx]));
            sb.append(" ".repeat(zLabelWidth + 1));
            for (int x = minX; x <= maxX; ++x) {
                if (x % 5 == 0) {
                    String label = String.valueOf(x);
                    sb.append(label);
                    int skip = label.length() - 1;
                    x += skip;
                    continue;
                }
                sb.append(' ');
            }
            sb.append('\n');
            for (int z = minZ; z <= maxZ; ++z) {
                sb.append(String.format("%" + zLabelWidth + "d ", z));
                for (int x = minX; x <= maxX; ++x) {
                    Block block = level.getBlockState(new BlockPos(x, y, z)).getBlock();
                    Character ch = BLOCK_CHAR_MAP.get(block);
                    if (ch != null) {
                        sb.append(ch);
                    } else {
                        sb.append('?');
                    }
                    if (block == Blocks.AIR || block == Blocks.CAVE_AIR) continue;
                    seenBlocks.add(block);
                }
                sb.append('\n');
            }
        }
        sb.append("\nLegend:\n");
        for (Block block : seenBlocks) {
            Character ch = BLOCK_CHAR_MAP.get(block);
            String symbol = ch != null ? String.valueOf(ch) : "?";
            String name = BuiltInRegistries.BLOCK.getKey((Object)block).getPath();
            sb.append("  %s = %s\n".formatted(symbol, name));
        }
        Files.writeString(file, (CharSequence)sb.toString(), new OpenOption[0]);
        return file;
    }

    static {
        BLOCK_CHAR_MAP.put(Blocks.AIR, Character.valueOf(' '));
        BLOCK_CHAR_MAP.put(Blocks.CAVE_AIR, Character.valueOf(' '));
        BLOCK_CHAR_MAP.put(Blocks.GRASS_BLOCK, Character.valueOf('G'));
        BLOCK_CHAR_MAP.put(Blocks.DIRT, Character.valueOf('D'));
        BLOCK_CHAR_MAP.put(Blocks.STONE, Character.valueOf('S'));
        BLOCK_CHAR_MAP.put(Blocks.COBBLESTONE, Character.valueOf('C'));
        BLOCK_CHAR_MAP.put(Blocks.OAK_PLANKS, Character.valueOf('P'));
        BLOCK_CHAR_MAP.put(Blocks.SPRUCE_PLANKS, Character.valueOf('P'));
        BLOCK_CHAR_MAP.put(Blocks.OAK_LOG, Character.valueOf('L'));
        BLOCK_CHAR_MAP.put(Blocks.SPRUCE_LOG, Character.valueOf('L'));
        BLOCK_CHAR_MAP.put(Blocks.OAK_STAIRS, Character.valueOf('/'));
        BLOCK_CHAR_MAP.put(Blocks.COBBLESTONE_STAIRS, Character.valueOf('/'));
        BLOCK_CHAR_MAP.put(Blocks.STONE_STAIRS, Character.valueOf('/'));
        BLOCK_CHAR_MAP.put(Blocks.OAK_SLAB, Character.valueOf('-'));
        BLOCK_CHAR_MAP.put(Blocks.COBBLESTONE_SLAB, Character.valueOf('-'));
        BLOCK_CHAR_MAP.put(Blocks.OAK_FENCE, Character.valueOf('|'));
        BLOCK_CHAR_MAP.put(Blocks.OAK_DOOR, Character.valueOf('d'));
        BLOCK_CHAR_MAP.put(Blocks.GLASS_PANE, Character.valueOf('='));
        BLOCK_CHAR_MAP.put(Blocks.GLASS, Character.valueOf('='));
        BLOCK_CHAR_MAP.put(Blocks.CHEST, Character.valueOf('$'));
        BLOCK_CHAR_MAP.put(Blocks.CRAFTING_TABLE, Character.valueOf('T'));
        BLOCK_CHAR_MAP.put(Blocks.FURNACE, Character.valueOf('F'));
        BLOCK_CHAR_MAP.put(Blocks.WATER, Character.valueOf('~'));
        BLOCK_CHAR_MAP.put(Blocks.FARMLAND, Character.valueOf('f'));
        BLOCK_CHAR_MAP.put(Blocks.WHEAT, Character.valueOf('w'));
        BLOCK_CHAR_MAP.put(Blocks.TORCH, Character.valueOf('t'));
        BLOCK_CHAR_MAP.put(Blocks.WALL_TORCH, Character.valueOf('t'));
        BLOCK_CHAR_MAP.put(Blocks.OAK_LEAVES, Character.valueOf('@'));
        BLOCK_CHAR_MAP.put(Blocks.SPRUCE_LEAVES, Character.valueOf('@'));
    }
}

