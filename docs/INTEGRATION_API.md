# Current quest integration contract

Solo maintenance: Kilo has stopped. Native FTB chapter and reviewed server bridge: QUEST_RUNTIME_INTEGRATION.md. SettlementQuestEvent remains Freebuff's overworld/server-thread event; consume kind.id strings, not enum ordinals/names. No speculative player/biome/dimension fields added. Shared5facts live in QuestSignals of the same Version2 settlement owner, no extra population/era registry. Personal actions use actor UUID/Player persistent data and vanilla advancements; FTB Teams owns shared task/reward state. Historical role assignments below are superseded.

# Контракт первого этапа для Kilo / Freebuff

Сохранён modId/namespace `reimagined`: Reimagined Core — новое назначение существующего мода, не второй дублирующий JAR. Не переименовывать registry/NBT IDs. API в `dev.reimagined.core`, common-side, без зависимостей от KubeJS/FTB/MCA.

- `CoreApi.research(ServerPlayer, ResourceLocation)` — одно знание, true только при первом открытии. `unlock(player, CoreProgress.Era)` — один из7независимых milestones, без жёстких recipe/quest gates. Не дублировать старые навыки/заклинания; их текущие владельцы остаются progression/research/magic.
- `CoreApi.progress(player)` — снимок личного ограниченного ledger,256ID +7эпох; отдельный NBT key `reimagined_core_progress_v1`. ID должны быть стабильны и namespaced. Clone переносит этот key; logout сохраняет штатная player NBT. Новые ledger фактически наполняются адаптерами через API, автоматической выдачи всех эпох нет.
- `CoreApi.signal(level, kind, subject, actorUUID)` — синхронный `CoreSignal` на NeoForge.EVENT_BUS. Kind RESEARCH/ERA/CIVILIZATION/ECOLOGY; actor null допустим для мирового события. Вызывать на серверном потоке после успешного изменения. Событие не отменяется, не сохраняет очередь и не содержит ItemStack/Player/Level. Не выдавать награду просто по клиентскому пакету. FTB/KubeJS listener ещё не установлен.
- `CoreBootstrap.integrate(modId, () -> adapter)` — ленивая фабрика, не вызывается для отсутствующего или отключённого мода. Не импортировать optional-классы в bootstrap, не захватывать их в eager static fields. Config `reimagined-common.toml`, `disabledIntegrations`, после перезапуска. Пока зарегистрированных внешних адаптеров нет.
- Мировые владельцы остаются существующими: `SettlementData`/`TradeData` в overworld SavedData, CalendarData общий на измерения; блоки/механизмы/ульи сохраняют штатный NBT. Не создавать второй общий мир/копию цивилизаций. Навыки, исследование журнала и магия принадлежат игроку; новая API не объединяет их между участниками.

Kilo: интегрирует знания/эпохи/FTB-квесты и команду двух игроков после проверки версии, оставляя навыки/исследования личными. Freebuff: публикует переходы CIVILIZATION/ECOLOGY по реальным событиям, не tick-broadcast. Оба не меняют общие bootstrap/API/lock чужим PR без обзора. Данные событий не являются сетевым протоколом: GUI синхронизировать соответствующим штатным меню/ограниченным пакетом владельца. Никаких сканов мира или загрузки чанков из адаптера.

Проверка: `/reimagined_core status` показывает личный ledger (может быть пустым до адаптеров). Test-only callers проверяют реальные ServerPlayer, idempotency, clone/serialization, двух независимых игроков. Это не доказательство сетевого подключения двух настоящих клиентов.
