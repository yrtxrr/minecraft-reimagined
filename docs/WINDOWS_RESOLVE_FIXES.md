# Исправление шести unresolved после первого Windows resolve

Основание — реальные сообщения INSTALL_REPORT.md пользователя, не предположение об отсутствии модов. Пользователь также подтвердил resolve Create6.0.10, Mekanism10.7.19.85, AE2 19.2.18, IE12.4.2-194 и FTB Quests2101.1.37. Это подтверждение каталога на его ПК, **не запуска**. Без официальных файлов/хешей новые версии не внесены в25фиксированных пинов.

| Цель | Проверено | Решение |
|---|---|---|
| Better Combat | Официальная ветка1.21.1 содержит NeoForge, публикацию на Modrinth/CurseForge, обязательные Cloth Config и Player Animator. Player Animator1.21.1 публикует NeoForge, по умолчанию каналalpha. Старое сообщение скрывало, какой именно проект/зависимость отфильтрован. | Для Better Combat разрешены release/beta; для его конкретной библиотеки Player Animator — release/beta/alpha. Slug-политика требует совпадения source_url с репозиторием автора. Реальный выбранный файл всё равно обязан иметь тегиNeoForge1.21.1 и официальныйхеш. |
| CC: Tweaked | Официальная веткаmc-1.21.x: Minecraft1.21.1, NeoForge21.1.9, isUnstable=true; publisher используетalpha. Modrinth IDgu7yAYhd прямо указан автором. | Точная привязка ID; разрешены release/alpha только этому проекту. При наличии стабильного файла он предпочтителен. Никакого глобального допускаalpha. |
| JEI | Официальная ветка1.21.1: publisher type=BETA; Modrinth IDu6dRKJwZ, CurseForge238222. Новая обязательная библиотекаMezzConfig такжеBETA, Modrinth7tEfOcA7 / CurseForge1689768. | Привязки по IDs, локальное разрешениеbeta дляJEI/MezzConfig. API-ключ CurseForge по-прежнему нужен для егоAPI; URL файлов не выдумываются. Modrinth остаётся основным способом. |
| Pufferfish's Skills | НаWindows старыйslug дал404. Точное официальное название/проект по доступным источникам не подтверждены; сайт Puffish недоступен изоблака. Нельзя утверждать, что404 означает отсутствие всех совместимыхрелизов. | Удалён неверный обязательныйslug. Выполняется только уникальный поиск по прежним точнымназваниям; похожее имя не принимается как доказательство. Если не найдено — явно optional-unresolved, установка продолжается с существующими личными навыкамиCore. |
| Thaumcraft4:Reforged | Точный проект и автор не идентифицированы. Нет подтверждённогоNeoForge1.21.1файла именноэтого проекта. | deferred-identity, не входит в обязательныйпервыйпрогон. Не заменяется оригинальнымThaumcraft, другимReforged-проектом или Ars Nouveau автоматически. Четыре школыCore сохраняются. |
| BotaniaRevived | Точный проект/автор/релиз не подтверждены. ОригинальнаяBotania не считается тождественнойRevived. | deferred-identity, исключён только из обязательногонабора. Магическое направление и кандидат вреестре сохранены; никаких чужихJARпод похожимназванием. |

Все40кандидатов остаются в отчёте. Две отложенные идентичности и необязательный нерешённыйSkills **не скрываются**: статусы/причины сохраняются, а скачанныйпрофиль помечаетсяINCOMPLETE при этих пропусках. Это готовность обязательногонабора к испытанию, не реализация всех исходныхмагическихнаправлений.

## Что изменено в алгоритме

- Старый фильтрrelease-only смешивал «нет подходящего загрузчика» и «есть подходящие файлы, но другой канал». Теперь ошибки показывают название/ID фактической зависимости и исключённые каналы.
- Политики каналов ограничены проверенными проектами. Непроверенныеalpha остаются запрещены. `--allow-beta` также больше не включаетalpha дляCurseForge побочно.
- Стабильныйрелиз предпочтителен, если есть. Точная версия, типрелиза, официальныйURL/хеш фиксируются в новомinstall-plan.json иinstalled.lock.json после реальногоразрешения наWindows.
- Все ограниченияMC1.21.1/NeoForge, лицензий/API-распространения, размеров и хешей остаются. Никакой подменыFabric/Forge.
- При просмотре текущего исходникаJEI обнаружен Minecraft-диапазон `[1.21, 1.21.1)`, исключающий1.21.1. Неизвестно, опубликован ли JAR с этим исходником. Поэтому аудит **фактически скачанного** JAR теперь проверяет простые числовые диапазоныMinecraft иNeoForge и остановит установку при таком конфликте. Maven-координаты/каталожный тег не заменяют descriptor. Сложные Maven-диапазоны и все межмодовые версии по-прежнему проверяетNeoForge; числоваяпроверка не объявляется полноценнымMavenresolver.

## Официальные источники и предел проверки

Исходники получены из официальных репозиториев; никакой код/ресурсы оттуда не копировались и не собирались в неофициальныеJAR.

- Better Combat: https://github.com/ZsoltMolnarrr/BetterCombat/tree/e4a6a4a2ae109045d7a1eae334728bd726c305ea — gradle.properties и neoforge/build.gradle. Авторскаяпубликация указывает Modrinthbetter-combat, CurseForge и зависимости; Maven вREADME использует официальный ModrinthMaven.
- Player Animator: https://github.com/KosmX/minecraftPlayerAnimator/tree/cb3227efc19ec46065597332ae265076d0f2b495 — Minecraft1.21.1, defaultalpha, NeoForgeзагрузчик вpublisher; README связываетофициальныеModrinth/CurseForgeстраницы.
- CC: https://github.com/cc-tweaked/CC-Tweaked/tree/e49896848178be8d8aa7f146a3893e9ae1d87d58 — gradle.properties, libs.versions.toml, build-logic/base/src/main/kotlin/cc-tweaked.published-mod.gradle.kts. АвторскиеGitHubReleases связаны изpublisher; Maven https://maven.squiddev.cc указан вREADME. Новые неподтверждённыеMaven/GitHubJARне подставляются вместо проверяемого каталожногофайла.
- JEI: https://github.com/mezz/JustEnoughItems/tree/98e703a70a728a13c0d22f9509adf044a23cc738 — gradle.properties, NeoForge/build.gradle.kts. ОфициальныйMaven https://maven.blamejared.com/mezz/jei/jei-1.21.1-neoforge/maven-metadata.xml указан вREADME; CurseForge https://www.curseforge.com/minecraft/mc-mods/jei.
- MezzConfig: https://github.com/mezz/MezzConfig/tree/a2f4af88372f495cd8225fd378f0dd2b45994d10 — IDs, Minecraft1.21.1, общаяконфигурационнаябиблиотека и публикацияBETA.

ОблачныеAPI/CDN/Maven ограничения не обойдены; прежние заблокированные каталоги не опрашивались повторно. Новая проверка https://puffish.net/skills/ вернула403, в томчисле единственная проверка внепесочницы. GitHubисточники доступны черезGit. Утверждать отсутствиеThaumcraft4:Reforged/BotaniaRevived вообще или отсутствие ихрелизов нельзя: корректныйстатус — идентичностьнеподтверждена. Конкретные бинарныеверсии этих шести целей, их текущиеGitHubReleaseAssets иMavenфайлы не подтверждены загрузкой в облаке.

## Следующий запуск наWindows

Скачайте prerelease **windows-trial-2026-10-30-r2** после завершения GitHubActions. Распакуйте в `C:\Games\Reimagined-Setup-r2`; старые файлы/отчёты не удаляйте.

```powershell
Set-Location C:\Games\Reimagined-Setup-r2
py -3 scripts/modpack/install.py resolve --output C:\Games\Reimagined-r2
notepad C:\Games\Reimagined-r2\INSTALL_REPORT.md
py -3 scripts/modpack/install.py install --output C:\Games\Reimagined-r2
```

Не используйте старый `-UseExistingPlan`: он содержит результаты до исправления. Глобальные `--allow-beta` и `--allow-incomplete` для этих трёх явных исключений не требуются. FTB уже разрешался у вас — не меняйте его наMavenрежим без конкретнойошибки. После успешной установки импортируйте новыйmrpack в отдельный экземплярPrism. Если остаётся обязательныйunresolved или исключающий платформуJAR, пришлите INSTALL_REPORT.md/DEPENDENCY_AUDIT.json и точныйтекстошибки; не заменяйте файл случайнойверсией.
