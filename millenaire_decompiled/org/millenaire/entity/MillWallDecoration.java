/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.core.Direction$Axis
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.network.protocol.Packet
 *  net.minecraft.network.protocol.game.ClientGamePacketListener
 *  net.minecraft.network.protocol.game.ClientboundAddEntityPacket
 *  net.minecraft.network.syncher.EntityDataAccessor
 *  net.minecraft.network.syncher.EntityDataSerializer
 *  net.minecraft.network.syncher.EntityDataSerializers
 *  net.minecraft.network.syncher.SynchedEntityData
 *  net.minecraft.network.syncher.SynchedEntityData$Builder
 *  net.minecraft.server.level.ServerEntity
 *  net.minecraft.sounds.SoundEvents
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.EntityType
 *  net.minecraft.world.entity.decoration.HangingEntity
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.level.GameRules
 *  net.minecraft.world.level.ItemLike
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.phys.AABB
 *  org.slf4j.Logger
 */
package org.millenaire.entity;

import com.mojang.logging.LogUtils;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.millenaire.entity.ModEntities;
import org.millenaire.entity.WallDecorationType;
import org.millenaire.entity.WallDecorationVariant;
import org.millenaire.item.ModItems;
import org.slf4j.Logger;

public class MillWallDecoration
extends HangingEntity {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final EntityDataAccessor<Integer> DATA_VARIANT = SynchedEntityData.defineId(MillWallDecoration.class, (EntityDataSerializer)EntityDataSerializers.INT);
    private WallDecorationVariant variant = WallDecorationVariant.Griffon;

    public MillWallDecoration(EntityType<? extends MillWallDecoration> type, Level level) {
        super(type, level);
    }

    public MillWallDecoration(Level level, BlockPos pos, Direction direction, WallDecorationVariant variant) {
        super((EntityType)ModEntities.WALL_DECORATION.get(), level, pos);
        this.variant = variant;
        this.entityData.set(DATA_VARIANT, (Object)variant.ordinal());
        this.setDirection(direction);
    }

    @Nullable
    public static MillWallDecoration createForBuilding(Level level, BlockPos pos, WallDecorationType type) {
        int maxHeight;
        Direction facing = MillWallDecoration.guessOrientation(level, pos);
        int maxWidth = MillWallDecoration.measureWallWidth(level, pos, facing);
        WallDecorationVariant selected = WallDecorationVariant.selectRandom(type, maxWidth, maxHeight = MillWallDecoration.measureWallHeight(level, pos), true, level.getRandom());
        if (selected == null) {
            LOGGER.debug("No variant {} fits on the wall at {} (max {}x{})", new Object[]{type.typeName(), pos.toShortString(), maxWidth, maxHeight});
            return null;
        }
        MillWallDecoration decoration = new MillWallDecoration(level, pos, facing, selected);
        if (decoration.survives()) {
            LOGGER.debug("Decoration {} ({}) created at {}/{}", new Object[]{selected.title(), type.typeName(), pos.toShortString(), facing});
            return decoration;
        }
        LOGGER.debug("Decoration {} does not survive at {}", (Object)selected.title(), (Object)pos.toShortString());
        return null;
    }

    @Nullable
    public static MillWallDecoration createForPlayer(Level level, BlockPos pos, Direction facing, WallDecorationType type) {
        int maxHeight;
        int maxWidth = MillWallDecoration.measureWallWidth(level, pos, facing);
        WallDecorationVariant selected = WallDecorationVariant.selectRandom(type, maxWidth, maxHeight = MillWallDecoration.measureWallHeight(level, pos), false, level.getRandom());
        if (selected == null) {
            return null;
        }
        MillWallDecoration decoration = new MillWallDecoration(level, pos, facing, selected);
        return decoration.survives() ? decoration : null;
    }

    private static Direction guessOrientation(Level level, BlockPos pos) {
        if (MillWallDecoration.isSolidWall(level, pos.north())) {
            return Direction.SOUTH;
        }
        if (MillWallDecoration.isSolidWall(level, pos.south())) {
            return Direction.NORTH;
        }
        if (MillWallDecoration.isSolidWall(level, pos.east())) {
            return Direction.WEST;
        }
        if (MillWallDecoration.isSolidWall(level, pos.west())) {
            return Direction.EAST;
        }
        return Direction.WEST;
    }

    private static boolean isSolidWall(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.isSolidRender((BlockGetter)level, pos);
    }

    private static int measureWallWidth(Level level, BlockPos pos, Direction facing) {
        BlockPos check;
        BlockPos behind;
        int i;
        Direction right = facing.getClockWise();
        int width = 1;
        for (i = 1; i <= 16 && MillWallDecoration.isSolidWall(level, behind = (check = pos.relative(right, i)).relative(facing.getOpposite())) && level.getBlockState(check).isAir(); ++i) {
            ++width;
        }
        for (i = 1; i <= 16 && MillWallDecoration.isSolidWall(level, behind = (check = pos.relative(right, -i)).relative(facing.getOpposite())) && level.getBlockState(check).isAir(); ++i) {
            ++width;
        }
        return width;
    }

    private static int measureWallHeight(Level level, BlockPos pos) {
        int i;
        int height = 1;
        for (i = 1; i <= 4 && level.getBlockState(pos.above(i)).isAir(); ++i) {
            ++height;
        }
        for (i = 1; i <= 4 && level.getBlockState(pos.below(i)).isAir(); ++i) {
            ++height;
        }
        return height;
    }

    protected AABB calculateBoundingBox(BlockPos pos, Direction direction) {
        return MillWallDecoration.computeBoundingBox(pos, direction, this.variant.widthBlocks(), this.variant.heightBlocks());
    }

    static AABB computeBoundingBox(BlockPos pos, Direction direction, int width, int height) {
        double depth = 0.0625;
        double halfWidth = (double)width / 2.0;
        double halfHeight = (double)height / 2.0;
        double cx = (double)pos.getX() + 0.5;
        double cy = (double)pos.getY() + 0.5;
        double cz = (double)pos.getZ() + 0.5;
        cx -= (double)direction.getStepX() * (0.5 - depth / 2.0);
        cz -= (double)direction.getStepZ() * (0.5 - depth / 2.0);
        Direction perpendicular = direction.getCounterClockWise();
        if (width % 2 == 0) {
            cx += (double)perpendicular.getStepX() * 0.5;
            cz += (double)perpendicular.getStepZ() * 0.5;
        }
        if (height % 2 == 0) {
            cy += 0.5;
        }
        if (direction.getAxis() == Direction.Axis.Z) {
            return new AABB(cx - halfWidth, cy - halfHeight, cz - depth / 2.0, cx + halfWidth, cy + halfHeight, cz + depth / 2.0);
        }
        return new AABB(cx - depth / 2.0, cy - halfHeight, cz - halfWidth, cx + depth / 2.0, cy + halfHeight, cz + halfWidth);
    }

    public void dropItem(@Nullable Entity breaker) {
        ItemStack drop;
        if (!this.level().getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
            return;
        }
        this.playSound(SoundEvents.PAINTING_BREAK, 1.0f, 1.0f);
        if (breaker instanceof Player) {
            Player player = (Player)breaker;
            if (player.getAbilities().instabuild) {
                return;
            }
        }
        if (!(drop = this.getDropStack()).isEmpty()) {
            this.spawnAtLocation(drop);
        }
    }

    private ItemStack getDropStack() {
        return switch (this.variant.type()) {
            default -> throw new MatchException(null, null);
            case WallDecorationType.NORMAN_TAPESTRY -> new ItemStack((ItemLike)ModItems.TAPESTRY.get());
            case WallDecorationType.INDIAN_STATUE -> new ItemStack((ItemLike)ModItems.INDIAN_STATUE.get());
            case WallDecorationType.MAYAN_STATUE -> new ItemStack((ItemLike)ModItems.MAYAN_STATUE.get());
            case WallDecorationType.BYZANTINE_ICON_SMALL -> new ItemStack((ItemLike)ModItems.BYZANTINE_ICON_SMALL.get());
            case WallDecorationType.BYZANTINE_ICON_MEDIUM -> new ItemStack((ItemLike)ModItems.BYZANTINE_ICON_MEDIUM.get());
            case WallDecorationType.BYZANTINE_ICON_LARGE -> new ItemStack((ItemLike)ModItems.BYZANTINE_ICON_LARGE.get());
            case WallDecorationType.HIDE_HANGING -> new ItemStack((ItemLike)ModItems.HIDE_HANGING.get());
            case WallDecorationType.WALL_CARPET_SMALL -> new ItemStack((ItemLike)ModItems.WALL_CARPET_SMALL.get());
            case WallDecorationType.WALL_CARPET_MEDIUM -> new ItemStack((ItemLike)ModItems.WALL_CARPET_MEDIUM.get());
            case WallDecorationType.WALL_CARPET_LARGE -> new ItemStack((ItemLike)ModItems.WALL_CARPET_LARGE.get());
        };
    }

    public void playPlacementSound() {
        this.playSound(SoundEvents.PAINTING_PLACE, 1.0f, 1.0f);
    }

    public Packet<ClientGamePacketListener> getAddEntityPacket(ServerEntity serverEntity) {
        return new ClientboundAddEntityPacket((Entity)this, this.getDirection().get3DDataValue(), this.getPos());
    }

    public void recreateFromPacket(ClientboundAddEntityPacket packet) {
        super.recreateFromPacket(packet);
        this.setDirection(Direction.from3DDataValue((int)packet.getData()));
    }

    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_VARIANT, (Object)WallDecorationVariant.Griffon.ordinal());
    }

    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (DATA_VARIANT.equals(key)) {
            int ordinal = (Integer)this.entityData.get(DATA_VARIANT);
            WallDecorationVariant[] variants = WallDecorationVariant.values();
            if (ordinal >= 0 && ordinal < variants.length) {
                this.variant = variants[ordinal];
                if (this.getDirection() != null) {
                    this.setDirection(this.getDirection());
                }
            }
        }
    }

    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Type", this.variant.type().legacyId());
        tag.putString("Motive", this.variant.title());
    }

    public void readAdditionalSaveData(CompoundTag tag) {
        String motive = tag.getString("Motive");
        WallDecorationVariant loaded = WallDecorationVariant.fromTitle(motive);
        if (loaded != null) {
            this.variant = loaded;
        } else {
            int typeId = tag.getInt("Type");
            WallDecorationType type = WallDecorationType.fromLegacyId(typeId);
            if (type != null) {
                for (WallDecorationVariant v : WallDecorationVariant.values()) {
                    if (v.type() != type) continue;
                    this.variant = v;
                    break;
                }
            }
            LOGGER.warn("Unknown wall variant '{}', fallback to {}", (Object)motive, (Object)this.variant.title());
        }
        this.entityData.set(DATA_VARIANT, (Object)this.variant.ordinal());
        super.readAdditionalSaveData(tag);
    }

    public WallDecorationVariant getVariant() {
        return this.variant;
    }

    public WallDecorationType getDecorationType() {
        return this.variant.type();
    }

    public String toString() {
        return "WallDecoration (" + this.variant.title() + ") " + super.toString();
    }
}

