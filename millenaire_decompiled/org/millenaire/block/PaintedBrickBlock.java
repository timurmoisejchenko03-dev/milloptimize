/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  net.minecraft.world.item.DyeColor
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.state.BlockBehaviour$Properties
 */
package org.millenaire.block;

import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.millenaire.block.IPaintedBlock;

public class PaintedBrickBlock
extends Block
implements IPaintedBlock {
    private final DyeColor color;
    private final BrickType brickType;

    public PaintedBrickBlock(DyeColor color, BrickType brickType, BlockBehaviour.Properties properties) {
        super(properties);
        this.color = color;
        this.brickType = brickType;
    }

    @Override
    public DyeColor getColor() {
        return this.color;
    }

    @Override
    public BrickType getBrickType() {
        return this.brickType;
    }

    public static final class BrickType
    extends Enum<BrickType> {
        public static final /* enum */ BrickType PLAIN = new BrickType();
        public static final /* enum */ BrickType DECORATED = new BrickType();
        private static final /* synthetic */ BrickType[] $VALUES;

        public static BrickType[] values() {
            return (BrickType[])$VALUES.clone();
        }

        public static BrickType valueOf(String name) {
            return Enum.valueOf(BrickType.class, name);
        }

        private static /* synthetic */ BrickType[] $values() {
            return new BrickType[]{PLAIN, DECORATED};
        }

        static {
            $VALUES = BrickType.$values();
        }
    }
}

