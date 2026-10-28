package dev.reimagined.engineering.profile;

import java.lang.reflect.Field;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.projectile.FishingHook;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemFishedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.LoggerFactory;

/** Opt-in read-only native QA aid for this audio-less cloud. Excluded from the player JAR. */
@EventBusSubscriber(modid="reimagined")
public final class FishingAcceptanceProbe {
    private static final boolean ENABLED=Boolean.getBoolean("reimagined.fishingAcceptance");
    private static Field nibble;
    private static boolean failed,biting;
    private static int hookId=-1;
    @SubscribeEvent public static void tick(ServerTickEvent.Post event) {
        if(!ENABLED || failed || event.getServer().getPlayerList().getPlayers().size()!=1)return;
        var hook=event.getServer().getPlayerList().getPlayers().getFirst().fishing;
        if(hook==null) {hookId=-1;biting=false;return;}
        try {
            if(nibble==null) {nibble=FishingHook.class.getDeclaredField("nibble");nibble.setAccessible(true);}
            boolean current=nibble.getInt(hook)>0;
            if(current && (!biting || hookId!=hook.getId()))LoggerFactory.getLogger(FishingAcceptanceProbe.class).info("NATIVE_FISHING_BITE hook={}",hook.getId());
            biting=current;hookId=hook.getId();
        } catch(ReflectiveOperationException ex) {
            failed=true;LoggerFactory.getLogger(FishingAcceptanceProbe.class).warn("Read-only fishing observer unavailable",ex);
        }
    }
    @SubscribeEvent public static void caught(ItemFishedEvent event) {
        if(!ENABLED)return;
        var drops=event.getDrops().stream().map(s->BuiltInRegistries.ITEM.getKey(s.getItem())+"x"+s.getCount()).toList();
        LoggerFactory.getLogger(FishingAcceptanceProbe.class).info("NATIVE_FISHING_CATCH {}",drops);
    }
}
