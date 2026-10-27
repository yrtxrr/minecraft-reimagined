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
- [ ] In-game exercise deer spawn/breeding, powered milling, saving/restart, client recipe reload and server/client interaction.
- [x] Produce a development JAR with server integration verification.
- [ ] Validate graphical client and multiplayer before calling the playable alpha ready.

## Subsequent playable increments

1. Browser: complete actual special/dynamic crafting adapters, recipe transfer/auto-fill, process-category expansion, accessibility and GUI-scale/end-to-end client reload verification. Shared displays, history restoration, vanilla brewing, smithing transforms, item categories and headless query/cache tests are implemented.
2. Engineering: animated wheels and bounded shared networks implemented; extend verified millstone hopper/capability automation to future machines, manual/wind drives, gear ratios, saw/press/crusher, pumps/lifts, redstone control refinement.
3. World: true watershed rivers/deltas, climate regions and regional geology/deposits, resource geography, ocean currents/archipelagos, settlement placement. Current noise preset is not a watershed simulation.
4. RPG: richer finite skill trees and UI, refine verified parry/special attacks and add shield/armor variants, extend the verified readable/discovery journal with research progression, spell research and stations, deterministic material/skill/equipment smithing.
5. Agriculture: ordinary barley with seasonal hooks, harvest and milling implemented; further crops/fruit trees, fertility/fertilizers/greenhouses, husbandry, cooking workstations without mandatory thirst/disease/micronutrients.
6. Content/civilizations: cultures, NPC states/cities, reputation/citizenship, diplomacy/trade/armies, persistent unloaded-region simulation. Player cannot found a state. Protect player terrain from NPC wars.
7. Exploration: original mobs/animations, room-connected dungeons/puzzles/mini-bosses, Nether/End biomes and independent bosses/artifacts, retaining dragon and wither.
8. Transport/economy: carts/caravans/sailing/ships, linked minecarts/switches/loading stations, roads/geography/economy integration.

Each increment requires a passing build, relevant automated tests, a meaningful game/server check and a Git checkpoint. No milestone is satisfied by documentation alone.

## Current ownership

All development is assigned to the main developer in `feat/reimagined-alpha`; FCC-Codex no longer participates. Prior handoff documents are historical. Mechanical animation/cache and ordinary barley source/automated checks are complete, with graphical validation still open. Next is the first unfinished browser increment above. No unrelated branch is merged automatically.
