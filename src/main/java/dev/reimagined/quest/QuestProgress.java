package dev.reimagined.quest;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Central quest progress registry. Tracks per-player and per-team quest task completion
 * using bounded, idempotent boolean milestones stored in player persistent NBT.
 *
 * <p>All checks are event-driven: no world scanning, no per-tick iteration.
 * Completion is recorded on the server and copied on player clone/death.
 *
 * <p>This class does not depend on FTB Quests classes; it is the bridge layer that
 * FTB Quests task/reward implementations (added when FTB Quests is available) consult.
 */
public final class QuestProgress {
    private static final String DATA_KEY = "reimagined_quest_progress_v1";

    /** Milestone IDs used by Chapter 1 quests. Stable strings; never rename without migration. */
    public static final String MILESTONE_FIRST_LOGIN = "ch1:first_login";
    public static final String MILESTONE_FIRST_CRAFT_TABLE = "ch1:first_craft_table";
    public static final String MILESTONE_FIRST_TOOL = "ch1:first_tool";
    public static final String MILESTONE_FIRST_SHELTER = "ch1:first_shelter";
    public static final String MILESTONE_FIRST_SLEEP = "ch1:first_sleep";
    public static final String MILESTONE_FIRST_MEAL_EATEN = "ch1:first_meal_eaten";
    public static final String MILESTONE_FIRST_SEASON_CHECK = "ch1:first_season_check";
    public static final String MILESTONE_FIRST_ORE_MINED = "ch1:first_ore_mined";
    public static final String MILESTONE_FIRST_BREED_ANIMAL = "ch1:first_breed_animal";
    public static final String MILESTONE_FIRST_VILLAGE_FOUND = "ch1:first_village_found";
    public static final String MILESTONE_FIRST_TRADE_COMPLETED = "ch1:first_trade_completed";

    private QuestProgress() {}

    private static void ensure(ServerPlayer player) {
        var data = player.getPersistentData();
        if (!data.contains(DATA_KEY)) data.put(DATA_KEY, new net.minecraft.nbt.CompoundTag());
    }

    /** Mark a milestone for a player. Idempotent: re-marking the same key is a no-op. */
    public static void mark(ServerPlayer player, String milestone) {
        ensure(player);
        var tags = player.getPersistentData().getCompound(DATA_KEY);
        if (tags.getBoolean(milestone)) return;
        tags.putBoolean(milestone, true);
        player.getPersistentData().put(DATA_KEY, tags);
    }

    public static boolean has(ServerPlayer player, String milestone) {
        ensure(player);
        return player.getPersistentData().getCompound(DATA_KEY).getBoolean(milestone);
    }

    /** Copy quest progress from original to cloned player (death/respawn). */
    public static void copy(PlayerEvent.Clone event) {
        if (!(event.getOriginal() instanceof ServerPlayer original)) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!original.getPersistentData().contains(DATA_KEY)) return;
        player.getPersistentData().put(DATA_KEY,
                original.getPersistentData().getCompound(DATA_KEY).copy());
    }

    /** Query helper for KubeJS / FTB Quests conditions. */
    public static boolean hasMilestone(Player player, String milestone) {
        if (!(player instanceof ServerPlayer sp)) return false;
        return has(sp, milestone);
    }
}
