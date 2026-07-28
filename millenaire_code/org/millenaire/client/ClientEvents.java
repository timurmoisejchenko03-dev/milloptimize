/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.model.geom.builders.CubeDeformation
 *  net.minecraft.client.renderer.BiomeColors
 *  net.minecraft.client.renderer.item.ItemProperties
 *  net.minecraft.core.BlockPos
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.packs.resources.PreparableReloadListener
 *  net.minecraft.util.Mth
 *  net.minecraft.world.entity.EntityType
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.item.BowItem
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.Items
 *  net.minecraft.world.level.BlockAndTintGetter
 *  net.minecraft.world.level.FoliageColor
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.entity.BlockEntityType
 *  net.neoforged.api.distmarker.Dist
 *  net.neoforged.bus.api.SubscribeEvent
 *  net.neoforged.fml.IExtensionPoint
 *  net.neoforged.fml.ModContainer
 *  net.neoforged.fml.common.EventBusSubscriber
 *  net.neoforged.fml.event.lifecycle.FMLClientSetupEvent
 *  net.neoforged.neoforge.client.event.ComputeFovModifierEvent
 *  net.neoforged.neoforge.client.event.EntityRenderersEvent$RegisterLayerDefinitions
 *  net.neoforged.neoforge.client.event.EntityRenderersEvent$RegisterRenderers
 *  net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent
 *  net.neoforged.neoforge.client.event.RegisterColorHandlersEvent$Block
 *  net.neoforged.neoforge.client.event.RegisterMenuScreensEvent
 *  net.neoforged.neoforge.client.gui.ConfigurationScreen
 *  net.neoforged.neoforge.client.gui.IConfigScreenFactory
 */
package org.millenaire.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.FoliageColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.IExtensionPoint;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import org.millenaire.Millenaire;
import org.millenaire.block.ModBlockEntities;
import org.millenaire.block.ModBlocks;
import org.millenaire.client.LanguageFallbackListener;
import org.millenaire.client.gui.FirePitScreen;
import org.millenaire.client.gui.LockedChestScreen;
import org.millenaire.client.gui.TradeScreen;
import org.millenaire.client.model.MillFemaleAsymModel;
import org.millenaire.client.model.MillFemaleSymModel;
import org.millenaire.client.model.MillMaleModel;
import org.millenaire.client.model.MillModelLayers;
import org.millenaire.client.render.FirePitRenderer;
import org.millenaire.client.render.LockedChestRenderer;
import org.millenaire.client.render.MillVillagerRenderer;
import org.millenaire.client.render.MillWallDecorationRenderer;
import org.millenaire.client.render.VillagePanelRenderer;
import org.millenaire.commerce.ModMenuTypes;
import org.millenaire.entity.ModEntities;
import org.millenaire.item.ModItems;

@EventBusSubscriber(modid="millenaire", value={Dist.CLIENT})
public final class ClientEvents {
    private ClientEvents() {
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer((EntityType)ModEntities.MILL_VILLAGER.get(), MillVillagerRenderer::new);
        event.registerEntityRenderer((EntityType)ModEntities.WALL_DECORATION.get(), MillWallDecorationRenderer::new);
        event.registerBlockEntityRenderer((BlockEntityType)ModBlockEntities.LOCKED_CHEST.get(), LockedChestRenderer::new);
        event.registerBlockEntityRenderer((BlockEntityType)ModBlockEntities.VILLAGE_PANEL.get(), VillagePanelRenderer::new);
        event.registerBlockEntityRenderer((BlockEntityType)ModBlockEntities.FIRE_PIT.get(), FirePitRenderer::new);
    }

    @SubscribeEvent
    public static void onRegisterLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        CubeDeformation none = CubeDeformation.NONE;
        CubeDeformation cloth0 = new CubeDeformation(0.1f);
        CubeDeformation cloth1 = new CubeDeformation(0.2f);
        event.registerLayerDefinition(MillModelLayers.MILL_VILLAGER_MALE, () -> MillMaleModel.createBodyLayer(none));
        event.registerLayerDefinition(MillModelLayers.MILL_VILLAGER_FEMALE_SYM, () -> MillFemaleSymModel.createBodyLayer(none));
        event.registerLayerDefinition(MillModelLayers.MILL_VILLAGER_FEMALE_ASYM, () -> MillFemaleAsymModel.createBodyLayer(none));
        event.registerLayerDefinition(MillModelLayers.MILL_VILLAGER_MALE_CLOTH_0, () -> MillMaleModel.createBodyLayer(cloth0));
        event.registerLayerDefinition(MillModelLayers.MILL_VILLAGER_FEMALE_SYM_CLOTH_0, () -> MillFemaleSymModel.createBodyLayer(cloth0));
        event.registerLayerDefinition(MillModelLayers.MILL_VILLAGER_FEMALE_ASYM_CLOTH_0, () -> MillFemaleAsymModel.createBodyLayer(cloth0));
        event.registerLayerDefinition(MillModelLayers.MILL_VILLAGER_MALE_CLOTH_1, () -> MillMaleModel.createBodyLayer(cloth1));
        event.registerLayerDefinition(MillModelLayers.MILL_VILLAGER_FEMALE_SYM_CLOTH_1, () -> MillFemaleSymModel.createBodyLayer(cloth1));
        event.registerLayerDefinition(MillModelLayers.MILL_VILLAGER_FEMALE_ASYM_CLOTH_1, () -> MillFemaleAsymModel.createBodyLayer(cloth1));
    }

    @SubscribeEvent
    public static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.TRADE.get(), TradeScreen::new);
        event.register(ModMenuTypes.LOCKED_CHEST.get(), LockedChestScreen::new);
        event.register(ModMenuTypes.FIRE_PIT.get(), FirePitScreen::new);
    }

    @SubscribeEvent
    public static void onRegisterBlockColors(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tintIndex) -> {
            if (level == null || pos == null) {
                return FoliageColor.getDefaultColor();
            }
            return BiomeColors.getAverageFoliageColor((BlockAndTintGetter)level, (BlockPos)pos);
        }, new Block[]{(Block)ModBlocks.PISTACHIO_TREE_LEAVES.get(), (Block)ModBlocks.APPLE_TREE_LEAVES.get(), (Block)ModBlocks.OLIVE_TREE_LEAVES.get()});
    }

    @SubscribeEvent
    public static void onRegisterReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((PreparableReloadListener)new LanguageFallbackListener());
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        ModContainer container = Millenaire.getModContainer();
        container.registerExtensionPoint(IConfigScreenFactory.class, (IExtensionPoint)((IConfigScreenFactory)(mc, parent) -> new ConfigurationScreen(container, parent)));
        event.enqueueWork(() -> {
            ClientEvents.registerBowProperties((BowItem)ModItems.YUMIBOW.get());
            ClientEvents.registerBowProperties((BowItem)ModItems.SELJUK_BOW.get());
            ClientEvents.registerBowProperties((BowItem)ModItems.INUIT_BOW.get());
        });
    }

    private static void registerBowProperties(BowItem bow) {
        ItemProperties.register((Item)bow, (ResourceLocation)ResourceLocation.withDefaultNamespace((String)"pull"), (stack, level, entity, seed) -> {
            if (entity == null) {
                return 0.0f;
            }
            if (entity.getUseItem() != stack) {
                return 0.0f;
            }
            return (float)(stack.getUseDuration(entity) - entity.getUseItemRemainingTicks()) / 20.0f;
        });
        ItemProperties.register((Item)bow, (ResourceLocation)ResourceLocation.withDefaultNamespace((String)"pulling"), (stack, level, entity, seed) -> entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1.0f : 0.0f);
    }

    @SubscribeEvent
    public static void onComputeFovModifier(ComputeFovModifierEvent event) {
        Player player = event.getPlayer();
        if (!player.isUsingItem()) {
            return;
        }
        ItemStack used = player.getUseItem();
        if (used.is(Items.BOW)) {
            return;
        }
        if (!(used.getItem() instanceof BowItem)) {
            return;
        }
        float vanillaRaw = event.getFovModifier();
        float withBow = vanillaRaw * ClientEvents.computeBowFovMultiplier(player.getTicksUsingItem());
        float scale = ((Double)Minecraft.getInstance().options.fovEffectScale().get()).floatValue();
        event.setNewFovModifier(Mth.lerp((float)scale, (float)1.0f, (float)withBow));
    }

    public static float computeBowFovMultiplier(int ticksUsingItem) {
        float charge = (float)ticksUsingItem / 20.0f;
        charge = charge > 1.0f ? 1.0f : (charge *= charge);
        return 1.0f - charge * 0.15f;
    }
}

