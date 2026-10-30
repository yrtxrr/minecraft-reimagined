package dev.reimagined.quest;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Integration hook for Freebuff's SettlementQuestEvent.
 *
 * <p>When Freebuff is available as a dependency, this class listens to
 * {@code freebuff.settlements.SettlementQuestEvent} and maps settlement
 * quest events to stable string milestone IDs that FTB Quests tasks can check.
 *
 * <p>Until Freebuff is added, this class registers no listeners and is a
 * safe no-op. All milestone IDs are stable strings that never change.
 */
public final class SettlementQuestIntegration {
    /** Settlement quest milestone IDs. Stable strings; never rename without migration. */
    public static final String SETTLEMENT_FIRST_CONTACT = "settlement:first_contact";
    public static final String SETTLEMENT_FIRST_TRADE = "settlement:first_trade";
    public static final String SETTLEMENT_FIRST_VILLAGE_ALLY = "settlement:first_village_ally";
    public static final String SETTLEMENT_FIRST_REPUTATION = "settlement:first_reputation";

    private SettlementQuestIntegration() {}

    /**
     * Register listeners for Freebuff's SettlementQuestEvent.
     * Currently a no-op until Freebuff is available as a dependency.
     */
    public static void register() {
        // When Freebuff is available:
        // NeoForge.EVENT_BUS.addListener(SettlementQuestIntegration::onSettlementQuest);
    }

    /** Mark a settlement milestone for a player. Idempotent. */
    public static void mark(ServerPlayer player, String milestone) {
        QuestProgress.mark(player, milestone);
    }

    /** Check if a player has reached a settlement milestone. */
    public static boolean has(ServerPlayer player, String milestone) {
        return QuestProgress.has(player, milestone);
    }
}
