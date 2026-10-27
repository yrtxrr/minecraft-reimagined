package dev.reimagined.engineering;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * Loaded component cache and fair, capped server scheduler. Values retain positions, never worlds/BEs.
 * Topology/chunk invalidation stops members immediately; ordinary expiry serves the last valid state
 * while a FIFO refresh is pending, so20-tick mill processing cannot starve on a20-tick TTL boundary.
 */
public final class MechanicalNetworks {
    public static final int MAX_SCANS_PER_TICK = 8;
    public static final int MAX_DEQUEUES_PER_TICK = 256;
    public static final int MAX_CACHE_POSITIONS = 16384;
    public static final int MAX_PENDING = 4096;
    public static final int REFRESH_TICKS = 20;
    private static final int MAX_RETIRED_PER_SCAN = 8;
    private static final Rotation STOPPED = new Rotation(0, 0, false);
    private static final Map<ServerLevel, Cache> LEVELS = new WeakHashMap<>();
    private static final Set<ServerLevel> UNLOADED_LEVELS = Collections.newSetFromMap(new WeakHashMap<>());
    private static final int MAX_UNLOADING_CHUNKS = 1024;
    private static boolean registered;
    private MechanicalNetworks() {}

    public record Stats(long scans, long cacheHits, long nodesRead, long blockReads, long skippedUnloaded,
                        long deferredRequests, long droppedRequests, int cachedPositions, int pending,
                        int lastTickScans, int maxTickScans, int lastTickDequeues, int maxFrontier) {}
    private static final class Entry {
        final Rotation rotation;
        final List<BlockPos> nodes;
        final Set<Long> chunks;
        final long checkedAt;
        Entry(Rotation rotation, List<BlockPos> nodes, long checkedAt) {
            this.rotation = rotation; this.nodes = nodes; this.checkedAt = checkedAt;
            chunks = new HashSet<>();
            for (BlockPos pos : nodes) chunks.add(ChunkPos.asLong(pos));
        }
    }
    private static final class Cache {
        final Map<BlockPos, Entry> members = new HashMap<>();
        final LinkedHashSet<Entry> entries = new LinkedHashSet<>();
        final Long2ObjectMap<Set<Entry>> chunks = new Long2ObjectOpenHashMap<>();
        final LinkedHashSet<BlockPos> pending = new LinkedHashSet<>();
        final LinkedHashSet<Long> unloadingChunks = new LinkedHashSet<>();
        long scans, cacheHits, nodesRead, blockReads, skippedUnloaded, deferredRequests, droppedRequests;
        int lastTickScans, maxTickScans, lastTickDequeues, maxFrontier;
        void enqueue(BlockPos pos) {
            if (pending.contains(pos)) return;
            if (pending.size() >= MAX_PENDING) { droppedRequests++; return; }
            pending.add(pos.immutable()); deferredRequests++;
        }
        void forget(Entry entry) {
            entries.remove(entry);
            for (BlockPos pos : entry.nodes) members.remove(pos, entry);
            for (long chunk : entry.chunks) {
                Set<Entry> indexed = chunks.get(chunk);
                if (indexed != null) { indexed.remove(entry); if (indexed.isEmpty()) chunks.remove(chunk); }
            }
        }
        void remember(Entry entry) {
            entries.add(entry);
            for (BlockPos pos : entry.nodes) members.put(pos, entry);
            for (long chunk : entry.chunks) chunks.computeIfAbsent(chunk, ignored -> new HashSet<>()).add(entry);
        }
    }
    public static void register() {
        if (registered) return;
        registered = true;
        NeoForge.EVENT_BUS.addListener(MechanicalNetworks::tick);
        NeoForge.EVENT_BUS.addListener(MechanicalNetworks::chunkUnloaded);
        NeoForge.EVENT_BUS.addListener(MechanicalNetworks::chunkLoaded);
        NeoForge.EVENT_BUS.addListener(MechanicalNetworks::levelUnloaded);
    }
    public static void request(ServerLevel level, BlockPos pos) {
        if (UNLOADED_LEVELS.contains(level) || !MechanicalWorldAccess.loaded(level, pos)) return;
        Cache cache = LEVELS.computeIfAbsent(level, ignored -> new Cache());
        if (!cache.unloadingChunks.contains(ChunkPos.asLong(pos))) cache.enqueue(pos);
    }
    public static Rotation rotation(ServerLevel level, BlockPos pos) {
        if (UNLOADED_LEVELS.contains(level) || !MechanicalWorldAccess.loaded(level, pos)) return STOPPED;
        Cache cache = LEVELS.computeIfAbsent(level, ignored -> new Cache());
        if (cache.unloadingChunks.contains(ChunkPos.asLong(pos))) return STOPPED;
        Entry entry = cache.members.get(pos);
        if (entry != null) {
            cache.cacheHits++;
            if (level.getGameTime() - entry.checkedAt >= REFRESH_TICKS) cache.enqueue(pos);
            return entry.rotation;
        }
        cache.enqueue(pos);
        return STOPPED;
    }
    /** Call for the changed node and its mechanical neighbors. One affected component has<=128nodes. */
    public static void invalidate(ServerLevel level, BlockPos pos) {
        if (UNLOADED_LEVELS.contains(level)) return;
        Cache cache = LEVELS.get(level);
        if (cache == null) { request(level, pos); return; }
        if (cache.unloadingChunks.contains(ChunkPos.asLong(pos))) return;
        Entry entry = cache.members.get(pos);
        if (entry != null) retire(level, cache, entry, Set.of());
        cache.enqueue(pos);
    }
    private static void retire(ServerLevel level, Cache cache, Entry entry, Set<BlockPos> retained) {
        cache.forget(entry);
        long time = level.getGameTime();
        for (BlockPos pos : entry.nodes) {
            if (retained.contains(pos) || !MechanicalWorldAccess.loaded(level, pos)) continue;
            if (MechanicalWorldAccess.blockEntity(level, pos) instanceof MachineBlockEntity machine)
                machine.applyRotation(0, machine.rotationAngle(0), time);
            cache.enqueue(pos);
        }
    }
    private static void tick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        Cache cache = LEVELS.get(level);
        if (cache == null) return;
        process(level, cache);
    }
    private static void process(ServerLevel level, Cache cache) {
        cache.lastTickScans = 0; cache.lastTickDequeues = 0;
        long now = level.getGameTime();
        while (!cache.pending.isEmpty() && cache.lastTickScans < MAX_SCANS_PER_TICK
                && cache.lastTickDequeues < MAX_DEQUEUES_PER_TICK) {
            Iterator<BlockPos> iterator = cache.pending.iterator();
            BlockPos start = iterator.next(); iterator.remove(); cache.lastTickDequeues++;
            if (!MechanicalWorldAccess.loaded(level, start) || cache.unloadingChunks.contains(ChunkPos.asLong(start))) {
                cache.skippedUnloaded++; continue;
            }
            Entry existing = cache.members.get(start);
            if (existing != null && now - existing.checkedAt < REFRESH_TICKS) continue;
            var scan = MechanicalNetwork.scan(level, start, cache.unloadingChunks);
            cache.scans++; cache.lastTickScans++; cache.nodesRead += scan.nodes().size(); cache.blockReads += scan.reads();
            cache.maxFrontier = Math.max(cache.maxFrontier, scan.frontierPeak());
            if (scan.unloadedBoundary()) cache.skippedUnloaded++;
            if (scan.nodes().isEmpty()) { if (existing != null) retire(level, cache, existing, Set.of()); continue; }
            var oldEntries = new LinkedHashSet<Entry>();
            for (BlockPos pos : scan.nodes()) {
                Entry old = cache.members.get(pos); if (old != null) oldEntries.add(old);
            }
            // A pathological missed merge may overlap many old components. Retire them in bounded
            // batches rather than doing up to128*128 callback operations in a single scan.
            if (oldEntries.size() > MAX_RETIRED_PER_SCAN) {
                int retired = 0;
                for (Entry old : oldEntries) { retire(level, cache, old, Set.of()); if (++retired == MAX_RETIRED_PER_SCAN) break; }
                cache.enqueue(start); continue;
            }
            var newMembers = new HashSet<>(scan.nodes());
            for (Entry old : oldEntries) retire(level, cache, old, newMembers);
            while (cache.members.size() + scan.nodes().size() > MAX_CACHE_POSITIONS && !cache.entries.isEmpty())
                retire(level, cache, cache.entries.iterator().next(), Set.of());
            Entry entry = new Entry(scan.rotation(), scan.nodes(), now);
            cache.remember(entry);
            double phase = 0;
            BlockPos representative = null;
            boolean movingRepresentative = false;
            for (BlockPos pos : entry.nodes) {
                if (!MechanicalWorldAccess.loaded(level, pos) || !(MechanicalWorldAccess.blockEntity(level, pos) instanceof MachineBlockEntity machine)) continue;
                boolean moving = machine.getBlockState().getValue(MechanicalBlock.ACTIVE)
                        && !(machine.isMillstone() && MechanicalNetwork.redstonePowered(level, pos));
                if (representative == null || moving && !movingRepresentative
                        || moving == movingRepresentative && pos.compareTo(representative) < 0) {
                    representative = pos; movingRepresentative = moving; phase = machine.rotationAngle(0);
                }
            }
            for (BlockPos pos : entry.nodes) {
                if (!MechanicalWorldAccess.loaded(level, pos) || !(MechanicalWorldAccess.blockEntity(level, pos) instanceof MachineBlockEntity machine)) continue;
                int rpm = entry.rotation.speed();
                if (machine.isMillstone() && MechanicalNetwork.redstonePowered(level, pos)) rpm = 0;
                machine.applyRotation(rpm, rpm == 0 ? machine.rotationAngle(0) : phase, now);
            }
        }
        cache.maxTickScans = Math.max(cache.maxTickScans, cache.lastTickScans);
    }
    private static void chunkUnloaded(ChunkEvent.Unload event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        unloadChunk(level, event.getChunk().getPos());
    }
    /** Also exposed for lifecycle integration tests; never requests or reads the departing chunk. */
    public static void unloadChunk(ServerLevel level, ChunkPos pos) {
        Cache cache = LEVELS.get(level);
        if (cache == null) return;
        if (cache.unloadingChunks.size() >= MAX_UNLOADING_CHUNKS)
            cache.unloadingChunks.remove(cache.unloadingChunks.iterator().next());
        cache.unloadingChunks.add(pos.toLong());
        // Only this chunk's indexed components are considered, not every cached network/world node.
        Set<Entry> entries = cache.chunks.get(pos.toLong());
        if (entries == null) return;
        for (Entry entry : new ArrayList<>(entries)) {
            cache.forget(entry);
            for (BlockPos member : entry.nodes) {
                if (new ChunkPos(member).equals(pos) || !MechanicalWorldAccess.loaded(level, member)) continue;
                if (MechanicalWorldAccess.blockEntity(level, member) instanceof MachineBlockEntity machine)
                    machine.applyRotation(0, machine.rotationAngle(0), level.getGameTime());
                cache.enqueue(member);
            }
        }
    }
    private static void chunkLoaded(ChunkEvent.Load event) {
        // Load events can occur before FULL: remove only the marker, never read this chunk here.
        if (event.getLevel() instanceof ServerLevel level) loadedChunk(level, event.getChunk().getPos());
    }
    public static void loadedChunk(ServerLevel level, ChunkPos pos) {
        Cache cache = LEVELS.get(level);
        if (cache != null) cache.unloadingChunks.remove(pos.toLong());
    }
    private static void levelUnloaded(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) { UNLOADED_LEVELS.add(level); clear(level); }
    }
    public static void clear(ServerLevel level) { LEVELS.remove(level); }
    /** Observation only, for diagnostics/tests; never schedules a request or creates a cache. */
    public static boolean isCached(ServerLevel level, BlockPos pos) {
        Cache cache = LEVELS.get(level);
        return cache != null && cache.members.containsKey(pos);
    }
    public static Stats stats(ServerLevel level) {
        Cache c = LEVELS.get(level);
        return c == null ? new Stats(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
                : new Stats(c.scans, c.cacheHits, c.nodesRead, c.blockReads, c.skippedUnloaded,
                    c.deferredRequests, c.droppedRequests, c.members.size(), c.pending.size(),
                    c.lastTickScans, c.maxTickScans, c.lastTickDequeues, c.maxFrontier);
    }
    /** Reset measurement counters only; profiling must not perturb live networks/caches. */
    public static void resetStats(ServerLevel level) {
        Cache c = LEVELS.get(level);
        if (c == null) return;
        c.scans = c.cacheHits = c.nodesRead = c.blockReads = c.skippedUnloaded = c.deferredRequests = c.droppedRequests = 0;
        c.lastTickScans = c.maxTickScans = c.lastTickDequeues = c.maxFrontier = 0;
    }
}
