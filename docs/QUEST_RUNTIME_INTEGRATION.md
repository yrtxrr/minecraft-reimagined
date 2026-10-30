# Первая глава: собственная техническая реализация

Kilo остановил работу. cbc7a5f не добавлен merge-parent: незавершённое объединение прекращено через merge --quit с сохранением рабочих изменений. Четыре design docs и намерения8заданий использованы как материалы проекта; оригинальные kubejs/quests остаются в исходной ветке/истории, не добавлены в активный профиль. Ошибочные Java.type/регистрация/условия переписаны. Новые глава/тексты/схема/серверный bridge — самостоятельная реализация, не объявление старого кандидата рабочим.

## Проверенные технические источники

- FTBTeam/FTB-Quests, ветка1.21.1/main, commit dc94c46a6a9ae806fb5921159513448fcedc8816; gradle.properties source version2101.1.37. Изучены BaseQuestFile.read/writeDataFull, Chapter, AdvancementTask, TaskTypes, CheckmarkTask, XPReward, TranslationManager. Это источник API/формата, **не подтверждение скачанного release JAR**.
- AllTheMods/ATM-10, commit827e3610c356922e59412834d4b08ca5b16a6ce0 (2026-10-29), Minecraft1.21.1/NeoForge. Реальные config/ftbquests/quests/data.snbt и chapters проверены на schema13,16hex IDs, вложенные quests/tasks/rewards, advancement tasks, type=xp/xp, flexible progression. Применены явные flexible/disabled-autoclaim/default_reward_team и native task схема. Чужие reward balances, тексты, IDs, скрипты, картинки не скопированы; README объявляет квесты/скрипты All Rights Reserved. Иерархия переводов ATM source — tooling layout, не слепой шаблон: наши flat lang/en_us.snbt/ru_ru.snbt соответствуют собственному FTB loader.

Официальный Git доступен для чтения источников; заблокированные CDN им не обходились, binaries из него не извлекались. Старые Expert/GTNH/TFG пока не исследовались в этой задаче: следующая адресная проверка нужна при связывании технологий, не при форматировании первой главы.

## Как это работает

Единственный активный источник книги: modpack/overrides/config/ftbquests/quests. Он копируется в client и dedicated server, в mrpack — overrides/config. Класс-прямая зависимость отFTB отсутствует; штатный FTB читает config, а серверные реальные действия открывают vanilla advancement с одним criterion milestone. Native FTB advancement task читает его обычным образом. Gradle включает сгенерированную из того же source копию для проверок ресурсов; второй редактируемой книги нет.

8целей: первый craft, инструмент, настоящий vanilla sleep advancement, употребление пищи, использование журнала, ore break, оплаченная сделка и явное ручное подтверждение выбора направления. Открытие торговли/недостаточная оплата/наличие изумруда/плавка металла не заменяют действия. Для reading используется реальный Journal.use, для NPC trade — notifyTrade после штатной server merchant transaction. Книга не блокирует рецепты и не требует пройти все ветви перед выбором пути. Награды — небольшие25vanillaXP, не фиктивный skillXP/несуществующие чертежи; навыки получают XP через свои существующие события.

QuestIntegration хранит только фиксированные11личных +5мировых booleanIDs в прежнем player key. Повторные события не дописывают одинаковый NBT и не выдают наград/предметов; критерии идемпотентны. Clone сохраняет копию player tag. Настоящие reward claims и совместное состояние находятся в FTB Quests/Teams, не в самодельной копии команды.

SettlementQuestEvent потребляется только по kind.id: settlement_founded/grew/expanded/era_advanced/first_trade. Producer/поля остаются Freebuff, не замораживаются выдуманные biome/player/dimension поля. В текущей модели события только overworld/server-thread, ServerLifecycleHooks предоставляет контекст текущего сервера. Общие факты хранятся в QuestSignals существующего Version2 owner, максимум5; population/economy/era принадлежат Aggregates. Поздний вход получает мировые критерии, а личные craft/research не рассылаются. Подтверждённые legacy announcement flags founded/trade/era восстанавливаются без повторной отправки событий; неоднозначная история не выдумывается. CoreSignal research/era применяется только к actorUUID.

## Проверки и остающийся gate

Native server GameTests проверяют actual advancement completion после craft/food/journal/sleep events, платную MerchantMenu сделку и отказ при недостаточной оплате, canceled ore break/правильный harvest tool, двух отдельных ServerPlayer/clone, stable settlement facts+save/replay,8настоящих ресурсов иконок/advancements. scripts/validate-quests.py проверяет граф/IDs/двуязычные тексты/формат. Это не два подключённых клиента и не загруженный FTB GUI.

FTB binaries/зависимости, технологические моды и клиентские оптимизаторы пока не скачаны в этой среде. Поэтому визуальная книга, Native FTB team completion/reward idempotency/relogin двух аккаунтов **не проверены**, первая глава не объявлена runtime-ready. Установить точный проверенный FTB1.21.1 release+FTB dependencies штатным resolver на Windows, открыть книгу, создать party FTB Teams, пригласить второго, выполнить цели/получить награды/умереть/save/restart/войти обоим. Только после этого начинать главу2 и фиксировать фактические release/hash в pack/mods.json. Старые неподтверждённые API не возвращать.
