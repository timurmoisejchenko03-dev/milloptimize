/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.InteractionResult
 *  net.minecraft.world.entity.EquipmentSlot
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.item.DyeColor
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.Item$Properties
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.context.UseOnContext
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.SlabBlock
 *  net.minecraft.world.level.block.StairBlock
 *  net.minecraft.world.level.block.WallBlock
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.properties.Half
 *  net.minecraft.world.level.block.state.properties.Property
 *  net.minecraft.world.level.block.state.properties.SlabType
 *  net.minecraft.world.level.block.state.properties.StairsShape
 *  net.minecraft.world.level.block.state.properties.WallSide
 */
package org.millenaire.item;

import java.util.HashSet;
import java.util.LinkedList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.block.state.properties.StairsShape;
import net.minecraft.world.level.block.state.properties.WallSide;
import org.millenaire.advancement.MillAdvancements;
import org.millenaire.block.IPaintedBlock;
import org.millenaire.block.ModBlocks;
import org.millenaire.block.PaintedBrickBlock;
import org.millenaire.block.PaintedBrickSlabBlock;
import org.millenaire.block.PaintedBrickStairBlock;
import org.millenaire.block.PaintedBrickWallBlock;

public class PaintBucketItem
extends Item {
    private static final int MAX_BLOCKS = 256;
    private final DyeColor color;

    public PaintBucketItem(DyeColor color, Item.Properties properties) {
        super(properties);
        this.color = color;
    }

    public DyeColor getColor() {
        return this.color;
    }

    public InteractionResult useOn(UseOnContext context) {
        ServerPlayer serverPlayer;
        Player player;
        BlockPos pos;
        Level level = context.getLevel();
        BlockState state = level.getBlockState(pos = context.getClickedPos());
        Block block = state.getBlock();
        if (!(block instanceof IPaintedBlock)) {
            return InteractionResult.PASS;
        }
        IPaintedBlock existingBrick = (IPaintedBlock)block;
        if (existingBrick.getColor() == this.color) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        DyeColor oldColor = existingBrick.getColor();
        int blocksColored = this.floodFill(level, pos, oldColor, this.color);
        if (blocksColored > 0 && (player = context.getPlayer()) instanceof ServerPlayer) {
            serverPlayer = (ServerPlayer)player;
            MillAdvancements.grant(serverPlayer, MillAdvancements.RAINBOW);
        }
        if (blocksColored > 0 && (player = context.getPlayer()) instanceof ServerPlayer) {
            serverPlayer = (ServerPlayer)player;
            ItemStack stack = context.getItemInHand();
            EquipmentSlot slot = serverPlayer.getEquipmentSlotForItem(stack);
            stack.hurtAndBreak(blocksColored, (ServerLevel)level, serverPlayer, item -> serverPlayer.onEquippedItemBroken(item, slot));
        }
        return InteractionResult.SUCCESS;
    }

    private int floodFill(Level level, BlockPos start, DyeColor oldColor, DyeColor newColor) {
        HashSet<BlockPos> visited = new HashSet<BlockPos>();
        LinkedList<BlockPos> queue = new LinkedList<BlockPos>();
        queue.add(start);
        int count = 0;
        while (!queue.isEmpty() && count < 256) {
            IPaintedBlock painted;
            BlockState state;
            Block block;
            BlockPos pos = (BlockPos)queue.poll();
            if (!visited.add(pos) || !((block = (state = level.getBlockState(pos)).getBlock()) instanceof IPaintedBlock) || (painted = (IPaintedBlock)block).getColor() != oldColor) continue;
            BlockState newState = this.getRecoloredState(state, painted, newColor);
            if (newState != null) {
                level.setBlock(pos, newState, 2);
                ++count;
            }
            for (Direction dir : Direction.values()) {
                queue.add(pos.relative(dir));
            }
        }
        return count;
    }

    private BlockState getRecoloredState(BlockState state, IPaintedBlock painted, DyeColor newColor) {
        if (painted instanceof PaintedBrickBlock) {
            PaintedBrickBlock brick = (PaintedBrickBlock)painted;
            if (brick.getBrickType() == PaintedBrickBlock.BrickType.DECORATED) {
                return ((Block)ModBlocks.DECORATED_BRICKS.get(newColor).get()).defaultBlockState();
            }
            return ((Block)ModBlocks.PAINTED_BRICKS.get(newColor).get()).defaultBlockState();
        }
        if (painted instanceof PaintedBrickStairBlock) {
            BlockState base = ((Block)ModBlocks.PAINTED_BRICK_STAIRS.get(newColor).get()).defaultBlockState();
            return (BlockState)((BlockState)((BlockState)((BlockState)base.setValue((Property)StairBlock.FACING, (Comparable)((Direction)state.getValue((Property)StairBlock.FACING)))).setValue((Property)StairBlock.HALF, (Comparable)((Half)state.getValue((Property)StairBlock.HALF)))).setValue((Property)StairBlock.SHAPE, (Comparable)((StairsShape)state.getValue((Property)StairBlock.SHAPE)))).setValue((Property)StairBlock.WATERLOGGED, (Comparable)((Boolean)state.getValue((Property)StairBlock.WATERLOGGED)));
        }
        if (painted instanceof PaintedBrickSlabBlock) {
            BlockState base = ((Block)ModBlocks.PAINTED_BRICK_SLABS.get(newColor).get()).defaultBlockState();
            return (BlockState)((BlockState)base.setValue((Property)SlabBlock.TYPE, (Comparable)((SlabType)state.getValue((Property)SlabBlock.TYPE)))).setValue((Property)SlabBlock.WATERLOGGED, (Comparable)((Boolean)state.getValue((Property)SlabBlock.WATERLOGGED)));
        }
        if (painted instanceof PaintedBrickWallBlock) {
            BlockState base = ((Block)ModBlocks.PAINTED_BRICK_WALLS.get(newColor).get()).defaultBlockState();
            return (BlockState)((BlockState)((BlockState)((BlockState)((BlockState)((BlockState)base.setValue((Property)WallBlock.UP, (Comparable)((Boolean)state.getValue((Property)WallBlock.UP)))).setValue((Property)WallBlock.NORTH_WALL, (Comparable)((WallSide)state.getValue((Property)WallBlock.NORTH_WALL)))).setValue((Property)WallBlock.EAST_WALL, (Comparable)((WallSide)state.getValue((Property)WallBlock.EAST_WALL)))).setValue((Property)WallBlock.SOUTH_WALL, (Comparable)((WallSide)state.getValue((Property)WallBlock.SOUTH_WALL)))).setValue((Property)WallBlock.WEST_WALL, (Comparable)((WallSide)state.getValue((Property)WallBlock.WEST_WALL)))).setValue((Property)WallBlock.WATERLOGGED, (Comparable)((Boolean)state.getValue((Property)WallBlock.WATERLOGGED)));
        }
        return null;
    }
}

