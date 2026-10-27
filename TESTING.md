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
7. Craft/use each wand. Verify elemental wall occlusion/PvP rules, nature healing/growth only on valid targets, arcane step not through walls/outside world/unloaded terrain, ritual resource consumption and ally-only buff. Confirm mana cost/regeneration, server-enforced cooldown and death/restart persistence. Journal provides guidance, not a complete discovery database.
8. Check `/reimagined calendar` and HUD, season command operator permission, season offset after restart, synchronized day after joining/dimension travel. Compare winter temperate crop growth with/without a glass roof. Time commands and sleep intentionally affect this world-time calendar.

Record actual outcomes before marking the alpha milestone complete. Do not publish an unverified build as a completed mod.

## Engineering automation checkpoint

Full Gradle build passed again (12 unit tests). `bash scripts/verify-integration.sh` executed **20/20 required GameTests, 0 failures, 0 skips**, and verified the fresh XML report against all declared tests. Three new runtime tests cover actual vanilla hoppers inserting two wheat and receiving two flour, invalid ingredient rejection; sided capability simulation, exact64 capacity/remainder and output/input restrictions; and saving/replacing a genuinely mid-process machine with both slots occupied before resuming at its saved progress. Water wheels expose no inventory/capability. Manual interaction remains supported.

The integration wrapper preserves Gradle's failure exit status, removes stale reports, and rejects missing/zero/incomplete/skipped runs. GitHub CI now runs it and archives reports/logs, but that remote workflow has not been observed in this session. Verified the built JAR excludes test classes and the empty test structure.
