# Development instructions

Continue the existing project and preserve implemented systems. The complete received specification is `docs/USER_SPEC_RU.md`; `docs/FULL_SPEC.md` is currently absent. Never claim the full specification or a playable alpha is complete on the strength of compilation alone.

## Git checkpoints

The user explicitly authorizes committing and pushing completed development stages to `yrtxrr/minecraft-reimagined`, on the existing working branch `feat/reimagined-alpha`, without asking for permission each time.

- At task start verify the current branch/worktree and GitHub access using the existing HTTPS Git proxy; do not request or extract a token merely because a token variable is unset.
- After each completed stage review the diff, run relevant full build/tests, create a clear commit, and immediately `git push origin feat/reimagined-alpha`.
- Preserve other people's work. Never use force push, reset remote history, or overwrite conflicts. If push is rejected, fetch and reconcile changes without discarding local or remote commits.
- Do not commit credentials, user worlds, logs, dependency caches, compiled Minecraft artifacts, mod build outputs or temporary test results. Keep the generated player JAR under ignored `build/libs/`; use an explicit authorized artifact/release flow if publishing binaries later.
- Save verified stages promptly so an interruption does not leave completed work only in the cloud machine.

## Verification

Java21, Minecraft1.21.1 and NeoForge21.1.264 are pinned. In this cloud checkout source `scripts/cloud-shell.sh` before Gradle commands. Use the existing checkout; tasks are already isolated, so do not create worktrees unless explicitly requested.

`./gradlew build` compiles the complete mod and runs unit tests. `bash scripts/verify-integration.sh` launches the official headless GameTestServer and checks a fresh XML report against every declared GameTest. `python3 scripts/validate-resources.py` checks local resources but does not replace Minecraft codec/runtime validation. Do not bypass TLS, checksums, authentication, network restrictions or legal prerequisites. Do not auto-accept an EULA on the user's behalf.

Keep `ROADMAP.md`, `ARCHITECTURE.md`, `PROGRESS.md`, `TESTING.md` and `THIRD_PARTY_NOTICES.md` accurate. Distinguish source implementation, actual executed checks, untested graphics/client interaction, online authentication, and full-specification gaps. Retain known limitations rather than silently marking them passed.

When parallel work is requested, assign disjoint packages/files and integrate centrally. Only the coordinating agent runs Gradle and commits/pushes in this shared cloud checkout. External FCC-Codex works in its own clone/branch and runs its own checks and checkpoints there; the main developer owns architecture, shared files, integration, full testing and merges. See docs/FCC_HANDOFF.md for proposed ownership boundaries. After the current research-journal checkpoint, stop new implementation until the user distributes tasks. Do not start the proposed tasks or merge FCC changes without that distribution.
