# Progress

## 2026-10-27 — initial implementation

Started from a verified empty GitHub repository. Preserved the complete specification; split implementation across independent modules and integrated registrations centrally.

Implemented source: NeoForge scaffold; calendar persistence/network/HUD and seasonal crop event; ten skill trees with finite upgrades and uncapped BigInteger levels; four wand schools and differentiated iron weapons; deer entity/model/texture/spawns; water wheel/axial shaft/millstone with custom milling recipes; client inventory item panel and recipe browser; optional world preset.

Verification is deliberately split: independent Java-rule tests execute without Minecraft. They caught a real static-initialization defect in SkillProgress; it was corrected and the suite rerun. Resource validation and upstream API inspection are separate checks, not proof of a successful game launch.

**Historical blocker at initial checkpoint:** actual cloud proxy denied CONNECT to `maven.neoforged.net` (403). Network domain additions were saved to the environment draft; saving the draft does not apply the running machine's network policy. Full compile, server launch, graphics and installable JAR remain unverified. No graphical client has been run. The alpha release gate remains open.

Next action after network changes apply: `bash scripts/setup-cloud.sh`, fix all resulting compile errors, then test dedicated startup/preset generation/persistence and prepare a client test build. Full specification systems such as civilizations, transport, bosses and watershed hydrology are not implemented.

## Continuation from 4090fdb

Stayed on `feat/reimagined-alpha`. NeoForge Maven became reachable through the authorized proxy. Full build now passes; fixed glass tag API compatibility and Java lambda capture, then added server integration coverage. 12 JUnit tests passed; all 17 GameTests passed on a real headless NeoForge GameTestServer. The initial worldgen test failure was a fixture lifecycle/status issue and was fixed without weakening assertions. An isolated same-seed pair of chunks went through actual biome/noise/surface generation and block comparison. Current XML reports distinguish actual execution from planned manual checks.

`docs/FULL_SPEC.md` is absent; reported this to the user. Existing complete requirements remain in `docs/USER_SPEC_RU.md`. No systems were removed. Test classes/template are excluded from player JAR. Real GUI rendering and multiplayer/login remain untested; the alpha is not declared fully ready. Authlib public-key service remains inaccessible in this cloud instance, without disabling security checks.

## Engineering automation and GitHub checkpoints

GitHub read and push access verified through the existing HTTPS proxy. Published the initial history and checked stage cdaaeb6 on `feat/reimagined-alpha`, without force pushing or changing other branches. Per user request, completed stages are now immediately committed and pushed; `AGENTS.md` records this for future tasks.

Implemented sided hopper/NeoForge item-handler automation for the millstone, recipe filtering, bounded inventory and progress-preserving save/load. Full Gradle build and **20/20** server integration tests passed. Added a fresh-report integration runner and CI execution. No temporary builds/worlds/credentials are tracked.

## Combat checkpoint

Implemented four server-authoritative weapon specials with saved shared cooldowns, attack-charge/durability limits, line-of-sight/alliance/PvP checks; automatic timed frontal shield parry respecting vanilla warmup/facing/unblockable sources. Normal attacks and offhand shielding remain vanilla. Full build passed with12unit tests and **27/27** real server GameTests. This does not validate graphical input/animation or complete the whole combat/armor specification.

## Research journal and pause for FCC-Codex coordination

Replaced chat-only guidance with a real readable written-book journal: localized guide chapters (including deer and current biome information), personal observed biome/creature catalogues, bounded local ten-second observation, versioned player saves/death cloning, per-reader rebuilding and explicit pages-before-open network ordering. This is not a research unlock/quest system.

Exercised the saved cloud setup script successfully, then full build with12unit tests and **32/32** required server GameTests. Five journal tests check real item use/network components/packet order, observer cadence/visibility/deduplication, NBT/clone and per-reader isolation, bounded valid entries/book size, and packaged EN/RU chapters. A packet class-name mismatch was corrected against Minecraft1.21.1 APIs before the final passing run. Graphical book display, GUI scale and real multiplayer/authentication remain unverified.

Prepared the player JAR under ignored build/libs with SHA256; test fixtures are excluded. Per user's new organization request, stop new implementation after this checkpoint and push. Four independent proposals and reserved shared files are recorded in docs/FCC_HANDOFF.md. No tasks assigned to FCC, no FCC branches created, and no next major stage started. Main developer retains architecture, integration, testing and merges.

## Built-in recipe-browser stage

User assigned FCC-Codex barley in its separate branch and authorized main-browser development on feat/reimagined-alpha. Audited existing code/full spec; preserved the panel/screens and existing uses mode. Added shared server-testable display/query/cache APIs, actual vanilla brewing and exact potion variants, public-codec smithing ingredients, multi-term/@namespace/potion-ID search, panel categories, page-preserving history/R/U/Backspace navigation, per-context caching and sync/logout invalidation. Added explicit dynamic-adapter dispatch; arbitrary special recipes are still unimplemented and not presented as complete.

Full build +16unit tests +41/41 real server GameTests passed. Rendering/client-network path remains unverified; documented exact remaining checks. No agriculture files or FCC branch were touched; no other major gameplay subsystem started. Details: docs/RECIPE_BROWSER.md.

## Cross-project performance policy

Recorded the user's high-priority performance rules in AGENTS.md and ARCHITECTURE.md: event/local/bounded work, aggregate tick budgets, loaded-only inspection, proper generation/thread boundaries, bounded lifecycle-aware caches, change-driven networking/persistence, and meaningful cost/scale checks. FCC integration now explicitly requires both correctness review and performance review. Real Minecraft profiling with a reproducible workload/baseline and measured TPS/MSPT/memory/CPU is required for costly features; successful builds/GameTests alone are not performance evidence. This stage changes documentation only; no runtime profiling or gameplay optimization was performed.

## Mechanical motion and bounded network checkpoint

Implemented native client BER animation for wheel/shaft/mill rotors with reload-cached geometry, synchronized common phase, state-dependent stopping and frozen persisted angle. Shared loaded-only component cache and capped per-dimension scheduler replace independent production BFS calls; topology/unload invalidation stops stale consumers. Existing milling, recipe reload lookup, hopper/capability access and inventory/progress NBT remain compatible. Create architecture/license research is documented without copying code/assets or adding dependencies.

Checks:full build19unit tests,49server GameTests; ordinary-server JFR baseline/candidate0/16/64components, and actual normal-world save/restart64mills passed. See docs/MECHANICAL_PROFILE.md for measurements, allocation/GC limitations and long ticks. Graphical animation and live client chunk streaming are not verified. All development now belongs to the main developer; FCC handoff is historical. Next:ordinary barley crop, then the first remaining browser increment in ROADMAP.md.

## Ordinary barley checkpoint

Implemented barley/seeds and eight-stage vanilla cultivation, original pixel resources, age-dependent harvest/replanting, additive short-grass seed loot with shears exclusion, bread/seeds/milling recipes and recipe-book unlocks. Compost/common tags/seed animal-food tags added. Existing seasonal growth/shelter hooks and powered mill automation work without modifying either system's implementation. No mandatory mod dependency or farm simulation added; reviewed Farmer's Delight and used the installed vanilla/NeoForge APIs directly. Full build19unit tests,55server GameTests and93JSON resource checks passed. Graphics and large-field profiling remain manual work.

## Next roadmap increment: concrete shulker coloring

Built-in recipe browser now projects actual dynamic shulker coloring with selected item components/cargo preserved,16 outgoing colors and17 incoming box variants, strict source ingredient matching and bounded selection-only construction. Recipe snapshots retain a reload-aware canonical vanilla handler, cleared on logout; custom extension adapters can override it. JEI's finite replacement approach/license was reviewed; no source/assets/dependencies copied. Full build19unit tests,58server GameTests and93JSON resource checks passed. This completes one concrete dynamic-recipe increment, not arbitrary special crafting or recipe transfer. Client visuals/reload interaction and multiplayer still need graphical validation; the alpha gate remains open.

## Graphical Alpha validation checkpoint

Launched the actual NeoForge graphical client using isolated Xvfb/Mesa tools, entered/reopened the disposable world and inspected rendered mechanisms/crops/deer, Russian journal and browser. Fixed two real client issues:double background blur hiding recipe content, and milling recipes entering unsupported vanilla recipe-book categories. Full build19unit/58server tests passed. A separate AlphaObserver client reaches the local server but login is blocked by unavailable authentication services; no authentication bypass was introduced. Alpha remains under validation, with physical-GPU, multiplayer, long-session and standalone-install checks open. Details:docs/ALPHA_VALIDATION.md.
