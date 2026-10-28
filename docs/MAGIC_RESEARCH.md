# Spell research and magic stations

This increment implements a finite, playable continuation of the four existing schools. All four basic spells remain available in old/new worlds. New personal knowledge does not reset existing wands, mana, skills or shared school cooldowns.

## Survival progression

Craft an Arcane Altar (amethyst shard,3obsidian,2copper ingots) and a Research Table (book,amethyst,paper,planks). Recipes unlock in the crafting book upon obtaining amethyst. Both stations have a real4-slot inventory, preview, mana/observation requirements and a server-authorized action button. Three ingredient slots are unordered; the fourth is output only. Materials stay in the block when its menu closes or the world saves. Breaking drops the actual inventory. No nearby dropped-item collection, multiblock search, mandatory dependency or automatic crafting loop exists.

| Altar focus | Ingredients | Mana | Research result |
|---|---|---:|---|
| Ember | Amethyst + blaze powder + redstone |15|Frost Lance folio|
| Verdant | Amethyst + barley + bone meal |15|Purification folio|
| Spatial | Amethyst + ender pearl + lapis |15|Magelight folio|
| Ward | Amethyst + gold + glowstone |15|Sanctuary folio|

At the Research Table, combine one focus, paper and lapis. Each folio costs20mana and requires personally recorded observations of2biomes and1creature. These are the existing journal discoveries, not a second exploration database. Using a journal also performs its normal bounded observation. Folios are physical, tradeable knowledge: the researcher must meet exploration requirements; a recipient may learn by reading a traded folio. Reading consumes one folio and permanently unlocks its spell. Already-known folios are not consumed or rewarded again. All four advanced spells can be learned; no mutually exclusive school choice or random success exists.

Sneak-use a wand to cycle that school's known spells. Normal use casts the selected spell. Selection persists in vanilla custom item data; casting still checks the **current player's** knowledge, so a borrowed/edited advanced wand cannot grant an unlock. Sneaking can return such a wand to its basic spell. Cooldowns remain shared by school, preventing basic/advanced switching from bypassing them. Mana is the existing100-point resource and regeneration system.

| New spell | School | Effect | Mana / cooldown |
|---|---|---|---|
| Frost Lance | Elemental |4magic damage +6sSlownessI; respects collision, teams/PvP checks |30 /3s|
| Purification | Nature |Removes poison/wither/hunger;6sRegenerationI |35 /8s|
| Magelight | Arcane |60sNight Vision +10sSlow Falling |20 /6s|
| Sanctuary | Ritual |30sFire Resistance +AbsorptionI, consumes one amethyst shard |45 /15s|

The journal adds two personal progress pages, retaining its strict vanilla-book size bound even at maximum discoveries. Browser results/uses show station, ingredients, mana and discovery requirements; folio recipes also show the current player's learned status/count. Small owner-only progress snapshots synchronize on login/respawn, changed discoveries and newly learned spells. Logout clears the primitive client snapshot. No client unlock/craft state is authoritative.

## Architecture, safety and work bounds

`magic` owns station registrations, menu/BE, recipe type/serializer pairs, finite Spell catalogue, versioned player knowledge and progress payload. Both `reimagined:arcane_infusion` and `reimagined:spell_research` use vanilla RecipeManager/sync with1..3nonempty ingredients, one result, mana1..100 and discovery requirements0..16. Existing shared recipe projection handles them. Server menu actions re-resolve actual recipes and verify owner/container/range/block/lock/spectator state, prerequisites, mana and output capacity before consuming anything. Unsupported buttons and repeated actions within10ticks fail closed. Shared inventory is mutated synchronously on the server, so concurrent viewers cannot spend the same ingredients twice.

Stations have **no ticker or scheduled work**. Only an open menu refreshes status/preview once per20ticks; no closed inventory or world-wide scan occurs. Actions inspect at most3ingredients and the existing bounded discovery data. Vanilla menu synchronization sends changed slots/data to viewers. World reads for station menus, comparators and validity use getChunkNow and fail closed when the ready chunk is absent. Normal persistence owns disk writes. No world/player references are retained in a global collection.

Basic and frost rays now use a loaded-only BlockGetter view: unavailable chunks behave as a solid barrier, without a request/future. Range is capped16blocks, living-entity candidate traversal stops after64visits, and the ray emits at most17particle-send calls rather than the previous65. This bounds target selection; crowded scenes may omit a target beyond the candidate budget. Nature targeting uses the same6-block loaded ray; blink checks at most16half-block steps and loaded collision-neighbor chunks before native collision checks. No newly researched spell scans chunks or runs a persistent effect tick handler; potion effects are Minecraft's own bounded effect system. Existing ritual group behavior is otherwise preserved. Packet bytes/TPS/large-scene cost still require actual profiling; source bounds alone are not a benchmark.

Knowledge uses a versioned player NBT bitmask of exactly4advanced spells, deep-copied on death. Selection is item-owned. Station inventory uses standard container NBT. No global research state or remote settlement/world simulation is introduced.

## Upstream research and provenance

Reviewed Ars Nouveau branch1.21.x at `8f0faf35e95dcad4578f33181779e5387b29dd3d`: license.txt(LGPLv3), ScribesTile, EnchantingApparatusTile, GlyphRecipe and Glyph learning code. Its separation of recipe-driven production and personal learned knowledge informed this independently written implementation. That checkout's version metadata mixes Minecraft1.21.0/ranges with NeoForge21.1.13; it is not treated as a drop-in1.21.1 API reference. Installed Minecraft1.21.1/NeoForge21.1.264 APIs and executed builds/tests determine compatibility here. No upstream source, textures, models or dependencies were copied. New pixel resources/geometry are original; model references resolve installed vanilla textures without redistributing them.

## Verification status

Source checkpoint:full build19JUnit tests,78/78required GameTests,141JSON/144bilingual resource checks passed. Ten new magic GameTests exercise real station transactions/discovery gates/costs/concurrency, rejected remote/full/low-mana requests, folio use, save/clone/isolation, selected-spell persistence and unlock authority, all four advanced effects, walls, no-load/no-ticker behavior, actual recipe codecs/sync/browser and personal journal/payload state. Graphical flow and real-server cost measurements follow separately. This is a finite research tier, not the complete eventual magic specification.
