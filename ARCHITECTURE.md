# Architecture

Single entry point: `dev.reimagined.Reimagined`. Each module registers its own deferred registries through the mod event bus; gameplay hooks use the NeoForge game bus. The main developer owns all modules and integration in feat/reimagined-alpha. No upstream gameplay-mod code is embedded.

| Module | Responsibility | State / authority |
|---|---|---|
| `season` | 96-day calendar, local crop growth, calendar commands | Overworld SavedData stores versioned offset; overworld day time is authoritative |
| `progression` | 10 skills, uncapped XP/levels, finite mastery | Player persistent NBT, decimal BigInteger, copied on player clone; server event XP and commands |
| `engineering` | water flow, axial connectivity, bounded load, milling | Shared bounded component scheduler; motion snapshots/client BER; milling every20 ticks staggered by position; NBT inventory/progress; WorldlyContainer and sided IItemHandler |
| `content` | weapons/specials/parry, four wand schools | Server use/damage events, bounded mana, shared saved special/parry cooldowns copied on death |
| `research` | readable journal and personal observations | Versioned player NBT, 256 total unique registry IDs, deep-copied on clone; every200 player ticks/current book use, 6-block local scan with at most8 visibility traces |
| `creature` | Deer entity and natural spawning | Vanilla entity persistence/pathfinding; client model registration isolated by Dist.CLIENT |
| `browser` | common display/query/cache/adapter API, brewing and smithing projection | Immutable recipe snapshots, actual Ingredient predicates, exact potion variants, no client-only imports |
| `client` | existing inventory item panel and recipe screen | Read-only common projections; recipe-sync invalidation, world/logout lifecycle, no client crafting authority |
| data `worldgen` | optional world preset | Stateless seeded chunk noise, built-in registry codecs |

## Network

`reimagined:calendar` is a version-1 clientbound payload with a single varlong day. Server pushes on login and every 100 ticks and after operator season changes. The handler only updates a primitive value cache, with no common-side references to client-only classes. The HUD is client-only. The research journal uses standard written-book data components and vanilla held-slot/OpenBook packets, explicitly syncing pages before opening; no additional writable client research state exists. Vanilla networking handles entity state, item use, player messages and recipe synchronization. Skill inspection is via server-generated command output, not a writable client cache.

## Shared recipe contract

New process recipes implement vanilla `Recipe<RecipeInput>` and register a `RecipeType` and `RecipeSerializer` with Mojang MapCodec and network StreamCodec. `MillingRecipe` is the first custom implementation: ingredient, result stack, processing seconds. This uses existing data-pack reload/sync rather than a second recipe database. Future smithing, alchemy and cooking processors must use the same contract. Not all future systems exist yet. Vanilla brewing now has a public-API projection and smithing transforms use the public recipe codec. Dynamic special crafting and opaque custom brewing predicates still require adapters; see docs/RECIPE_BROWSER.md.

## Performance and boundaries

No world-wide tick scans. Mechanical graphs inspect at most 128 loaded positions and never deliberately load chunks. Crop changes run only in existing random growth events; greenhouse checks stay in one loaded column. Deer uses standard bounded AI goals. Remote civilization simulation and climate weather effects are future work; no placeholder simulation is presented as complete.

Player state is server-owned. There is no state-level player kingdom creation API and no NPC war terrain destruction. Both civilizations and wars remain unimplemented.

## Performance contracts

These requirements apply to new modules, changes to existing modules and FCC-Codex integration. The target is stable 20TPS on the declared supported workload, predictable tick latency and bounded memory. A server tick has a 50ms budget at 20TPS; that is an aggregate budget for vanilla and all mods, not an allowance for each machine. Current implementation limits above are design facts, not evidence that current large-scale performance is satisfactory.

For each potentially expensive mechanic, document its trigger/cadence, maximum work per activation, aggregate work with many instances, loaded-chunk policy, retained state and cleanup, cache invalidation, packet recipients/rate and persistence behavior. Prefer events and change notifications. Stagger or budget independent machines so aligned updates do not create periodic spikes. Bounds must also apply to work queues and catch-up after inactivity; defer or degrade optional work explicitly rather than running an unbounded backlog in one tick.

World inspection must stay within loaded state and must not acquire chunk tickets or call generation synchronously. Heavy terrain computation belongs in Minecraft's supported chunk-generation stages. A bounded terrain sample or test that generates chunks does not authorize moving that work into a gameplay tick. Background computation is permitted only with safe immutable inputs, bounded queues, cancellation/lifecycle handling and safe result application; moving live world reads to an arbitrary thread is not an optimization.

Cache ownership must match its lifetime (world, dimension, chunk, player or UI). Define a size/eviction policy where growth is possible, invalidate on relevant source changes and unload/disconnect, and release owner references. Prefer initial synchronization plus change-driven/delta updates to unchanged broadcasts; coalesce repeated changes where safe. Dirty flags reflect actual mutations, and normal Minecraft persistence owns disk writes. Review unnecessary hot-path object creation using allocation profiles rather than pooling or rewriting code without evidence.

## Performance validation and real-Minecraft profiling

Automatic checks protect implementation budgets: cap traversal/read counts and query frequency, reject unloaded access, exercise cache reuse/invalidation/unload, and verify unchanged state does not cause redundant packets or dirty/save work. Select checks relevant to the feature rather than adding meaningless timing assertions. GameTests run accelerated scenarios and shared environments have variable load; their runtime alone is not a gameplay benchmark.

For a potentially costly stage or FCC change, the main developer must also run or review a reproducible profile in an actual Minecraft runtime before calling its performance validated:

1. Record exact commit/build, Minecraft/NeoForge/JDK and JVM flags, mods/datapacks, hardware/resource limits, seed/world state, view/simulation distance, loaded chunks and player count. Keep conditions comparable between baseline and candidate; distinguish steady-state gameplay from chunk generation, startup or recipe reload.
2. Use an isolated disposable world. Capture an idle baseline, a representative workload, and increasing declared counts of mechanisms/creatures until the intended target scale is represented. Include interacting networks/AI, lifecycle changes, cache invalidation and burst updates where relevant. Do not force-load a huge world merely to create a benchmark. Define safe stop conditions for sustained overload or memory pressure.
3. Warm up until loading/JIT and activity have settled, then measure for a stated interval long enough to include periodic work and saves. Report the actual warmup, duration, instance counts and profiler overhead. Repeat comparable samples when variability makes the conclusion uncertain; do not replace measurements with arbitrary universal timing thresholds.
4. Use Minecraft's supported profiling/JFR facilities (for example `/jfr start` and `/jfr stop` when available), JVM tooling or an explicitly approved profiler. Measure server TPS/MSPT, CPU hotspots, allocations, GC pauses and retained memory. Record p95/p99 or worst-tick latency only when the instrumentation actually provides per-tick samples; do not invent tail latency from an averaged JFR event. Inspect memory after repeated load/unload/reload cycles under comparable conditions, without adding forced GC or disabling gameplay to improve results.
5. Measure networking with actual connected clients where required: packet/payload rate and bytes, recipients and unchanged-state behavior. Inspect persistence/change frequency and save stalls where relevant. A headless test with no connected client cannot establish client UI performance, network scalability or real multiplayer behavior.
6. Summarize baseline and candidate results, regressions, hotspots, supported workload and limitations in TESTING.md/PROGRESS.md or a linked feature report. Keep raw JFR recordings, logs, generated worlds and build artifacts ignored. Correctness remains required; do not trade away assertions, security or required simulation to make numbers pass.

FCC handoff reviews must include the same cost contract and actual profiling evidence for potentially expensive changes. If profiling cannot be performed, record that gap and withhold a performance-readiness claim; compilation and passing GameTests remain separate correctness evidence. Optimize observed structural bottlenecks first. No new profiling run or optimization is claimed by this documentation update.

## Mechanical component and animation update

Engineering now uses the per-dimension bounded cache/scheduler described in [MECHANICAL_SYSTEM.md](docs/MECHANICAL_SYSTEM.md). Passive shaft block entities have no tickers. Server-authoritative changed-motion snapshots feed a client BER with reload-cached rotor quads; stopped phases persist across saves and require topology revalidation before resuming. Earlier references to mechanical caching as future work are superseded by this implementation.

Before major features, review existing open implementations, pinned licenses and NeoForge compatibility. Reuse licensed components when proportionate, otherwise apply learned principles independently. Record provenance and attribution; never assume code licenses cover art. New mandatory mod dependencies require user agreement. Current development is solely on feat/reimagined-alpha; historical FCC ownership restrictions no longer apply.

## Ordinary agriculture

`agriculture` adds barley through a vanilla CropBlock subclass and ItemNameBlockItem seed. Growth remains vanilla random ticking and the shared seasonal event; no new per-tick coordinator or persistent farm simulation exists. Grass seed acquisition is an additive NeoForge loot modifier using the built-in add_table codec. Milling uses the existing reloadable recipe type/automation unchanged. Barley details and upstream review: docs/AGRICULTURE.md.

## Finite dynamic recipe previews

The browser snapshots one canonical exact vanilla ShulkerBoxColoring holder per recipe refresh. The selection-only ShulkerColorDisplays adapter assembles up to33 concrete previews using vanilla results and strict component ingredients, preserving container contents and names. No recipe-wide expansion occurs per frame/tick in the default bridge. Snapshot replacement/logout invalidates retained recipe holders; arbitrary subclasses are excluded. Optional adapters keep the existing extension path and can override the built-in. Other special recipe families remain explicit future adapters.
