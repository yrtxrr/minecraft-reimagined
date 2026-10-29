# Progress

## 2026-10-29 — rare mountain iron vein

Reviewed `docs/USER_SPEC_RU.md`, roadmap and existing world generation. The suggested fallen trees, stumps and clearings are not specified as required natural features; the geology section does explicitly call for small veins and large deposits. Added one small vanilla iron ore feature, placed in the vanilla `underground_ores` step only for `minecraft:is_mountain` biomes, with a 1-in-12 rarity filter and uniform Y=20–112 placement. It is additive to ordinary ore generation, uses standard configured/placed feature and NeoForge biome-modifier data, and adds no tick-time work or structure edits. This is a biome-local small vein, not regional geology or a large deposit. English/Russian names and world-generation scope/roadmap notes were updated.

Validation: `./gradlew build --max-workers=1` passed; the resource validator passed for 220 JSON resources, local model/texture references and 184 bilingual keys; fresh NeoForge GameTestServer passed all 93/93 tests, including two new tests that verify exact mountain-tag membership and actual iron-ore replacement in stone. The first build attempt failed because parallel Gradle test workers could not load `GradleWorkerMain`; rerunning with the existing local Gradle cache and one worker passed without disabling tests. A graphical Minecraft client/display was unavailable in this Windows checkout, so no screenshot or in-game visual observation of the vein is claimed. This does not complete broad geological provinces/large deposits or full-spec world-generation work.

## 2026-10-27 — initial implementation

Started from a verified empty GitHub repository. Preserved the complete specification; split implementation across independent modules and integrated registrations centrally.

Implemented source: NeoForge scaffold; calendar persistence/network/HUD and seasonal crop event; ten skill trees with finite upgrades and uncapped BigInteger levels; four wand schools and differentiated iron weapons; deer entity/model/texture/spawns; water wheel/axial shaft/millstone with custom milling recipes; client inventory item panel and recipe browser; optional world preset.

Verification is deliberately split: independent Java-rule tests execute without Minecraft. They caught a real static-initialization defect in SkillProgress; it was corrected and the suite rerun. Resource validation and upstream API inspection are separate checks, not proof of a successful game launch.

**Historical blocker at initial checkpoint:** actual cloud proxy denied CONNECT to `maven.neoforged.net` (403). Network domain additions were saved to the environment draft; saving the draft does not apply the running machine's network policy. Full compile, server launch, graphics and installable JAR remain unverified. No graphical client has been run. The alpha release gate remains open.

Next action after network changes apply: `bash scripts/setup-cloud.sh`, fix all resulting compile errors, then test dedicated startup/preset generation/persistence and prepare a client test build. Full specification systems such as civilizations, transport, bosses and watershed hydrology are not implemented.

## Continuation from 4090fdb

Stayed on `feat/reimagined-alpha`. NeoForge Maven became reachable through the authorized proxy. Full build now passes; fixed glass tag API compatibility and Java lambda capture, then added server integration coverage. 12 JUnit tests passed; all 17 GameTests passed on a real headless NeoForge GameTestServer. The initial worldgen test failure was a fixture lifecycle/status issue and was fixed without weakening assertions. An isolated same-seed pair of chunks went through actual biome/noise/surface generation and block comparison. Current XML reports distinguish actual execution from planned manual checks.

`docs/FULL_SPEC.md` is absent; reported this to the user. Existing complete requirements remain in `docs/USER_SPEC_RU.md`. No systems were removed. Test classes/template are excluded from player JAR. Real GUI rendering and multiplayer/login remain untested; the alpha is not declared fully ready. Authlib public-key service remains inaccessible in this cloud instance, without disabling security checks.

## Engineering automation and GitHub checkpoints

GitHub read and push access verified through the existing HTTPS proxy. Published the initial history and checked stage cdaaeb6 on `feat/reimagined-alpha`, without force pushing or changing other branches. Per user request, completed stages are now immediately committed and pushed; `AGENTS.md` records this for future tasks.

Implemented sided hopper/NeoForge item-handler automation for the millstone, recipe filtering, bounded inventory and progress-preserving save/load. Full Gradle build and **20/20** server integration tests passed. Added a fresh-report integration runner and CI execution. No temporary builds/worlds/credentials are tracked.

## Combat checkpoint

Implemented four server-authoritative weapon specials with saved shared cooldowns, attack-charge/durability limits, line-of-sight/alliance/PvP checks; automatic timed frontal shield parry respecting vanilla warmup/facing/unblockable sources. Normal attacks and offhand shielding remain vanilla. Full build passed with12unit tests and **27/27** real server GameTests. This does not validate graphical input/animation or complete the whole combat/armor specification.

## Research journal and pause for FCC-Codex coordination

Replaced chat-only guidance with a real readable written-book journal: localized guide chapters (including deer and current biome information), personal observed biome/creature catalogues, bounded local ten-second observation, versioned player saves/death cloning, per-reader rebuilding and explicit pages-before-open network ordering. This is not a research unlock/quest system.

Exercised the saved cloud setup script successfully, then full build with12unit tests and **32/32** required server GameTests. Five journal tests check real item use/network components/packet order, observer cadence/visibility/deduplication, NBT/clone and per-reader isolation, bounded valid entries/book size, and packaged EN/RU chapters. A packet class-name mismatch was corrected against Minecraft1.21.1 APIs before the final passing run. Graphical book display, GUI scale and real multiplayer/authentication remain unverified.

Prepared the player JAR under ignored build/libs with SHA256; test fixtures are excluded. Per user's new organization request, stop new implementation after this checkpoint and push. Four independent proposals and reserved shared files are recorded in docs/FCC_HANDOFF.md. No tasks assigned to FCC, no FCC branches created, and no next major stage started. Main developer retains architecture, integration, testing and merges.

## Built-in recipe-browser stage

User assigned FCC-Codex barley in its separate branch and authorized main-browser development on feat/reimagined-alpha. Audited existing code/full spec; preserved the panel/screens and existing uses mode. Added shared server-testable display/query/cache APIs, actual vanilla brewing and exact potion variants, public-codec smithing ingredients, multi-term/@namespace/potion-ID search, panel categories, page-preserving history/R/U/Backspace navigation, per-context caching and sync/logout invalidation. Added explicit dynamic-adapter dispatch; arbitrary special recipes are still unimplemented and not presented as complete.

Full build +16unit tests +41/41 real server GameTests passed. Rendering/client-network path remains unverified; documented exact remaining checks. No agriculture files or FCC branch were touched; no other major gameplay subsystem started. Details: docs/RECIPE_BROWSER.md.

## Cross-project performance policy

Recorded the user's high-priority performance rules in AGENTS.md and ARCHITECTURE.md: event/local/bounded work, aggregate tick budgets, loaded-only inspection, proper generation/thread boundaries, bounded lifecycle-aware caches, change-driven networking/persistence, and meaningful cost/scale checks. FCC integration now explicitly requires both correctness review and performance review. Real Minecraft profiling with a reproducible workload/baseline and measured TPS/MSPT/memory/CPU is required for costly features; successful builds/GameTests alone are not performance evidence. This stage changes documentation only; no runtime profiling or gameplay optimization was performed.

## Mechanical motion and bounded network checkpoint

Implemented native client BER animation for wheel/shaft/mill rotors with reload-cached geometry, synchronized common phase, state-dependent stopping and frozen persisted angle. Shared loaded-only component cache and capped per-dimension scheduler replace independent production BFS calls; topology/unload invalidation stops stale consumers. Existing milling, recipe reload lookup, hopper/capability access and inventory/progress NBT remain compatible. Create architecture/license research is documented without copying code/assets or adding dependencies.

Checks:full build19unit tests,49server GameTests; ordinary-server JFR baseline/candidate0/16/64components, and actual normal-world save/restart64mills passed. See docs/MECHANICAL_PROFILE.md for measurements, allocation/GC limitations and long ticks. Graphical animation and live client chunk streaming are not verified. All development now belongs to the main developer; FCC handoff is historical. Next:ordinary barley crop, then the first remaining browser increment in ROADMAP.md.

## Ordinary barley checkpoint

Implemented barley/seeds and eight-stage vanilla cultivation, original pixel resources, age-dependent harvest/replanting, additive short-grass seed loot with shears exclusion, bread/seeds/milling recipes and recipe-book unlocks. Compost/common tags/seed animal-food tags added. Existing seasonal growth/shelter hooks and powered mill automation work without modifying either system's implementation. No mandatory mod dependency or farm simulation added; reviewed Farmer's Delight and used the installed vanilla/NeoForge APIs directly. Full build19unit tests,55server GameTests and93JSON resource checks passed. Graphics and large-field profiling remain manual work.

## Next roadmap increment: concrete shulker coloring

Built-in recipe browser now projects actual dynamic shulker coloring with selected item components/cargo preserved,16 outgoing colors and17 incoming box variants, strict source ingredient matching and bounded selection-only construction. Recipe snapshots retain a reload-aware canonical vanilla handler, cleared on logout; custom extension adapters can override it. JEI's finite replacement approach/license was reviewed; no source/assets/dependencies copied. Full build19unit tests,58server GameTests and93JSON resource checks passed. This completes one concrete dynamic-recipe increment, not arbitrary special crafting or recipe transfer. Client visuals/reload interaction and multiplayer still need graphical validation; the alpha gate remains open.

## Graphical Alpha validation checkpoint

Launched the actual NeoForge graphical client using isolated Xvfb/Mesa tools, entered/reopened the disposable world and inspected rendered mechanisms/crops/deer, Russian journal and browser. Fixed two real client issues:double background blur hiding recipe content, and milling recipes entering unsupported vanilla recipe-book categories. Full build19unit/58server tests passed. A separate AlphaObserver client reaches the local server but login is blocked by unavailable authentication services; no authentication bypass was introduced. Alpha remains under validation, with physical-GPU, multiplayer, long-session and standalone-install checks open. Details:docs/ALPHA_VALIDATION.md.

## Loaded-read performance correction

Full-stack JFR investigation found ticket-only hasChunkAt checks followed by Level getters entering chunk-future machinery. Mechanical inspection/callbacks now use getChunkNow and direct ready-chunk reads, failing closed without requesting generation. The packed-chunk index now uses the existing platform primitive-long map instead of boxed collision-tree lookups. Budgets/recipes/saved inventories/automation stay unchanged. Full build19unit/58server tests passed; fresh ordinary-server profile0/16/64 passed all real mill-output/budget checks. Remaining rare long ticks and attribution limits are retained in docs/MECHANICAL_PROFILE.md rather than hidden. Alpha graphical validation includes native farming, four spell schools/weapon specials, true chunk streaming and fresh preset-world creation; authenticated multiplayer and standalone-install/physical-hardware checks remain open.

## 2026-10-28 — cooking workstation and connected meals

Added a real heated cooking pot with a localized5-slot menu, timed server-owned processing, heat/progress indications, sided item capabilities, physical hopper automation, comparator output, destruction drops and saved work/inventory. Five meals connect barley, mill flour, deer/beef, vegetables, mushrooms and fishing; eating restores hunger/saturation and returns a bowl. Datapack recipes synchronize through the existing vanilla recipe/browser contract, including uses/station/time/bowls; journal adds cooking instructions. No new mandatory dependency, farm simulation or per-tick BE scanner. Empty pots sleep; active work is staggered once/second, negative recipe matches are cached with reload invalidation. Full feature/lifecycle/provenance:docs/COOKING.md.

Full build19JUnit/67required server GameTests and113JSON/106bilingual resource checks passed. Nine new cooking tests cover real scheduled cooking, ingredient matching, pause/output/bowl gates, save/load, reload positive/negative cache invalidation, side capabilities/menu routing, actual hoppers into a chest, eating/bowl return, browser and codecs. The ordinary-server pilot profile completed0/64idle/64active/256active pots with correct3meals from every active pot and no scheduled idle work. Concurrent Gradle/GameTest activity contaminated timings (256phase mean8.208ms/max1591.7ms), so this pilot is not a performance-readiness claim. A quiet run and direct client UI/model checks follow this source checkpoint. Alpha's prior multiplayer/physical-GPU/standalone-install gates remain open; no unrelated large system started.

### Cooking client checks and loaded-entry correction

Native client input verified the rendered pot, RU menu, actual progress/output, shift-click meal extraction, hunger restoration/bowl return and browser recipe/station/duration. Quiet4025077 profiling completed with all meal/idle checks, but256pots retained rare1046.8mswhole ticks. A sample exposed a Level.getBlockEntity entry read, so all pot entry/menu/comparator/neighbor inspection now uses getChunkNow rather than requesting FULL access. Full build19JUnit and68/68GameTests passed, including distant no-load inspection and idle-sleep coverage. Added an optional runClient quickPlayWorld property and reusable JFR/report analyzer. Exact final-source profiling and process-restart confirmation are the remaining focused checks, not a new subsystem.

### Cooking increment complete

Published runtime source7b92e31 passed the final ordinary-server0/64idle/64active/256active profile, all real meal counts and no-idle-work assertions. At256pots:mean0.850ms/p992.803ms/max102.571ms,19.999TPS,768meals; the empty baseline also retained large tails, so no lag-free or isolated speedup claim. JFR allocation/GC observations, noisy earlier runs, exact scenario and unresolved attribution are preserved in docs/COOKING.md. Added thread labels and excluded post-measurement audit work from the analyzer; moved only diagnostic measurement-end metadata before that audit.

A new graphical client process reopened the saved CookingSmoke world, verified4retained meals, reopened the RU menu and cooked a fifth from exactly one added barley/apple/bowl. A native positive condition confirmed persistence; the reopened final menu showed five meals and empty input/bowl slots after processing. Full final build19JUnit,68server GameTests (10cooking tests),113JSON/106bilingual resource checks passed. Player JAR excludes both profile harnesses and GameTests. Full Alpha survival/multiplayer/standalone/physical-GPU/audio gates remain open. No next large subsystem was started during this focused runtime/performance follow-up; next content priority is research progression/spell stations tied to discoveries and existing resources.

## 2026-10-28 — finite spell research and magic stations

Implemented Arcane Altar/Research Table with saved4-slot inventories, server-owned manual recipe transactions, functional localized menus and explicit mana/discovery/output checks. Four focus recipes link existing resources (including barley) to four tradeable study folios. Reading unlocks one additional real spell per school: Frost Lance, Purification, Magelight, Sanctuary. Existing basic spells remain available. Selected known spells persist on wands, school cooldowns are shared, player knowledge is bounded/versioned/deep-copied on death. Journal and browser show personal research; changed/initial progress sync is owner-only and clears on logout. Stations have no tickers. Loaded-only rays/entry checks and capped64candidate/17particle ray work strengthen spell boundaries without replacing existing schools.

Full build19JUnit,78/78required GameTests and141JSON/144bilingual resource checks passed. Ten new tests cover transactions/concurrency/security, actual effects, folios, knowledge/selection save and clone, no-load/no-ticker behavior, recipes/browser/codec/personal progress. Upstream Ars Nouveau research/license/version caveats and design are in docs/MAGIC_RESEARCH.md; no source/assets/dependency copied. Native client flow and real-server workload measurements remain to be executed after this source checkpoint.

### Spell research native flow checkpoint

Real client verified altar focus production, table's missing-observation rejection, two-fixture-biome journal observations, research production, folio consumption/unlock,1/4journal/browser status and selected wand components. Native Purification removed actual poison and supplied regeneration. Knowledge bit IDs are explicit/stable, selected-spell tooltips now describe actual effects, and a96-target operation-bound test checks64candidate abort. Full build19JUnit/79required server tests pass. Added opt-in idle-station mode to the existing profiling harness, retaining its bounded loaded-only setup and analyzer rather than duplicating diagnostics. Full process restart and ordinary-server0/64/256station measurements follow; functional client JFR is not treated as a standalone benchmark.

### Research increment completed and profiled

On92aca0a, a new graphical client process positively verified saved personal knowledge and selected Purification; the selected tooltip reflected the real effect/cost. Actual saved region data retained2paper in the research table after normal Save/Quit. The native full Purification chain, progress journal/browser and four server-tested advanced effects form a complete first research tier, with four old basic spells preserved.

Ordinary-server0/64tables/64altars/256mixed idle-station profile passed all no-ticker/no-scheduled-work assertions and600measured JFR events/phase. At256:mean0.442ms/p997.645ms/max37.707ms,19.976TPS. The64altar phase had one61.575msoutlier; exact attribution, massive menu/casting/network/save workloads and retained heap remain unverified. Full hardware/settings, allocation/GC caveats and manual checklist are in docs/MAGIC_RESEARCH.md. Final build19JUnit,79server tests,141JSON/152bilingual keys passed. JAR excludes tests/diagnostics. Next content priorities:manual/wind engineering drives or exploration content; no new major system started before this checkpoint.

Native effect follow-up also passed for Frost Lance(controlled cow6health+Slowness), Magelight(Night Vision+Slow Falling) and Sanctuary(Fire Resistance+Absorption+shard consumption), using supplied test folios read normally and matching preselected test wands. Journal reached4/4. This supplements the full survival-style Purification chain; it is not a natural-resource playthrough or AI benchmark. All four new effects now have server and native client evidence.

## 2026-10-28 — independent steelworking and smithing cycle

Per current user assignments, OpenCode owns pear orchards and Antigravity owns performance/testing of existing systems. Implemented a separate smithing module in feat/reimagined-alpha:iron/carbon blend→vanilla blast-furnace steel, manual forge with seven saved slots, reusable hammer/bellows,46equipment recipes for14iron equipment/weapon types, keen/sturdy finishes and bounded deterministic quality from metal/Smith rank/equipment. Names/enchantments/foreign data and proportional wear survive; actual vanilla durability/attack components change. Re-forging/default-durability guards prevent recursive bonuses after vanilla grid-repair metadata stripping. Browser/UI/localized tooltips/standard repair are integrated; no orchard/mechanical/profiler/existing-test implementation changed, and no branch merged.

Full build23JUnit and88/88required server GameTests passed. Nine new smithing tests exercise the real steel/crafting/furnace cycle, component/wear/attributes/quality, costs/tool reuse, access/concurrency, vanilla repairs, persistence, drops, recipes/browser/reload and no-ticker/no-unloaded-access behavior. TerraFirmaCraft research/license/provenance and scope are in docs/SMITHING.md; no code/assets/dependency copied. Native client checks are next. Profiling remains Antigravity's scope, with no TPS/performance claim from build/tests.
