# World and civilization performance

## Contract

The build targets a personal single-player computer. That does not relax the project rules:
no world-wide tick scans, no chunk loading for simulation, no unbounded catch-up, no per-tick
saves and no duplicated entities/buildings.

## 1. Civilization prototype cost

| Property | Value |
|---|---|
| Simulation cadence | every 100 ticks (5 s) |
| Settlements examined per pass | ≤ 16, round-robin |
| Simulated hours per settlement per pass | ≤ 6 |
| Work per hour | O(1) integer arithmetic on settlement scalars |
| Worst case arithmetic per pass | 16 × 6 = 96 hour-steps, independent of world size |
| Settlement cap | 64 |
| World access | none — no chunk, block, entity or player queries |
| Chunk loading/tickets | none |
| Randomness | none in the prototype |
| Persistence | one `SavedData` dirty mark per advanced settlement; normal world save cadence |
| Per-settlement save size | fixed scalar fields + UUID/name/position, no inventories or entities |

The scheduler keeps a cursor and processes the next batch on the next pass, so a large
settlement count does not lengthen one tick. A settlement is visited at least every four passes
(20 s) at the 64-settlement cap. Downtime catch-up is capped at six hours per pass; the rest is
intentionally discarded rather than replayed as a backlog.

### What automatic checks prove

- `SettlementEconomyTest` (9 JUnit tests) verifies deterministic rules, season/era scaling,
  growth/housing gates, starvation and capacities.
- `CivilizationGameTests` (6 headless GameTests) verifies:
  - a distant settlement advances and persists while its chunk stays unloaded
    (`ServerChunkSource.getChunkNow` remains null);
  - one pass advances exactly 16 of 20 settlements;
  - founding spacing, command wiring, NBT roundtrip, event emission and post-reload dedupe;
  - era advancement is autonomous.
- Full current run: **99/99 GameTests**, 0 failures/skips; **32/32 JUnit tests**.

These checks prove bounds and correctness, not gameplay TPS. The GameTest server runs an
accelerated batch harness and shared machines are noisy; its 6.8 s total is not a benchmark.

## 2. What has not been profiled

No real-server TPS/MSPT/JFR profile was run for this stage:

- a normal world server requires accepting the Minecraft EULA, which this environment must not
  do on the user's behalf;
- the previously used cloud checkout is not this Windows checkout, and no graphical client is
  available here;
- the pack's terrain cost belongs to Terralith/Tectonic/Incendium/Nullscape and has not been
  measured at all.

Therefore **no performance-readiness claim is made** for the modpack or for large settlement
counts. Do not infer smooth gameplay from the passing build or GameTests.

Required profile before claiming readiness (see `ARCHITECTURE.md` for the general procedure):

1. Record the commit/JAR, Minecraft 1.21.1, NeoForge 21.1.264, JDK 21, JVM flags, hardware,
   seed, view/simulation distance and the exact pack files.
2. Idle baseline and a 30-minute exploration workload (flight/teleport across new terrain) with
   `/jfr start`/`/jfr stop`; report TPS/MSPT, chunk-generation hotspots, allocation and GC.
3. A settlement workload with 1, 16 and 64 settlements, warmup, then at least 600 measured
   ticks; confirm the pass budget and no chunk loading in logs. Do not force-load chunks to
   create the scenario.
4. Optionally a save/restart cycle to verify restored stocks, era and event dedupe.
5. Include tails only if the instrumentation supplies per-tick samples.

Store the summary in this file and keep raw recordings/worlds out of Git.

## 3. Terrain-generation cost

- Terralith/Tectonic are data-driven worldgen mods. Their cost is Minecraft's normal chunk
  generation, not a per-tick mod task.
- Reimagined contributes data-driven biome modifiers: three deer spawn entries, one mossy
  boulder feature and one rare mountain iron vein. These add negligible per-chunk work but do
  add spawn entries.
- Deer/Naturalist spawn density can affect entity-tick cost; it has not been measured.
- `reimagined:continents` is kept but not the pack default; its own noise functions run in the
  supported chunk pipeline.

## 4. Memory and lifecycle

- `SettlementData` holds ≤64 settlements with no cached world, chunk, entity or player
  references. Removing a settlement releases its data.
- No client state or network payload exists for settlements yet.
- The only shared static is the scheduler cursor (an int); it is safe across worlds and
  restarts because the per-settlement `lastGameTime` drives eligibility.
- Future local NPCs/buildings must use persisted IDs and materialisation flags; otherwise a
  chunk reload or server restart could duplicate them. That failure mode is explicitly
  designed against, not solved yet because the local tier does not exist.

## 5. Chunk-generation safety rules for future work

- Site selection/terrain sampling must use already-loaded chunks or the supported generation
  pipeline; never request a FULL chunk from a tick handler just to inspect it.
- Building work is queued, budgeted and applied only while the target chunk is ready.
- Player claims and a destructive-work switch precede any world edit.
- No remote settlement work may load a chunk to "look at" its terrain.
