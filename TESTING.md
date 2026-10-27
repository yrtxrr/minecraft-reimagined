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
