package dev.reimagined.civilization;

/** Stable quest-event identifiers. The string id is the contract for quest integrations. */
public enum QuestSignalType {
    SETTLEMENT_FOUNDED("settlement_founded"),
    SETTLEMENT_GREW("settlement_grew"),
    SETTLEMENT_EXPANDED("settlement_expanded"),
    SETTLEMENT_ERA_ADVANCED("settlement_era_advanced"),
    SETTLEMENT_FIRST_TRADE("settlement_first_trade");

    public final String id;

    QuestSignalType(String id) {
        this.id = id;
    }
}
