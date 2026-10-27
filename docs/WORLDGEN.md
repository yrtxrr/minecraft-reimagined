# Reimagined Continents — initial world preset

Choose **Reimagined Continents** during world creation. A dedicated server can
select `level-type=reimagined:continents` before creating its world. The vanilla
world types remain available: the preset tag is additive (`replace: false`).
Changing a server's level type does not regenerate an existing world.

This preset uses the Minecraft 1.21.1 noise-generator and registry codecs. Its
Overworld has original, data-driven density functions and noise settings. The
Nether and End keep their vanilla settings. It does not replace global vanilla
noise functions or modify worlds created with another preset.

## Implemented terrain model

- Broad continental, temperature and humidity fields use a 0.0625 horizontal
  noise scale, compared with the vanilla Overworld's 0.25 scale.
- An original height function combines continental elevation, erosion and
  land-weighted ridges. Sea level is 63 and the build range is -64 to 319.
- Narrow noise-based drainage bands follow the same ridge field that the
  vanilla multi-noise biome source uses for river placement.
- A separate three-dimensional cave field and a reference to vanilla noodle
  caves make underground cavities. Aquifers and ore veins remain enabled.
- Surface rules provide sand, red sand, exposed stone, snowy peak surfaces,
  grass/dirt, gravel underwater, deepslate and a sealed bedrock floor.
- Existing vanilla biomes, vegetation, ore features and structures are supplied
  by Minecraft's registry data. The generator derives all noise from the world's
  seed; there is no mutable global random state or per-tick world simulation.

All distributed density functions, surface rules and JSON configurations are
original expressions. Vanilla registry IDs are runtime references; no third-party
mod code, textures or extracted Mojang configuration blobs are included.

## Scope and validation

This is an initial continental terrain preset, **not** the complete generation
system from the project specification. It does not yet implement physical
watersheds, connected hydrological rivers, lake/delta planning, new geological
strata, ore provinces, new biomes or geography-aware settlement placement.
Vanilla biome placement was designed for vanilla terrain; biome/elevation and
structure-placement tuning still needs in-game sampling across many seeds.

Static validation checks JSON syntax, local registry references, acyclic density
function dependencies and the additive preset tag. These checks do not prove
that Minecraft's codecs accept the resources or that terrain looks correct.
The required runtime check is to start a Minecraft 1.21.1 / NeoForge 21.1.264
server with this preset in a fresh disposable world, let spawn chunks generate,
then inspect several seeds in a client. Track the actual result in the root
README; do not treat this procedure as an already-passed test.
