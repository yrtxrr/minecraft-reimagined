## Полный набор: установка на Windows после e4426e7

Облачный доступ к API Modrinth снова вернул CONNECT403 доTLS, включая проверку вне песочницы. Это не означает несовместимость модов. Технологические моды/FTB/оптимизаторы ещё **не скачаны и не запущены здесь**. Все40кандидатов и25старых фиксированных файлов сохранены.

Распакуйте свежий Reimagined-Windows-Setup.zip в отдельную папку. Python3.11+, Java21. В PowerShell из этой папки:

```powershell
# Все кандидаты, включая технологии и оптимизацию; не PinnedOnly.
.\scripts\modpack\Install-Reimagined.ps1 -OutputDirectory C:\Games\Reimagined-full
```

Прочитайте INSTALL_REPORT.md. Для FTB есть дополнительный официальный источник авторов:

```powershell
.\scripts\modpack\Install-Reimagined.ps1 -OutputDirectory C:\Games\Reimagined-full -OfficialFtbMaven
```

Этот явный режим проверяет FTB Maven вместо каталога для FTB Quests. Координаты взяты из проверенного исходного кода FTB1.21.1; **публикация этих версий не предполагается**: установщик проверит maven-metadata.xml, официальныйSHA1, реальныйNeoForgeJAR, modId/version, платформенные зависимости и наличие классов. Development/sources/Fabric-файлы не принимаются. Quests2101.1.37, Library2101.1.36 иTeams2101.1.9 остаются кандидатами до этой проверки. Architectury выбирается через официальный каталог с проверкойNeoForge1.21.1. Незнакомые дополнительные зависимости останавливают этот путь. Доступ к Maven сам по себе не доказывает работоспособность в игре.

После разрешения всех выбранных целей:

```powershell
.\scripts\modpack\Install-Reimagined.ps1 -OutputDirectory C:\Games\Reimagined-full -UseExistingPlan -Install
```

Если отдельные цели недоступны, можно явно выбрать -InstallAvailable вместо -Install. Это не полная сборка: **не начинайте приёмку первой главы**, если FTB Quests/Teams/Library/Architectury отсутствуют; не считайте выполненным технологический этап безCreate/IE/Mekanism/AE2/CC. Список пропусков сохраняется. Нельзя заменять неподтверждённые цели случайнымиJAR.

Для файлов, полученных вручную с официальной страницы автора, используйте -LocalFiles C:\Games\OfficialJars при установке существующего плана. Имя, размер и официальный хеш должны точно совпасть с install-plan.json. Неправильный файл останавливает установку; ограничения распространения не обходятся. Это не способ довериться произвольномуJAR/выдуманномухешу. Оба игрока используют **один** installed.lock.json и одинmrpack, не два новых независимыхresolve.

После скачивания DEPENDENCY_AUDIT.json проверяет фактические обязательныеJava modId отдельно дляclient/server, включая вложенныеJarJarбиблиотеки. Диапазоны Maven и выбор версий общихJarJar, mixins и runtime проверяет NeoForge. Зависимость отforge не считается автоматически зависимостью отneoforge. Незавершённая установка не создаёт готовые профили.

Импортируйте Reimagined-candidate.mrpack вPrism. Для выделенного сервера, вместо ручного поискаinstaller:

```powershell
py -3 scripts/modpack/prepare-server.py --profile C:\Games\Reimagined-full\server --java "C:\Program Files\Java\jdk-21\bin\java.exe"
```

Укажите реальный путь кJava21. Скрипт скачивает точныйофициальныйNeoForge21.1.264installer, проверяетSHA1/платформу, запускает только --installServer и переносит лимит4GB в **user_jvm_args.txt**, который читаетrun.bat. Существующие world/eula/libraries/run.bat не перезаписывает. При сбое сохраняет loader-install.log; повторный запуск поверх частично установленного сервера требует проверки, не автоматической очистки. Скрипт **не принимаетEULA, не запускаетмир и не открываетfirewall**. EULA прочитайте и примите самостоятельно, если согласны; затем запускайтеrun.bat. LAN использует клиентPrism и не требует отдельного сервера.

На16GB/Vega7: клиент6GB приLAN/удалённомсервере; если сервер на томжеПК — клиент4GB+сервер4GB, fast/720p, view8/simulation5, безшейдеров. Это начальные настройки, **не измеренный результат**. Проверьте сценарии вVERIFY_WINDOWS.md: двааккаунта/FTBparty,8заданий,личныеисследования,триизмерения,чистаяостановка/перезапуск,5минпрогрев+10минобычнойигры+исследованиедвухнаправлений. Сохраните installed.lock.json, DEPENDENCY_AUDIT.json, обаclient latest.log, server latest.log иsparkотчёт локально. Не публикуйте токены/данныеучётныхзаписей/миры.

# Быстрый Prism import и совпадающий сервер

**Reimagined-world-25-candidate.mrpack** можно сразу импортировать в современныйPrismLauncher:25точных внешнихпинов скачиваютсяофициально и проверяютсяSHA512. Для этого путиPython/ModrinthAPIkey не нужны. НашCoreJAR иконфиги вложены; Minecraft1.21.1/NeoForge21.1.264 устанавливаетлаунчер. Это мировойбазис, не полная40целевая/технологическая/FTBсборка. ColdSweat в25наборе явно включён.

**Reimagined-world-25-server.zip** содержиттотже source manifest, Core иобщиеconfig. После распаковки Python3.11+: `py -3 scripts/fetch-modpack.py --destination server/mods --optional`, затем `py -3 scripts/validate-modpack.py --mods server/mods`. Поставить официальныйNeoForge server в server/, самостоятельно принятьEULA при запуске; никтоеёзаваснепринимал. Оба игрока используютодинклиентскийпрофиль; дляLAN отдельныйserverне нужен.

Перед старыммиром — закрытьигру/сервер и сделатькопиювсегосохранения. ПредпочтительноновыйDefault мир, не reimagined:continents совместно сTerralith. Дляоткатаv2 используйтеbackup, не старыйJAR.

ПространствоFTBconfig ужеестьвclient/server, **но FTBбинарного модав25наборенет**, поэтому этотbaseline не выдаётсяза работающуюGUIкнигу. Для дополнений/FTB/технологий — полныйустановщик ниже, resolve безPinnedOnly на компьютере сдоступом, затем реальнаяприёмка VERIFY_WINDOWS.md. До первой главы в настоящем FTB/party/restart главу2 не начинаем.

Для24наборабезColdSweat: `py -3 scripts/modpack/export-pinned-profiles.py` из полногоsetup/source. Генераторне требуетGit в распакованномWindows toolkit. Все версии читаются pack/mods.json, второйручнойконфиг не создаётся.

# Проверенный внешний базис Freebuff

Точный источник версий/URL/SHA512/зависимостей — **pack/mods.json** (25файлов, ColdSweat optional). Это не Core-only. Исторический cloud CONNECT403 не отменяет server evidence Freebuff59326be:25модов/99GameTests. После объединения требуется повторный запуск всех файлов; здесь он пока блокирован скачиванием.

Для установки именно этого состава без повторного latest resolve:

```powershell
.\scripts\modpack\Install-Reimagined.ps1 -OutputDirectory C:\Games\Reimagined25 -PinnedOnly -IncludeOptional
.\scripts\modpack\Install-Reimagined.ps1 -OutputDirectory C:\Games\Reimagined25 -UseExistingPlan -InstallAvailable
```

Без IncludeOptional ставится24файла, ColdSweat явно помечен optional-not-selected. Остальные40кандидатов не удалены: техномоды/FTB/магия/оптимизаторы пока не входят в эти25пинов. Для поиска **добавочных** релизов используйте resolve без PinnedOnly: уже проверенные пины остаются фиксированными; conflicting dependency отклоняется. Nullscape теперь выбран по умолчанию из проверенного набора, BetterEnd остаётся альтернативой. scripts/fetch-modpack.py использует тот же manifest/transport, отдельного ручного lock нет.

# Установка настоящих сторонних модов на Windows

Этот комплект — **установщик-кандидат**, а не очередной Core-only игровой архив. Он получает реальные официальные релизы и зависимости **в вашей сети**. В облаке каталоги/CDN блокируются CONNECT403; поэтому внешние JAR не вложены и состав не объявлен проверенным в Minecraft. Все40кандидатов остаются в отчёте. Nullscape — альтернатива BetterEnd, не второй одновременный provider.

Нужно: Windows10/11, Python3.11+ с python.org, лицензированный Minecraft, Java21 и Prism Launcher (либо другой NeoForge-лаунчер). Скрипт не устанавливает программы, не принимает EULA, не просит Microsoft-пароль, не отключает TLS/proxy/online-mode. Прочитайте скрипт перед запуском; системную политику исполнения PowerShell он не меняет. Если PowerShell запрещает локальные скрипты, используйте приведённые Python-команды после проверки исходника, не отключая политику.

## Быстрый путь

Распакуйте `Reimagined-Windows-Setup.zip` в отдельную папку. Из PowerShell в ней:

```powershell
# 1. Проверить все40кандидатов, записать точные версии, причины пропусков и зависимости.
.\scripts\modpack\Install-Reimagined.ps1 -OutputDirectory C:\Games\ReimaginedCandidate
# 2. Прочитать C:\Games\ReimaginedCandidate\INSTALL_REPORT.md и install-plan.json.
# 3. Установить строго тот же проверенный каталог файлов, не подбирать новые версии повторно.
.\scripts\modpack\Install-Reimagined.ps1 -OutputDirectory C:\Games\ReimaginedCandidate -UseExistingPlan -Install
```

Если есть недоступные/неподтверждённые цели, второй шаг **остановится**. Чтобы попробовать доступный состав, **явно** выполните:

```powershell
.\scripts\modpack\Install-Reimagined.ps1 -OutputDirectory C:\Games\ReimaginedCandidate -UseExistingPlan -InstallAvailable
```

Пропуски останутся в отчёте/PROFILE_STATUS.txt; профиль будет INCOMPLETE, не «полная сборка». При0внешних модов установка откажется создавать Core-only. Beta допускается только при первом resolve с `-AllowBeta`; Fabric/Forge/Alpha не подставляются автоматически. Для выбора Nullscape вместо BetterEnd создайте новый план с `-End nullscape` и новой выходной папкой.

Без PowerShell-wrapper:

```powershell
py -3 scripts/modpack/install.py resolve --output C:\Games\ReimaginedCandidate
py -3 scripts/modpack/install.py install --output C:\Games\ReimaginedCandidate --core-jar payload/reimagined-0.1.0-alpha.jar
# Только если осознанно принят неполный состав: добавить --allow-incomplete.
```

## Если проект не находится

`modpack/source-bindings.json` содержит **подсказки slug, не подтверждённые привязки**. Скрипт сверяет точное название официального проекта; похожий мод не заменяет цель молча. Впишите reviewed `modrinth_project_id`/`modrinth_version_id` либо `curseforge_project_id`/`curseforge_file_id` после проверки официальной страницы. Допустимые aliases расширяйте только после проверки идентичности, не чтобы скрыть ошибку.

Для CurseForge API нужен ваш законно полученный developer key в локальном `CURSEFORGE_API_KEY`; в Git/командную строку/отчёт его не вставлять. Можно временно задать через маскированный prompt PowerShell, затем удалить переменную; ключ не нужен Modrinth. Официальный CF API не сообщает надёжную сторону: после проверки exact release установите `side_override`=`both`/`client`/`server`; стороны библиотек — `dependency_sides["curseforge:PROJECT_ID"]`. Если данные Modrinth unknown, тоже нужна явная проверка стороны.

Без developer key используйте **официальное приложение CurseForge/сайт** для отсутствующего мода и required dependencies. Не брать чужие ключи из лаунчера, не скачивать через зеркала и не вычислять CDN URL по fileId. Если API автора не отдаёт downloadUrl, автоматизация этого файла останавливается. Экспорт/лог с точными project/file IDs позволит добавить проверенную привязку в следующей итерации; файл из приложения не считается автоматически совместимым с dedicated. Ручные JAR не подмешиваются незаметно в зафиксированный план.

## Что получается

- `client/` — Core, реальные выбранные client/common JAR, графические настройки, KubeJS scaffolding;
- `server/` — Core, только common/server JAR, max-players2/view8/simulation5/online-mode=true; **без** options.txt и client-only оптимизаторов;
- `Reimagined-candidate.mrpack` — импорт в Prism через «Добавить экземпляр → Импорт». Формат фиксирует Minecraft1.21.1/NeoForge21.1.264, официальные URLs/SHA512/SHA1; Core вложен как собственный override. Внешние лицензии в metadata/official page, публикация этих файлов не разрешается автоматически личным использованием;
- `installed.lock.json` — точные project/version/file IDs, required deps, URL/официальные hashes+фактическийSHA256; `runtime_verified=false`;
- `INSTALL_REPORT.md`/`install-plan.json` — **все40целей**, включая недоступные и невыбранную альтернативу. Сохраняйте plan+lock для второго игрока, не запускайте два независимых latest resolve.

Существующие client/server/mods/saves не перезаписываются: для другой попытки новая выходная папка. Повторный install использует hash-cache, но не обновляет уже созданный профиль. Minecraft/NeoForge runtime ставит лаунчер; dedicated — официальный installer с neoforged.net, собственное согласие EULA и штатный run.bat. Не копируйте только mods без одинаковых common/config/скриптов. Отдельные server-only JAR учитывайте из `server/`, а не только mrpack.

Для16GB/Vega7: single/LAN host heap6GB,720p/fast/view8/simulation5/no shaders/60FPS; отдельный сервер на том же ПК — сначала4GBclient+4GBserver. Это стартовые настройки, не сертификацияFPS. Не угадывать mod-specific config keys. Установка каталога не доказывает renderer/генерационную совместимость. Особенно проверить Reimagined seasons+SereneSeasons, собственных NPC+MCA, собственную фауну+Naturalist, готовку+FD, рельефTectonic/Terralith и culling механических BER.

## До настоящего прохождения

Новый тестовый мир → JEI/технологии/магия/генерация/квесты; затем save→полный restart. Два лицензированных клиента/LAN либо dedicated: одинаковые commonверсии, серверная оплата торговли, синхронные механизмы/заклинания, личные skills/research. FTB team sharing настраивается после реального открытия редактора. Кандидат Kilo cbc7a5f **не установлен** из-за блокеров, docs/KILO_PROGRESSION_REVIEW.md. Не выдавать отсутствующую книгу за готовую.

Сначала резервная копия старого мира; не включайте новую генерацию в единственное сохранение.10мин обычной игры+новые чанки двумя игроками: spark MSPT/TPS/heap/GC, FPS/1%low; затем длительный сеанс и повторный вход обоих. До этих проверок профиль остаётся candidate. Для следующего исправления достаточно INSTALL_REPORT.md, installed.lock.json, latest.log/crash report **без токенов/персональных данных**.
