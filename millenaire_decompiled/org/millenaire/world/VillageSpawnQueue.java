/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  it.unimi.dsi.fastutil.longs.LongOpenHashSet
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Holder
 *  net.minecraft.core.Vec3i
 *  net.minecraft.network.chat.Component
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.tags.TagKey
 *  net.minecraft.world.level.ChunkPos
 *  net.minecraft.world.level.biome.Biome
 *  net.minecraft.world.level.levelgen.Heightmap$Types
 *  org.slf4j.Logger
 */
package org.millenaire.world;

import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import org.millenaire.config.MillenaireServerConfig;
import org.millenaire.culture.ModCultures;
import org.millenaire.culture.VillageType;
import org.millenaire.village.VillageManager;
import org.millenaire.village.VillageSavedData;
import org.millenaire.world.SiteValidator;
import org.millenaire.world.StructureAvoidance;
import org.millenaire.world.VillageSpawner;
import org.slf4j.Logger;

public class VillageSpawnQueue {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int MAX_CHEAP_PER_TICK = 10;
    static final int MAX_LB_CHECKED_PER_TICK = 20;
    static final int LB_COVERAGE_RADIUS_CHUNKS = 8;
    private static final int BASE_EXPENSIVE_PER_TICK = 1;
    private static final int QUEUE_PRESSURE_THRESHOLD = 50;
    private static final int MAX_EXPENSIVE_PER_TICK = 5;
    static final long LB_TTL_TICKS = 72000L;
    private static final float BIOME_VALIDITY_FLOOR = 0.2f;
    private static final int BIOME_SAMPLE_RADIUS_CAP = 64;
    static final long LB_RETRY_BACKOFF_TICKS = 200L;
    private final Queue<BlockPos> candidates = new ArrayDeque<BlockPos>();
    private final Deque<LBCandidate> loneBuildingQueue = new ArrayDeque<LBCandidate>();
    private final LongOpenHashSet resolvedChunks = new LongOpenHashSet();
    private Map<TagKey<Biome>, List<VillageType>> villageBiomeIndex;
    private Map<TagKey<Biome>, List<VillageType>> lbBiomeIndex;
    private List<VillageType> villagesNoBiomeTags;
    private List<VillageType> lbsNoBiomeTags;
    @Nullable
    private volatile Thread ownerThread;
    private volatile boolean offThreadReported;

    private void checkThread() {
        Thread owner = this.ownerThread;
        if (owner != null && owner != Thread.currentThread() && !this.offThreadReported) {
            this.offThreadReported = true;
            LOGGER.error("VillageSpawnQueue mutated off the server thread ({}) \u2014 this is a bug, queue state may be corrupted", (Object)Thread.currentThread().getName(), (Object)new IllegalStateException());
        }
    }

    public void enqueue(BlockPos chunkCenter) {
        this.checkThread();
        this.candidates.add(chunkCenter);
    }

    public void markResolved(ChunkPos chunkPos) {
        this.checkThread();
        this.resolvedChunks.add(chunkPos.toLong());
    }

    public static boolean couldHostAnyVillage(Collection<VillageType> types, Predicate<TagKey<Biome>> biomeTagMatcher) {
        for (VillageType vt : types) {
            if (vt.weight() <= 0) continue;
            for (TagKey<Biome> tag : vt.biomeTags()) {
                if (!biomeTagMatcher.test(tag)) continue;
                return true;
            }
        }
        return false;
    }

    public void trySpawnNext(ServerLevel level) {
        if (this.ownerThread == null) {
            this.ownerThread = Thread.currentThread();
        }
        boolean log = (Boolean)MillenaireServerConfig.SERVER.logSpawnAttempts.get();
        boolean genVillages = (Boolean)MillenaireServerConfig.SERVER.generateVillages.get();
        boolean genLone = (Boolean)MillenaireServerConfig.SERVER.generateLoneBuildings.get();
        if (!genVillages && !genLone) {
            return;
        }
        VillageSavedData savedData = VillageSavedData.get(level);
        VillageManager manager = savedData.getVillageManager();
        long currentTick = level.getGameTime();
        this.processMainQueue(level, savedData, manager, genVillages, genLone, log, currentTick);
        if (genVillages && genLone && currentTick % 5L == 0L) {
            this.processLBQueue(level, savedData, manager, log, currentTick);
        }
    }

    private void processMainQueue(ServerLevel level, VillageSavedData savedData, VillageManager manager, boolean genVillages, boolean genLone, boolean log, long currentTick) {
        int cheapChecked = 0;
        int expensiveBudget = VillageSpawnQueue.computeExpensiveBudget(this.candidates.size());
        int expensiveUsed = 0;
        long spawnProtRadius = MillenaireServerConfig.SERVER.spawnProtectionRadius.getAsInt();
        while (cheapChecked < 10) {
            boolean loneKeyOnly;
            BlockPos candidate = this.candidates.poll();
            if (candidate == null) {
                return;
            }
            ++cheapChecked;
            double distToSpawnSq = candidate.distSqr((Vec3i)level.getSharedSpawnPos());
            if (distToSpawnSq < (double)(spawnProtRadius * spawnProtRadius)) {
                if (!log) continue;
                LOGGER.info("[Millenaire spawn] {} rejected: within spawn protection ({} < {})", new Object[]{candidate.toShortString(), (int)Math.sqrt(distToSpawnSq), spawnProtRadius});
                continue;
            }
            boolean tooCloseForVillage = genVillages && (manager.isWithinMinDistance(candidate, MillenaireServerConfig.SERVER.minVillageDistance.getAsInt()) || manager.isWithinMinDistanceOfLoneBuildings(candidate, MillenaireServerConfig.SERVER.minVillageLoneBuildingDistance.getAsInt(), savedData.getLoneBuildingPositions()));
            VillageManager.LBPlacement lonePlacement = genLone ? manager.classifyLBPlacement(candidate, MillenaireServerConfig.SERVER.minLoneBuildingDistance.getAsInt(), MillenaireServerConfig.SERVER.minVillageLoneBuildingDistance.getAsInt(), savedData.getLoneBuildingPositions()) : VillageManager.LBPlacement.BLOCKED;
            boolean tooCloseForLone = lonePlacement == VillageManager.LBPlacement.BLOCKED;
            boolean bl = loneKeyOnly = lonePlacement == VillageManager.LBPlacement.KEY_ONLY;
            if (!(genVillages && !tooCloseForVillage || genLone && !tooCloseForLone)) {
                if (!log || !genVillages || !tooCloseForVillage) continue;
                LOGGER.info("[Millenaire spawn] {} skipped for village: tooClose", (Object)candidate.toShortString());
                continue;
            }
            int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, candidate.getX(), candidate.getZ());
            if (surfaceY <= level.getMinBuildHeight()) continue;
            BlockPos surfacePos = new BlockPos(candidate.getX(), surfaceY, candidate.getZ());
            if (genVillages && genLone) {
                if (!tooCloseForVillage) {
                    SpawnResult result = this.trySpawnVillage(level, surfacePos, distToSpawnSq, manager, false, log, false);
                    ++expensiveUsed;
                    if (result == SpawnResult.SPAWNED || result == SpawnResult.DEFERRED) {
                        if (expensiveUsed < expensiveBudget) continue;
                        return;
                    }
                    if (!tooCloseForLone) {
                        this.loneBuildingQueue.addLast(new LBCandidate(surfacePos, distToSpawnSq, currentTick, 0L));
                        if (log) {
                            LOGGER.info("[Millenaire spawn] {} deferred to LB queue (village failed)", (Object)surfacePos.toShortString());
                        }
                    }
                } else if (!tooCloseForLone) {
                    this.loneBuildingQueue.addLast(new LBCandidate(surfacePos, distToSpawnSq, currentTick, 0L));
                    if (log) {
                        LOGGER.info("[Millenaire spawn] {} deferred to LB queue (too close for village)", (Object)surfacePos.toShortString());
                    }
                }
                if (expensiveUsed < expensiveBudget) continue;
                return;
            }
            if (genVillages && !tooCloseForVillage) {
                this.trySpawnVillage(level, surfacePos, distToSpawnSq, manager, false, log, false);
                if (++expensiveUsed < expensiveBudget) continue;
                return;
            }
            if (!genLone || tooCloseForLone) continue;
            this.trySpawnVillage(level, surfacePos, distToSpawnSq, manager, true, log, loneKeyOnly);
            if (++expensiveUsed < expensiveBudget) continue;
            return;
        }
    }

    private void processLBQueue(ServerLevel level, VillageSavedData savedData, VillageManager manager, boolean log, long currentTick) {
        int queueSize = this.loneBuildingQueue.size();
        if (queueSize == 0) {
            return;
        }
        int scanBudget = Math.min(queueSize, 20);
        int expensiveBudget = VillageSpawnQueue.computeExpensiveBudget(queueSize);
        int expensiveUsed = 0;
        for (int i = 0; i < scanBudget; ++i) {
            SpawnResult result;
            boolean keyOnly;
            LBCandidate candidate = this.loneBuildingQueue.pollFirst();
            if (candidate == null) {
                return;
            }
            if (VillageSpawnQueue.isExpired(candidate, currentTick, 72000L)) {
                if (!log) continue;
                LOGGER.info("[Millenaire spawn] LB candidate {} expired (TTL)", (Object)candidate.pos().toShortString());
                continue;
            }
            if (VillageSpawnQueue.shouldBackoff(candidate, currentTick)) {
                this.loneBuildingQueue.addLast(candidate);
                continue;
            }
            if (!this.isCoverageComplete(candidate.pos())) {
                LBCandidate deferred = new LBCandidate(candidate.pos(), candidate.distToSpawnSq(), candidate.enqueuedTick(), currentTick + 200L);
                this.loneBuildingQueue.addLast(deferred);
                continue;
            }
            VillageManager.LBPlacement placement = manager.classifyLBPlacement(candidate.pos(), MillenaireServerConfig.SERVER.minLoneBuildingDistance.getAsInt(), MillenaireServerConfig.SERVER.minVillageLoneBuildingDistance.getAsInt(), savedData.getLoneBuildingPositions());
            if (placement == VillageManager.LBPlacement.BLOCKED) {
                if (!log) continue;
                LOGGER.info("[Millenaire spawn] LB candidate {} dropped (too close after re-check)", (Object)candidate.pos().toShortString());
                continue;
            }
            boolean bl = keyOnly = placement == VillageManager.LBPlacement.KEY_ONLY;
            if (log) {
                LOGGER.info("[Millenaire spawn] LB candidate {} passed coverage check", (Object)candidate.pos().toShortString());
            }
            if ((result = this.trySpawnVillage(level, candidate.pos(), candidate.distToSpawnSq(), manager, true, log, keyOnly)) == SpawnResult.DEFERRED) {
                this.loneBuildingQueue.addLast(new LBCandidate(candidate.pos(), candidate.distToSpawnSq(), candidate.enqueuedTick(), currentTick + 200L));
            }
            if (++expensiveUsed < expensiveBudget) continue;
            return;
        }
    }

    static boolean isCoverageComplete(BlockPos pos, LongOpenHashSet resolved, int radiusChunks) {
        int centerCX = pos.getX() >> 4;
        int centerCZ = pos.getZ() >> 4;
        for (int cx = centerCX - radiusChunks; cx <= centerCX + radiusChunks; ++cx) {
            for (int cz = centerCZ - radiusChunks; cz <= centerCZ + radiusChunks; ++cz) {
                if (resolved.contains(ChunkPos.asLong((int)cx, (int)cz))) continue;
                return false;
            }
        }
        return true;
    }

    private boolean isCoverageComplete(BlockPos pos) {
        return VillageSpawnQueue.isCoverageComplete(pos, this.resolvedChunks, 8);
    }

    static boolean isExpired(LBCandidate candidate, long currentTick, long ttlTicks) {
        return currentTick - candidate.enqueuedTick() > ttlTicks;
    }

    static boolean shouldBackoff(LBCandidate candidate, long currentTick) {
        return currentTick < candidate.retryAfterTick();
    }

    static int computeExpensiveBudget(int queueSize) {
        if (queueSize <= 50) {
            return 1;
        }
        int extra = (queueSize - 50) / 50;
        return Math.min(1 + extra, 5);
    }

    public void invalidateBiomeIndex() {
        this.villageBiomeIndex = null;
    }

    private void ensureBiomeIndex() {
        if (this.villageBiomeIndex != null) {
            return;
        }
        this.villageBiomeIndex = new HashMap<TagKey<Biome>, List<VillageType>>();
        this.lbBiomeIndex = new HashMap<TagKey<Biome>, List<VillageType>>();
        this.villagesNoBiomeTags = new ArrayList<VillageType>();
        this.lbsNoBiomeTags = new ArrayList<VillageType>();
        for (VillageType vt : ModCultures.getAllVillageTypes().values()) {
            if (vt.weight() <= 0) continue;
            if (vt.biomeTags().isEmpty()) {
                (vt.loneBuilding() ? this.lbsNoBiomeTags : this.villagesNoBiomeTags).add(vt);
                continue;
            }
            Map<TagKey<Biome>, List<VillageType>> index = vt.loneBuilding() ? this.lbBiomeIndex : this.villageBiomeIndex;
            for (TagKey<Biome> tag : vt.biomeTags()) {
                index.computeIfAbsent(tag, k -> new ArrayList()).add(vt);
            }
        }
        LOGGER.debug("[Millenaire spawn] Biome index built: {} village tags, {} LB tags, {} no-tag villages, {} no-tag LBs", new Object[]{this.villageBiomeIndex.size(), this.lbBiomeIndex.size(), this.villagesNoBiomeTags.size(), this.lbsNoBiomeTags.size()});
    }

    /*
     * WARNING - void declaration
     */
    private SpawnResult trySpawnVillage(ServerLevel level, BlockPos surfacePos, double distToSpawnSq, VillageManager manager, boolean loneBuilding, boolean log, boolean keyOnly) {
        void var28_47;
        void var26_32;
        String label = loneBuilding ? "lone building" : "village";
        Holder biome = level.getBiome(surfacePos);
        ResourceLocation biomeId = biome.unwrapKey().map(k -> k.location()).orElse(null);
        if (biomeId == null) {
            return SpawnResult.FAILED;
        }
        VillageSavedData savedData = VillageSavedData.get(level);
        HashMap<ResourceLocation, Integer> lbCounts = null;
        if (loneBuilding) {
            lbCounts = new HashMap<ResourceLocation, Integer>();
            for (VillageSavedData.LoneBuildingEntry entry : savedData.getLoneBuildingPositions()) {
                lbCounts.merge(entry.type(), 1, Integer::sum);
            }
        }
        this.ensureBiomeIndex();
        Map<TagKey<Biome>, List<VillageType>> biomeIndex = loneBuilding ? this.lbBiomeIndex : this.villageBiomeIndex;
        List<VillageType> noBiomeTags = loneBuilding ? this.lbsNoBiomeTags : this.villagesNoBiomeTags;
        LinkedHashSet<VillageType> biomeCandidates = new LinkedHashSet<VillageType>(noBiomeTags);
        for (Map.Entry<TagKey<Biome>, List<VillageType>> entry : biomeIndex.entrySet()) {
            if (!biome.is(entry.getKey())) continue;
            biomeCandidates.addAll((Collection<VillageType>)entry.getValue());
        }
        if (keyOnly) {
            biomeCandidates.removeIf(vt -> !vt.keyLoneBuilding());
        }
        boolean hamletConfigEnabled = (Boolean)MillenaireServerConfig.SERVER.generateHamlets.get();
        int spawnProtRadius = MillenaireServerConfig.SERVER.spawnProtectionRadius.getAsInt();
        ArrayList<VillageType> compatible = new ArrayList<VillageType>();
        LinkedHashMap<Integer, List> byRadius = new LinkedHashMap<Integer, List>();
        int filteredByHamlets = 0;
        int filteredBySpawnDist = 0;
        int filteredByMaxCount = 0;
        int filteredByMinDist = 0;
        for (VillageType villageType : biomeCandidates) {
            int n;
            long l;
            if (!(loneBuilding || hamletConfigEnabled || villageType.hamlets().isEmpty())) {
                ++filteredByHamlets;
                continue;
            }
            if (loneBuilding && villageType.minDistanceFromSpawn() >= 0 && distToSpawnSq <= (double)(l = (long)villageType.minDistanceFromSpawn()) * (double)l) {
                ++filteredByMinDist;
                continue;
            }
            if (loneBuilding && villageType.max() != -1 && lbCounts != null && (n = lbCounts.getOrDefault((Object)villageType.id(), 0).intValue()) >= villageType.max()) {
                ++filteredByMaxCount;
                continue;
            }
            l = spawnProtRadius + villageType.radius();
            if (distToSpawnSq < (double)l * (double)l) {
                ++filteredBySpawnDist;
                continue;
            }
            if (villageType.biomeTags().isEmpty()) {
                compatible.add(villageType);
                continue;
            }
            int sampleRadius = Math.min(villageType.radius(), 64);
            byRadius.computeIfAbsent(sampleRadius, k -> new ArrayList()).add(villageType);
        }
        int filteredByBiomeValidity = 0;
        for (Map.Entry entry : byRadius.entrySet()) {
            int n = (Integer)entry.getKey();
            List types = (List)entry.getValue();
            int n2 = 0;
            ArrayList<Holder> samples = new ArrayList<Holder>();
            int biomeSampleY = level.getMaxBuildHeight() - 1;
            for (int gx = -n; gx <= n; gx += 16) {
                for (int gz = -n; gz <= n; gz += 16) {
                    ++n2;
                    samples.add(level.getBiome(new BlockPos(surfacePos.getX() + gx, biomeSampleY, surfacePos.getZ() + gz)));
                }
            }
            for (VillageType vt3 : types) {
                int validCount = 0;
                for (Holder sample : samples) {
                    if (!VillageSpawnQueue.matchesAnyTag((Holder<Biome>)sample, vt3.biomeTags())) continue;
                    ++validCount;
                }
                float validPerc = (float)validCount / (float)n2;
                float requiredValidity = vt3.minimumBiomeValidity();
                if (validPerc < (requiredValidity += SiteValidator.radiusRelaxation(vt3.radius()) * (0.2f - requiredValidity))) {
                    ++filteredByBiomeValidity;
                    if (!log || validCount <= 0) continue;
                    LOGGER.info("[Millenaire spawn]   {} rejected: biome validity {}/{} = {}% < {}%", new Object[]{vt3.id(), validCount, n2, Math.round(validPerc * 100.0f), Math.round(requiredValidity * 100.0f)});
                    continue;
                }
                compatible.add(vt3);
            }
        }
        if (compatible.isEmpty()) {
            if (log) {
                int n = (int)Math.sqrt(distToSpawnSq);
                if (biomeCandidates.isEmpty()) {
                    LOGGER.info("[Millenaire spawn] {} at {} rejected: no {} type registered for biome {}", new Object[]{label, surfacePos.toShortString(), label, biomeId});
                } else {
                    LOGGER.info("[Millenaire spawn] {} at {} rejected: {} biome-compatible {} type(s) found but all filtered out (spawnDist={}/{}: {}, hamlets: {}, maxCount: {}, minDist: {}, biomeValidity: {})", new Object[]{label, surfacePos.toShortString(), biomeCandidates.size(), label, n, spawnProtRadius, filteredBySpawnDist, filteredByHamlets, filteredByMaxCount, filteredByMinDist, filteredByBiomeValidity});
                }
            }
            return SpawnResult.FAILED;
        }
        boolean bl = false;
        for (VillageType villageType : compatible) {
            var26_32 += villageType.weight();
        }
        if (var26_32 <= 0) {
            return SpawnResult.FAILED;
        }
        int n = ThreadLocalRandom.current().nextInt((int)var26_32);
        VillageType villageType = (VillageType)compatible.getLast();
        for (VillageType villageType2 : compatible) {
            if ((var27_38 -= villageType2.weight()) >= 0) continue;
            VillageType villageType3 = villageType2;
            break;
        }
        if (log) {
            LOGGER.info("[Millenaire spawn] {} at {} trying type '{}' (radius={}, biome={})", new Object[]{label, surfacePos.toShortString(), var28_47.id(), var28_47.radius(), biomeId});
        }
        if (manager.overlapsExistingVillage(surfacePos, var28_47.radius())) {
            if (log) {
                LOGGER.info("[Millenaire spawn] {} at {} rejected: footprint would overlap an existing village (radius={})", new Object[]{label, surfacePos.toShortString(), var28_47.radius()});
            }
            return SpawnResult.FAILED;
        }
        int r = var28_47.radius();
        if (!level.hasChunksAt(new BlockPos(surfacePos.getX() - r, 0, surfacePos.getZ() - r), new BlockPos(surfacePos.getX() + r, 0, surfacePos.getZ() + r))) {
            if (log) {
                LOGGER.info("[Millenaire spawn] {} at {} deferred: chunks not loaded (radius={})", new Object[]{label, surfacePos.toShortString(), r});
            }
            if (!loneBuilding) {
                this.candidates.add(new BlockPos(surfacePos.getX(), 0, surfacePos.getZ()));
            }
            return SpawnResult.DEFERRED;
        }
        if (StructureAvoidance.hasConflict(level, surfacePos, var28_47.radius())) {
            if (log) {
                LOGGER.info("[Millenaire spawn] {} at {} rejected: vanilla structure conflict", (Object)label, (Object)surfacePos.toShortString());
            }
            return SpawnResult.FAILED;
        }
        if (!SiteValidator.validate(level, surfacePos, var28_47.radius(), loneBuilding)) {
            if (log) {
                LOGGER.info("[Millenaire spawn] {} at {} rejected: terrain validation failed", (Object)label, (Object)surfacePos.toShortString());
            }
            return SpawnResult.FAILED;
        }
        double d = Math.sqrt(distToSpawnSq);
        int completion = VillageSpawnQueue.computeCompletionByDistance(d);
        LOGGER.info("[Millenaire] Natural spawn ({}) : {} at {} (completion: {}%, queue: {}, lbQueue: {})", new Object[]{label, var28_47.id(), surfacePos.toShortString(), completion, this.candidates.size(), this.loneBuildingQueue.size()});
        Component failure = VillageSpawner.spawnVillage(level, surfacePos, (VillageType)var28_47, completion);
        if (log && failure != null) {
            LOGGER.info("[Millenaire spawn] {} at {} rejected: {}", new Object[]{label, surfacePos.toShortString(), failure.getString()});
        }
        if (failure == null && loneBuilding) {
            savedData.registerLoneBuilding(surfacePos, var28_47.id(), var28_47.culture().getPath(), null);
        }
        return failure == null ? SpawnResult.SPAWNED : SpawnResult.FAILED;
    }

    static int computeMaxCompletion(double distanceToSpawn, int minDist, int maxDist, int maxPct) {
        if (maxPct <= 0 || maxDist <= 0) {
            return 0;
        }
        if (distanceToSpawn <= (double)minDist) {
            return 0;
        }
        float completionRatio = distanceToSpawn > (double)maxDist ? (float)maxPct / 100.0f : (float)((double)maxPct * ((distanceToSpawn - (double)minDist) / (double)maxDist) / 100.0);
        return Math.round(completionRatio * 100.0f);
    }

    static int computeCompletionByDistance(double distanceToSpawn) {
        int maxCompletion = VillageSpawnQueue.computeMaxCompletion(distanceToSpawn, MillenaireServerConfig.SERVER.completionMinDistance.getAsInt(), MillenaireServerConfig.SERVER.completionMaxDistance.getAsInt(), MillenaireServerConfig.SERVER.completionMaxPercentage.getAsInt());
        if (maxCompletion <= 0) {
            return 0;
        }
        return ThreadLocalRandom.current().nextInt(maxCompletion + 1);
    }

    public Map<String, Object> getStats() {
        LinkedHashMap<String, Object> stats = new LinkedHashMap<String, Object>();
        stats.put("mainQueueSize", this.candidates.size());
        stats.put("lbQueueSize", this.loneBuildingQueue.size());
        stats.put("resolvedChunks", this.resolvedChunks.size());
        return stats;
    }

    private static boolean matchesAnyTag(Holder<Biome> biome, List<TagKey<Biome>> tags) {
        for (TagKey<Biome> tag : tags) {
            if (!biome.is(tag)) continue;
            return true;
        }
        return false;
    }

    static final class SpawnResult
    extends Enum<SpawnResult> {
        public static final /* enum */ SpawnResult SPAWNED = new SpawnResult();
        public static final /* enum */ SpawnResult FAILED = new SpawnResult();
        public static final /* enum */ SpawnResult DEFERRED = new SpawnResult();
        private static final /* synthetic */ SpawnResult[] $VALUES;

        public static SpawnResult[] values() {
            return (SpawnResult[])$VALUES.clone();
        }

        public static SpawnResult valueOf(String name) {
            return Enum.valueOf(SpawnResult.class, name);
        }

        private static /* synthetic */ SpawnResult[] $values() {
            return new SpawnResult[]{SPAWNED, FAILED, DEFERRED};
        }

        static {
            $VALUES = SpawnResult.$values();
        }
    }

    record LBCandidate(BlockPos pos, double distToSpawnSq, long enqueuedTick, long retryAfterTick) {
    }
}

