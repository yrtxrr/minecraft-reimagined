# Remaining agent content integration —2026-10-28

Starting main60b7a4a. Exact requested heads:Freebuffdf7a223 and Kilo25e4ea7 (parents1835d41/afc5bd3). All four requested commits were absent by ancestry; Freebuff fishing4a71f92 was already present and is not transferred again. Only the missing source deltas are merged, preserving author history. No writes/pushes to agent branches; no new major mechanic.

## Freebuff boulders

Vanilla `forest_rock` configured with mossy cobblestone; standard biome modifier targets #minecraft:is_forest, including ordinary/birch forests. Placement has one rarity18 candidate, in-square spread, MOTION_BLOCKING heightmap and biome filter. Nominally one attempt per18eligible chunks; unsuitable soil/terrain can reduce actual success. No count multiplier, ticker, chunk scan or custom terrain generator. Generation occurs through Minecraft's decoration pipeline. Existing vanilla mossy cobblestone supplies assets/loot/tags; both feature-name translations are present.

Full build23JUnit/110required GameTests passed. Author registration/biome test retained; added real configured-feature placement on soil with a small bounded rock count and4096rarity-filter samples asserting <=1candidate and sparse acceptance. Tests cover forest/birch inclusion and desert exclusion. This is executable feature/rate coverage, not a survey of natural chunks or a world-generation performance benchmark. Native combined content acceptance follows later in this document.

## Kilo integration corrections and source checkpoint

Merge retains all three author commitsafc5bd3/1835d41/25e4ea7. Language conflicts were resolved by preserving current main translations and adding new keys semantically; incoming Russian mojibake, duplicate thatch keys and JSON BOMs were not propagated. Added original16pxthatch/clover textures, real thatch self-loot and vanilla block/item small_flower tags. Clover now has a sparse vanilla flower patch (one1/8candidate,12bounded placement trials) in forests/birch/plains/meadow, so it is obtainable naturally rather than creative-only. No new farming framework or recipes beyond author thatch4wheat→4blocks.

Composting required a real NeoForge1.21.1 fix: vanilla reads the compostables item data map, not late writes to the deprecated Java map. Added supported data-map entries for the intended thatch(.85)/clover(.3) and existing barley/seeds/pear/sapling/leaves values, preserving their declared design. Actual composter insertion tests initially failed and now pass, including existing barley and pears.

Journal rare-block IDs remain additive v1NBT under Blocks; old saves lacking the field and the two-list constructor remain compatible. Death/nondeath clone copies the full compound deeply, and actual player serialization/reentry is tested. New entries now appear in written pages/introduction. At the combined256discovery cap, layout packs four entries only when the normal three-entry layout would reach100pages. Two JUnit checks cover all full-budget catalogue partitions and ordinary unchanged layout; actual256block journal encoding also passes.

Replaced the new volumetric729position/hidden-ore scan with one six-block loaded-only sight ray per existing ten-second observation. Creature observation now caps64candidates/eight visibility rays; biome lookup guards nearby ready chunks and all observation rays use non-requesting loaded access. No background chunk loading, per-tick world scan or new permanent cache. Walls hide undiscovered rare blocks. No performance-readiness claim is inferred from these source bounds.

Full corrected source build25JUnit/118required fresh GameTests passed (author tests retained). Added actual thatch crafting/loot/browser/composter, clover soil/loot/tags/real bounded generation, rare-block visibility/idempotence, old-save/death/deep-copy/nondeath/reentry preservation, unknown-ID filtering and maximum-book codec checks. Resource validation284JSON/197bilingual keys. Initial failures (compost data map, incoming RU encoding and a locale-dependent book assertion) were fixed without weakening functional expectations. Native combined validation follows separately.
