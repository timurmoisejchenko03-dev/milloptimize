/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  net.minecraft.resources.ResourceLocation
 *  org.slf4j.Logger
 */
package org.millenaire.content;

import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import org.millenaire.content.ContentDirectoryManager;
import org.millenaire.goal.GoalRegistry;
import org.millenaire.goal.gathering.GatheringHandlerRegistry;
import org.slf4j.Logger;

public final class ReferenceCatalogGenerator {
    private static final Logger LOGGER = LogUtils.getLogger();

    private ReferenceCatalogGenerator() {
    }

    public static void generate(GoalRegistry goalRegistry) {
        if (!ContentDirectoryManager.isInitialized()) {
            return;
        }
        try {
            Path dir = ContentDirectoryManager.getStandardDir().resolve("_reference");
            Files.createDirectories(dir, new FileAttribute[0]);
            ReferenceCatalogGenerator.writeGoals(dir, goalRegistry);
            ReferenceCatalogGenerator.writeGatheringHandlers(dir);
            ReferenceCatalogGenerator.writeSpecialPoints(dir);
            ReferenceCatalogGenerator.writeIndex(dir);
            LOGGER.info("Reference catalogs written to {}", (Object)dir);
        }
        catch (Exception e) {
            LOGGER.warn("Could not generate reference catalogs: {}", (Object)e.getMessage(), (Object)e);
        }
    }

    private static void writeGoals(Path dir, GoalRegistry goalRegistry) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("# Registered goals\n");
        sb.append("# Use these ids in villager_type JSONs under the \"goals\" field.\n");
        sb.append("# Generated automatically \u2014 do not edit.\n\n");
        List<ResourceLocation> ids = goalRegistry.getAllIds();
        for (ResourceLocation id : ids) {
            sb.append(id).append('\n');
        }
        sb.append("\n# Total: ").append(ids.size()).append(" goals.\n");
        Files.writeString(dir.resolve("goals.txt"), (CharSequence)sb.toString(), StandardCharsets.UTF_8, new OpenOption[0]);
    }

    private static void writeGatheringHandlers(Path dir) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("# Registered gathering handlers\n");
        sb.append("# Use these ids in gathering_type/<id>.json under the \"handler\" field.\n");
        sb.append("# Generated automatically \u2014 do not edit.\n\n");
        List<String> ids = GatheringHandlerRegistry.getAllIds();
        for (String id : ids) {
            sb.append(id).append('\n');
        }
        sb.append("\n# Total: ").append(ids.size()).append(" handlers.\n");
        Files.writeString(dir.resolve("gathering_handlers.txt"), (CharSequence)sb.toString(), StandardCharsets.UTF_8, new OpenOption[0]);
    }

    private static void writeSpecialPoints(Path dir) throws IOException {
        String content = "# Special-point mock blocks\n# Place these blocks inside an NBT template to mark semantic positions.\n# The mod extracts them at load time (MockBlockExtractor) and processes\n# them at placement time (MockBlockProcessor). See docs/systems/ for\n# per-type processing semantics.\n#\n# Generated automatically \u2014 do not edit.\n\nmillenaire:mock_marker          Generic marker (spawn point, waypoint, etc.)\nmillenaire:mock_facing_marker   Directional marker (door facing, etc.)\nmillenaire:mock_chest           Chest spawn location\nmillenaire:mock_soil            Farmable soil zone\nmillenaire:mock_source          Resource source marker (water source, etc.)\nmillenaire:mock_free            Free space (kept empty during construction)\nmillenaire:mock_tree_spawn      Tree planting location\nmillenaire:mock_animal_spawn    Animal spawn zone\nmillenaire:mock_decor           Decorative item placement\n\n# Total: 9 mock block types.\n";
        Files.writeString(dir.resolve("special_points.txt"), (CharSequence)content, StandardCharsets.UTF_8, new OpenOption[0]);
    }

    private static void writeIndex(Path dir) throws IOException {
        String content = "Mill\u00e9naire custom content \u2014 reference catalogs\n==============================================\n\nThis directory is regenerated on every server start. Do not edit\nthese files \u2014 edits are overwritten. Use them as read-only\nreferences when authoring custom content.\n\nFiles:\n  goals.txt                All registered goal ids (for villager_type.goals)\n  gathering_handlers.txt   All registered gathering handler ids\n                           (for gathering_type/<id>.json \"handler\" field)\n  special_points.txt       The 9 mock block types usable inside NBT templates\n\nNot yet generated (pending):\n  tags.txt                 Building + villager tags. Tags are crowd-sourced\n                           from existing JSON files \u2014 inspect\n                           cultures/norman/ for real examples.\n\nFor JSON schema references, inspect the deployed built-in cultures\n(cultures/norman/ is the most complete). See docs/feat/custom-content.md\nfor authoring workflows.\n";
        Files.writeString(dir.resolve("README.txt"), (CharSequence)content, StandardCharsets.UTF_8, new OpenOption[0]);
    }
}

