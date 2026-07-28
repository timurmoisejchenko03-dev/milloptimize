/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.resources.ResourceLocation
 */
package org.millenaire.culture;

import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import org.millenaire.culture.TravelBookCategories;

public record Culture(ResourceLocation id, String displayName, ResourceLocation panelTexture, String nativeLanguage, TravelBookCategories travelBookCategories, List<String> knownCrops, List<String> knownHuntingDrops, Optional<ResourceLocation> mapIcon, @Nullable String cultureBannerNbt) {
    public static final ResourceLocation DEFAULT_PANEL_TEXTURE = ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)"textures/entity/panels/default.png");
}

