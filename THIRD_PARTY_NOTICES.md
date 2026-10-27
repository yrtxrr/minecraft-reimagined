# Third-party notices

Project gameplay code, deer geometry and deer pixel texture were written for this project. No Create, TerraFirmaCraft, Farmer's Delight, MineColonies, Alex's Mobs, Terralith, Tectonic, YUNG, JEI/EMI or Ars Nouveau code/assets are incorporated.

- **Gradle wrapper scripts/JAR**, Gradle contributors, Apache License 2.0. Upstream https://github.com/gradle/gradle/tree/v9.2.1. The scripts retain their copyright/license header; distribution and wrapper JAR are checked against upstream SHA-256. Full license: `docs/licenses/GRADLE-APACHE-2.0.txt`.
- **NeoForge / ModDevGradle** are external build/runtime dependencies, not vendored source. See https://github.com/neoforged/NeoForge (LGPL-2.1-only) and https://github.com/neoforged/ModDevGradle (LGPL-2.1-only) for authoritative license files.
- **JUnit 5**, Eclipse Public License 2.0, test-only dependency. https://github.com/junit-team/junit5. Not packaged as gameplay code.
- **Minecraft** remains proprietary Mojang/Microsoft software and is not redistributed in this repository. Asset models reference vanilla texture/resource identifiers resolved from the installed game; referenced vanilla textures are not copied into this repository. Generated development artifacts stay ignored. Development and use remain subject to Minecraft's terms and EULA.
- The cloud bootstrap downloads **Oracle JDK 21.0.8**, separately from the mod, from Oracle's official archive and checks its published checksum. Its bundled license and release-specific Oracle terms apply; it is not redistributed in the mod. Developers may instead use another licensed JDK 21. https://www.oracle.com/java/technologies/javase/jdk21-archive-downloads.html

Current original project files are All Rights Reserved pending the owner's license choice. This is not a grant to redistribute third-party software beyond its own terms.

Mechanical implementation research: Create commit `7ff2760e4924d35d4bc2e22c4b19923f0178cd16`, code MIT, assets all rights reserved. No source/assets copied; reviewed patterns and compatibility are recorded in docs/MECHANICAL_SYSTEM.md. The native NeoForge renderer requires no Create/Flywheel dependency.

Barley research: Farmer's Delight commit `dd8b9b0bfec916a2da88a6d64e3a37f4a02c3259`, MIT (vectorwing). No copied code/assets; vanilla CropBlock and the installed NeoForge AddTableLootModifier API are used directly. Barley pixel textures are original project artwork. See docs/AGRICULTURE.md.
