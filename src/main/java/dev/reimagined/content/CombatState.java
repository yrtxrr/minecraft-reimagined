package dev.reimagined.content;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/** Shared cooldowns deliberately survive switching weapons, logout and death. */
public final class CombatState {
    private static final String SPECIAL_READY = "reimagined_special_ready_v1";
    private static final String PARRY_READY = "reimagined_parry_ready_v1";
    private CombatState() {}
    public static long now(ServerPlayer player) { return player.serverLevel().getServer().overworld().getGameTime(); }
    public static long specialReadyAt(Player player) { return player.getPersistentData().getLong(SPECIAL_READY); }
    public static boolean specialReady(ServerPlayer player) { return specialReadyAt(player) <= now(player); }
    public static void startSpecial(ServerPlayer player, int ticks) {
        player.getPersistentData().putLong(SPECIAL_READY, now(player) + ticks);
    }
    public static boolean parryReady(ServerPlayer player) { return player.getPersistentData().getLong(PARRY_READY) <= now(player); }
    public static void startParry(ServerPlayer player) { player.getPersistentData().putLong(PARRY_READY, now(player) + 30); }
    public static void copyState(Player original, Player replacement) {
        for (String key : new String[] {SPECIAL_READY, PARRY_READY}) {
            if (original.getPersistentData().contains(key)) replacement.getPersistentData().putLong(key, original.getPersistentData().getLong(key));
        }
    }
}
