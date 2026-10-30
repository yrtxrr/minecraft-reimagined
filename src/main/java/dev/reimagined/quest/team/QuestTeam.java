package dev.reimagined.quest.team;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Team-based quest progress for co-op play.
 * Tracks milestones shared between team members (FTB Quests teams when available).
 * Personal progress (skills, research) remains per-player.
 */
public final class QuestTeam {
    private static final String DATA_KEY = "reimagined_quest_team_v1";

    private QuestTeam() {}

    /** Team ID for a player. Empty string means solo (no team). */
    public static String teamId(Player player) {
        // When FTB Quests is available, this returns the FTB team ID.
        // For now, every player is solo (no team).
        return "";
    }

    /** Check if a milestone is reached by any member of the player's team. */
    public static boolean hasTeamMilestone(Player player, String milestone) {
        String team = teamId(player);
        if (team.isEmpty()) {
            // Solo: check personal progress.
            return dev.reimagined.quest.QuestProgress.has((ServerPlayer) player, milestone);
        }
        // Team: check if any member has the milestone.
        // Implementation requires FTB Quests team API; not available yet.
        return false;
    }

    /** Mark a milestone for the player's team (shared with all members). */
    public static void markTeam(ServerPlayer player, String milestone) {
        dev.reimagined.quest.QuestProgress.mark(player, milestone);
        // Team propagation requires FTB Quests team API; not available yet.
    }

    /** Copy team data on player clone. */
    public static void copy(PlayerEvent.Clone event) {
        if (!(event.getOriginal() instanceof ServerPlayer original)) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (original.getPersistentData().contains(DATA_KEY)) {
            player.getPersistentData().put(DATA_KEY,
                    original.getPersistentData().getCompound(DATA_KEY).copy());
        }
    }
}
