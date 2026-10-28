# Kilo world-polish integration — 2026-10-28

## Review and ancestry

Target starts at published d861718. The source branch feat/kilo-world-polish contains e9af309(parent e7e6969) and65f11a0(parent e9af309), neither previously an ancestor of the target. The history-preserving merge includes only those two missing commits; the complete fauna/nature integration remains intact. Source branches and all existing worktrees are unchanged.

The incoming change does **not** add farming behavior or missing meals. Ordinary barley growth/bonemeal/seed loot and both meals already existed. The two recipe files in65f11a0 have exactly the same parsed content as the target, only compressed formatting and a new UTF-8 BOM. Keep the original canonical BOM-free files byte-for-byte; no duplicated recipes or new content IDs. Both recipes still require16seconds, one bowl and one of each ingredient:

- Mushroom dumplings:reimagined:flour + brown mushroom + red mushroom → one reimagined:mushroom_dumplings.
- Vegetable stew:carrot + potato + beetroot → one reimagined:vegetable_stew.

## Corrected tests

Incoming compost test queried legacy ComposterBlock.COMPOSTABLES with Block keys for clover/thatch instead of their items. Corrected to the active NeoForge1.21.1 item data map:clover0.3,thatch0.85,seeds0.3,grain0.65. Also execute actual item insertion and check consumption/first layer, not only configured numbers.

Incoming moist-soil test merely checked that a placed block had the barley ID. Replace with512paired real random-growth trials using identical seeded RandomSource sequences, age resets and matching3×3soil layouts, dry0versus moist7. Equal lighting and a glass shelter exclude time/season fluctuations without disabling the growth event. Both must grow; moist must grow more than1.8×as often and remain probabilistic. This is a bounded test fixture, no production tick loop.

Retain Kilo's five-recipe registry test. Add actual scheduled heated-pot cooking for both reviewed meals:at100ticks no output and positive work; then exactly one specified meal, with all three ingredients and one bowl consumed. No manual cookStep loop is used for this acceptance. Existing planting, bonemeal, age-aware loot, seasonal restrictions, milling, menu/automation, browser, saved cooking state and reload tests remain required.

## Executed automated checks

- Full `./gradlew build`:25/25JUnit, no failure/error/skip.
- Fresh isolated `bash scripts/verify-integration.sh --console=plain`:136/136required GameTests, no failure/error/skip (132previous +4new). Includes existing fauna behavior, sparse vegetation, journal, crafting and other regression checks.
- `python3 scripts/validate-resources.py`:299JSON/203bilingual keys, valid resource links.
- No production class/resource differs from d861718. Player JAR is byte-identical:SHA25651bc75a9c08a74412d13da5eff0f135ca042c1541272b5edac006c0e70bd5e72. Test classes remain excluded. No new runtime work, dependency, cache, packet, save rate or generation path is introduced. No new performance improvement/benchmark is claimed; unresolved long-tick observations from the fauna integration remain open.

An initial integration compile used the wrong package for NeoForgeDataMaps; corrected to registries.datamaps.builtin before the successful full build/test run. No failing check was counted as acceptance.

## Actual graphical client checks

NeoForge21.1.264/Minecraft1.21.1, Java21/4active processors/2GiB, RU/GUI2, Xvfb/Mesa llvmpipe, view4/simulation5. Separate marked disposable KiloWorldPolishSmoke copied from CookingSmoke; original worlds preserved. Existing ignored FML config-watcher workaround remains. Fixture uses fixed daytime/spring/clear weather, supplied resources/platforms/heated pots, normal randomTickSpeed3 and ordinary Survival interactions. Inherited background mob spawning is disabled; this is not a natural survival or fauna encounter check.

Two seedlings were planted by actual seed right-clicks, consuming8→6seeds. Wet soil initially moisture0 hydrated naturally to7next to water; remote dry soil remained0. The wet seedling advanced from age0through ordinary random growth before any bonemeal. Read-only native conditional checks confirmed these world states. The first hydration check was too early and did not pass; a later real hydrated state was observed, not supplied with a moisture-setting command. This is not a statistical native wet/dry growth-rate experiment; the paired quantitative comparison is the GameTest above.

A third seed was actually planted at age0, then real bonemeal use reached age7. Inventory confirmed one more seed consumed (6→5) and three bonemeal consumed (16→13), with mature barley visibly rendered. No age-changing command or custom growth probe manufactured that result.

Mushroom dumplings were cooked in the actual heated-pot GUI using supplied flour, brown/red mushrooms and one of two bowls. Ingredients/bowl disappeared only through normal cooking and one real meal was extracted into player inventory through the GUI; native inventory read confirmed reimagined:mushroom_dumplings×1and one remaining bowl. Vegetable stew used the other pot and supplied carrot/potato/beetroot/last bowl through normal GUI slots. Native block read confirmed exactly reimagined:vegetable_stew×1in output slot4. GUI extraction then left one of each meal in player slots9/10; both pots had empty Items lists, no ingredients/bowls remained. These are actual scheduled outputs, not supplied result-item fixtures. The world was saved through the normal Save and Quit flow; no additional full-process reentry was claimed for this checkpoint.

Raw logs, screenshots, disposable world/datapack and binary remain ignored, not committed. Native controls do not establish survival resource acquisition, statistical crop balance, physical-GPU/audio, authenticated multiplayer, long-session stability or standalone-JAR acceptance. No new large mechanic started and Alpha is not declared ready.
