package dev.reimagined.core;

import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;

/** Event-driven integration boundary. Seven eras are independent milestones, never recipe locks. */
public final class CoreApi {
    public static final String PLAYER_KEY="reimagined_core_progress_v1";
    private CoreApi() { }
    public static CoreProgress progress(ServerPlayer player) {
        requireServerThread(player.serverLevel()); return CoreProgress.load(player.getPersistentData().getCompound(PLAYER_KEY));
    }
    public static boolean research(ServerPlayer player, ResourceLocation knowledge) {
        var state=progress(player); if (!state.learn(knowledge)) return false;
        player.getPersistentData().put(PLAYER_KEY,state.save());
        signal(player.serverLevel(),CoreSignal.Kind.RESEARCH,knowledge,player.getUUID()); return true;
    }
    public static boolean unlock(ServerPlayer player, CoreProgress.Era era) {
        var state=progress(player); if (!state.unlock(era)) return false;
        player.getPersistentData().put(PLAYER_KEY,state.save());
        signal(player.serverLevel(),CoreSignal.Kind.ERA,ResourceLocation.fromNamespaceAndPath("reimagined",era.name().toLowerCase(java.util.Locale.ROOT)),player.getUUID()); return true;
    }
    /** Emit one actual transition, not a tick heartbeat. No history queue or retained player/world reference. */
    public static void signal(ServerLevel level, CoreSignal.Kind kind, ResourceLocation subject, UUID actor) {
        requireServerThread(level); NeoForge.EVENT_BUS.post(new CoreSignal(kind,subject,actor));
    }
    public static void requireServerThread(ServerLevel level) {
        if (!level.getServer().isSameThread()) throw new IllegalStateException("Core API is server-thread only");
    }
}
