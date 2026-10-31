R5: для исправления JAR JEI/More Culling и повторного использования R4 следуйте docs/WINDOWS_R5_ARTIFACT_REPAIR.md. Не запускайте install со старым планом.

R4: сохраните план R3 и выполните resolve с --base-plan, чтобы не обновить 51 уже разрешённый артефакт. Пример: `py -3 scripts/modpack/install.py resolve --output C:\Games\Reimagined-r4 --base-plan C:\Games\Reimagined-r3\install-plan.json`. Затем install с тем же новым output.

R3: обычный resolve использует официальный FTB Maven; Better Combat проверяет официальные source/Issues ссылки. Новый каталог вывода: C:\Games\Reimagined-r3.

# Первый запуск Reimagined через Prism Launcher

Исправление R2 после вашего первого resolve: см. `docs/WINDOWS_RESOLVE_FIXES.md`. Используйте новую папку вывода и новый resolve, а не старый план.

Это испытание клиента, а не объявление готовой Alpha. Собирать Java-проект не нужно: собственный Core уже находится в ZIP. Внешние моды установщик получает на вашем компьютере.

## 1. Получить архив

Откройте [GitHub Releases](https://github.com/yrtxrr/minecraft-reimagined/releases) и prerelease **windows-trial-2026-10-31-r5**. В **Assets** скачайте `Reimagined-Windows-Setup.zip` и `SHA256SUMS.txt`. Не выбирайте автоматически добавленный GitHub архив **Source code**: он не содержит собранного Core.

Публикация выполняется GitHub Actions после отправки тега. Если Assets ещё нет, проверьте [Windows first-run kit](https://github.com/yrtxrr/minecraft-reimagined/actions/workflows/windows-trial.yml). Пока процесс не завершился, это ожидаемый адрес, а не подтверждение наличия релиза. При успешном шаге package ZIP также доступен в **Artifacts → Reimagined-Windows-Setup** на странице запуска (нужен вход в GitHub). Его внешний ZIP содержит установочный ZIP и checksum; распакуйте оба уровня. Если сборка красная, передайте ссылку/текст ошибки — самостоятельно собирать проект не требуется.

В PowerShell проверьте хеш скачанного файла:

```powershell
Get-FileHash "$HOME\Downloads\Reimagined-Windows-Setup.zip" -Algorithm SHA256
Get-Content "$HOME\Downloads\SHA256SUMS.txt"
```

Хеш должен совпасть с опубликованным для **этого** архива. Распакуйте ZIP через Проводник в `C:\Games\Reimagined-Setup`, чтобы внутри сразу были `payload`, `scripts`, `modpack` и `START_HERE_RU.md`.

## 2. Установить необходимые программы

- [Prism Launcher для Windows](https://prismlauncher.org/download/windows/), официальный сайт. Добавьте свой Microsoft-аккаунт с Minecraft Java Edition.
- [Python для Windows](https://www.python.org/downloads/windows/), версия 3.11 или новее, с Python Launcher (`py`).
- Java **21**, например официальный [Eclipse Temurin](https://adoptium.net/temurin/releases/?version=21). В Prism выберите именно 21 через **Настройки → Java → Автоопределение**, либо установку Java средствами самого Prism.

Проверьте Python в PowerShell: `py -3 --version`. Далее используются команды Python, поэтому изменение PowerShell ExecutionPolicy не требуется.

## 3. Получить внешний набор модов

Откройте PowerShell в распакованной папке:

```powershell
Set-Location C:\Games\Reimagined-Setup
py -3 scripts/modpack/install.py resolve --output C:\Games\Reimagined-full
notepad C:\Games\Reimagined-full\INSTALL_REPORT.md
```

Это полный поиск всех целей, **без `--pinned-only`**. Код завершения 2 означает, что остались нерешённые цели: прочитайте причины. Точные подтверждённые 25 файлов, остальные кандидаты и ручные случаи перечислены в `modpack\FIRST_RUN_MODS.md`. Неподтверждённые версии не установлены автоматически.

Если FTB не разрешился через каталог, есть уже реализованный отдельный официальный путь:

```powershell
py -3 scripts/modpack/install.py resolve --output C:\Games\Reimagined-full --ftb-maven
```

Не повторяйте команды бесконечно при одинаковом 403: сохраните отчёт. Для ручных официальных файлов с известным хешем см. FIRST_RUN_MODS.md.

Если выбранные цели разрешились:

```powershell
py -3 scripts/modpack/install.py install --output C:\Games\Reimagined-full
```

Если часть кандидатов недоступна, для **частичного диагностического запуска** можно явно выполнить:

```powershell
py -3 scripts/modpack/install.py install --output C:\Games\Reimagined-full --allow-incomplete
```

В таком случае запишите все пропуски. Без FTB Quests/Teams/Library/Architectury можно проверить окно Minecraft и базовый мир, но нельзя отметить проверку главы успешной. Если не установились технологии и магия, это ещё не полноценная Reimagined.

После успешной установки появятся `Reimagined-candidate.mrpack`, `client`, `server`, `installed.lock.json` и `DEPENDENCY_AUDIT.json`. Ошибка зависимости останавливает создание профилей; пришлите её вместе с отчётом. Не удаляйте существующие миры для повторной попытки. Одинаковый ZIP/lock/mrpack используйте у обоих игроков; не запускайте независимый новый resolve для второго игрока.

## 4. Импортировать и запустить Prism

1. **Добавить сборку / Add Instance → Импорт / Import**. Выберите `C:\Games\Reimagined-full\Reimagined-candidate.mrpack`. Назовите экземпляр `Reimagined Trial`.
2. Дождитесь скачивания файлов. Если Prism показывает недоступный файл, сохраните его имя/URL/ошибку. Не заменяйте его похожим JAR с другого загрузчика.
3. **Редактировать экземпляр / Edit → Версии / Version**: Minecraft `1.21.1`, NeoForge `21.1.264`. Не добавляйте Forge/Fabric поверх NeoForge.
4. **Настройки экземпляра → Java**: Java21, минимальная память 1024–2048 MiB, максимальная **6144 MiB**. Если выделенный сервер запускается на этом же компьютере с 16 ГБ RAM, поставьте клиенту **4096 MiB**, серверу оставьте 4 ГБ. Не вставляйте дополнительные `-Xmx` поверх этих настроек.
5. Нажмите **Запустить / Launch**. Сохраните консоль Prism при ошибке. Если загрузилось меню, в **Mods** проверьте Core, FTB и остальные ожидаемые моды по отчёту. Запишите фактически загруженные версии.
6. Видео для начала: 1280×720, Fast, дальность 8 чанков, симуляция 5, без шейдеров. Это стартовые параметры для Ryzen 5600H/Vega7, не обещание FPS.
7. Создайте **новый тестовый мир**, Survival, генератор **Default**, разрешённые команды для диагностики. Не выбирайте `reimagined:continents` вместе с Terralith. Старое прохождение открывайте только после проверки и резервной копии всей папки мира.

Кнопка **Папка Minecraft / Minecraft Folder** у экземпляра показывает нужный игровой каталог. В нём должны лежать `mods`, `config`, `logs`, позже `saves`; не путайте его с корнем установки Prism.

## 5. Пройти короткую проверку и вернуть результат

Откройте `modpack\FIRST_RUN_RESULTS.md`. Выполните сценарий и заполните «успех / ошибка / не проверено» с пояснениями. Если FTB не открывается, не обходите это командой выдачи advancement: это скроет неисправность.

Для первого совместного запуска проще LAN: хозяин **Esc → Открыть для сети**, второй игрок запускает **тот же mrpack** под своим аккаунтом и подключается через Multiplayer/LAN. Если автоматическое обнаружение не сработало, используйте адрес компьютера хозяина и порт, показанный Minecraft. Разрешение Java в брандмауэре — только по вашему решению для своей локальной сети. Для выделенного сервера используется существующий `prepare-server.py`; EULA пользователь читает и принимает сам, см. INSTALL_WINDOWS.md.

Закройте игру нормально, чтобы лог завершился. Соберите отчёт (замените путь INSTANCE на реальную **Папку Minecraft** из Prism):

```powershell
py -3 scripts/modpack/collect-diagnostics.py --instance "C:\PATH\TO\INSTANCE\.minecraft" --install-output C:\Games\Reimagined-full --results modpack\FIRST_RUN_RESULTS.md --output C:\Games\Reimagined-first-run.zip
```

При отдельном сервере добавьте `--server C:\Games\Reimagined-full\server`. Если установка не дошла до клиента, достаточно:

```powershell
py -3 scripts/modpack/collect-diagnostics.py --install-output C:\Games\Reimagined-full --results modpack\FIRST_RUN_RESULTS.md --output C:\Games\Reimagined-install-failure.zip
```

ZIP содержит выбранные логи, отчёты и список SHA256 JAR, но не сами JAR, миры или аккаунты лаунчера. Очевидные токены маскируются; перед отправкой просмотрите логи — там могут быть никнеймы, IP и чат. Добавьте скриншот ошибки/главы и текст консоли Prism отдельно, если Minecraft вообще не создал latest.log. При размере лога больше 16 МиБ коллектор укажет пропуск в COLLECTION.json. Ранее созданный ZIP не перезаписывается: для нового испытания укажите новое имя.

Пришлите ZIP и кратко: на каком шаге остановились, что ожидали и что произошло. Далее исправляем обнаруженную проблему, а не расширяем сборку до успешного первого запуска.
