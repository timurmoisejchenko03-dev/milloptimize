/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonObject
 *  com.mojang.logging.LogUtils
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.util.GsonHelper
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.properties.IntegerProperty
 *  net.minecraft.world.level.block.state.properties.Property
 *  org.slf4j.Logger
 */
package org.millenaire.goal.gathering.handler;

import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import org.millenaire.goal.GoalContext;
import org.millenaire.goal.gathering.GatheringTarget;
import org.millenaire.goal.gathering.GatheringType;
import org.millenaire.goal.gathering.handler.AbstractGatheringHandler;
import org.millenaire.item.BlockHelper;
import org.slf4j.Logger;

public class HarvestingHandler
extends AbstractGatheringHandler {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final Map<ResourceLocation, Predicate<BlockState>> predicateCache = new ConcurrentHashMap<ResourceLocation, Predicate<BlockState>>();

    @Override
    public String id() {
        return "harvesting";
    }

    @Override
    public String getHeldToolCategoryId(GatheringType type) {
        return "toolshoe";
    }

    @Override
    public List<String> validate(GatheringType type) {
        return HarvestingHandler.validateBlockParam(type, "targetBlock", true);
    }

    @Override
    public boolean canStart(GoalContext ctx, GatheringType type) {
        Predicate<BlockState> predicate = this.getCachedPredicate(type);
        List<BlockPos> soilPositions = this.collectSoilPositions(ctx, type);
        for (BlockPos soil : soilPositions) {
            BlockPos above = soil.above();
            if (!ctx.level().isLoaded(above) || !predicate.test(ctx.level().getBlockState(above))) continue;
            return true;
        }
        return false;
    }

    @Override
    @Nullable
    public GatheringTarget findTarget(GoalContext ctx, GatheringType type, @Nullable GatheringTarget lastTarget) {
        Predicate<BlockState> predicate = this.getCachedPredicate(type);
        ServerLevel level = ctx.level();
        List<BlockPos> soilPositions = this.collectSoilPositions(ctx, type);
        ArrayList<BlockPos> abovePositions = new ArrayList<BlockPos>(soilPositions.size());
        for (BlockPos soil : soilPositions) {
            abovePositions.add(soil.above());
        }
        BlockPos reference = lastTarget != null ? lastTarget.navigationPos() : ctx.villager().blockPosition();
        BlockPos best = HarvestingHandler.findClosestBlock(abovePositions, pos -> level.isLoaded(pos) && predicate.test(level.getBlockState(pos)), reference, lastTarget, type.batchRadius());
        return best != null ? new GatheringTarget.BlockTarget(best) : null;
    }

    @Override
    public boolean performAction(GoalContext ctx, GatheringType type, GatheringTarget target) {
        if (!(target instanceof GatheringTarget.BlockTarget)) {
            return true;
        }
        GatheringTarget.BlockTarget blockTarget = (GatheringTarget.BlockTarget)target;
        BlockPos pos = blockTarget.pos();
        ServerLevel level = ctx.level();
        Predicate<BlockState> predicate = this.getCachedPredicate(type);
        if (!predicate.test(level.getBlockState(pos))) {
            return true;
        }
        BlockState state = level.getBlockState(pos);
        level.destroyBlock(pos, false);
        boolean skipDrops = GsonHelper.getAsBoolean((JsonObject)type.handlerParams(), (String)"skipDrops", (boolean)false);
        if (!skipDrops) {
            List drops = Block.getDrops((BlockState)state, (ServerLevel)level, (BlockPos)pos, null);
            for (ItemStack drop : drops) {
                ctx.villager().getInventory().add(drop.getItem(), drop.getCount());
            }
            this.applyIrrigationBonus(ctx, type);
        }
        return true;
    }

    @Override
    public void onClear() {
        this.predicateCache.clear();
    }

    private Predicate<BlockState> getCachedPredicate(GatheringType type) {
        return this.predicateCache.computeIfAbsent(type.id(), id -> this.buildPredicate(type));
    }

    private Predicate<BlockState> buildPredicate(GatheringType type) {
        JsonObject targetState;
        JsonObject params = type.handlerParams();
        String targetBlockId = GsonHelper.getAsString((JsonObject)params, (String)"targetBlock", null);
        JsonObject jsonObject = targetState = params.has("targetState") ? GsonHelper.getAsJsonObject((JsonObject)params, (String)"targetState") : null;
        if (targetBlockId == null) {
            return state -> false;
        }
        Block targetBlock = BlockHelper.resolve(targetBlockId);
        if (targetBlock == null) {
            return state -> false;
        }
        return state -> {
            if (!state.is(targetBlock)) {
                return false;
            }
            if (targetState != null) {
                for (String key : targetState.keySet()) {
                    IntegerProperty intProp;
                    Integer actual;
                    int expectedValue = targetState.get(key).getAsInt();
                    Property property = state.getBlock().getStateDefinition().getProperty(key);
                    if (!(property instanceof IntegerProperty) || (actual = (Integer)state.getValue((Property)(intProp = (IntegerProperty)property))) == expectedValue) continue;
                    return false;
                }
            }
            return true;
        };
    }
}

