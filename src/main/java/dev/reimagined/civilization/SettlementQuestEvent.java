package dev.reimagined.civilization;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.neoforged.bus.api.Event;

/**
 * Server-side quest signal for one actual settlement change. Posted on the NeoForge event bus
 * after the persisted settlement state has been updated, so listeners observe committed state.
 * Emitted once per real transition; persistence flags prevent duplicates after a restart.
 */
public final class SettlementQuestEvent extends Event {
    private final QuestSignalType kind;
    private final UUID settlementId;
    private final String settlementName;
    private final BlockPos position;
    private final long gameDay;
    private final int population;
    private final TechEra era;

    public SettlementQuestEvent(QuestSignalType kind, UUID settlementId, String settlementName, BlockPos position,
                                long gameDay, int population, TechEra era) {
        this.kind = kind;
        this.settlementId = settlementId;
        this.settlementName = settlementName;
        this.position = position.immutable();
        this.gameDay = gameDay;
        this.population = population;
        this.era = era;
    }

    public QuestSignalType kind() { return kind; }
    public UUID settlementId() { return settlementId; }
    public String settlementName() { return settlementName; }
    public BlockPos position() { return position; }
    public long gameDay() { return gameDay; }
    public int population() { return population; }
    public TechEra era() { return era; }
}
