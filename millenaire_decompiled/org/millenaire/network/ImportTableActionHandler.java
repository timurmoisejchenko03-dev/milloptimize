/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Vec3i
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.nbt.NbtAccounter
 *  net.minecraft.nbt.NbtIo
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.protocol.common.custom.CustomPacketPayload
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate
 *  net.neoforged.neoforge.network.PacketDistributor
 *  net.neoforged.neoforge.network.handling.IPayloadContext
 *  org.slf4j.Logger
 */
package org.millenaire.network;

import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntUnaryOperator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.millenaire.block.ImportTableBlockEntity;
import org.millenaire.building.BuildingCostCalculator;
import org.millenaire.building.BuildingExporter;
import org.millenaire.building.BuildingImporter;
import org.millenaire.building.BuildingPlan;
import org.millenaire.building.BuildingPlanSet;
import org.millenaire.building.ImportTableIdentifierValidator;
import org.millenaire.building.TemplateLoader;
import org.millenaire.culture.ModCultures;
import org.millenaire.item.ImportTableItem;
import org.millenaire.network.ImportTableActionPayload;
import org.millenaire.network.ImportTableCostsPayload;
import org.millenaire.network.PayloadRateLimiter;
import org.slf4j.Logger;

public final class ImportTableActionHandler {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final PayloadRateLimiter RATE_LIMITER = new PayloadRateLimiter(200L);

    private ImportTableActionHandler() {
    }

    public static void handleAction(ImportTableActionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            boolean insidePlot;
            Player patt0$temp = context.player();
            if (!(patt0$temp instanceof ServerPlayer)) {
                return;
            }
            ServerPlayer player = (ServerPlayer)patt0$temp;
            Level patt1$temp = player.level();
            if (!(patt1$temp instanceof ServerLevel)) {
                return;
            }
            ServerLevel serverLevel = (ServerLevel)patt1$temp;
            if (!serverLevel.getServer().isSingleplayerOwner(player.getGameProfile()) && !player.hasPermissions(2)) {
                player.sendSystemMessage((Component)Component.literal((String)"\u00a7c[ImportTable] Requires operator permissions on a server"));
                return;
            }
            if (!RATE_LIMITER.acquire(player.getUUID())) {
                LOGGER.debug("ImportTable action rate-limited for {}", (Object)player.getName().getString());
                return;
            }
            BlockPos pos = payload.blockPos();
            BlockEntity be = serverLevel.getBlockEntity(pos);
            if (!(be instanceof ImportTableBlockEntity)) {
                LOGGER.warn("ImportTable action received for non-ImportTable block at {}", (Object)pos);
                return;
            }
            ImportTableBlockEntity importTable = (ImportTableBlockEntity)be;
            boolean nearBlock = player.blockPosition().distSqr((Vec3i)pos) <= 64.0;
            boolean bl = insidePlot = importTable.hasPlan() && ImportTableItem.isInsidePlot(player.blockPosition(), importTable);
            if (!nearBlock && !insidePlot) {
                player.sendSystemMessage((Component)Component.literal((String)"\u00a7c[ImportTable] Too far from the table"));
                return;
            }
            CompoundTag data = payload.actionData();
            if (payload.action() == null) {
                LOGGER.warn("Received ImportTable action with invalid ordinal from player {} at {}", (Object)player.getName().getString(), (Object)pos);
                return;
            }
            switch (payload.action()) {
                case CREATE_NEW: {
                    ImportTableActionHandler.handleCreateNew(serverLevel, importTable, player, data);
                    break;
                }
                case IMPORT_LEVEL: {
                    ImportTableActionHandler.handleImportLevel(serverLevel, importTable, player, data);
                    break;
                }
                case IMPORT_ALL: {
                    ImportTableActionHandler.handleImportAll(serverLevel, importTable, player, data);
                    break;
                }
                case IMPORT_LEVEL_EXPORT: {
                    ImportTableActionHandler.handleImportLevelExport(serverLevel, importTable, player, data);
                    break;
                }
                case IMPORT_ALL_EXPORT: {
                    ImportTableActionHandler.handleImportAllExport(serverLevel, importTable, player, data);
                    break;
                }
                case REIMPORT: {
                    BuildingImporter.reimport(serverLevel, importTable, player);
                    BuildingImporter.announceFromExports(player, importTable.getCultureKey(), importTable.getBuildingId());
                    break;
                }
                case REIMPORT_ALL: {
                    BuildingImporter.reimportAll(serverLevel, importTable, player);
                    break;
                }
                case EXPORT: {
                    BuildingExporter.exportLevel(serverLevel, importTable, player);
                    break;
                }
                case EXPORT_NEW_LEVEL: {
                    BuildingExporter.exportNewLevel(serverLevel, importTable, player);
                    break;
                }
                case UPDATE_SETTINGS: {
                    ImportTableActionHandler.handleUpdateSettings(serverLevel, importTable, data);
                    break;
                }
                case SHOW_COSTS: {
                    ImportTableActionHandler.handleShowCosts(serverLevel, importTable, player);
                }
            }
        });
    }

    private static void handleShowCosts(ServerLevel level, ImportTableBlockEntity be, ServerPlayer player) {
        if (!be.hasPlan()) {
            player.sendSystemMessage((Component)Component.literal((String)"\u00a7c[ImportTable] No plan loaded on this table"));
            return;
        }
        String buildingId = be.getBuildingId();
        String variant = be.getVariant();
        int upgradeLevel = be.getUpgradeLevel();
        CompoundTag templateNbt = ImportTableActionHandler.loadTemplateNbt(level, be);
        if (templateNbt == null) {
            player.sendSystemMessage((Component)Component.literal((String)("\u00a7c[ImportTable] Could not load template NBT for " + buildingId + " " + variant + " level " + upgradeLevel)));
            return;
        }
        Map<ResourceLocation, Integer> cost = BuildingCostCalculator.computeCost(templateNbt);
        ArrayList<ImportTableCostsPayload.Entry> entries = new ArrayList<ImportTableCostsPayload.Entry>(cost.size());
        cost.forEach((id, qty) -> entries.add(new ImportTableCostsPayload.Entry(id.toString(), (int)qty)));
        entries.sort(Comparator.comparing(ImportTableCostsPayload.Entry::itemId));
        ImportTableCostsPayload payload = new ImportTableCostsPayload(be.getBlockPos(), buildingId, variant, upgradeLevel, entries);
        PacketDistributor.sendToPlayer((ServerPlayer)player, (CustomPacketPayload)payload, (CustomPacketPayload[])new CustomPacketPayload[0]);
    }

    private static CompoundTag loadTemplateNbt(ServerLevel level, ImportTableBlockEntity be) {
        CompoundTag compoundTag;
        block10: {
            Optional<StructureTemplate> opt;
            BuildingPlan plan;
            BuildingPlanSet.LevelDef levelDef;
            BuildingPlanSet planSet;
            ResourceLocation planSetId;
            String cultureKey = be.getCultureKey();
            String buildingId = be.getBuildingId();
            String variant = be.getVariant();
            int upgradeLevel = be.getUpgradeLevel();
            if (!cultureKey.isEmpty() && !cultureKey.startsWith("millenaire-custom") && (planSetId = ResourceLocation.tryParse((String)(cultureKey + "/" + buildingId))) != null && (planSet = ModCultures.getBuildingPlanSet(planSetId)) != null && (levelDef = planSet.getLevel(variant, upgradeLevel)) != null && (plan = ModCultures.getBuildingPlan(levelDef.planId())) != null && (opt = TemplateLoader.load(plan, level, TemplateLoader.cultureFsForImport(plan.culture()))).isPresent()) {
                return opt.get().save(new CompoundTag());
            }
            Optional<Path> nbtPathOpt = BuildingExporter.findExportedNbt(level, buildingId, variant, upgradeLevel);
            if (nbtPathOpt.isEmpty()) {
                return null;
            }
            Path nbtPath = nbtPathOpt.get();
            InputStream is = Files.newInputStream(nbtPath, new OpenOption[0]);
            try {
                compoundTag = NbtIo.readCompressed((InputStream)is, (NbtAccounter)NbtAccounter.unlimitedHeap());
                if (is == null) break block10;
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
                catch (IOException e) {
                    LOGGER.warn("Failed to load NBT for costs: {}", (Object)nbtPath, (Object)e);
                    return null;
                }
            }
            is.close();
        }
        return compoundTag;
    }

    private static void handleCreateNew(ServerLevel level, ImportTableBlockEntity be, ServerPlayer player, CompoundTag data) {
        int length = data.getInt("length");
        int width = data.getInt("width");
        int startingLevel = data.getInt("startingLevel");
        int height = data.getInt("height");
        boolean clearGround = data.getBoolean("clearGround");
        int numberOfUpgrades = data.getInt("numberOfUpgrades");
        boolean preBuilt = data.getBoolean("preBuilt");
        String buildingName = data.getString("buildingName");
        length = Math.max(1, Math.min(length, 256));
        width = Math.max(1, Math.min(width, 256));
        height = Math.max(1, Math.min(height, 256));
        numberOfUpgrades = Math.max(0, Math.min(numberOfUpgrades, 16));
        BuildingImporter.createNewBuilding(level, be, player, length, width, startingLevel, height, clearGround, numberOfUpgrades, preBuilt, buildingName);
    }

    private static void handleImportLevel(ServerLevel level, ImportTableBlockEntity be, ServerPlayer player, CompoundTag data) {
        String parentVariant;
        String cultureKey = data.getString("cultureKey");
        String buildingId = data.getString("buildingId");
        String variant = data.getString("variant");
        int upgradeLevel = data.getInt("level");
        String parentBuildingId = data.getString("parentBuildingId");
        if (!ImportTableActionHandler.validateImportInputs(player, cultureKey, buildingId, variant, parentBuildingId, parentVariant = data.getString("parentVariant"))) {
            return;
        }
        BuildingImporter.importLevelFromCulture(level, be, player, cultureKey, buildingId, variant, upgradeLevel, parentBuildingId, parentVariant);
        BuildingImporter.announceFromExports(player, cultureKey, buildingId);
    }

    private static void handleImportAll(ServerLevel level, ImportTableBlockEntity be, ServerPlayer player, CompoundTag data) {
        String variant;
        String buildingId;
        String cultureKey = data.getString("cultureKey");
        if (!ImportTableActionHandler.validateImportInputs(player, cultureKey, buildingId = data.getString("buildingId"), variant = data.getString("variant"), "", "")) {
            return;
        }
        if (BuildingImporter.readIsSubBuildingFlag(cultureKey, buildingId)) {
            player.sendSystemMessage((Component)Component.literal((String)("\u00a7c[ImportTable] '" + buildingId + "' is a sub-building \u2014 import it under its parent, not via Import All")));
            return;
        }
        BuildingImporter.importAllFromCulture(level, be, player, cultureKey, buildingId, variant);
    }

    private static void handleImportLevelExport(ServerLevel level, ImportTableBlockEntity be, ServerPlayer player, CompoundTag data) {
        String buildingId = data.getString("buildingId");
        String variant = data.getString("variant");
        int upgradeLevel = data.getInt("level");
        if (!ImportTableActionHandler.validateImportInputs(player, "", buildingId, variant, "", "")) {
            return;
        }
        BuildingImporter.importLevelFromExports(level, be, player, buildingId, variant, upgradeLevel);
    }

    private static void handleImportAllExport(ServerLevel level, ImportTableBlockEntity be, ServerPlayer player, CompoundTag data) {
        String variant;
        String buildingId = data.getString("buildingId");
        if (!ImportTableActionHandler.validateImportInputs(player, "", buildingId, variant = data.getString("variant"), "", "")) {
            return;
        }
        BuildingImporter.importAllFromExports(level, be, player, buildingId, variant);
    }

    private static boolean validateImportInputs(ServerPlayer player, String cultureKey, String buildingId, String variant, String parentBuildingId, String parentVariant) {
        if (!ImportTableIdentifierValidator.isValidCultureKey(cultureKey)) {
            return ImportTableActionHandler.reject(player, "cultureKey", cultureKey);
        }
        if (!ImportTableIdentifierValidator.isValidIdentifier(buildingId)) {
            return ImportTableActionHandler.reject(player, "buildingId", buildingId);
        }
        if (!ImportTableIdentifierValidator.isValidIdentifier(variant)) {
            return ImportTableActionHandler.reject(player, "variant", variant);
        }
        if (!ImportTableIdentifierValidator.isValidOptionalIdentifier(parentBuildingId)) {
            return ImportTableActionHandler.reject(player, "parentBuildingId", parentBuildingId);
        }
        if (!ImportTableIdentifierValidator.isValidOptionalIdentifier(parentVariant)) {
            return ImportTableActionHandler.reject(player, "parentVariant", parentVariant);
        }
        return true;
    }

    private static boolean reject(ServerPlayer player, String field, String value) {
        LOGGER.warn("ImportTable: rejected {} from {} (value length={}): refusing to act on a path-unsafe identifier", new Object[]{field, player.getName().getString(), value == null ? -1 : value.length()});
        player.sendSystemMessage((Component)Component.literal((String)("\u00a7c[ImportTable] Invalid " + field)));
        return false;
    }

    private static void handleUpdateSettings(ServerLevel level, ImportTableBlockEntity be, CompoundTag data) {
        int newOrient;
        boolean orientationChanged = false;
        boolean anyChange = false;
        if (data.contains("orientation") && (newOrient = data.getInt("orientation") & 3) != be.getOrientation()) {
            be.setOrientation(newOrient);
            orientationChanged = true;
            anyChange = true;
        }
        anyChange |= ImportTableActionHandler.updateInt(data, "startingLevel", be.getStartingLevel(), be::setStartingLevel, v -> v);
        anyChange |= ImportTableActionHandler.updateInt(data, "height", be.getHeight(), be::setHeight, v -> Math.max(1, v));
        anyChange |= ImportTableActionHandler.updateBool(data, "exportSnow", be.isExportSnow(), be::setExportSnow);
        anyChange |= ImportTableActionHandler.updateBool(data, "importMockBlocks", be.isImportMockBlocks(), be::setImportMockBlocks);
        if ((anyChange |= ImportTableActionHandler.updateBool(data, "convertToPreserveGround", be.isConvertToPreserveGround(), be::setConvertToPreserveGround)) && be.hasPlan()) {
            be.setDirty(true);
        }
        if (orientationChanged && be.hasPlan()) {
            ImportTableActionHandler.propagateOrientationAndRepaint(level, be);
        }
    }

    private static boolean updateInt(CompoundTag data, String key, int current, IntConsumer setter, IntUnaryOperator transform) {
        if (!data.contains(key)) {
            return false;
        }
        int newValue = transform.applyAsInt(data.getInt(key));
        if (newValue == current) {
            return false;
        }
        setter.accept(newValue);
        return true;
    }

    private static boolean updateBool(CompoundTag data, String key, boolean current, Consumer<Boolean> setter) {
        if (!data.contains(key)) {
            return false;
        }
        boolean newValue = data.getBoolean(key);
        if (newValue == current) {
            return false;
        }
        setter.accept(newValue);
        return true;
    }

    private static void propagateOrientationAndRepaint(ServerLevel level, ImportTableBlockEntity be) {
        int newOrient = be.getOrientation();
        String buildingId = be.getBuildingId();
        String variant = be.getVariant();
        ImportTableBlockEntity main = be.resolveMainTable(level);
        ArrayList<ImportTableBlockEntity> all = new ArrayList<ImportTableBlockEntity>();
        all.add(main);
        all.addAll(main.findChildTables(level));
        for (ImportTableBlockEntity table : all) {
            if (!buildingId.equals(table.getBuildingId()) || !variant.equals(table.getVariant())) continue;
            if (table != be) {
                table.setOrientation(newOrient);
                if (table.hasPlan()) {
                    table.setDirty(true);
                }
            }
            if (!table.hasPlan()) continue;
            BuildingImporter.placeConstructionBorder(level, table);
        }
    }
}

