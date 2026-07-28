/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.registries.Registries
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.world.entity.EntityType
 *  net.minecraft.world.entity.EntityType$Builder
 *  net.minecraft.world.entity.MobCategory
 *  net.neoforged.bus.api.IEventBus
 *  net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent
 *  net.neoforged.neoforge.registries.DeferredHolder
 *  net.neoforged.neoforge.registries.DeferredRegister
 */
package org.millenaire.entity;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.millenaire.entity.MillVillager;
import org.millenaire.entity.MillWallDecoration;

public final class ModEntities {
    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create((ResourceKey)Registries.ENTITY_TYPE, (String)"millenaire");
    public static final DeferredHolder<EntityType<?>, EntityType<MillVillager>> MILL_VILLAGER = ENTITY_TYPES.register("villager", () -> EntityType.Builder.of(MillVillager::new, (MobCategory)MobCategory.CREATURE).sized(0.6f, 1.95f).clientTrackingRange(10).build("millenaire:villager"));
    public static final DeferredHolder<EntityType<?>, EntityType<MillWallDecoration>> WALL_DECORATION = ENTITY_TYPES.register("wall_decoration", () -> EntityType.Builder.of(MillWallDecoration::new, (MobCategory)MobCategory.MISC).sized(0.5f, 0.5f).clientTrackingRange(10).updateInterval(Integer.MAX_VALUE).build("millenaire:wall_decoration"));

    private ModEntities() {
    }

    public static void register(IEventBus modEventBus) {
        ENTITY_TYPES.register(modEventBus);
        modEventBus.addListener(ModEntities::onAttributeCreation);
    }

    private static void onAttributeCreation(EntityAttributeCreationEvent event) {
        event.put((EntityType)MILL_VILLAGER.get(), MillVillager.createAttributes().build());
    }
}

