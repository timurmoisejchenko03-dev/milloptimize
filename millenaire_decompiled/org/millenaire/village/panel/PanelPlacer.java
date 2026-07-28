/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.level.block.state.BlockState
 *  org.slf4j.Logger
 */
package org.millenaire.village.panel;

import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.millenaire.block.ModBlocks;
import org.millenaire.block.VillagePanelBlock;
import org.millenaire.block.VillagePanelBlockEntity;
import org.millenaire.building.BuildingInstance;
import org.millenaire.building.BuildingPlanSet;
import org.millenaire.building.SpecialPoint;
import org.millenaire.culture.ModCultures;
import org.millenaire.culture.VillageType;
import org.millenaire.village.Village;
import org.millenaire.village.panel.HallOfFameLoader;
import org.millenaire.village.panel.PanelContentGenerator;
import org.millenaire.village.panel.PanelType;
import org.slf4j.Logger;

public final class PanelPlacer {
    private static final Logger LOGGER = LogUtils.getLogger();

    private PanelPlacer() {
    }

    public static void placePanels(ServerLevel level, Village village, BuildingInstance building) {
        BuildingPlanSet planSet;
        BuildingPlanSet buildingPlanSet = planSet = building.getPlanSetId() != null ? ModCultures.getBuildingPlanSet(building.getPlanSetId()) : null;
        if (planSet != null && planSet.isHoF()) {
            PanelPlacer.fillHoFSigns(level, village, building);
            return;
        }
        VillageType villageType = ModCultures.getVillageType(village.getVillageTypeId());
        if (villageType != null && !villageType.showTownHallSigns()) {
            return;
        }
        List<SpecialPoint> signPoints = building.getPointsByType("signPos");
        if (signPoints.isEmpty()) {
            return;
        }
        int[] signOrder = PanelPlacer.resolveSignOrder(planSet, building);
        for (int i = 0; i < signPoints.size(); ++i) {
            SpecialPoint sp = signPoints.get(i);
            BlockPos pos = sp.pos();
            int logicalIndex = PanelPlacer.applySignOrder(signOrder, i);
            boolean isMarvel = villageType != null && villageType.isMarvel();
            boolean isPlayerControlled = villageType != null && villageType.playerControlled();
            PanelType panelType = PanelPlacer.determinePanelType(logicalIndex, planSet, building, isMarvel, isPlayerControlled);
            Direction facing = PanelPlacer.resolveFacing(level, sp, pos, village.getCenter());
            BlockState panelState = (BlockState)((VillagePanelBlock)((Object)ModBlocks.VILLAGE_PANEL.get())).defaultBlockState().setValue(VillagePanelBlock.FACING, (Comparable)facing);
            level.setBlock(pos, panelState, 3);
            BlockEntity be = level.getBlockEntity(pos);
            if (!(be instanceof VillagePanelBlockEntity)) continue;
            VillagePanelBlockEntity panelBe = (VillagePanelBlockEntity)be;
            panelBe.setBuildingId(building.getId());
            panelBe.setPanelType(panelType);
            panelBe.setVillageId(village.getId().uuid());
            panelBe.setCultureId(village.getCultureId());
            panelBe.setSignIndex(logicalIndex);
            PanelContentGenerator.DisplayData displayData = PanelContentGenerator.generateDisplayLines(panelType, village, building, level, logicalIndex);
            panelBe.updateDisplayLines(displayData.displayLines());
            level.sendBlockUpdated(pos, panelState, panelState, 3);
        }
        LOGGER.debug("[Millenaire] {} panels placed for {}", (Object)signPoints.size(), (Object)building.getPlanId().getPath());
    }

    static PanelType determinePanelType(int index, @Nullable BuildingPlanSet planSet, BuildingInstance building, boolean isMarvel) {
        return PanelPlacer.determinePanelType(index, planSet, building, isMarvel, false);
    }

    static PanelType determinePanelType(int index, @Nullable BuildingPlanSet planSet, BuildingInstance building, boolean isMarvel, boolean isPlayerControlled) {
        boolean isTownhall;
        boolean bl = isTownhall = planSet != null && planSet.isTownHall();
        if (isTownhall && isMarvel) {
            return switch (index) {
                case 0 -> PanelType.VILLAGE_SUMMARY;
                case 1 -> PanelType.RESOURCES;
                case 2 -> PanelType.WALLS;
                case 3 -> PanelType.MARVEL_PROJECTS;
                case 4 -> PanelType.PROJECTS;
                case 5 -> PanelType.CONSTRUCTIONS;
                case 6 -> PanelType.POPULATION;
                case 7 -> PanelType.VILLAGE_MAP;
                case 8 -> PanelType.MILITARY;
                case 9 -> PanelType.MARVEL_RESOURCES;
                case 10 -> PanelType.MARVEL_DONATIONS;
                default -> PanelType.VILLAGE_SUMMARY;
            };
        }
        if (isTownhall) {
            return switch (index) {
                case 0 -> PanelType.VILLAGE_SUMMARY;
                case 1 -> PanelType.RESOURCES;
                case 2 -> PanelType.WALLS;
                case 3 -> PanelType.VILLAGE_SUMMARY;
                case 4 -> {
                    if (isPlayerControlled) {
                        yield PanelType.CONTROLLED_PROJECTS;
                    }
                    yield PanelType.PROJECTS;
                }
                case 5 -> PanelType.CONSTRUCTIONS;
                case 6 -> PanelType.POPULATION;
                case 7 -> PanelType.VILLAGE_MAP;
                case 8 -> {
                    if (isPlayerControlled) {
                        yield PanelType.CONTROLLED_MILITARY;
                    }
                    yield PanelType.MILITARY;
                }
                default -> PanelType.VILLAGE_SUMMARY;
            };
        }
        if (planSet != null) {
            if (planSet.hasVisitors()) {
                return planSet.isMarket() ? PanelType.MARKET_MERCHANTS : PanelType.VISITORS;
            }
            if (planSet.isInn()) {
                return index == 0 ? PanelType.INN_VISITORS : PanelType.INN_TRADE_GOODS;
            }
            if (planSet.isArchives()) {
                return PanelType.ARCHIVES;
            }
            if (planSet.isBorderPost()) {
                return PanelType.VILLAGE_SUMMARY;
            }
        }
        if (!(planSet == null || planSet.maleResidents().isEmpty() && planSet.femaleResidents().isEmpty())) {
            return PanelType.BUILDING_DEFAULT;
        }
        return PanelType.BUILDING_DEFAULT;
    }

    private static void fillHoFSigns(ServerLevel level, Village village, BuildingInstance building) {
        List<SpecialPoint> signPoints = building.getPointsByType("signPos");
        if (signPoints.isEmpty()) {
            LOGGER.debug("[Millenaire] No signPos found for HoF in {}", (Object)building.getPlanId().getPath());
            return;
        }
        signPoints = new ArrayList<SpecialPoint>(signPoints);
        signPoints.sort((a, b) -> {
            int cz = Integer.compare(b.pos().getZ(), a.pos().getZ());
            if (cz != 0) {
                return cz;
            }
            int cx = Integer.compare(a.pos().getX(), b.pos().getX());
            if (cx != 0) {
                return cx;
            }
            return Integer.compare(b.pos().getY(), a.pos().getY());
        });
        List<String> hofData = HallOfFameLoader.getHoFData();
        LOGGER.info("[HoF] {} signPos found, {} HoF entries", (Object)signPoints.size(), (Object)hofData.size());
        for (int dbg = 0; dbg < Math.min(10, signPoints.size()); ++dbg) {
            BlockPos p = signPoints.get(dbg).pos();
            String entry = dbg < hofData.size() ? hofData.get(dbg) : "N/A";
            LOGGER.info("[HoF] sign[{}] pos=({},{},{}) -> {}", new Object[]{dbg, p.getX(), p.getY(), p.getZ(), entry.length() > 40 ? entry.substring(0, 40) : entry});
        }
        int signNb = 0;
        for (SpecialPoint sp : signPoints) {
            BlockPos pos = sp.pos();
            Direction facing = PanelPlacer.resolveFacing(level, sp, pos, village.getCenter());
            BlockState panelState = (BlockState)((VillagePanelBlock)((Object)ModBlocks.VILLAGE_PANEL.get())).defaultBlockState().setValue(VillagePanelBlock.FACING, (Comparable)facing);
            level.setBlock(pos, panelState, 3);
            BlockEntity be = level.getBlockEntity(pos);
            if (!(be instanceof VillagePanelBlockEntity)) continue;
            VillagePanelBlockEntity panelBe = (VillagePanelBlockEntity)be;
            panelBe.setBuildingId(building.getId());
            panelBe.setPanelType(PanelType.HALL_OF_FAME);
            if (signNb < hofData.size()) {
                List<PanelContentGenerator.DisplayLine> lines = PanelPlacer.buildHoFDisplayLines(hofData.get(signNb));
                panelBe.updateDisplayLines(lines);
            }
            level.sendBlockUpdated(pos, panelState, panelState, 3);
            ++signNb;
        }
        LOGGER.debug("[Millenaire] HoF: {} panels filled out of {} signPos for {}", new Object[]{Math.min(signNb, hofData.size()), signPoints.size(), building.getPlanId().getPath()});
    }

    static List<PanelContentGenerator.DisplayLine> buildHoFDisplayLines(String entry) {
        String[] parts = entry.split(";", -1);
        ArrayList<PanelContentGenerator.DisplayLine> lines = new ArrayList<PanelContentGenerator.DisplayLine>();
        boolean twoPatrons = PanelPlacer.isTwoPatronEntry(parts);
        for (int i = 0; i < Math.min(4, parts.length); ++i) {
            String text;
            if (twoPatrons && i == 2) {
                lines.add(PanelContentGenerator.DisplayLine.centered(""));
            }
            if ((text = parts[i]).isEmpty() || !text.startsWith("hof.")) {
                lines.add(PanelContentGenerator.DisplayLine.centered(text));
                continue;
            }
            lines.add(PanelContentGenerator.DisplayLine.centeredTranslatable(text));
        }
        return lines;
    }

    private static boolean isTwoPatronEntry(String[] parts) {
        return parts.length == 4 && PanelPlacer.isName(parts[0]) && PanelPlacer.isKey(parts[1]) && PanelPlacer.isName(parts[2]) && PanelPlacer.isKey(parts[3]);
    }

    private static boolean isName(String field) {
        return !field.isEmpty() && !field.startsWith("hof.");
    }

    private static boolean isKey(String field) {
        return field.startsWith("hof.");
    }

    public static void recreatePanel(ServerLevel level, Village village, BuildingInstance building, BlockPos pos) {
        BuildingPlanSet planSet = building.getPlanSetId() != null ? ModCultures.getBuildingPlanSet(building.getPlanSetId()) : null;
        List<SpecialPoint> signPoints = building.getPointsByType("signPos");
        int index = 0;
        SpecialPoint matchedSp = null;
        for (int i = 0; i < signPoints.size(); ++i) {
            if (!signPoints.get(i).pos().equals((Object)pos)) continue;
            index = i;
            matchedSp = signPoints.get(i);
            break;
        }
        int[] signOrder = PanelPlacer.resolveSignOrder(planSet, building);
        int logicalIndex = PanelPlacer.applySignOrder(signOrder, index);
        VillageType villageType = ModCultures.getVillageType(village.getVillageTypeId());
        boolean isMarvel = villageType != null && villageType.isMarvel();
        PanelType panelType = PanelPlacer.determinePanelType(logicalIndex, planSet, building, isMarvel);
        Direction facing = PanelPlacer.resolveFacing(level, matchedSp, pos, village.getCenter());
        BlockState panelState = (BlockState)((VillagePanelBlock)((Object)ModBlocks.VILLAGE_PANEL.get())).defaultBlockState().setValue(VillagePanelBlock.FACING, (Comparable)facing);
        level.setBlock(pos, panelState, 3);
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof VillagePanelBlockEntity) {
            VillagePanelBlockEntity panelBe = (VillagePanelBlockEntity)be;
            panelBe.setBuildingId(building.getId());
            panelBe.setPanelType(panelType);
            panelBe.setVillageId(village.getId().uuid());
            panelBe.setCultureId(village.getCultureId());
            panelBe.setSignIndex(logicalIndex);
            PanelContentGenerator.DisplayData displayData = PanelContentGenerator.generateDisplayLines(panelType, village, building, level, logicalIndex);
            panelBe.updateDisplayLines(displayData.displayLines());
            level.sendBlockUpdated(pos, panelState, panelState, 3);
        }
        LOGGER.debug("[Millenaire] Panel recreated at {} for {}", (Object)pos, (Object)building.getPlanId().getPath());
    }

    private static Direction resolveFacing(ServerLevel level, @Nullable SpecialPoint sp, BlockPos panelPos, BlockPos villageCenter) {
        Direction fixed;
        if (sp != null && sp.orientation() != null && !"guess".equals(sp.orientation()) && (fixed = PanelPlacer.parseDirection(sp.orientation())) != null) {
            return fixed;
        }
        BlockPos pos = sp != null ? sp.pos() : panelPos;
        Direction detected = PanelPlacer.detectWallFacing(level, pos);
        if (detected != null) {
            return detected;
        }
        return PanelPlacer.computeFacing(villageCenter, pos);
    }

    @Nullable
    private static Direction parseDirection(String name) {
        return switch (name.toLowerCase(Locale.ROOT)) {
            case "north" -> Direction.NORTH;
            case "south" -> Direction.SOUTH;
            case "east" -> Direction.EAST;
            case "west" -> Direction.WEST;
            default -> null;
        };
    }

    @Nullable
    private static Direction detectWallFacing(ServerLevel level, BlockPos panelPos) {
        Direction[] priority;
        for (Direction dir : priority = new Direction[]{Direction.EAST, Direction.WEST, Direction.SOUTH, Direction.NORTH}) {
            BlockPos adjacent = panelPos.relative(dir);
            if (!level.getBlockState(adjacent).isSolidRender((BlockGetter)level, adjacent)) continue;
            return dir.getOpposite();
        }
        return null;
    }

    @Nullable
    static int[] resolveSignOrder(@Nullable BuildingPlanSet planSet, BuildingInstance building) {
        if (planSet == null) {
            return null;
        }
        String variant = building.getVariant();
        if (variant == null) {
            return null;
        }
        for (int lvl = building.getLevel(); lvl >= 0; --lvl) {
            BuildingPlanSet.LevelDef levelDef = planSet.getLevel(variant, lvl);
            if (levelDef == null || levelDef.signOrder() == null) continue;
            return levelDef.signOrder();
        }
        return null;
    }

    static int applySignOrder(@Nullable int[] signOrder, int physicalIndex) {
        return signOrder != null && physicalIndex < signOrder.length ? signOrder[physicalIndex] : physicalIndex;
    }

    private static Direction computeFacing(BlockPos from, BlockPos panelPos) {
        int dx = from.getX() - panelPos.getX();
        int dz = from.getZ() - panelPos.getZ();
        if (Math.abs(dx) > Math.abs(dz)) {
            return dx > 0 ? Direction.EAST : Direction.WEST;
        }
        return dz > 0 ? Direction.SOUTH : Direction.NORTH;
    }
}

