/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.MapCodec
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.protocol.common.custom.CustomPacketPayload
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.InteractionHand
 *  net.minecraft.world.InteractionResult
 *  net.minecraft.world.ItemInteractionResult
 *  net.minecraft.world.entity.Mob
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.context.BlockPlaceContext
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.BaseEntityBlock
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.HorizontalDirectionalBlock
 *  net.minecraft.world.level.block.RenderShape
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.level.block.state.BlockBehaviour$Properties
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.StateDefinition$Builder
 *  net.minecraft.world.level.block.state.properties.EnumProperty
 *  net.minecraft.world.level.block.state.properties.Property
 *  net.minecraft.world.level.pathfinder.PathType
 *  net.minecraft.world.phys.BlockHitResult
 *  net.minecraft.world.phys.shapes.CollisionContext
 *  net.minecraft.world.phys.shapes.VoxelShape
 *  net.neoforged.neoforge.network.PacketDistributor
 */
package org.millenaire.block;

import com.mojang.serialization.MapCodec;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.network.PacketDistributor;
import org.millenaire.block.VillagePanelBlockEntity;
import org.millenaire.building.BuildingId;
import org.millenaire.building.BuildingInstance;
import org.millenaire.building.SpecialPoint;
import org.millenaire.item.SummoningWandItem;
import org.millenaire.network.ControlledMilitaryPayload;
import org.millenaire.network.ControlledProjectsPayload;
import org.millenaire.network.PanelContentPayload;
import org.millenaire.village.ControlledMilitaryService;
import org.millenaire.village.ControlledProjectsService;
import org.millenaire.village.Village;
import org.millenaire.village.VillageId;
import org.millenaire.village.VillageManager;
import org.millenaire.village.VillageSavedData;
import org.millenaire.village.panel.PanelContent;
import org.millenaire.village.panel.PanelContentGenerator;
import org.millenaire.village.panel.PanelType;

public class VillagePanelBlock
extends BaseEntityBlock {
    public static final MapCodec<VillagePanelBlock> CODEC = VillagePanelBlock.simpleCodec(VillagePanelBlock::new);
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    private static final VoxelShape SHAPE_NORTH = Block.box((double)0.0, (double)2.0, (double)14.0, (double)16.0, (double)14.0, (double)16.0);
    private static final VoxelShape SHAPE_SOUTH = Block.box((double)0.0, (double)2.0, (double)0.0, (double)16.0, (double)14.0, (double)2.0);
    private static final VoxelShape SHAPE_WEST = Block.box((double)14.0, (double)2.0, (double)0.0, (double)16.0, (double)14.0, (double)16.0);
    private static final VoxelShape SHAPE_EAST = Block.box((double)0.0, (double)2.0, (double)0.0, (double)2.0, (double)14.0, (double)16.0);

    public VillagePanelBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState((BlockState)((BlockState)this.stateDefinition.any()).setValue(FACING, (Comparable)Direction.NORTH));
    }

    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(new Property[]{FACING});
    }

    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return (BlockState)this.defaultBlockState().setValue(FACING, (Comparable)context.getHorizontalDirection().getOpposite());
    }

    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch ((Direction)state.getValue(FACING)) {
            case Direction.SOUTH -> SHAPE_SOUTH;
            case Direction.WEST -> SHAPE_WEST;
            case Direction.EAST -> SHAPE_EAST;
            default -> SHAPE_NORTH;
        };
    }

    @Nullable
    public PathType getBlockPathType(BlockState state, BlockGetter level, BlockPos pos, @Nullable Mob mob) {
        return PathType.BLOCKED;
    }

    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new VillagePanelBlockEntity(pos, state);
    }

    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (stack.getItem() instanceof SummoningWandItem) {
            return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer)) {
            return InteractionResult.PASS;
        }
        ServerPlayer serverPlayer = (ServerPlayer)player;
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof VillagePanelBlockEntity)) {
            return InteractionResult.PASS;
        }
        VillagePanelBlockEntity panel = (VillagePanelBlockEntity)be;
        this.sendPanelContent(serverPlayer, panel, (ServerLevel)level);
        return InteractionResult.SUCCESS;
    }

    private void sendPanelContent(ServerPlayer player, VillagePanelBlockEntity panel, ServerLevel level) {
        BuildingId buildingId = panel.getBuildingId();
        if (buildingId == null) {
            player.sendSystemMessage((Component)Component.literal((String)"\u00a76[Panneau]\u00a7r Aucun b\u00e2timent li\u00e9."));
            return;
        }
        VillageManager villageManager = VillageSavedData.get(level).getVillageManager();
        Village village = null;
        if (panel.getVillageId() != null) {
            village = villageManager.getVillage(new VillageId(panel.getVillageId()));
        }
        if (village == null) {
            village = villageManager.findVillageContaining(buildingId);
        }
        if (village == null) {
            player.sendSystemMessage((Component)Component.literal((String)"\u00a76[Panneau]\u00a7r B\u00e2timent introuvable."));
            return;
        }
        BuildingInstance building = village.findBuildingById(buildingId);
        if (building == null) {
            player.sendSystemMessage((Component)Component.literal((String)"\u00a76[Panneau]\u00a7r B\u00e2timent introuvable."));
            return;
        }
        if (panel.getPanelType() == PanelType.HALL_OF_FAME) {
            PanelContent content = PanelContentGenerator.buildHoFContent(panel);
            PacketDistributor.sendToPlayer((ServerPlayer)player, (CustomPacketPayload)PanelContentPayload.fromContent(content), (CustomPacketPayload[])new CustomPacketPayload[0]);
            return;
        }
        if (panel.getPanelType() == PanelType.CONTROLLED_PROJECTS && village.isControlledBy(player.getUUID())) {
            ControlledProjectsPayload payload = ControlledProjectsService.buildPayload(village);
            PacketDistributor.sendToPlayer((ServerPlayer)player, (CustomPacketPayload)payload, (CustomPacketPayload[])new CustomPacketPayload[0]);
            return;
        }
        if (panel.getPanelType() == PanelType.CONTROLLED_MILITARY && village.isControlledBy(player.getUUID())) {
            ControlledMilitaryPayload payload = ControlledMilitaryService.buildPayload(village, level);
            PacketDistributor.sendToPlayer((ServerPlayer)player, (CustomPacketPayload)payload, (CustomPacketPayload[])new CustomPacketPayload[0]);
            return;
        }
        int signIndex = this.computeSignIndex(panel.getBlockPos(), building);
        PanelContent content = PanelContentGenerator.generate(panel.getPanelType(), village, building, level, signIndex, player);
        if (panel.getPanelType() == PanelType.VILLAGE_MAP) {
            PacketDistributor.sendToPlayer((ServerPlayer)player, (CustomPacketPayload)PanelContentGenerator.createMapPayload(content, village, level, player), (CustomPacketPayload[])new CustomPacketPayload[0]);
        } else {
            PacketDistributor.sendToPlayer((ServerPlayer)player, (CustomPacketPayload)PanelContentPayload.fromContent(content), (CustomPacketPayload[])new CustomPacketPayload[0]);
        }
    }

    private int computeSignIndex(BlockPos panelPos, BuildingInstance building) {
        List<SpecialPoint> signPoints = building.getPointsByType("signPos");
        for (int i = 0; i < signPoints.size(); ++i) {
            if (!signPoints.get(i).pos().equals((Object)panelPos)) continue;
            return i;
        }
        return 0;
    }
}

