package dev.reimagined.cooking.profile;

import com.google.gson.*;
import dev.reimagined.cooking.*;
import dev.reimagined.agriculture.AgricultureContent;
import java.lang.management.ManagementFactory;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import jdk.jfr.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.*;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.LoggerFactory;

/** Opt-in normal-server development profile, never packaged in the player mod. */
@EventBusSubscriber(modid="reimagined")
public final class CookingProfile {
    private static final boolean ENABLED=Boolean.getBoolean("reimagined.cookingProfile");
    private static final int[] COUNTS={0,64,64,256};
    private static final int WARMUP=200, MEASURE=600;
    private static Run run;
    @Name("reimagined.CookingTick")
    @Label("Measured ordinary server tick in cooking scenario")
    public static final class MeasuredTick extends Event {
        public int phase;
        public long tick;
        public long durationNanos;
    }
    @SubscribeEvent public static void start(ServerStartedEvent event) {
        if(!ENABLED)return;
        try {
            if(event.getServer() instanceof net.minecraft.gametest.framework.GameTestServer || !Files.isRegularFile(Path.of(".profile-approved-disposable")))
                throw new IllegalStateException("Requires an ordinary server in an explicitly disposable game directory");
            run=new Run(event.getServer());run.next();
        } catch(Exception ex) { LoggerFactory.getLogger(CookingProfile.class).error("Cooking profile refused",ex);event.getServer().halt(false); }
    }
    @SubscribeEvent public static void tick(ServerTickEvent.Post event) {
        if(!ENABLED || run==null || run.finished)return;
        try { run.tick(); } catch(Exception ex) { run.fail(ex); }
    }
    @SubscribeEvent public static void stop(ServerStoppedEvent event) {
        if(run!=null && !run.finished)run.fail(new IllegalStateException("Server stopped early"));run=null;
    }
    private static final class Run {
        final MinecraftServer server; final ServerLevel level;
        final JsonObject report=new JsonObject();final JsonArray phases=new JsonArray();
        final List<BlockPos> positions=new ArrayList<>();final ArrayDeque<BlockPos> cleanup=new ArrayDeque<>();final long[] times=new long[MEASURE];
        final Path path;final String name;
        int phase=-1,setup,counter;long started,measureStart;Instant measurementStartUtc;boolean finished,measuring;
        Recording recording;
        Run(MinecraftServer server)throws Exception {
            this.server=server;level=server.overworld();
            if(server.getPlayerList().getPlayerCount()!=0)throw new IllegalStateException("No players allowed in isolated profile");
            for(String cmd:List.of("gamerule spawnChunkRadius 4","gamerule doMobSpawning false","gamerule doDaylightCycle false","gamerule doWeatherCycle false","weather clear"))
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),cmd);
            name="cooking-"+System.currentTimeMillis();Files.createDirectories(Path.of("reports"));path=Path.of("reports",name+".json");
            report.addProperty("revision",System.getProperty("reimagined.profileRevision","working-tree"));
            report.addProperty("startedUtc",Instant.now().toString());report.addProperty("java",System.getProperty("java.runtime.version"));
            report.addProperty("minecraft","1.21.1");report.addProperty("neoforge","21.1.264");
            report.addProperty("processors",Runtime.getRuntime().availableProcessors());report.addProperty("maxHeapBytes",Runtime.getRuntime().maxMemory());
            report.addProperty("seed",level.getSeed());report.addProperty("viewDistance",server.getPlayerList().getViewDistance());report.addProperty("simulationDistance",server.getPlayerList().getSimulationDistance());
            report.addProperty("warmupTicks",WARMUP);report.addProperty("measureTicks",MEASURE);
            report.addProperty("notes","Ordinary20TPS server, no clients. Same loaded region; phase0empty,phase1:64empty pots,phase2:64cooking,phase3:256cooking. Magma heat,64barley/apple/bowls each. Setup capped16pots/tick, no forced chunk access. Empty-time reset for headless activity. JFR profile warmup+measurement, no arbitrary env/properties/CLI capture. Per-tick vanilla ring, not averaged TickTime. No forcedGC; heap is not retained-memory proof. No network/save-frequency instrumentation.");
            report.add("phases",phases);
        }
        void next()throws Exception {
            cleanup.addAll(positions);
            positions.clear();phase++;setup=counter=0;measuring=false;started=System.nanoTime();
        }
        void tick()throws Exception {
            level.resetEmptyTime();
            if(server.getPlayerList().getPlayerCount()!=0)throw new IllegalStateException("Unexpected player");
            if(System.nanoTime()-started>120_000_000_000L)throw new IllegalStateException("Phase exceeds120seconds");
            if(!cleanup.isEmpty()) {
                for(int i=0;i<16 && !cleanup.isEmpty();i++) {
                    BlockPos pos=cleanup.removeFirst();
                    var chunk=level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4);
                    if(chunk==null)throw new IllegalStateException("Profile cleanup chunk unloaded");
                    if(chunk.getBlockEntity(pos) instanceof CookingPotBlockEntity pot)pot.clearContent();
                    level.setBlock(pos,Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL);
                    level.setBlock(pos.below(),Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL);
                }
                return;
            }
            if(phase==COUNTS.length) {finished=true;report.addProperty("complete",true);write();server.halt(false);return;}
            int count=COUNTS[phase];
            if(setup<count) {
                for(int n=0;n<16 && setup<count;n++,setup++) {
                    BlockPos origin=level.getSharedSpawnPos();
                    BlockPos pos=origin.offset(-24+(setup%16)*3,4,-24+(setup/16)*3);
                    var chunk=level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4);
                    if(chunk==null || !level.shouldTickBlocksAt(net.minecraft.world.level.ChunkPos.asLong(pos)))throw new IllegalStateException("Fixture not ready/ticking");
                    level.setBlock(pos.below(),Blocks.MAGMA_BLOCK.defaultBlockState(),Block.UPDATE_ALL);
                    level.setBlock(pos,CookingContent.POT.get().defaultBlockState(),Block.UPDATE_ALL);positions.add(pos);
                    if(phase>=2) {
                        var pot=(CookingPotBlockEntity)chunk.getBlockEntity(pos);
                        pot.setItem(0,new ItemStack(AgricultureContent.BARLEY.get(),64));pot.setItem(1,new ItemStack(Items.APPLE,64));pot.setItem(3,new ItemStack(Items.BOWL,64));
                    }
                }
                return;
            }
            if(recording==null) {
                recording=new Recording(Configuration.getConfiguration("profile"));
                recording.disable("jdk.InitialEnvironmentVariable");recording.disable("jdk.InitialSystemProperty");recording.disable("jdk.JVMInformation");
                recording.enable(MeasuredTick.class);recording.setName(name+"-"+phase);recording.start();return;
            }
            if(!measuring) {
                if(++counter<WARMUP)return;counter=0;measuring=true;measureStart=System.nanoTime();measurementStartUtc=Instant.now();return;
            }
            long duration=server.getTickTimesNanos()[Math.floorMod(server.getTickCount(),100)];
            times[counter++]=duration;
            var sample=new MeasuredTick();sample.phase=phase;sample.tick=server.getTickCount();sample.durationNanos=duration;sample.commit();
            if(counter==MEASURE)complete();
        }
        void complete()throws Exception {
            long elapsed=System.nanoTime()-measureStart;Instant measurementEndUtc=Instant.now();int working=0,outputs=0;
            for(BlockPos pos:positions) {
                var chunk=level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4);
                if(chunk==null || !(chunk.getBlockEntity(pos) instanceof CookingPotBlockEntity))throw new IllegalStateException("Measured pot missing");
                var pot=(CookingPotBlockEntity)chunk.getBlockEntity(pos);ItemStack result=pot.getItem(4);
                if(result.is(CookingContent.BARLEY_PORRIDGE.get())) {working++;outputs+=result.getCount();}
                if(phase==1 && (!pot.isEmpty() || pot.progress()!=0 || level.getBlockTicks().hasScheduledTick(pos,CookingContent.POT.get())))
                    throw new IllegalStateException("Idle pot must have no periodic scheduled work");
                if(phase>=2 && (!result.is(CookingContent.BARLEY_PORRIDGE.get()) || result.getCount()!=3 || pot.getItem(0).getCount()!=61 || pot.getItem(3).getCount()!=61))
                    throw new IllegalStateException("Every pot must actually cook3meals, consuming matching inputs and bowls");
            }
            long[] sorted=times.clone();Arrays.sort(sorted);
            JsonObject data=new JsonObject();data.addProperty("phase",phase);data.addProperty("pots",COUNTS[phase]);data.addProperty("active",phase>=2);
            data.addProperty("measurementStartUtc",measurementStartUtc.toString());data.addProperty("measurementEndUtc",measurementEndUtc.toString());
            data.addProperty("ticks",counter);data.addProperty("wallSeconds",elapsed/1e9);data.addProperty("tps",counter*1e9/elapsed);
            data.addProperty("meanMs",Arrays.stream(sorted).average().orElseThrow()/1e6);data.addProperty("p95Ms",sorted[569]/1e6);data.addProperty("p99Ms",sorted[593]/1e6);data.addProperty("maxMs",sorted[599]/1e6);
            data.addProperty("over50ms",Arrays.stream(sorted).filter(v->v>50_000_000).count());
            data.addProperty("workingPots",working);data.addProperty("mealOutput",outputs);data.addProperty("loadedChunks",level.getChunkSource().getLoadedChunksCount());
            data.addProperty("heapUsedBytes",ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed());
            Path jfr=Path.of("reports",name+"-"+phase+".jfr");recording.stop();recording.dump(jfr);recording.close();recording=null;
            data.addProperty("jfr",jfr.toString());phases.add(data);report.addProperty("complete",false);write();
            LoggerFactory.getLogger(CookingProfile.class).info("COOK_PROFILE {}",data);next();
        }
        void write()throws Exception {Files.writeString(path,new GsonBuilder().setPrettyPrinting().create().toJson(report));}
        void fail(Exception ex) {
            finished=true;LoggerFactory.getLogger(CookingProfile.class).error("Incomplete cooking profile",ex);
            try {report.addProperty("complete",false);report.addProperty("error",ex.toString());write();if(recording!=null)recording.close();}catch(Exception ignored){}
            server.halt(false);
        }
    }
}
