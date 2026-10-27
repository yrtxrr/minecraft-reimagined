# Architecture

Single entry point: `dev.reimagined.Reimagined`. Each module registers its own deferred registries through the mod event bus; gameplay hooks use the NeoForge game bus. Parallel contributors own separate package/resource paths; shared entry point and build files are integrated centrally. No upstream gameplay-mod code is embedded.

| Module | Responsibility | State / authority |
|---|---|---|
| `season` | 96-day calendar, local crop growth, calendar commands | Overworld SavedData stores versioned offset; overworld day time is authoritative |
| `progression` | 10 skills, uncapped XP/levels, finite mastery | Player persistent NBT, decimal BigInteger, copied on player clone; server event XP and commands |
| `engineering` | water flow, axial connectivity, bounded load, milling | Server tick every 20 ticks staggered by position; block entity NBT inventory/progress; WorldlyContainer plus sided IItemHandler, input top/sides and output bottom |
| `content` | weapons/specials/parry, four wand schools | Server use/damage events, bounded mana, shared saved special/parry cooldowns copied on death |
| `creature` | Deer entity and natural spawning | Vanilla entity persistence/pathfinding; client model registration isolated by Dist.CLIENT |
| `client` | inventory item panel and recipe screen | Read-only synchronized recipe-manager data; no client item spawning/crafting authority |
| data `worldgen` | optional world preset | Stateless seeded chunk noise, built-in registry codecs |

## Network

`reimagined:calendar` is a version-1 clientbound payload with a single varlong day. Server pushes on login and every 100 ticks and after operator season changes. The handler only updates a primitive value cache, with no common-side references to client-only classes. The HUD is client-only. Vanilla networking handles entity state, item use, player messages and recipe synchronization. Skill inspection is via server-generated command output, not a writable client cache.

## Shared recipe contract

New process recipes implement vanilla `Recipe<RecipeInput>` and register a `RecipeType` and `RecipeSerializer` with Mojang MapCodec and network StreamCodec. `MillingRecipe` is the first custom implementation: ingredient, result stack, processing seconds. This uses existing data-pack reload/sync rather than a second recipe database. Future smithing, alchemy and cooking processors must use the same contract. Not all future systems exist yet; brewing and dynamic special crafting require dedicated display adapters before claiming universal browser coverage.

## Performance and boundaries

No world-wide tick scans. Mechanical graphs inspect at most 128 loaded positions and never deliberately load chunks. Crop changes run only in existing random growth events; greenhouse checks stay in one loaded column. Deer uses standard bounded AI goals. Remote civilization simulation, mechanical caching across large networks and climate weather effects are future work; no placeholder simulation is presented as complete.

Player state is server-owned. There is no state-level player kingdom creation API and no NPC war terrain destruction. Both civilizations and wars remain unimplemented.
