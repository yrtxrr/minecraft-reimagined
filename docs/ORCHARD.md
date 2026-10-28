# Pear orchard

Pear trees are ordinary Minecraft trees with a seasonal fruit layer, not a separate orchard simulation. The system is deliberately built from the vanilla `TreeGrower`, `TreeFeature`, `SaplingBlock`, `LeavesBlock` and loot-table contracts so world generation, bonemeal, leaf decay, shears, explosions and block placement keep their normal rules.

## Gameplay

- **Tree generation.** `reimagined:pear_tree` is a normal configured tree feature: pear logs, pear leaves, a 4–6 block straight trunk and a blob canopy. `reimagined:pear_trees` is a rare placed feature (one attempt per chunk, rarity 1/12) added to forests, flower forests, birch forests, meadows and plains through `neoforge:add_features`. It does not generate or load chunks outside Minecraft's normal feature pipeline.
- **Sapling.** Pear saplings grow with ordinary sapling random ticks and bonemeal. The sapling's `TreeGrower` points at the registered pear configured feature. Breaking leaves can drop a sapling (5% base, Fortune-scaled); shears and Silk Touch return the leaves instead.
- **Fruit.** Inner canopy leaves (`distance` 1–2) carry a `fruit` block state. A bare inner leaf has a 1/12 chance per random tick to ripen, but only in summer or autumn in the overworld. Other dimensions are treated as mild. Ripe leaves stop random ticking, so a mature orchard does not keep doing work. The outer canopy never receives fruit work.
- **Harvest.** Right-clicking ripe leaves with an empty hand or any item drops one or two pears, clears `fruit` and plays the sweet-berry pick sound. The same harvest is available through the block's normal item/no-item interaction paths. Breaking ripe leaves also yields pears through the real loot table.
- **Food and wood.** A pear restores four hunger points and 0.3 saturation modifier (2.4 saturation restored). Pear logs and wood are in the vanilla log tags, so leaf decay recognises the trunk; pear planks are in the planks tag, so vanilla stick recipes work. Pear logs/wood convert to four planks and four logs make three pear wood through ordinary crafting recipes.
- **Propagation.** Grafting is a shapeless recipe: one pear plus one oak sapling makes one pear sapling. This links the orchard to existing resources instead of adding a second seed economy.
- **Journal.** The research journal has an orchard chapter with the tree, season and harvest rules.

## Performance boundaries

The only recurring work is vanilla random ticking on loaded leaf blocks. Fruit work is restricted to `distance <= 2` and stops once `fruit=true`; there is no block entity, scheduled ticker, world scan, chunk load, packet broadcast or save data. Natural placement runs inside the ordinary `VEGETAL_DECORATION` feature step. A large orchard has not been profiled separately; the bound is structural (inner leaves only) rather than a measured TPS claim.

## Implementation and provenance

`agriculture/OrchardContent.java` owns registrations, creative tabs, compost values and the `TreeGrower`; `PearLeavesBlock` owns the bounded fruit state and harvest; `PearSaplingBlock` only binds the grower. Resources live under the corresponding `pear_*` asset/data names. All pear pixel textures are original project artwork generated for this stage; no vanilla or third-party texture is copied.

The implementation uses the pinned Minecraft 1.21.1 / NeoForge 21.1.264 source contracts for `TreeConfiguration`, `TreeGrower`, `LeavesBlock`, `SaplingBlock` and loot-table codecs. Vanilla oak/birch tree data was used only as a schema reference, not copied as project data. No Farmer's Delight, TerraFirmaCraft, Create or other mod source/assets are incorporated and no mandatory dependency is added.

## Validation scope

`OrchardGameTests` exercises real registration/tags/recipes/browser projection, the loaded configured/placed features, bounded random-tick rules, summer ripening and winter rejection with the real calendar, actual interaction harvest and pear entities, real leaf loot/shears, and sapling growth through the actual configured tree feature. These are headless server checks. Graphical tree/leaf rendering, sapling growth in a survival client, large-orchard profiling and authenticated multiplayer remain manual checks.

Integration follow-up: server harvest re-reads the ready chunk/current FRUIT state before mutation and drops. Repeated stale-state and block-replacement callbacks cannot duplicate fruit or overwrite terrain. Grower name is namespaced `reimagined:pear`. A new actual GameTest covers these cases; 95/95 tests passed in the proposed main+orchard candidate. The 100-page worst-case journal bound is still legal; reserve should be restored before adding more guide chapters.
