# R8: матрица совместимости

Все значения относятся к проверенным исходникам, если явно не указан бинарный релиз. Ни один JAR аддона не подтверждён и не установлен. Полные source URL/revision/поля публикации — в pack/r8/addon-research.json.

| № | Кандидат | Статус | Версия в источнике (НЕ pin) | Ограничения / блокер |
|---|---|---|---|---|
| 1 | Create: Aeronautics | UNVERIFIED | 1.3.0+mc1.21.1 | Indirect integration declarations in CreateAddition1.7.2: Modrinth create-aeronautics, Sable2.0.3; EnchantmentIndustry declares eriksonn/aeronautics, simulated and offroad coordinates. Official Aeronautics artifact/author ownership/Sable closure not confirmed. |
| 2 | Aeronautics: Gadgets & Gizmos | UNVERIFIED | Не подтверждена | Exact official project identity, aircraft/API/Sable version constraints unknown; no replacement guessed. |
| 3 | Create: Airliners | UNVERIFIED | Не подтверждена | Exact official project identity and Aeronautics dependency not confirmed. |
| 4 | Create: Steam ’n’ Rails | DEFERRED | 1.7.3-beta | Reviewed official source/default1.20/dev and branch list target1.20.1; create-v6 branch is also1.20, not proof of NeoForge1.21.1. No matching binary confirmed. |
| 5 | Create: Enchantment Industry | UNVERIFIED | 2.5.4b | Source MC1.21.1 NF>=21.1.228 Create>=6.0.10; required CreateDragonsPlus>=1.11.9. Sable>=2.0.3 is OPTIONAL integration, not required by default. |
| 6 | Create: Slice & Dice | UNVERIFIED | Не подтверждена | Official source/project identity not confirmed in accessible sources; guessed Git URLs failed and are NOT official-source evidence. |
| 7 | Create Crafts & Additions | UNVERIFIED | 1.7.2 | Source MC1.21.1 NF>=21.1.248 Create[6.0.7,6.1.0); compiled Create6.0.10-280. JarJar SableCompanion1.6.0; Sable2.0.3/Aeronautics1.3.0 optional dev/integration declarations. |
| 8 | Create: Diesel Generators | UNVERIFIED | 1.21.1-1.3.15 | Source MC1.21.1 NF>=21.1.174 Create[6.0.7,6.1.0), build6.0.10-280. |
| 9 | Mekanism Generators | UNVERIFIED | 10.7.19 (build suffix unconfirmed) | Official release/1.21.x; exact [file.jarVersion] dependency on baseMekanism. Never mix module/base build numbers. |
| 10 | Mekanism Tools | UNVERIFIED | 10.7.19 (build suffix unconfirmed) | Same exact-version module rule; MC1.21.1 NF>=21.1.194. |
| 11 | Applied Mekanistics | UNVERIFIED | 1.6.0 fallback source version | MC1.21.1; AE2[19.2.10,20.0.0), Mekanism[10.7.14,11-). APPMEK_VERSION environment can override source version. No release guessed. |
| 12 | Extended AE | UNVERIFIED | 1.21-2.2.42-neoforge | Explicit1.21.1-neoforge branch NF>=21.1.194, GuideME21.1.1/AE2WirelessTerminalAPI JarJar require actual descriptor audit. Default26.1.2 is not our source. |
| 13 | Expanded Delight | UNVERIFIED | 0.1.4 | Source MC1.21.1, buildFD1.21.1-1.2.6 but descriptor FarmersDelight1.21-1.2.4. Placeholder neo_version_range appears undeclared in gradle.properties; published JAR must be checked. MIT declaration, template license is not full mod license. |
| 14 | Thaumcraft 4: Reforged | DEFERRED | 1.1.2 source | Exact user-selected notlikethiss port, mc/1.21.1 dffee19a4f7f82e0538d323d5de403883db1fe0c; TerraBlender/Curios; local build blocked by403. Personal-only redistribution caveat. |
| 15 | Botania Revived | DEFERRED | 1.0.4 source | Exact Velron-Devs port8da09edaf6bd5ab454899f6306560baf93c79bae; MC1.21.1 NF>=21.1.240, Patchouli>=93; builds previously blocked403. Addon ABI differs from originalBotania. |
| 16 | Mekanism Additions | UNVERIFIED | 10.7.19 (build suffix unconfirmed) | Same exact-version module rule; new mob/balloon persistence and collision checks required. |
| 17 | My Nether’s Delight | UNVERIFIED | 1.10.4 | Explicit Neo-1.21+ branch MC1.21.1 NF>=21.1.4, FD build1.21.1-1.3.2. Default1.20.1 backport must not be installed. |
| 18 | End’s Delight | UNVERIFIED | Не подтверждена | Official project/release identity not confirmed; guessed Git URL failed, not evidence no release exists. |
| 19 | Ocean’s Delight | UNVERIFIED | Не подтверждена | Official project/release identity not confirmed; guessed Git URL failed, not evidence no release exists. |
| 20 | MythicBotany | DEFERRED | 4.0 source | Reviewed upstream master targets Forge1.20.1/Botania1.20.1-443-FORGE. This source incompatible with target; compatible1.21.1 binary/port and BotaniaRevived ABI not confirmed. |

Для Create6.0.10 приведённые диапазоныC&A/Diesel/Industry предварительно подходят математически. Без фактического lock, jar metadata и запуска это не VERIFIED_METADATA. Для Aeronautics/Sable/Gadgets/Airliners соответствие не подтверждено. Модульные версииMekanism должны совпадать полностью. Статус DEFERRED для проверенного старого источника не является утверждением об отсутствии любых новых портов.
