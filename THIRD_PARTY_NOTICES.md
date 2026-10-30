# Third-party notices

Project gameplay code, deer geometry and deer pixel texture were written for this project. No Create, TerraFirmaCraft, Farmer's Delight, MineColonies, Alex's Mobs, Terralith, Tectonic, YUNG, JEI/EMI or Ars Nouveau code/assets are incorporated.

- **Gradle wrapper scripts/JAR**, Gradle contributors, Apache License 2.0. Upstream https://github.com/gradle/gradle/tree/v9.2.1. The scripts retain their copyright/license header; distribution and wrapper JAR are checked against upstream SHA-256. Full license: `docs/licenses/GRADLE-APACHE-2.0.txt`.
- **NeoForge / ModDevGradle** are external build/runtime dependencies, not vendored source. See https://github.com/neoforged/NeoForge (LGPL-2.1-only) and https://github.com/neoforged/ModDevGradle (LGPL-2.1-only) for authoritative license files.
- **JUnit 5**, Eclipse Public License 2.0, test-only dependency. https://github.com/junit-team/junit5. Not packaged as gameplay code.
- **Minecraft** remains proprietary Mojang/Microsoft software and is not redistributed in this repository. Asset models reference vanilla texture/resource identifiers resolved from the installed game; referenced vanilla textures are not copied into this repository. Generated development artifacts stay ignored. Development and use remain subject to Minecraft's terms and EULA.
- The cloud bootstrap downloads **Oracle JDK 21.0.8**, separately from the mod, from Oracle's official archive and checks its published checksum. Its bundled license and release-specific Oracle terms apply; it is not redistributed in the mod. Developers may instead use another licensed JDK 21. https://www.oracle.com/java/technologies/javase/jdk21-archive-downloads.html

Pack mods are **not** redistributed by this repository. `pack/mods.json` pins official Modrinth CDN URLs and SHA-512 hashes; `scripts/fetch-modpack.py` downloads them to an ignored local `mods/` directory for the user's own single-player installation. Each mod keeps its own licence, including all-rights-reserved and no-derivatives terms; downloading for personal use is not redistribution. Pack entries added as required libraries:

- **Apollo's Library** 1.2.0 (Modrinth `apollib`), MIT. Required by Lithostitched/Tectonic. https://modrinth.com/mod/apollib
- **Moonlight Lib** 1.21.1-3.7.2 (Modrinth `moonlight`), LGPL-3.0 with an additional dependency clause. Required by Immersive Weathering: Renewed. https://modrinth.com/mod/moonlight

Their metadata (`META-INF/neoforge.mods.toml`, licence id, file hash) was read from the downloaded jars; no source or assets are copied into this repository, and the mod does not depend on them.

Current original project files are All Rights Reserved pending the owner's license choice. This is not a grant to redistribute third-party software beyond its own terms.

Mechanical implementation research: Create commit `7ff2760e4924d35d4bc2e22c4b19923f0178cd16`, code MIT, assets all rights reserved. No source/assets copied; reviewed patterns and compatibility are recorded in docs/MECHANICAL_SYSTEM.md. The native NeoForge renderer requires no Create/Flywheel dependency.

Barley research: Farmer's Delight commit `dd8b9b0bfec916a2da88a6d64e3a37f4a02c3259`, MIT (vectorwing). No copied code/assets; vanilla CropBlock and the installed NeoForge AddTableLootModifier API are used directly. Barley pixel textures are original project artwork. See docs/AGRICULTURE.md.

Dynamic browser research: JEI Minecraft1.21.1 commit `7d70c027c9a03901afb74bf3e4d40638a7a7a383`, MIT (mezz), reviewed `Library/src/main/java/mezz/jei/library/plugins/vanilla/crafting/replacers/ShulkerBoxColoringRecipeMaker.java` and LICENSE.txt. Its finite concrete-replacement pattern informed the independently written, vanilla-assembly-based ShulkerColorDisplays adapter. No JEI source/assets are copied and no JEI dependency is added.

The packed-chunk cache index uses fastutil's primitive-long map API, already supplied by Minecraft (Apache License2.0, https://github.com/vigna/fastutil). No fastutil source or separate library is bundled in the mod, and no additional mod dependency is required. Non-requesting access uses Minecraft/NeoForge's existing getChunkNow API after reviewing the pinned1.21.1 implementation; no upstream implementation was vendored.

Cooking research: Farmer's Delight Minecraft1.21.1 / NeoForge21.1 branch at `dd8b9b0bfec916a2da88a6d64e3a37f4a02c3259`, MIT(vectorwing). Reviewed LICENSE, CookingPotBlockEntity/CookingPotRecipe and version metadata through the configured Git HTTPS access. No code/assets copied; original pot, GUI and food pixel resources and platform recipe/menu APIs are used. See docs/COOKING.md. No mandatory gameplay dependency added.

Magic research: Ars Nouveau at `8f0faf35e95dcad4578f33181779e5387b29dd3d`(1.21.x branch), license.txt LGPLv3. Reviewed scribes/apparatus/recipe/personal glyph architecture, not incorporated code. Its mixed version metadata is recorded in docs/MAGIC_RESEARCH.md; compatibility was checked against installed1.21.1/21.1.264 APIs. No copied code/assets or new mandatory dependency. New station, rune, focus, folio and GUI artwork is original.

Pinned magic research source: https://github.com/baileyholl/Ars-Nouveau/tree/8f0faf35e95dcad4578f33181779e5387b29dd3d ; license: https://github.com/baileyholl/Ars-Nouveau/blob/8f0faf35e95dcad4578f33181779e5387b29dd3d/license.txt . Architecture study only; no incorporated upstream component.

Smithing research:TerraFirmaCraft at `72750e42f3e0e8b337400ff1b8468a7ff7bc786d`, LICENSE.txt EUPL1.2. Reviewed AnvilRecipe and ForgingBonus/item component architecture; no source/assets incorporated and no dependency added. New ingot/blend/bellows/forge/UI artwork is original; vanilla textures are referenced by resource ID only. https://github.com/TerraFirmaCraft/TerraFirmaCraft/tree/72750e42f3e0e8b337400ff1b8468a7ff7bc786d . Version/API and repair edge cases:docs/SMITHING.md.
