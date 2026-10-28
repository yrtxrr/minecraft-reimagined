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

1. Browser: concrete shulker-coloring dynamic previews implemented; complete remaining special/dynamic crafting adapters, recipe transfer/auto-fill, process-category expansion, accessibility and GUI-scale/end-to-end client reload verification. Shared displays, history restoration, vanilla brewing, smithing transforms, item categories and headless query/cache tests are implemented.
2. Engineering: animated wheels and bounded shared networks implemented; extend verified millstone hopper/capability automation to future machines, manual/wind drives, gear ratios, saw/press/crusher, pumps/lifts, redstone control refinement.
3. World: true watershed rivers/deltas, climate regions and regional geology/deposits, resource geography, ocean currents/archipelagos, settlement placement. Current noise preset is not a watershed simulation.
4. RPG: richer finite skill trees and UI, refine verified parry/special attacks and add shield/armor variants, extend the verified readable/discovery journal with research progression, further spell tiers/stations beyond the implemented four-spell research tier (altar/table, discovery gates, personal knowledge, journal/browser and focused client/server checks complete), first deterministic smithing cycle implemented (steel, two finishes, three bounded qualities, hammer/bellows and standard repairs); further alloys/stations/equipment tiers remain.
5. Agriculture: ordinary barley with seasonal hooks, harvest and milling implemented; pear orchards now integrated; fertility/fertilizers/greenhouses and husbandry remain. Heated cooking pot, five connected meals, browser/menu and hopper automation implemented; no further cooking stages planned for this priority; next content favors world variety/exploration rather than deeper production chains.
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
