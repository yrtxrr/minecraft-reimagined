# Разделение работы с FCC-Codex

Текущее распределение пользователем: FCC-Codex выполняет ячмень в `fcc/agriculture-barley`; главный разработчик улучшает браузер рецептов в `feat/reimagined-alpha`. Остальные направления ниже остаются предложениями, а не выданными заданиями. Ячмень и браузер не должны изменять файлы друг друга.

Основная интеграционная ветка — `feat/reimagined-alpha`. FCC работает в отдельном клоне и своей ветке от актуального проверенного коммита этой ветки, без force push. Перед началом конкретной задачи фиксируем базовый SHA и выбранную область. Ветка ячменя создана от `e8bfb8094bc907140f2bc7db718bc2599430fcb1`; остальные имена веток остаются предложениями.

## Общие файлы остаются у главного разработчика

- `build.gradle`, `settings.gradle`, `.github/workflows/`, `AGENTS.md`.
- `src/main/java/dev/reimagined/Reimagined.java`, существующий `content/Content.java`.
- Существующие `client`, `engineering`, `progression`, `season`, `research` и боевые классы `content`.
- `gametest/RegistryGameTests.java`, `gametest/TestReportHooks.java` и общая структура тестового мира.
- Глобальные `assets/reimagined/lang/en_us.json`, `ru_ru.json`, списки loot modifiers и другие агрегирующие реестры.
- Итоговые ROADMAP/ARCHITECTURE/PROGRESS/TESTING и проверка лицензий при интеграции.

FCC добавляет отдельный регистратор `XContent.register(IEventBus)` без редактирования существующей точки входа. Точное подключение и серверный прогон этого регистратора согласует и выполняет главный разработчик при интеграции. Переводы передаются отдельными JSON-фрагментами в `docs/i18n/<область>.<локаль>.json`, затем объединяются централизованно. Никаких обязательных новых зависимостей без согласования.

## 1. Ячмень и посевы — `fcc/agriculture-barley`

**Код:** `src/main/java/dev/reimagined/agriculture/AgricultureContent.java`, `BarleyCropBlock.java`; тесты только `gametest/AgricultureGameTests.java` и `src/test/java/dev/reimagined/agriculture/`.

**Ресурсы:** новые `blockstates/barley.json`, `models/block/agriculture/`, `models/item/barley*.json`, `textures/block/agriculture/`, `data/reimagined/recipe/agriculture/`, `loot_table/blocks/barley.json`; переводы `docs/i18n/agriculture.*.json`.

Один полноценный выращиваемый злак: восемь стадий, семена с доступным способом получения, зрелый урожай и восстановление посадки. Помол через существующий тип `reimagined:milling` в существующую муку, без изменения механической системы или её сериализатора. Сезонные правила использовать как есть. Приёмка: рост, незрелый/зрелый урожай, сохранение состояния блока, загрузка рецептов; собственные ассеты и уведомления о лицензиях.

## 2. Геологическая разведка — `fcc/geology-prospecting`

**Код:** `src/main/java/dev/reimagined/geology/GeologyContent.java`, `ProspectingHammer.java`, `ProspectingSurvey.java`; `gametest/GeologyGameTests.java`, тесты чистой логики в `src/test/java/dev/reimagined/geology/`.

**Ресурсы:** только собственные `models/item/prospecting_hammer.json`, `textures/item/prospecting_hammer.png`, `recipe/geology/` и `docs/i18n/geology.*.json`.

Инструмент сообщает реальные признаки существующей руды поблизости. Серверный ограниченный поиск только загруженных блоков по действию игрока, с ограничениями радиуса/числа чтений, прочностью и перезарядкой. Никаких постоянных сканирований, загрузки чанков, изменения генератора или выдуманных месторождений. Приёмка: известная/отсутствующая руда, граница чанка, блокировка повторного использования, сохранение перезарядки.

## 3. Кабан — `fcc/fauna-boar`

**Код:** `src/main/java/dev/reimagined/fauna/boar/BoarContent.java`, `Boar.java`, клиентские `client/BoarModel.java`, `BoarRenderer.java`, `BoarClient.java`; только `gametest/BoarGameTests.java`.

**Ресурсы:** собственная текстура `textures/entity/boar.png`, модель яйца `models/item/boar_spawn_egg.json`, `neoforge/biome_modifier/boar_spawns.json`, `loot_table/entities/boar.json`, `docs/i18n/boar.*.json`.

Отдельное животное с ограниченными ванильными AI-целями, собственным поведением, питанием/размножением, естественным появлением и добычей. Не изменять существующего оленя, общие наблюдения книги или климат. Приёмка: атрибуты, AI-условия, размножение, biome modifier, сохранение сущности, безопасность выделенного сервера; графика отмечается проверенной только после настоящего клиента.

## 4. Кулинарный котёл — `fcc/cooking-pot`

**Код:** `src/main/java/dev/reimagined/cooking/CookingContent.java`, `CookingPotBlock.java`, `CookingPotBlockEntity.java`, `CookingRecipe.java`; только `gametest/CookingGameTests.java` и `src/test/java/dev/reimagined/cooking/`.

**Ресурсы:** `blockstates/cooking_pot.json`, `models/block/cooking/`, собственные `models/item/cooking_*.json`, `textures/block/cooking/`, `textures/item/cooking/`, `recipe/cooking/`, `loot_table/blocks/cooking_pot.json`, `docs/i18n/cooking.*.json`.

Небольшая законченная система приготовления нескольких блюд над источником тепла. Рецепты через стандартный `Recipe`/`RecipeType`/`RecipeSerializer`, MapCodec и StreamCodec, со станцией `getToastSymbol` для существующего браузера. Сохранение ингредиентов/выхода/прогресса, ограниченный серверный тик. Не изменять браузер, жернов или общую боевую систему. Приёмка: нагрев/остановка, расход входов/выход, JSON/сетевой roundtrip, сохранение незаконченного рецепта; без жажды/болезней/обязательных питательных веществ.

## Передача и интеграция

Каждая область — отдельная ветка и понятные коммиты. В отчёте FCC указывает точный SHA, изменённые файлы, реально выполненные проверки и непроверенные подключения. Код, который только скомпилирован без подключения регистратора, не объявляется игровым готовым модулем. Главный разработчик рассматривает патч, подключает модуль, объединяет переводы/общие реестры, запускает полный build и весь текущий GameTest-набор, проверяет артефакт и выполняет безопасный push интеграционной ветки. Не ослаблять тесты и не скрывать незапущенные проверки.

Текущий полный полученный текст требований: `docs/USER_SPEC_RU.md`; `docs/FULL_SPEC.md` пока отсутствует.
