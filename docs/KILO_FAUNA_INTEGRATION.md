# Kilo fauna and nature integration — 2026-10-28

## Scope and ancestry

Target `feat/reimagined-alpha` started at published e7e6969 (following60b7a4a). Kilo820e5db has missing ancestors1002d09 (three animals),18eeada (behavior),78e5134 (plant groups); all are included by one history-preserving merge. Kilo25e4ea7 and prior thatch/clover/journal ancestors were already present and are not reapplied. Agent branches/worktrees are left intact. No additional major system or dependency.

## Review findings and fixes

- Incoming EN JSON had extra data after the root; incoming RU changes would regress the repaired catalogue. Preserve the complete main catalogue and merge only six new entity/egg keys, valid UTF-8 without BOM. Keep deer/boar registrations and client renderers.
- Three new animals had no registered client renderer. Add compact independent original quadruped meshes and layers using the supplied Kilo64px textures. Keep vanilla walking, head and juvenile transforms. Deer does not render unrelated Animal types.
- Otter's unconditional WaterAvoidingRandomStroll override neither prepared paths nor yielded movement safely. Use amphibious navigation, genuine horizontal surface swimming, buoyancy, bounded bank choice, a100tick land rest and a throttled return-to-water search. Float is lava-only; panic/predators, breeding, feeding and parent-following take precedence. Swim time accumulates across surface transitions and resets at rest, rather than requiring uninterrupted water contact. State is ephemeral AI state; ordinary Animal data persists normally.
- Land-only grass spawn rules made river otter spawning effectively unsuitable. Register IN_WATER with a two-water-block surface/air rule, still under ordinary CREATURE cap and river-only small groups. Marmot habitats use grassy meadow/windswept hills/forest instead of bare/frozen peaks rejected by ordinary animal soil rules. Natural encounter frequency still needs a longer survey.
- Rain shelter previously only stopped a badger outdoors. It now chooses covered walkable ground, creates a reachable path and actually walks under a roof. Shelter ends when rain ends or danger/feeding/breeding preempts it.
- All-day marmot idle previously blocked movement, and player avoidance conflicted with food. Daytime ordinary wandering is more frequent; nighttime idle claims MOVE at low priority and yields to danger, player escape and food. Creative/spectator or food-bearing players do not trigger avoidance.
- Deer look-around now owns MOVE/LOOK, uses two stable lateral targets over40ticks and a cooldown, and yields to existing higher-priority movement goals. No per-tick random head target or independent global timer.
- Plant JSON used nonexistent minecraft:patch, unsupported fields and nonexistent old_growth_taiga. Use codec-loaded vanilla random_patch/simple_block, correct biome IDs and actual two-half tall-grass placement. Grass12trials at rarity8; fern8trials at rarity10; spread3/1, one candidate maximum, no count multiplier. Existing clover/boulders/trees are preserved.
- Add simple0–1vanilla-resource adult loot tables (cod/leather/rabbit hide); no new production chain.

## Runtime bounds

Custom searches read only FULL chunks through getChunkNow/direct LevelChunk state reads. Otter bank selection checks81positions (three states each) every40swim ticks after its swim interval. Return-to-water checks363positions at most once per100–119ticks; badger checks243candidates at most once per60–79ticks. Both attempt at most one vanilla path per search and require the24block surrounding chunk square ready (at most16chunks) before navigation. Negative searches are throttled and staggered. No entity-wide/world/chunk scan, forced chunk generation, gameplay async task, custom payload, forced save or per-tick synced flag. Vanilla movement synchronization remains vanilla. Idle rest/look countdowns run only while their server Goal runs.

These source bounds are not evidence of acceptable dense-population TPS. Native measurements and their limits are recorded below; no universal optimization or complete Alpha claim.

## Tests

The original two behavior tests were invalid: they mixed relative/absolute coordinates, used a player absent from the entity index and only checked water occupancy. Replaced with real state/displacement tests. Indexed Survival FakePlayers are actually added to ServerLevel and removed; separate day/night/rain batches restore world time/weather even when a test fails. Controlled glass-walled arenas keep the actors on the test floor; tested animals retain normal AI.

Coverage: horizontal near-surface travel, dry-bank rest, unforced land-to-water search; covered badger displacement; actual daytime wandering versus nighttime rest; real player escape including nighttime preemption; food temptation over avoidance; actual deer head-angle changes followed by hurt-driven movement and look preemption; spawn entries/surface placement; species offspring and vanilla age/health/name/persistence NBT; sparse biome-scoped plant codecs/4096rarity samples; actual fern and both tall-grass halves. Prior systems' tests remain required. A first pass exposed an otter surface-transition timer defect; repeated deer test exposed a fixture edge/path-lifetime assumption. These were corrected, not marked passed or ignored. One overlapping harness attempt hit the world session lock and was not counted; only a fresh isolated complete report is final acceptance.

## Executed final checks

Full Gradle build passed **25/25 JUnit**. The final fresh isolated official server report passed **132/132 required GameTests**, no failure/error/skip. Resource validation passed299JSON/203EN-RU keys. The Overworld feature sorter resolves all possible vanilla biomes with our combined modifiers without a cycle. Player JAR was inspected: no GameTests, profiling classes or empty template. `build/libs/reimagined-0.1.0-alpha.jar` SHA256:51bc75a9c08a74412d13da5eff0f135ca042c1541272b5edac006c0e70bd5e72. Binary/checksum remain ignored local artifacts, not uploaded in Git.

## Native acceptance

Actual Minecraft1.21.1/NeoForge21.1.264 graphical client, Java21, RU/GUI2, Xvfb/Mesa llvmpipe (no physical GPU/audio), marked disposable AgentIntegrationSmoke world. This uses supplied grass platforms, roof/pool and summoned normal AI-enabled animals, not a natural survival encounter. The ignored FML config-watcher workaround described in the previous integration remains necessary; authentication service DNS remains unavailable, no security bypass.

Otter's compact textured model, walking/surface motion and repeated shore rest were observed. A controlled initial teleport into the pool supplied the starting position; subsequent exits/rests/returns ran unforced. Read-only telemetry recorded dry grounded rest at18.809/181/14.587 over12:26:17–21 and another at18.782/181/12.452 over12:26:44–48, followed by further cycles. Badger travelled from outdoors to the roof at5.643/181/6.489 and remained covered while raining; its textured model displayed. Deer rendered and periodically entered look-around with changing head angles. All four tagged animals reappeared after a complete client-process save/restart before the profiling fixtures replaced them; transient goal timers are not asserted persistent.

Marmot moved in daylight, held position at23.290/181/19.264 after time was changed to midnight, then fled when the actual nearby player switched from Creative to Survival:26.020/181/22.021 then28.516/181/26.010. This occurred before the later camera teleport. That later attempt teleported the player outside the platform and fell into water; its screenshot is **not** acceptance evidence of fleeing. Food temptation, natural placement rules, offspring/persistent NBT, exact two-sided deer angles and safety arbitration have direct server-test coverage, not manual survival/breeding coverage for every new species.

Both configured plant features were placed through real `/place feature`, displayed intact tall grass and ferns and survived normal ticks. This verifies the runtime feature/visual path, not natural patch frequency. Biome scopes, exclusions, bounded trials, rarity and whole-generator feature order are covered by GameTests. A longer multi-seed natural-spawn/vegetation survey remains open.

## Native population measurement

Same final creature source diff fingerprint(relative toe7e6969):f1a4fb93c6fe7a10d423fa72bb993504c0f2a1719e1cbebbf261d621073a0806. Host reports5XeonPlatinum8573C CPUs; client JVM has4active processors/2GiB heap, Mesa LP_NUM_THREADS=2, view4/simulation5. Integrated server in existing vanilla-noise disposable world;30×30enclosed grass arena aty180,11×11one-block-deep water pool at8..18, roof1..5/183/5..10. Normal background world simulation retained. No NoAI, tick freeze, changed tick rate or AI budget suppression.

An initial marker-passenger bulk-spawn attempt produced0animals, detected by the actual entity counter; it was discarded entirely. Corrected fixtures use32ordinary individual summon commands in a dev-only datapack. Baseline32cows, candidate8each otter/badger/marmot/deer, same four starting areas. The live counter confirmed32before each measurement and32after; a final NBT predicate counted0NoAI actors. Vanilla AI remains running, predators/players not artificially targeted. Rain enabled; the world clock advances naturally (candidate approaches nighttime), so this is a bounded scale smoke comparison, **not a controlled optimization speedup experiment**.

|600 individual ServerTickEvent.Pre→Post samples after100warmup ticks|32cows|32mixed fauna|
|---|---:|---:|
|Mean ms|9.585|6.618|
|p95 ms|22.022|14.035|
|p99 ms|103.093|36.541|
|Maximum ms|392.651|510.310|
|700tick elapsed wall seconds|34.929|34.999|
|Used heap before/after MiB|376.9/636.5|375.7/319.7|
|GC MXBean collection-time delta ms(includes warmup)|77|29|

Approximately20ticks/sec sustained over these short windows, with real isolated long ticks. These are direct empirical per-tick percentiles from600samples, not percentiles of Minecraft's one-second averages. Shared software rendering, world time/weather, background entities/chunk/random ticking, short duration, no repeated order-randomized trials and a brief concurrent JFR-analysis process during candidate capture limit comparison. Used heap is **not retained-memory/leak evidence**. Do not claim a speedup or stable-release TPS.

JFR captures were collected alongside both windows. Whole-capture allocation estimates were roughly17.3/19.6MB/s, but capture lengths differ and the second built-in summary lacks periodic CPU/ServerTick fields; its buffer also includes earlier events. Whole-file CPU/network/save aggregates are therefore not a matched per-scenario comparison. Standard entity/chunk packets and ordinary world IO appear, with no new production payload/forced-save implementation. Do not interpret whole-JVM allocation/IO as new-goal costs. The candidate30second measurement interval contains176server-thread execution samples:22top frames in vanilla BitRandomSource.nextInt,16in PalettedContainer.get and8each in natural spawning/block collisions; one stack contains a FaunaGoals avoidance predicate. That small sample does not identify new goals as a dominant hotspot and cannot attribute the510msoutlier. Use timestamp-filtered execution samples for hotspot inspection; per-goal allocation, retained heap, synchronized network/save deltas and standalone-server repeat trials remain open.

## Reproduction and remaining Alpha gates

`python3 scripts/create-fauna-qa.py run/saves/<marked-disposable-world>` creates an opt-in QA datapack only if .alpha-approved-disposable exists. Prepare the arena/roof/pool above; enable the datapack and create `scoreboard objectives add fauna_count dummy`. Run client with `-PquickPlayWorld=<world> -PfaunaAcceptance` and the cloud shell/Mesa settings above. `/function reimagined_qa:fauna_baseline` or `fauna_candidate` supplies the ordinary actors and records count. Start native `/jfr start`, then `/tag @s add fauna_baseline` or `fauna_candidate`; wait for NATIVE_FAUNA_PROFILE completion without pausing/changing the scene, stop JFR and recount actors. The opt-in read-only probe collects100warmup+600samples once per label per client process, logging at most16tagged QA subjects once/sec outside measurement; it is excluded from the player JAR. Remove profile tags before saving. For tighter repeatability pin time/weather and rerun both orders in independent processes; never change AI to improve results.

No next major system started. Open Alpha gates: multi-seed natural encounters/density, manual breeding/juvenile visuals and audio for these species, long survival/save sessions, physical-GPU and standalone-JAR checks, authenticated multiplayer, reproducible standalone-server profiling/retained-memory/network-save comparison and attribution of isolated long ticks. The observed510msmaximum is unresolved and not asserted harmless or caused by our AI. Broader full-specification geography/civilizations/transport/dungeon/late-dimension gaps remain in ROADMAP. Integration is verified within the stated scope; a complete playable Alpha is not declared.
