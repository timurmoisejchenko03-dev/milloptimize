/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  net.minecraft.core.HolderLookup$Provider
 *  net.minecraft.core.registries.BuiltInRegistries
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.nbt.ListTag
 *  net.minecraft.nbt.Tag
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.level.block.Block
 *  org.slf4j.Logger
 */
package org.millenaire.building;

import com.mojang.logging.LogUtils;
import java.util.function.Predicate;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import org.millenaire.tag.ModTags;
import org.slf4j.Logger;

public final class HearthTemplateSanitizer {
    private static final Logger LOGGER = LogUtils.getLogger();

    private HearthTemplateSanitizer() {
    }

    public static boolean sanitize(CompoundTag templateNbt, HolderLookup.Provider lookup) {
        return HearthTemplateSanitizer.sanitizeWithPredicate(templateNbt, HearthTemplateSanitizer::isTaggedHearthBlock);
    }

    static boolean sanitizeWithPredicate(CompoundTag templateNbt, Predicate<Block> isHearth) {
        if (templateNbt == null) {
            return false;
        }
        boolean anyMutation = false;
        if (templateNbt.contains("palette", 9)) {
            anyMutation |= HearthTemplateSanitizer.sanitizePaletteList(templateNbt.getList("palette", 10), isHearth);
        }
        if (templateNbt.contains("palettes", 9)) {
            ListTag palettes = templateNbt.getList("palettes", 9);
            for (int i = 0; i < palettes.size(); ++i) {
                anyMutation |= HearthTemplateSanitizer.sanitizePaletteList(palettes.getList(i), isHearth);
            }
        }
        return anyMutation;
    }

    private static boolean sanitizePaletteList(ListTag palette, Predicate<Block> isHearth) {
        if (palette == null) {
            return false;
        }
        boolean mutated = false;
        for (int i = 0; i < palette.size(); ++i) {
            CompoundTag props;
            Block block;
            ResourceLocation blockId;
            CompoundTag entry = palette.getCompound(i);
            String name = entry.getString("Name");
            if (name.isEmpty() || (blockId = ResourceLocation.tryParse((String)name)) == null || (block = (Block)BuiltInRegistries.BLOCK.get(blockId)) == null || !isHearth.test(block)) continue;
            boolean propsCreated = false;
            if (entry.contains("Properties", 10)) {
                props = entry.getCompound("Properties");
            } else {
                props = new CompoundTag();
                entry.put("Properties", (Tag)props);
                propsCreated = true;
            }
            String currentLit = props.getString("lit");
            if (!"false".equals(currentLit)) {
                props.putString("lit", "false");
                mutated = true;
                LOGGER.debug("HearthTemplateSanitizer: forced lit=false on palette entry {}", (Object)name);
                continue;
            }
            if (!propsCreated) continue;
            mutated = true;
        }
        return mutated;
    }

    private static boolean isTaggedHearthBlock(Block block) {
        return block.builtInRegistryHolder().is(ModTags.Blocks.HEARTH_BLOCKS);
    }
}

