# Active R8 research stage

Work only on feat/reimagined-r8-addons. User R8 request supersedes historical branch restrictions below. Preserve R7 tag c7b140e, release archives, pins, locks, Core and worlds. Research all20candidates before installing addons; no Ars Nouveau, no unverified JAR or automatic magic substitution. Actual user installed.lock/Core hash are required for baseline acceptance. Source declarations are not VERIFIED_METADATA or runtime proof. Follow docs/R8_INTEGRATION_PLAN.md; publish checkpoints only on this R8 branch.

# Current solo-development priority

Kilo has stopped. Do not merge its unverified technical implementation automatically. Its cbc7a5f docs are design input; the current native first-chapter/advancement bridge is independently implemented against reviewed FTB1.21.1 source. Finish actual first-chapter runtime acceptance before later chapters. Preserve Freebuffc20c6a6, v2Settlers+Aggregates and pre-migration backups. Consult concrete working pack/mod implementations before complex integration; record source/license/provenance, copy no unlicensed code/text/assets. Keep the two-player scope and only publish the current integration branch. Historical roles/merge instructions below are superseded.

# Current integration authorization

Work on integration/modpack-agents-21cdd49. Integrate reviewed current Freebuff/Kilo commits with history and tests preserved. Do not push main/source branches. Unify mod version management with pack/mods.json and keep one settlement SavedData owner; no EULA acceptance. Prioritize playable two-player external-mod profile, actual tests/runtime and honest limits. This supersedes historical branch/no-merge restrictions below.

# Installation-first priority (2026-10-30)

Do not expand Core-only gameplay while third-party installation is blocked. Maintain all40target candidates and an official-source Windows install path. Source restrictions must not be bypassed. Kilo progression cbc7a5f has blocking FTB quest review in docs/KILO_PROGRESSION_REVIEW.md; no merge until actual compatibility/action/team checks pass. Review Freebuff separately when ready. Keep all existing tests/save compatibility and distinguish candidate metadata/hash checks from actual client/server runtime.

# Current personal modpack authorization (2026-10-30)

Work on `feat/reimagined-modpack-core`. The latest user task supersedes historical branch restrictions below. Preserve existing gameplay, the complete target mod list and source agent branches. Prioritize a playable personal Windows client for Ryzen 5 5600H/Vega 7/16 GB, use existing mods/configuration/KubeJS before new Java. Commit/push only this task branch; do not update alpha or agent branches. No force push. No automatic license/EULA acceptance or network restriction bypass. Record verified versions and dependencies; blocked candidates are not installed mods. Keep documentation concise. Core remains mod ID `reimagined` for save compatibility. No new elaborate framework or full coverage obligation; retain meaningful save/load and integration checks. The older integration instructions below describe completed checkpoints.

# Current integration authorization

The user authorizes merging only d8af955 (Freebuff/OpenCode candidate) and c31544c (verified settlement recovery) on integration/final-agents-settlements-c31544c. Keep both source branches and origin/feat/reimagined-alpha unchanged. Do not substitute29ba004, force push, delete tests or create unrelated systems. Publish only the new integration branch after build/JUnit/full GameTests/native joint-world/restart checks. This supersedes historical no-merge/branch-specific push instructions below.

# Development instructions

Continue the existing project and preserve implemented systems. The complete received specification is `docs/USER_SPEC_RU.md`; `docs/FULL_SPEC.md` is currently absent. Never claim the full specification or a playable alpha is complete on the strength of compilation alone.

## Git checkpoints

The user explicitly authorizes committing and pushing completed development stages to `yrtxrr/minecraft-reimagined`, on the existing working branch `feat/reimagined-alpha`, without asking for permission each time.

- At task start verify the current branch/worktree and GitHub access using the existing HTTPS Git proxy; do not request or extract a token merely because a token variable is unset.
- After each completed stage review the diff, run relevant full build/tests, create a clear commit, and immediately `git push origin feat/reimagined-alpha`.
- Preserve other people's work. Never use force push, reset remote history, or overwrite conflicts. If push is rejected, fetch and reconcile changes without discarding local or remote commits.
- Do not commit credentials, user worlds, logs, dependency caches, compiled Minecraft artifacts, mod build outputs or temporary test results. Keep the generated player JAR under ignored `build/libs/`; use an explicit authorized artifact/release flow if publishing binaries later.
- Save verified stages promptly so an interruption does not leave completed work only in the cloud machine.

## Performance requirements for every stage

Performance is a high priority: stable server TPS, bounded tick latency, reasonable retained memory and scaling with many mechanisms/creatures. Avoid premature microoptimization; fix structural costs and measured bottlenecks first.

- Do not scan the world, all entities or all chunks every tick. Prefer relevant events, local queries and scheduled work with explicit limits on frequency, radius, nodes/entities examined and total work. A bounded per-machine operation is not enough if thousands run together: assess aggregate cost, stagger/batch work and define a per-tick budget where needed.
- Never generate/load chunks just to inspect mechanisms, resources or entities. Check loaded state before access. Keep expensive world generation in Minecraft's supported chunk-generation pipeline, not gameplay ticks or synchronous tick-time waits. Do not access live world state from arbitrary worker threads; async work needs immutable inputs and safe lifecycle/result application.
- Cache only when ownership, size limits and invalidation are defined. Cover relevant block/config/recipe/registry changes, chunk unload, dimension/world changes and disconnect. Release world/entity/player references when their owner ends; do not cache unbounded positions or stale worlds.
- Minimize network payloads, recipients and frequency: prefer changes/deltas and initial state over unchanged broadcasts. Mark/save state only when it actually changes; never force disk saves every tick. Avoid unnecessary allocations in hot paths, using profiles to decide which matter.
- Potentially costly mechanics require meaningful budget/scale checks: verify bounds, loaded-only access, cache reuse/invalidation, no-op behavior and relevant packet/save rates. Do not weaken assertions, disable AI/gameplay or silently skip checks to manufacture performance success.
- Every FCC-Codex integration must review correctness and aggregate performance. For potentially costly stages or integrations, require a reproducible real-Minecraft profiling scenario, relevant scale, baseline/comparison and measured results; the main developer owns this integration gate.
- A successful build, GameTests, operation-count checks or theoretical complexity does not establish smooth gameplay or acceptable TPS/memory. Profile an actual Minecraft server/client as relevant, recording revision, hardware/JVM/settings, scenario size, warmup/duration, TPS/MSPT, CPU hotspots, allocation/GC/retained memory and network/save metrics where relevant. Include tails only if the instrumentation measures them. See ARCHITECTURE.md for the profiling procedure.
- Report profiling as not performed or blocked when the required runtime/clients/scenario are unavailable. Do not claim an optimization or good performance without measurements. Keep raw recordings/worlds/logs out of Git; retain the setup, results summary and limitations in documentation.

## Verification

Java21, Minecraft1.21.1 and NeoForge21.1.264 are pinned. In this cloud checkout source `scripts/cloud-shell.sh` before Gradle commands. Use the existing checkout; tasks are already isolated, so do not create worktrees unless explicitly requested.

`./gradlew build` compiles the complete mod and runs unit tests. `bash scripts/verify-integration.sh` launches the official headless GameTestServer and checks a fresh XML report against every declared GameTest. `python3 scripts/validate-resources.py` checks local resources but does not replace Minecraft codec/runtime validation. Do not bypass TLS, checksums, authentication, network restrictions or legal prerequisites. Do not auto-accept an EULA on the user's behalf.

Keep `ROADMAP.md`, `ARCHITECTURE.md`, `PROGRESS.md`, `TESTING.md` and `THIRD_PARTY_NOTICES.md` accurate. Distinguish source implementation, actual executed checks, untested graphics/client interaction, online authentication, and full-specification gaps. Retain known limitations rather than silently marking them passed.

## Ownership and upstream research

Current integration scope: review/merge Freebuff c6a0cb9, OpenCode090a2cc and Kilo23a515f in that order, preserving history and tests. Stop a system on critical blockers. Do not update origin/feat/reimagined-alpha until the requested integration gates are complete; verified partial checkpoints may be saved on an integration review branch. Source agent branches/worktrees must not change. The approved deer update is complete at0c30793; no further animal art changes. JEI remains deferred. This supersedes the historical scope and immediate-main-push rule for this integration task.

Historical authorization (completed stages): continue from the published creative-tab checkpoint cb4cbc2 without repeating it. Remove the bespoke recipe UI while preserving recipes/gameplay; prepare optional JEI compatibility only through official accessible sources. If official access returns403, report the required access and continue independent work without bypassing restrictions. Prepare the five-animal texture plan from the existing57-image audit; do not replace art without visual verification. Test/commit/push each independent completed stage. Do not touch Kilo branches/worktrees.

Current recovery authorization: preserve the verified local settlement implementation on codex/settlement-safety-recovery-63cdf1b. Do not modify or push the agent source branches, do not merge, and never force push. Remote Kilo is now29ba004; that alternate implementation was not tested by this checkpoint. This supersedes the previous branch-specific implementation authorization below.

Previous implementation authorization: fix settlement/trade blockers only in feat/kilo-settlements-economy, starting23a515f. Commit and push completed stages to this branch, without merges or changes to Codex/main branches. The user explicitly supersedes the historical agent-branch exclusion for this task. Keep existing professions, schedules and persistence; use supported vanilla merchant APIs rather than duplicate unsafe client purchases.

Historical authorization: finish integration of Freebuff's boulders atdf7a223 and Kilo's thatch/clover/journal commitsafc5bd3/1835d41/25e4ea7 into `feat/reimagined-alpha`, preserving source history and fixing integration defects. Freebuff fishing4a71f92 is already integrated and must not be reimported. Do not push to, rewrite or edit either agent branch. Earlier Kilo exclusions are superseded by this explicit request. No new major mechanics: finish validation and document remaining gaps. Push completed checkpoints only to the main working branch, without force.

Before a major mechanic, inspect relevant open implementations (Create, TerraFirmaCraft, Farmer's Delight, JEI/EMI and others as appropriate). Evaluate architecture, aggregate cost and NeoForge 1.21.1 compatibility. Reuse only when licenses and attribution requirements permit it; document pinned sources, licenses and provenance in THIRD_PARTY_NOTICES.md. Otherwise implement the relevant technical principles independently. Do not copy protected assets or add mandatory mod dependencies without user consent.

For Minecraft1.21.1, do not treat ServerLevel.hasChunkAt as proof of FULL readiness:it checks tickets. Loaded-only mechanical inspection uses MechanicalWorldAccess/getChunkNow on the server thread and direct LevelChunk reads; ordinary Level getters after a ticket check can request/await a chunk future. Reuse this contract for future inspection code and fail closed when no ready chunk exists.
