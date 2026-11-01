# R8 Party A — отдельный клиент и сервер для двух игроков

Это кандидат для **реального испытания**, не объявление стабильной R8. В составе — неизменённые53артефакта R7 + Generators/Tools/Additions10.7.19.85 + тот же Core. Игровая версия Mekanism вmanifest10.7.19, полный номер публикации10.7.19.85; они проверены раздельно.

ZIP содержит собственный Core, замороженныйплан/отчёт и скрипты/конфигурации. СторонниеJAR не включены: они берутся из проверенных локальныхкэшей либо с точныхофициальныхURL, без resolve/latest. SHA256 **каждого** файла сверяется дополнительно к официальнойконтрольнойсумме.

## Установка новых профилей

1. Закройте R7. Не используйте его мир дляпервогоиспытания. Резервное копирование всего закрытогомира описано вR8_MIGRATION.md исходников; копию пользовательскогомира скрипты не создавали.
2. Распакуйте `Reimagined-R8-Party-A-Windows.zip` в `C:\Games\Reimagined-R8-Party-A-Setup`. Python3.11+ уже использовался дляR7.
3. В PowerShell:

```powershell
Set-Location C:\Games\Reimagined-R8-Party-A-Setup
py -3 scripts/r8/install_party_a.py --output C:\Games\Reimagined-R8-Party-A --reuse-downloads C:\Games\Reimagined-r7\downloads --reuse-downloads C:\Games\R8-Party-A-Evidence-artifact-cache
```

Если кэши расположены иначе, замените пути; флаг `--reuse-downloads` можно повторить дляR4/R5/R6. Старыефайлы не перемещаются/не меняются, только копируются после проверки. Новыефайлы скачиваются только если не найдены по ожидаемомухешу. Если output непустой, выберите новоеимя; после неудачнойпопытки можно использовать её downloads как ещёодин reuse-cache. Не удаляйте чужиепрофили/сохранения.

Приуспехе: `Reimagined-R8-Party-A.mrpack`, `client`, `server`, `installed.lock.json`, `DEPENDENCY_AUDIT.json` иотчёт. Новыхвнешнихфайлов56: все56вclient,51вserver; Coreдобавляетсяккаждому. Пятьклиентскихоптимизаторов не копируютсянасервер. Сохранены original40target статусы: deferredмагия/optionalSkills вPROFILE_STATUS — прежние ограниченияR7, а не ошибкаразрешения трёхмодулей.

## Prism

**Добавить экземпляр → Импорт → Reimagined-R8-Party-A.mrpack**. Не обновляйте/редактируйте R7экземпляр. Имя нового: `Reimagined R8 Party A - TEST`. Версии: MC1.21.1/NeoForge21.1.264, Java21. ДляVega7:720p/Fast/view8/simulation5/безшейдеров;6GB приLAN/серверенадругомПК или4GBклиент+4GBdedicated наодном16GBПК. Это стартовыенастройки, не измеренныйFPS.

Создать новыйDefault-мир. Не выбиратьreimagined:continents одновременносTerralith. Следовать `docs/R8_PARTY_A_TEST_RESULTS.md`. Наличие меню/creativeпредметов не считается прохождениемполногоцикла. После успеха можно проверить **копию** R7тестовогомира; откат — восстановлениеbackup, не открытиеR8мира старымR7.

## Отдельный сервер

Скрипт подготовит официальный NeoForgeinstaller, проверивJava21/контрольнуюсумму/платформу; **не принимаетEULA и не запускаетмир**:

```powershell
py -3 scripts/modpack/prepare-server.py --profile C:\Games\Reimagined-R8-Party-A\server
```

Если Java21 не вPATH, добавьте `--java "C:\PATH\TO\JDK21\bin\java.exe"`. Скрипт откажется перезаписывать existingworld/eula/libraries/run.bat. При ошибке сохраните loader-install.log; не очищайте существующийсервер автоматически.

Затем самостоятельно прочитайте MinecraftEULA и примите её, если согласны. Запустите `server\run.bat`, создайте новыймир, затем подключите **два** клиента с темжемrpack под разнымиаккаунтами. На одномПК —localhost, влокальнойсети —адрессервера; настройкабрандмауэра только повашемурешению. maxplayers2/view8/simulation5/RCONoff уже вновомserver.properties. Сервер использует user_jvm_args.txt с4GBheap. На томже16GBПК лучше одинклиент+сервер, второйигрок надругомкомпьютере.

АльтернативаLAN: новыйодиночныймир → Esc → Открытьдлясети, второйклиент с идентичнымmrpack. LAN не заменяет проверкуdedicated.

## Диагностика

Заполнить `docs/R8_PARTY_A_TEST_RESULTS.md`, затем штатно закрытьклиент исервер (`stop`). Путь --instance — кнопкаPrism **ПапкаMinecraft**, а некореньлаунчера; уElyPrism он может называться `minecraft`, не `.minecraft`.

```powershell
py -3 scripts/modpack/collect-diagnostics.py --instance "C:\PATH\TO\R8-INSTANCE\minecraft" --server C:\Games\Reimagined-R8-Party-A\server --install-output C:\Games\Reimagined-R8-Party-A --results docs\R8_PARTY_A_TEST_RESULTS.md --output C:\Games\R8-Party-A-Runtime-Report.zip
```

Если установкаостановилась, достаточно --install-output/--results/--output, без --instance. Отдельно сохранитьтекстконсолиPrism, еслиMinecraftнеуспелсоздатьлог. Не отправлятьJAR/миры/аккаунты. Просмотретьлоги передпередачей: таммогутбытьникнеймы/IP/чат. Повторныйотчёт — новоеназваниеZIP.

К следующейпартии не переходить поMetadata verified3/3. Нужна реальнаяприёмкаA: мир/основныемеханики/save/restart/dedicated/двоеигроков и отсутствие критическихошибок. Непроверенныереакторы и другиедополнительныевозможности отдельно сохранять какNOT_TESTED.
