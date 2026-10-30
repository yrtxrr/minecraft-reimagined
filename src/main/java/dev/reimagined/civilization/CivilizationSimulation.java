package dev.reimagined.civilization;

import dev.reimagined.season.CalendarData;
import dev.reimagined.season.SeasonCalendar;
import java.util.List;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Bounded remote simulation. Economies advance in whole simulated hours; a pass examines at most
 * {@link #MAX_SETTLEMENTS_PER_PASS} settlements and replays at most {@link #MAX_HOURS_PER_PASS}
 * hours per settlement. No chunk, entity or block access occurs here.
 */
public final class CivilizationSimulation {
    /** One simulated economy hour. A Minecraft day is 24 passes' worth of hours. */
    public static final int TICKS_PER_HOUR = 1_000;
    public static final int PASS_INTERVAL_TICKS = 100;
    public static final int MAX_SETTLEMENTS_PER_PASS = 16;
    public static final int MAX_HOURS_PER_PASS = 6;
    public static final int MAX_COMMAND_HOURS = 240;
    private static int cursor;

    private CivilizationSimulation() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(CivilizationSimulation::onServerTick);
    }

    private static void onServerTick(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % PASS_INTERVAL_TICKS != 0) return;
        pass(event.getServer(), event.getServer().overworld().getGameTime());
    }

    /** One round-robin pass. Exposed with an explicit clock so scale and catch-up bounds are testable. */
    public static void pass(MinecraftServer server, long gameTime) {
        SettlementData data = SettlementData.get(server);
        List<Settlement> settlements = List.copyOf(data.all());
        if (settlements.isEmpty()) {
            cursor = 0;
            return;
        }
        int start = Math.floorMod(cursor, settlements.size());
        int processed = 0;
        for (int i = 0; i < settlements.size() && processed < MAX_SETTLEMENTS_PER_PASS; i++) {
            Settlement settlement = settlements.get((start + i) % settlements.size());
            long elapsed = gameTime - settlement.lastSimulatedGameTime();
            if (elapsed < TICKS_PER_HOUR) continue;
            int hours = (int) Math.min(MAX_HOURS_PER_PASS, elapsed / TICKS_PER_HOUR);
            advance(server.overworld(), data, settlement, hours, gameTime);
            processed++;
        }
        cursor = start + processed;
    }

    /** Explicit advancement for operator commands and tests. Still runs through the same rules and event dedupe. */
    public static int advanceHours(ServerLevel level, Settlement settlement, int hours) {
        int bounded = Math.clamp(hours, 1, MAX_COMMAND_HOURS);
        SettlementData data = SettlementData.get(level.getServer());
        return advance(level, data, settlement, bounded, level.getGameTime());
    }

    /** Announces a newly persisted settlement exactly once. Call after the settlement data is stored. */
    public static void announceFounding(ServerLevel level, Settlement settlement) {
        if (!settlement.markFoundedAnnounced()) return;
        SettlementData.get(level.getServer()).setDirty();
        post(level, settlement, 1 << QuestSignalType.SETTLEMENT_FOUNDED.ordinal());
    }

    private static int advance(ServerLevel level, SettlementData data, Settlement settlement, int hours, long gameTime) {
        if (hours <= 0) return 0;
        SeasonCalendar.Season season = SeasonCalendar.season(CalendarData.get(level).day(level));
        int triggered = 0;
        for (int hour = 0; hour < hours; hour++) {
            int hourEvents = settlement.advanceHour(season);
            triggered |= hourEvents;
            // Post per hour: two transitions in one bounded catch-up call must remain two signals.
            if (hourEvents != 0) post(level, settlement, hourEvents);
        }
        settlement.markSimulated(gameTime);
        data.setDirty();
        return triggered;
    }

    private static void post(ServerLevel level, Settlement settlement, int mask) {
        long day = CalendarData.get(level).day(level);
        for (QuestSignalType kind : QuestSignalType.values()) {
            if ((mask & (1 << kind.ordinal())) == 0) continue;
            NeoForge.EVENT_BUS.post(new SettlementQuestEvent(kind, settlement.id(), settlement.name(),
                    settlement.position(), day, settlement.population(), settlement.era()));
        }
    }
}
