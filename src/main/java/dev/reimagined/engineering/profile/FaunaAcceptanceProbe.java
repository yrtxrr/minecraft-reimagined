package dev.reimagined.engineering.profile;

import dev.reimagined.creature.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.LoggerFactory;

/** Read-only opt-in client acceptance telemetry, excluded from the player JAR. */
@EventBusSubscriber(modid="reimagined")
public final class FaunaAcceptanceProbe {
    private static final boolean ENABLED=Boolean.getBoolean("reimagined.faunaAcceptance");
    private static final long[] samples=new long[600];
    private static final java.util.Set<String> completed=new java.util.HashSet<>();
    private static final String[] LABELS={"fauna_baseline","fauna_candidate"};
    private static String label;
    private static int sampleIndex,warmup;
    private static long tickStart,started,heapStart,gcStart;
    private static long heap() { var r=Runtime.getRuntime();return r.totalMemory()-r.freeMemory(); }
    private static long gcMillis() { return java.lang.management.ManagementFactory.getGarbageCollectorMXBeans().stream().mapToLong(b->Math.max(0,b.getCollectionTime())).sum(); }
    @SubscribeEvent public static void pre(ServerTickEvent.Pre event) { if(ENABLED && label!=null) tickStart=System.nanoTime(); }
    @SubscribeEvent public static void measure(ServerTickEvent.Post event) {
        if(!ENABLED)return;
        if(label==null) {
            var players=event.getServer().getPlayerList().getPlayers();if(players.size()!=1)return;
            for(String candidate:LABELS)
                if(players.getFirst().getTags().contains(candidate) && !completed.contains(candidate)) {
                    label=candidate;warmup=100;sampleIndex=0;started=System.nanoTime();heapStart=heap();gcStart=gcMillis();
                    LoggerFactory.getLogger(FaunaAcceptanceProbe.class).info("NATIVE_FAUNA_PROFILE_START {} warmup=100 samples=600",label);break;
                }
            return;
        }
        if(warmup-->0)return;
        samples[sampleIndex++]=System.nanoTime()-tickStart;
        if(sampleIndex==samples.length) {
            var sorted=samples.clone();java.util.Arrays.sort(sorted);
            double mean=java.util.Arrays.stream(sorted).average().orElseThrow()/1e6;
            LoggerFactory.getLogger(FaunaAcceptanceProbe.class).info("NATIVE_FAUNA_PROFILE {} n={} meanMs={} p95Ms={} p99Ms={} maxMs={} elapsedSeconds={} heapBefore={} heapAfter={} gcMillis={}",
                    label,sorted.length,mean,sorted[569]/1e6,sorted[593]/1e6,sorted[599]/1e6,(System.nanoTime()-started)/1e9,heapStart,heap(),gcMillis()-gcStart);
            completed.add(label);label=null;
        }
    }
    @SubscribeEvent public static void tick(ServerTickEvent.Post event) {
        if(!ENABLED || label!=null || event.getServer().getTickCount()%20!=0)return;
        // A single local query per second, maximum 16 tagged QA subjects; never mutates AI.
        var players=event.getServer().getPlayerList().getPlayers();if(players.size()!=1)return;
        var p=players.getFirst();
        var subjects=p.serverLevel().getEntitiesOfClass(net.minecraft.world.entity.animal.Animal.class,p.getBoundingBox().inflate(32),e->e.getTags().contains("fauna_qa"));
        for(int i=0;i<Math.min(subjects.size(),16);i++) {
            var a=subjects.get(i);
            LoggerFactory.getLogger(FaunaAcceptanceProbe.class).info("NATIVE_FAUNA id={} type={} pos={} water={} ground={} rest={} shelter={} look={} head={}",
                    a.getId(),a.getType().toShortString(),a.position(),a.isInWater(),a.onGround(),a instanceof Otter o && o.isResting(),a instanceof Badger b && b.isSheltering(),a instanceof Deer d && d.isLookingAround(),a.yHeadRot);
        }
    }
    private FaunaAcceptanceProbe() {}
}
