# Nether and End design

The pack keeps vanilla's three dimensions and the vanilla dragon/wither fights. External mods
supply most new terrain; this mod's job is integration, resources and progression signals.

## 1. Nether — Incendium

**Pinned:** Incendium 5.4.4 (`Incendium_1.21.x_v5.4.4.jar`), listed for 1.21.1 NeoForge, no
required dependencies, Stardust Labs licence. Install only the mod jar; do not also install a
datapack copy.

Intended role in the world:

- varied Nether regions and hazards;
- additional structures and ruins;
- piglin settlements and trade-route flavour (Incendium content, not ours);
- industrial resources (ores, fuels, Nether-specific materials) feeding the existing
  smithing/engineering systems;
- magic-research sites and anomalies as quest/research targets.

Reimagined will **not** edit Incendium's biome or structure JSON. Integration points are
additive: quest signals, research observations, recipes that accept Nether materials, and
bounded resource tags. None of the quest/research integration is implemented yet.

Compatibility caveats:

- Incendium changes Nether generation globally; other Nether structure mods (including any
  Cataclysm Nether content) can overlap. Structure spacing has not been measured in-game.
- Incendium is a 1.21.x build. It loaded its metadata as NeoForge 1.21.1, but no world has been
  generated with it in this checkout.

## 2. End — Nullscape instead of BetterEnd

**BetterEnd is not usable here.** A Modrinth query for BetterEnd (`betterend`) with
`game_versions=["1.21.1"]` and `loaders=["neoforge"]` returned **zero** versions; the project's
loader list is Fabric. Its plants, mobs, cities and progression are therefore **not**
implemented or claimed.

**Pinned alternative:** Nullscape 1.2.14 (`Nullscape_1.21.x_v1.2.14.jar`), 1.21.1 NeoForge,
no required dependencies, Stardust Labs licence. It provides End terrain/biome variety. It does
**not** provide BetterEnd's unique flora/fauna or a post-dragon quest line.

Intended End design (only terrain exists today):

- diverse End biomes and unusual resources through Nullscape and vanilla;
- ancient cities/ruins from vanilla and Cataclysm/other structure mods, not duplicated;
- spatial technologies and magic anomalies as future Reimagined systems tied to End materials;
- post-dragon progression: **not implemented**. No new End boss, portal network or progression
  gate exists in this mod; the dragon fight remains vanilla.

Any future End content must be optional side content, not a mandatory linear campaign.

## 3. Dimension-safe implementation rules

- No dimension-wide tick scans. If a dimension-specific mechanic is added, it must be
  event/local/chunk-owned and budgeted like the Overworld systems.
- No settlement/building simulation in the Nether or End until a dimension ownership and
  persistence design exists. The prototype is Overworld-only by construction.
- No chunk loading for resource or structure inspection. Locate/quest integration must use
  commands, structure events or already-loaded state.
- New dimension content must not replace vanilla structures or the dragon/wither.

## 4. Verification status

- Verified: pinned versions and loader availability from Modrinth; null NeoForge 1.21.1
  BetterEnd result; no dependency/licence blockers for personal installation.
- Not verified: actual Nether/End world generation, structure overlap, resource distribution,
  performance, or any post-dragon gameplay. The checklist is in `MODPACK.md`.
