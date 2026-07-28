/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.registries.Registries
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.tags.TagKey
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.level.block.Block
 */
package org.millenaire.tag;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public final class ModTags {
    private ModTags() {
    }

    public static final class Blocks {
        public static final TagKey<Block> DEPENDENT_BLOCKS = TagKey.create((ResourceKey)Registries.BLOCK, (ResourceLocation)ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)"dependent_blocks"));
        public static final TagKey<Block> DEPENDENT_VEGETATION = TagKey.create((ResourceKey)Registries.BLOCK, (ResourceLocation)ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)"dependent_vegetation"));
        public static final TagKey<Block> FORBIDDEN_EXCEPTIONS = TagKey.create((ResourceKey)Registries.BLOCK, (ResourceLocation)ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)"forbidden_exceptions"));
        public static final TagKey<Block> ARTIFICIAL_BLOCKS = TagKey.create((ResourceKey)Registries.BLOCK, (ResourceLocation)ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)"artificial_blocks"));
        public static final TagKey<Block> HEARTH_BLOCKS = TagKey.create((ResourceKey)Registries.BLOCK, (ResourceLocation)ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)"hearth_blocks"));

        private Blocks() {
        }
    }

    public static final class Items {
        public static final TagKey<Item> CULTURE_WEAPONS = TagKey.create((ResourceKey)Registries.ITEM, (ResourceLocation)ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)"culture_weapons"));

        private Items() {
        }
    }
}

