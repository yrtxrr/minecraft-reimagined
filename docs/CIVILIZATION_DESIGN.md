# Autonomous civilizations — design and prototype

## Goal

Settlements must live without the player: population, production, consumption, stores,
specialisation, trade, technological eras and, later, professions, politics, alliances and
conflicts. The player may trade, help, explore or interfere, but never has to manage
construction or the economy. The player cannot found a state.

This stage deliberately delivers **one reliable abstract settlement**, not a full civilization
game. Local NPCs, buildings, diplomacy and wars are not implemented and are not presented as
working.

## 1. What exists in source

Module: `dev.reimagined.civilization`

| Class | Responsibility |
|---|---|
| `SettlementType` | seven specialisations with per-mille production profiles |
| `TechEra` | four eras, production multiplier, knowledge/material thresholds |
| `SettlementEconomy` | pure deterministic hourly rules; no Minecraft imports |
| `Settlement` | persistent settlement scalars and NBT codec |
| `SettlementData` | overworld `SavedData` registry, founding rules, id lookup |
| `CivilizationSimulation` | bounded round-robin simulation and quest-signal posting |
| `SettlementQuestEvent` | server-side event carrying a committed change |
| `QuestSignalType` | stable event ids |
| `CivilizationCommands` | operator commands (`/reimagined settlement ...`) |

### Settlement types

| Type | Food | Materials | Knowledge | Trade |
|---|---:|---:|---:|---:|
| Agricultural | 700 | 150 | 80 | 40 |
| Trading | 350 | 200 | 120 | 220 |
| Industrial | 250 | 550 | 140 | 100 |
| Military | 400 | 300 | 100 | 60 |
| Thaumaturgic | 300 | 150 | 400 | 90 |
| Nature-magic | 500 | 200 | 300 | 70 |
| Mixed | 450 | 300 | 180 | 110 |

Values are per-mille of population per simulated hour before era and season multipliers.

### Eras

| Era | Production | Knowledge required | Materials paid on entry |
|---|---:|---:|---:|
| Frontier | 100% | 0 | 0 |
| Settled | 125% | 120 | 80 |
| Crafted | 160% | 480 | 260 |
| Arcane | 200% | 1200 | 650 |

Knowledge is cumulative; the material cost is spent once when the era is entered. Housing cap
is 32 / 80 / 128 / 176 by era.

### Hourly economy (implemented)

```
food       += pop * foodPermille * era% * season% / 10_000_000
materials  += pop * materialPermille * era% / 100_000
knowledge  += pop * knowledgePermille * era% / 100_000
treasury   += pop * tradePermille * era% / 50_000      (trade income)
food       -= pop * 400 / 1000                         (consumption)
```

Seasonal food multiplier: spring 100%, summer 110%, autumn 90%, winter 60%.

Stores are capped at `max(120, pop*40)` food, `max(80, pop*20)` materials and
`max(60, pop*100)` treasury. A settlement starts at population 24, housing 32, food 160,
materials 120, treasury 40, era Frontier.

- **Growth:** population < housing, food ≥ population×8 and materials ≥ 12 → +1 population,
  −8 food, −12 materials.
- **Housing:** below the era cap, materials ≥ 60 and population ≥ housing−4 → +2 housing,
  −60 materials. This is abstract construction; no blocks are placed.
- **Hunger:** if food < consumption, buy 10 food for 20 treasury once; if still short, lose
  1 population (minimum 1) and empty the granary.
- **Era advance:** next era's knowledge and material thresholds are met → pay materials,
  advance, unlock a larger housing cap.

Everything is deterministic. There is no random factor in the prototype.

## 2. Persistence and identity

- `SettlementData` is stored in the overworld's `DataStorage` as `reimagined_settlements`
  (version 1), up to 64 settlements.
- Each settlement has a stable UUID, name (≤32 chars), type, position, era, stocks and event
  dedupe flags. Save/load roundtrip is covered by a GameTest.
- Founding requires ≥192 blocks of horizontal spacing; this is the first, data-level territory
  claim and prevents two settlements from occupying the same site. It also protects the future
  building system from overlapping claims.
- Founding is operator-only (`/reimagined settlement found <type> [name]`, permission 2) in the
  prototype. Natural/worldgen founding is future work, so a normal player world starts empty.

Commands:

```
/reimagined settlement list
/reimagined settlement info <id|prefix>
/reimagined settlement found <type> [name]     (operator)
/reimagined settlement advance <id> <hours>    (operator, 1..240)
```

`advance` is an explicit test/demo tool; it uses the same economy and event dedupe as the
automatic simulation.

## 3. Two-level simulation

### Remote level (implemented)

`CivilizationSimulation` runs every 100 ticks (5 s), selects at most 16 settlements in
round-robin order, and replays at most 6 whole simulated hours per settlement per pass.
One hour is 1000 ticks. A settlement that was offline longer than that intentionally loses the
excess catch-up instead of running an unbounded backlog. There is no chunk, block, entity or
player query anywhere in this path.

Consequences:

- 64 settlements worst case: 16 × 6 = 96 O(1) arithmetic steps per 5 s, plus a small NBT save;
- a settlement is visited at least every 4 passes (20 s) when 64 exist;
- no NPC, building or route exists remotely, so no duplicated entities are possible;
- after restart, saved populations/stocks/flags resume and event announcements do not replay.

### Local level (not implemented)

The intended local tier is:

- real NPCs/buildings only inside loaded, player-near chunks;
- local jobs, pathfinding and trade handled by bounded vanilla/MCA goals;
- the abstract state remains authoritative for stocks/population; local entities are a view
  driven by it, not a second competing simulation;
- one owner per NPC/building, with a persisted ID and a "materialised" flag to prevent
  duplication after restart or chunk reload;
- MCA Reborn/Guard Villagers integration instead of a parallel village population.

None of this exists yet. No entities or blocks are created by this module.

## 4. City building architecture (design only)

1. **Site selection** happens at settlement founding/expansion time, from a queue, never in a
   tick scan. Terrain sampling is limited to already-loaded chunks or the supported
   chunk-generation pipeline.
2. **Plots and roads** are data records (site id, purpose, footprint, era/style) with a cap per
   settlement. A plot is only materialised when its chunk is loaded and a player is near; the
   same plot ID is never built twice.
3. **Player protection:** a claim record and a configurable destructive-work switch are
   mandatory before any world edit. Default policy: never break or replace player-placed
   blocks, never build inside another claim, and allow a per-world "no destructive changes"
   mode. The 192-block settlement spacing is the first layer of that protection.
4. **Expansion** is triggered by population/housing/era state, queued, budgeted and applied in
   small batches. It must not run for unloaded chunks; pending work is saved and resumed.
5. **Era/style** changes affect new construction only; existing player and settlement blocks
   are not retroactively replaced.

## 5. Economy, professions, trade and politics (roadmap)

- **Professions** become a local assignment layer derived from settlement type and buildings;
  the prototype stores no profession counts and must not pretend to.
- **Inter-settlement trade** needs two settlements and a route record; goods should move as
  abstract ledger entries with bounded caravan entities only near players. Nothing exists yet.
- **Diplomacy** needs relationship values, alliance/conflict state, cooldowns and a persisted
  event log. Wars must never destroy player builds; the safe default is no NPC terrain
  destruction at all until an explicit opt-in rule exists.
- **Magic/technology** research will reuse the existing research journal and magic stations
  rather than a second knowledge system; era thresholds are the current placeholder.

## 6. Quest integration contract

Implemented signal: `dev.reimagined.civilization.SettlementQuestEvent`, posted on
`NeoForge.EVENT_BUS` on the server thread after state mutation.

| Kind id | Fired when |
|---|---|
| `settlement_founded` | a settlement is persisted and announced |
| `settlement_grew` | population crosses a new multiple of 8 |
| `settlement_expanded` | housing crosses a new multiple of 8 |
| `settlement_era_advanced` | the settlement enters a new era (once per era) |
| `settlement_first_trade` | the settlement first earns trade income |

Fields: `kind`, `settlementId` (UUID), `settlementName`, `position` (immutable `BlockPos`),
`gameDay` (Reimagined calendar day), `population`, `era`.

Dedupe: flags/milestones are saved before the signal is posted, so a reload does not repeat
one-time signals. Signals are **not** replayed for historical changes.

For Codex/Kilo: subscribe read-only; do not mutate settlement data from a listener. The event
is server-side only for now (no client packet), appropriate for single-player quests. Proposed
future signals that belong to the quest/world layer rather than this module:
`biome_discovered`, `ancient_structure_visited`, `magic_anomaly_found`, `nether_studied`,
`end_explored`. They should use a namespaced kind string and the same immutable payload shape
so quests have one contract. No such events are implemented yet.

API requests to Codex:
1. Consume `SettlementQuestEvent` by kind id, not enum ordinal.
2. Tell us which extra fields quests need (e.g., biome id, structure id, player UUID) before we
   freeze a versioned payload.
3. Do not add a second settlement registry or own population state.
4. Reuse the Reimagined calendar day for event timestamps.

## 7. Limits and next steps

Working: founding/spacing, abstract economy, population/housing change, era advancement,
persistence, bounded remote simulation, quest signals, operator commands, unit + GameTest
coverage.

Not working / not implemented: NPCs, buildings, roads, local economy, professions, trade
routes, caravans, diplomacy, alliances, wars, natural settlement founding, multiplayer
networking, graphical UI, environmental effects.

Next, in order: (1) natural founding/placement from worldgen with terrain checks;
(2) a visual/debug settlement marker; (3) local entity representation with dedupe IDs;
(4) inter-settlement trade; (5) politics with player-safe defaults.
