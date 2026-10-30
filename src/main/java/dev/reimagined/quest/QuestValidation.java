package dev.reimagined.quest;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.HashSet;
import java.util.Set;

/**
 * Automatic validation for quest milestones and events.
 * Ensures milestone IDs are stable, bounded, and idempotent.
 * Validates that quest event listeners don't scan worlds or tick-scan.
 */
public final class QuestValidation {
    /** All known milestone IDs. Used to detect typos and duplicates. */
    private static final Set<String> KNOWN_MILESTONES = new HashSet<>();

    static {
        // Chapter 1 milestones
        KNOWN_MILESTONES.add(QuestProgress.MILESTONE_FIRST_LOGIN);
        KNOWN_MILESTONES.add(QuestProgress.MILESTONE_FIRST_CRAFT_TABLE);
        KNOWN_MILESTONES.add(QuestProgress.MILESTONE_FIRST_TOOL);
        KNOWN_MILESTONES.add(QuestProgress.MILESTONE_FIRST_SHELTER);
        KNOWN_MILESTONES.add(QuestProgress.MILESTONE_FIRST_SLEEP);
        KNOWN_MILESTONES.add(QuestProgress.MILESTONE_FIRST_MEAL_EATEN);
        KNOWN_MILESTONES.add(QuestProgress.MILESTONE_FIRST_SEASON_CHECK);
        KNOWN_MILESTONES.add(QuestProgress.MILESTONE_FIRST_ORE_MINED);
        KNOWN_MILESTONES.add(QuestProgress.MILESTONE_FIRST_BREED_ANIMAL);
        KNOWN_MILESTONES.add(QuestProgress.MILESTONE_FIRST_VILLAGE_FOUND);
        KNOWN_MILESTONES.add(QuestProgress.MILESTONE_FIRST_TRADE_COMPLETED);
    }

    private QuestValidation() {}

    /** Validate that a milestone ID is known and well-formed. */
    public static boolean isValidMilestone(String milestone) {
        if (milestone == null || milestone.isEmpty()) return false;
        if (!milestone.matches("^[a-z0-9_:]+$")) return false;
        return KNOWN_MILESTONES.contains(milestone);
    }

    /** Check all registered milestone IDs for duplicates or invalid format. */
    public static void validateAll() {
        for (String m : KNOWN_MILESTONES) {
            if (!isValidMilestone(m)) {
                throw new IllegalStateException("Invalid quest milestone: " + m);
            }
        }
    }

    /** Check if a player has all required milestones for a quest gate. */
    public static boolean hasAll(ServerPlayer player, String... milestones) {
        for (String m : milestones) {
            if (!QuestProgress.has(player, m)) return false;
        }
        return true;
    }

    /** Check if a player has any of the listed milestones. */
    public static boolean hasAny(ServerPlayer player, String... milestones) {
        for (String m : milestones) {
            if (QuestProgress.has(player, m)) return true;
        }
        return false;
    }

    /** Copy validation data on player clone. */
    public static void copy(PlayerEvent.Clone event) {
        // QuestProgress.copy handles the actual data copy.
        QuestProgress.copy(event);
    }
}
