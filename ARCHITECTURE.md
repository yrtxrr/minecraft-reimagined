# Architecture

Single entry point: `dev.reimagined.Reimagined`. Each module registers its own deferred registries through the mod event bus; gameplay hooks use the NeoForge game bus. Parallel contributors own separate package/resource paths; shared entry point and build files are integrated centrally. No upstream gameplay-mod code is embedded.

| Module | Responsibility | State / authority |
|---|---|---|
| `season` | 96-day calendar, local crop growth, calendar commands | Overworld SavedData stores versioned offset; overworld day time is authoritative |
| `progression` | 10 skills, uncapped XP/levels, finite mastery | Player persistent NBT, decimal BigInteger, copied on player clone; server event XP and commands |
| `engineering` | water flow, axial connectivity, bounded load, milling | Server tick every 20 ticks staggered by position; block entity NBT inventory/progress; WorldlyContainer plus sided IItemHandler, input top/sides and output bottom |
| `content` | weapons/specials/parry, four wand schools | Server use/damage events, bounded mana, shared saved special/parry cooldowns copied on death |
| `research` | readable journal and personal observations | Versioned player NBT, 256 total unique registry IDs, deep-copied on clone; every200 player ticks/current book use, 6-block local scan with at most8 visibility traces |
| `creature` | Deer entity and natural spawning | Vanilla entity persistence/pathfinding; client model registration isolated by Dist.CLIENT |
| `browser` | common display/query/cache/adapter API, brewing and smithing projection | Immutable recipe snapshots, actual Ingredient predicates, exact potion variants, no client-only imports |
| `client` | existing inventory item panel and recipe screen | Read-only common projections; recipe-sync invalidation, world/logout lifecycle, no client crafting authority |
| data `worldgen` | optional world preset | Stateless seeded chunk noise, built-in registry codecs |

## Network

`reimagined:calendar` is a version-1 clientbound payload with a single varlong day. Server pushes on login and every 100 ticks and after operator season changes. The handler only updates a primitive value cache, with no common-side references to client-only classes. The HUD is client-only. The research journal uses standard written-book data components and vanilla held-slot/OpenBook packets, explicitly syncing pages before opening; no additional writable client research state exists. Vanilla networking handles entity state, item use, player messages and recipe synchronization. Skill inspection is via server-generated command output, not a writable client cache.

## Shared recipe contract

New process recipes implement vanilla `Recipe<RecipeInput>` and register a `RecipeType` and `RecipeSerializer` with Mojang MapCodec and network StreamCodec. `MillingRecipe` is the first custom implementation: ingredient, result stack, processing seconds. This uses existing data-pack reload/sync rather than a second recipe database. Future smithing, alchemy and cooking processors must use the same contract. Not all future systems exist yet. Vanilla brewing now has a public-API projection and smithing transforms use the public recipe codec. Dynamic special crafting and opaque custom brewing predicates still require adapters; see docs/RECIPE_BROWSER.md.

## Performance and boundaries

No world-wide tick scans. Mechanical graphs inspect at most 128 loaded positions and never deliberately load chunks. Crop changes run only in existing random growth events; greenhouse checks stay in one loaded column. Deer uses standard bounded AI goals. Remote civilization simulation, mechanical caching across large networks and climate weather effects are future work; no placeholder simulation is presented as complete.

Player state is server-owned. There is no state-level player kingdom creation API and no NPC war terrain destruction. Both civilizations and wars remain unimplemented.
