/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Holder
 *  net.minecraft.core.Vec3i
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.protocol.common.custom.CustomPacketPayload
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.tags.TagKey
 *  net.minecraft.world.InteractionHand
 *  net.minecraft.world.InteractionResult
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.Item$Properties
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.context.UseOnContext
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.biome.Biome
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.level.block.state.BlockState
 *  net.neoforged.neoforge.network.PacketDistributor
 *  org.slf4j.Logger
 */
package org.millenaire.item;

import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;
import org.millenaire.advancement.MillAdvancements;
import org.millenaire.block.LockedChestBlock;
import org.millenaire.block.LockedChestBlockEntity;
import org.millenaire.block.VillagePanelBlock;
import org.millenaire.block.VillagePanelBlockEntity;
import org.millenaire.building.BuildingId;
import org.millenaire.building.BuildingInstance;
import org.millenaire.building.BuildingPlanSet;
import org.millenaire.config.MillenaireServerConfig;
import org.millenaire.culture.Culture;
import org.millenaire.culture.ModCultures;
import org.millenaire.culture.VillageType;
import org.millenaire.culture.VillagerType;
import org.millenaire.entity.MillVillager;
import org.millenaire.network.BuildingProjectListPayload;
import org.millenaire.network.VillageTypeListPayload;
import org.millenaire.network.WandDebugMenuPayload;
import org.millenaire.quest.QuestRegistry;
import org.millenaire.village.LocalMerchantHelper;
import org.millenaire.village.PlayerCultureReputation;
import org.millenaire.village.PlayerQuestData;
import org.millenaire.village.Village;
import org.millenaire.village.VillageManager;
import org.millenaire.village.VillageSavedData;
import org.millenaire.world.VillageSpawner;
import org.slf4j.Logger;

public class SummoningWandItem
extends Item {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int MARVEL_MIN_DISTANCE = 200;

    public SummoningWandItem(Item.Properties properties) {
        super(properties);
    }

    public InteractionResult useOn(UseOnContext context) {
        Player player;
        ServerPlayer player2;
        Level level = context.getLevel();
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!level.dimension().equals(Level.OVERWORLD)) {
            Player player3 = context.getPlayer();
            if (player3 instanceof ServerPlayer) {
                ServerPlayer player4 = (ServerPlayer)player3;
                player4.sendSystemMessage((Component)Component.literal((String)"The summoning wand only works in the Overworld."));
            }
            return InteractionResult.FAIL;
        }
        ServerLevel serverLevel = (ServerLevel)level;
        BlockPos clickedPos = context.getClickedPos();
        BlockState clickedBlock = serverLevel.getBlockState(clickedPos);
        BlockPos spawnPos = clickedPos.above();
        if (clickedBlock.getBlock() instanceof LockedChestBlock || clickedBlock.getBlock() instanceof VillagePanelBlock) {
            ServerPlayer player5;
            Player player6 = context.getPlayer();
            if (player6 instanceof ServerPlayer && ((player5 = (ServerPlayer)player6).hasPermissions(2) || player5.server.isSingleplayer())) {
                this.openBuildingDebugMenu(serverLevel, player5, clickedPos);
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        }
        if (clickedBlock.is(Blocks.GOLD_BLOCK)) {
            Player player7 = context.getPlayer();
            if (player7 instanceof ServerPlayer) {
                ServerPlayer player8 = (ServerPlayer)player7;
                List<VillageType> selectable = this.getWandSelectableVillageTypes();
                if (selectable.isEmpty()) {
                    player8.sendSystemMessage((Component)Component.literal((String)"No village type available."));
                    return InteractionResult.FAIL;
                }
                ArrayList<VillageTypeListPayload.VillageTypeEntry> entries = new ArrayList<VillageTypeListPayload.VillageTypeEntry>();
                PlayerCultureReputation cultureRep = PlayerCultureReputation.get(serverLevel);
                for (VillageType vt : selectable) {
                    boolean hasControl;
                    String cultureKey = vt.culture().getPath();
                    Culture culture = ModCultures.getCulture(vt.culture());
                    String cultureName = culture != null ? culture.displayName() : cultureKey;
                    boolean requiresControl = vt.playerControlled();
                    boolean bl = hasControl = requiresControl && cultureRep.hasCultureControl(player8.getUUID(), vt.culture());
                    if (requiresControl && !hasControl) continue;
                    entries.add(new VillageTypeListPayload.VillageTypeEntry(cultureKey, cultureName, vt.id().getPath(), vt.name(), vt.weight(), requiresControl, hasControl));
                }
                PacketDistributor.sendToPlayer((ServerPlayer)player8, (CustomPacketPayload)new VillageTypeListPayload(spawnPos, entries), (CustomPacketPayload[])new CustomPacketPayload[0]);
            }
            return InteractionResult.SUCCESS;
        }
        if (clickedBlock.is(Blocks.OBSIDIAN)) {
            return this.spawnRandomVillage(serverLevel, spawnPos, context);
        }
        Player selectable = context.getPlayer();
        if (selectable instanceof ServerPlayer) {
            player2 = (ServerPlayer)selectable;
            Village closestVillage = this.findClosestVillageInRange(serverLevel, spawnPos);
            if (closestVillage != null && closestVillage.isPlayerControlled()) {
                if (closestVillage.isControlledBy(player2.getUUID())) {
                    this.sendBuildingProjectList(serverLevel, player2, closestVillage, clickedPos);
                    return InteractionResult.SUCCESS;
                }
                String villageName = closestVillage.getVillageName() != null ? closestVillage.getVillageName() : "";
                player2.sendSystemMessage((Component)Component.translatable((String)"ui.wand_invillagerange", (Object[])new Object[]{villageName}));
                return InteractionResult.FAIL;
            }
        }
        if ((player = context.getPlayer()) instanceof ServerPlayer && this.handleMarvelLocationPick(serverLevel, player2 = (ServerPlayer)player)) {
            return InteractionResult.SUCCESS;
        }
        player = context.getPlayer();
        if (player instanceof ServerPlayer) {
            player2 = (ServerPlayer)player;
            player2.sendSystemMessage((Component)Component.translatable((String)"ui.wandinstruction"));
        }
        return InteractionResult.FAIL;
    }

    private boolean handleMarvelLocationPick(ServerLevel level, ServerPlayer player) {
        BlockPos pos = player.blockPosition();
        ServerLevel overworld = level.getServer().getLevel(Level.OVERWORLD);
        if (overworld == null) {
            return false;
        }
        PlayerQuestData data = PlayerQuestData.get(overworld, QuestRegistry::get);
        if (!data.hasPlayerTag(player.getUUID(), "normanmarvel_picklocation")) {
            return false;
        }
        VillageSavedData savedData = VillageSavedData.get(overworld);
        double closestDist = this.getClosestVillageDistance(savedData, pos);
        if (closestDist < 200.0) {
            player.sendSystemMessage((Component)Component.translatable((String)"actions.normanmarvel_villagetooclose", (Object[])new Object[]{String.valueOf(200), String.valueOf(Math.round(closestDist))}));
            return true;
        }
        ResourceLocation notredameId = ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)"norman/notredame");
        VillageType notredameType = ModCultures.getVillageType(notredameId);
        if (notredameType == null) {
            LOGGER.warn("Village type 'norman/notredame' not found \u2014 skipping site validation");
        } else {
            Component siteError = VillageSpawner.validateSite(overworld, pos, notredameType);
            if (siteError != null) {
                player.sendSystemMessage((Component)Component.translatable((String)"actions.normanmarvel_notgenerated"));
                LOGGER.info("Marvel site rejected at {} for player {}: {}", new Object[]{pos.toShortString(), player.getName().getString(), siteError.getString()});
                return true;
            }
        }
        String locationStr = pos.getX() + "/" + pos.getY() + "/" + pos.getZ();
        data.setActionData(player.getUUID(), "normanmarvel_location", locationStr);
        data.setPlayerTag(player.getUUID(), "normanmarvel_picklocation_complete");
        player.sendSystemMessage((Component)Component.translatable((String)"actions.normanmarvel_locationset"));
        LOGGER.info("Marvel location set at {} for player {}", (Object)locationStr, (Object)player.getName().getString());
        return true;
    }

    private double getClosestVillageDistance(VillageSavedData savedData, BlockPos pos) {
        double dist;
        double closest = Double.MAX_VALUE;
        for (Village v : savedData.getVillageManager().getAllVillages()) {
            dist = Math.sqrt(v.getCenter().distSqr((Vec3i)pos));
            if (!(dist < closest)) continue;
            closest = dist;
        }
        for (VillageSavedData.LoneBuildingEntry entry : savedData.getLoneBuildingPositions()) {
            dist = Math.sqrt(entry.pos().distSqr((Vec3i)pos));
            if (!(dist < closest)) continue;
            closest = dist;
        }
        return closest;
    }

    @Nullable
    private Village findClosestVillageInRange(ServerLevel level, BlockPos pos) {
        return VillageSavedData.get(level).getVillageManager().findNearestVillage(pos, 100.0);
    }

    private void sendBuildingProjectList(ServerLevel level, ServerPlayer player, Village village, BlockPos clickedPos) {
        VillageType vt = ModCultures.getVillageType(village.getVillageTypeId());
        if (vt == null) {
            return;
        }
        ArrayList<BuildingProjectListPayload.BuildingEntry> entries = new ArrayList<BuildingProjectListPayload.BuildingEntry>();
        for (VillageType.LayoutSlot slot : vt.layout()) {
            BuildingPlanSet planSet;
            String role = slot.role();
            if (role.equals("centre") || role.equals("start") || (planSet = ModCultures.getBuildingPlanSet(slot.plan())) == null || planSet.isTownHall()) continue;
            String name = planSet.nativeName() != null ? planSet.nativeName() : slot.plan().getPath();
            String planIdStr = slot.plan().toString();
            if (entries.stream().anyMatch(e -> e.planSetId().equals(planIdStr))) continue;
            entries.add(new BuildingProjectListPayload.BuildingEntry(planIdStr, name));
        }
        String villageName = village.getVillageName() != null ? village.getVillageName() : "";
        PacketDistributor.sendToPlayer((ServerPlayer)player, (CustomPacketPayload)new BuildingProjectListPayload(village.getId().uuid().toString(), villageName, clickedPos, entries), (CustomPacketPayload[])new CustomPacketPayload[0]);
    }

    private InteractionResult spawnRandomVillage(ServerLevel serverLevel, BlockPos spawnPos, UseOnContext context) {
        if (this.isVillageTooClose(serverLevel, spawnPos, context)) {
            return InteractionResult.FAIL;
        }
        List<VillageType> compatible = this.getCompatibleVillageTypes(serverLevel, spawnPos);
        if (compatible.isEmpty()) {
            Player player = context.getPlayer();
            if (player instanceof ServerPlayer) {
                ServerPlayer player2 = (ServerPlayer)player;
                player2.sendSystemMessage((Component)Component.literal((String)"No village type compatible with this biome."));
            }
            return InteractionResult.FAIL;
        }
        int totalWeight = 0;
        for (VillageType vt : compatible) {
            totalWeight += vt.weight();
        }
        if (totalWeight <= 0) {
            VillageType vt;
            vt = context.getPlayer();
            if (vt instanceof ServerPlayer) {
                ServerPlayer player = (ServerPlayer)vt;
                player.sendSystemMessage((Component)Component.literal((String)"No village type with weight > 0 for this biome."));
            }
            return InteractionResult.FAIL;
        }
        int roll = ThreadLocalRandom.current().nextInt(totalWeight);
        VillageType chosen = compatible.getLast();
        int cumulative = 0;
        for (VillageType vt : compatible) {
            if (roll >= (cumulative += vt.weight())) continue;
            chosen = vt;
            break;
        }
        Component failure = VillageSpawner.spawnVillage(serverLevel, spawnPos, chosen);
        Player player = context.getPlayer();
        if (player instanceof ServerPlayer) {
            ServerPlayer player3 = (ServerPlayer)player;
            if (failure == null) {
                player3.sendSystemMessage((Component)Component.literal((String)("Village " + chosen.name() + " (" + chosen.id().getPath() + ") created at " + spawnPos.toShortString())));
                MillAdvancements.grant(player3, MillAdvancements.SUMMONING_WAND);
            } else {
                player3.sendSystemMessage(failure);
            }
        }
        return failure == null ? InteractionResult.SUCCESS : InteractionResult.FAIL;
    }

    private InteractionResult spawnSpecificVillage(ServerLevel serverLevel, BlockPos spawnPos, ResourceLocation villageTypeId, UseOnContext context) {
        if (this.isVillageTooClose(serverLevel, spawnPos, context)) {
            return InteractionResult.FAIL;
        }
        VillageType villageType = ModCultures.getVillageType(villageTypeId);
        if (villageType == null) {
            Player player = context.getPlayer();
            if (player instanceof ServerPlayer) {
                ServerPlayer player2 = (ServerPlayer)player;
                player2.sendSystemMessage((Component)Component.literal((String)("Village type not found: " + String.valueOf(villageTypeId))));
            }
            return InteractionResult.FAIL;
        }
        Component failure = VillageSpawner.spawnVillage(serverLevel, spawnPos, villageType);
        Player player = context.getPlayer();
        if (player instanceof ServerPlayer) {
            ServerPlayer player3 = (ServerPlayer)player;
            if (failure == null) {
                player3.sendSystemMessage((Component)Component.literal((String)("Village " + villageType.name() + " created at " + spawnPos.toShortString())));
                MillAdvancements.grant(player3, MillAdvancements.SUMMONING_WAND);
            } else {
                player3.sendSystemMessage(failure);
            }
        }
        return failure == null ? InteractionResult.SUCCESS : InteractionResult.FAIL;
    }

    private boolean isVillageTooClose(ServerLevel serverLevel, BlockPos spawnPos, UseOnContext context) {
        int minDistance;
        VillageManager villageManager = VillageSavedData.get(serverLevel).getVillageManager();
        if (villageManager.isWithinMinDistance(spawnPos, minDistance = MillenaireServerConfig.SERVER.minVillageDistance.getAsInt())) {
            Player player = context.getPlayer();
            if (player instanceof ServerPlayer) {
                ServerPlayer player2 = (ServerPlayer)player;
                player2.sendSystemMessage((Component)Component.literal((String)("A village already exists within " + minDistance + " blocks.")));
            }
            return true;
        }
        return false;
    }

    private void openBuildingDebugMenu(ServerLevel level, ServerPlayer player, BlockPos blockPos) {
        BuildingPlanSet planSet;
        BlockEntity be = level.getBlockEntity(blockPos);
        BuildingId buildingId = null;
        if (be instanceof LockedChestBlockEntity) {
            LockedChestBlockEntity chest = (LockedChestBlockEntity)be;
            buildingId = chest.getBuildingId();
        } else if (be instanceof VillagePanelBlockEntity) {
            VillagePanelBlockEntity panel = (VillagePanelBlockEntity)be;
            buildingId = panel.getBuildingId();
        }
        if (buildingId == null) {
            return;
        }
        Village village = VillageSavedData.get(level).getVillageManager().findVillageContaining(buildingId);
        if (village == null) {
            return;
        }
        BuildingInstance building = village.findBuildingById(buildingId);
        if (building == null) {
            return;
        }
        Object header = building.getPlanId().getPath();
        if (village.getVillageName() != null) {
            header = (String)header + " \u2014 " + village.getVillageName();
        }
        ArrayList<WandDebugMenuPayload.ActionEntry> actions = new ArrayList<WandDebugMenuPayload.ActionEntry>();
        actions.add(new WandDebugMenuPayload.ActionEntry("building_info", "wand_debug.building_info"));
        BuildingPlanSet buildingPlanSet = planSet = building.getPlanSetId() != null ? ModCultures.getBuildingPlanSet(building.getPlanSetId()) : null;
        if (planSet != null && planSet.isTownHall()) {
            actions.add(new WandDebugMenuPayload.ActionEntry("rush_construction", "wand_debug.rush_construction"));
            if (!village.isLoneBuilding()) {
                actions.add(new WandDebugMenuPayload.ActionEntry("fill_resources", "wand_debug.fill_resources"));
            }
        }
        if (village.getPathManager() != null) {
            actions.add(new WandDebugMenuPayload.ActionEntry("recalculate_paths", "wand_debug.recalculate_paths"));
            actions.add(new WandDebugMenuPayload.ActionEntry("clear_paths", "wand_debug.clear_paths"));
        }
        if (LocalMerchantHelper.getMerchantRecord(village, building) != null) {
            actions.add(new WandDebugMenuPayload.ActionEntry("force_merchant", "wand_debug.force_merchant"));
        }
        PacketDistributor.sendToPlayer((ServerPlayer)player, (CustomPacketPayload)new WandDebugMenuPayload(-1, blockPos, (String)header, actions), (CustomPacketPayload[])new CustomPacketPayload[0]);
    }

    private void openVillagerDebugMenu(ServerLevel level, ServerPlayer player, MillVillager villager) {
        String header = villager.getVillagerDisplayName();
        VillagerType vType = ModCultures.getVillagerType(villager.getVillagerTypeId());
        ArrayList<WandDebugMenuPayload.ActionEntry> actions = new ArrayList<WandDebugMenuPayload.ActionEntry>();
        actions.add(new WandDebugMenuPayload.ActionEntry("villager_info", "wand_debug.villager_info"));
        actions.add(new WandDebugMenuPayload.ActionEntry("nav_state", "wand_debug.nav_state"));
        if (vType != null && vType.isChild()) {
            actions.add(new WandDebugMenuPayload.ActionEntry("grow_child", "wand_debug.grow_child"));
        } else if (vType != null && (vType.maleChild() != null || vType.femaleChild() != null)) {
            actions.add(new WandDebugMenuPayload.ActionEntry("force_child", "wand_debug.force_child"));
        }
        actions.add(new WandDebugMenuPayload.ActionEntry("visualize_path", "wand_debug.visualize_path"));
        actions.add(new WandDebugMenuPayload.ActionEntry("visualize_waypoints", "wand_debug.visualize_waypoints"));
        actions.add(new WandDebugMenuPayload.ActionEntry("rebuild_waypoint_graph", "wand_debug.rebuild_waypoint_graph"));
        actions.add(new WandDebugMenuPayload.ActionEntry("clear_waypoint_blocks", "wand_debug.clear_waypoint_blocks"));
        PacketDistributor.sendToPlayer((ServerPlayer)player, (CustomPacketPayload)new WandDebugMenuPayload(villager.getId(), BlockPos.ZERO, header, actions), (CustomPacketPayload[])new CustomPacketPayload[0]);
    }

    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (player.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(target instanceof MillVillager)) {
            return InteractionResult.PASS;
        }
        MillVillager villager = (MillVillager)target;
        if (!(player instanceof ServerPlayer)) {
            return InteractionResult.PASS;
        }
        ServerPlayer serverPlayer = (ServerPlayer)player;
        if (!serverPlayer.hasPermissions(2) && !serverPlayer.server.isSingleplayer()) {
            return InteractionResult.PASS;
        }
        ServerLevel level = serverPlayer.serverLevel();
        this.openVillagerDebugMenu(level, serverPlayer, villager);
        return InteractionResult.SUCCESS;
    }

    private List<VillageType> getCompatibleVillageTypes(ServerLevel level, BlockPos pos) {
        Holder biome = level.getBiome(pos);
        ArrayList<VillageType> compatible = new ArrayList<VillageType>();
        for (VillageType vt : ModCultures.getAllVillageTypes().values()) {
            if (vt.weight() <= 0 && !vt.playerControlled() || !vt.spawnable() || vt.isHamlet()) continue;
            if (vt.biomeTags().isEmpty()) {
                compatible.add(vt);
                continue;
            }
            boolean matches = false;
            for (TagKey<Biome> tag : vt.biomeTags()) {
                if (!biome.is(tag)) continue;
                matches = true;
                break;
            }
            if (!matches) continue;
            compatible.add(vt);
        }
        return compatible;
    }

    private List<VillageType> getWandSelectableVillageTypes() {
        ArrayList<VillageType> selectable = new ArrayList<VillageType>();
        for (VillageType vt : ModCultures.getAllVillageTypes().values()) {
            if (!vt.spawnable() || vt.isHamlet()) continue;
            selectable.add(vt);
        }
        return selectable;
    }
}

