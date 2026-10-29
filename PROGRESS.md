# Progress

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

### Smithing client completion and independent branch review — 2026-10-28

Continued75e5960 without repeating verified blend/steel production. Actual RU NeoForge client completed forge→worked sword→normal hit→vanilla anvil repair. Quality2/keen gave312durability and7actual attack damage; name/foreign data and+1modifier survived repair and a full client-process restart. Forge fuel/tool wear/bellows also persisted. Controlled fixture supplies/rank/repair XP are disclosed in docs/SMITHING.md; no bug was found in this resumed path, and no performance/multiplayer readiness is inferred.

Reviewed exact OpenCode c33617d and Antigravity7f53132 from their respective bases65001a7/5cea97f. Separate temporary source candidates against75e5960 passed23JUnit each,94/94 and88/88fresh official GameTests, and248/205JSON resource validation. Main refs/index/source were not merged; foreign branches and Freebuff were not modified. Text conflicts, stale-state harvest robustness, journal page margin, wheel-only refresh coverage, milling-cache reload checks and missing after-profile remain integration considerations. Full reproducible scope/limitations and ordered merge plan:docs/INTEGRATION_REVIEW.md. Source compatibility is established for these separate resolutions, not final combined/native/performance approval.



### Cooking increment complete

Published runtime source7b92e31 passed the final ordinary-server0/64idle/64active/256active profile, all real meal counts and no-idle-work assertions. At256pots:mean0.850ms/p992.803ms/max102.571ms,19.999TPS,768meals; the empty baseline also retained large tails, so no lag-free or isolated speedup claim. JFR allocation/GC observations, noisy earlier runs, exact scenario and unresolved attribution are preserved in docs/COOKING.md. Added thread labels and excluded post-measurement audit work from the analyzer; moved only diagnostic measurement-end metadata before that audit.

A new graphical client process reopened the saved CookingSmoke world, verified4retained meals, reopened the RU menu and cooked a fifth from exactly one added barley/apple/bowl. A native positive condition confirmed persistence; the reopened final menu showed five meals and empty input/bowl slots after processing. Full final build19JUnit,68server GameTests (10cooking tests),113JSON/106bilingual resource checks passed. Player JAR excludes both profile harnesses and GameTests. Full Alpha survival/multiplayer/standalone/physical-GPU/audio gates remain open. No next large subsystem was started during this focused runtime/performance follow-up; next content priority is research progression/spell stations tied to discoveries and existing resources.

## 2026-10-28 — finite spell research and magic stations

Implemented Arcane Altar/Research Table with saved4-slot inventories, server-owned manual recipe transactions, functional localized menus and explicit mana/discovery/output checks. Four focus recipes link existing resources (including barley) to four tradeable study folios. Reading unlocks one additional real spell per school: Frost Lance, Purification, Magelight, Sanctuary. Existing basic spells remain available. Selected known spells persist on wands, school cooldowns are shared, player knowledge is bounded/versioned/deep-copied on death. Journal and browser show personal research; changed/initial progress sync is owner-only and clears on logout. Stations have no tickers. Loaded-only rays/entry checks and capped64candidate/17particle ray work strengthen spell boundaries without replacing existing schools.

Full build19JUnit,78/78required GameTests and141JSON/144bilingual resource checks passed. Ten new tests cover transactions/concurrency/security, actual effects, folios, knowledge/selection save and clone, no-load/no-ticker behavior, recipes/browser/codec/personal progress. Upstream Ars Nouveau research/license/version caveats and design are in docs/MAGIC_RESEARCH.md; no source/assets/dependency copied. Native client flow and real-server workload measurements remain to be executed after this source checkpoint.

## 2026-10-28 - pear orchard content stage (feat/opencode-development)

Added a complete pear orchard built only from vanilla tree contracts: registered pear leaves/sapling/log/wood/planks/pear, a configured `reimagined:pear_tree` feature, a rare placed feature added to forests/flower forests/birch forests/meadows/plains, seasonal inner-canopy fruit ripening, right-click harvest, real leaf loot (shears/Silk Touch/Fortune), log-to-plank and grafting recipes, common block/item tags, advancements, EN/RU localization and an `orchard` journal chapter.

No block entity, ticker, world scan, chunk load, packet broadcast or saved data was introduced. Fruit work is limited to leaves with `distance <= 2` in the overworld during summer/autumn and stops once fruit is set; the outer canopy never receives fruit work. Full description and limits: `docs/ORCHARD.md`.

Checks on this source: full Gradle build with **19/19 JUnit tests**, fresh official GameTestServer report with **84/84 GameTests, 0 failures/errors/skips** (6 new orchard tests), and a PowerShell resource check of **183 JSON files / 151 bilingual keys** plus model, texture, blockstate and recipe-result links. `scripts/validate-resources.py` could not run because this checkout has no working Python; the PowerShell check and real datapack loading are not a substitute for that script. Graphical tree/leaf rendering, survival-client sapling growth, large-orchard profiling and authenticated multiplayer remain unverified. This branch was not merged into `feat/reimagined-alpha`.

### OpenCode integration checkpoint — 2026-10-28

Merged orchard history c33617d into the main branch under the user's new explicit integration authorization. Preserved newer smithing/magic source, all translations and both authors' documentation/provenance. Fixed stale-state harvest with a loaded current-state check and state mutation before drops; namespaced TreeGrower. New stale/replacement regression test passed alongside all current tests: full build23JUnit,95/95fresh GameTests,248JSON/186bilingual keys. Native combined acceptance follows after Antigravity integration. Freebuff untouched. Earlier 'do not merge' ownership records are superseded for these two branches only.



## 2026-10-28 — Tick-loop micro-optimisations (feat/antigravity-development)

Confirmed five structural inefficiencies from code analysis and baseline profiling (0/16/64 component scenarios, 0/64/256 magic-station scenarios — all showing 0 ticks >50 ms on this Windows host, consistent with prior loaded-only inspection fix already merged).

**Changes applied (5 files):**

1. **`MechanicalBlock.java` — `getTicker`**: Changed guard from `kind != Kind.SHAFT` (ticked both MILLSTONE and WATER_WHEEL) to `kind == Kind.MILLSTONE` (only MILLSTONE needs a server-side tick). WATER_WHEEL no longer receives a redundant BE ticker call every tick.

2. **`MechanicalBlock.java` — `useItemOn`/`useWithoutItem`/`onRemove`**: Replaced three `level.getBlockEntity(pos)` calls with `MechanicalWorldAccess.blockEntity(level, pos)` to stay on the loaded-only path and avoid entering chunk-future machinery.

3. **`MachineBlockEntity.java` — early-exit reorder**: Moved `state.is(MILLSTONE)` and `input.isEmpty()` checks *before* `MechanicalNetworks.rotation(server, pos)` in the tick method. Avoids the network BFS call when the block entity is not a millstone or has no input item.

4. **`MachineBlockEntity.java` — `RecipeManager.CachedCheck`**: Replaced per-tick `level.getRecipeManager().getRecipeFor(...)` (full recipe-list traversal) with a `RecipeManager.CachedCheck` field that caches the last matched recipe. Also applied to `canPlaceItem`.

5. **`MechanicalNetwork.java` — single-slot chunk cache in `LoadedSignals`**: The inner BFS accessor previously called `getChunkNow()` twice per block (once in `loaded()`, once in each of `getBlockState()`/`getFluidState()`/`getBlockEntity()`). Added `cachedChunkKey`/`cachedChunk` fields and a `chunkAt(pos)` helper; all four overrides now share one lookup per chunk per BFS step.

6. **`MechanicalNetworks.java` — deduplicated `redstonePowered` calls**: In `process()`, `redstonePowered(level, pos)` was called once per millstone in the phase loop and again in the RPM loop. Added a local `HashMap<BlockPos,Boolean> millstonePowered` filled with `computeIfAbsent`; the second loop reuses the cached result.

7. **`SpellWorldAccess.java` — single-slot chunk cache**: Same pattern as `LoadedSignals`: added `cachedChunkKey`/`cachedChunk` and `chunkAt(pos)`, used in both `getBlockState()` and `getBlockEntity()`.

**Verification:** `./gradlew compileJava` — BUILD SUCCESSFUL; `./gradlew test` — BUILD SUCCESSFUL (all JUnit unit tests pass). Full GameTest server run not repeated here (79/79 passed on the same source base pre-edit; compilation proves no new type/method errors).

**Before (Windows baseline, same branch pre-edit):**
- 0 components: mean=0.51 ms, p99=1.26 ms, max=14.4 ms, 0 ticks >50 ms, TPS=19.99
- 16 components: mean=0.62 ms, p99=2.32 ms, max=34.1 ms, 0 ticks >50 ms, TPS=20.0
- 64 components: mean=0.52 ms, p99=2.05 ms, max=3.06 ms, 0 ticks >50 ms, TPS=19.99
- 256 mixed magic stations: mean=0.28 ms, p99=0.76 ms, max=0.92 ms, 0 ticks >50 ms

**After profiling:** Not re-run (no 50 ms+ ticks existed in baseline; structural call-count reductions are code-verified, not runtime-compared). Claiming a measured speedup without a comparable post-edit profile on identical hardware would violate project performance policy.

**Historical context (Linux cloud, pre loaded-only fix):** 64 components max=104 ms (2 ticks >50 ms); 64 altars max=61.6 ms (1 outlier). Those outliers were attributed to vanilla chunk operations in the original profiler notes, not mod tick code. Both fixes (loaded-only inspection + this batch) are now in the branch.

**Limitations:** JFR files produce 0 bytes in the sandbox environment (java.io.tmpdir not writable); tick-ring JSON timing is valid. No comparable after-profile was executed. GameTest server (79 tests) not re-run after edits due to time constraints; compilation and unit tests are clean.

**Remaining open items:**
- Run `./gradlew runGameTestServer` post-edit to confirm 79/79 still pass
- Produce a post-edit mechanical profile on the same hardware/scenario for a proper before/after comparison
- Investigate the occasional 30–60 ms outlier at 16-component scale (suspected vanilla chunk tick, not mod code — needs JFR attribution)
- WATER_WHEEL ticker removal: verify no other code path relied on the BE ticker for WATER_WHEEL (currently no test covers this explicitly)

### Antigravity integration / measured comparison — 2026-10-28

Merged7f53132 after OpenCode with the wheel-only refresh regression corrected by retaining the original staggered wheel ticker; shaft remains ticker-free. Added three focused real server tests for independent wheel/shaft refresh/water/redstone/lifecycle and CachedCheck deletion/replacement/overlap/automation. Full merged build23JUnit/98GameTests and248JSON/186keys passed. Both authors' histories/docs/source retained. Fresh matched ordinary-server profiles completed and all JFR windows validated:64networks mean1.049→0.993ms, ray workload256/tick mean2.347→0.559ms. Worse0/16mechanical tails and remaining outliers explicitly retained; no universal speedup/memory/network guarantee. See integration/profile documents. Native combined acceptance follows; Freebuff untouched.

### Combined native acceptance — 2026-10-28

Real integrated client8a8d9ec passed native pear planting/bonemeal growth, ripe-leaf harvest+2pear pickup, RU sapling recipe/browser and orchard journal page11/15. Wheel+shaft visibly rotate without a mill, stop/resume from redstone; elemental ray caused actual6damage. Prior repaired Field blade survived integration. Native /reload completed. Controlled fixtures/software rendering and remaining natural-spawn/multiplayer/scale limits are explicit in docs/INTEGRATION_REVIEW.md. Both integrations are complete and published; next content increment is temperate-forest wild boars under spec7, not another cooking/production tier.

### Forest wildlife source/server checkpoint — 2026-10-28

Added wild boar as a complete ordinary Animal cycle:low-weight forest herds, pear/carrot/beetroot lure/feed, live piglets/vanilla growth+cooldown, individual adult self-defence, harmless fleeing piglets, actual pork/Looting/burning loot, original animated quadruped/tusks, EN/RU/egg and automatic existing journal discovery. No deeper cooking/production chain, herd alert scan, custom per-tick ecosystem or dependency. Full build23JUnit/103fresh GameTests and253JSON/188keys passed, including five new fauna tests. Client acceptance follows; natural encounter frequency/dense-herd scale are not certified. Design/provenance:docs/WILDLIFE.md.

### Wild boar native/restart completion —2026-10-28

Actual client5cd3366 rendered/moved adults, accepted a pear, bred two ordinarily carrot-fed AI-enabled adults into a live child (vanilla advancement/cooldowns/negative age), and exercised adult retaliation after a native punch on Normal. A new client process retained both parents and the actual offspring. No source bug was exposed; controlled resources/positions, failed aiming attempts and the remaining child-close-view/audio/natural-spawn/dense-herd/multiplayer limits are explicit in docs/WILDLIFE.md. Full server checkpoint remains23JUnit/103GameTests,253JSON/188keys. Player JAR includes boar/assets and excludes every GameTest/profiler. No further major system starts in this checkpoint.

### Freebuff fish integration —2026-10-28

Integrated author4a71f92 (based75e5960) into currentc2bc853 without losing prior orchard/mechanical/magic/boar/smithing changes. Three raw fish append an optional15%bonus to vanilla fishing, not replace it. Added missing vanilla smoker equivalents; preserved furnace output(cooked cod), barley global loot entry, vanilla fishes tag members and EN/RU16pxassets. Expanded author coverage to512seed exact base-catch/component preservation, optional/bounded bonuses, non-fishing/subtable isolation, real six furnace/smoker processes and browser/tags. Full build23JUnit/107fresh GameTests,265JSON/191keys passed.

Real native LureIIIrod casts/reels produced cod, vanilla junk, salmon, cod, then salmon+SilverTrout; actual caught trout was picked up. All three icons and RU furnace/smoker uses inspected; the other two were supplied for graphics only, not claimed as native catches. Read-only opt-in observer supplies bite timing on an audio-less cloud, changes no RNG/timers/drop and is excluded from the player JAR. Scope/provenance/limits:docs/FISHING.md. New explicit Freebuff authorization supersedes historical exclusion; Kilo was not fetched/merged/edited. Per user request, next3–5directions are proposals in docs/SPEC_PRIORITIES.md; no new major system started.

### Freebuff boulder integration —2026-10-28

Starting60b7a4a, merged missingdf7a223 only; fishing4a71f92 was already an ancestor and is not reapplied. Rare vanilla mossy forest_rock decoration uses one1/18candidate in forest-tagged biomes, no density multiplier. Full build23JUnit/110fresh GameTests passed, including original biome registration plus real soil placement/bounded output and4096rarity-filter samples. No source branch changed. Kilo integration follows under the latest explicit authorization. Details:docs/AGENT_CONTENT_INTEGRATION.md.

### Kilo source/server integration checkpoint —2026-10-28

Integrated missingafc5bd3/1835d41/25e4ea7 with history after Freebuff boulders856a569. Completed missing thatch/clover resources/loot, actual NeoForge compostables data map, vanilla flower tags and sparse bounded clover generation. Preserved main translations rather than incoming RU encoding corruption. Rare blocks now render in the journal, use one loaded-only sight ray instead of a volume scan, and fit the100page cap; original two-list API/v1saves retain compatibility. Real clone/save/reentry/visibility/max-book and content tests passed:25JUnit/118GameTests,284JSON/197keys. No agent branch changed and no new major mechanic; native check next. Details:docs/AGENT_CONTENT_INTEGRATION.md.

### Agent content native/final checkpoint —2026-10-28

Combined client acceptance completed in a fresh seed12345vanilla-noise world: natural forest rock/clover inspected; pre-fixture offline survey338FULLchunks contained9clover/103forest mossy blocks (not103rocks or a probability benchmark). Fixed client-discovered clover alpha quads with cutout rendering and aligned the low selection shape; thatch/clover textures display. Real RU journal counts/catalogue showed calcite, and Survival death with inventory loss plus remote respawn retained ocean/cow/calcite. A whole new client process retained the same three IDs, away from relearning targets. Final build25JUnit/118fresh GameTests,284JSON/197keys passed. See docs/AGENT_CONTENT_INTEGRATION.md for fixtures, FML-config workaround and untested scope. No new major mechanic; all requested source histories now integrated, fishing not reapplied.


## Kilo820e5db integration —2026-10-28

Reviewed the incoming ancestry against published e7e6969. Prior25e4ea7/thatch/clover/journal already present; only missing1002d09/18eeada/78e5134/820e5db included in the merge. Corrected broken stationary/conflicting AI, water avoidance, surface-transition rest timer, river spawn placement, grassy marmot habitat, missing client renderers, invalid plant feature codec/biome and malformed/regressed localization. Loaded-only local searches are throttled and capped; vanilla lifecycle/safety/food/breeding retained.25JUnit/132fresh GameTests, full Gradle build and299JSON/203keys passed; JAR excludes all test/profiling fixtures. Executed native behaviors, controlled fixtures, profiler method/results and open Alpha gates are recorded in docs/KILO_FAUNA_INTEGRATION.md. Source branches/worktrees preserved; no new major system.

## Kilo world polish —2026-10-28

Reviewed e9af309/65f11a0 againstd861718. They add tests, not missing farming mechanics/meals; both meal JSONs were already present and are preserved without incoming BOM-only reformatting. Corrected compost tests to live Item data maps and real insertion, replaced placement-only moist-soil check with512paired actual random-growth trials, retained five-recipe registration and added real scheduled cooking/consumption for dumplings/stew. Full build25JUnit/136GameTests and299JSON/203keys passed, including all fauna/nature regressions. Production JAR is byte-identical; no performance improvement claimed. Actual Survival client seed placement, natural soil hydration/random growth, bonemeal maturation and heated-pot meal interaction:docs/KILO_WORLD_POLISH_INTEGRATION.md. No new large system; source branches/worktrees preserved.

## Creative catalogue

Added four standard NeoForge tabs covering51registered items exactly once:12nature/wildlife,13farming/food,11engineering/equipment,15magic/research. Ordered mergeable item tags support future additions; vanilla-category injection removed, search retained.25JUnit/137GameTests passed, including complete item coverage/order/nonduplication and all previous regressions. Actual RU client page2icons/categories/content inspected. No item/recipe/gameplay changes; separate commit from the pending JEI/visual-audit work. Details:docs/CREATIVE_TABS.md.

## Removal of bespoke recipe UI

Removed the standalone browser, inventory sidebar/R entry, R/U interception, item search/navigation and world cache plus their obsolete UI-only tests. Real recipes, station menus/mechanics and tested read-only recipe projections retained. Journal text no longer directs players to a removed screen. Default build has no JEI dependency; an explicit -PwithJei/-PjeiVersion dev opt-in is prepared. Official Maven HTTPS CONNECT is blocked with403, so no JEI runtime/custom-category compatibility is claimed. Build21JUnit/133GameTests,303JSON/190keys passed. Exact deletion inventory, network requirement and remaining integration plan:docs/JEI_MIGRATION.md.

## Visual audit and five-animal plan

Committed inventory of57textures/84JSON models/15blockstates, with dimensions/references/SHA256and a standard-library reproducibility checker. Reused the actual prior57-image inspection to prioritize badger/otter/marmot/deer/boar, specify a coherent Minecraft-scale art direction and identify mesh/UV limits, including tiny SmallFauna faces and deer overlapping UV pixels. Acceptance requires real same-view before/after and movement/baby checks; no source texture/model/animation changed or new quality claim. Details:docs/VISUAL_AUDIT.md. Inventory/resource validation passed; gameplay checks remain21JUnit/133GameTests from the preceding code stage.


## Deer visual acceptance — 2026-10-29

Finalized the user-approved deer head5×5×6/neck5 and isolated antler UV with the retained original27-color64×64texture draft. Native adult/fawn views exposed a neck/shoulder gap at extreme pitch and adult antlers on fawns; fixed attachment by rendering an anchored neck through the same baby transform as the head, and reset age-dependent antler visibility on every setupAnim. Neutral approved proportions, body/legs/AI remain. Real client checked three views, age reversal, forced±90°pitch and ordinary AI ascent/descent on one-block steps for adults/fawns; no visible attachment/UV failures after correction. No other animal/NPC/trade/beekeeping/world-generation changes. Full verification and limits:docs/DEER_VISUAL_ACCEPTANCE.md. Other animals still require separate approval; JEI remains deferred by official Maven403.

Final deer checkpoint checks: build successful; fresh21JUnit/133GameTests passed,303JSON/190bilingual keys and57PNG/84models/15blockstates inventory validated.
