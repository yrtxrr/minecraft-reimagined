# Minecraft: Reimagined

Minecraft Java **1.21.1**, NeoForge **21.1.264**, Java **21**. One mod, no JEI/Create or other gameplay mod required.

**Development status:** the full Gradle build and 12 unit tests pass. A headless NeoForge server successfully executed 32 integration GameTests, including actual seeded terrain generation. A development JAR is available at `build/libs/reimagined-0.1.0-alpha.jar`. This is not yet a fully validated playable alpha: graphical rendering, inventory UI and a real client/server connection still require client checks.

## Develop

Use the existing checkout; each cloud task is already isolated. Do not create a worktree unless explicitly requested.

```sh
cd /workspace/minecraft-reimagined
bash scripts/setup-cloud.sh
source scripts/cloud-shell.sh
./gradlew build
```

On other machines, install any supported JDK 21 and run `./gradlew build`. The cloud bootstrap supplies a checksum-verified Oracle JDK for this restricted machine; it is a development tool, not bundled with the mod. Keep its license terms in mind when selecting a JDK for another environment. Gradle and ModDevGradle versions are pinned.

```sh
./gradlew runClient            # requires graphics and the normal Minecraft client prerequisites
./gradlew runServer            # dedicated dev server, --nogui
bash scripts/verify-integration.sh # all current server GameTests, fresh XML report
bash scripts/verify-domain.sh  # rule tests only; does not validate Minecraft compatibility
```

The dedicated server's first run creates its EULA/configuration files under `run/`. Read and accept Mojang's EULA yourself before running a normal game server. Never commit `run/` or player/world files.

## Implemented source

- Optional **Reimagined** world preset; the ordinary vanilla world remains available.
- A deer with custom antlers, original texture, forest spawning and breeding with sweet berries.
- Water wheel → axial shafts → millstone. Flowing water perpendicular to the wheel axis supplies rotation; still water does not. Click a face when placing to choose the shaft/wheel axis. Each wheel provides 16 capacity, each mill requires 8; overload stalls the network. Redstone stops wheels/mills. Searches are capped at 128 loaded blocks.
- Right-click a mill with wheat, cobblestone or gravel to insert input. Empty-hand click retrieves output, then input. Milling produces flour, gravel or sand; 3 flour crafts into 2 bread. Hoppers insert recipe inputs from the top/sides and extract finished output from below. Sided NeoForge item handlers enforce the same rules. Wheel animation remains pending.
- Research journal: right-click opens localized guide pages, deer facts, current-biome warmth index and personal discovered biomes/creatures. A bounded local observation runs every 10 seconds and on book use; discoveries are saved across death/rejoin. Pages rebuild for each reader and are synchronized before opening. This is not a quest/research-gate system.
- Ten skill trees: `/reimagined skills`; `/reimagined skills unlock <tree>`. Levels use arbitrary precision XP, while each tree has only 3 mastery ranks. Bonuses are bounded.
- Combat: ordinary attacks remain vanilla. **Sneak + right-click** uses spear thrust (4s cooldown), hammer stagger (6s), dagger thrust/brief speed (2s), or halberd front sweep (5s). Specials share a saved cooldown across weapon swaps and cost 2 durability. A frontal melee shield block at 5–10 use ticks parries, staggers the attacker and saves shield durability, with a saved 1.5s lockout. Shield warmup, rear attacks and unblockable damage retain vanilla behavior.
- Four wands: elemental ray, nature growth/healing, safe arcane step, protective ritual consuming amethyst. Mana and cooldowns are server-controlled.
- Four 24-day seasons. `/reimagined calendar`; operator command `/reimagined season set 0..3`. Seasons follow overworld day time, including sleeping/time commands. Temperate crops grow slower in summer/winter; a glass roof within 16 blocks shelters them. No global weather overhaul yet.
- Standalone inventory recipe browser: **R** button beside inventory, search, item categories, left-click recipes/right-click usages. Narrow screens use the full browser. Recipes come from the server-synchronized Minecraft recipe manager, including the custom milling type.

Common gameplay paths have server integration coverage; graphical behavior still needs a real client. Read [TESTING.md](TESTING.md) before relying on them. The much larger complete target is in [docs/USER_SPEC_RU.md](docs/USER_SPEC_RU.md); implementation gaps are recorded in [ROADMAP.md](ROADMAP.md).
