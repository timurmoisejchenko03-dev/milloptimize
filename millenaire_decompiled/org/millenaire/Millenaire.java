/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.CommandDispatcher
 *  com.mojang.logging.LogUtils
 *  javax.annotation.Nullable
 *  net.minecraft.commands.CommandSourceStack
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Holder
 *  net.minecraft.network.protocol.common.custom.CustomPacketPayload
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.tags.BlockTags
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.Items
 *  net.minecraft.world.level.ChunkPos
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.LevelAccessor
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraft.world.level.block.entity.BlockEntityType
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.properties.Property
 *  net.neoforged.bus.api.EventPriority
 *  net.neoforged.bus.api.IEventBus
 *  net.neoforged.fml.ModContainer
 *  net.neoforged.fml.common.Mod
 *  net.neoforged.fml.config.IConfigSpec
 *  net.neoforged.fml.config.ModConfig$Type
 *  net.neoforged.neoforge.capabilities.Capabilities$ItemHandler
 *  net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent
 *  net.neoforged.neoforge.common.NeoForge
 *  net.neoforged.neoforge.common.world.chunk.RegisterTicketControllersEvent
 *  net.neoforged.neoforge.event.RegisterCommandsEvent
 *  net.neoforged.neoforge.event.entity.EntityJoinLevelEvent
 *  net.neoforged.neoforge.event.entity.living.LivingDamageEvent$Pre
 *  net.neoforged.neoforge.event.entity.living.LivingDropsEvent
 *  net.neoforged.neoforge.event.server.ServerStartedEvent
 *  net.neoforged.neoforge.network.PacketDistributor
 *  org.slf4j.Logger
 */
package org.millenaire;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.logging.LogUtils;
import java.util.HashSet;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.IConfigSpec;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.world.chunk.RegisterTicketControllersEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.millenaire.advancement.AdvancementReporter;
import org.millenaire.advancement.MillAdvancements;
import org.millenaire.advancement.UsageReporter;
import org.millenaire.block.BlockSilkWorm;
import org.millenaire.block.BlockSnailSoil;
import org.millenaire.block.BlockWetBrick;
import org.millenaire.block.ModBlockEntities;
import org.millenaire.block.ModBlocks;
import org.millenaire.building.BuildingInstance;
import org.millenaire.building.MillenaireStructureLoader;
import org.millenaire.command.ConvertAddonCommand;
import org.millenaire.command.DevCommand;
import org.millenaire.command.QueryCommand;
import org.millenaire.command.ReputationCommand;
import org.millenaire.command.SpawnLoneBuildingCommand;
import org.millenaire.command.SpawnVillageCommand;
import org.millenaire.command.TestCommand;
import org.millenaire.command.TpCommand;
import org.millenaire.command.VillagesCommand;
import org.millenaire.commerce.ModMenuTypes;
import org.millenaire.config.MillenaireClientConfig;
import org.millenaire.config.MillenaireCommonConfig;
import org.millenaire.config.MillenaireServerConfig;
import org.millenaire.config.VillagerConfig;
import org.millenaire.content.ContentDirectoryManager;
import org.millenaire.content.ContentStatsReporter;
import org.millenaire.content.NativeContentDeployer;
import org.millenaire.content.ReferenceCatalogGenerator;
import org.millenaire.content.ValidationReport;
import org.millenaire.content.legacy.LegacyAutoConverter;
import org.millenaire.culture.CultureLoader;
import org.millenaire.culture.ModCultures;
import org.millenaire.culture.VillagerType;
import org.millenaire.diagnostics.WaypointVisualizationManager;
import org.millenaire.entity.BuilderSafetyHandler;
import org.millenaire.entity.MillVillager;
import org.millenaire.entity.ModEntities;
import org.millenaire.entity.VillagerAppearanceFactory;
import org.millenaire.goal.GoalRegistry;
import org.millenaire.goal.gathering.GatheringHandlerRegistry;
import org.millenaire.goal.gathering.GatheringTypeLoader;
import org.millenaire.goal.impl.BringBackHomeGoal;
import org.millenaire.goal.impl.BuildGoal;
import org.millenaire.goal.impl.BuildPathGoal;
import org.millenaire.goal.impl.ChatGoal;
import org.millenaire.goal.impl.ChildBecomeAdultGoal;
import org.millenaire.goal.impl.ClearOldPathGoal;
import org.millenaire.goal.impl.DefendVillageGoal;
import org.millenaire.goal.impl.DeliverResourcesToShopGoal;
import org.millenaire.goal.impl.EngageTargetGoal;
import org.millenaire.goal.impl.ForeignMerchantKeepStallGoal;
import org.millenaire.goal.impl.GatherGoodsGoal;
import org.millenaire.goal.impl.GetGoodsForHouseholdGoal;
import org.millenaire.goal.impl.GetResourcesForShopsGoal;
import org.millenaire.goal.impl.GetToolGoal;
import org.millenaire.goal.impl.HideGoal;
import org.millenaire.goal.impl.HiredEscortGoal;
import org.millenaire.goal.impl.HostileVillagerGoal;
import org.millenaire.goal.impl.HuntMonsterGoal;
import org.millenaire.goal.impl.IdleGoal;
import org.millenaire.goal.impl.LightHearthGoal;
import org.millenaire.goal.impl.MerchantVisitBuildingGoal;
import org.millenaire.goal.impl.MerchantVisitInnGoal;
import org.millenaire.goal.impl.RaidVillageGoal;
import org.millenaire.goal.impl.RestGoal;
import org.millenaire.goal.impl.SellerGoal;
import org.millenaire.goal.impl.SocialiseGoal;
import org.millenaire.goal.impl.SpotGatheringGoal;
import org.millenaire.goal.visit.VisitGoalLoader;
import org.millenaire.goal.visit.VisitGoalValidator;
import org.millenaire.item.InuitHuntingDropHandler;
import org.millenaire.item.ItemHelper;
import org.millenaire.item.ModCreativeTabs;
import org.millenaire.item.ModItems;
import org.millenaire.language.I18nKeyAuditor;
import org.millenaire.language.ServerTranslationCache;
import org.millenaire.map.VillageMapDecorationTypes;
import org.millenaire.map.VillageMapMarkerService;
import org.millenaire.net.DonorChecker;
import org.millenaire.net.VersionChecker;
import org.millenaire.network.ModPayloads;
import org.millenaire.network.QuestNetworkHelper;
import org.millenaire.quest.QuestInstance;
import org.millenaire.quest.QuestLoader;
import org.millenaire.quest.QuestManager;
import org.millenaire.quest.QuestRegistry;
import org.millenaire.tag.ModTags;
import org.millenaire.test.TestPlayerManager;
import org.millenaire.tool.ToolCategoryRegistry;
import org.millenaire.village.PlayerQuestData;
import org.millenaire.village.TravelBookNavigationState;
import org.millenaire.village.VillageChunkLoader;
import org.millenaire.village.VillageSavedData;
import org.millenaire.world.BiomeTagDisjointnessValidator;
import org.millenaire.world.ModProcessors;
import org.millenaire.world.VillageSpawnQueue;
import org.slf4j.Logger;

@Mod(value="millenaire")
public class Millenaire {
    public static final String MODID = "millenaire";
    private static final Logger LOGGER = LogUtils.getLogger();
    private static GoalRegistry goalRegistryInstance;
    private static ModContainer modContainerInstance;
    private static VillageSpawnQueue spawnQueueInstance;
    private static volatile boolean naturalSpawnSuppressed;

    public static void setNaturalSpawnSuppressed(boolean suppressed) {
        naturalSpawnSuppressed = suppressed;
    }

    public static GoalRegistry getGoalRegistry() {
        return goalRegistryInstance;
    }

    public static ModContainer getModContainer() {
        return modContainerInstance;
    }

    public static String getModVersion() {
        return modContainerInstance.getModInfo().getVersion().toString();
    }

    @Nullable
    public static VillageSpawnQueue getSpawnQueue() {
        return spawnQueueInstance;
    }

    public Millenaire(IEventBus modEventBus, ModContainer modContainer) {
        VillageSpawnQueue spawnQueue;
        modContainerInstance = modContainer;
        LOGGER.info("Mill\u00e9naire {} \u2014 Initialisation", (Object)modContainer.getModInfo().getVersion());
        modContainer.registerConfig(ModConfig.Type.SERVER, (IConfigSpec)MillenaireServerConfig.SPEC);
        modContainer.registerConfig(ModConfig.Type.COMMON, (IConfigSpec)MillenaireCommonConfig.SPEC);
        modContainer.registerConfig(ModConfig.Type.CLIENT, (IConfigSpec)MillenaireClientConfig.SPEC);
        modEventBus.addListener(event -> {
            if (event.getConfig().getType() == ModConfig.Type.COMMON) {
                MillenaireCommonConfig.COMMON.bindLogCategories();
            }
        });
        modEventBus.addListener(event -> {
            if (event.getConfig().getType() == ModConfig.Type.COMMON) {
                MillenaireCommonConfig.COMMON.bindLogCategories();
            }
        });
        ModBlocks.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModEntities.register(modEventBus);
        ModItems.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        ModMenuTypes.MENU_TYPES.register(modEventBus);
        ModProcessors.register(modEventBus);
        MillAdvancements.register(modEventBus);
        VillageMapDecorationTypes.register(modEventBus);
        modEventBus.addListener(ModPayloads::register);
        modEventBus.addListener(RegisterCapabilitiesEvent.class, event -> event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, (BlockEntityType)ModBlockEntities.LOCKED_CHEST.get(), (blockEntity, side) -> null));
        modEventBus.addListener(RegisterTicketControllersEvent.class, event -> event.register(VillageChunkLoader.getController()));
        GoalRegistry goalRegistry = new GoalRegistry();
        goalRegistry.register(new IdleGoal());
        goalRegistry.register(new RestGoal());
        goalRegistry.register(new BuildGoal());
        goalRegistry.register(new SocialiseGoal());
        goalRegistry.register(new ChatGoal());
        goalRegistry.register(new SellerGoal());
        goalRegistry.register(new ChildBecomeAdultGoal());
        goalRegistry.register(new BringBackHomeGoal());
        goalRegistry.register(new DeliverResourcesToShopGoal());
        goalRegistry.register(new GetResourcesForShopsGoal());
        goalRegistry.register(new GetGoodsForHouseholdGoal());
        goalRegistry.register(new GetToolGoal());
        goalRegistry.register(new GatherGoodsGoal());
        goalRegistry.register(new LightHearthGoal());
        goalRegistry.register(new HuntMonsterGoal());
        goalRegistry.register(new RaidVillageGoal());
        goalRegistry.register(new DefendVillageGoal());
        goalRegistry.register(new HideGoal());
        goalRegistry.register(new HostileVillagerGoal());
        goalRegistry.register(new EngageTargetGoal());
        goalRegistry.register(new HiredEscortGoal());
        goalRegistry.register(new SpotGatheringGoal(ResourceLocation.fromNamespaceAndPath((String)MODID, (String)"gather_silk"), "silkwormfarm", 2, 100, 2, SpotGatheringGoal.StockSource.FARM, () -> (Item)ModItems.SILK.get(), 128, BuildingInstance::getSilkwormBlockPositions, (level, pos) -> {
            BlockState st = level.getBlockState(pos);
            return st.is((Block)ModBlocks.SILK_WORM.get()) && (Integer)st.getValue((Property)BlockSilkWorm.AGE) == 3;
        }, (ctx, pos) -> {
            BlockState st = ctx.level().getBlockState(pos);
            if (st.is((Block)ModBlocks.SILK_WORM.get()) && (Integer)st.getValue((Property)BlockSilkWorm.AGE) == 3) {
                ctx.level().setBlock(pos, (BlockState)st.setValue((Property)BlockSilkWorm.AGE, (Comparable)Integer.valueOf(0)), 2);
                ctx.villager().getInventory().add((Item)ModItems.SILK.get(), 1);
            }
        }, false));
        goalRegistry.register(new SpotGatheringGoal(ResourceLocation.fromNamespaceAndPath((String)MODID, (String)"gather_snails"), "snailsfarm", 2, 100, 2, SpotGatheringGoal.StockSource.FARM, () -> Items.PURPLE_DYE, 128, BuildingInstance::getSnailSoilBlockPositions, (level, pos) -> {
            BlockState st = level.getBlockState(pos);
            return st.is((Block)ModBlocks.SNAIL_SOIL.get()) && (Integer)st.getValue((Property)BlockSnailSoil.AGE) == 3;
        }, (ctx, pos) -> {
            BlockState st = ctx.level().getBlockState(pos);
            if (st.is((Block)ModBlocks.SNAIL_SOIL.get()) && (Integer)st.getValue((Property)BlockSnailSoil.AGE) == 3) {
                ctx.level().setBlock(pos, (BlockState)st.setValue((Property)BlockSnailSoil.AGE, (Comparable)Integer.valueOf(0)), 2);
                ctx.villager().getInventory().add(Items.PURPLE_DYE, 1);
            }
        }, false));
        goalRegistry.register(new SpotGatheringGoal(ResourceLocation.fromNamespaceAndPath((String)MODID, (String)"gather_brick"), "brickkiln", 1, 100, 2, SpotGatheringGoal.StockSource.TOWNHALL, () -> ((Block)ModBlocks.MUD_BRICK.get()).asItem(), 4096, BuildingInstance::getBrickSpotPositions, (level, pos) -> level.getBlockState(pos).is((Block)ModBlocks.MUD_BRICK.get()), (ctx, pos) -> {
            if (ctx.level().getBlockState(pos).is((Block)ModBlocks.MUD_BRICK.get())) {
                ctx.villager().getInventory().add(((Block)ModBlocks.MUD_BRICK.get()).asItem(), 1);
                ctx.level().setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            }
        }, true));
        goalRegistry.register(new SpotGatheringGoal(ResourceLocation.fromNamespaceAndPath((String)MODID, (String)"dry_brick"), "brickkiln", 1, 120, 0, SpotGatheringGoal.StockSource.NONE, null, 0, BuildingInstance::getBrickSpotPositions, (level, pos) -> level.getBlockState(pos).isAir(), (ctx, pos) -> {
            if (ctx.level().getBlockState(pos).isAir()) {
                ctx.level().setBlock(pos, ((BlockWetBrick)((Object)((Object)ModBlocks.WET_BRICK.get()))).defaultBlockState(), 3);
            }
        }, true));
        goalRegistry.register(new SpotGatheringGoal(ResourceLocation.fromNamespaceAndPath((String)MODID, (String)"harvest_sugar_cane"), "sugarplantation", 3, 200, 4, SpotGatheringGoal.StockSource.TOWNHALL, () -> Items.SUGAR_CANE, 0, b -> b.getSoilPositions("sugarcane"), (level, pos) -> level.getBlockState(pos.above(2)).is(Blocks.SUGAR_CANE), (ctx, pos) -> {
            int irrigation = ctx.village().getVillageIrrigation();
            BlockPos top = pos.above(3);
            if (ctx.level().getBlockState(top).is(Blocks.SUGAR_CANE)) {
                ctx.level().setBlock(top, Blocks.AIR.defaultBlockState(), 3);
                int nbCrop = 1;
                if (Math.random() < (double)irrigation / 100.0) {
                    ++nbCrop;
                }
                ctx.villager().getInventory().add(Items.SUGAR_CANE, nbCrop);
            }
            BlockPos mid = pos.above(2);
            if (ctx.level().getBlockState(mid).is(Blocks.SUGAR_CANE)) {
                ctx.level().setBlock(mid, Blocks.AIR.defaultBlockState(), 3);
                int nbCrop = 1;
                if (Math.random() < (double)irrigation / 100.0) {
                    ++nbCrop;
                }
                ctx.villager().getInventory().add(Items.SUGAR_CANE, nbCrop);
            }
        }, true));
        goalRegistry.register(new SpotGatheringGoal(ResourceLocation.fromNamespaceAndPath((String)MODID, (String)"plant_sugar_cane"), "sugarplantation", 3, 120, 0, SpotGatheringGoal.StockSource.NONE, null, 0, b -> b.getSoilPositions("sugarcane"), (level, pos) -> level.getBlockState(pos.above()).isAir(), (ctx, pos) -> {
            BlockPos cropPos = pos.above();
            BlockState state = ctx.level().getBlockState(cropPos);
            if (state.isAir() || state.is(BlockTags.LEAVES)) {
                ctx.level().setBlock(cropPos, Blocks.SUGAR_CANE.defaultBlockState(), 3);
            }
        }, true));
        goalRegistry.register(new ClearOldPathGoal());
        goalRegistry.register(new BuildPathGoal());
        VisitGoalLoader.loadAll(goalRegistry);
        goalRegistry.register(new ForeignMerchantKeepStallGoal());
        goalRegistry.register(new MerchantVisitInnGoal());
        goalRegistry.register(new MerchantVisitBuildingGoal());
        GatheringHandlerRegistry.registerDefaults();
        GatheringTypeLoader.loadAll(goalRegistry);
        goalRegistry.freezeBuiltins();
        goalRegistryInstance = goalRegistry;
        NeoForge.EVENT_BUS.addListener(LivingDamageEvent.Pre.class, event -> {
            Entity patt0$temp = event.getSource().getEntity();
            if (patt0$temp instanceof ServerPlayer) {
                ItemStack weapon;
                ServerPlayer attacker = (ServerPlayer)patt0$temp;
                if (event.getEntity() instanceof ServerPlayer && (weapon = attacker.getMainHandItem()).is(ModTags.Items.CULTURE_WEAPONS)) {
                    MillAdvancements.grant(attacker, MillAdvancements.MP_WEAPON);
                }
            }
        });
        NeoForge.EVENT_BUS.addListener(LivingDropsEvent.class, InuitHuntingDropHandler::onLivingDrops);
        NeoForge.EVENT_BUS.addListener(LivingDamageEvent.Pre.class, BuilderSafetyHandler::onLivingDamage);
        NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, RegisterCommandsEvent.class, event -> {
            SpawnVillageCommand.register((CommandDispatcher<CommandSourceStack>)event.getDispatcher());
            SpawnLoneBuildingCommand.register((CommandDispatcher<CommandSourceStack>)event.getDispatcher());
            ReputationCommand.register((CommandDispatcher<CommandSourceStack>)event.getDispatcher());
            VillagesCommand.register((CommandDispatcher<CommandSourceStack>)event.getDispatcher());
            QueryCommand.register((CommandDispatcher<CommandSourceStack>)event.getDispatcher());
            TestCommand.register((CommandDispatcher<CommandSourceStack>)event.getDispatcher());
            DevCommand.register((CommandDispatcher<CommandSourceStack>)event.getDispatcher());
            TpCommand.register((CommandDispatcher<CommandSourceStack>)event.getDispatcher());
        });
        NeoForge.EVENT_BUS.addListener(event -> {
            TestPlayerManager.onServerStopping();
            TravelBookNavigationState.clearAll();
            ConvertAddonCommand.onServerStopping(event);
            MillenaireStructureLoader.clearCache();
            ItemHelper.clearCache();
            VersionChecker.reset();
            UsageReporter.reset();
        });
        NeoForge.EVENT_BUS.addListener(event -> {
            Player patt0$temp = event.getEntity();
            if (patt0$temp instanceof ServerPlayer) {
                ServerPlayer serverPlayer = (ServerPlayer)patt0$temp;
                TravelBookNavigationState.clear(serverPlayer.getUUID());
            }
        });
        NeoForge.EVENT_BUS.addListener(event -> {
            Player patt0$temp = event.getEntity();
            if (patt0$temp instanceof ServerPlayer) {
                ServerPlayer serverPlayer = (ServerPlayer)patt0$temp;
                ContentStatsReporter.notifyOpIfRelevant(serverPlayer);
                VersionChecker.onPlayerLogin(serverPlayer);
                DonorChecker.onPlayerLogin(serverPlayer);
                AdvancementReporter.onPlayerLogin(serverPlayer);
                ServerLevel overworld = serverPlayer.server.getLevel(Level.OVERWORLD);
                if (overworld != null) {
                    PlayerQuestData data = PlayerQuestData.get(overworld, QuestRegistry::get);
                    for (QuestInstance qi : data.getActiveQuests(serverPlayer.getUUID())) {
                        PacketDistributor.sendToPlayer((ServerPlayer)serverPlayer, (CustomPacketPayload)QuestNetworkHelper.buildSyncPayload(qi, serverPlayer), (CustomPacketPayload[])new CustomPacketPayload[0]);
                    }
                }
            }
        });
        spawnQueueInstance = spawnQueue = new VillageSpawnQueue();
        NeoForge.EVENT_BUS.addListener(event -> {
            UsageReporter.tryReport(event.getServer());
            TestPlayerManager.acknowledgeChunkBatch();
            ServerLevel overworld = event.getServer().getLevel(Level.OVERWORLD);
            if (overworld != null) {
                VillageSavedData.get(overworld).getVillageManager().tick(overworld);
                if (!naturalSpawnSuppressed) {
                    spawnQueue.trySpawnNext(overworld);
                }
            }
            for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
                ServerLevel playerLevel = player.serverLevel();
                QuestManager.tickQuests(player, playerLevel);
                QuestManager.tickSpecialActions(player, playerLevel);
            }
            WaypointVisualizationManager.tick(event.getServer());
            if (overworld != null && overworld.getGameTime() % 20L == 0L) {
                VillageMapMarkerService.processAllPlayers(event.getServer());
            }
        });
        NeoForge.EVENT_BUS.addListener(event -> {
            ServerLevel serverLevel;
            if (naturalSpawnSuppressed) {
                return;
            }
            LevelAccessor patt0$temp = event.getLevel();
            if (patt0$temp instanceof ServerLevel && (serverLevel = (ServerLevel)patt0$temp).dimension() == Level.OVERWORLD) {
                ChunkPos chunkPos = event.getChunk().getPos();
                boolean isNewChunk = event.isNewChunk();
                serverLevel.getServer().execute(() -> {
                    spawnQueue.markResolved(chunkPos);
                    if (!isNewChunk) {
                        return;
                    }
                    BlockPos biomeSamplePos = chunkPos.getMiddleBlockPosition(serverLevel.getMaxBuildHeight() - 1);
                    Holder centerBiome = serverLevel.getBiome(biomeSamplePos);
                    if (!VillageSpawnQueue.couldHostAnyVillage(ModCultures.getAllVillageTypes().values(), tag -> centerBiome.is(tag))) {
                        return;
                    }
                    spawnQueue.enqueue(chunkPos.getMiddleBlockPosition(64));
                });
            }
        });
        NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, ServerStartedEvent.class, event -> {
            ContentDirectoryManager.init(event.getServer());
            NativeContentDeployer.deployIfNeeded();
            LegacyAutoConverter.convertIfNeeded();
            ToolCategoryRegistry.load();
            ServerTranslationCache.load();
            VillagerConfig.load();
            goalRegistryInstance.resetToBuiltins();
            CultureLoader.loadAll();
            GatheringTypeLoader.loadExternal(goalRegistryInstance);
            VisitGoalLoader.loadExternalPerCultureGoals(goalRegistryInstance, CultureLoader.discoverCultures());
            CultureLoader.validateAll(goalRegistryInstance);
            GatheringTypeLoader.validateAll(goalRegistryInstance);
            VisitGoalValidator.validateAll(goalRegistryInstance);
            QuestRegistry.clear();
            QuestLoader.loadAll();
            ValidationReport.generate(CultureLoader.discoverCultures(), new HashSet<String>(CultureLoader.BUILTIN_CULTURES));
            ReferenceCatalogGenerator.generate(goalRegistryInstance);
            I18nKeyAuditor.audit();
            ServerLevel overworld = event.getServer().getLevel(Level.OVERWORLD);
            if (overworld != null) {
                BiomeTagDisjointnessValidator.validate(overworld.registryAccess());
                CultureLoader.rehydrateConstructionTasks(overworld);
                this.initVillagerGoals(overworld);
            }
        });
        NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, EntityJoinLevelEvent.class, event -> {
            Entity patt0$temp = event.getEntity();
            if (patt0$temp instanceof MillVillager) {
                VillagerType vType;
                MillVillager villager = (MillVillager)patt0$temp;
                if (!event.getLevel().isClientSide() && villager.getVillagerTypeId() != null && (vType = ModCultures.getVillagerType(villager.getVillagerTypeId())) != null) {
                    villager.syncChiefFlag(vType);
                    if (villager.getGoalScheduler() == null && goalRegistryInstance != null) {
                        villager.initGoals(goalRegistryInstance, vType);
                        LOGGER.debug("Goals reinitialized for villager {} (loaded from chunk)", (Object)villager.getVillagerTypeId());
                    }
                }
            }
        });
    }

    private void initVillagerGoals(ServerLevel level) {
        int count = 0;
        for (Entity entity : level.getAllEntities()) {
            VillagerType vType;
            MillVillager villager;
            if (!(entity instanceof MillVillager) || (villager = (MillVillager)entity).getVillagerTypeId() == null || (vType = ModCultures.getVillagerType(villager.getVillagerTypeId())) == null) continue;
            villager.initGoals(goalRegistryInstance, vType);
            villager.syncChiefFlag(vType);
            if (villager.getTexture() == null) {
                VillagerAppearanceFactory.randomizeAppearance(villager, vType);
            }
            ++count;
        }
        if (count > 0) {
            LOGGER.info("Goals initialized for {} existing villagers", (Object)count);
        }
    }

    static {
        naturalSpawnSuppressed = false;
    }
}

