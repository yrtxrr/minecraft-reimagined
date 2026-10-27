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

When parallel work is requested, assign disjoint packages/files and integrate centrally. Only the coordinating agent runs Gradle and commits/pushes in this shared cloud checkout. External FCC-Codex works in its own clone/branch and runs its own checks and checkpoints there; the main developer owns architecture, shared files, integration, full testing and merges. See docs/FCC_HANDOFF.md for proposed ownership boundaries. Current explicit allocation: FCC-Codex develops barley only in fcc/agriculture-barley; the main developer develops the built-in recipe browser in feat/reimagined-alpha. Do not edit FCC-reserved agriculture files, merge its branch, or start unrelated major systems without a new assignment. The earlier journal coordination pause was superseded by this allocation.
