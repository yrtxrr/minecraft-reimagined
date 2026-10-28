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
4. RPG: richer finite skill trees and UI, refine verified parry/special attacks and add shield/armor variants, extend the verified readable/discovery journal with research progression, further spell tiers/stations beyond the implemented four-spell research tier, deterministic material/skill/equipment smithing.
5. Agriculture: ordinary barley with seasonal hooks, harvest and milling implemented; further crops/fruit trees, fertility/fertilizers/greenhouses, husbandry. Heated cooking pot, five connected meals, browser/menu and hopper automation implemented; expand cooking variety after focused runtime validation, without mandatory thirst/disease/micronutrients.
6. Content/civilizations: cultures, NPC states/cities, reputation/citizenship, diplomacy/trade/armies, persistent unloaded-region simulation. Player cannot found a state. Protect player terrain from NPC wars.
7. Exploration: original mobs/animations, room-connected dungeons/puzzles/mini-bosses, Nether/End biomes and independent bosses/artifacts, retaining dragon and wither.
8. Transport/economy: carts/caravans/sailing/ships, linked minecarts/switches/loading stations, roads/geography/economy integration.

Each increment requires a passing build, relevant automated tests, a meaningful game/server check and a Git checkpoint. No milestone is satisfied by documentation alone.

## Current ownership

All development is assigned to the main developer in `feat/reimagined-alpha`; FCC-Codex no longer participates. Prior handoff documents are historical. Mechanical animation/cache and ordinary barley include graphical checks recorded in docs/ALPHA_VALIDATION.md. The cooking workstation increment has source/server coverage, native client/restart checks and a measured0/64/256-pot server scenario; see docs/COOKING.md for limits. The first research tier now has two functional stations/four advanced spells and source/server coverage; native client/performance validation is next. Subsequent content priorities include manual/wind engineering drives or exploration content. No unrelated branch is merged automatically.

## Pear orchard increment

Full Gradle build **19/19 JUnit** tests and a fresh official server report of **84/84 required GameTests** passed, zero failures/errors/skips. Resource check: **183 JSON files / 151 bilingual keys**, with model/texture/blockstate/recipe-result links valid. Six orchard tests cover real registration/tags/recipes and browser projection, loaded configured/placed features, bounded inner-canopy random ticking, summer ripening versus winter rejection through the real calendar, actual interaction harvest and pear entities, real leaf loot with shears, and sapling growth through the actual configured tree feature.

Agriculture roadmap item 5 now has ordinary barley plus pear fruit trees; fertility/fertilizers/greenhouses and husbandry remain open. Headless checks do not cover graphical tree/leaf rendering, survival-client sapling growth, large-orchard profiling or authenticated multiplayer. Source lives on `feat/opencode-development` and is not merged into `feat/reimagined-alpha`. Details: `docs/ORCHARD.md`.
