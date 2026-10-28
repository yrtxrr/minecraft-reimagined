# Remaining agent content integration —2026-10-28

Starting main60b7a4a. Exact requested heads:Freebuffdf7a223 and Kilo25e4ea7 (parents1835d41/afc5bd3). All four requested commits were absent by ancestry; Freebuff fishing4a71f92 was already present and is not transferred again. Only the missing source deltas are merged, preserving author history. No writes/pushes to agent branches; no new major mechanic.

## Freebuff boulders

Vanilla `forest_rock` configured with mossy cobblestone; standard biome modifier targets #minecraft:is_forest, including ordinary/birch forests. Placement has one rarity18 candidate, in-square spread, MOTION_BLOCKING heightmap and biome filter. Nominally one attempt per18eligible chunks; unsuitable soil/terrain can reduce actual success. No count multiplier, ticker, chunk scan or custom terrain generator. Generation occurs through Minecraft's decoration pipeline. Existing vanilla mossy cobblestone supplies assets/loot/tags; both feature-name translations are present.

Full build23JUnit/110required GameTests passed. Author registration/biome test retained; added real configured-feature placement on soil with a small bounded rock count and4096rarity-filter samples asserting <=1candidate and sparse acceptance. Tests cover forest/birch inclusion and desert exclusion. This is executable feature/rate coverage, not a survey of natural chunks or a world-generation performance benchmark. Native combined content acceptance follows later in this document.
