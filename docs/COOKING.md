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
