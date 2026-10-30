# Settlement merge plan (civilization × settlements)

Analysis only — no merge was performed and no branch was modified.
Branches: `feat/reimagined-world` (`8e9d6b3`) vs `feat/reimagined-modpack-core` (`21cdd49`), merge-base `c6a0cb9`.

## 1. What each side actually is

| | `dev.reimagined.civilization` (Freebuff) | `dev.reimagined.settlements` (Codex) |
|---|---|---|
| Unit | abstract settlement (≤64), no entity | `Settler` entity, one per NPC |
| State | population, housing, food/materials/knowledge/treasury, type, era, UUID, name, position | settler UUID, home/work `BlockPos`, profession, `tradeTick`, stock; per-NPC `MerchantOffers` |
| Simulation | round-robin pass every 100 ticks, ≤16 settlements × ≤6 hours, no world access | event/local only: join/death/interact, navigation budgeted per NPC, no world scan |
| Persistence | `SettlementData` key **`reimagined_settlements`**, `Version=1`, `Settlements` list | `SettlementData` key **`reimagined_settlements`**, `Version=1`, `Settlers` list; plus `TradeData` key `reimagined_trades` |
| Economy | production/consumption/era thresholds, abstract | real merchant offers, stock, restock per work day |
| Events | `SettlementQuestEvent` on `NeoForge.EVENT_BUS` (server-side) | internal listeners, no public quest event |
| Commands | `/reimagined settlement list|info|found|advance` | none |
| Client | none | `SettlerRenderer`/`SettlerModel` |

**Blocking conflict:** both declare a `SavedData` named `reimagined_settlements` in the overworld with incompatible layouts. `DataStorage.computeIfAbsent` keys by name only, so whichever factory is created first wins and the other's data is silently ignored/dropped on save. `TradeData` has no collision.

## 2. Which one should be primary

**Keep Codex's `settlements` as the local/entity tier and the owner of the `reimagined_settlements` key.** It already has real NPCs, trading, navigation budgets, rendering and live save compatibility. Our `civilization` stays the **remote/abstract tier** (economy, eras, quest signals, commands) but must stop owning that key.

Reasons: deleting Codex's module loses real entities and trading; deleting ours loses autonomous growth while unloaded and quest signals. They are complementary tiers, not duplicates.

## 3. Merge plan (no data loss)

1. **One registry class.** Extend `dev.reimagined.settlements.SettlementData` with our abstract fields, or rename ours to `dev.reimagined.civilization.AggregateData` and key it `reimagined_settlement_aggregates`. Preferred: **single class + single key**, so a settlement has one record containing both tiers.
   - Add to `settlements.SettlementData`: `List<Settlement>`/`AbstractRecord` with id UUID, name, type, `BlockPos`, era, population, housing, food, materials, knowledge, treasury, `lastSimulatedGameTime`, event flags; bump `Version` to `2`.
   - `load(CompoundTag)` must keep reading `Settlers` exactly as today and additionally read `Aggregates` when present; `save` writes both. Do **not** reuse `Settlements` for the new list — old worlds from Freebuff may contain that tag, so read it as a legacy source and migrate once.
   - If instead two classes are kept, `civilization.SettlementData` must change `DATA_NAME` to a new key (for example `reimagined_settlement_economy`) and migrate any existing `Settlements` tag from the old key before the first save. Two classes with the same key is not acceptable.
2. **Simulation ownership.** `CivilizationSimulation` (our 100-tick round-robin) stays but reads/writes the shared registry through one accessor. It must not spawn `Settler` entities; the local tier remains event-driven. `SettlementEvents.onJoin` should look up the abstract record by dimension/position and use its population/profession data when creating a new settler, instead of hardcoding `"commoner"`; on death, `removeSettler` must also decrement the abstract population (bounded, no per-tick persistence).
3. **Quest events.** Keep `SettlementQuestEvent` and `QuestSignalType`; after a merged mutation, post from `CivilizationSimulation` exactly as now (saved-before-post dedupe). Add optional fields later (`settlerId`, biome/structure) only after Codex fixes the payload version.
4. **Commands.** Keep `/reimagined settlement ...` in `CivilizationCommands`. Add a permission-2 `materialize <id>` later; do not let `found` place blocks or entities in this merge.
5. **Registration.** `Reimagined` constructor on the Codex branch already calls `SettlementContent.register(modBus)` and takes `(IEventBus, ModContainer)`. Keep our `CivilizationContent.register()` call (no bus needed) in the same constructor; no second `DeferredRegister`. Ensure `CivilizationSimulation.register()` and `SettlementEvents.register()` are both invoked exactly once.
6. **Entity/profession coupling.** Map our settlement types to Codex professions (`agricultural→farmer`, `trading→librarian/butcher`, `industrial→blacksmith`, `military→guard` etc.) in one adapter class, not in either module's core.

### Exact classes/methods to touch

- `settlements/SettlementData.java`: add aggregate list + codec, `Version=2`, add `aggregate(UUID)`, `allAggregates()`, keep `findSettler/registerSettler/removeSettler`.
- `civilization/SettlementData.java`: remove its own `SavedData` key (or rename), delegate to the shared registry; delete its `Factory`.
- `civilization/Settlement.java`: keep NBT codec; nest it as `Aggregates` entries.
- `civilization/CivilizationSimulation.java`: `pass()`/`advance()` read the shared accessor; `post()` unchanged.
- `settlements/SettlementEvents.java`: `onJoin` consults the aggregate; `onDeath` decrements population.
- `Reimagined.java`: merge the two registration lines; constructor signature from Codex wins.
- `gametest/CivilizationGameTests.java`: replace direct `SettlementData.get(...)` assumptions; add one round-trip test that loads a v1 `Settlers`-only tag and saves it back without losing settlers.

## 4. Test/verification expectations after merge

- Existing `SettlementGameTests`, `SettlementTradingGameTests`, `SettlementNavigationGameTests`, `JointSettlementBeekeepingGameTests` must still pass unchanged.
- Our six civilization GameTests may only change their registry accessor, not their assertions.
- Add: v1→v2 save migration, one registry instance per world, no duplicated population after reload, quest signal once per era, and a distant aggregate advancing while its chunk stays unloaded.

## 5. Mod manifest coexistence

Codex owns the pack assembly (`modpack/mods.lock.json`, `scripts/assemble-modpack.py`, Windows installer, `modpack/server/server.properties`). Our `pack/mods.json` holds the verified 25-mod set including **Apollo's Library 1.2.0** and **Moonlight Lib 1.21.1-3.7.2**, which the first manifest lacked. Keep Codex's installer/pipeline and replace its mod set with our pinned entries; keep `scripts/validate-modpack.py` as the offline hash/dependency gate in their CI. Do not ship two manifests. Our `pack/mods/` is ignored and must remain so.

## 6. Unresolved risks

- Silent data loss is the main risk: same key, same `Version=1`, different schemas. Any integration that simply calls both `computeIfAbsent` factories will lose one side on first save.
- Codex's `SettlementData.load` currently ignores unknown tags; after adding aggregates it must not overwrite a v1 file before reading settlers.
- Two calendars (Reimagined vs Serene Seasons) remain unresolved and are independent of this merge.
- No real world has been launched (EULA), so settlement placement/spawning in generated Terralith terrain is unverified.
