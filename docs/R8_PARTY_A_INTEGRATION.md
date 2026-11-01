# Интеграция партии A: замороженный кандидат для испытания

Основание: присланный пользователем Windows отчёт, SHA256 `c6ca7ee72cc3316bc0b7a29434b59fc18a6379f68158ff90519eeb584a596e46`. Он прочитан как данные, не инструкции. VERIFIED_METADATA не означает runtime; фактическиеJAR в облако не загружены.

| Мод | Публикация / игровойversion | Version ID | SHA256 |
|---|---|---|---|
| mekanism-generators | 10.7.19.85 / 10.7.19 | `a6gl7srE` | `0e5783b111e756f27b48c62b2f0e02fff750c77f7985ff809bfadc5f444ba4ac` |
| mekanism-tools | 10.7.19.85 / 10.7.19 | `v5zlSE9s` | `e1dbca7d70df150101da535f6ab4b165a2e402fde830d34df75b58d9aad24dd8` |
| mekanism-additions | 10.7.19.85 / 10.7.19 | `6mkdykZa` | `3d8a6ed67f5ccf3797ad9d5236ec4e22b74481aa9cd45636feb16f89e1d56311` |

Все три: MIT, commonclient/server, официальный source https://github.com/mekanism/Mekanism. Каталог требует base Ce6I4WUE / version5KzzycBT, как R7; реальныйdescriptor требует Mekanism[10.7.19], совпадает с настоящимbase_game_descriptor10.7.19. В R7зафиксирован полныйномерпубликации10.7.19.85. Эти поля не смешиваются.

План pack/r8/party-a/install-plan.json = исходные53записи безизменений +ровно3записи; original40target/constraints/dependencies сохранены. Утрёхновыхмодулей полеdependencies указываетединственный существующийключbaseMekanism. Новыйнабор56внешнихартефактов,51общий/5client-only. Runtimeflagfalse. Лицензии и заданныехеши проверены на согласованность поотчёту; реальное совпадениеSHA256/SHA512байтов повторяется вWindowsinstall перед созданиемпрофилей.

Клиент/сервер готовятся существующимinstaller без resolve/latest/новогоменеджерапинов. Новыйwrapper только композитит зафиксированныефайлы, перепроверяет SHA256 каждого из56файлов и Core266c7e..., защищаетнепустойoutput. ВнешниеJAR непубликуютсявsetupZIP. Profilemanifest/lockR7, архивы и существующиемиры не меняются.

Сборка Core/JUnit/GameTests в облаке безвнешних3JAR будет лишь базовойпроверкойCore, не испытаниемпартииA; полныйнабор, меню/мир/сохранение/дваигрока остаютсяNOT_TESTED доотчётаWindows. Бинарныефайлы не запрашиваются повторно череззаблокированныйCDN. ПартияB не начата.

Инструкция: R8_PARTY_A_WINDOWS.md; результаты: R8_PARTY_A_TEST_RESULTS.md.

## Automated checks in this checkout

Gradle build succeeded; fresh JUnit succeeded (40 tests); Core-only GameTestServer passed 185/185. All 120 installer tests and 16 R8 tests passed. Resource validation passed: 346 JSON resources and 276 bilingual keys. These checks do not load the three external module JARs or establish runtime acceptance. No graphical client, two-player connection or performance measurement was performed.
