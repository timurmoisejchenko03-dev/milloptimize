/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.builder.LiteralArgumentBuilder
 *  com.mojang.brigadier.context.CommandContext
 *  net.minecraft.commands.CommandSourceStack
 *  net.minecraft.commands.Commands
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.BlockPos$MutableBlockPos
 *  net.minecraft.core.Position
 *  net.minecraft.network.chat.Component
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.properties.Property
 *  net.minecraft.world.level.levelgen.Heightmap$Types
 */
package org.millenaire.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.levelgen.Heightmap;
import org.millenaire.block.MillPathBlock;
import org.millenaire.block.MillPathSlabBlock;
import org.millenaire.culture.ModCultures;
import org.millenaire.culture.VillageType;
import org.millenaire.village.Village;
import org.millenaire.village.VillageSavedData;

public final class PathAuditCommand {
    private PathAuditCommand() {
    }

    public static void registerUnder(LiteralArgumentBuilder<CommandSourceStack> parent) {
        parent.then(Commands.literal((String)"path-audit").executes(PathAuditCommand::execute));
    }

    private static int execute(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = (CommandSourceStack)ctx.getSource();
        ServerLevel level = source.getLevel();
        if (level.dimension() != Level.OVERWORLD) {
            source.sendFailure((Component)Component.literal((String)"No villages outside the Overworld."));
            return 0;
        }
        BlockPos searchPos = BlockPos.containing((Position)source.getPosition());
        Village village = VillageSavedData.get(level).getVillageManager().findNearestVillage(searchPos, 5000.0);
        if (village == null) {
            source.sendFailure((Component)Component.literal((String)"No village within 5000 blocks."));
            return 0;
        }
        int radius = 80;
        VillageType vt = ModCultures.getVillageType(village.getVillageTypeId());
        if (vt != null) {
            radius = vt.radius();
        }
        int finalRadius = radius;
        AuditReport report = PathAuditCommand.scan(level, village.getCenter(), radius);
        source.sendSuccess(() -> Component.literal((String)("=== path-audit for village " + village.getId().uuid().toString().substring(0, 8) + " @" + village.getCenter().toShortString() + " r=" + finalRadius + " ===")), false);
        source.sendSuccess(() -> Component.literal((String)("blocks.total=" + report.total)), false);
        source.sendSuccess(() -> Component.literal((String)("blocks.full=" + report.full)), false);
        source.sendSuccess(() -> Component.literal((String)("blocks.slab=" + report.slab)), false);
        source.sendSuccess(() -> Component.literal((String)("edges.total=" + report.edgesTotal)), false);
        source.sendSuccess(() -> Component.literal((String)("edges.flat=" + report.edgesFlat)), false);
        source.sendSuccess(() -> Component.literal((String)("edges.half=" + report.edgesHalf)), false);
        source.sendSuccess(() -> Component.literal((String)("edges.full_step=" + report.edgesFullStep)), false);
        source.sendSuccess(() -> Component.literal((String)("edges.big_step=" + report.edgesBigStep)), false);
        source.sendSuccess(() -> Component.literal((String)("peaks.isolated=" + report.isolatedPeaks)), false);
        source.sendSuccess(() -> Component.literal((String)("valleys.isolated=" + report.isolatedValleys)), false);
        source.sendSuccess(() -> Component.literal((String)("max_abs_dy_half=" + report.maxAbsDyHalf)), false);
        String rough = String.format("%.3f", report.roughness());
        source.sendSuccess(() -> Component.literal((String)("roughness=" + rough)), false);
        int shown = 0;
        for (BlockPos p : report.peakSamples) {
            if (shown >= 5) break;
            source.sendSuccess(() -> Component.literal((String)("sample.peak=" + p.toShortString())), false);
            ++shown;
        }
        shown = 0;
        for (BlockPos p : report.valleySamples) {
            if (shown >= 5) break;
            source.sendSuccess(() -> Component.literal((String)("sample.valley=" + p.toShortString())), false);
            ++shown;
        }
        return 1;
    }

    private static int surfaceHalfY(BlockPos pos, BlockState state) {
        Block b = state.getBlock();
        if (b instanceof MillPathSlabBlock) {
            return 2 * pos.getY() + 1;
        }
        return 2 * (pos.getY() + 1);
    }

    private static boolean isSystemPath(BlockState s) {
        Block block = s.getBlock();
        if (block instanceof MillPathBlock) {
            MillPathBlock mp = (MillPathBlock)block;
            return (Boolean)s.getValue((Property)MillPathBlock.STABLE) == false;
        }
        block = s.getBlock();
        if (block instanceof MillPathSlabBlock) {
            MillPathSlabBlock ms = (MillPathSlabBlock)block;
            return (Boolean)s.getValue((Property)MillPathSlabBlock.STABLE) == false;
        }
        return false;
    }

    private static AuditReport scan(ServerLevel level, BlockPos center, int radius) {
        AuditReport r = new AuditReport();
        HashMap<Long, PathCell> cells = new HashMap<Long, PathCell>();
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        int cx = center.getX();
        int cz = center.getZ();
        for (int x = cx - radius; x <= cx + radius; ++x) {
            block1: for (int z = cz - radius; z <= cz + radius; ++z) {
                int terrainY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
                for (int y = terrainY - 3; y <= terrainY + 4; ++y) {
                    m.set(x, y, z);
                    BlockState s = level.getBlockState((BlockPos)m);
                    if (!PathAuditCommand.isSystemPath(s)) continue;
                    ++r.total;
                    boolean isSlab = s.getBlock() instanceof MillPathSlabBlock;
                    if (isSlab) {
                        ++r.slab;
                    } else {
                        ++r.full;
                    }
                    int halfY = PathAuditCommand.surfaceHalfY((BlockPos)m, s);
                    cells.put(PathAuditCommand.packXZ(x, z), new PathCell(y, halfY, m.immutable()));
                    continue block1;
                }
            }
        }
        for (Map.Entry e : cells.entrySet()) {
            long k = (Long)e.getKey();
            int x = PathAuditCommand.unpackX(k);
            int z = PathAuditCommand.unpackZ(k);
            PathCell me = (PathCell)e.getValue();
            int peakMatches = 0;
            int peakNeighbors = 0;
            int valleyMatches = 0;
            int valleyNeighbors = 0;
            for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                PathCell nb = (PathCell)cells.get(PathAuditCommand.packXZ(x + d[0], z + d[1]));
                if (nb == null) continue;
                if (x + d[0] < x || x + d[0] == x && z + d[1] < z) {
                    int dy = nb.halfY - me.halfY;
                    int abs = Math.abs(dy);
                    ++r.edgesTotal;
                    r.sumAbsDyHalf += (long)abs;
                    if (abs > r.maxAbsDyHalf) {
                        r.maxAbsDyHalf = abs;
                    }
                    if (abs == 0) {
                        ++r.edgesFlat;
                    } else if (abs == 1) {
                        ++r.edgesHalf;
                    } else if (abs == 2) {
                        ++r.edgesFullStep;
                    } else {
                        ++r.edgesBigStep;
                    }
                }
                ++peakNeighbors;
                ++valleyNeighbors;
                if (nb.halfY < me.halfY) {
                    ++peakMatches;
                }
                if (nb.halfY <= me.halfY) continue;
                ++valleyMatches;
            }
            if (peakNeighbors >= 2 && peakMatches == peakNeighbors) {
                ++r.isolatedPeaks;
                if (r.peakSamples.size() < 16) {
                    r.peakSamples.add(me.pos);
                }
            }
            if (valleyNeighbors < 2 || valleyMatches != valleyNeighbors) continue;
            ++r.isolatedValleys;
            if (r.valleySamples.size() >= 16) continue;
            r.valleySamples.add(me.pos);
        }
        return r;
    }

    private static long packXZ(int x, int z) {
        return (long)x << 32 | (long)z & 0xFFFFFFFFL;
    }

    private static int unpackX(long k) {
        return (int)(k >> 32);
    }

    private static int unpackZ(long k) {
        return (int)k;
    }

    private static final class AuditReport {
        int total;
        int full;
        int slab;
        int edgesTotal;
        int edgesFlat;
        int edgesHalf;
        int edgesFullStep;
        int edgesBigStep;
        int maxAbsDyHalf;
        long sumAbsDyHalf;
        int isolatedPeaks;
        int isolatedValleys;
        final List<BlockPos> peakSamples = new ArrayList<BlockPos>();
        final List<BlockPos> valleySamples = new ArrayList<BlockPos>();

        private AuditReport() {
        }

        double roughness() {
            return this.edgesTotal == 0 ? 0.0 : (double)this.sumAbsDyHalf / (double)this.edgesTotal / 2.0;
        }
    }

    private record PathCell(int y, int halfY, BlockPos pos) {
    }
}

