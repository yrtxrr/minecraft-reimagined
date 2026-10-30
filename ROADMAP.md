# Roadmap

The complete Russian specification is preserved in `docs/USER_SPEC_RU.md`. This file separates implemented source from validated release milestones.

## 0.1.0-alpha gate — in progress

- [x] NeoForge 1.21.1 project, modular entry point, pinned/checksummed Gradle wrapper, Java 21.
- [x] Source modules for optional world preset, new deer, water wheel/shaft/millstone, ten skill trees, independent recipe browser.
- [x] Server state and calendar payload, custom reloadable milling recipes.
- [x] Additional source: seasonal crop growth, four wand schools, four weapon profiles.
- [x] Executed independent tests for skill arithmetic, mana, seasons and load calculations.
- [x] Restore NeoForge Maven access in the actual cloud instance.
- [x] Compile all common/client Minecraft APIs; correct integration errors.
- [x] Complete full Gradle build/test and headless GameTestServer startup/execution.
- [x] Validate preset registry loading and actual same-seed chunk generation.
- [x] In-game powered milling, actual server/client chunk unload/reload, client-process saving/restart, native barley planting/harvest and open-browser recipe reload.
- [ ] Complete natural deer spawning/breeding and full survival/multiplayer playthrough validation. Deer rendering and scripted spawn are checked in the graphical client; breeding retains server-test coverage.
- [x] Produce a development JAR with server integration verification.
- [x] Launch the graphical NeoForge client; inspect world models, RU browser/journal and actual recipe reload; fix client-discovered issues.
- [ ] Complete survival/physical-GPU/standalone-JAR validation and authenticated multiplayer before calling the playable alpha ready. Second client reached the local server but authentication services were unavailable.

## Subsequent playable increments

1. Recipe presentation:bespoke inventory UI/search/history removed. Complete optional JEI custom-process categories, reload/lifecycle checks and native client acceptance after official artifact access is available. Vanilla recipe book/station menus and real recipe/projection logic remain. See docs/JEI_MIGRATION.md.
2. Engineering: animated wheels and bounded shared networks implemented; extend verified millstone hopper/capability automation to future machines, manual/wind drives, gear ratios, saw/press/crusher, pumps/lifts, redstone control refinement.
3. World: true watershed rivers/deltas, climate regions and regional geology/deposits, resource geography, ocean currents/archipelagos, settlement placement. Current noise preset is not a watershed simulation.
4. RPG: richer finite skill trees and UI, refine verified parry/special attacks and add shield/armor variants, extend the verified readable/discovery journal with research progression, further spell tiers/stations beyond the implemented four-spell research tier (altar/table, discovery gates, personal knowledge, journal/browser and focused client/server checks complete), first deterministic smithing cycle implemented (steel, two finishes, three bounded qualities, hammer/bellows and standard repairs); further alloys/stations/equipment tiers remain.
5. Agriculture: ordinary barley with seasonal hooks, harvest and milling implemented; pear orchards now integrated; fertility/fertilizers/greenhouses and husbandry remain. Heated cooking pot, five connected meals, browser/menu and hopper automation implemented; no further cooking stages planned for this priority; next content favors world variety/exploration rather than deeper production chains.

4. RPG: richer finite skill trees and UI, refine verified parry/special attacks and add shield/armor variants, extend the verified readable/discovery journal with research progression, further spell tiers/stations beyond the implemented four-spell research tier, deterministic material/skill/equipment smithing.
5. Agriculture: ordinary barley with seasonal hooks, harvest and milling implemented; pear orchard and beekeeping (vanilla hives, frames, wax, honey meals) implemented; further crops, fertility/fertilizers/greenhouses and other husbandry remain open. Heated cooking pot, seven connected meals, browser/menu and hopper automation implemented; expand cooking variety after focused runtime validation, without mandatory thirst/disease/micronutrients.
6. Content/civilizations: cultures, NPC states/cities, reputation/citizenship, diplomacy/trade/armies, persistent unloaded-region simulation. Player cannot found a state. Protect player terrain from NPC wars.
7. Exploration: original mobs/animations, room-connected dungeons/puzzles/mini-bosses, Nether/End biomes and independent bosses/artifacts, retaining dragon and wither.
8. Transport/economy: carts/caravans/sailing/ships, linked minecarts/switches/loading stations, roads/geography/economy integration.

Each increment requires a passing build, relevant automated tests, a meaningful game/server check and a Git checkpoint. No milestone is satisfied by documentation alone.

## Current ownership

Ready Freebuff/Kilo content is integrated through Kilo820e5db, with prior thatch/clover/journal changes preserved rather than repeated. Agent branches and existing worktrees remain unchanged. Current authorization covers only integration repairs and focused validation, no next major system. Fauna/plant details and acceptance limits:docs/KILO_FAUNA_INTEGRATION.md; prior content:docs/AGENT_CONTENT_INTEGRATION.md. Full Alpha gates remain open.

## Historical acceptance before orchard integration (2026-10-28)

Smithing's focused native forge→hit→anvil repair→full process restart path now passed; already verified blend/steel was not repeated. See docs/SMITHING.md for controlled-fixture limits. No new major system started. Independent OpenCode and Antigravity source candidates passed build/23JUnit and94/88GameTests respectively; neither branch is merged. Integration remains conditional on the source-review follow-ups and runtime/performance gates in docs/INTEGRATION_REVIEW.md. Freebuff is outside this review. Full Alpha and full specification remain incomplete.


## Pear orchard increment

Full Gradle build **19/19 JUnit** tests and a fresh official server report of **84/84 required GameTests** passed, zero failures/errors/skips. Resource check: **183 JSON files / 151 bilingual keys**, with model/texture/blockstate/recipe-result links valid. Six orchard tests cover real registration/tags/recipes and browser projection, loaded configured/placed features, bounded inner-canopy random ticking, summer ripening versus winter rejection through the real calendar, actual interaction harvest and pear entities, real leaf loot with shears, and sapling growth through the actual configured tree feature.

Agriculture roadmap item 5 now has ordinary barley plus pear fruit trees; fertility/fertilizers/greenhouses and husbandry remain open. Headless checks do not cover graphical tree/leaf rendering, survival-client sapling growth, large-orchard profiling or authenticated multiplayer. Source lives on `feat/opencode-development` and is not merged into `feat/reimagined-alpha`. Details: `docs/ORCHARD.md`.

## Integrated content checkpoint

OpenCode147c92e and Antigravity8a8d9ec histories are integrated and published, with98GameTests/23JUnit and focused native acceptance recorded in docs/INTEGRATION_REVIEW.md. Performance comparisons are measured and qualified, not a universal speedup promise. Next playable increment:forest wildlife/wild boar with ordinary breeding, defensive behavior, loot and original renderer; avoid deeper cooking/smithing chains. Freebuff remains outside the integration.

## Forest wildlife increment

Wild boar source/server cycle now includes biome-tagged low-weight herds, ordinary pear/root-vegetable feeding/breeding, piglets, individual defence and useful vanilla loot, original renderer/localization and existing journal integration.23JUnit/103GameTests passed; native adult/breeding/retaliation and family restart checks completed with limits in docs/WILDLIFE.md. Next broader content priorities remain exploration/dungeons, ecology and regional world variety; no extra cooking/production tier was added. This is an incremental contribution to spec7, not completion of all creatures/biomes or the full Alpha.

## Freebuff fishing checkpoint

Three edible fish items extend ordinary vanilla fishing through one optional additive loot roll. Existing catch/treasure components and barley loot entry are preserved; furnace/smoker recipes, fishes tags,16pxmodels/textures, EN/RU and browser uses are checked. Full build23JUnit/107GameTests and focused real-client fishing verified (native salmon+SilverTrout, other two appearances confirmed server-side). These are item fish, not new swimming animal entities or regional habitats. See docs/FISHING.md.

Requested remaining-spec review and five proposed priorities:docs/SPEC_PRIORITIES.md. Existing cooking/smithing/engineering systems are not expanded into more complex chains here. No Kilo merge and no next large mechanic started.

## Completed remaining-agent integration

Rare forest/birch boulders (one1/18attempt), obtainable wild clover and thatch resources/recipe/loot/actual compost data map are integrated with histories. Block discoveries are visible in the journal and passed real death/remote-respawn/client-process restart, alongside old-save/clone/max-page server checks. Final25JUnit/118GameTests and284JSON/197keys. Native natural generation/alpha fix and scope:docs/AGENT_CONTENT_INTEGRATION.md. No next major system started. Major geography/civilization/transport/dungeon/late-dimension gaps and full Alpha/multiplayer/scale gates remain.


## Kilo fauna and nature checkpoint

History-preserving integration of1002d09/18eeada/78e5134/820e5db adds three biome-scoped animals, corrected optional behavior and rare grass/fern groups. Amphibious otter surface swimming/bank rest/return to water, actual covered badger travel, day-active/night-resting/fleeing marmots and occasional deer head turns coexist with ordinary safety/breeding/food goals. River surface spawn placement, grassy marmot habitats, missing renderers and invalid plant codecs/localization repaired. Existing deer/boar/clover/boulders/journal remain. Full build25JUnit/132GameTests and299JSON/203keys passed. Native and measured-scale scope, natural-spawn/audio/multiplayer/long-survival gaps:docs/KILO_FAUNA_INTEGRATION.md. No new major system started; playable Alpha is not declared complete.

## Kilo world-polish QA checkpoint

Missing e9af309/65f11a0 history integrated afterd861718; existing barley and five meals are not duplicated.25JUnit/136GameTests passed with live composting, moisture-sensitive actual growth and two-meal scheduled cooking checks. Native scope/remaining Alpha gates:docs/KILO_WORLD_POLISH_INTEGRATION.md. This checkpoint adds QA coverage, no production mechanics or additional runtime work; earlier long-tick, survival/multiplayer/physical-hardware gates remain open.

## Recipe UI migration status

The bespoke item/recipe screen and inventory overlay are removed. Creative categories fromcb4cbc2remain. Real recipes/stations and recipe metadata tests remain;21JUnit/133GameTests pass. Optional JEI dev launch prepared but no released JAR/API could be fetched (official Maven CONNECT403). JEI custom process categories and client-with-JEI acceptance remain pending; historical browser implementation milestones above are not the current UI. Details:docs/JEI_MIGRATION.md. No Kilo integration or new gameplay subsystem.


## Deer visual checkpoint

- [x] Approved deer proportions/64pxtexture, independent antler UV, anchored neck and fawn antler visibility.
- [x] Native adult/fawn views, extreme pitch and ordinary AI stepping up/down controlled uneven ground; source/server checks recorded in docs/DEER_VISUAL_ACCEPTANCE.md.
- [ ] Remaining animal/other texture revisions require separate user approval; no mass replacement in this checkpoint.
- Existing physical-hardware, multiplayer, long-survival and full-Alpha gates remain open. JEI is still deferred; no new gameplay system started.

Agriculture roadmap item 5 now has ordinary barley, pear fruit trees and beekeeping (vanilla hives with up to three frames, a bee smoker, wax milling, a wax block and two honey meals); fertility/fertilizers/greenhouses and non-bee husbandry remain open. Headless checks do not cover graphical tree/leaf rendering, survival-client sapling growth, large-orchard profiling or authenticated multiplayer. Source lives on `feat/opencode-development` and is not merged into `feat/reimagined-alpha`. Details: `docs/ORCHARD.md`.

## Beekeeping increment

Beekeeping is built on vanilla bees, nests and hives instead of a duplicate hive block. Hive frames are a persisted NeoForge data attachment on the real `BeehiveBlockEntity` (maximum three, returned when a player breaks the hive). The bee smoker harvests a full hive safely (one bottle plus one honeycomb per frame, colony retained, nearby angry bees calmed), mills honeycomb into wax through the existing millstone and adds a wax block plus two cooking-pot meals (`honeyed_pear`, `honey_cake`). No ticker, world scan, chunk load, packet or saved-data index is added; the only recurring work is vanilla bee AI and random ticking.

Full Gradle build **19/19 JUnit** tests and a fresh official server report of **92/92 required GameTests** passed, zero failures/errors/skips. Resource check: **205 JSON files / 164 bilingual keys**, with model/texture/blockstate/recipe-result links valid. Eight beekeeping tests cover registration/tags/recipes, the frame cap and NBT persistence, real interaction installation plus break recovery and non-hive rejection, safe smoked harvest/calming/durability, unripe and frameless hives, air-use cooldown, real pot cooking with container remainders and eating, plus browser projection, network codec and EN/RU keys. Headless checks do not cover rendered textures/particles, survival-client harvesting, large-apiary profiling or authenticated multiplayer. Details: `docs/BEEKEEPING.md`.


## Agent integration candidate — 2026-10-29 (main publication blocked)

Freebuff c6a0cb9 is history-preserving merge1a82150: build,21freshJUnit and133GameTests passed. OpenCode090a2cc beekeeping is merged into the local candidate with creative-category,14-page journal budget, reloadable wax composting and bounded/cooldown smoker corrections. Combined build/21JUnit/143GameTests and324JSON/203keys/63PNG inventory passed; all133base tests plus8author tests retained,2integration tests added. Native installation/harvest, Survival durability, creative food icons and /reload checked; a second client process retained3frames, harvested hive state and smoker damage1.

Kilo23a515f was **not merged**: missing renderer, disconnected client/server trade UI, unprotected output/incorrect quantities and unbounded/ticket-based NPC work are critical blockers. Do not present all-three integration or Alpha as complete. origin/feat/reimagined-alpha stays0c30793; verified partial work is saved only on integration/agents-review-0c30793. Details, source evidence, limits and next gates:docs/AGENT_INTEGRATION_REVIEW.md.

## Settlement audit remediation — 2026-10-29

Only feat/kilo-settlements-economy: no merge/Codex branch changes. Preserved professions/prices/old save fields; vanilla merchant packets and result slots now own server trades, with full-result capacity checks. Per-UUID stock persists and lazily restocks once per work day. Renderer/model/reference vanilla texture and nonduplicated spawn egg are registered. Navigation checks the full vanilla path region through getChunkNow, throttles40ticks and never uses tickets as FULL proof; no global per-tick SavedData scan.

Final build/25freshJUnit/134GameTests passed,286JSON/200bilingual keys validated;128original tests retained and6added. Native Survival64sticks→2emeralds, model rendering and actual client-process reentry/persisted uses=[2,0,0] verified. Real integrated-server600sample load:1NPC mean1.863/p99=9.627ms;33NPC mean3.504/p99=38.438/max85.935ms. This is not a lag-free or before/after optimization claim; a separate long stall outside samples remains unexplained. Exact scopes, procedure, JFR limitations and remaining integration gates:docs/SETTLEMENT_REMEDIATION.md.


## Final combined integration candidate — 2026-10-29

On integration/final-agents-settlements-c31544c: history-preserving merge04ac191 joins d8af955 and verified c31544c, not29ba004. Own tabs/nature tag, recipes, EN/RU, module registrations and all introduced tests retained. Native joint-world testing exposed smoker air-use falling through after trader interaction;22a4f48 consumes the client action without client-side inventory authority and adds a real joint hive/trade regression. Final build/21freshJUnit/160GameTests and325JSON/206keys passed. Native bees/hive/NPC/paid trade/creative egg/configured boulder and full process restart checked;33NPCs,3frames and stockuses4 persisted. Core deer visuals unchanged.

Published candidate only; no main/source-branch update. Repeat32NPC profile had mean2.184ms/max115.093ms and baseline max382.014ms; long pauses remain unexplained. Functional gates pass but unconditional main promotion/Alpha completion is not recommended before a controlled dedicated-server profile. Full procedure, attribution/history and exact limitations:docs/FINAL_AGENT_INTEGRATION.md.
