/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.registries.Registries
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType
 *  net.neoforged.bus.api.IEventBus
 *  net.neoforged.neoforge.registries.DeferredRegister
 */
package org.millenaire.world;

import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.millenaire.world.MockBlockProcessor;

public final class ModProcessors {
    public static final DeferredRegister<StructureProcessorType<?>> PROCESSORS = DeferredRegister.create((ResourceKey)Registries.STRUCTURE_PROCESSOR, (String)"millenaire");
    public static final Supplier<StructureProcessorType<MockBlockProcessor>> MOCK_BLOCK_PROCESSOR = PROCESSORS.register("mock_block", () -> () -> MockBlockProcessor.CODEC);

    public static void register(IEventBus bus) {
        PROCESSORS.register(bus);
    }

    private ModProcessors() {
    }
}

