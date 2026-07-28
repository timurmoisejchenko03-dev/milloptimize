/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  net.minecraft.core.Holder
 *  net.minecraft.core.HolderSet$Named
 *  net.minecraft.core.Registry
 *  net.minecraft.core.RegistryAccess
 *  net.minecraft.core.registries.Registries
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.tags.TagKey
 *  net.minecraft.world.level.biome.Biome
 *  org.slf4j.Logger
 */
package org.millenaire.world;

import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.StreamSupport;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import org.slf4j.Logger;

public final class BiomeTagDisjointnessValidator {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final List<TagPair> DISJOINT_PAIRS = List.of(new TagPair("c", "is_forest", "c", "is_birch_forest"), new TagPair("c", "is_forest", "c", "is_dark_forest"), new TagPair("c", "is_plains", "c", "is_meadow"));

    private BiomeTagDisjointnessValidator() {
    }

    public static void validate(RegistryAccess registryAccess) {
        Registry biomes = registryAccess.registryOrThrow(Registries.BIOME);
        for (TagPair pair : DISJOINT_PAIRS) {
            List<ResourceLocation> overlap = BiomeTagDisjointnessValidator.findOverlap((Registry<Biome>)biomes, pair.left(), pair.right());
            if (overlap.isEmpty()) continue;
            LOGGER.warn("[Millenaire] Biome tag overlap: {} biome(s) belong to both #{}:{} and #{}:{} \u2014 village/LB biome redistribution assumes these tags are disjoint. Overlapping biomes: {}", new Object[]{overlap.size(), pair.left().location().getNamespace(), pair.left().location().getPath(), pair.right().location().getNamespace(), pair.right().location().getPath(), overlap});
        }
    }

    private static List<ResourceLocation> findOverlap(Registry<Biome> biomes, TagKey<Biome> a, TagKey<Biome> b) {
        HolderSet.Named setA = biomes.getTag(a).orElse(null);
        HolderSet.Named setB = biomes.getTag(b).orElse(null);
        if (setA == null || setB == null) {
            return List.of();
        }
        ArrayList<ResourceLocation> overlap = new ArrayList<ResourceLocation>();
        for (Holder holder : setA) {
            if (!StreamSupport.stream(setB.spliterator(), false).anyMatch(h -> h.equals((Object)holder))) continue;
            holder.unwrapKey().ifPresent(key -> overlap.add(key.location()));
        }
        return overlap;
    }

    private record TagPair(TagKey<Biome> left, TagKey<Biome> right) {
        TagPair(String nsA, String pathA, String nsB, String pathB) {
            this((TagKey<Biome>)TagKey.create((ResourceKey)Registries.BIOME, (ResourceLocation)ResourceLocation.fromNamespaceAndPath((String)nsA, (String)pathA)), (TagKey<Biome>)TagKey.create((ResourceKey)Registries.BIOME, (ResourceLocation)ResourceLocation.fromNamespaceAndPath((String)nsB, (String)pathB)));
        }
    }
}

