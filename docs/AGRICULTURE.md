# Barley

Barley is an ordinary Minecraft crop, not a separate farming simulation. It uses the vanilla CropBlock implementation with ages0–7, hydrated-farmland/light/random-tick growth, standard bonemeal, normal placement/survival checks and no block entity, scheduled ticker, world scan or custom network protocol. Mature plants stop random growth ticking. The existing seasonal CropGrowEvent hook applies automatically, including its glass-roof shelter and climate exceptions. Bonemeal follows the same direct growth rules as vanilla wheat; seasons affect natural growth, not bonemeal.

Short grass has an additive1/8 chance to supply a seed on ordinary breaking, while shears keep vanilla grass harvesting without extra barley seeds. This uses NeoForge's existing `neoforge:add_table` global loot modifier and a data-only extra table; vanilla loot is not replaced. Immature crops return one seed; mature crops yield one barley plus one guaranteed seed and0–3 additional seeds. Explosion decay applies. Fortune-specific yield scaling is not implemented.

One grain crafts two seeds; three grains craft vanilla bread. Existing water-powered milling turns one barley into one shared flour in8 processing seconds, retaining hopper/capability automation and shared recipe-browser uses. Crafting unlock advancements are included. Seeds/grain compost at30%/65%; crop/farmland, common crop/seed and chicken/parrot food tags support vanilla/data-driven interoperability. Deer still uses its current explicit food rules. There is no dependency on another farming mod.

## Implementation and provenance

`agriculture/AgricultureContent.java` owns registrations/composting; `BarleyCropBlock.java` specializes only the codec and seed item. Crop states, original16×16 pixel textures, block/item models, translations, recipes, advancements, tags and loot definitions live under the corresponding barley resource names. The project entry point only invokes the module registration.

Reviewed Farmer's Delight branch1.21 commit `dd8b9b0bfec916a2da88a6d64e3a37f4a02c3259`, [TomatoBlock](https://github.com/vectorwing/FarmersDelight/blob/dd8b9b0bfec916a2da88a6d64e3a37f4a02c3259/src/main/java/vectorwing/farmersdelight/common/block/TomatoBlock.java), [MIT license](https://github.com/vectorwing/FarmersDelight/blob/dd8b9b0bfec916a2da88a6d64e3a37f4a02c3259/LICENSE) and gradle.properties (Minecraft1.21.1, NeoForge21.1.219). Its crop specialization confirms that vanilla growth/hooks are suitable; its vine/right-click-harvest mechanics are unnecessary for ordinary barley. No source or artwork was copied. NeoForge's shipped AddTableLootModifier is consumed through its public data codec, not vendored.

## Validation scope

AgricultureGameTests exercises real item placement, bonemeal, all eight age-dependent loot tables, additive grass seed loot/shears exclusion, actual vanilla random ticks reaching the shared seasonal hook, glass shelter, compost registration, real powered automated barley milling, actual crafting and browser recipe projection. Crop rendering and a survival-client harvesting session require a graphical client. Existing mechanical JFR profiling covers the mill; barley introduces no new tick infrastructure. A large planted field has not been profiled separately.
