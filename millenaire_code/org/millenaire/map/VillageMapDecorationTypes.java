/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  net.minecraft.core.Holder
 *  net.minecraft.core.registries.BuiltInRegistries
 *  net.minecraft.core.registries.Registries
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.level.saveddata.maps.MapDecorationType
 *  net.neoforged.bus.api.IEventBus
 *  net.neoforged.neoforge.registries.DeferredHolder
 *  net.neoforged.neoforge.registries.DeferredRegister
 *  org.slf4j.Logger
 */
package org.millenaire.map;

import com.mojang.logging.LogUtils;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.saveddata.maps.MapDecorationType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.millenaire.culture.Culture;
import org.slf4j.Logger;

public final class VillageMapDecorationTypes {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final DeferredRegister<MapDecorationType> REGISTER = DeferredRegister.create((ResourceKey)Registries.MAP_DECORATION_TYPE, (String)"millenaire");
    public static final ResourceLocation GENERIC_ID = ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)"village_generic");
    public static final DeferredHolder<MapDecorationType, MapDecorationType> NORMAN = VillageMapDecorationTypes.decoration("village_norman");
    public static final DeferredHolder<MapDecorationType, MapDecorationType> INDIAN = VillageMapDecorationTypes.decoration("village_indian");
    public static final DeferredHolder<MapDecorationType, MapDecorationType> MAYAN = VillageMapDecorationTypes.decoration("village_mayan");
    public static final DeferredHolder<MapDecorationType, MapDecorationType> BYZANTINE = VillageMapDecorationTypes.decoration("village_byzantine");
    public static final DeferredHolder<MapDecorationType, MapDecorationType> JAPANESE = VillageMapDecorationTypes.decoration("village_japanese");
    public static final DeferredHolder<MapDecorationType, MapDecorationType> SELJUK = VillageMapDecorationTypes.decoration("village_seljuk");
    public static final DeferredHolder<MapDecorationType, MapDecorationType> INUIT = VillageMapDecorationTypes.decoration("village_inuit");
    public static final DeferredHolder<MapDecorationType, MapDecorationType> GENERIC = VillageMapDecorationTypes.decoration("village_generic");

    private VillageMapDecorationTypes() {
    }

    public static void register(IEventBus modEventBus) {
        REGISTER.register(modEventBus);
    }

    private static DeferredHolder<MapDecorationType, MapDecorationType> decoration(String name) {
        return REGISTER.register(name, () -> new MapDecorationType(ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)name), true, -1, false, false));
    }

    public static ResourceLocation resolveId(Culture culture) {
        return culture.mapIcon().orElse(GENERIC_ID);
    }

    public static Holder<MapDecorationType> resolveHolder(Culture culture) {
        ResourceLocation id = VillageMapDecorationTypes.resolveId(culture);
        ResourceKey key = ResourceKey.create((ResourceKey)Registries.MAP_DECORATION_TYPE, (ResourceLocation)id);
        return BuiltInRegistries.MAP_DECORATION_TYPE.getHolder(key).map(h -> h).orElseGet(() -> {
            LOGGER.warn("MapDecorationType {} not registered, falling back to {}", (Object)id, (Object)GENERIC_ID);
            return GENERIC;
        });
    }
}

