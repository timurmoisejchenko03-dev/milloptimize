/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  javax.annotation.Nullable
 *  net.minecraft.core.HolderLookup
 *  net.minecraft.core.registries.Registries
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.nbt.NbtAccounter
 *  net.minecraft.nbt.NbtIo
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate
 *  org.slf4j.Logger
 */
package org.millenaire.building;

import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.millenaire.building.BuildingExporter;
import org.millenaire.building.BuildingPlan;
import org.millenaire.building.BuildingPlanSet;
import org.millenaire.building.HearthTemplateSanitizer;
import org.millenaire.content.ChainedContentFs;
import org.millenaire.content.ContentFs;
import org.millenaire.content.CustomContentIndex;
import org.millenaire.content.Resource;
import org.millenaire.content.SourceKind;
import org.millenaire.culture.ModCultures;
import org.slf4j.Logger;

public final class TemplateLoader {
    private static final Logger LOGGER = LogUtils.getLogger();

    private TemplateLoader() {
    }

    public static Optional<StructureTemplate> load(BuildingPlan plan, ServerLevel level, ContentFs cultureFs) {
        if (plan == null) {
            throw new IllegalArgumentException("plan null");
        }
        return TemplateLoader.loadFromPath(plan.nbtPath(), level, cultureFs);
    }

    public static Optional<StructureTemplate> resolve(ServerLevel level, String cultureKey, String buildingId, String variant, int upgradeLevel) {
        if (level == null) {
            throw new IllegalArgumentException("level null");
        }
        BuildingPlan plan = TemplateLoader.resolvePlan(cultureKey, buildingId, variant, upgradeLevel);
        if (plan != null) {
            ContentFs cultureFs = TemplateLoader.cultureFsFor(plan.culture());
            return TemplateLoader.load(plan, level, cultureFs);
        }
        if (cultureKey == null || cultureKey.isEmpty()) {
            return TemplateLoader.resolveFromExportsFallback(level, buildingId, variant, upgradeLevel);
        }
        return Optional.empty();
    }

    private static Optional<StructureTemplate> resolveFromExportsFallback(ServerLevel level, String buildingId, String variant, int upgradeLevel) {
        Optional<Path> path = BuildingExporter.findExportedNbt(level, buildingId, variant, upgradeLevel);
        if (path.isEmpty()) {
            return Optional.empty();
        }
        return TemplateLoader.resolveFromExportsPath(path.get(), (HolderLookup<Block>)level.holderLookup(Registries.BLOCK), buildingId, variant, upgradeLevel);
    }

    static Optional<StructureTemplate> resolveFromExportsPath(Path nbtFile, HolderLookup<Block> blocks, String buildingId, String variant, int upgradeLevel) {
        Optional<StructureTemplate> optional;
        block8: {
            InputStream is = Files.newInputStream(nbtFile, new OpenOption[0]);
            try {
                CompoundTag nbt = NbtIo.readCompressed((InputStream)is, (NbtAccounter)NbtAccounter.unlimitedHeap());
                HearthTemplateSanitizer.sanitize(nbt, null);
                StructureTemplate tmpl = new StructureTemplate();
                tmpl.load(blocks, nbt);
                optional = Optional.of(tmpl);
                if (is == null) break block8;
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
                    LOGGER.warn("HIGH#3 exports/ fallback failed for {}_{}_{}: {}", new Object[]{buildingId, variant, upgradeLevel, e.getMessage()});
                    return Optional.empty();
                }
            }
            is.close();
        }
        return optional;
    }

    @Nullable
    public static BuildingPlan resolvePlan(String cultureKey, String buildingId, String variant, int upgradeLevel) {
        if (cultureKey == null || cultureKey.isEmpty()) {
            return null;
        }
        ResourceLocation cultureRL = ResourceLocation.parse((String)cultureKey);
        ResourceLocation planSetId = ResourceLocation.fromNamespaceAndPath((String)cultureRL.getNamespace(), (String)(cultureRL.getPath() + "/" + buildingId));
        BuildingPlanSet planSet = ModCultures.getBuildingPlanSet(planSetId);
        if (planSet == null) {
            return null;
        }
        BuildingPlanSet.LevelDef levelDef = planSet.getLevel(variant, upgradeLevel);
        if (levelDef == null) {
            return null;
        }
        return ModCultures.getBuildingPlan(levelDef.planId());
    }

    public static int resolveGroundLevel(String cultureKey, String buildingId, String variant, int upgradeLevel, int fallback) {
        BuildingPlan plan = TemplateLoader.resolvePlan(cultureKey, buildingId, variant, upgradeLevel);
        return plan != null ? plan.groundLevel() : fallback;
    }

    public static ContentFs cultureFsFor(ResourceLocation culture) {
        if (culture == null) {
            throw new IllegalArgumentException("culture null");
        }
        return CustomContentIndex.current().forCulture(culture.getPath());
    }

    public static ContentFs cultureFsFor(BuildingPlan plan) {
        if (plan == null) {
            throw new IllegalArgumentException("plan null");
        }
        return TemplateLoader.cultureFsFor(plan.culture());
    }

    public static ContentFs cultureFsForImport(ResourceLocation culture) {
        if (culture == null) {
            throw new IllegalArgumentException("culture null");
        }
        ContentFs culturePrimary = CustomContentIndex.current().forCulture(culture.getPath());
        ContentFs exportedFallback = CustomContentIndex.current().exportedFs().sub("cultures/" + culture.getPath());
        return TemplateLoader.chainForImport(culturePrimary, exportedFallback);
    }

    public static ContentFs chainForImport(ContentFs culturePrimary, ContentFs exportedOverlay) {
        return new ChainedContentFs(exportedOverlay, culturePrimary);
    }

    public static Optional<StructureTemplate> loadFromPath(String nbtPath, ServerLevel level, ContentFs cultureFs) {
        if (level == null) {
            throw new IllegalArgumentException("level null");
        }
        return TemplateLoader.loadFromPath(nbtPath, (HolderLookup<Block>)level.holderLookup(Registries.BLOCK), cultureFs);
    }

    static Optional<StructureTemplate> loadFromPath(String nbtPath, HolderLookup<Block> blocks, ContentFs cultureFs) {
        if (nbtPath == null) {
            throw new IllegalArgumentException("nbtPath null");
        }
        if (blocks == null) {
            throw new IllegalArgumentException("blocks null");
        }
        if (cultureFs == null) {
            throw new IllegalArgumentException("cultureFs null");
        }
        Optional<Resource> nbt = cultureFs.findFirst(nbtPath + ".nbt");
        if (nbt.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(TemplateLoader.loadStructureFromResource(nbt.get(), blocks));
    }

    public static StructureTemplate loadStructureFromResource(Resource res, ServerLevel level) {
        if (level == null) {
            throw new IllegalArgumentException("level null");
        }
        return TemplateLoader.loadStructureFromResource(res, (HolderLookup<Block>)level.holderLookup(Registries.BLOCK));
    }

    static StructureTemplate loadStructureFromResource(Resource res, HolderLookup<Block> blocks) {
        StructureTemplate structureTemplate;
        block10: {
            if (res == null) {
                throw new IllegalArgumentException("res null");
            }
            if (blocks == null) {
                throw new IllegalArgumentException("blocks null");
            }
            InputStream is = res.open();
            try {
                CompoundTag nbt = NbtIo.readCompressed((InputStream)is, (NbtAccounter)TemplateLoader.accounterFor(res.kind()));
                HearthTemplateSanitizer.sanitize(nbt, null);
                StructureTemplate tmpl = new StructureTemplate();
                tmpl.load(blocks, nbt);
                structureTemplate = tmpl;
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
                    throw new UncheckedIOException("Failed to load NBT " + res.relPath() + " from " + res.source().displayName(), e);
                }
            }
            is.close();
        }
        return structureTemplate;
    }

    public static NbtAccounter accounterFor(SourceKind kind) {
        return switch (kind) {
            default -> throw new MatchException(null, null);
            case SourceKind.CLASSPATH, SourceKind.STANDARD -> NbtAccounter.unlimitedHeap();
            case SourceKind.SUBMOD -> NbtAccounter.create((long)10000000L);
        };
    }
}

