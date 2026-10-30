# Почему загрузка заблокирована —2026-10-30

Проверки выполнены с обычной проверкой TLS/штатным HTTPS_PROXY. `HTTPS_PROXY`/`HTTP_PROXY` указывают на `proxy:8080`, без user/password. Ключи/секреты не запрашивались и не печатались.

| Официальный источник | Проверенный путь | Результат |
|---|---|---|
| Modrinth API | `/v2/project/terralith`, `/v2/search?query=jei` | CONNECT403 |
| CurseForge API | `/v1/mods/238222` (JEI) | CONNECT403 |
| CurseForge сайт | `/minecraft/mc-mods/jei` | CONNECT403 |
| Modrinth CDN | `https://cdn.modrinth.com/` | CONNECT403 |
| CurseForge CDN | `https://edge.forgecdn.net/` | CONNECT403 |
| JEI Maven | `/mezz/jei/jei-1.21.1-neoforge/maven-metadata.xml` | CONNECT403 |
| GitHub API | `/repos/mezz/JustEnoughItems/releases` | CONNECT403 |
| NeoForge Maven, предыдущая проверка | `21.1.264/neoforge-21.1.264-installer.jar` | CONNECT403 |
| Maven Central (контроль) | `https://repo.maven.apache.org/maven2/` | HTTP200 |

Повтор Modrinth/CurseForge/CDN вне sandbox тоже CONNECT403, ответ `server:envoy`. CONNECT отвергается **до TLS к origin**: это не доказательство удалённого файла, неправильной версии, rate limit либо запрета автора. Вероятно ограничение доменов/маршрутизации платформенного proxy; точная ACL/причина политики средой не раскрыта. Корни CDN проверяют доступ к домену, **не наличие конкретного JAR**. Прямые ссылки на моды не выдумывались. Полный проход установщика сохранил все40кандидатов,0файлов разрешено,39активных целей unresolved плюс Nullscape как проверяемая альтернатива.

Разрешённые варианты:

1. Открыть официальный API Modrinth и `cdn.modrinth.com` для получения release metadata, required dependencies и файлов с официальными SHA512. Никаких credentials для публичного Modrinth API не требуется.
2. Официальный CurseForge API: как правило нужен личный developer API key; скрипт принимает `CURSEFORGE_API_KEY` **только из локального окружения**, передаёт только `api.curseforge.com`, не пишет в план/Git/отчёт. Текущий403 возник ещё до проверки ключа origin. Никаких чужих ключей из лаунчеров.
3. Официальное приложение CurseForge на Windows либо официальный сайт: допустимая ручная установка отсутствующих проектов и их зависимостей. Если API `downloadUrl=null`/автор запретил API-распространение, не вычислять forgecdn URL по fileId; использовать разрешённый официальный путь. Для dedicated нужны явно проверенные стороны.
4. Официальные Maven/GitHub releases можно использовать после разрешения доступа и проверки координат/loader/hash. Git proxy для этого GitHub-репозитория работает; это не доказательство доступности API/CDN и не повод использовать его как обход скачивания.
5. Выполнить приложенный установщик на компьютере пользователя, со штатной сетью/TLS. Не менять proxy ради обхода облачной политики, не отключать TLS/аутентификацию, не использовать зеркала/пиратские модпаки.

Статусы `unresolved` не объявляют40модов несовместимыми. Для четырёх неоднозначных названий/портов (Thaumcraft4:Reforged, BotaniaRevived, BetterEnd:NewDawn, ImmersiveWeatheringRenewed) требуется точная официальная идентичность, не подмена одноимённым модом другого loader. Полный runtime остаётся непроверенным до фактической установки.

## Дополнительный путь без каталога

ОфициальныйGit позволяетчитатьисходники FTBQuests/ATM10/Prism. Prism ModrinthInstanceCreationTask.cpp в573665ffa1ccbd7e19df39c28193d26d5b202d60 явнотребуетsha512 и поддерживаетneoforge; SHA1 дляегоimportне требуется. Поэтому записанныеFreebuff25пинов экспортируютсявклиентскийmrpack без сетевыхобращений/вымышленныххешей; самPrismскачиваеттежеофициальныефайлывсети пользователя. Это не обходcloudproxy и не утверждениеGUIruntime. Официальный Maven FTB (из build.gradle upstream) https://maven.ftb.dev/releases/dev/ftb/mods/ftb-quests-neoforge/maven-metadata.xml тоже CONNECT403; повторовCDNбольше не выполнялось.
