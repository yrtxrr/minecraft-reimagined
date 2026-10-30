# World and ecology design

Personal single-player build. The goal is a varied, living world that stays pleasant to run on
one computer. This document separates what exists in source from what is only planned.

## 1. Audit: what is reused

| Existing system | Location | Reuse decision |
|---|---|---|
| Optional continental preset | `data/reimagined/worldgen`, `docs/WORLDGEN.md` | Kept selectable; not the pack's default while Terralith/Tectonic are installed |
| Deer entity, model, spawns | `creature` | Kept; spawns broadened through vanilla biome tags |
| Rare mountain iron vein | `neoforge/biome_modifier/mountain_iron_vein.json` | Kept; tag-driven, so it covers Terralith mountains |
| Mossy forest boulders | `neoforge/biome_modifier/mossy_forest_boulders.json` | Kept; forest-tag driven |
| 96-day seasons, crop growth, glass shelter, HUD | `season` | Kept; defers crop growth to Serene Seasons when that mod is loaded |
| Barley, meals, fishing | `agriculture`, `cooking`, `fishing` | Kept unchanged |
| Deer spawning / Naturalist | external | Naturalist adds its own fauna; no block/entity IDs are duplicated |
| Villages and trading | vanilla + MCA Reborn + Guard Villagers | Vanilla villages untouched; abstract settlement prototype adds no entities |

Nothing was deleted. No existing noise function, biome, structure or spawn file was replaced.

## 2. Terrain stack

- **Terralith 2.6.2** supplies biomes, cave types, features and structures.
- **Tectonic 3.0.28** supplies continental/ocean terrain shaping.
- **Lithostitched 1.8.1** is their required injection library.

The official Terralith page states that Terralith is compatible with the **mod** version of
Tectonic, and that datapack users need Terratonic instead. The pack therefore uses mod jars and
does not install Tectonic as a datapack. Both jars were verified to require Lithostitched.

### Our preset versus the pack

`reimagined:continents` is an original noise preset with its own continental/erosion fields,
river bands, caves, surface rules and ores. It was designed before this pack. Terralith injects
biomes into the vanilla multi-noise biome source; our preset also uses that source with custom
noise settings. The combination has **not** been sampled in a real world. Until that is done:

- main pack worlds use the **Default** world type;
- `reimagined:continents` stays available as a separate experiment and is not advertised as
  Terralith/Tectonic-compatible.

### Chunk-generation cost

Terralith/Tectonic are data-driven worldgen: their cost is Minecraft's normal chunk pipeline,
not a mod tick loop. Reimagined adds only data-driven spawn/feature modifiers and no per-tick
terrain work. No chunk-generation benchmark has been run for the combined pack; flight-time
generation spikes are a required in-game measurement (see `WORLD_PERFORMANCE.md`).

## 3. Fauna and seasons

### Implemented now

- Deer spawn through three NeoForge biome modifiers: `#minecraft:is_forest`,
  `#minecraft:is_taiga` and `minecraft:meadow`. Terralith puts its forest/taiga biomes into
  those vanilla tags, so deer inherit its biomes without hardcoding third-party IDs.
- Naturalist supplies additional animals with its own spawn rules.
- The Reimagined calendar shortens temperate crop growth in winter and protects crops under a
  glass roof. When Serene Seasons is installed, that hook is skipped so two seasonal growth
  modifiers do not stack.
- Serene Seasons owns its own seasonal world effects; it is not reimplemented here.

### Designed, not implemented

- **Seasonal animal behaviour**: migration/fleeing/breeding windows driven by a small number of
  entity events and vanilla goals, never a world-wide ecosystem scan. Only loaded entities
  within the normal entity-ticking radius may be touched.
- **Regional fauna**: biome-tag-based spawn modifiers (as with deer) plus a small number of
  region rules derived from biome temperature/humidity. No new global spawner tick.
- **Climate effects**: prefer Serene Seasons' existing temperature/season data if a stable
  public API is verified; otherwise use local biome temperature and our calendar. No parallel
  climate simulation over unloaded chunks.
- **Agriculture/season interaction**: continue using the existing `CropGrowEvent` and
  Farmer's Delight crops; add no mandatory thirst, disease or micronutrient system.
- **Settlement environmental impact**: abstract consumption/production already exists; local
  effects (woodcutting, farmland, pollution) must be bounded, queued and disableable, and must
  never modify player-built blocks without an explicit rule.

## 4. Structures and exploration

YUNG's Better Dungeons/Mineshafts/Strongholds, Better Archeology and Cataclysm are installed
as content providers; their structures are not duplicated by this mod. The planned link between
exploration and progression is:

- ancient knowledge and blueprints found in structures feed the existing research journal and
  spell research;
- rare archaeology finds feed smithing/magic recipes;
- quest signals for first visits are emitted by the quest layer, not by rewriting structure
  generation.

None of the knowledge/blueprint integration or structure-visit quest signal is implemented yet.
Structures are optional content, not a mandatory linear campaign.

## 5. What must not be duplicated

- No second set of vanilla-like biomes while Terralith is installed.
- No replacement Overworld noise settings in the pack world.
- No custom villagers/animals that duplicate MCA Reborn, Guard Villagers or Naturalist.
- No per-tick ecosystem, weather or settlement scan.

## 6. Verification status

- Verified: pinned versions/dependencies/licences from the Modrinth API; Terralith jar
  dependency metadata and vanilla biome-tag membership; all 25 pinned pack files downloaded
  with matching SHA-512 and a complete required dependency closure
  (`scripts/validate-modpack.py`); the mod compiles and 99/99 headless GameTests pass with the
  tag-based deer modifiers loaded.
- Not verified: a launched modpack, terrain quality, structure overlap, spawn balance, season
  interaction in-game, or chunk-generation performance.
