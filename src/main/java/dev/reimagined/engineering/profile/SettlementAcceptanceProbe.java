package dev.reimagined.engineering.profile;

import dev.reimagined.settlements.Settler;
import java.util.*;
import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.LoggerFactory;

/** Explicit disposable-world QA only, excluded from the player JAR by the existing profile rule. */
@EventBusSubscriber(modid="reimagined")
public final class SettlementAcceptanceProbe {
    private static final boolean ENABLED=Boolean.getBoolean("reimagined.settlementAcceptance");
    private static final String[] LABELS={"settlement_profile_baseline","settlement_profile_32"};
    private static final Set<String> done=new HashSet<>();
    private static final long[] samples=new long[600];
    private static List<Settler> actors=List.of();
    private static String label;
    private static int warmup,index;
    private static long start,tickStart,heapStart,gcStart;
    private static long heap() {var r=Runtime.getRuntime();return r.totalMemory()-r.freeMemory();}
    private static long gc() {return java.lang.management.ManagementFactory.getGarbageCollectorMXBeans().stream().mapToLong(b->Math.max(0,b.getCollectionTime())).sum();}
    @SubscribeEvent public static void pre(ServerTickEvent.Pre event) {if(ENABLED && label!=null)tickStart=System.nanoTime();}
    @SubscribeEvent public static void post(ServerTickEvent.Post event) {
        if(!ENABLED)return;
        if(label==null) {
            if(event.getServer().getTickCount()%20!=0)return;
            var players=event.getServer().getPlayerList().getPlayers();if(players.size()!=1)return;
            var p=players.getFirst();
            for(String name:LABELS) if(p.getTags().contains(name) && !done.contains(name)) {
                label=name;warmup=100;index=0;start=System.nanoTime();heapStart=heap();gcStart=gc();
                if(name.endsWith("_32")) {
                    actors=p.serverLevel().getEntitiesOfClass(Settler.class,p.getBoundingBox().inflate(48),s->s.getTags().contains("settlement_profile_actor"));
                    if(actors.size()!=32 || actors.stream().anyMatch(Settler::isNoAi))throw new IllegalStateException("Profile requires32AI-enabled subjects");
                }
                LoggerFactory.getLogger(SettlementAcceptanceProbe.class).info("SETTLEMENT_PROFILE_START {} actors={} warmup=100 samples=600",label,actors.size());break;
            }
            return;
        }
        // Deliberate QA stimulus: alternate workplaces so the cohort walks, rather than disabling AI.
        int phase=index/100;
        if(!actors.isEmpty() && (warmup==100 || warmup<=0 && index%100==0))
            for(var s:actors)s.setWork(s.homePos().offset(phase%2==0?8:-8,0,0));
        if(warmup-->0)return;
        samples[index++]=System.nanoTime()-tickStart;
        if(index==samples.length) {
            var sorted=samples.clone();Arrays.sort(sorted);
            LoggerFactory.getLogger(SettlementAcceptanceProbe.class).info(
                "SETTLEMENT_PROFILE {} n={} meanMs={} p95Ms={} p99Ms={} maxMs={} elapsedSeconds={} heapBefore={} heapAfter={} gcMillis={}",
                label,index,Arrays.stream(sorted).average().orElseThrow()/1e6,sorted[569]/1e6,sorted[593]/1e6,sorted[599]/1e6,
                (System.nanoTime()-start)/1e9,heapStart,heap(),gc()-gcStart);
            done.add(label);label=null;actors=List.of();
        }
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent e) {label=null;actors=List.of();done.clear();}
    private SettlementAcceptanceProbe() {}
}
