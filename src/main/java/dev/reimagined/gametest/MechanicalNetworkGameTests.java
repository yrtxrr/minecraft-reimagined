package dev.reimagined.gametest;

import dev.reimagined.engineering.EngineeringContent;
import dev.reimagined.engineering.MachineBlockEntity;
import dev.reimagined.engineering.MechanicalBlock;
import dev.reimagined.engineering.MechanicalNetwork;
import dev.reimagined.engineering.MechanicalNetworks;
import dev.reimagined.engineering.Rotation;
import java.util.ArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Real loaded-world tests for shared-cache scheduling, not a substitute for steady-state profiling. */
@GameTestHolder("reimagined")
@PrefixGameTestTemplate(false)
public final class MechanicalNetworkGameTests {
    private static final BlockPos WHEEL = new BlockPos(3, 3, 3);
    private static final BlockPos SHAFT = new BlockPos(4, 3, 3);
    private static final BlockPos MILL = new BlockPos(5, 3, 3);
    private MechanicalNetworkGameTests() {}
    private static MachineBlockEntity machine(GameTestHelper helper, BlockPos pos) {
        return (MachineBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(pos));
    }
    private static void setup(GameTestHelper helper) {
        helper.setBlock(WHEEL, EngineeringContent.WATER_WHEEL.get().defaultBlockState().setValue(MechanicalBlock.AXIS, Direction.Axis.X));
        helper.setBlock(SHAFT, EngineeringContent.SHAFT.get().defaultBlockState().setValue(MechanicalBlock.AXIS, Direction.Axis.X));
        helper.setBlock(MILL, EngineeringContent.MILLSTONE.get().defaultBlockState());
        for (int z = 4; z <= 5; z++) {
            helper.setBlock(new BlockPos(3, 2, z), Blocks.STONE);
            helper.setBlock(new BlockPos(2, 3, z), Blocks.STONE);
            helper.setBlock(new BlockPos(4, 3, z), Blocks.STONE);
        }
        helper.setBlock(new BlockPos(3, 3, 6), Blocks.STONE);
        helper.setBlock(new BlockPos(3, 3, 5), Blocks.WATER);
        helper.setBlock(new BlockPos(3, 3, 4), Blocks.WATER.defaultBlockState().setValue(LiquidBlock.LEVEL, 1));
    }
    private static void running(GameTestHelper helper) {
        for (BlockPos relative : new BlockPos[] {WHEEL, SHAFT, MILL}) {
            BlockPos pos = helper.absolutePos(relative);
            helper.assertTrue(MechanicalNetworks.isCached(helper.getLevel(), pos)
                            && helper.getLevel().getBlockState(pos).getValue(MechanicalBlock.ACTIVE),
                    "Real wheel, shaft and mill must receive the shared running state");
        }
    }
    private static double difference(double a, double b) {
        double difference = Math.abs(a - b) % 360;
        return Math.min(difference, 360 - difference);
    }
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void repeatedMemberReadsReuseCacheAndMotionPhase(GameTestHelper helper) {
        setup(helper);
        helper.succeedWhen(() -> {
            running(helper);
            var level = helper.getLevel();
            long scans = MechanicalNetworks.stats(level).scans();
            for (int i = 0; i < 100; i++) for (BlockPos relative : new BlockPos[] {WHEEL, SHAFT, MILL})
                helper.assertTrue(MechanicalNetworks.rotation(level, helper.absolutePos(relative)).running(),
                        "Every loaded member must observe the same valid component");
            helper.assertTrue(MechanicalNetworks.stats(level).scans() == scans,
                    "Member lookups must not synchronously repeat the component BFS");
            double wheelPhase = machine(helper, WHEEL).rotationAngle(0.5F);
            helper.assertTrue(difference(wheelPhase, machine(helper, SHAFT).rotationAngle(0.5F)) < 0.02
                            && difference(wheelPhase, machine(helper, MILL).rotationAngle(0.5F)) < 0.02,
                    "Connected wheel, shaft and mill must interpolate from a common phase");
            var before = MechanicalNetworks.stats(level);
            MechanicalNetworks.resetStats(level);
            var after = MechanicalNetworks.stats(level);
            helper.assertTrue(after.scans() == 0 && after.cacheHits() == 0
                            && after.cachedPositions() == before.cachedPositions() && after.pending() == before.pending(),
                    "Resetting profiler counters must not clear or perturb live cached networks");
        });
    }
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void oversizedGraphsBoundDiscoveryFrontierAndReads(GameTestHelper helper) {
        for (int x = 2; x < 7; x++) for (int y = 3; y < 9; y++) for (int z = 2; z < 7; z++)
            helper.setBlock(new BlockPos(x, y, z), EngineeringContent.MILLSTONE.get());
        var scan = MechanicalNetwork.scan(helper.getLevel(), helper.absolutePos(new BlockPos(2, 3, 2)));
        helper.assertTrue(scan.rotation().truncated() && !scan.rotation().running(),
                "A connected graph larger than the supported bound must fail closed");
        helper.assertTrue(scan.nodes().size() <= Rotation.MAX_NODES && scan.frontierPeak() <= Rotation.MAX_NODES
                        && scan.reads() <= MechanicalNetwork.MAX_READS,
                "Node discovery, pending frontier and nested fluid/redstone reads must all remain bounded");
        helper.succeed();
    }
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void fairSchedulerEventuallyCachesColdComponentsWithinTickBudgets(GameTestHelper helper) {
        var positions = new ArrayList<BlockPos>();
        for (int x = 2; x <= 12; x += 2) for (int z = 2; z <= 10; z += 2) {
            BlockPos relative = new BlockPos(x, 3, z);
            helper.setBlock(relative, EngineeringContent.SHAFT.get().defaultBlockState().setValue(MechanicalBlock.AXIS, Direction.Axis.Y));
            BlockPos pos = helper.absolutePos(relative); positions.add(pos); MechanicalNetworks.request(helper.getLevel(), pos);
        }
        helper.succeedWhen(() -> {
            helper.assertTrue(positions.stream().allMatch(pos -> MechanicalNetworks.isCached(helper.getLevel(), pos)),
                    "Every cold independent component must eventually be served without FIFO starvation");
            var stats = MechanicalNetworks.stats(helper.getLevel());
            helper.assertTrue(stats.lastTickScans() <= MechanicalNetworks.MAX_SCANS_PER_TICK
                            && stats.maxTickScans() <= MechanicalNetworks.MAX_SCANS_PER_TICK
                            && stats.lastTickDequeues() <= MechanicalNetworks.MAX_DEQUEUES_PER_TICK
                            && stats.pending() <= MechanicalNetworks.MAX_PENDING
                            && stats.cachedPositions() <= MechanicalNetworks.MAX_CACHE_POSITIONS,
                    "Aggregate per-level scheduling and retained state must respect their declared budgets");
        });
    }
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void removingTransmissionStopsCachedMillAndKeepsItsPhaseFrozen(GameTestHelper helper) {
        setup(helper);
        boolean[] removed = {false};
        double[] stoppedPhase = {0};
        long[] removedTick = {0};
        helper.succeedWhen(() -> {
            if (!removed[0]) {
                running(helper);
                helper.setBlock(SHAFT, Blocks.AIR);
                helper.assertTrue(!helper.getLevel().getBlockState(helper.absolutePos(MILL)).getValue(MechanicalBlock.ACTIVE),
                        "Topology invalidation must stop the old consumer before a stale snapshot can process inventory");
                stoppedPhase[0] = machine(helper, MILL).rotationAngle(0);
                removedTick[0] = helper.getLevel().getGameTime(); removed[0] = true;
            }
            helper.assertTrue(helper.getLevel().getGameTime() - removedTick[0] >= 5, "Observe several real server ticks after disconnection");
            helper.assertTrue(!helper.getLevel().getBlockState(helper.absolutePos(MILL)).getValue(MechanicalBlock.ACTIVE)
                            && difference(stoppedPhase[0], machine(helper, MILL).rotationAngle(0.75F)) < 0.02,
                    "A disconnected stopped mill must hold its angle instead of visually drifting");
            helper.assertTrue(helper.getLevel().getBlockEntity(helper.absolutePos(SHAFT)) == null,
                    "A destroyed shaft must not retain its block entity");
        });
    }
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void unloadedQueriesDoNotLoadChunksOrEnterTheWorkQueue(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos absent = new BlockPos(29999968, 100, 29999968);
        helper.assertTrue(!level.hasChunkAt(absent), "Fixture must target an actually unloaded distant chunk");
        int loaded = level.getChunkSource().getLoadedChunksCount();
        int pending = MechanicalNetworks.stats(level).pending();
        var scan = MechanicalNetwork.scan(level, absent);
        MechanicalNetworks.request(level, absent);
        helper.assertTrue(!MechanicalNetworks.rotation(level, absent).running()
                        && scan.nodes().isEmpty() && scan.unloadedBoundary(),
                "Unloaded starts must fail closed without reading remote block state");
        helper.assertTrue(level.getChunkSource().getLoadedChunksCount() == loaded
                        && MechanicalNetworks.stats(level).pending() == pending,
                "Neither diagnostics nor queued requests may acquire or load an unloaded chunk");
        helper.succeed();
    }
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void indexedChunkTeardownDropsMembershipWithoutWorldScanning(GameTestHelper helper) {
        setup(helper);
        helper.succeedWhen(() -> {
            running(helper);
            var level = helper.getLevel();
            int loaded = level.getChunkSource().getLoadedChunksCount();
            var chunk = new ChunkPos(helper.absolutePos(WHEEL));
            MechanicalNetworks.unloadChunk(level, chunk);
            try {
            helper.assertTrue(!MechanicalNetworks.isCached(level, helper.absolutePos(WHEEL))
                            && !MechanicalNetworks.isCached(level, helper.absolutePos(MILL)),
                    "The chunk-unload invalidation path must discard its complete cached component");
            helper.assertTrue(level.getChunkSource().getLoadedChunksCount() == loaded,
                    "Chunk teardown must not load any chunk while stopping remaining members");
            } finally { MechanicalNetworks.loadedChunk(level, chunk); }
        });
    }
}
