# Приёмка на Windows: внешний набор + двое игроков

1. Полностью закройте Minecraft/сервер. Скопируйте **всю папку мира** в резервный каталог до обновления. Встроенные .pre-v2 копии защищают только файл settlement-data, не заменяют backup инвентарей/мира/торгового файла.
2. Примените новый установщик: PinnedOnly+IncludeOptional даёт точные25файлов Freebuff; затем UseExistingPlan+InstallAvailable. Установка создаёт новый профиль и не меняет старые saves. Для технологий/FTB/оптимизаторов отдельный resolve безPinnedOnly на той же исходной canonical pin базе, после просмотра report — InstallAvailable в свежую папку. Уже проверенные25версий не должны плавать между игроками. ColdSweat безIncludeOptional явно выключен.
3. `py -3 scripts/validate-modpack.py --mods C:\Games\Reimagined25\server\mods` проверяет25пинов/SHA512/известную required closure, optionalmissing допустим. Диапазоны Maven проверит NeoForge при загрузке, не Python. `py -3 scripts/validate-quests.py` проверяет8задач/граф/локализацию, неGUI.
4. Импортируйте полученный mrpack вPrism либо client/ в отдельныйNeoForge1.21.1/21.1.264 instance. Java21,720p/fast/view8/simulation5,6GBheap безшейдеров. Если клиент+dedicated на одном16GB/Vega7ПК — сначала4GB+4GB. Сервер: штатный официальныйinstaller/run.bat, пользователь сам читает/принимает EULA; scripts не создают eula=true.
5. Создайте новый **Default** мир. Не выбирайте reimagined:continents одновременно сTerralith. Пройдите лес/тайгу/горы/деревню, проверьте Terralith+Tectonic, YUNG, животных/MCA, thenNether/Incendium иEnd/Nullscape. Запишите seed, crash/latest.log; compile/flatGameTests этого не заменяют.
6. Если FTB Quests+его реальные зависимости установлены: откройте книгу через назначение клавиши в настройках, увидьте8заданий с иконками/RUтекстом. Выполните craft/tool/eat/ore/sleep/journal/paidtrade; открытие торговли и недоплата не завершают task. Последний checkmark — явное подтверждение свободного выбора. Получите25XP один раз, проверьте повторный клик. Неподтверждённый FTB release не объявлять поддержанным: source-reviewed формат13, source2101.1.37; фактическийversion/hash/зависимости должны быть вinstalled.lock.
7. LAN или dedicated: два лицензированных аккаунта, одинаковыеcommon/config, создайтеparty черезFTBTeams и примите приглашение вторым игроком. Один выполняет задачу — команда видит завершение; личные навыки/исследования второго не изменяются из-за чужого craft. СерверныеNPCоплата/запасы, магия, мельница и улей должны быть согласованы. Общие civilization events могут завершать общие мировые задания; они не создают NPC/population copies.
8. Умереть/выйти/сохранить/полностью остановить/запустить/войти обоим: личныйprogress/инвентарь, stock,33необязательноNPC, Aggregates/экономика/era, QuestSignals икоманда должны сохраниться безудвоения. Сырые старыеv1данные передмиграцией копируютсявdata/reimagined-settlement-backups; уже утраченные ранее данные не восстанавливаются автоматически.
9. После установки проверенногоspark — прогрев5мин+10мин обычной игры, затем новые чанки вдвухнаправлениях. Записать FPS/1%low,MSPTp95/p99/max,TPS,heap/GC. До реального прогона нет обещания производительностиVega7 или полного25+techнабора.

## GameTests из исходников на Windows (не из playerJAR)

В рабочем checkout с ASCII-путём (например C:\Games\Reimagined-source), не в мире пользователя, создайте run-gametest/mods. Скопируйте туда **только внешние** JAR из подготовленного server/mods; **не копируйте reimagined-*.jar**, иначе его modId задублирует dev output. Сохраните текущий report отдельно или удалите перед запуском, чтобы не считать старый.

```powershell
$env:GRADLE_USER_HOME = "$PWD\build\gradle-home"
.\gradlew.bat build
.\gradlew.bat runGameTestServer
py -3 scripts/check-gametest-report.py
```

GameTestServer специальный headless harness, не пользовательский dedicated. Сохранить лог и новыйXML: текущий источник ожидает185тестов. У Freebuff было99наего sourcebase; здесь все текущие тесты должны выполниться снаружи тоже. Ничего не удалять радиPASS. Дополнительные новые версии тоже требуют freshload; старый99pass не покрывает технологии/FTB. Первая глава до реальногоGUI/team/restart gate не считается завершённой; главу2 не начинаем.
