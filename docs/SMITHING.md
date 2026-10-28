# Steelworking and deterministic forging

This stage implements a complete first smithing cycle from USER_SPEC_RU section15:carbon steel, a manual forge, two finishes, finite quality derived from material/training/equipment, actual equipment attributes and repair. It is separate from OpenCode's pear orchards and Antigravity's existing-system performance work. No orchard, seasonal, mechanical network, old profiler or existing test-suite implementation is changed. No foreign branch is merged.

## Survival cycle

1. Craft **Carburized Iron Blend** from one iron ingot and one coal/charcoal. Blast it in a vanilla blast furnace(200ticks, normal furnace fuel/XP) to obtain one **Steel Ingot**. Ordinary iron mining, smelting and recipes remain available. Common additive `c:ingots/steel`/`c:ingots/iron` tags permit compatible ingots without a mandatory dependency.
2. Craft a **Smithing Forge** from an anvil,3brick blocks and3copper ingots. Craft optional **Smithing Bellows** from2planks,2leather and one iron ingot. Existing iron hammer is the reusable tool, also listed through `reimagined:smithing_hammers` for extensions.
3. Open the forge. Top slots:unworked iron equipment, metal, treatment and fuel. Lower equipment slots:hammer and optional bellows. The rightmost slot is output-only; a separate ghost preview is not an inventory slot. Empty-slot hints explain roles. Shift-click routes fuel/tools/bellows separately.
4. Choose **flint/keen** for a weapon/tool's normal attack damage, or **leather/sturdy** for extra durability. Supply one coal/charcoal. Press Forge:consume the original equipment, one metal, one treatment and one fuel; damage the reusable hammer by4using vanilla durability/Unbreaking rules. Bellows are not consumed. The operation awards16base Smith XP through the existing skill system; generic mastery XP bonuses apply normally. No idle fuel consumption exists.
5. Use the finished vanilla/mod equipment normally. Quality uses built-in MAX_DAMAGE/ATTRIBUTE_MODIFIERS components, so normal equipment, durability, damage and repair behavior sees the real changes. Custom names, enchantments, repair cost and foreign data are copied. Proportional wear is preserved with upward damage rounding; this is not free repair.

Supported:iron sword/axe/pickaxe/shovel/hoe, four iron armor pieces, shield, and the existing spear/hammer/dagger/halberd. There are46explicit material/treatment recipes:all14types support sturdy with iron/steel,9attacking types also support keen. Existing weapon specials/parries are unchanged; keen modifies normal attack attributes, not hard-coded special-attack values. Armor/shield protection is not inflated.

| Quality | Condition | Keen durability / attack | Sturdy durability |
|---|---|---|---|
|1Reinforced|Iron, no trained-bellows bonus|+10% / +0.5damage|+20%|
|2Tempered|Steel; or iron plus trained bellows|+25% / +1damage|+35%|
|3Masterwork|Steel + Smith rank2or3 + installed bellows|+40% / +1.5damage|+50%|

The trained-bellows bonus requires **both** Smith rank>=2and bellows; it adds only1quality, capped3. Skill levels/XP are uncapped as before, but cannot produce unbounded quality/stats. Spending skill points uses the existing `/reimagined skills unlock smith`. No random quality rolls, per-tick stat growth or unlimited re-forging occurs.

## Repair and anti-stacking

Treatment is one-time. The private versioned `reimagined_forging` custom-data compound stores quality/finish; existing unrelated custom data is retained. A repeated treatment is rejected, including an item already carrying this module's attribute ID. Nondefault per-stack maximum durability is also rejected instead of overwriting another mod's work.

Use the **forged item in the left vanilla anvil slot** for repairs/renaming/enchantment work; the tested repair preserves finish and attributes. Normal repair materials and Mending mechanisms remain vanilla. Vanilla grid repair intentionally removes custom data/most enchantments while retaining MAX_DAMAGE. This module does not replace that recipe:the resulting nondefault durability remains ineligible for further forging, preventing recursive scaling after metadata stripping. Grid repair may lose the edge/quality label, so the GUI explicitly recommends anvil repair. Other mods' nonstandard per-stack durability equipment is deliberately refused, not silently rewritten.

## Recipes, UI and lifecycle

`reimagined:forging` is a standard RecipeManager type with MapCodec/registry-friendly StreamCodec. JSON specifies exact equipment identity, metal/treatment ingredients, base material quality1..2and sturdy boolean. Shared browser projection shows all46outputs/uses/stations plus fuel and a reusable hammer. The item preview is the untrained baseline; the note states the trained-bellows bonus and reuse/one-time restrictions. The actual forge preview uses the current player's rank and installed equipment. Browser ingredients are type-level bills of materials; actual eligibility (unworked/default durability) is checked by the server, not granted by browsing.

Menu button transactions re-resolve the real recipe and verify owner, current container, range, block identity/lock, nonspectator status, valid resources/hammer and empty output before mutation. Unsupported buttons/remote viewers fail closed. A10tick per-menu action limit prevents repeated immediate clicks; shared inventory transactions are synchronous, so concurrent viewers cannot double-spend old previews. Item crafting remainders from metals/treatments/fuels are retained or dropped safely.

Seven inventory slots, tool wear and worked items persist with normal container/item NBT. Closing a menu does not discard them; breaking drops contents and forge once. No new custom network payload exists:vanilla menus synchronize changed slots and3small status/rank/quality integers. Names, locks, range checks and comparator fullness use standard container behavior.

## Work contract and ownership

No BE ticker, scheduled job, heat/temperature simulation, world/entity scan, chunk ticket, offline catch-up or global station cache. Manual work touches seven local slots and one player's skill data. Closed stations do no module work. Open menus refresh preview/status at most once per20ticks using the vanilla last-recipe check; actual action always validates fresh data. Loaded inspection/range/menu/comparator reads use getChunkNow/direct existing BE lookup. No deliberate chunk generation is needed. Dirty state follows actual inventory/tool mutations; normal Minecraft saves own disk I/O. One sound and a bounded8-particle burst occur per completed action.

New module correctness/resource/functional-client checks belong to this task. Antigravity retains existing-system optimization, profiling and broad performance work. No existing profiler/test class is edited. Suggested independent acceptance scenarios for Antigravity:0/64/256closed forges, then representative open-menu/crafting viewers with equivalent baselines, recording tick tails, allocation/GC, packets and saves. No performance-readiness/TPS/heap claim is inferred from source limits or GameTests here.

## Upstream research

Reviewed TerraFirmaCraft1.21.x at `72750e42f3e0e8b337400ff1b8468a7ff7bc786d`:LICENSE.txt(EUPL1.2), AnvilRecipe tier/assembly contract, ForgingBonus/component model, version metadata(Minecraft1.21.1/NeoForge21.1.234). Tiered processes and item-owned forging properties inform this independent implementation. No source, textures, models or mandatory dependency copied. Our deterministic three-quality material/skill/bellows system is not TFC's work-target minigame. Platform MAX_DAMAGE/ATTRIBUTE_MODIFIERS/anvil/grid-repair behavior was checked against installed1.21.1/NeoForge21.1.264 sources/APIs.

## Source validation checkpoint

Full Gradle build passed23JUnit tests (four new arithmetic/invariant tests including10000wear cases and integer-overflow bounds). Fresh official server harness **88/88GameTests**, no failures/skips. Nine new smithing tests run actual blend crafting/blast-furnace production, component/wear/attribute forging, mastery/material/bellows/armor behavior, resource/owner/range/output rejection, concurrent smiths, real vanilla anvil/grid repair, BE/item NBT and no-ticker/unloaded checks, destruction drops, all46recipe codecs/browser projections and stale-recipe reload rejection. Native client flow follows separately. Full specification, multiplayer, physical hardware and performance readiness are not certified by these checks.
