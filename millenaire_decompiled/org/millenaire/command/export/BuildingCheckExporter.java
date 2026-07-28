/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.google.gson.GsonBuilder
 *  net.minecraft.core.BlockPos
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.levelgen.Heightmap$Types
 */
package org.millenaire.command.export;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.lang.invoke.CallSite;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import org.millenaire.building.BuildingInstance;
import org.millenaire.building.BuildingPlan;
import org.millenaire.building.SpecialPoint;
import org.millenaire.culture.ModCultures;
import org.millenaire.village.Village;

public final class BuildingCheckExporter {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private BuildingCheckExporter() {
    }

    public static Path export(ServerLevel level, Village village, Path dir) throws IOException {
        Path file = dir.resolve("building-check.json");
        LinkedHashMap<String, Object> root = new LinkedHashMap<String, Object>();
        root.put("village", village.getVillageTypeId().toString());
        root.put("center", BuildingCheckExporter.blockPosStr(village.getCenter()));
        ArrayList<Map<String, Object>> buildings = new ArrayList<Map<String, Object>>();
        for (BuildingInstance b : village.getBuildings()) {
            buildings.add(BuildingCheckExporter.checkBuilding(level, b));
        }
        root.put("buildings", buildings);
        Files.writeString(file, (CharSequence)GSON.toJson(root), new OpenOption[0]);
        return file;
    }

    private static Map<String, Object> checkBuilding(ServerLevel level, BuildingInstance b) {
        LinkedHashMap<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("plan", b.getPlanId().toString());
        map.put("origin", BuildingCheckExporter.blockPosStr(b.getOrigin()));
        map.put("rotation", b.getRotation().name());
        BuildingPlan plan = ModCultures.getBuildingPlan(b.getPlanId());
        if (plan != null) {
            map.put("groundLevel", plan.groundLevel());
            map.put("size", plan.width() + "x" + plan.height() + "x" + plan.depth());
        }
        int cx = b.getOrigin().getX();
        int cz = b.getOrigin().getZ();
        if (plan != null) {
            cx += plan.width() / 2;
            cz += plan.depth() / 2;
        }
        int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, cx, cz);
        map.put("surfaceYAtCenter", surfaceY);
        map.put("originY", b.getOrigin().getY());
        ArrayList<CallSite> column = new ArrayList<CallSite>();
        int scanBottom = b.getOrigin().getY() - 5;
        int scanTop = b.getOrigin().getY() + (plan != null ? plan.height() + 3 : 15);
        for (int y = scanBottom; y <= scanTop; ++y) {
            BlockState state = level.getBlockState(new BlockPos(cx, y, cz));
            Object label = "";
            if (y == b.getOrigin().getY()) {
                label = " <-- ORIGIN_Y";
            }
            if (plan != null && y == b.getOrigin().getY() - plan.groundLevel()) {
                label = " <-- SURFACE (origin - groundLevel)";
            }
            column.add((CallSite)((Object)("Y=" + y + ": " + state.getBlock().getName().getString() + (String)label)));
        }
        map.put("centerColumn", column);
        List<SpecialPoint> points = b.getResolvedPoints();
        ArrayList checks = new ArrayList();
        for (SpecialPoint sp : points) {
            boolean isAccessPoint;
            String type;
            if (sp.pos() == null || (type = sp.type()).equals("preserve_ground") || type.equals("sign_pos")) continue;
            LinkedHashMap<String, Object> check = new LinkedHashMap<String, Object>();
            check.put("type", type);
            check.put("pos", BuildingCheckExporter.blockPosStr(sp.pos()));
            BlockPos pos = sp.pos();
            BlockState atPos = level.getBlockState(pos);
            BlockState below = level.getBlockState(pos.below());
            BlockState above = level.getBlockState(pos.above());
            BlockState above2 = level.getBlockState(pos.above(2));
            check.put("blockAtPos", atPos.getBlock().getName().getString());
            check.put("blockBelow", below.getBlock().getName().getString());
            check.put("blockAbove", above.getBlock().getName().getString());
            check.put("blockAbove2", above2.getBlock().getName().getString());
            ArrayList<Object> issues = new ArrayList<Object>();
            boolean bl = isAccessPoint = type.equals("path_start_pos") || type.equals("selling_pos") || type.equals("gathering_pos") || type.equals("source_pos");
            if (isAccessPoint) {
                if (!atPos.isAir()) {
                    issues.add("BLOCKED: block " + atPos.getBlock().getName().getString() + " instead of air");
                }
                if (!above.isAir()) {
                    issues.add("BLOCKED ABOVE: " + above.getBlock().getName().getString());
                }
                if (below.isAir()) {
                    issues.add("NO GROUND below");
                }
            }
            if (type.equals("sleep_pos") && !above2.isAir()) {
                issues.add("LOW ROOF: " + above2.getBlock().getName().getString() + " at +2");
            }
            check.put("issues", issues);
            checks.add(check);
        }
        map.put("specialPointChecks", checks);
        long issueCount = checks.stream().mapToLong(c -> ((List)c.get("issues")).size()).sum();
        map.put("totalIssues", issueCount);
        return map;
    }

    private static String blockPosStr(BlockPos pos) {
        return pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }
}

