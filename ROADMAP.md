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
5. Agriculture: ordinary barley with seasonal hooks, harvest and milling implemented; further crops/fruit trees, fertility/fertilizers/greenhouses, husbandry. Heated cooking pot, five connected meals, browser/menu and hopper automation implemented; expand cooking variety after focused runtime validation, without mandatory thirst/disease/micronutrients.
6. Content/civilizations: cultures, NPC states/cities, reputation/citizenship, diplomacy/trade/armies, persistent unloaded-region simulation. Player cannot found a state. Protect player terrain from NPC wars.
7. Exploration: original mobs/animations, room-connected dungeons/puzzles/mini-bosses, Nether/End biomes and independent bosses/artifacts, retaining dragon and wither.
8. Transport/economy: carts/caravans/sailing/ships, linked minecarts/switches/loading stations, roads/geography/economy integration.

Each increment requires a passing build, relevant automated tests, a meaningful game/server check and a Git checkpoint. No milestone is satisfied by documentation alone.

## Current ownership

Main developer owns the new smithing cycle and its focused validation in `feat/reimagined-alpha`. OpenCode develops pear orchards independently; Antigravity handles existing-system performance/testing. Do not overlap their files or merge their branches. Shared entry-point/language/documentation edits are additive only. FCC-Codex handoffs are historical. The smithing source/server checkpoint has23JUnit/88GameTests; native client validation follows. Prior Alpha gates remain open. See docs/SMITHING.md and docs/MAGIC_RESEARCH.md for current feature limits.
