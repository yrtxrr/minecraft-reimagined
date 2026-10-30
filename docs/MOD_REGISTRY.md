# Целевой состав, не список установленных модов

Источник истины: `modpack/mod-registry.json`; установленные сторонние версии: `modpack/mods.lock.json`. Список сохранён полностью, Nullscape — только альтернативный кандидат. На2026-10-30 ни один сторонний JAR не установлен этим этапом.

Проверены официальные адреса api.modrinth.com/v2/project/terralith, Maven NeoForge installer21.1.264, Maven BlameJared JEI metadata и API GitHub ModernFix: CONNECT proxy HTTP403. Повтор API Modrinth вне sandbox также403. Не использовались зеркала, отключение TLS, обход proxy или придуманные версии. Требуется разрешённый HTTPS-доступ к официальным каталогам и указанным ими официальным CDN/репозиториям. Локальные кэшированные MC/NeoForge работают, это не доступ к каталогу сторонних модов.

**Не подтверждена совместимость** не означает **несовместим**. Для каждого кандидата version/license/dependencies оставлены null; фиксация ложной версии опаснее явного блокера. Нужны release с явными1.21.1+NeoForge, зависимости с версиями, источник/лицензия, SHA256 фактического JAR и загрузка в клиенте. Не использовать Fabric Lithium/BetterEnd либо Forge-моды в NeoForge без подтверждённого порта. Названия Thaumcraft4:Reforged, BotaniaRevived, BetterEnd:NewDawn и ImmersiveWeatheringRenewed требуют проверки точной идентичности проекта.

Конфликтные зоны: Terralith+Tectonic — порядок datapack/рельеф; Naturalist+собственная фауна — density; SereneSeasons+старый календарь — двойной рост; Farmer’sDelight+готовка — рецепты/теги; MCA+собственные поселенцы — профессии/спавн/торговля; BetterCombat+боевые callbacks — урон; пять tech-модов — дубли металлов/энергия/баланс; magic+старые школы — прогресс; Incendium/YUNG/Cataclysm — генерация; End — один основной provider; culling/renderer — отдельные визуальные проверки BER/животных. Это риски для проверки, не доказанные конфликты.

Временные функциональные замены без исключения целей: старые механизмы/кулинария/магия/поселенцы/экология остаются включены; ванильные биомы и измерения доступны; журнал остаётся учебником до FTBQuests; отладка vanilla F3/JFR до spark. Неподтверждённые аналоги не добавляются. После открытия доступа подключать сначала диагностику/оптимизацию/JEI, затем FD+квесты, генерацию на новом мире, цивилизации, технологии и магию по измеряемым группам.

| Группа | Кандидаты (все ожидают проверки официальных файлов) |
|---|---|
|world|Terralith; Tectonic|
|ecology|Naturalist; Serene Seasons; Immersive Weathering Renewed|
|survival|Farmer’s Delight; Cold Sweat|
|combat|Better Combat; Improved Mobs|
|technology|Create; Immersive Engineering; Mekanism; Applied Energistics 2; CC: Tweaked|
|magic|Thaumcraft 4: Reforged; Botania Revived|
|civilization|MCA Reborn; Guard Villagers|
|nether|Incendium|
|end|BetterEnd: New Dawn; Nullscape|
|adventure|YUNG’s Better Dungeons; YUNG’s Better Mineshafts; YUNG’s Better Strongholds; L_Ender’s Cataclysm; Better Archeology|
|progression|Pufferfish’s Skills; FTB Quests|
|integration|KubeJS; JEI|
|optimization|Embeddium; ModernFix; FerriteCore; ImmediatelyFast; Entity Culling; More Culling; Dynamic FPS; Lithium|
|diagnostics|spark|
|pregeneration|Chunky|
