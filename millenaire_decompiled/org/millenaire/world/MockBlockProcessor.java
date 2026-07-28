/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.MapCodec
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.HolderLookup$Provider
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.world.level.LevelReader
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.EntityBlock
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings
 *  net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor
 *  net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType
 *  net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate
 *  net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate$StructureBlockInfo
 */
package org.millenaire.world;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.millenaire.block.mock.MockBlock;
import org.millenaire.world.ModProcessors;

public class MockBlockProcessor
extends StructureProcessor {
    public static final MockBlockProcessor INSTANCE = new MockBlockProcessor();
    public static final MapCodec<MockBlockProcessor> CODEC = MapCodec.unit((Object)((Object)INSTANCE));

    @Nullable
    public StructureTemplate.StructureBlockInfo process(LevelReader level, BlockPos offset, BlockPos pos, StructureTemplate.StructureBlockInfo original, StructureTemplate.StructureBlockInfo transformed, StructurePlaceSettings settings, @Nullable StructureTemplate template) {
        BlockState state = transformed.state();
        Block block = state.getBlock();
        if (block instanceof MockBlock) {
            EntityBlock entityBlock;
            BlockEntity tempBe;
            MockBlock mock = (MockBlock)block;
            BlockState replacement = mock.getReplacementState(state);
            if (replacement == null) {
                return null;
            }
            CompoundTag nbt = null;
            Block block2 = replacement.getBlock();
            if (block2 instanceof EntityBlock && (tempBe = (entityBlock = (EntityBlock)block2).newBlockEntity(transformed.pos(), replacement)) != null) {
                nbt = tempBe.saveWithoutMetadata((HolderLookup.Provider)level.registryAccess());
            }
            return new StructureTemplate.StructureBlockInfo(transformed.pos(), replacement, nbt);
        }
        return transformed;
    }

    protected StructureProcessorType<?> getType() {
        return ModProcessors.MOCK_BLOCK_PROCESSOR.get();
    }
}

