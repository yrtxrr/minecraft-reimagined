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

## Executed combined native acceptance

Minecraft1.21.1/NeoForge21.1.264, Java21/4active processors/2GiB, RU/GUI2, Xvfb/Mesa software rendering, view4/simulation5. Fresh marked disposable AgentIntegrationSmoke with vanilla overworld noise/biomes and seed12345; no old region data was copied. The first client attempt failed in FML early-display config before world load. Disabling the config watcher in ignored run/config/fml.toml allowed the normal client to start; this is an environment workaround, not a mod/security/authentication change or an upstream FML fix.

### Natural generation and resources

Visited forest coordinates through normal world generation, without placing clover/rocks manually. A bounded offline read of saved FULL chunks before any content fixtures counted338chunks,9clover blocks and103mossy-cobblestone blocks at forest-biome positions. These are **blocks, not103boulders**, and a one-seed survey is not a frequency estimate. Rarity18 and the absence of a count multiplier remain the explicit production contract. Native inspection saw a real forest rock under the canopy and natural clover patches. Registration in both ordinary/birch forests and the placement budget have direct server-test coverage; there was no separate natural birch-only statistical survey.

Client inspection exposed opaque black clover quads: the new cross model needed `render_type: minecraft:cutout`. Fixed and confirmed through a real resource reload; transparent pixels now render correctly. Clover's low outline follows the vanilla positional offset and its six-pixel visible height. The supplied thatch cube and controlled clover also rendered in the client. Original16pxtextures are now present; no external artwork copied.

### Journal death and reentry

A separate high platform supplied a visible calcite block and a stationary cow for controlled observation; no discovery NBT was supplied. Actual journal use recorded ocean/cow/calcite. Its compact Russian introduction showed all three counts, and the final catalogue page16/16displayed “Редкие блоки / Кальцит”.

Changed to Survival, keepInventory=false, doImmediateRespawn=true, with spawn point more than six blocks from the calcite and the cow. Real `/kill` death/respawn dropped the inventory. Positive before/after NBT predicates confirmed all three original IDs survived. The remote respawn position prevents relearning from masking lost data. Normal save/quit stopped the whole client; a new client process loaded at that remote position and again passed the full three-catalogue predicate. This verifies real death and reentry, not merely keeping an old book component. Nondeath clone/deep-copy/old-save/max256cases additionally passed GameTests.

### Final scope and limits

Final full build25JUnit/118fresh official GameTests,0failures/errors/skips;284JSON/197bilingual keys. Existing fishing, barley/pear, boar, mechanics, magic and smithing checks retained. Player JAR excludes all GameTests/profilers. Native evidence/worlds/NBT scans/logs remain ignored under run/ and /tmp. Agent branches were not written; main merge parents preserve df7a223 and25e4ea7 (includingafc5bd3/1835d41).

Actual crafting, harvest loot, compost insertion and sparse clover/configured-rock placement were exercised by server tests; these complete interactions were not all repeated manually in the client. Native survey/visuals/journal persistence are the client acceptance above. No authenticated multiplayer, physical GPU/audio, long-session stability or generation/MSPT/retained-memory benchmark was performed. Source bounds/successful tests do not establish lag-free gameplay or a complete Alpha. No next major mechanic started.
