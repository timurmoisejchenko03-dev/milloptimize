/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.sounds.SoundEvents
 *  net.minecraft.world.InteractionResult
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.Item$Properties
 *  net.minecraft.world.item.context.UseOnContext
 *  net.minecraft.world.level.Level
 */
package org.millenaire.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.millenaire.entity.MillWallDecoration;
import org.millenaire.entity.WallDecorationType;

public class WallDecorationItem
extends Item {
    private final WallDecorationType decorationType;

    public WallDecorationItem(Item.Properties properties, WallDecorationType decorationType) {
        super(properties);
        this.decorationType = decorationType;
    }

    public WallDecorationType getDecorationType() {
        return this.decorationType;
    }

    public InteractionResult useOn(UseOnContext context) {
        Direction clickedFace = context.getClickedFace();
        if (clickedFace == Direction.DOWN || clickedFace == Direction.UP) {
            return InteractionResult.FAIL;
        }
        Level level = context.getLevel();
        BlockPos hangingPos = context.getClickedPos().relative(clickedFace);
        if (context.getPlayer() != null && !context.getPlayer().mayUseItemAt(hangingPos, clickedFace, context.getItemInHand())) {
            return InteractionResult.FAIL;
        }
        MillWallDecoration decoration = MillWallDecoration.createForPlayer(level, hangingPos, clickedFace, this.decorationType);
        if (decoration == null) {
            return InteractionResult.FAIL;
        }
        if (!level.isClientSide) {
            decoration.playSound(SoundEvents.PAINTING_PLACE, 1.0f, 1.0f);
            level.addFreshEntity((Entity)decoration);
        }
        context.getItemInHand().shrink(1);
        return InteractionResult.SUCCESS;
    }
}

