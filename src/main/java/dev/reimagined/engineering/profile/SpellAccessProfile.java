package dev.reimagined.engineering.profile;

import com.google.gson.*;
import com.mojang.authlib.GameProfile;
import dev.reimagined.magic.SpellWorldAccess;
import java.nio.file.*;
import java.util.*;
import jdk.jfr.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.server.*;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.LoggerFactory;

/** Opt-in, ordinary-server loaded spell-ray workload. Excluded from the player JAR with other probes. */
@EventBusSubscriber(modid="reimagined")
public final class SpellAccessProfile {
    private static final boolean ENABLED=Boolean.getBoolean("reimagined.spellAccessProfile");
    private static Run run;
    @Name("reimagined.SpellAccessTick") static final class Sample extends Event {
        public int rays; public long workloadNanos; public long tickNanos;
    }
    @SubscribeEvent public static void start(ServerStartedEvent e) {
        if(!ENABLED)return;
        try {
            if(!Files.isRegularFile(Path.of(".profile-approved-disposable")) || !e.getServer().getPlayerList().getPlayers().isEmpty())
                throw new IllegalStateException("Requires a marked disposable server without clients");
            run=new Run(e.getServer());
        } catch(Exception ex) {LoggerFactory.getLogger(SpellAccessProfile.class).error("Spell profile refused",ex);e.getServer().halt(false);}
    }
    @SubscribeEvent public static void tick(ServerTickEvent.Post e) {
        if(run==null)return;
        try {run.tick();} catch(Exception ex) {LoggerFactory.getLogger(SpellAccessProfile.class).error("Spell profile incomplete",ex);run.close();run=null;e.getServer().halt(false);}
    }
    @SubscribeEvent public static void stop(ServerStoppedEvent e) {if(run!=null)run.close();run=null;}
    private static final class Run {
        final MinecraftServer server; final FakePlayer actor;
        final JsonObject report=new JsonObject(); final JsonArray phases=new JsonArray();
        final long[] workload=new long[600],ticks=new long[600];
        final String name="spell-access-"+System.currentTimeMillis();
        int phase,counter; long start; Recording recording;
        Run(MinecraftServer s) throws Exception {
            server=s; var level=s.overworld();
            for(String command:List.of("gamerule spawnChunkRadius 4","gamerule doMobSpawning false","gamerule doDaylightCycle false","gamerule doWeatherCycle false","weather clear"))
                s.getCommands().performPrefixedCommand(s.createCommandSourceStack(),command);
            for(int x=-1;x<=2;x++)for(int z=-1;z<=2;z++)
                if(level.getChunkSource().getChunkNow(x,z)==null)throw new IllegalStateException("Scene chunks not ready");
            // Two small loaded-only walls give both collision and miss rays, including chunk crossings.
            for(int x=7;x<=9;x++)for(int y=-58;y<=-56;y++)level.setBlock(new BlockPos(x,y,20),Blocks.STONE.defaultBlockState(),2);
            actor=new FakePlayer(level,new GameProfile(UUID.fromString("718d2734-6b9a-4ca9-9409-e122cb05cf41"),"SpellProfile"));
            actor.setPos(8.5,-59,8.5);
            report.addProperty("revision",System.getProperty("reimagined.profileRevision","working-tree"));
            report.addProperty("notes","Java21/4 active processors/2GiB; 600 warmup and600 measured ticks per phase;0/256 loaded-only16-block rays per tick using one unspawned fake player. No mana/cooldown bypass in gameplay: this measures ray access only, not full spells or multiplayer. No clients/forced chunks. Workload timer is separate from vanilla tick ring. JFR captures measured windows only; no environment/property/CLI capture.");
            Files.createDirectories(Path.of("reports"));
        }
        void tick() throws Exception {
            int rays=phase==0?0:256;
            if(counter==600) {
                recording=new Recording(Configuration.getConfiguration("profile"));
                for(String event:List.of("jdk.InitialEnvironmentVariable","jdk.InitialSystemProperty","jdk.JVMInformation"))recording.disable(event);
                recording.enable(Sample.class);recording.start();start=System.nanoTime();
            }
            long begin=System.nanoTime();
            for(int i=0;i<rays;i++) {actor.setYRot((i%8)*45); SpellWorldAccess.ray(actor,16,ClipContext.Block.COLLIDER);}
            long duration=System.nanoTime()-begin;
            if(counter>=600) {
                int index=counter-600;workload[index]=duration;ticks[index]=server.getTickTimesNanos()[Math.floorMod(server.getTickCount(),100)];
                Sample sample=new Sample();sample.rays=rays;sample.workloadNanos=duration;sample.tickNanos=ticks[index];sample.commit();
            }
            if(++counter<1200)return;
            double seconds=(System.nanoTime()-start)/1e9;
            recording.stop();recording.dump(Path.of("reports",name+"-"+rays+".jfr"));close();
            Arrays.sort(workload);Arrays.sort(ticks);
            JsonObject p=new JsonObject();p.addProperty("raysPerTick",rays);p.addProperty("ticks",600);p.addProperty("wallSeconds",seconds);p.addProperty("tps",600/seconds);
            p.addProperty("workMeanMs",Arrays.stream(workload).average().orElseThrow()/1e6);p.addProperty("workP99Ms",workload[593]/1e6);p.addProperty("workMaxMs",workload[599]/1e6);
            p.addProperty("tickMeanMs",Arrays.stream(ticks).average().orElseThrow()/1e6);p.addProperty("tickP99Ms",ticks[593]/1e6);p.addProperty("tickMaxMs",ticks[599]/1e6);p.addProperty("over50ms",Arrays.stream(ticks).filter(t->t>50_000_000).count());phases.add(p);
            report.add("phases",phases);report.addProperty("complete",phase==1);
            Files.writeString(Path.of("reports",name+".json"),new GsonBuilder().setPrettyPrinting().create().toJson(report));
            if(++phase==2) {run=null;server.halt(false);} else counter=0;
        }
        void close(){if(recording!=null){recording.close();recording=null;}}
    }
}
