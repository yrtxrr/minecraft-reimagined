package dev.reimagined.gametest;

import dev.reimagined.civilization.CivilizationSimulation;
import dev.reimagined.civilization.QuestSignalType;
import dev.reimagined.civilization.Settlement;
import dev.reimagined.civilization.SettlementData;
import dev.reimagined.civilization.SettlementQuestEvent;
import dev.reimagined.civilization.SettlementType;
import dev.reimagined.civilization.TechEra;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Integration coverage for the abstract settlement prototype: persistence, bounds, events, commands. */
@GameTestHolder("reimagined")
@PrefixGameTestTemplate(false)
public final class CivilizationGameTests {
    private CivilizationGameTests() {}

    private static void remove(ServerLevel level, Settlement settlement) {
        SettlementData.get(level.getServer()).remove(settlement.id());
    }

    @GameTest(template = "empty", timeoutTicks = 60)
    public static void distantSettlementEvolvesAndPersistsWithoutLoadingChunks(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        SettlementData data = SettlementData.get(level.getServer());
        BlockPos position = new BlockPos(30_000, 80, 30_000);
        ChunkPos chunk = new ChunkPos(position);
        Settlement settlement = null;
        try {
            helper.assertTrue(level.getChunkSource().getChunkNow(chunk.x, chunk.z) == null,
                    "Fixture requires an unloaded settlement chunk");
            SettlementData.FoundResult result = data.found(level, SettlementType.AGRICULTURAL, "Alderford", position);
            helper.assertTrue(result.reason() == SettlementData.Reason.OK && result.settlement() != null,
                    "A valid distant position must accept a settlement");
            settlement = result.settlement();
            int foodBefore = settlement.food();
            int knowledgeBefore = settlement.knowledge();

            CivilizationSimulation.advanceHours(level, settlement, 24);

            helper.assertTrue(settlement.knowledge() > knowledgeBefore && settlement.population() > 24,
                    "A settlement must produce knowledge and grow without any player");
            helper.assertTrue(settlement.food() != foodBefore, "Food stores must change through production and consumption");
            helper.assertTrue(level.getChunkSource().getChunkNow(chunk.x, chunk.z) == null,
                    "Simulation must not load or ticket the settlement chunk");

            CompoundTag saved = data.save(new CompoundTag(), level.registryAccess());
            SettlementData reloaded = SettlementData.load(saved, level.registryAccess());
            Settlement copy = reloaded.byId(settlement.id()).orElse(null);
            helper.assertTrue(copy != null && copy.name().equals("Alderford"),
                    "Settlement identity must survive SavedData roundtrip");
            helper.assertTrue(copy.population() == settlement.population() && copy.food() == settlement.food()
                            && copy.knowledge() == settlement.knowledge() && copy.era() == settlement.era()
                            && copy.position().equals(position),
                    "Population, stocks, era and position must survive SavedData roundtrip");
        } finally {
            if (settlement != null) remove(level, settlement);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void foundingEnforcesTerritorySpacing(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        SettlementData data = SettlementData.get(level.getServer());
        List<Settlement> created = new ArrayList<>();
        try {
            Settlement first = data.found(level, SettlementType.MIXED, "First", new BlockPos(40_000, 70, 40_000)).settlement();
            helper.assertTrue(first != null, "First settlement must be founded");
            created.add(first);
            SettlementData.FoundResult close = data.found(level, SettlementType.TRADING, "Close",
                    new BlockPos(40_064, 70, 40_000));
            helper.assertTrue(close.reason() == SettlementData.Reason.TOO_CLOSE,
                    "Settlements inside the claimed radius must be rejected");
            SettlementData.FoundResult far = data.found(level, SettlementType.TRADING, "Far",
                    new BlockPos(40_400, 70, 40_000));
            helper.assertTrue(far.reason() == SettlementData.Reason.OK, "Settlements outside the radius must be accepted");
            created.add(far.settlement());
        } finally {
            for (Settlement settlement : created) remove(level, settlement);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 60)
    public static void questSignalsAreEmittedForRealChangesOnly(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        SettlementData data = SettlementData.get(level.getServer());
        List<SettlementQuestEvent> events = new ArrayList<>();
        Consumer<SettlementQuestEvent> listener = events::add;
        NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.LOWEST, SettlementQuestEvent.class, listener);
        Settlement settlement = null;
        try {
            settlement = data.found(level, SettlementType.AGRICULTURAL, "Signalwatch",
                    new BlockPos(43_000, 70, 43_000)).settlement();
            CivilizationSimulation.announceFounding(level, settlement);
            CivilizationSimulation.advanceHours(level, settlement, 24);
            long founded = count(events, QuestSignalType.SETTLEMENT_FOUNDED);
            long trades = count(events, QuestSignalType.SETTLEMENT_FIRST_TRADE);
            long growth = count(events, QuestSignalType.SETTLEMENT_GREW);
            helper.assertTrue(founded == 1, "Founding must emit exactly one event");
            helper.assertTrue(trades == 1, "The first trade income must emit exactly one event");
            helper.assertTrue(growth >= 1, "Real population growth must emit a growth event");

            CompoundTag saved = data.save(new CompoundTag(), level.registryAccess());
            Settlement reloaded = SettlementData.load(saved, level.registryAccess()).byId(settlement.id()).orElseThrow();
            CivilizationSimulation.advanceHours(level, reloaded, 12);
            helper.assertTrue(count(events, QuestSignalType.SETTLEMENT_FOUNDED) == 1,
                    "Founding must not be replayed after reload");
            helper.assertTrue(count(events, QuestSignalType.SETTLEMENT_FIRST_TRADE) == 1,
                    "First-trade dedupe must persist across reload");
        } finally {
            NeoForge.EVENT_BUS.unregister(listener);
            if (settlement != null) remove(level, settlement);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 60)
    public static void eraAdvancementIsAutonomousAndAnnouncedOncePerEra(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        SettlementData data = SettlementData.get(level.getServer());
        List<SettlementQuestEvent> events = new ArrayList<>();
        Consumer<SettlementQuestEvent> listener = events::add;
        NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.LOWEST, SettlementQuestEvent.class, listener);
        Settlement settlement = null;
        try {
            settlement = data.found(level, SettlementType.THAUMATURGIC, "Emberhold",
                    new BlockPos(46_000, 70, 46_000)).settlement();
            CivilizationSimulation.advanceHours(level, settlement, 240);
            helper.assertTrue(settlement.era() != TechEra.FRONTIER,
                    "Knowledge and materials must advance a settlement past the frontier era");
            long announcements = count(events, QuestSignalType.SETTLEMENT_ERA_ADVANCED);
            helper.assertTrue(announcements == settlement.era().ordinal(),
                    "Each era transition must be announced exactly once: era=" + settlement.era()
                            + " announcements=" + announcements + " knowledge=" + settlement.knowledge()
                            + " materials=" + settlement.materials());

            CompoundTag saved = data.save(new CompoundTag(), level.registryAccess());
            Settlement reloaded = SettlementData.load(saved, level.registryAccess()).byId(settlement.id()).orElseThrow();
            CivilizationSimulation.advanceHours(level, reloaded, 12);
            helper.assertTrue(count(events, QuestSignalType.SETTLEMENT_ERA_ADVANCED) == reloaded.era().ordinal(),
                    "Era announcements must not repeat after reload");
        } finally {
            NeoForge.EVENT_BUS.unregister(listener);
            if (settlement != null) remove(level, settlement);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 60)
    public static void passAdvancesAtMostSixteenSettlements(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        SettlementData data = SettlementData.get(level.getServer());
        List<Settlement> created = new ArrayList<>();
        try {
            for (int i = 0; i < 20; i++) {
                Settlement settlement = data.found(level, SettlementType.MIXED, "Budget" + i,
                        new BlockPos(50_000 + i * 256, 70, 50_000)).settlement();
                helper.assertTrue(settlement != null, "Budget fixture settlement must be founded");
                created.add(settlement);
            }
            long now = level.getGameTime() + CivilizationSimulation.TICKS_PER_HOUR;
            CivilizationSimulation.pass(level.getServer(), now);
            long advanced = created.stream()
                    .filter(settlement -> settlement.lastSimulatedGameTime() == now)
                    .count();
            helper.assertTrue(advanced == CivilizationSimulation.MAX_SETTLEMENTS_PER_PASS,
                    "One pass must advance exactly its declared settlement budget");
        } finally {
            for (Settlement settlement : created) remove(level, settlement);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void operatorCommandsDriveTheSameSimulation(GameTestHelper helper) throws Exception {
        ServerLevel level = helper.getLevel();
        SettlementData data = SettlementData.get(level.getServer());
        var dispatcher = level.getServer().getCommands().getDispatcher();
        var source = level.getServer().createCommandSourceStack();
        Settlement created = null;
        try {
            helper.assertTrue(dispatcher.execute("reimagined settlement found agricultural CommandTown", source) == 1,
                    "Registered operator command must found a settlement");
            created = data.all().stream().filter(settlement -> settlement.name().equals("CommandTown"))
                    .findFirst().orElse(null);
            helper.assertTrue(created != null, "Command must create world-owned settlement data");
            helper.assertTrue(dispatcher.execute("reimagined settlement list", source) == 1,
                    "Registered settlement list must execute");
            helper.assertTrue(dispatcher.execute("reimagined settlement info " + created.id(), source) == 1,
                    "Registered settlement info must resolve a full id");
            helper.assertTrue(dispatcher.execute("reimagined settlement advance " + created.id() + " 2", source) == 1,
                    "Registered advance command must run the simulation");
        } finally {
            if (created != null) remove(level, created);
        }
        helper.succeed();
    }

    private static long count(List<SettlementQuestEvent> events, QuestSignalType kind) {
        return events.stream().filter(event -> event.kind() == kind).count();
    }
}
