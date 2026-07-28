/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 */
package org.millenaire.goal.gathering;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import org.millenaire.goal.gathering.GatheringHandler;
import org.millenaire.goal.gathering.handler.BreedingHandler;
import org.millenaire.goal.gathering.handler.ChoppingHandler;
import org.millenaire.goal.gathering.handler.CocoaHarvestingHandler;
import org.millenaire.goal.gathering.handler.CocoaPlantingHandler;
import org.millenaire.goal.gathering.handler.CraftingHandler;
import org.millenaire.goal.gathering.handler.FishingHandler;
import org.millenaire.goal.gathering.handler.FishingInuitHandler;
import org.millenaire.goal.gathering.handler.FlowerPlantingHandler;
import org.millenaire.goal.gathering.handler.FruitHarvestingHandler;
import org.millenaire.goal.gathering.handler.HarvestingHandler;
import org.millenaire.goal.gathering.handler.MiningHandler;
import org.millenaire.goal.gathering.handler.PaddyHarvestingHandler;
import org.millenaire.goal.gathering.handler.PaddyPlantingHandler;
import org.millenaire.goal.gathering.handler.PlantingHandler;
import org.millenaire.goal.gathering.handler.SaplingPlantingHandler;
import org.millenaire.goal.gathering.handler.ShearingHandler;
import org.millenaire.goal.gathering.handler.SlaughterHandler;
import org.millenaire.goal.gathering.handler.SmeltingHandler;
import org.millenaire.goal.gathering.handler.TakeFromBuildingHandler;

public final class GatheringHandlerRegistry {
    private static final Map<String, GatheringHandler> HANDLERS = new ConcurrentHashMap<String, GatheringHandler>();

    private GatheringHandlerRegistry() {
    }

    public static void register(GatheringHandler handler) {
        HANDLERS.put(handler.id(), handler);
    }

    @Nullable
    public static GatheringHandler get(String id) {
        return HANDLERS.get(id);
    }

    public static List<String> getAllIds() {
        return HANDLERS.keySet().stream().sorted().toList();
    }

    public static void clear() {
        HANDLERS.values().forEach(GatheringHandler::onClear);
        HANDLERS.clear();
    }

    public static void registerDefaults() {
        GatheringHandlerRegistry.register(new HarvestingHandler());
        GatheringHandlerRegistry.register(new PlantingHandler());
        GatheringHandlerRegistry.register(new ChoppingHandler());
        GatheringHandlerRegistry.register(new SaplingPlantingHandler());
        GatheringHandlerRegistry.register(new BreedingHandler());
        GatheringHandlerRegistry.register(new MiningHandler());
        GatheringHandlerRegistry.register(new SlaughterHandler());
        GatheringHandlerRegistry.register(new CraftingHandler());
        GatheringHandlerRegistry.register(new ShearingHandler());
        GatheringHandlerRegistry.register(new FishingHandler());
        GatheringHandlerRegistry.register(new FishingInuitHandler());
        GatheringHandlerRegistry.register(new SmeltingHandler());
        GatheringHandlerRegistry.register(new FlowerPlantingHandler());
        GatheringHandlerRegistry.register(new FruitHarvestingHandler());
        GatheringHandlerRegistry.register(new CocoaHarvestingHandler());
        GatheringHandlerRegistry.register(new CocoaPlantingHandler());
        GatheringHandlerRegistry.register(new PaddyPlantingHandler());
        GatheringHandlerRegistry.register(new PaddyHarvestingHandler());
        GatheringHandlerRegistry.register(new TakeFromBuildingHandler());
    }
}

