package dev.reimagined.engineering.profile;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.reimagined.engineering.EngineeringContent;
import dev.reimagined.engineering.MachineBlockEntity;
import dev.reimagined.engineering.MechanicalBlock;
import dev.reimagined.engineering.MechanicalNetworks;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import jdk.jfr.Configuration;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.Recording;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Opt-in development harness. Uses ordinary 20TPS server ticks, not the accelerated GameTest runner. */
@EventBusSubscriber(modid = "reimagined")
public final class MechanicalProfile {
    private static final Logger LOGGER = LoggerFactory.getLogger(MechanicalProfile.class);
    private static final boolean ENABLED = Boolean.getBoolean("reimagined.mechanicalProfile");
    private static final int[] COUNTS = {0, 16, 64};
    private static final int WARMUP_TICKS = Math.clamp(Integer.getInteger("reimagined.profileWarmupTicks", 200), 200, 1200);
    private static final int MEASURE_TICKS = 600;
    private static final int MAX_WRITES_PER_TICK = 32;
    private static final long PHASE_TIMEOUT_NS = 120_000_000_000L;
    private static Run run;
    private MechanicalProfile() {}

    @Name("reimagined.MechanicalTick")
    @Label("Measured normal Minecraft server tick")
    public static final class MeasuredTick extends Event {
        @Label("Independent wheel-shaft-mill components") public int components;
        @Label("Server tick") public long tick;
        @Label("Actual per-tick vanilla tick body nanoseconds") public long tickDurationNanos;
        @Label("Server thread CPU nanoseconds between tick events; -1 if unsupported") public long serverThreadCpuNanos;
        @Label("Wall nanoseconds between tick events") public long eventPairWallNanos;
        @Label("Overworld post-tick bus wall nanoseconds (contains network scheduler)") public long levelPostWallNanos;
        @Label("Overworld post-tick bus server-thread CPU nanoseconds") public long levelPostCpuNanos;
    }

    @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.HIGHEST)
    public static void beforeTick(ServerTickEvent.Pre event) {
        if (ENABLED && run != null && !run.finished) run.beginTick();
    }

    @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.HIGHEST)
    public static void beforeLevelPost(LevelTickEvent.Post event) {
        if (!ENABLED || run == null || run.finished || event.getLevel() != run.level) return;
        run.levelPostStart = System.nanoTime();
        run.levelPostCpuStart = run.cpu.isCurrentThreadCpuTimeSupported() ? run.cpu.getCurrentThreadCpuTime() : -1;
    }

    @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.LOWEST)
    public static void afterLevelPost(LevelTickEvent.Post event) {
        if (!ENABLED || run == null || run.finished || event.getLevel() != run.level) return;
        run.levelPostCpu = run.levelPostCpuStart < 0 ? -1 : run.cpu.getCurrentThreadCpuTime() - run.levelPostCpuStart;
        run.levelPostWall = System.nanoTime() - run.levelPostStart;
    }

    @SubscribeEvent public static void started(ServerStartedEvent event) {
        if (!ENABLED) return;
        var server = event.getServer();
        try {
            if (server instanceof net.minecraft.gametest.framework.GameTestServer)
                throw new IllegalStateException("Profiling must use an ordinary server, not the accelerated GameTest runner");
            if (!Files.isRegularFile(Path.of(".profile-approved-disposable")))
                throw new IllegalStateException("Profiling requires .profile-approved-disposable in its isolated game directory");
            if (server.getPlayerList().getPlayerCount() != 0)
                throw new IllegalStateException("This isolated profile requires no connected players");
            for (String command : List.of("gamerule spawnChunkRadius 4", "gamerule doMobSpawning false", "gamerule doDaylightCycle false", "gamerule doWeatherCycle false", "weather clear"))
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), command);
            if (Boolean.getBoolean("reimagined.mechanicalRestartProbe")) { MechanicalRestartProbe.start(server); return; }
            run = new Run(server);
            run.nextPhase();
        } catch (Exception error) {
            LOGGER.error("Mechanical profile refused to start", error);
            server.halt(false);
        }
    }

    @SubscribeEvent public static void tick(ServerTickEvent.Post event) {
        if (!ENABLED) return;
        // Headless servers otherwise suspend all block-entity ticks after300 empty ticks.
        event.getServer().overworld().resetEmptyTime();
        if (MechanicalRestartProbe.tick(event.getServer())) return;
        if (run == null || run.finished) return;
        try { run.tick(); }
        catch (Exception error) { run.fail(error); }
    }

    @SubscribeEvent public static void stopped(ServerStoppedEvent event) {
        if (run == null) return;
        if (!run.finished) run.fail(new IllegalStateException("Server stopped before all measured phases completed"));
        run = null;
    }

    private record Placement(BlockPos pos, BlockState state) {}
    private enum Stage { SETUP, WARMUP, MEASURE }
    private static final class Run {
        final MinecraftServer server;
        final ServerLevel level;
        final JsonObject report = new JsonObject();
        final JsonArray phases = new JsonArray();
        final ArrayDeque<Placement> writes = new ArrayDeque<>();
        final List<BlockPos> previous = new ArrayList<>();
        final List<BlockPos> mills = new ArrayList<>();
        final long[] tickTimes = new long[MEASURE_TICKS];
        final long[] cpuTimes = new long[MEASURE_TICKS];
        final long[] levelPostTimes = new long[MEASURE_TICKS];
        final long[] levelPostCpuTimes = new long[MEASURE_TICKS];
        final JsonArray slowTicks = new JsonArray();
        final java.lang.management.ThreadMXBean cpu = ManagementFactory.getThreadMXBean();
        long tickWallStart, tickCpuStart;
        long levelPostStart, levelPostCpuStart, levelPostWall, levelPostCpu;
        final Path directory = Path.of("reports");
        final String name;
        int phase = -1, counter;
        Stage stage;
        Recording recording;
        long phaseStart, measureStart, heapStart;
        Instant measureStartUtc;
        boolean finished;

        Run(MinecraftServer server) throws Exception {
            this.server = server; level = server.overworld();
            if (cpu.isCurrentThreadCpuTimeSupported() && !cpu.isThreadCpuTimeEnabled()) cpu.setThreadCpuTimeEnabled(true);
            Files.createDirectories(directory);
            String label = System.getProperty("reimagined.profileLabel", "mechanical").replaceAll("[^a-zA-Z0-9_-]", "_");
            name = label + "-" + System.currentTimeMillis();
            report.addProperty("label", label);
            report.addProperty("revision", System.getProperty("reimagined.profileRevision", "UNSPECIFIED"));
            report.addProperty("startedUtc", Instant.now().toString());
            report.addProperty("java", System.getProperty("java.runtime.version"));
            report.addProperty("vm", System.getProperty("java.vm.name"));
            report.addProperty("os", System.getProperty("os.name") + " " + System.getProperty("os.arch"));
            report.addProperty("processors", Runtime.getRuntime().availableProcessors());
            report.addProperty("maxHeapBytes", Runtime.getRuntime().maxMemory());
            JsonArray jvmFlags = new JsonArray();
            for (String argument : ManagementFactory.getRuntimeMXBean().getInputArguments())
                if (argument.startsWith("-Xmx") || argument.startsWith("-Xms") || argument.startsWith("-XX:+Use")
                        || argument.startsWith("-XX:-Use") || argument.startsWith("-XX:Max")
                        || argument.startsWith("-XX:Initial") || argument.startsWith("-XX:Min")) jvmFlags.add(argument);
            report.add("memoryGcJvmFlags", jvmFlags);
            report.addProperty("jvmFlagsNote", "Only memory/GC flags are recorded; arbitrary system properties/arguments may contain credentials and are omitted.");
            report.addProperty("generator", level.getChunkSource().getGenerator().getClass().getName());
            report.addProperty("minecraft", "1.21.1");
            report.addProperty("neoforge", "21.1.264");
            report.addProperty("seed", level.getSeed());
            report.addProperty("viewDistance", server.getPlayerList().getViewDistance());
            report.addProperty("simulationDistance", server.getPlayerList().getSimulationDistance());
            report.addProperty("playerCount", 0);
            report.addProperty("warmupTicksPerPhase", WARMUP_TICKS);
            report.addProperty("measurementTicksPerPhase", MEASURE_TICKS);
            report.addProperty("cpuTimingNote", "CPU is the main server thread only, not chunk/IO/JIT/GC workers. Event-pair wall time brackets tick events and differs slightly from the vanilla tick ring. Neither proves a sampled frame caused the whole tick.");
            report.addProperty("notes", "Normal server, no clients. 3 mechanical blocks/component, continuous wheat milling. Setup/cleanup excluded. JFR includes warmup; custom tick events cover measurement only. Heap change is not a leak verdict. No forced GC or mechanism chunk loading. Vanilla spawn radius4 supplies ticking chunks; empty-world suspension is reset to simulate presence without a client. Individual tick samples use the vanilla server tick-time ring, not averaged JFR TickTime events.");
            report.add("phases", phases);
        }

        void nextPhase() throws Exception {
            if (phase == COUNTS.length - 1) {
                MechanicalRestartProbe.saveExpected(level, mills);
                finish(); return;
            }
            for (BlockPos pos : mills) {
                if (!level.hasChunkAt(pos)) throw new IllegalStateException("Previously measured mill chunk unloaded");
                if (level.getBlockEntity(pos) instanceof MachineBlockEntity machine) machine.clearContent();
            }
            mills.clear();
            // Water was placed last; remove it first so cleanup cannot flood the next scenario.
            for (int i = previous.size() - 1; i >= 0; i--) writes.addLast(new Placement(previous.get(i), Blocks.AIR.defaultBlockState()));
            previous.clear();
            phase++;
            if (phase >= COUNTS.length) { finish(); return; }
            stage = Stage.SETUP; counter = 0; phaseStart = System.nanoTime();
            slowTicks.asList().clear();
            BlockPos spawn = level.getSharedSpawnPos();
            int y = spawn.getY() + 3;
            for (int index = 0; index < COUNTS[phase]; index++) {
                BlockPos wheel = new BlockPos(spawn.getX() - 24 + index % 8 * 6, y, spawn.getZ() - 24 + index / 8 * 6);
                fixture(wheel);
            }
            LOGGER.info("Mechanical profile phase {}: {} components, {} queued writes", phase, COUNTS[phase], writes.size());
        }

        void place(BlockPos pos, BlockState state) {
            if (writes.size() >= 2048) throw new IllegalStateException("Fixture write budget exceeded");
            writes.addLast(new Placement(pos.immutable(), state));
            previous.add(pos.immutable());
        }
        void fixture(BlockPos wheel) {
            for (int z = 1; z <= 2; z++) {
                place(wheel.offset(0, -1, z), Blocks.STONE.defaultBlockState());
                place(wheel.offset(-1, 0, z), Blocks.STONE.defaultBlockState());
                place(wheel.offset(1, 0, z), Blocks.STONE.defaultBlockState());
            }
            place(wheel.offset(0, 0, 3), Blocks.STONE.defaultBlockState());
            place(wheel, EngineeringContent.WATER_WHEEL.get().defaultBlockState().setValue(MechanicalBlock.AXIS, Direction.Axis.X));
            place(wheel.east(), EngineeringContent.SHAFT.get().defaultBlockState().setValue(MechanicalBlock.AXIS, Direction.Axis.X));
            place(wheel.east(2), EngineeringContent.MILLSTONE.get().defaultBlockState());
            mills.add(wheel.east(2));
            place(wheel.offset(0, 0, 2), Blocks.WATER.defaultBlockState());
            place(wheel.offset(0, 0, 1), Blocks.WATER.defaultBlockState().setValue(LiquidBlock.LEVEL, 1));
        }

        void tick() throws Exception {
            if (System.nanoTime() - phaseStart > PHASE_TIMEOUT_NS)
                throw new IllegalStateException("Phase wall-time budget exceeded (120s)");
            if (stage == Stage.SETUP) {
                for (int i = 0; i < MAX_WRITES_PER_TICK && !writes.isEmpty(); i++) {
                    Placement action = writes.removeFirst();
                    if (!level.hasChunkAt(action.pos()) || !level.shouldTickBlocksAt(net.minecraft.world.level.ChunkPos.asLong(action.pos()))) throw new IllegalStateException("Fixture attempted unloaded access: " + action.pos());
                    level.setBlock(action.pos(), action.state(), Block.UPDATE_ALL);
                }
                if (!writes.isEmpty()) return;
                for (BlockPos pos : mills) {
                    if (!(level.getBlockEntity(pos) instanceof MachineBlockEntity machine)) throw new IllegalStateException("Mill block entity missing");
                    machine.clearContent();
                    machine.setItem(0, new ItemStack(Items.WHEAT, 64));
                }
                recording = new Recording(Configuration.getConfiguration("profile"));
                // Keep recordings useful without capturing arbitrary environment/properties/CLI secrets.
                recording.disable("jdk.InitialEnvironmentVariable");
                recording.disable("jdk.InitialSystemProperty");
                recording.disable("jdk.JVMInformation");
                recording.enable(MeasuredTick.class);
                recording.setName(name + "-" + COUNTS[phase]);
                recording.start();
                stage = Stage.WARMUP; counter = 0;
                return;
            }
            if (stage == Stage.WARMUP) {
                if (++counter < WARMUP_TICKS) return;
                measureStart = System.nanoTime(); measureStartUtc = Instant.now();
                heapStart = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
                MechanicalNetworks.resetStats(level);
                counter = 0; stage = Stage.MEASURE;
                return;
            }
            // The current ring entry is populated by Minecraft before ServerTickEvent.Post.
            long elapsed = server.getTickTimesNanos()[Math.floorMod(server.getTickCount(), 100)];
            long threadCpu = tickCpuStart < 0 ? -1 : cpu.getCurrentThreadCpuTime() - tickCpuStart;
            long wall = System.nanoTime() - tickWallStart;
            tickTimes[counter] = elapsed;
            levelPostTimes[counter] = levelPostWall;
            levelPostCpuTimes[counter] = levelPostCpu;
            cpuTimes[counter++] = threadCpu;
            if (elapsed > 50_000_000L) {
                JsonObject slow = new JsonObject();
                slow.addProperty("tick", server.getTickCount());
                slow.addProperty("tickMs", elapsed / 1e6);
                slow.addProperty("eventPairWallMs", wall / 1e6);
                slow.addProperty("serverThreadCpuMs", threadCpu < 0 ? -1 : threadCpu / 1e6);
                slow.addProperty("levelPostWallMs", levelPostWall / 1e6);
                slow.addProperty("levelPostCpuMs", levelPostCpu < 0 ? -1 : levelPostCpu / 1e6);
                slowTicks.add(slow);
            }
            MeasuredTick sample = new MeasuredTick();
            sample.components = COUNTS[phase]; sample.tick = server.getTickCount(); sample.tickDurationNanos = elapsed;
            sample.serverThreadCpuNanos = threadCpu; sample.eventPairWallNanos = wall;
            sample.levelPostWallNanos = levelPostWall; sample.levelPostCpuNanos = levelPostCpu;
            sample.commit();
            if (counter == MEASURE_TICKS) completePhase();
        }

        void beginTick() {
            levelPostWall = 0; levelPostCpu = 0;
            tickWallStart = System.nanoTime();
            tickCpuStart = cpu.isCurrentThreadCpuTimeSupported() ? cpu.getCurrentThreadCpuTime() : -1;
        }

        void completePhase() throws Exception {
            long duration = System.nanoTime() - measureStart;
            long heapEnd = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
            int outputs = 0, workingMills = 0;
            for (BlockPos pos : mills) {
                if (!level.hasChunkAt(pos) || !(level.getBlockEntity(pos) instanceof MachineBlockEntity machine))
                    throw new IllegalStateException("Measured mill unloaded or missing");
                if (machine.getItem(1).is(EngineeringContent.FLOUR.get())) {
                    outputs += machine.getItem(1).getCount();
                    if (machine.getItem(1).getCount() > 0) workingMills++;
                }
            }
            Path jfr = directory.resolve(name + "-" + COUNTS[phase] + ".jfr");
            recording.stop(); recording.dump(jfr); recording.close(); recording = null;
            long[] sorted = tickTimes.clone(); Arrays.sort(sorted);
            long total = 0; int overBudget = 0;
            for (long value : sorted) { total += value; if (value > 50_000_000L) overBudget++; }
            JsonObject result = new JsonObject();
            result.addProperty("components", COUNTS[phase]);
            result.addProperty("mechanicalBlocks", COUNTS[phase] * 3);
            result.addProperty("measurementStartUtc", measureStartUtc.toString());
            result.addProperty("measurementEndUtc", Instant.now().toString());
            result.addProperty("ticks", counter);
            result.addProperty("wallSeconds", duration / 1e9);
            result.addProperty("observedTps", counter * 1e9 / duration);
            result.addProperty("tickMeanMs", total / (double) counter / 1e6);
            result.addProperty("tickP95Ms", sorted[(int) Math.ceil(counter * .95) - 1] / 1e6);
            result.addProperty("tickP99Ms", sorted[(int) Math.ceil(counter * .99) - 1] / 1e6);
            result.addProperty("tickMaxMs", sorted[counter - 1] / 1e6);
            result.addProperty("ticksOver50ms", overBudget);
            long[] sortedCpu = cpuTimes.clone(); Arrays.sort(sortedCpu);
            if (sortedCpu[0] >= 0) {
                result.addProperty("serverThreadCpuMeanMs", Arrays.stream(sortedCpu).average().orElseThrow() / 1e6);
                result.addProperty("serverThreadCpuP99Ms", sortedCpu[(int) Math.ceil(counter * .99) - 1] / 1e6);
            }
            result.add("slowTicks", slowTicks.deepCopy());
            result.addProperty("levelPostMeanMs", Arrays.stream(levelPostTimes).average().orElseThrow() / 1e6);
            result.addProperty("levelPostMaxMs", Arrays.stream(levelPostTimes).max().orElseThrow() / 1e6);
            result.addProperty("levelPostCpuMaxMs", Arrays.stream(levelPostCpuTimes).max().orElseThrow() / 1e6);
            result.addProperty("mechanicalScans", MechanicalNetworks.stats(level).scans());
            result.addProperty("mechanicalMaxScansPerTick", MechanicalNetworks.stats(level).maxTickScans());
            result.addProperty("heapUsedBeforeBytes", heapStart);
            result.addProperty("heapUsedAfterBytes", heapEnd);
            result.addProperty("loadedChunks", level.getChunkSource().getLoadedChunksCount());
            result.addProperty("flourOutput", outputs);
            result.addProperty("millsWithOutput", workingMills);
            result.addProperty("jfr", jfr.toString());
            phases.add(result);
            writeReport(false, null);
            LOGGER.info("PROFILE phase {} complete: {}components, {}TPS, mean {}ms, p99 {}ms, output {}", phase, COUNTS[phase], counter * 1e9 / duration, total / (double) counter / 1e6, sorted[(int) Math.ceil(counter * .99) - 1] / 1e6, outputs);
            if (workingMills != COUNTS[phase]) throw new IllegalStateException("Not every measured mill actually produced flour");
            nextPhase();
        }

        void finish() throws Exception {
            report.addProperty("finishedUtc", Instant.now().toString());
            writeReport(true, null);
            finished = true;
            LOGGER.info("Mechanical profile completed: {}", directory.resolve(name + ".json").toAbsolutePath());
            server.halt(false);
        }
        void writeReport(boolean complete, String error) throws Exception {
            report.addProperty("complete", complete);
            if (error != null) report.addProperty("error", error);
            Files.writeString(directory.resolve(name + ".json"), new GsonBuilder().setPrettyPrinting().create().toJson(report));
        }
        void fail(Exception error) {
            if (finished) return;
            finished = true;
            LOGGER.error("Mechanical profile incomplete", error);
            try {
                if (recording != null) {
                    if (recording.getState() == jdk.jfr.RecordingState.RUNNING) recording.stop();
                    recording.dump(directory.resolve(name + "-incomplete.jfr")); recording.close(); recording = null;
                }
                writeReport(false, error.toString());
            } catch (Exception reportError) { LOGGER.error("Could not persist incomplete profile report", reportError); }
            server.halt(false);
        }
    }
}
