# Testing

## Executed in the cloud — 2026-10-27

- JDK 21 compiler and Gradle 9.2.1 activation: passed. Host originally supplied only a JRE; checksum-verified JDK was installed under `/workspace/.tools`. Wrapper JAR/distribution integrity checked against official Gradle SHA-256.
- `bash scripts/verify-domain.sh`: **12 tests found, 12 started, 12 passed, 0 skipped**. Covers uncapped XP/level thresholds/finite mastery and invalid state, bounded mana/costs, seasonal boundaries/greenhouse crop rules, water-wheel capacity/overload and truncated mechanical graphs. Latest XML reports: ignored `build/domain-tests/results/`. These tests compile only pure domain classes, not Minecraft adapters.
- A real defect was found: SkillProgress.EMPTY initialized before its BigInteger constant caused ExceptionInInitializerError. Corrected constant order; complete domain suite rerun successfully.
- `python3 scripts/validate-resources.py`: **59 JSON resources**, model/texture cross-links, **66 RU/EN translation keys**, local density-function references/cycle checks and additive preset tag passed. This does not execute Mojang codecs or generate chunks.
- `bash -n scripts/*.sh`: passed.
- Official NeoForge 1.21.1 documentation/source signatures inspected for events, registries, payloads and block-entity serialization. Static review does not prove compilation.

## Blocked — not passed

`bash scripts/setup-cloud.sh` invokes `./gradlew --no-daemon build`. It stops at `:createMinecraftArtifacts` because the cloud egress proxy returns **403 Forbidden** for `https://maven.neoforged.net/releases/net/neoforged/neoform-runtime/2.0.31/neoform-runtime-2.0.31.pom`. TLS verification is enabled; the system truststore is used. No checksums/signatures were disabled and no forbidden endpoint is bypassed.

Domain additions saved in the environment draft need applying through environment settings. Then rerun:

```sh
cd /workspace/minecraft-reimagined
bash scripts/setup-cloud.sh
source scripts/cloud-shell.sh
python3 scripts/validate-resources.py
./gradlew build
```

CI is configured to run the full Gradle build on GitHub, but has not run in this session. No compiled mod JAR, server launch, GameTest suite or graphical client validation is claimed. `runGameTestServer` has a development configuration but no registered GameTests yet; a zero-test run must not be accepted as verification.

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
