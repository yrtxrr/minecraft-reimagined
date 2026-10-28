# Freebuff fishing integration

Integrated source: `feat/freebuff-development`, `4a71f92ea79977e68cbb0c0a1e7de24725a4765b`, based on75e5960. Main started atc2bc853. Preserve this author commit as a merge parent; no cherry-pick/squash or external-branch overwrite. The user's new authorization supersedes the earlier Freebuff exclusion. `feat/kilo-development` was not fetched, merged or changed.

## Actual gameplay

Three new **item** fish (not swimming entities): River Perch / Речной окунь, Silver Trout / Серебряная форель, Blue Mackerel / Голубая скумбрия. Food nutrition/saturation modifier:2/.2,3/.3,2/.2 (restored saturation .8,1.8,.8). They appear in the food creative tab and shared searchable item browser. All have16x16RGBA item textures, generated-item models and EN/RU names supplied by Freebuff.

The built-in NeoForge `add_table` modifier applies only to `minecraft:gameplay/fishing`. At each successful loot roll it has15%chance to append **one** fish, with5:3:2weights. Thus nominal per-catch bonus chances are7.5%perch,4.5%trout,3%mackerel. A bonus may accompany ordinary fish, junk or eligible treasure. Vanilla root/nested tables are not replaced; original catches/counts/components remain intact in the default mod set. Vanilla open-water/luck/treasure rules still own the base catch. These are globally fishable names, not regional biome restrictions. Other datapacks/modifiers can still compose with/alter loot through standard NeoForge ordering.

Fishes tag extends `minecraft:fishes` with replace:false, preserving cod/salmon/etc. The global modifier list also preserves the existing barley-seed entry. Existing seasons, machines, boars, smithing and magical recipes are not rewritten. Raw-fish acquisition is loot, not a RecipeManager crafting recipe; browser **uses** show its actual processing recipes, not a fabricated fishing crafting recipe.

Each fish smelts to the existing vanilla cooked cod (200ticks,.35XP). Integration adds the missing ordinary smoking equivalent (100ticks,.35XP), same output. No new cooked variant, station, chain or advanced cooking subsystem. The common browser automatically displays all six ingredient/result/furnace/smoker projections.

## Review and corrections

The original test only found three bonus item types and did not assert preservation of vanilla catches. It now compares512nonzero deterministic seeds against an unmodified raw control roll: original item/count/components must match exactly and total length may increase by at most1. All three bonuses and all four vanilla fish occur; the bonus remains optional. Raw rolls are deliberately restricted to this test control, never the gameplay acquisition path.

Additional tests prove unrelated chest loot receives no new fish, direct vanilla fish subtables are not modified independently, and the extra table returns exactly one fish without recursive bonuses. Real fueled furnaces and smokers process all three inputs; tags and common browser projections are checked. The original registration/food/recipe test is retained. No custom loot modifier Java implementation or gameplay cache was required.

No new production ticker, chunk loading, world/entity scan, persistent queue, network packet or mandatory mod dependency is introduced. The workload is one supported loot modifier at a completed catch, with an optional one-roll/three-entry subtable. This is a cost contract, not a large-server TPS or memory benchmark.

Upstream verification: installed NeoForge21.1.264 AddTableLootModifier (LGPL2.1-only) was inspected. It appends a raw subtable once and deliberately does not reapply global modifiers to it. We use the existing platform codec through JSON; no source copied. Freebuff's supplied PNG assets are retained; integration imports no external mod assets. See THIRD_PARTY_NOTICES.md.

## Native acceptance procedure and limits

Opt-in `./gradlew runClient -PquickPlayWorld=FishingSmoke -PfishingAcceptance` enables a read-only dev observer in the already JAR-excluded engineering/profile package. It reads only the sole connected player's existing hook nibble field to log a fresh bite, and logs actual ItemFishedEvent drops. Reflection is confined to this dev probe; it never writes hook state, accelerates timers, changes RNG, drops or cancels events. With the option absent it is inert; the class is excluded from the player JAR entirely. This is necessary for precise native mouse reeling on a cloud with no OpenAL sound device, not a production fishing system.

External X11 input casts and reels through the real client upon observed bites. A disposable copy of the existing approved smoke world contains a15x15x3open-water pond, clear daytime and a supplied vanilla LureIIIrod. Lure affects the vanilla waiting period, not the15%bonus. No Luck of the Sea or custom loot command is used. The first fixture accidentally retained a stone ceiling; it produced no catch and was corrected before acceptance. Tests/observer do not replace a natural survival resource-acquisition trip or authenticated multiplayer.

Executed results are appended below after native completion. Large-server fishing concurrency, physical GPU/audio and third-party loot overrides remain unverified. No universal performance/compatibility guarantee is inferred from a successful build.

## Executed acceptance —2026-10-28

Full merged Gradle build:23JUnit passed. Fresh official harness:107/107GameTests,0failures/errors/skips, including the four fishing tests described above. Resource validation:265JSON,191bilingual keys, model/texture links valid. All old103tests and author history retained. The final player JAR is checked to include the fish assets/recipes and exclude GameTests/all dev profile probes; no binary/world/log/screenshot is committed.

Real client Minecraft1.21.1/NeoForge21.1.264, Java21.0.8/4active processors/2GiB, RU/GUI2/Xvfb/Mesa llvmpipe, view4/simulation5. Ordinary casting/reeling with supplied vanilla LureIIIrod produced these first five successful catches:

1. Cod.
2. Tripwire hook (vanilla junk).
3. Salmon.
4. Cod.
5. **Salmon plus Silver Trout**.

Actual ItemFishedEvent drops and a subsequent positive player-inventory assertion confirmed the trout. No fish was supplied before these catches. Perch/mackerel were then supplied only for visual inspection, so they are not claimed as separate native fishing catches; all three occur in the512seed server check. All three icons rendered in the real inventory, and the RU browser showed “Серебряная форель”, its furnace recipe and smoking recipe with the actual cooked-cod result/station. Furnace/smoker processing of every species was executed in real server block entities by GameTests; manual client cooking of all six cases was not repeated.

The disposable world saved normally and the client stopped. No new source defect was exposed by this native path. Audio remained unavailable (no OpenAL device); authentication endpoints remained unavailable and were not bypassed. Physical GPU, natural-world fishing trip, native catches of the other two species, authenticated multiplayer and other mods' loot overrides remain outside this acceptance. The five catches do not statistically measure the15%rate or establish large-server performance.

Native logs/rod inputs/screenshots remain ignored under run/ and /tmp. The review of remaining specification priorities is docs/SPEC_PRIORITIES.md; no next large mechanic was started.
