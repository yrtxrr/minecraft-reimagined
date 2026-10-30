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


Pear orchard research: vanilla Minecraft 1.21.1 `TreeConfiguration`/`TreeGrower`/`LeavesBlock`/`SaplingBlock` and loot-table codecs were used directly through the installed NeoForge 21.1.264 APIs; vanilla oak/birch tree JSON served only as a schema reference and was not copied. No Farmer's Delight, TerraFirmaCraft, Create or other mod source/assets are incorporated and no mandatory dependency is added. All pear block/item pixel textures are original project artwork. See `docs/ORCHARD.md`.

Wild boar research: TerraFirmaCraft1.21.x at72750e42f3e0e8b337400ff1b8468a7ff7bc786d, EUPL1.2, WildAnimal/RammingPrey/Boar architecture reviewed only. No source, model, textures, sounds or mod dependency incorporated. The boar mesh and pixel texture are original. Installed Minecraft1.21.1 Animal/goal/render APIs are used, vanilla pig sound events referenced by ID. Sources/license: https://github.com/TerraFirmaCraft/TerraFirmaCraft/tree/72750e42f3e0e8b337400ff1b8468a7ff7bc786d and https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/72750e42f3e0e8b337400ff1b8468a7ff7bc786d/LICENSE.txt . Scope and differences:docs/WILDLIFE.md.

Freebuff fish integration retains4a71f92's three item textures/models and food/loot registration. No outside mod assets or implementation were imported during integration. Installed NeoForge21.1.264 `AddTableLootModifier` (LGPL2.1-only) was studied to verify additive/nonrecursive semantics; the implementation is used through its supported JSON codec, not copied. Native QA observes vanilla FishingHook state without changing gameplay; that dev-only probe is excluded from the player JAR. Details:docs/FISHING.md.

Remaining agent integration:Freebuffdf7a223 uses vanilla forest_rock and mossy-cobblestone references; Kiloafc5bd3/1835d41/25e4ea7 supplies thatch/clover/journal source. Added missing original project16pxthatch/clover pixels without external asset input. Installed Minecraft1.21.1 feature/flower/book APIs and NeoForge21.1.264 Compostable data-map codec were studied, not copied. No additional mod dependency. Author history retained; fixes/limits:docs/AGENT_CONTENT_INTEGRATION.md.

Kilo820e5db integration retains1002d09/18eeada/78e5134/820e5db source history and supplied otter/badger/marmot64pxtextures. Integration meshes/behavior corrections are independently written; installed Minecraft1.21.1 Animal, AmphibiousPathNavigation, vanilla goal, simple-block/random-patch and spawn-placement APIs were studied and called, not vendored. No additional external assets/code/mod dependency incorporated. Prior TerraFirmaCraft wildlife architecture research above remains background, not copied source. Scope/provenance limits:docs/KILO_FAUNA_INTEGRATION.md.

Kilo world-polish e9af309/65f11a0:author history retained, only QA extended. Existing project meal recipes/assets preserved byte-for-byte; no external code/assets or dependency incorporated. Installed NeoForge21.1.264 compostable item data-map API and Minecraft1.21.1 crop/cooking APIs are called in dev-only tests. Native scope:docs/KILO_WORLD_POLISH_INTEGRATION.md.

JEI migration preparation:official1.21.1source at4a9042f579f4602ff21827dd873cffe57329473d, MIT(mezz), version/platform/license metadata inspected through Git. No JEI source/assets vendored, no required dependency or JEI JAR bundled. Official Maven artifact fetch blocked by proxy CONNECT403; no binary/runtime/category compatibility claim. Details:docs/JEI_MIGRATION.md.


Deer visual update: original project mesh revised with the user-approved5×5×6head/5-unit neck and isolated antler UV. The retained review texture originates from an image_gen artistic concept made for this project, technically reprojected onto the project UV atlas with nearest-neighbour sampling and limited to27colors. Antler material reuses the original project deer texture. No outside mod texture/model/code or additional dependency incorporated. Installed Minecraft1.21.1 AgeableListModel/QuadrupedModel transforms were inspected and used through subclassing; not vendored. Approval and native coverage:docs/DEER_VISUAL_ACCEPTANCE.md.

Beekeeping research: vanilla Minecraft 1.21.1 `BeehiveBlock`, `BeehiveBlockEntity`, `Bee`/`NeutralMob`, `UseOnContext` and loot/recipe codecs were used directly through the installed NeoForge 21.1.264 APIs, together with NeoForge data attachments (`AttachmentType`), `PlayerInteractEvent.RightClickBlock` and `BlockEvent.BreakEvent`. No vanilla source is vendored and no custom hive block entity duplicates the vanilla colony logic. No Farmer's Delight, TerraFirmaCraft, Create or other mod source/data/assets are incorporated and no mandatory dependency is added. All beekeeping item/block pixel textures are original project artwork generated for this stage. See `docs/BEEKEEPING.md`.

Settlement trading safety: installed Minecraft1.21.1 MerchantMenu, MerchantResultSlot, MerchantOffers, Merchant and server offer/selection packet implementations were inspected and reused by inheritance and supported APIs. No vanilla source/assets vendored and no external mod dependency. Existing project professions and SavedData retained.

Settlement rendering uses Minecraft1.21.1 VillagerModel/ModelLayers.VILLAGER by subclassing and references minecraft:textures/entity/villager/villager.png in the installed game. No vanilla or third-party texture/model/source file copied into project assets, no extra mod dependency. Installed GroundPathNavigation/PathNavigationRegion were inspected to guard their exact loaded-chunk region before calling the vanilla implementation.

## Modpack migration2026-10-30

No third-party code/assets/JAR added by the Core foundation. Official catalog/source probes are blocked403; target candidate metadata/license fields remain unverified in modpack/mod-registry.json. All target names identify prospective upstream projects, not bundled dependencies. Personal use does not imply permission to redistribute. Local build archives include only existing Reimagined code/resources and original configuration/integration glue; external artifact lock requires explicit source/license/personal-use confirmation before packaging.

## Targeted quest-source research (2026-10-30)

Reviewed https://github.com/FTBTeam/FTB-Quests at dc94c46a6a9ae806fb5921159513448fcedc8816 (1.21.1/main, source2101.1.37) for its published native data/API contract. Reviewed https://github.com/AllTheMods/ATM-10 at827e3610c356922e59412834d4b08ca5b16a6ce0 for a working NeoForge1.21.1 schema13 quest configuration. ATM quest/script README is All Rights Reserved; no permission inferred, no quest text/reward balance/script/code/assets copied. Reimagined native chapter text/IDs/layout and vanilla advancement bridge are original project work using technical field names/contracts. No external binaries added via Git. Kilo cbc7a5f design docs retained as this project's attributed design material; unverified Java.type/registration code is not activated. See docs/QUEST_RUNTIME_INTEGRATION.md.

Reviewed PrismLauncher/PrismLauncher573665ffa1ccbd7e19df39c28193d26d5b202d60, ModrinthInstanceCreationTask.cpp (GPL-3.0-only), solely for the import format/API: SHA512 validator and NeoForge dependency key. No C++ code or launcher assets copied. Original Python exporter emits Reimagined-owned Core/config and factual official file references; no third-party JAR embedded.

### Official FTB Maven installation (2026-10-30)

FTB Quests source dc94c46a6a9ae806fb5921159513448fcedc8816:
https://github.com/FTBTeam/FTB-Quests/tree/dc94c46a6a9ae806fb5921159513448fcedc8816
Reviewed gradle.properties, neoforge/build.gradle (production remapJar vs dev classifier), publication/repository configuration and game descriptor. FTB Library f0c8d837fcabf6846da51e323f663db20300c364 and FTB Teams47e8c4faa797a19f41a7a351eb78ae89e41b4a26 confirm game IDs/dependency roles. Their newer source versions were NOT substituted for the Quests source's dependency versions. Official Maven is https://maven.ftb.dev/releases. All Rights Reserved: no code/assets/quest text copied; installer independently downloads authorized official artifacts locally, with source-reviewed coordinates, publication metadata, checksums and JAR checks. No FTB binary is bundled. Runtime compatibility/publication in this cloud remains unconfirmed. Future GitLab/Codeberg/developer-site sources require equally explicit provenance/hash/platform review; no generic mirror fallback was introduced.
