# Cooking pots and survival meals

This increment implements the cooking-workstation part of section13 of USER_SPEC_RU, using existing barley, mill flour, vegetables, hunting and fishing. It does not add thirst, dietary statistics, crop simulation or a mandatory food-mod dependency.

## Playing

Craft a cooking pot with5iron ingots and a brick (`I I / IBI /  I `). Place above a lit campfire/soul campfire, fire/soul fire or magma block. An optional **single** hopper, iron bar or iron block between heat and pot conducts heat; no taller column or horizontal heat search exists. Extensible additive block tags: `reimagined:cooking_heat_sources` and `reimagined:cooking_heat_conductors`. Tagged lit-capable sources must be lit, and waterlogged sources must be dry.

Open the pot with right click. The three left slots accept food/process ingredients in any order, the fourth accepts bowls, the rightmost is output only. One batch uses one item from each occupied ingredient slot and one bowl; extra unrelated ingredients prevent a match. The heat indicator, progress bar and empty-slot tooltips explain operation in English/Russian. All recipes are searchable in the independent recipe browser, including uses of barley, flour and bowls, station, duration and heat requirement. A short cooking chapter is included in the research journal.

| Meal | Ingredients (plus1bowl) | Time | Hunger / saturation |
|---|---|---|---|
| Apple barley porridge | Barley + apple |12s|7 /8.4|
| Vegetable stew | Carrot + potato + beetroot |16s|9 /12.6|
| Hunter's stew | Beef + barley + brown mushroom |16s|11 /17.6|
| Fish chowder | Cod + potato + dried kelp |16s|10 /16|
| Mushroom dumplings | Mill flour + brown mushroom + red mushroom |16s|9 /13.5|

Deer already drop vanilla beef; hunter's stew uses that existing loot without introducing an incompatible meat replacement. Meals stack to16and return a bowl through Minecraft's food component when eaten. They add no permanent attributes or effects. Existing bread, furnace, milling and other food recipes are preserved.

Top hoppers/handlers insert the three ingredients. Side handlers insert bowls; bottom handlers extract meals. A bottom hopper can conduct heat from the block directly below it and discharge into an adjacent chest. GUI shift-click routes bowls separately, ingredients into the input slots and meals to player inventory. Output rejects both GUI and automation insertion. Standard comparator fullness, destruction drops, container locks/names, range validation and vanilla menu networking apply.

## Recipe and lifecycle contract

`cooking` is an independent internal module; it uses vanilla `RecipeManager`, registry-friendly codecs and vanilla menu/container synchronization. JSON `reimagined:cooking`:1..3nonempty `ingredients`, one-item `result`, integer `seconds`1..1200(default12). The bound also keeps progress/duration within vanilla signed-short ContainerData transport. Recipe matching uses NeoForge's bounded3-slot shapeless matching so overlapping ingredient predicates work correctly. Item crafting remainders are retained in an emptied input slot or dropped if the original stack remains.

RecipeManager.CachedCheck retains only the last recipe ID. Each loaded pot memoizes its actual positive/negative match until inventory changes or the server's OnDatapackSync epoch changes. Reloads clear unfinished work when the recipe object changes; removal cannot finish the old recipe. No global world/entity references are retained. Idle invalid recipes are not repeatedly searched. A missed recipe is retried against new data within5seconds after sync, or on the next inventory change. The browser uses its existing recipe-sync lifecycle.

Progress, recipe ID and all5slots save through normal block-entity NBT. Initial load resumes only the saved recipe ID, clamps progress to valid bounds and requests a single scheduled update. Cold/missing-bowl/full-output states pause progress; incompatible ingredient/recipe changes reset it. Unloaded pots do not cook or catch up elapsed time. Breaking the pot drops actual contents through the standard container path.

## Work and performance contract

There is **no block-entity ticker**, entity search, world/chunk traversal or background thread. A scheduled update checks the already-ready own chunk via getChunkNow, reads1or2blocks in the same column and processes at most3ingredients. No inspection requests chunk generation/tickets. Empty pots stop scheduling. Nonempty blocked/cold/invalid pots check at most once per100ticks, with matching memoized; active pots advance20ticks of progress once per20ticks. Initial wake is spread across20position phases. Repeated wake requests are coalesced by the vanilla scheduled-tick queue.

Aggregate work scales with loaded nonempty pots, not the entire world. No custom packets exist: only block-state transitions and vanilla container deltas for actual viewers. Inventory/completed progress changes dirty normal saved data; no forced disk writes occur. Progress is dirty once/second while working. No server-wide scan/reload traversal is introduced. The vanilla scheduled-tick dispatcher supplies its global tick budget; this is not a claim that an unlimited number of active pots is safe.

## Upstream research

Reviewed Farmer's Delight1.21branch at `dd8b9b0bfec916a2da88a6d64e3a37f4a02c3259`: LICENSE(MIT, vectorwing), `CookingPotBlockEntity`, `CookingPotRecipe`, and gradle.properties(Minecraft1.21.1 / NeoForge21.1). Its standard recipe-manager/container architecture and shapeless ingredient matching informed the design. This pot is independently written with a smaller inventory and scheduled work. No upstream code, textures, models or dependency is incorporated. All added PNGs are original project pixel resources; pot model references installed vanilla cauldron textures rather than copying them.

## Verification

See PROGRESS.md and TESTING.md for executed checks. `CookingGameTests` covers real timed scheduling, unordered/extra ingredients, cold/waterlogged heat, bowl/output gating, NBT resume, reload hit/miss invalidation, capability/GUI routing, physical hoppers, food/bowl consumption, browser projection and recipe codecs.

Optional real-server profile: `source scripts/cloud-shell.sh; ./gradlew runCookingProfile -PprofileRevision=<commit>` in an isolated ignored `run-cooking-profile` directory containing `.profile-approved-disposable`. Configure seed12345/flat, view4/simulation4, no players and a spare server port. Do not accept an EULA or change authentication for this profile. The harness refuses a GameTestServer and aborts if any client connects. It measures four phases:0pots,64empty,64active,256active;200warmup+600measured ordinary ticks each. Setup/cleanup is capped16pots/tick, loaded-only. Checks require every active pot to produce exactly3real meals and consume matching inputs/bowls; empty pots must retain no scheduled work. JSON/per-tick tails and JFR remain ignored under its reports directory. CPU samples/allocations/GC must be read as observations, not precise per-feature cost or retained-memory proof. Client rendering/network traffic and huge factories require separate measurements.

## Executed runtime checkpoint

Graphical NeoForge1.21.1 client: Xvfb1280x800, Mesa llvmpipe, Russian/GUI scale2, Java21, -Xmx2G, ActiveProcessorCount4, view4/simulation5. A separate marked disposable CookingSmoke copy was used; original worlds were preserved. Native input opened the pot, observed barley/apple/bowl counts decrement, progress/heat display and rendered meal icons. Shift-click extracted cooked porridge; native eating returned a bowl (positive in-game COOK_NATIVE_EAT_RETURNED_BOWL check). The browser showed actual barley/apple/bowl inputs, pot/result and12s/heat note with clear localized rendering. No missing reimagined textures/models were logged. Software rendering/audio/authentication limits from the earlier Alpha checks still apply.

The first quiet profile at4025077 completed with every active pot producing3meals and sleeping pots having no scheduled work. Mean/p99/max(ms):0pots0.917/7.168/190.006;64idle0.463/1.548/97.308;64active0.417/2.084/5.890;256active4.640/9.926/1046.779(TPS19.326). No measured GC pauses; sampled allocation weights over30s:4.47/2.84/25.67/32.04MiB. The256phase still had rare long ticks. A full-stack sample in its longest-tick window showed CookingPotBlock.tick → Level.getBlockEntity → Level.getChunk; this is not proof that it caused the whole1047ms. Entry/menu/neighbor/comparator reads were consequently moved to getChunkNow/direct ready-chunk reads, extending the existing inner cookStep contract. A new no-load/no-ticker/idle-sleep GameTest passed. Final-source profiling follows separately; do not read the earlier table as a final-source performance claim.

Use `python3 scripts/analyze-cooking-profile.py <report.json>` with Java21 on PATH/JAVA_HOME to verify all four completed phases, real meal counts and600custom JFR tick events per measured phase, and summarize sampling/allocations/GC/nearby long-tick stacks. Raw reports/recordings/screenshots remain ignored.

## Final source runtime results —2026-10-28

Production revision **7b92e31**, Minecraft1.21.1/NeoForge21.1.264/Oracle Java21.0.8, Linux amd64, Intel Xeon Platinum8573C host,5visible CPUs/4CPU cgroup quota,32GiB container limit; server JVM ActiveProcessorCount4/Xmx2G. Same seed12345flat disposable world, view4/simulation4, spawn radius4,1089ready chunks, no clients/extra mods, magma heating,64barley/apple/bowls per active pot. No concurrent Gradle build/GameTest/client run during measurement. Four phases of200warmup+600measured ordinary ticks (10+30seconds); JFR profile mode includes warmup, summaries use600custom measured events. Raw report identifier:`cooking-1793135510422`; not committed.

| Scenario | Mean ms | p95 ms | p99 ms | Max ms | Ticks>50ms | TPS | Actual meals |
|---|---:|---:|---:|---:|---:|---:|---:|
|0pots|4.352|3.557|128.654|378.653|13|20.000|0|
|64empty pots|0.575|0.907|3.325|120.518|1|20.000|0|
|64active pots|1.042|1.599|4.123|283.942|2|20.000|192|
|256active pots|0.850|1.011|2.803|102.571|3|19.999|768|

Every active pot produced exactly3meals and consumed3of each input/bowl. Idle pots retained no scheduled updates. JFR weighted allocation samples over each measured30s were9.34/0.97/26.88/47.06MiB respectively; these are sampled weights across the whole JVM, not exact cooking allocations or live retained memory. The256phase recorded one32.75msGC pause. No forcedGC or heap-leak claim. Setup/cleanup/save windows are outside measured ticks. Analyzer clips sampling at the last measured Post event, excluding subsequent report/audit work; the harness now also records measurement-end UTC before that audit for future runs.

These short sequential phases vary substantially, including outliers in the empty baseline. They establish executed bounded scenarios and working output, **not** a statistically isolated speedup, unlimited scalability or lag-free gameplay. In the256phase's102.57mswindow, one main-thread stack sampled the bounded tag membership check in cookStep; another87.24mswindow sampled vanilla scheduled-block dispatch/Level.getBlockState. A single sample cannot attribute the whole delay. Exact remaining tail causes, longer warmup/duration, physical-hardware play, retained heap across repeated cycles, connected-client packet volume and save stalls remain future measurements. Do not optimize a five-entry tag lookup solely because it appeared in one sample.

The actual final-source client was then restarted into CookingSmoke. A native conditional command confirmed the4meals saved before process exit. Adding one barley/apple/bowl and restoring heat cooked a fifth meal; the reopened menu visibly showed output5and empty ingredient/bowl slots. The localized pot menu reopened on this final code. Evidence stays ignored at `run/screenshots/cooking-validation/`. Earlier native meal eating/browser observations remain valid; renderer/GUI/resources were unchanged by the loaded-entry fix. This focused check does not close Alpha's authenticated multiplayer, standalone-JAR/physical-GPU/audio or long survival-session gates.
