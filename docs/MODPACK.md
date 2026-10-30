# Integration scope note

This file records Freebuff branch evidence. integration/modpack-agents-21cdd49 keeps those pins/history and fixes the old-NPC/abstract-civilization SavedData key collision with one owner. Current installer uses pack/mods.json; client/two-player/joint external runtime must be checked independently, not inferred from Freebuff99tests.

# Minecraft: Reimagined — single-player modpack

This is a personal single-player build, not a public-server platform. The pack is assembled
from external NeoForge 1.21.1 mods plus the locally built `reimagined` integration mod.

## Pinned environment

- Minecraft **1.21.1**, NeoForge **21.1.264**, Java **21**.
- Reimagined mod version `0.1.0-alpha` (build it locally, see the root README).
- Mod list and exact files: [`pack/mods.json`](../pack/mods.json). Every entry pins a
  Modrinth version ID, filename, official CDN URL and SHA-512.
- Fetch the required files with:

```sh
python scripts/fetch-modpack.py --destination <your-instance>/mods
# Optional, deliberately opt-in:
python scripts/fetch-modpack.py --destination <your-instance>/mods --optional
```

The script downloads only from the official CDN, verifies SHA-512 and never bundles mod
files in this repository. `pack/mods/` and `run*/` are ignored. Licences differ per mod
(including all-rights-reserved and no-derivatives); personal use download is not
redistribution. See [THIRD_PARTY_NOTICES.md](../THIRD_PARTY_NOTICES.md).

## What is in the pack

| Role | Mod | Pinned version | Licence | Notes |
|---|---|---|---|---|
| Overworld | Terralith | 2.6.2 (2026-06-09) | Stardust Labs licence | ~100 biomes; requires Lithostitched |
| Overworld | Tectonic | 3.0.28-neoforge-21.1 (2026-09-11) | MIT | terrain shaping; requires Lithostitched |
| Library | Lithostitched | 1.8.1-neoforge-21.1 | MIT | required by Terralith/Tectonic |
| Library | Apollo's Library | 1.2.0-neoforge-21.1 | MIT | required by Lithostitched ≥1.2.0 and Tectonic ≥1.1.6 |
| Library | Moonlight Lib | 1.21.1-3.7.2 | LGPL-3.0 with an additional dependency clause | required by Immersive Weathering: Renewed |
| Ecology | Naturalist | 2.0.7 (2026-10-21) | custom | new animals/behaviour |
| Ecology | Serene Seasons | 10.1.0.9 (2026-09-05) | all rights reserved | requires GlitchCore |
| Library | GlitchCore | 2.1.0.2 | all rights reserved | Serene Seasons dependency |
| Ecology | Immersive Weathering: Renewed | V1.0.4-NeoForge beta (2026-05-09) | LGPL-3.0 | maintained NeoForge fork; beta |
| Ecology | Farmer's Delight | 1.21.1-1.3.4 (2026-08-29) | MIT | food/farming content |
| Nether | Incendium | 5.4.4 (2024-12-04, 1.21.x) | Stardust Labs licence | Nether biomes/structures |
| End | Nullscape | 1.2.14 (2025-10-07, 1.21.x) | Stardust Labs licence | verified End alternative |
| Structures | YUNG's Better Dungeons / Mineshafts / Strongholds | 5.1.4 / 5.1.1 / 5.1.3 | LGPL-3.0 | require YUNG's API 5.1.9 |
| Structures | Better Archeology | 1.3.9 (2026-10-08) | MIT | requires Architectury + Resourceful Config |
| Structures | L_Ender's Cataclysm | 3.33 (2026-09-20) | CC-BY-NC-ND-4.0 | requires Lionfish API + Curios |
| Civilization | MCA Reborn | 7.7.36-beta.3 (2026-08-16) | GPL-3.0 | NPC villagers/families |
| Civilization | Guard Villagers | 2.4.12 (2026-08-30) | custom | village guards |
| Optional ecology | Cold Sweat | 2.4.4 (2026-10-16) | GPL-3.0-or-later | harsher climate; off by default |

### Load-blocking dependencies found by validation

The first manifest was assembled from the mods the user asked for, so it contained Terralith,
Tectonic, Lithostitched and Immersive Weathering but not what those jars require at runtime.
Reading every `META-INF/neoforge.mods.toml` found two real gaps: **Apollo's Library** (required
by Lithostitched `[1.2.0,)` and Tectonic `[1.1.6,)`) and **Moonlight Lib** (required by
Immersive Weathering `[1.21-2.29.33,)`). A pack built from the previous manifest would have
failed to load. Both are now pinned with verified hashes; nothing else outside
Minecraft/NeoForge was missing.

### Deliberately excluded

- **BetterEnd (original, Modrinth `betterend`)**: on 1.21.1 it publishes Fabric builds only;
  a filtered query returned **zero** NeoForge 1.21.1 versions. It cannot be loaded by this
  NeoForge pack, and none of its features are claimed as implemented. Nullscape is the
  verified alternative for End terrain, but it does not supply BetterEnd's plants, mobs or
  progression.
- **Immersive Weathering (original)**: also Fabric/Forge only on 1.21.1 (zero NeoForge
  builds). The maintained **Renewed** fork is pinned instead.
- **Serene Seasons / Terralith / Incendium / Nullscape / Cataclysm** are not redistributed
  here because their licences do not permit bundling.

## World creation

1. Install NeoForge 21.1.264 and put the files from `pack/mods.json` plus the built
   `reimagined-0.1.0-alpha.jar` into `mods/`.
2. Create the world with the **Default** world type.
   - Terralith replaces/augments the vanilla Overworld biome source; Tectonic reshapes
     vanilla terrain. Their official pages state that the *mod* versions of Terralith and
     Tectonic work together; do not mix the datapack versions (Tectonic datapack needs
     Terratonic).
   - The Reimagined `reimagined:continents` preset remains selectable for vanilla-plus
     experiments, but combining our custom noise settings with Terralith's biome source has
     **not** been tested. Do not use it for the main pack world yet.
3. Accept the Minecraft EULA yourself for a dedicated server. Nothing here auto-accepts it.

## Offline validation of an installed pack

```sh
python scripts/validate-modpack.py --mods <your-instance>/mods
```

It checks that every required pinned file exists, that its SHA-512 matches the manifest, that
the jar exposes a `modId`/version, and that every required non-loader dependency is supplied by
another pinned mod. It never uses the network, and it does **not** evaluate NeoForge version
ranges (Maven ordering is not reproduced); a present dependency is reported as present, not as
range-compatible. Optional mods that are absent are notes, not failures.

Current result with all 25 files downloaded: **25 jars validated, 12 non-loader required
dependencies, all resolved, 0 problems**. `Incendium_1.21.x_v5.4.4.jar` declares internal
`version=5.4.3` and MCA declares `7.7.36-beta.3+1.21.1`, so the jar name and manifest version
differ from the internal string; the file hashes are the authoritative pin.

## Verified compatibility facts

- Inspected `Terralith_1.21.1_v2.6.2_Neoforge.jar`: it declares `lithostitched` as a
  required dependency (`>=1.7.7`), ships `pack_format` 48/61, and adds its biomes to the
  vanilla tags `minecraft:is_forest`, `minecraft:is_taiga`, `minecraft:is_mountain` and
  `minecraft:is_overworld`.
- Deer spawning was therefore changed from a hardcoded vanilla biome list to
  `#minecraft:is_forest`, `#minecraft:is_taiga` and `minecraft:meadow`, split across three
  biome-modifier files because NeoForge's `add_spawns` codec accepts one tag string or a
  list of biome IDs, not a list of tags. Terralith forests/taigas now inherit deer spawns.
- The existing rare mountain iron vein already targets `#minecraft:is_mountain`, so it also
  applies to Terralith mountain biomes. It remains a small biome-local vein, not a
  geological province.
- All 25 pinned jars were downloaded from the official CDN and matched their SHA-512; their
  required dependency closure is complete (see above). This is metadata and file-integrity
  evidence only.
- These are data/metadata checks, not a launched pack. Terrain quality, structure overlap
  and spawn balance must be checked in a real world (see the checklist below).

## Known integration limits

- **Two season systems.** Serene Seasons has its own calendar and crop rules. When it is
  loaded, `Seasons.grow` now returns early so our seasonal crop modifier is not stacked on
  top of Serene Seasons. The Reimagined HUD still shows the Reimagined 96-day calendar, so
  the two displayed seasons can disagree. Recommended for now: treat Serene Seasons as the
  world's season authority; unify the calendars before advertising them together.
- **MCA Reborn** replaces village life with its own villagers. The current civilization
  prototype is abstract data only and spawns no villagers or buildings, so there is no
  duplication today. A future local settlement tier must integrate MCA/Guard Villagers
  instead of spawning a second population.
- **Naturalist** adds fauna independently. Spawn weights have not been balanced against
  deer or vanilla animals.
- **Cold Sweat** is opt-in because the project explicitly avoids exhausting survival
  mechanics. Do not enable it for a relaxed playthrough without tuning.
- **Cataclysm** is large (73 MB) and adds bosses/structures; no structure-spacing
  integration with YUNG's or Terralith has been measured.

## In-game verification checklist (not yet executed)

1. Create a fresh Default world with the full pack; confirm no registry/load errors and
   note the seed.
2. Sample at least five distant regions: Terralith biome variety, Tectonic terrain, ocean
   and mountain transitions; confirm our mountain iron vein appears only in mountain-tagged
   biomes.
3. Confirm deer and Naturalist animals appear in Terralith forests/taigas; check spawn
   density and whether Naturalist animals dominate.
4. Enter the Nether: Incendium biomes/structures load; piglin settlements exist; no
   structure overlap crashes.
5. Enter the End after the dragon: Nullscape biomes load; record what post-dragon content
   is actually present (Nullscape alone is terrain, not a full progression).
6. Check village/MCA behaviour and Guard Villagers spawning.
7. Run a 30-minute session with JFR or `/jfr` and record chunk-generation spikes during
   flight/exploration; Terralith/Tectonic are data-driven but still expensive on one PC.
