# Spell research and magic stations

This increment implements a finite, playable continuation of the four existing schools. All four basic spells remain available in old/new worlds. New personal knowledge does not reset existing wands, mana, skills or shared school cooldowns.

## Survival progression

Craft an Arcane Altar (amethyst shard,3obsidian,2copper ingots) and a Research Table (book,amethyst,paper,planks). Recipes unlock in the crafting book upon obtaining amethyst. Both stations have a real4-slot inventory, preview, mana/observation requirements and a server-authorized action button. Three ingredient slots are unordered; the fourth is output only. Materials stay in the block when its menu closes or the world saves. Breaking drops the actual inventory. No nearby dropped-item collection, multiblock search, mandatory dependency or automatic crafting loop exists.

| Altar focus | Ingredients | Mana | Research result |
|---|---|---:|---|
| Ember | Amethyst + blaze powder + redstone |15|Frost Lance folio|
| Verdant | Amethyst + barley + bone meal |15|Purification folio|
| Spatial | Amethyst + ender pearl + lapis |15|Magelight folio|
| Ward | Amethyst + gold + glowstone |15|Sanctuary folio|

At the Research Table, combine one focus, paper and lapis. Each folio costs20mana and requires personally recorded observations of2biomes and1creature. These are the existing journal discoveries, not a second exploration database. Using a journal also performs its normal bounded observation. Folios are physical, tradeable knowledge: the researcher must meet exploration requirements; a recipient may learn by reading a traded folio. Reading consumes one folio and permanently unlocks its spell. Already-known folios are not consumed or rewarded again. All four advanced spells can be learned; no mutually exclusive school choice or random success exists.

Sneak-use a wand to cycle that school's known spells. Normal use casts the selected spell. Selection persists in vanilla custom item data; casting still checks the **current player's** knowledge, so a borrowed/edited advanced wand cannot grant an unlock. Sneaking can return such a wand to its basic spell. Cooldowns remain shared by school, preventing basic/advanced switching from bypassing them. Mana is the existing100-point resource and regeneration system.

| New spell | School | Effect | Mana / cooldown |
|---|---|---|---|
| Frost Lance | Elemental |4magic damage +6sSlownessI; respects collision, teams/PvP checks |30 /3s|
| Purification | Nature |Removes poison/wither/hunger;6sRegenerationI |35 /8s|
| Magelight | Arcane |60sNight Vision +10sSlow Falling |20 /6s|
| Sanctuary | Ritual |30sFire Resistance +AbsorptionI, consumes one amethyst shard |45 /15s|

The journal adds two personal progress pages, retaining its strict vanilla-book size bound even at maximum discoveries. Browser results/uses show station, ingredients, mana and discovery requirements; folio recipes also show the current player's learned status/count. Small owner-only progress snapshots synchronize on login/respawn, changed discoveries and newly learned spells. Logout clears the primitive client snapshot. No client unlock/craft state is authoritative.

## Architecture, safety and work bounds

`magic` owns station registrations, menu/BE, recipe type/serializer pairs, finite Spell catalogue, versioned player knowledge and progress payload. Both `reimagined:arcane_infusion` and `reimagined:spell_research` use vanilla RecipeManager/sync with1..3nonempty ingredients, one result, mana1..100 and discovery requirements0..16. Existing shared recipe projection handles them. Server menu actions re-resolve actual recipes and verify owner/container/range/block/lock/spectator state, prerequisites, mana and output capacity before consuming anything. Unsupported buttons and repeated actions within10ticks fail closed. Shared inventory is mutated synchronously on the server, so concurrent viewers cannot spend the same ingredients twice.

Stations have **no ticker or scheduled work**. Only an open menu refreshes status/preview once per20ticks; no closed inventory or world-wide scan occurs. Actions inspect at most3ingredients and the existing bounded discovery data. Vanilla menu synchronization sends changed slots/data to viewers. World reads for station menus, comparators and validity use getChunkNow and fail closed when the ready chunk is absent. Normal persistence owns disk writes. No world/player references are retained in a global collection.

Basic and frost rays now use a loaded-only BlockGetter view: unavailable chunks behave as a solid barrier, without a request/future. Range is capped16blocks, living-entity candidate traversal stops after64visits, and the ray emits at most17particle-send calls rather than the previous65. This bounds target selection; crowded scenes may omit a target beyond the candidate budget. Nature targeting uses the same6-block loaded ray; blink checks at most16half-block steps and loaded collision-neighbor chunks before native collision checks. No newly researched spell scans chunks or runs a persistent effect tick handler; potion effects are Minecraft's own bounded effect system. Existing ritual group behavior is otherwise preserved. Packet bytes/TPS/large-scene cost still require actual profiling; source bounds alone are not a benchmark.

Knowledge uses a versioned player NBT bitmask of exactly4advanced spells, deep-copied on death. Selection is item-owned. Station inventory uses standard container NBT. No global research state or remote settlement/world simulation is introduced.

## Upstream research and provenance

Reviewed Ars Nouveau branch1.21.x at `8f0faf35e95dcad4578f33181779e5387b29dd3d`: license.txt(LGPLv3), ScribesTile, EnchantingApparatusTile, GlyphRecipe and Glyph learning code. Its separation of recipe-driven production and personal learned knowledge informed this independently written implementation. That checkout's version metadata mixes Minecraft1.21.0/ranges with NeoForge21.1.13; it is not treated as a drop-in1.21.1 API reference. Installed Minecraft1.21.1/NeoForge21.1.264 APIs and executed builds/tests determine compatibility here. No upstream source, textures, models or dependencies were copied. New pixel resources/geometry are original; model references resolve installed vanilla textures without redistributing them.

## Verification status

Source checkpoint:full build19JUnit tests,78/78required GameTests,141JSON/144bilingual resource checks passed. Ten new magic GameTests exercise real station transactions/discovery gates/costs/concurrency, rejected remote/full/low-mana requests, folio use, save/clone/isolation, selected-spell persistence and unlock authority, all four advanced effects, walls, no-load/no-ticker behavior, actual recipe codecs/sync/browser and personal journal/payload state. Graphical flow and real-server cost measurements follow separately. This is a finite research tier, not the complete eventual magic specification.

## Client flow and bounded-work follow-up

Native NeoForge client on Xvfb1280x800/Mesa llvmpipe, Russian GUI scale2, view4/simulation5, Java21/Xmx2G/ActiveProcessorCount4. A separately marked MagicSmoke copy preserves original worlds. Native altar activation consumed amethyst/barley/bone meal into Verdant Focus; shift-click transferred it to the research table. The menu correctly disabled research at1/2biomes and1/1creatures. A controlled biome fixture plus actual journal observation reached2/1; the table consumed focus/paper/lapis and mana for Purification Folio. Reading consumed it and produced a positive MAGIC_LEARNED condition on personal Known32. Journal page11 visibly showed1/4and Purification learned. Browser changed from0/4unlearned to1/4learned via the owner payload. Native sneak-use selected Purification in item components; casting after applying test poison removed it and added regeneration (positive MAGIC_PURIFIED check).

Initial native automation pressed sneak/use simultaneously, before the server received the movement flag; holding sneak before clicking verified the intended normal input. No gameplay bypass or source workaround was needed. For the biome fixture, a broad enough loaded region is needed because vanilla BiomeManager samples neighboring quart cells; narrow fillbiome boxes did not change the actual player biome. This checks observation gates, not natural exploratory travel.

Saved knowledge bits are now explicit stable constants rather than enum ordinals; selection still uses stable spell IDs. Tooltips describe the selected spell instead of incorrectly retaining the basic school's effect description. Added a96armor-stand correctness fixture checking the actual spatial-index query aborts at64candidates; this is an operation-bound test, not an AI/performance benchmark. Full build19JUnit/79required GameTests passed; final resources add eight matching EN/RU effect descriptions. Loaded BlockGetter BE reads also preserve access to ready block data for compatible modded collision shapes.

A vanilla JFR recording was captured during the functional client flow; it includes book pauses, software rendering and concurrent development checks, so it is not used as an isolated performance benchmark. An optional ordinary-server idle-station profile reuses the existing cooking diagnostic harness/analyzer through `./gradlew runMagicProfile -PprofileRevision=<commit>` in marked disposable run-magic-profile. Configure flat seed12345, view4/simulation4 and a free port; no EULA/authentication override. Phases:0baseline,64tables,64altars,256mixed stations,200warmup+600measured ticks each. Setup/cleanup16stations/tick; all inspect only ready chunks. Requires every station empty, with no ticker/scheduled work. Legacy pots/count and CookingTick fields remain for the shared analyzer, with explicit stationKind/notes distinguishing this workload. It does not measure open menus, mass casting, client packets or large-battle performance.

## Final executed validation —2026-10-28

Runtime production revision **92aca0a**; only subsequent translation-spacing/documentation edits affect the final package. Full Gradle build19JUnit, **79/79required GameTests**,141JSON/152bilingual keys pass. The extra dense-scene test covers64candidate abort and explicit stable bit IDs; selected-basic casts cannot bypass the advanced spell's shared cooldown. Both dedicated server and graphical client load the registrations, recipes, handlers and models. Player JAR excludes GameTests and profile harnesses.

A fresh complete client process reopened MagicSmoke. Positive native conditions MAGIC_KNOWLEDGE_RELOADED and MAGIC_SELECTION_RELOADED confirmed personal Known32and the saved `purification` item selection. The final selected-wand tooltip described Purification with35mana/8scooldown. Two paper items were placed in the research table through the standard Minecraft inventory command; after normal Save and Quit, reading the actual saved region NBT confirmed those2items in slot1of reimagined:magic_station. This complements the real BE load/save and player clone GameTests; no world files were edited to manufacture these persistence results. Screenshots/logs/JFR/worlds are ignored under run/ and run-magic-profile/.

### Ordinary-server idle-station profile

Shared diagnostic harness in **magic mode**, report identifier `magic-stations-1793164451540`, revision92aca0a. Ordinary20TPS server, no clients/extra mods, flat seed12345, view4/simulation4, spawn radius4,1089ready chunks. Oracle Java21.0.8/NeoForge21.1.264/Minecraft1.21.1, Xmx2GiB/ActiveProcessorCount4. Linux amd64 on Intel Xeon Platinum8573C;5visible CPUs,4CPU cgroup quota,32GiB memory limit.200warmup and600measured ticks per phase; setup/cleanup excluded and capped16stations/tick. No concurrent client/build/GameTest workload during measurement. Existing legacy pots/count/meal/CookingTick field names are diagnostic compatibility only; stationKind identifies the actual workload.

| Workload | Mean ms | p99 ms | Max ms | Ticks>50ms | Observed TPS |
|---|---:|---:|---:|---:|---:|
|0stations|0.276|2.961|7.845|0|20.000|
|64research tables|0.169|0.663|2.680|0|20.000|
|64altars|0.273|1.128|61.575|1|20.000|
|256mixed stations|0.442|7.645|37.707|0|19.976|

All stations were empty and had neither a ticker nor a pending scheduled update. The shared analyzer validated600custom measured JFR events in every phase. Whole-JVM sampled allocation weights per30s:9.49/5.20/5.70/0MiB; zero means **no sampled allocation events**, not zero allocation or memory cost. The64-table phase contained3GC pause events(max34.75ms); other phases had none. GC can occur between main-thread tick bodies, so pause duration and tick-body maxima are not interchangeable. Heap retention/leaks and save frequency were not measured. No forcedGC.

These short sequential samples establish the tested idle-station scenario, not a speedup or a guarantee for an arbitrarily large factory/server. The61.58msoutlier remains visible; timing alone does not establish its exact cause. This profile intentionally **does not cover** hundreds of open menus, synchronized spell spam, mass AI/PvP, connected-client packet rates or long sessions. The96-target GameTest protects an operation bound, not battle TPS. The actual software-rendered integrated client also logged long ticks during its mixed functional session; that trace includes book pauses/concurrent development activity and cannot isolate a mod cause. Do not present either trace as physical-GPU/FPS validation or as proof of universally stable TPS.

### Reproducing the manual chain

1. In a disposable command-enabled survival test world, place both stations and prepare amethyst, barley, bone meal, paper, lapis, nature wand and research journal.
2. At the altar, use the button to create Verdant Focus; verify inputs and mana changed. Transfer it with paper/lapis to the research table. With fewer than2recorded biomes, the menu must refuse research.
3. Observe a creature and two biomes using the existing journal. In the cloud fixture, loaded fillbiome regions changed the actual observed biome; natural travel remains a separate playthrough check.
4. Research, extract and read Purification Folio. Journal page11and browser should show1/4learned. A second identical folio should remain unconsumed.
5. Hold sneak before using the nature wand, then release sneak and cast. In the isolated fixture, applying poison first and checking regeneration/absence of poison confirms the effect. Mana/cooldown/tooltips must match the selected spell.
6. Save/quit the whole client, reopen with `./gradlew runClient -PquickPlayWorld=MagicSmoke` and verify learned state/selection/inventory. Never apply the fixture to a user's real world.

Native client coverage is Russian/GUI2and the complete Purification crafting/study chain. The remaining effects were then checked with explicitly supplied test folios/wands: Frost Lance left a controlled cow at6health with Slowness, Magelight applied Night Vision and Slow Falling, and Sanctuary applied Fire Resistance/Absorption while consuming the supplied shard. Positive native conditions MAGIC_FROST_OK/MAGIC_MAGELIGHT_OK/MAGIC_SANCTUARY_OK confirmed these outcomes. Folios were read normally; admin-preselected matching wands avoided repeating the already checked sneak-selection step. The stationary cow is a controlled effect fixture, not an AI/performance benchmark. The journal visibly reached4/4learned. These checks do not claim survival acquisition/crafting of all four chains. Natural exploration balance, trading folios between authenticated clients, real multiplayer, physical GPU/audio, standalone-JAR installation, sustained combat/network/save profiling and additional spell tiers remain open. No unrelated major subsystem was started during this verification.
