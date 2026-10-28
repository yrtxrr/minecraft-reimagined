# Remaining specification: proposed priorities after Freebuff

Review based on docs/USER_SPEC_RU.md and the implemented sources/ROADMAP at this integration. This is a proposal, **not the start of another mechanic**. This proposal predates the subsequent Freebuff/Kilo content integration, now recorded in docs/AGENT_CONTENT_INTEGRATION.md. No new major mechanic has been started; future allocations require a new user task.

| Priority | Direction and spec sections | First bounded playable increment | Scope guard |
|---|---|---|---|
|1|Exploration/dungeons (§16)|One original small room-connected ruined sanctuary: readable clues, optional puzzle/secret, ordinary breakable blocks, guarded reward and a real reason to explore.|Use Minecraft structure/worldgen pipeline and bounded piece counts; avoid a giant dungeon engine or another manufacturing chain.|
|2|Living regional fauna (§7,18)|One distinct aquatic or biome-specific creature with natural placement, habitat behavior, useful interaction and original animations; new fish items are not swimming animals.|Local vanilla AI goals/events and ordinary population caps; no global ecosystem scan.|
|3|World geography/climate/geology (§4,5,14)|A first real regional resource/stone geography increment with deterministic biome/terrain-compatible placement and a player-readable prospecting clue.|Retain vanilla world option/mining/smelting; do not claim the current noise preset already supplies watersheds, deltas or full climate regions.|
|4|Settlements/NPC contact (§6)|A small geographically placed NPC outpost with a functional trade/reputation interaction as the entry to civilization content.|No state founding, permanent remote-tick simulation, war/army engine or full economy in the first increment.|
|5|Practical transport (§12)|One useful animal-drawn cargo cart with a clear loading/travel/unloading loop and save/client support.|Start with one bounded transport entity; avoid introducing caravans, ship simulation and all logistics machinery at once.|

## Large gaps still open

- World generation remains short of true watershed rivers/deltas, climate regions, regional geology/deposits, ocean currents/archipelagos and geography-aware cities.
- Civilizations/citizenship/reputation/diplomacy/armies and unloaded-region simulation are not complete; neither are caravans/ships/linked minecarts/roads/economy.
- Room-connected dungeons/puzzles/mini-bosses, Nether/End biome and boss/artifact expansions, and broad aquatic fauna remain open. Existing deer/boar and three fish items do not satisfy all creature/ocean requirements.
- Skills have working progression foundations but not all rich finite talent trees/UI; the four magic schools and first research tier do not exhaust magic. Existing steel/forge and basic mechanical network are implemented; further engineering drives/machines remain, but need not take priority over world variety.
- Seasonal barley/pears, ordinary animal breeding and the cooking pot exist; fertility/fertilizers/greenhouses and broader husbandry remain partial. Additional cooking tiers are not recommended now.
- Recipe browser has item search/categories/recipes/usages/brewing/reload coverage, while remaining dynamic adapters/transfer/accessibility need future focused work.
- Release gates remain: full natural survival playthrough, broader long-session/scale profiling, physical GPU/audio, authenticated multiplayer and standalone packaged-JAR validation. Source/GameTests and focused developer-client checks do not certify a complete playable Alpha or full specification.
