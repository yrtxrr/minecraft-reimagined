# Progress

## 2026-10-27 — initial implementation

Started from a verified empty GitHub repository. Preserved the complete specification; split implementation across independent modules and integrated registrations centrally.

Implemented source: NeoForge scaffold; calendar persistence/network/HUD and seasonal crop event; ten skill trees with finite upgrades and uncapped BigInteger levels; four wand schools and differentiated iron weapons; deer entity/model/texture/spawns; water wheel/axial shaft/millstone with custom milling recipes; client inventory item panel and recipe browser; optional world preset.

Verification is deliberately split: independent Java-rule tests execute without Minecraft. They caught a real static-initialization defect in SkillProgress; it was corrected and the suite rerun. Resource validation and upstream API inspection are separate checks, not proof of a successful game launch.

**External blocker:** actual cloud proxy denies CONNECT to `maven.neoforged.net` (403). Network domain additions were saved to the environment draft; saving the draft does not apply the running machine's network policy. Full compile, server launch, graphics and installable JAR remain unverified. No graphical client has been run. The alpha release gate remains open.

Next action after network changes apply: `bash scripts/setup-cloud.sh`, fix all resulting compile errors, then test dedicated startup/preset generation/persistence and prepare a client test build. Full specification systems such as civilizations, transport, bosses and watershed hydrology are not implemented.
