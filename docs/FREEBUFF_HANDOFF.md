# Freebuff → Codex handoff

Branch `feat/reimagined-world`, HEAD **`59326be`** (pushed). Base `c6a0cb9`.
Target: personal single-player / two-player NeoForge 1.21.1 pack, not a public server platform.

## What to port

1. **Pinned pack** — `pack/mods.json` (25 entries: version ID, filename, official CDN URL, SHA-512), `scripts/fetch-modpack.py`, `scripts/validate-modpack.py`. `pack/mods/` stays ignored.
2. **Two dependencies that are mandatory** — Apollo's Library 1.2.0 (needed by Lithostitched ≥1.2.0 and Tectonic ≥1.1.6) and Moonlight Lib 1.21.1-3.7.2 (needed by Immersive Weathering: Renewed). Without them the pack fails to start. This was a real gap in the first manifest.
3. **Terralith compatibility** — deer spawns split into `deer_spawns.json` (`#minecraft:is_forest`), `deer_spawns_taiga.json`, `deer_spawns_meadow.json`; the mountain vein already uses `#minecraft:is_mountain`. NeoForge's `add_spawns` codec does not accept a list of biome tags.
4. **Serene Seasons deferral** — `Seasons.grow` returns early when `sereneseasons` is loaded.
5. **Abstract settlements** — `dev.reimagined.civilization` + `CivilizationContent.register()` call, `SettlementEconomyTest`, `CivilizationGameTests`, lang keys, `scripts/verify-domain.sh` additions. No NPCs, blocks or player-terrain edits.
6. **Fixes found during pack runs** (keep these): `CombatGameTests.target()` spawns a no-AI `Zombie` because **MCA Reborn replaces villager damage handling**; `AgricultureGameTests` expects zero Reimagined winter penalty when Serene Seasons is loaded and the real reduction otherwise.

## Reproduce the checks

```sh
python scripts/fetch-modpack.py --destination <instance>/mods          # required only
python scripts/fetch-modpack.py --destination <instance>/mods --optional
PYTHONUTF8=1 python scripts/validate-modpack.py --mods <instance>/mods # Windows: UTF-8 mode needed
source scripts/cloud-shell.sh
./gradlew build
bash scripts/verify-integration.sh                                     # 99/99 required GameTests
```

To reproduce the pack run itself: copy all 25 jars into an ignored `run-gametest/mods/` and run `./gradlew runGameTestServer`. With the full pack: **99/99 passed**, mod list clean, no dependency/registry/mixin failures. The seasonal test branches on Serene Seasons; the melee tests must not use villagers while MCA is installed.

Windows note: `GRADLE_USER_HOME` under a Cyrillic user path breaks the test worker (`GradleWorkerMain` not found). Use `GRADLE_USER_HOME="$(pwd -W)/build/gradle-home"`.

## Overlap with `feat/reimagined-modpack-core` (do not merge blindly)

`origin/feat/reimagined-modpack-core` is at `21cdd49`, merge-base `c6a0cb9`. It already has a `modpack/` tree (`mods.lock.json`, `server.properties`, `jvm-args.txt`, Windows installer, `docs/MOD_REGISTRY.md`, `scripts/assemble-modpack.py`) and `core`/`settlements` bootstrap code.

- **Canonical pack definition:** prefer its `modpack/` pipeline and merge our 25 pins, the Apollo/Moonlight correction and `validate-modpack.py` into it rather than keeping two manifests. `pack/mods.json` is the verified 1.21.1 set from this branch.
- **True text conflicts:** `src/main/java/dev/reimagined/Reimagined.java` (their constructor adds `ModContainer`, `CoreBootstrap`, `SettlementContent`, orchard/beekeeping), `ARCHITECTURE.md`, `PROGRESS.md`, `ROADMAP.md`, `TESTING.md`, `THIRD_PARTY_NOTICES.md`, both lang files, `.gitignore` (`pack/mods/` vs their entries).
- **Possible semantic conflict:** `AgricultureGameTests.java` (they add orchard tests), `src/main/java/dev/reimagined/settlements/**` versus our `civilization` module — two settlement state models must not both become authoritative. Pick one `SavedData` registry.
- No merge was performed here.

## Recommended `server.properties` for two players

Not applied anywhere; merge with `modpack/server/server.properties` from the other branch.

```properties
max-players=2
view-distance=10
simulation-distance=8
online-mode=true
sync-chunk-writes=false
entity-broadcast-range-percentage=75
network-compression-threshold=256
spawn-protection=16
```

Terrain mods make chunk generation the main cost: keep `view-distance` ≤10 and `simulation-distance` ≤8, and use `-Xmx4G` with the pinned JDK 21. Do not force-load chunks to "pre-generate" during play.

## Known limitations

- **No real world was launched**: a normal client/server needs the user's own EULA acceptance. Terralith+Tectonic terrain, structure overlap, Incendium/Nullscape dimensions and chunk-generation cost are unmeasured. `reimagined:continents` must not be combined with Terralith in one test world.
- **Two calendars**: Serene Seasons owns crop growth; the Reimagined HUD still shows its own 96-day calendar. They can disagree.
- **No physical settlements**: no NPCs, buildings, roads, professions, inter-settlement trade, diplomacy or wars. Population is abstract; founding is operator-only (`/reimagined settlement found`).
- No TPS/MSPT/JFR profile of the settlement pass; the 99/99 run is correctness, not performance evidence.
- BetterEnd is Fabric-only on 1.21.1; Nullscape is pinned as terrain-only alternative. Cold Sweat is optional and off by default.
- Quest contract: consume `SettlementQuestEvent` by string kind id, do not create a second population registry, reuse the Reimagined calendar day; agree on biome/structure/player-UUID fields before freezing the payload.

Design references: `docs/MODPACK.md`, `docs/WORLD_DESIGN.md`, `docs/CIVILIZATION_DESIGN.md`, `docs/DIMENSIONS_DESIGN.md`, `docs/WORLD_PERFORMANCE.md`, `TESTING.md`.
