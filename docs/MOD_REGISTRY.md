# Реестр целей и проверенные пины

Все40кандидатов сохраняются в modpack/mod-registry.json. Единственный редактируемый источник фиксированных версий/URL/SHA512/required refs — **pack/mods.json**:25файлов Freebuff, включая9библиотек и optionalColdSweat. Sourceс20c6a6/код59326be сообщает25файлов/99headlessGameTests. На текущей интеграции external байты не скачаны; общий185test прогон выполнен без них. Agent evidence не выдаётся за новый client/runtime pass.

Windows resolver начинает с этихпинов и подбирает лишь дополнительные цели; plans/installed.lock — производные артефакты. Старый пустой mods.lock.json относится только к принятому Core-only checkpoint и **не** источник текущего worldпрофиля. Nullscape выбран из проверенногонабора, BetterEnd — сохранённый альтернативный кандидат, не одновременно. После интеграции Apollo/Moonlight mandatory refs явно отражены по финальному handoff.

Готовые exports: Reimagined-world-25-candidate.mrpack и совпадающий serverbootstrap. Внешние файлы они получают на компьютере пользователя; нет заявленной полной40модовой/FTB/techсборки. Клиентский import использует существующиеSHA512, а не выдуманныйSHA1, согласно проверенному Prism reader. Подробно modpack/INSTALL_WINDOWS.md и VERIFY_WINDOWS.md.

Остальные23активные цели (когдаColdSweat включён и выбранNullscape) не имеют проверенных пинов: Create/IE/Mekanism/AE2/CC/FTB/KubeJS/JEI/оптимизаторы/внешняямагия среди них. Их статус unresolved, не «установлен» и не автоматическое «несовместим». ОбычныйBetterEnd1.21.1 Freebuff отметил какFabric-only; точныйNewDawn NeoForgeпорт не подтверждён. Nullscape заменяетрельефEnd, не обещаетвсюигруBetterEnd. ДляThaumcraft4:Reforged/BotaniaRevived нужны точные официальные release/лицензии. Как дополнительные функциональные варианты магии замечены ArsNouveau/Iron’sSpells вреальных главахATM10NeoForge1.21.1; версии/хеши ещёнеполучены, они не установлены и не заменяют40целей молча.

FTB nativeперваяглава реализована самостоятельно, source-reviewed schema13; самFTBJAR и team/GUI acceptance pending. Kilo прекращаетработу, его docs —дизайн, не API. Для несовместимых/неизвестных loader/file identity installer останавливает цель и сохраняет причину. Подключать дополнения после сверки отчёта; реально скачанный состав фиксируется только в installed.lock.json и новом runtime логе.

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
