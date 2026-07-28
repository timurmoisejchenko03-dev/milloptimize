/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.registries.Registries
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.entity.BlockEntityType
 *  net.minecraft.world.level.block.entity.BlockEntityType$Builder
 *  net.neoforged.bus.api.IEventBus
 *  net.neoforged.neoforge.registries.DeferredHolder
 *  net.neoforged.neoforge.registries.DeferredRegister
 */
package org.millenaire.block;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.millenaire.block.FirePitBlockEntity;
import org.millenaire.block.ImportTableBlockEntity;
import org.millenaire.block.LockedChestBlockEntity;
import org.millenaire.block.ModBlocks;
import org.millenaire.block.VillagePanelBlockEntity;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create((ResourceKey)Registries.BLOCK_ENTITY_TYPE, (String)"millenaire");
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LockedChestBlockEntity>> LOCKED_CHEST = BLOCK_ENTITIES.register("locked_chest", () -> BlockEntityType.Builder.of(LockedChestBlockEntity::new, (Block[])new Block[]{(Block)ModBlocks.LOCKED_CHEST.get()}).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<VillagePanelBlockEntity>> VILLAGE_PANEL = BLOCK_ENTITIES.register("village_panel", () -> BlockEntityType.Builder.of(VillagePanelBlockEntity::new, (Block[])new Block[]{(Block)ModBlocks.VILLAGE_PANEL.get()}).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ImportTableBlockEntity>> IMPORT_TABLE = BLOCK_ENTITIES.register("import_table", () -> BlockEntityType.Builder.of(ImportTableBlockEntity::new, (Block[])new Block[]{(Block)ModBlocks.IMPORT_TABLE.get()}).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FirePitBlockEntity>> FIRE_PIT = BLOCK_ENTITIES.register("fire_pit", () -> BlockEntityType.Builder.of(FirePitBlockEntity::new, (Block[])new Block[]{(Block)ModBlocks.FIRE_PIT.get()}).build(null));

    private ModBlockEntities() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCK_ENTITIES.register(modEventBus);
    }
}

