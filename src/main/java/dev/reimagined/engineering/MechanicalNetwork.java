package dev.reimagined.engineering;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.SignalGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

/** Bounded diagnostic traversal. Production ticking uses the shared MechanicalNetworks cache. */
public final class MechanicalNetwork {
    public static final int MAX_READS = 16384;
    private MechanicalNetwork() {}
    public record Scan(Rotation rotation, List<BlockPos> nodes, int reads, int frontierPeak, boolean unloadedBoundary) {
        public Scan { nodes = List.copyOf(nodes); }
    }
    public static Rotation inspect(Level level, BlockPos start) { return scan(level, start).rotation(); }
    public static Scan scan(Level level, BlockPos start) { return scan(level, start, Set.of()); }
    static Scan scan(Level level, BlockPos start, Set<Long> unavailableChunks) {
        var access = new LoadedSignals(level, unavailableChunks);
        var queue = new ArrayDeque<BlockPos>();
        var discovered = new HashSet<BlockPos>();
        var nodes = new ArrayList<BlockPos>();
        BlockPos first = start.immutable();
        queue.add(first); discovered.add(first);
        int wheels = 0, machines = 0, peak = 1;
        boolean truncated = false;
        while (!queue.isEmpty() && !access.exhausted) {
            BlockPos pos = queue.removeFirst();
            if (!access.loaded(pos)) { access.unloaded = true; continue; }
            BlockState state = access.getBlockState(pos);
            if (!(state.getBlock() instanceof MechanicalBlock)) continue;
            nodes.add(pos);
            if (state.is(EngineeringContent.WATER_WHEEL.get()) && powered(access, pos, state)) wheels++;
            if (state.is(EngineeringContent.MILLSTONE.get()) && !access.hasNeighborSignal(pos)) machines++;
            for (Direction direction : Direction.values()) {
                if (!connects(state, direction)) continue;
                BlockPos next = pos.relative(direction);
                if (discovered.contains(next)) continue;
                if (!access.loaded(next)) { access.unloaded = true; continue; }
                BlockState neighbor = access.getBlockState(next);
                if (!(neighbor.getBlock() instanceof MechanicalBlock) || !connects(neighbor, direction.getOpposite())) continue;
                if (discovered.size() == Rotation.MAX_NODES) { truncated = true; continue; }
                BlockPos immutable = next.immutable();
                discovered.add(immutable); queue.addLast(immutable);
                peak = Math.max(peak, queue.size());
            }
        }
        return new Scan(new Rotation(wheels, machines, truncated || access.exhausted || access.unloaded),
                nodes, access.reads, peak, access.unloaded);
    }
    private static boolean connects(BlockState state, Direction face) {
        return state.is(EngineeringContent.MILLSTONE.get()) || state.getValue(MechanicalBlock.AXIS) == face.getAxis();
    }
    public static boolean redstonePowered(Level level, BlockPos pos) {
        var access = new LoadedSignals(level);
        boolean powered = access.hasNeighborSignal(pos);
        // Unknown redstone state must not permit an unverified machine to process inventory.
        return powered || access.unloaded || access.exhausted;
    }
    public static boolean powered(Level level, BlockPos pos, BlockState state) {
        var access = new LoadedSignals(level);
        boolean powered = powered(access, pos, state);
        return powered && !access.unloaded && !access.exhausted;
    }
    private static boolean powered(LoadedSignals access, BlockPos pos, BlockState state) {
        if (access.hasNeighborSignal(pos)) return false;
        for (Direction direction : Direction.values()) {
            if (direction.getAxis() == state.getValue(MechanicalBlock.AXIS)) continue;
            BlockPos water = pos.relative(direction);
            FluidState fluid = access.getFluidState(water);
            if (fluid.is(FluidTags.WATER) && fluid.getFlow(access, water).lengthSqr() > 0.001) return true;
        }
        return false;
    }
    /** Vanilla flow/redstone helpers may query beyond their first neighbor; none may load a chunk. */
    private static final class LoadedSignals implements SignalGetter {
        private final Level level;
        private final Set<Long> unavailableChunks;
        private int reads;
        private boolean unloaded;
        private boolean exhausted;
        private long cachedChunkKey = Long.MIN_VALUE;
        private LevelChunk cachedChunk;
        LoadedSignals(Level level) { this(level, Set.of()); }
        LoadedSignals(Level level, Set<Long> unavailableChunks) { this.level = level; this.unavailableChunks = unavailableChunks; }
        private LevelChunk chunkAt(BlockPos pos) {
            long key = ChunkPos.asLong(pos);
            if (key == cachedChunkKey) return cachedChunk;
            cachedChunkKey = key;
            if (unavailableChunks.contains(key)) return cachedChunk = null;
            return cachedChunk = MechanicalWorldAccess.chunk(level, pos);
        }
        boolean loaded(BlockPos pos) { return chunkAt(pos) != null; }
        private boolean readable(BlockPos pos) {
            if (reads >= MAX_READS) { exhausted = true; return false; }
            reads++;
            if (!loaded(pos)) { unloaded = true; return false; }
            return true;
        }
        @Override public BlockState getBlockState(BlockPos pos) {
            if (!readable(pos)) return Blocks.AIR.defaultBlockState();
            var chunk = chunkAt(pos);
            return chunk == null ? Blocks.AIR.defaultBlockState() : chunk.getBlockState(pos);
        }
        @Override public FluidState getFluidState(BlockPos pos) {
            if (!readable(pos)) return Fluids.EMPTY.defaultFluidState();
            var chunk = chunkAt(pos);
            return chunk == null ? Fluids.EMPTY.defaultFluidState() : chunk.getFluidState(pos);
        }
        @Override public BlockEntity getBlockEntity(BlockPos pos) { if (!readable(pos)) return null; var chunk = chunkAt(pos); return chunk == null ? null : chunk.getBlockEntity(pos); }
        @Override public int getHeight() { return level.getHeight(); }
        @Override public int getMinBuildHeight() { return level.getMinBuildHeight(); }
    }
}
