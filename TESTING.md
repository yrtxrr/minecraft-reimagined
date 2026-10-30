# Testing

## Executed in the cloud — 2026-10-27

- JDK 21 compiler and Gradle 9.2.1 activation: passed. Host originally supplied only a JRE; checksum-verified JDK was installed under `/workspace/.tools`. Wrapper JAR/distribution integrity checked against official Gradle SHA-256.
- `bash scripts/verify-domain.sh`: **12 tests found, 12 started, 12 passed, 0 skipped**. Covers uncapped XP/level thresholds/finite mastery and invalid state, bounded mana/costs, seasonal boundaries/greenhouse crop rules, water-wheel capacity/overload and truncated mechanical graphs. Latest XML reports: ignored `build/domain-tests/results/`. These tests compile only pure domain classes, not Minecraft adapters.
- A real defect was found: SkillProgress.EMPTY initialized before its BigInteger constant caused ExceptionInInitializerError. Corrected constant order; complete domain suite rerun successfully.
- `python3 scripts/validate-resources.py`: **59 JSON resources**, model/texture cross-links, **66 RU/EN translation keys**, local density-function references/cycle checks and additive preset tag passed. This does not execute Mojang codecs or generate chunks.
- `bash -n scripts/*.sh`: passed.
- Official NeoForge 1.21.1 documentation/source signatures inspected for events, registries, payloads and block-entity serialization. Static review does not prove compilation.

## Continuation — actual Minecraft validation

NeoForge Maven now returns HTTP 200 through the existing authorized proxy. `./gradlew build` **passed**, including **12/12 JUnit tests, 0 skipped**. Corrected incompatible greenhouse glass API references using NeoForge common glass tags, and the non-effectively-final ray endpoint captured by a lambda. Removed deprecated client subscriber bus selection.

`./gradlew runGameTestServer` **passed: 17 required GameTests, 0 failed**. Current-run XML: `run-gametest/reports/gametest.xml` (ignored). The first run was 16/17: an isolated ProtoChunk fixture failed because it did not commit generation statuses. Fixed the fixture to follow normal BIOMES→NOISE→SURFACE lifecycle; reran all tests successfully.

Coverage: all 14 item / 3 block / deer / machine / milling registry entries; all 16 custom recipes through JSON and network codecs; deer biome modifier, actual breeding, entity NBT; water-powered transmission, shaft axis, overload, redstone, actual server-tick milling and block-entity NBT; calendar commands/SavedData; player skills save/death clone; nature-wand healing, mana, XP, rejected casts and actual cooldown expiry; preset codec/vanilla preservation, full biomes/noise/surface generation of two identical-seed isolated ocean chunks, block-for-block comparison, sealed bedrock and water/stone/air assertions.

GameTest code and the empty template are excluded from the player JAR. Test server processes exit on completion; no running process is part of the prepared environment. No graphical client or online player authentication has been tested. Vanilla Authlib explicitly uses a direct Proxy.NO_PROXY connection for its public-key fetch; it logs an unavailable authentication endpoint in this cloud environment. This does not fail the headless tests and is not evidence that real online login works. `api.minecraftservices.com` and `sessionserver.mojang.com` were added to the draft allowlist; applying settings is separate from this tested server run. TLS and authentication checks remain enabled.

```sh
source scripts/cloud-shell.sh
./gradlew build
./gradlew runGameTestServer
python3 scripts/validate-resources.py
```

CI is configured but has not run on GitHub in this session. The existing detailed manual checklist below remains required where automated tests do not cover client behavior, multiplayer or broader terrain sampling.

## Required Minecraft checks after compilation succeeds

1. Launch a dedicated development server; after personally accepting the server EULA, verify startup reaches normal readiness with no registry/data-pack errors. Use a fresh temporary world under ignored `run/`; never replace an existing player world.
2. Create separate fresh vanilla and `reimagined:continents` worlds. Check both load; compare terrain in two fresh continents worlds with the same seed; inspect ocean/land/mountain/cave/surface behavior. Static JSON checks cannot demonstrate deterministic generated terrain quality.
3. Join from a matching Minecraft 1.21.1/NeoForge client with this JAR. Check normal inventory screens and multiple GUI scales. Browser left-click shows recipes, right-click usages, R/U on hovered slots, ingredient navigation/history, station/result counts, `/reload` refresh. Special dynamic crafting and potion-brewing coverage remain incomplete.
4. Spawn deer using its egg and natural forest generation. Verify visible antlers/texture, walking, fleeing wolves, berries breeding and babies, drops. Save/restart and check animals persist.
5. Build flowing water beside a wheel perpendicular to its axis; attach aligned shafts and millstone. Verify wheat→flour in 8 processing seconds at speed, 3flour→2bread, cobblestone→gravel and gravel→sand. Test still-water no power, wrong shaft axis no power, redstone stop, third mill overload on one wheel, recovery with second wheel, unloading/reloading chunks, breaking blocks drops stored stacks, restart preserves partial progress/output. No hopper or animated rotation support is claimed.
6. Earn XP through mining/crops/crafting/smelting/trading/fishing/combat/spells. Inspect `/reimagined skills`, learn ranks only after earning points, verify max3 and bounded PvE boosts; death/rejoin/restart retains progression. No level cap, no endlessly scaling combat bonus.
7. Craft/use each wand. Verify elemental wall occlusion/PvP rules, nature healing/growth only on valid targets, arcane step not through walls/outside world/unloaded terrain, ritual resource consumption and ally-only buff. Confirm mana cost/regeneration, server-enforced cooldown and death/restart persistence. Journal opens real localized pages and personal discoveries; it is not a quest/research-unlock database.
8. Check `/reimagined calendar` and HUD, season command operator permission, season offset after restart, synchronized day after joining/dimension travel. Compare winter temperate crop growth with/without a glass roof. Time commands and sleep intentionally affect this world-time calendar.

Record actual outcomes before marking the alpha milestone complete. Do not publish an unverified build as a completed mod.

## Engineering automation checkpoint

Full Gradle build passed again (12 unit tests). `bash scripts/verify-integration.sh` executed **20/20 required GameTests, 0 failures, 0 skips**, and verified the fresh XML report against all declared tests. Three new runtime tests cover actual vanilla hoppers inserting two wheat and receiving two flour, invalid ingredient rejection; sided capability simulation, exact64 capacity/remainder and output/input restrictions; and saving/replacing a genuinely mid-process machine with both slots occupied before resuming at its saved progress. Water wheels expose no inventory/capability. Manual interaction remains supported.

The integration wrapper preserves Gradle's failure exit status, removes stale reports, and rejects missing/zero/incomplete/skipped runs. GitHub CI now runs it and archives reports/logs, but that remote workflow has not been observed in this session. Verified the built JAR excludes test classes and the empty test structure.

## Combat checkpoint

Full Gradle build passed (12 unit tests); fresh-report integration runner verified **27/27 required server GameTests, 0 failures, 0 skips**. Seven new combat tests cover actual special item use/damage and cooldown sharing, normal right-click/offhand shield compatibility, ordinary attack-charge rejection, walls and scoreboard allies, distinct hammer/dagger effects, halberd front arc, actual entity save/death clone cooldown persistence, and the real shield damage event path (timing, facing, shield wear, cooldown, and shield-bypassing magic).

A fixture initially failed compilation by referencing NeoForge's private FakePlayer transport class. Replaced it with a public normal ServerGamePacketListenerImpl and no-op packet transport; actual ServerPlayer ticking/hurt, item use and vanilla shield rules remain active. No private access or reflection is used. Graphical client controls/animation still require the manual checks.

## Research-journal checkpoint — final current run

`bash scripts/setup-cloud.sh` completed successfully (reusable bootstrap + full build), including **12 unit tests, 0 failures/errors/skips**. `bash scripts/verify-integration.sh` verified a fresh XML report with **32/32 required GameTests passed, 0 failures/skips**. Five new journal tests cover actual book use and held-slot-before-open packet order, registry-aware ItemStack network roundtrip preserving translatable pages, real observer event cadence/visibility and deduplication, player NBT/death clone/per-reader isolation, registry validation/discovery bounds/book size, and packaged short RU/EN guide translations.

The generated JAR contains the research module and excludes GameTest classes/fixtures; SHA256 is next to the JAR in ignored `build/libs/`. `python3 scripts/validate-resources.py` and shell syntax checks pass. No temporary results or binaries are committed. The book's actual graphical layout, client input, multiplayer and online authentication are still manual/unverified checks; do not call the alpha fully ready.

## Recipe-browser checkpoint

Full Gradle build passed: **16 unit tests, 0 failures/errors/skips**. Fresh `scripts/verify-integration.sh` report: **41/41 required server GameTests passed, 0 failures/skips**. Nine new browser integration tests exercise the actual shared UI model: crafting/furnace/smithing/milling ingredients/results/stations/uses, vanilla potion strengthening/duration and splash/lingering conversions, ingredient alternatives/counted outputs/simple brewing API, dynamic-adapter dispatch, recipe replacement and the real UI cache's reuse/invalidation/clear, real registry search/categories and potion variant IDs. Four new unit tests cover multi-term/Russian search, locale independence, page shrinking/overflow and bounded history restoration.

A stronger smithing assertion exposed missing vanilla smithing displays (empty getIngredients override). Corrected through the public recipe codec without private-field access; reran the complete harness successfully. Resource validation and build compile the client code, but no graphical Minecraft client was run. Actual GUI layout, focus, input and end-to-end network `/reload` remain manual checks in docs/RECIPE_BROWSER.md. Existing authentication/cloud limitation remains; no security checks were disabled. FCC barley sources/resources/branch were not changed.

## Mechanical animation/cache checkpoint

Full Gradle build passed with19/19 JUnit tests (three new rotation interpolation/phase/pause tests). Fresh server integration report:49/49 required GameTests, no failures/skips. Eight new tests cover shared-cache hits/common phase, bounded oversized graphs/frontiers/reads, aggregate FIFO budgets, removal/frozen phase, actual unloaded-position non-loading, indexed chunk-unload hook cleanup, silent identical motion updates/private payloads and saved angle/inventory compatibility. The indexed hook test is not a live client chunk-streaming test.

An ordinary Minecraft server completed the0/16/64-component JFR baseline/candidate scenarios, all mills produced flour. A second process reopened the candidate's saved world and verified exact input/output/progress/recipe restoration before ticking, then resumed consumption and common drive phase at64 mills over220 normal ticks:restart-result complete:true. Details, noisy long ticks and limitations: [MECHANICAL_PROFILE.md](docs/MECHANICAL_PROFILE.md). No graphical client was available; visible animation, resource baking/rendering, live chunk streaming and multiplayer still require client testing. This checkpoint does not certify playable-alpha readiness.

## Ordinary barley checkpoint

Full Gradle build19/19 JUnit tests passed; resource check93JSON/91bilingual keys passed. Fresh official server harness55/55 required GameTests passed, zero failures/skips. Six new agriculture tests run actual seed placement/consumption, bonemeal/maturity and farmland survival, eight age-specific loot rolls,256 ordinary grass/shears loot pairs,128 actual random ticks through the shared seasonal hook (plus glass shelter/compost checks), powered automated barley-to-flour processing, real bread/seed recipes and browser uses. This is headless gameplay verification; crop textures/rendering and survival-client interactions have not been visually tested. No new tick subsystem exists; large crop-field performance remains unprofiled separately.

## Dynamic shulker-color browser increment

Full Gradle build19/19 JUnit tests and fresh official server report58/58 required GameTests passed, zero failures/skips. Three dynamic-browser tests check every generated preview against real vanilla recipe matching/assembly, name/cargo/component preservation and non-mutation, strict concrete ingredients, incoming/outgoing usage counts and unique bounded IDs, all17 dye-input boxes, rejection of unrelated selections/arbitrary subclasses, and snapshot cache reuse/replacement/invalidation/logout. Resource validation93JSON/92bilingual keys passed. GUI layout, end-to-end client recipe reload and client performance remain unverified; no graphical client/display server was available in this cloud session.

## Graphical Alpha validation

A real NeoForge client now launches on Xvfb/Mesa llvmpipe and enters the saved test world. This exposed/fixed the recipe browser's double-blur rendering order and milling recipes' unsupported vanilla recipe-book category. Fixed Russian GUI/item/recipe rendering, actual drive animation, barley/deer visuals, journal pages and client-process save/reopen were observed. Full build19unit tests and58server GameTests passed after the fixes. See docs/ALPHA_VALIDATION.md for exact environment, evidence boundaries and remaining checks. A second graphical client launched but its local-server login was rejected because Minecraft authentication services were unreachable; multiplayer is not marked passed.

Final server correction validation:full Gradle build19JUnit tests and58/58 required GameTests passed after introducing getChunkNow/direct-chunk access and the primitive chunk index. The fresh ordinary-server0/16/64-component follow-up completed with every mill producing output and all scan budgets intact. Mean/p99 at64 were1.240/11.883ms; rare whole ticks still reached104.555ms. Full-stack attribution and remaining uncertainty are recorded in docs/MECHANICAL_PROFILE.md. No lag-free or large-factory certification is claimed.

## Cooking workstation increment

Full Gradle build19JUnit and fresh67/67required server GameTests passed, without failures/skips. Resource validation113JSON/106bilingual keys. Nine cooking tests cover real scheduled timing, matching/extras, heat/bowl/full-output pauses, NBT work resume, recipe hit/miss invalidation, capability and GUI slot rules, physical hopper/chest cooking through a heat conductor, real food consumption/bowl return, common browser displays and codec/network transport. The journal translation test includes the new cooking chapter and its200character bound.

Pilot ordinary-server profile completed0/64idle/64active/256active scenarios, verifying3meals per active pot and no idle scheduled work. It overlapped compilation/GameTest activity, so noisy timings do not establish performance. See docs/COOKING.md for repeatable quiet-profile setup and pending client/network/long-session limits. New graphics have not yet been directly observed at this checkpoint.

Cooking follow-up:19JUnit/68required GameTests pass after non-requesting entry/menu/comparator access. The added test verifies distant inspection leaves chunks unloaded, no BE ticker exists, and an empty pot has no scheduled update after settling. Actual graphical Russian menu/model/progress, meal shift-click/eating/bowl return and browser projection were observed. Pre-correction quiet profile results and limitations are retained in docs/COOKING.md; rare long ticks remain unresolved rather than certified acceptable.

Final cooking runtime:an ordinary-server profile on7b92e31completed all4phases/600measured ticks each,600custom JFR events/phase, all active pots producing3meals and idle pots with no scheduled work. At256active pots:mean0.850ms,p992.803ms,max102.571ms,19.999TPS; rare tails also exist in the empty baseline. Full allocation/GC observations, hardware/settings and attribution limits:docs/COOKING.md. These data do not establish an isolated improvement, huge-factory readiness or memory-leak absence.

A fresh actual graphical client process reopened CookingSmoke and positively verified4saved meals; final code cooked the fifth and consumed exactly the new ingredients/bowl. Menu/model/porridge/browser/eating observations were native client checks, not mocked screens. New client runtime did not close prior Alpha audio/physical-GPU/standalone/authenticated-multiplayer gaps. Final full build19JUnit,68/68required GameTests,113JSON/106bilingual keys pass; no failures/skips. Raw worlds/screenshots/logs/JFR remain ignored. Player JAR is in build/libs with SHA256 and excludes diagnostics/tests.

## Spell research source checkpoint

Full build19JUnit,78/78required server GameTests,141JSON/144bilingual keys passed. Ten new magic tests cover station transactions/prerequisites/costs/concurrency, remote/invalid/full/low-mana rejection, folio and personal knowledge save/death clone, selection persistence/unlock enforcement, four actual advanced spell effects/wall collision, no chunk loading/no BE ticker, recipe codecs/both browser stations, journal and progress payload/reset. Client interaction and real-runtime performance are not inferred from these checks; see docs/MAGIC_RESEARCH.md.

Magic follow-up:full build19JUnit and79/79required GameTests pass, including a96-target spatial-index fixture that must return exactly64candidates and shared-school cooldown enforcement after selection changes. Native client checked the full Verdant Focus→Purification Folio→learn→select→cast chain, exploration gates, journal and live browser progress. Controlled biome observations are not a natural-travel test. Process restart and idle-station profile are pending this checkpoint; software-rendered client trace alone does not establish TPS/large-battle readiness.

Final magic checkpoint:19JUnit/79required GameTests,141JSON/152bilingual keys passed. Native RU/GUI2Purification chain and journal/browser status were observed; a complete new client process confirmed saved knowledge and selection, and actual region NBT after normal save retained station inventory. The remaining advanced effects were additionally checked in the native client through supplied/read test folios and preselected test wands: positive Frost/health+slow, Magelight/effects and Sanctuary/effects+shard conditions passed; journal reached4/4. Their complete resource-acquisition/crafting chains remain unplayed. Ordinary-server0/64tables/64altars/256mixed closed stations passed600measured JFR events/phase and no-ticker/no-scheduled-work checks;256mean/p99/max0.442/7.645/37.707ms,19.976TPS. One64altar tick reached61.575ms. This tests idle stations, not huge battles/open-menu/client-network/save/memory-retention scale. Full settings, sampled allocation/GC limits and reproducible manual checklist:docs/MAGIC_RESEARCH.md.

## New smithing module correctness

Full build23JUnit/88required GameTests passed. Four new arithmetic tests include10000wear-fraction cases and overflow limits; nine new smithing GameTests cover actual blast-furnace steel, component/attribute/wear preservation, finite quality/bellows, resource/remote/concurrent actions, vanilla anvil/grid repair anti-stacking, saved inventories/items, drops, all46recipes/browser/codecs and recipe reload denial. Existing tests/profilers are not edited; running the required integration runner is focused integration verification, not Antigravity performance work. Source correctness is separate from client/performance readiness. See docs/SMITHING.md.

## Smithing native completion and branch compatibility (2026-10-28)

On75e5960, real client verified actual forge UI output (312durability, quality2/keen, retained name/custom data), a fully charged7damage hit, complete vanilla anvil repair preserving the+1modifier, and persistence after a full client process restart. Fuel1/hammer4wear/bellows retained in the saved forge. Previously checked blend/steel was not repeated. Controlled rank/resources/XP and software-rendering limits:docs/SMITHING.md.

Separate proposed source resolutions with OpenCode c33617d and Antigravity7f53132 passed Gradle build/23JUnit each and fresh94/94 versus88/88GameTests; resource validator248JSON/186keys versus205JSON/179keys. These ran in temporary archive copies, not merged branches. Actual combined candidate, orchard graphics/scale, wheel-only refresh/milling-cache reload additions and after-optimization profile remain open. See docs/INTEGRATION_REVIEW.md for conflicts, exact revisions, procedure and acceptance gates. Main source/JAR code is unchanged from75e5960.


## Pear orchard stage

Full Gradle build passed with **19/19 JUnit** tests, zero failures/errors/skips. Fresh `runGameTestServer` report: **84/84 required GameTests**, zero failures/errors/skips (`run-gametest/reports/gametest.xml`, ignored). Six new `OrchardGameTests` execute real block/item registration and common tags, real crafting recipes and browser projection, loading of `reimagined:pear_tree`/`reimagined:pear_trees`, bounded fruit rules (outer canopy excluded, ripe leaves stop ticking), summer ripening and winter rejection against the actual calendar, actual right-click harvest producing pear items, leaf loot with shears, and sapling growth through the configured tree feature.

Resource validation: **183 JSON files / 151 bilingual EN/RU keys** parsed and cross-checked by PowerShell (model parents, blockstate models, recipe results, textures). `python3 scripts/validate-resources.py` was not executed because no working Python interpreter is available in this checkout; the PowerShell check and real datapack loading in GameTestServer do not replace it. Graphical rendering of the tree/leaves, survival-client sapling growth, large-orchard profiling and authenticated multiplayer are not covered. No performance claim is made for large orchards.

Spell-access performance gate now has its own opt-in ordinary-server ray workload (0/256rays per tick), rather than reusing the unrelated idle-station profile. The old-access baseline ran600warmup/600measurement ticks per phase and both JFR recordings passed600event validation. Commands/scope/timing/sampling limitations:docs/SPELL_ACCESS_PROFILE.md. This is a diagnostic tool checkpoint; after comparison follows in the integration record.

Integrated OpenCode+Antigravity version passed full build23JUnit/98fresh GameTests and248JSON/186bilingual keys. Added stale harvest, wheel-only periodic revalidation without rotation reads, water/redstone/lifecycle-callback checks, and milling CachedCheck reload/overlap/directional insertion coverage. The uncorrected removed-wheel-ticker candidate failed the new periodic test; restoring the staggered ticker passed. True runtime before/after profiles and caveats are in docs/MECHANICAL_PROFILE.md and docs/SPELL_ACCESS_PROFILE.md. Native combined acceptance is recorded separately.

Native8a8d9ec acceptance: pear planting/bonemeal tree/ripe harvest+pickup, RU sapling browser recipe and journal chapter, wheel-only visible rotation/redstone stop/resume, elemental6damage hit, existing worked blade retention and real /reload. Scope/fixture/physical-unload and multiplayer limitations:docs/INTEGRATION_REVIEW.md. No repeated full Alpha playthrough was claimed.

Wildlife source checkpoint:23JUnit/103required GameTests passed,253JSON/188bilingual keys. Five boar tests execute actual biome/diet registration, live breeding/vanilla save state, hurt-driven adult AI with no herd/baby targeting, actual death loot/no baby loot and existing observer discovery. Source/client/performance limitations are separated in docs/WILDLIFE.md; native checks follow there.

Wildlife native checkpoint:adult renderer/movement, actual pear acceptance, two carrot-fed live parents→real offspring/advancement/cooldowns, actual adult retaliation on Normal and family survival across full client-process restart observed. Child-specific close-view/audio/natural-encounter/dense-herd checks remain open; fixtures and unsuccessful camera attempts are disclosed in docs/WILDLIFE.md. Existing pear world state and wheel-only restart also passed. No new source change after103passing server tests.

## Freebuff integrated fishing acceptance

Full Gradle build23JUnit and fresh107/107official GameTests passed with no failures/skips;265JSON/191bilingual keys validated. Fishing coverage retains the author tests and adds exact raw-control preservation for512seeded root rolls, all four vanilla fish and three bonuses, at-most-one optional addition, no chest leakage/subtable recursion, real fueled furnace/smoker processing for all three inputs, vanilla/additive tag and shared browser projections.

Actual client fishing used an ordinary supplied LureIIIvanilla rod and native mouse inputs, with a read-only bite observer excluded from the player JAR. Fifth successful catch was salmon+SilverTrout; prior four included vanilla fish and junk, and trout inventory pickup passed. All fish icons/RU browser furnace+smoker uses were inspected. Other two species' native catches, manual six-case client cooking, physical GPU/audio/authenticated multiplayer/natural-world journey were not verified. No loot command or changed bite/drop RNG was used. Details and diagnostic scope:docs/FISHING.md.

Freebuff boulders:full build23JUnit/110fresh GameTests, retaining original feature/forest/birch checks and adding actual forest_rock soil placement, bounded output, desert exclusion and4096sparse rarity-filter samples with at most one candidate. Natural chunk survey/client acceptance and performance are separate; see docs/AGENT_CONTENT_INTEGRATION.md.

Kilo integration corrected source:25JUnit/118fresh required GameTests passed;284JSON/197bilingual keys. Two page-budget JUnit tests plus new content/journal tests exercise actual crafting/loot/composting (including existing crops/pears), clover soil/tags/bounded feature, wall occlusion, deep death/nondeath clone, old v1saves/reentry and actual256block written-book codec. Author tests and all previous checks retained. Native/decorated-chunk survey remain separate from these checks; see docs/AGENT_CONTENT_INTEGRATION.md.

Final agent-content version:full build25JUnit/118fresh GameTests and284JSON/197keys passed. Real client found natural clover/forest rock; a pre-fixture offline338FULLchunk survey found9clover and103forest mossy blocks. Native inspection found/fixed clover's missing cutout layer, then verified transparency, thatch appearance, RU three-count introduction and rare-block catalogue. Actual Survival death/remote respawn (keepInventory=false, auto-respawn UI) and a new client process preserved ocean/cow/calcite. No direct discovery-NBT fixture, no relearning near respawn. Native limits and failed early FML-config startup/workaround:docs/AGENT_CONTENT_INTEGRATION.md. Automatic crafting/loot/compost/codec coverage is distinguished from client visuals/persistence there.


## Kilo fauna/nature integration

On the combined candidate, full `./gradlew build` passed25JUnit; a fresh isolated `bash scripts/verify-integration.sh --console=plain` report passed132/132required GameTests with no failures/errors/skips.299JSON/203bilingual keys validated.14new fauna/nature tests execute behavior/position/head-angle/state transitions, indexed player avoidance versus food, night/safety preemption, registered surface spawn rules, vanilla persistent state, actual two-half grass/fern placement, biome scope/rarity and complete Overworld feature ordering (no biome-modifier cycle). They replace the invalid incoming occupancy/coordinate tests. Earlier failed/interfering attempts are not acceptance. Native/profiling fixtures and exact remaining limits:docs/KILO_FAUNA_INTEGRATION.md. JAR was inspected to exclude GameTests/profilers/empty template.

## Kilo world polish

Full build25/25JUnit and fresh136/136official required GameTests passed with no failures/errors/skips;299JSON/203keys validated. Four new checks cover live compost data-map probabilities plus actual insertion,512same-seed dry/wet growth trials, all five cooking recipe IDs and actual scheduled16second cooking of both reviewed meals with exact batch/bowl consumption. Existing132tests include fauna and vegetation regressions. Recipes already existed and retain canonical bytes; production JAR remains identical tod861718. Native actual planting, default random growth/hydration, bonemeal and GUI cooking are distinguished from automated/statistical tests in docs/KILO_WORLD_POLISH_INTEGRATION.md.

Creative catalogue:full build25JUnit/137GameTests passed; new live registry/tab rebuild checks every item, icon, category/order and no vanilla-category duplicates. RU graphical client inspected four custom tabs on the standard second page. No full survival revalidation or third-party creative-tab compatibility certification claimed; gameplay/recipes are unchanged. See docs/CREATIVE_TABS.md.

Bespoke recipe UI removal:clean build21JUnit/133required GameTests passed, no failures/skips;303JSON/190keys validated. The4JUnit and4GameTests removed covered only deleted search/history/pagination/cache code; real recipe and gameplay coverage remains. A JEI runtime run is blocked by proxy CONNECT403to the official Maven; default no-JEI build does not resolve it. Native no-JEI checks are recorded in docs/JEI_MIGRATION.md.

Visual plan only:python3 scripts/inventory-visual-resources.py --check and scripts/validate-resources.py passed. All57PNG and all JSON models/blockstates match cb4cbc2byte-for-byte; no texture replacement/new-render acceptance claimed. No repeated full gameplay run for this documentation/inventory stage; previous21JUnit/133GameTests remain the code acceptance.


## Deer visual acceptance — 2026-10-29

Minecraft1.21.1/NeoForge21.1.264 native client, disposable DeerTextureReview, Xvfb/Mesa software rendering, no JEI or resource-pack override. Adult/fawn front/side/back, ±90°forced pitch, age reversal and AI-enabled ascent/descent on one-block steps inspected after the neck attachment fix. Static NoAI stress poses are distinguished from ordinary TemptGoal motion. Head/neck/ears remain connected; fawn antlers hide and adult antlers return. Original approved box dimensions retained. Exact fixtures/results/limits:docs/DEER_VISUAL_ACCEPTANCE.md. Screenshots/worlds/logs remain out of Git.

Recheck with `source scripts/cloud-shell.sh`, `./gradlew build`, `./gradlew test --rerun`, `bash scripts/verify-integration.sh`, `python3 scripts/validate-resources.py` and `python3 scripts/inventory-visual-resources.py --check`. JUnit/GameTests validate the existing gameplay regressions, not pixel quality; only the native checks support the visual findings. No measured performance or physical-GPU/multiplayer claim.

Final deer checkpoint checks: build successful; fresh21JUnit/133GameTests passed,303JSON/190bilingual keys and57PNG/84models/15blockstates inventory validated.

## Beekeeping checkpoint - 2026-10-29 (feat/opencode-development)

Source: vanilla-hive beekeeping with a persisted frame attachment, bee smoker, wax milling, wax block, two honey meals, advancements, EN/RU text and an original journal chapter. Details and limits: `docs/BEEKEEPING.md`.

Executed on this Windows checkout with the pinned JDK 21 (`JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot`):

- `.\gradlew.bat build` - **BUILD SUCCESSFUL**, **19/19 JUnit tests**, 0 failures/errors/skips (`build/test-results/test/*.xml`).
- `.\gradlew.bat runGameTestServer` - fresh `run-gametest/reports/gametest.xml`: **92/92 declared and executed GameTests, 0 failures/errors/skips** (8 new beekeeping tests).
- `python scripts/validate-resources.py` with `PYTHONUTF8=1` - **205 JSON files / 164 bilingual keys**, model/texture/blockstate/recipe-result links and acyclic density references valid. Without `PYTHONUTF8` the default Windows cp1251 decoder rejects UTF-8 resources; that is a shell-environment issue, not a resource defect.

`bash` is not installed in this checkout, so `bash scripts/verify-integration.sh` could not run. Its report check was reproduced directly in Python against the fresh XML: expected tests are counted from `@GameTest(` declarations, and the script fails on any missing, extra, failed or skipped test. This is equivalent report validation but is not the same command.

New tests: `BeekeepingGameTests` covers registration/hoe tag/attachment/all recipes; the three-frame cap and hive NBT persistence; real interaction installation, full-hive and non-hive rejection, and frame recovery on the break event; a smoked full-hive harvest (bottle, comb per frame, colony retained, angry bees calmed, durability); unripe and frameless hives; air use with cooldown; real pot cooking of both meals with glass-bottle/bucket remainders, bowl consumption and eating; and browser projection, `ItemStack` network codec and packaged EN/RU keys.

Not covered: rendered textures/tooltips/smoke particles, survival-client harvesting, large-apiary profiling/TPS, explosion frame loss, and authenticated multiplayer. No performance-readiness claim is made.


## Agent integration candidate — 2026-10-29 (main publication blocked)

Freebuff c6a0cb9 is history-preserving merge1a82150: build,21freshJUnit and133GameTests passed. OpenCode090a2cc beekeeping is merged into the local candidate with creative-category,14-page journal budget, reloadable wax composting and bounded/cooldown smoker corrections. Combined build/21JUnit/143GameTests and324JSON/203keys/63PNG inventory passed; all133base tests plus8author tests retained,2integration tests added. Native installation/harvest, Survival durability, creative food icons and /reload checked; a second client process retained3frames, harvested hive state and smoker damage1.

Kilo23a515f was **not merged**: missing renderer, disconnected client/server trade UI, unprotected output/incorrect quantities and unbounded/ticket-based NPC work are critical blockers. Do not present all-three integration or Alpha as complete. origin/feat/reimagined-alpha stays0c30793; verified partial work is saved only on integration/agents-review-0c30793. Details, source evidence, limits and next gates:docs/AGENT_INTEGRATION_REVIEW.md.

## Settlement audit remediation — 2026-10-29

Only feat/kilo-settlements-economy: no merge/Codex branch changes. Preserved professions/prices/old save fields; vanilla merchant packets and result slots now own server trades, with full-result capacity checks. Per-UUID stock persists and lazily restocks once per work day. Renderer/model/reference vanilla texture and nonduplicated spawn egg are registered. Navigation checks the full vanilla path region through getChunkNow, throttles40ticks and never uses tickets as FULL proof; no global per-tick SavedData scan.

Final build/25freshJUnit/134GameTests passed,286JSON/200bilingual keys validated;128original tests retained and6added. Native Survival64sticks→2emeralds, model rendering and actual client-process reentry/persisted uses=[2,0,0] verified. Real integrated-server600sample load:1NPC mean1.863/p99=9.627ms;33NPC mean3.504/p99=38.438/max85.935ms. This is not a lag-free or before/after optimization claim; a separate long stall outside samples remains unexplained. Exact scopes, procedure, JFR limitations and remaining integration gates:docs/SETTLEMENT_REMEDIATION.md.


## Final combined integration candidate — 2026-10-29

On integration/final-agents-settlements-c31544c: history-preserving merge04ac191 joins d8af955 and verified c31544c, not29ba004. Own tabs/nature tag, recipes, EN/RU, module registrations and all introduced tests retained. Native joint-world testing exposed smoker air-use falling through after trader interaction;22a4f48 consumes the client action without client-side inventory authority and adds a real joint hive/trade regression. Final build/21freshJUnit/160GameTests and325JSON/206keys passed. Native bees/hive/NPC/paid trade/creative egg/configured boulder and full process restart checked;33NPCs,3frames and stockuses4 persisted. Core deer visuals unchanged.

Published candidate only; no main/source-branch update. Repeat32NPC profile had mean2.184ms/max115.093ms and baseline max382.014ms; long pauses remain unexplained. Functional gates pass but unconditional main promotion/Alpha completion is not recommended before a controlled dedicated-server profile. Full procedure, attribution/history and exact limitations:docs/FINAL_AGENT_INTEGRATION.md.

## Modpack foundation —2026-10-30

Executed ./gradlew build:24JUnit; scripts/verify-integration.sh:164/164GameTests;325JSON/207bilingual keys; unchanged visual inventory64PNG/92models/16blockstates. New checks cover absent/disabled lazy adapters, two independent server-player ledgers, nonlinear eras, NBT/clone, finite progress budgets/future schemas and transition-only events.5Python assembler tests cover refusing incomplete full profile, checksum tampering, unsafe paths, dependencies and wrong-loader/unverified metadata. Existing tests retained. Core-only archives contain no worlds, secrets, third-party JAR or GameTest/profile classes. Runtime LAN/full-client/save evidence is a separate gate; mocked ServerPlayers do not prove a network connection.

### Core-only native acceptance

Actual client18baf09 loads preserved joint world, Russian `/reimagined_core status` and common config; NPC/bees render and vanilla trading GUI opens. Save→full process exit→new process→same world succeeds. Inventory retains3honey bottles,6honeycombs,4emeralds and smoker damage3; hive retains3frames and normal live colony. This is an existing-world compatibility test, not fresh survival world generation or full modpack acceptance. A second graphical AlphaObserver client attempted LAN25578 but authentication servers were unreachable; no authentication workaround applied. Two-client gameplay/FTB team quests/network persistence remain unverified. Detailed short debug result/limits in docs/PERFORMANCE_PLAN.md. Evidence outsideGit: /workspace/modpack-core-review and /tmp/reimagined-modpack-*.log.

## Trade fixture ownership correction —2026-10-30

A repeat full run during installer work exposed one failure in stockIsPerUuidPersistsAndFrozenTimeDoesNotRestock: the fixture inherited a spent legacy catalogue template from other catalogue tests. It now owns an explicit unused TradeData catalogue and asserts initial zero uses. All existing frozen-time/per-UUID/save-load/day-boundary/actual-resident assertions remain. No production trading code changed and no tests removed. Subsequent build succeeds and full GameTestServer passes164/164; existing24JUnit unchanged. Initial failed run is not counted as a pass.

## Installer stage acceptance —2026-10-30

39new stdlib Python tests cover official HTTPS/redirect/key handling, exact identities/loaders/releases, required/pinned/optional/incompatible/cyclic dependencies, all40target coverage/End alternatives, side separation, SHA/size/cache corruption/unsafe filenames, CF author-disallowed/null-URL cases, duplicate Java mod IDs, no overwrites/no0-mod Core-only output and mrpack/actual lock generation.5previous pack-assembler tests retained. Real cloud resolver visits all40candidates, writes0-artifact403report and refuses installation rather than manufacturing a full profile. Fixture JARs are synthetic and temp-only, not working Minecraft mods.

After isolated trade-fixture correction: Gradle build succeeds and all164GameTests pass; existing24JUnit present,325JSON/207bilingual keys and unchanged64PNG/92models/16blockstates validate. Unmodified Kilo QuestIntegration.java compiles in /tmp against currentNeoForge, not integrated. No full-pack client run possible without files; no new save/performance/two-player claims. Python backend tested onLinux; PowerShell wrapper/actual Windows online download not executed here (pwsh unavailable). Prior Core-only native results remain historical, not full-pack proof.
## World pack and settlement prototype — 2026-10-30

- `./gradlew build`: passed, **32/32 JUnit tests** (9 new deterministic economy tests covering season/era scaling, growth and housing gates, starvation floors, era thresholds and store caps).
- `bash scripts/verify-integration.sh`: fresh official GameTestServer report **99/99 required GameTests, 0 failures, 0 skips**. The six new settlement tests exercise: a distant settlement advancing and saving while `getChunkNow` for its chunk stays null; the pass advancing at most 16 of 20 settlements; founding spacing and command wiring; NBT round-trip; one-time quest signals not repeating after reload; and autonomous era advancement with exactly one signal per era.
- The era test first failed for a real reason: transitions from several catch-up hours were OR-ed into one bitmask before posting, collapsing two advances into one signal. Each simulated hour now posts its own signals; the assertion was not weakened.
- Windows-only Gradle test-worker failure (`GradleWorkerMain` not found) was traced to `GRADLE_USER_HOME` pointing at a path under a Cyrillic user directory; the worker argfile's classpath then cannot be read. Running with a repo-local ASCII cache (`GRADLE_USER_HOME="$(pwd -W)/build/gradle-home"`) fixed it without disabling tests.
- `python3 scripts/validate-resources.py` and `bash -n scripts/*.sh`: passed.
- `python scripts/validate-modpack.py`: **25/25 pinned jars present and SHA-512 verified, 12 non-loader required dependencies all resolved, 0 problems**. The first manifest was missing Apollo's Library and Moonlight Lib, which Tectonic/Lithostitched and Immersive Weathering require; without them the pack could not load. This is offline metadata/file evidence and does not evaluate Maven version ranges.
- All 25 pack jars were fetched from the official Modrinth CDN with the repository downloader; files stay in ignored `pack/mods/`.

Not executed: launching the actual modpack (a normal world/server needs the user's own EULA acceptance, and this headless GameTest harness is not the pack), any graphical client check of the terrain or deer in Terralith biomes, and any TPS/MSPT/JFR profile of the settlement pass. GameTest wall time is not a gameplay benchmark. Terrain quality, structure overlap, spawn balance, Nether/End generation and chunk-generation spikes remain manual checklist items in [MODPACK.md](docs/MODPACK.md).

## Modpack load check — 2026-10-30 (real NeoForge server process)

All 25 pinned jars were copied into the ignored `run-gametest/mods/` and a real NeoForge 21.1.264 server (GameTestServer, no EULA required or accepted) was launched. The mod list printed by FML contained every pack mod: Terralith 2.6.2, Tectonic 3.0.28, Lithostitched 1.8.1, Apollib 1.2.0, Naturalist 2.0.7, Serene Seasons 10.1.0.9, GlitchCore 2.1.0.2, Immersive Weathering 1.0.4, Moonlight 3.7.2, Farmer's Delight 1.3.4, Incendium 5.4.3, Nullscape 1.2.14, YUNG's API/Better Dungeons/Mineshafts/Strongholds, Better Archeology, Architectury, Resourceful Config, Cataclysm 3.33, Curios, Lionfish API, MCA Reborn, Guard Villagers, Cold Sweat 2.4.4, plus Reimagined. Jar-in-jar added CodecUI 1.4.7 and Sable Companion 1.4.2. There were no dependency, mixin-apply or registry crashes; only the normal development-environment "refmap could not be read" warnings.

**Cause of the four failures, found by bisection:** the three melee tests spawned vanilla `Villager` targets, and **MCA Reborn** replaces villager damage handling, so player specials never reduced their health. Removing MCA alone made all three pass; Guard Villagers, Serene Seasons, Cataclysm, Curios, Lionfish, Naturalist, Immersive Weathering, the YUNG suite and the worldgen mods were each ruled out. The seasonal test asserted the Reimagined winter penalty while the hook deliberately defers to Serene Seasons when it is installed.

**Fix:** combat fixtures now target an ordinary hostile (`Zombie`, no AI) instead of a villager, with unchanged assertions for arc, damage, stagger, knockback and cooldown; the seasonal test now asserts that Reimagined applies no second penalty when Serene Seasons is loaded and still verifies the real winter reduction otherwise.

**Result with the full 25-mod pack: 99/99 required GameTests passed, 0 failures, 0 skips.**

Not verified: an actual world. A normal dedicated server or client requires the user to accept the Minecraft EULA; this environment does not accept it on the user's behalf. Therefore Terralith+Tectonic terrain, structure overlap, Incendium/Nullscape dimension generation and chunk-generation cost remain unmeasured.

## Freebuff merged checkpoint

Latest59326be combat/season corrections preserved. Joint no-external build and173/173GameTests pass, including single-owner NPC/civilization save+dirty/migration test. Windows backend now42tests, with25pin closure/all40coverage/offline-plan/optional+End checks. Freebuff99/99external results retained as branch evidence; exact current CDN probe403 prevents independently repeating external load here. Do not conflate the two harnesses. All prior tests retained.

### Freebuff final c20c6a6 migration checkpoint

Updated final handoff/merge plan/fixtures. Version2 now writes Settlers+Aggregates, one SavedData key, first-wins duplicates and no forced write on load. Content-addressed verified .dat/.dat_old backups before legacy migration; failed backup refuses migration. Build,36JUnit and178GameTests pass;42installer tests retained. Native old-world load/backup/NPC+inventory/founding observed, X11BadDrawable on shutdown prevents clean-restart claim. See docs/MODPACK_AGENT_INTEGRATION.md; full external runtime still uses Freebuff99test evidence pending local reproduction.

## Solo native quest implementation acceptance

Build passes with40JUnit,185/185GameTests,0failures/skips. All original current tests retained; four previously existing navigation unit tests/helpers from a9c7423 were reviewed and retained without restoring browser UI. Seven quest GameTests check actual criterion state, failed/paid MerchantMenu transaction, food vs smelting, native sleep/journal use, canceled/incorrect-tool ore events, two ServerPlayer clone/actor isolation, shared signals persistence/replay and native config/real icons/advancement references. Native source schema13/8quests/25IDs/bilingual graph verified independently. These server players are not2network clients and FTB binary/GUI/reward claims have not run.42installer tests and5legacy assembler tests pass;25known pins validate structurally, actual bytes here still blocked.
