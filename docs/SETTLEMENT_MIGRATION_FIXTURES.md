# Settlement v1 → v2 migration: fixtures and acceptance checks

Companion to [SETTLEMENT_MERGE_PLAN.md](SETTLEMENT_MERGE_PLAN.md). No implementation is changed here and no save format in `feat/reimagined-world` is modified. These are the exact inputs Codex must load and the outputs a merged implementation must produce.

## The two colliding v1 schemas

Both live in the overworld `DataStorage` under the name **`reimagined_settlements`**.

### A. Codex `settlements.SettlementData` (entity tier)

```snbt
{
  Version: "1",
  NextSettlerId: 3,
  Settlers: [
    {
      Uuid: [I; 11259375, 2002870789, -1430532899, 11259375],
      HomeX: 120, HomeY: 64, HomeZ: -40,
      WorkX: 128, WorkY: 64, WorkZ: -40,
      Profession: "farmer",
      TradeTick: 1400,
      Stock: 6
    },
    {
      Uuid: [I; 843991, -2002870789, 1430532899, -843991],
      HomeX: 118, HomeY: 64, HomeZ: -36,
      WorkX: 130, WorkY: 64, WorkZ: -44,
      Profession: "librarian",
      TradeTick: 0,
      Stock: 0
    }
  ]
}
```

### B. Freebuff `civilization.SettlementData` (abstract tier)

```snbt
{
  Version: 1,
  Settlements: [
    {
      Version: 1,
      Id: [I; 305419896, 2596069104, -19088744, 2271560481],
      Name: "Millford",
      Type: "agricultural",
      Era: "SETTLED",
      X: 112, Y: 63, Z: -48,
      Population: 34, Housing: 40,
      Food: 512, Materials: 208, Knowledge: 240, Treasury: 96,
      FoundedDay: 12, LastGameTime: 240000,
      FoundedAnnounced: 1b, FirstTradeAnnounced: 1b,
      EraAnnouncedMask: 3, GrowthMilestone: 4, HousingMilestone: 1
    }
  ]
}
```

Note the type mismatch: the outer tag uses a string `"1"` in A and an int `1` in B. Codex's `SettlementData.load` reads no outer `Version` at all; our loader reads `Settlements` only. A merged loader must tolerate both.

## Required v2 shape

One registry, one key, both tiers nested and independently versioned:

```snbt
{
  Version: 2,
  NextSettlerId: 3,
  Settlers: [ ...unchanged A entries... ],
  Aggregates: [ ...B entries, unchanged field names and per-entry Version... ]
}
```

Rules:

1. `Settlers` entries keep their exact field names — Codex's trading and navigation read them.
2. `Aggregates` entries keep our field names and per-entry `Version`, so `Settlement.load` works unchanged.
3. `Version` is written as int `2`; readers accept `"1"`, `1`, `2`, and missing.
4. Migration is read-only until the next normal save; it must not force a disk write on load.

## Migration acceptance scenarios

| # | Input | Expected |
|---|---|---|
| 1 | A only | All settlers preserved byte-for-byte; `Aggregates` empty; no errors; save produces v2 with the same settlers and `NextSettlerId` |
| 2 | B only | One aggregate with UUID, name, type, era, position, population/housing, all four stocks, `FoundedDay`, `LastGameTime` and every announcement flag/milestone preserved; `Settlers` empty |
| 3 | A and B in the same tag | Both lists preserved; no UUID dedupe across tiers unless an explicit link exists |
| 4 | Legacy `Settlements` inside an A-shaped tag | Treated as B, not discarded |
| 5 | Load v2 twice | Identical object graph; no new entries, no bumped `NextSettlerId`, no second `setDirty` |
| 6 | Save → load → save | Byte-identical NBT (order-insensitive comparison); no repeated migration |
| 7 | Aggregate with unknown `Type`/`Era` | `Settlement.load` returns `null` / falls back to `FRONTIER` exactly as today; the other aggregates still load |
| 8 | Settler with unknown profession | Preserved as stored; trading falls back to the existing catalogue path; never deleted |
| 9 | Distant aggregate after migration | `CivilizationSimulation.pass` advances it with `getChunkNow == null` for its chunk |
| 10 | Duplicate UUIDs inside one list | First wins, second is reported and dropped; no population double-count |

## Idempotency and duplication

- Migration runs from the loader, not from a tick handler; a second load of an already-v2 tag is a no-op.
- `removeSettler(uuid)` must not delete the aggregate; `removeAggregate(uuid)` may delete its settlers only through an explicit, tested operation.
- Population must never be derived by counting `Settlers` (most settlements are unloaded). The aggregate is authoritative for population/economy; settlers are a local view.
- If both tiers ever need a link, add `SettlementId` to the settler record, defaulting to null for v1 entries.

## Already-overwritten saves

A world that ran both branches may contain only one of A or B, because `computeIfAbsent` returned whichever factory was created first and the next save wrote only that schema. **Do not promise recovery.** The migration can only preserve what is still present in the tag. Detect and report:

- tag has `Settlers` but no `Settlements`/`Aggregates` → likely B was overwritten;
- tag has `Settlements` but no `Settlers` → likely A was overwritten;
- neither → the registry was saved empty.

Log one warning naming the missing tier during migration and record it in `/reimagined settlement list` output or a startup log. Do not synthesize population, trades or UUIDs to "restore" missing data. Old worlds created after the conflict are the only place data loss is unrecoverable; this is a documented limitation, not a bug to hide.

## Suggested tests for Codex (do not modify Freebuff files)

Put these in a new test class such as `SettlementMigrationGameTests`, constructing `CompoundTag`s from the SNBT above (or literal code):

1. `codexV1SettlersSurviveMigration` — scenario 1.
2. `freebuffV1AggregatesSurviveMigration` — scenario 2, assert every field.
3. `mergedTagKeepsBothTiers` — scenario 3.
4. `migrationIsIdempotentAcrossReload` — scenarios 5 and 6 with a fresh `SettlementData.load` of the saved tag.
5. `missingTierIsReportedNotInvented` — the overwrite detection above.
6. `distantAggregateStillAdvancesWithoutChunk` — scenario 9, reusing our existing assertion style in `CivilizationGameTests`.

Until the merged loader exists, these fixtures are the contract; no automatic test is added to `feat/reimagined-world` because it would fail against either single-tier implementation.
