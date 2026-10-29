# Beekeeping

Beekeeping extends vanilla bees, bee nests and bee hives. It does not add a second hive block, a second bee entity or a parallel honey simulation: Minecraft's own pollination, honey level, colony storage and harvest rules remain authoritative. The mod adds two small tools (hive frames and a bee smoker), one processed resource (beeswax), a wax block and two honey meals that connect beekeeping to the existing millstone, cooking pot and pear orchard.

## Why there is no custom hive block

`BeehiveBlockEntity` hardcodes `BlockEntityType.BEEHIVE` in its constructor, and `LevelChunk` refuses to attach a block entity whose type is not valid for the placed block. A modded hive block would therefore either need a second block entity with a duplicate of the vanilla colony logic or an unsafe block-entity type override. Both would duplicate vanilla mechanics and risk losing bees on chunk load. Instead, the frame count is stored on the real vanilla `BeehiveBlockEntity` through a NeoForge data attachment, so nests, hives, shears, bottles, campfires, fire, chunk unloading and bee release keep their vanilla behaviour.

## Gameplay

- **Hive frames.** Right-click a bee nest or bee hive with a frame to install one, up to three per hive. Installation is handled on the server and consumes exactly one frame; a full hive keeps the item and shows a short action-bar message. Frames are saved in the hive's normal block-entity NBT, so they survive chunk unload, world save and restart. Breaking a hive with a player returns every installed frame as an item.
- **Bee smoker.** Right-clicking a full hive or nest (honey level 5) harvests one honey bottle into the player's inventory and one honeycomb per installed frame, then resets the honey level. The colony stays inside the hive, so no bees are released and no anger is created. The same use calms angry bees within 8 blocks horizontally and 6 vertically. Using the smoker in the air calms bees within 6 blocks, costs one durability and starts a one-second cooldown. The tool has 96 durability.
- **No duplication.** The vanilla bottle and shears interactions are untouched. A smoked harvest replaces the bottle harvest, it does not stack with it; harvesting an unripe hive produces nothing and keeps its state.
- **Beeswax.** Milling one honeycomb in the existing water-powered millstone yields one beeswax after 6 seconds. Nine wax compress into a beeswax block and the block unpacks back into nine wax. The block is hoe-mineable, has normal block loot and can be composted (0.3).
- **Recipes.** Two planks, one wax and one string make two hive frames. One iron ingot, one wax and one stick make a smoker. Both recipes unlock from owning wax, through ordinary recipe advancements.
- **Honey meals.** The cooking pot gains two recipes that use real bowls and return containers:
  - *Honeyed pear*: pear + honey bottle + barley, 12 seconds, 9 hunger / 0.85 saturation, returns a glass bottle.
  - *Honey cake*: flour + honey bottle + milk bucket, 16 seconds, 10 hunger / 0.9 saturation, returns a glass bottle and a bucket.
- **Integration.** Honey comes only from vanilla bee pollination, so flowers, daylight, nest availability and the vanilla bee AI still define production. The pear comes from the seasonal orchard (summer/autumn fruit), flour from the existing millstone chain and milk from vanilla husbandry, so the meals link four already-implemented systems instead of adding a new economy. Wax is also a generic crafting material for future content.
- **Presentation.** Items and the wax block use original 16×16 vanilla-style pixel textures; the journal gains a short beekeeping chapter; all names, tooltips and messages exist in English and Russian.

## Balance and limits

- At most three frames per hive, so a smoked harvest yields at most one bottle and three honeycomb. That matches vanilla's own honeycomb-per-hive rate while requiring a crafted, consumed frame for each extra comb.
- Frames and the smoker are consumed/crafted items, not free upgrades. The smoker's 96 durability limits total uses; there is no automatic harvesting, hopper integration or passive wax generation.
- Honey production is not accelerated. The mod never raises the vanilla honey level itself, so the vanilla pollination cadence and flower requirements remain the production limit.
- Seasonal behaviour is intentionally indirect: vanilla bees are not season-gated, but the orchard fruit used by the honeyed pear is. A future season gate for bee activity would need a real `BeehiveBlockEntity`/`Bee` hook and is not claimed here.

## Performance boundaries

- No block entity, ticker, scheduled task, world scan, chunk load, packet or saved-data index is added.
- The frame attachment is O(1) per hive, is written only when a frame is installed or removed, and serializes only when non-zero. Removing all frames removes the attachment.
- The smoker performs one bounded entity query (17×13×17 box) and only when a player explicitly uses the item. There is no per-tick bee or hive work.
- Breaking a hive does one block-entity lookup and at most one item drop.
- Explosions, pistons and other non-player removal do not fire `BlockEvent.BreakEvent`; frames are lost in those cases. This is a deliberate trade-off against a global per-hive index and is documented rather than hidden.

## Implementation and provenance

`beekeeping/BeekeepingContent.java` owns registrations, the `reimagined:hive_frames` attachment, creative-tab entries and the compost value. `BeeFrames.java` is the O(1) attachment helper, `BeeFrameItem`/`BeeSmokerItem` implement the two tools and `BeekeepingEvents` handles server-authoritative installation and frame recovery. Meals are registered in `cooking/CookingContent.java`; resources live under `beeswax`, `bee_frame`, `bee_smoker`, `honeyed_pear`, `honey_cake` and `beeswax_block`.

The implementation uses the pinned Minecraft 1.21.1 / NeoForge 21.1.264 contracts for `BeehiveBlock`, `BeehiveBlockEntity`, `NeutralMob`, `UseOnContext`, `PlayerInteractEvent.RightClickBlock`, `BlockEvent.BreakEvent` and NeoForge data attachments. Vanilla classes were inspected as API references only; no vanilla or third-party source/assets are copied. No Farmer's Delight, TerraFirmaCraft, Create or other mod code, data or textures are incorporated, and no mandatory dependency is added. All beekeeping pixel textures are original project artwork generated for this stage.

## Validation scope

`BeekeepingGameTests` adds eight headless server tests:

1. registration of items/block/attachment, the hoe tag, all five recipes and both cooking recipes;
2. the three-frame cap and persistence through a real hive NBT save/load round trip;
3. real interaction installation, the full-hive rejection, non-hive rejection and frame recovery on the break event;
4. a smoked harvest of a full hive: bottle, one comb per frame, colony retained, angry bees calmed, durability spent;
5. unripe-hive safety and the frameless full-hive harvest (bottle only);
6. air use calming bees, durability and cooldown enforcement;
7. real pot cooking of both meals, glass-bottle/bucket remainders, bowl consumption and eating;
8. recipe-browser projection, real `ItemStack` network codec round trip and packaged EN/RU keys.

These are server checks. The actual rendered item/block textures, client tooltips, graphical smoke particles, survival-client harvesting, large-apiary profiling and authenticated multiplayer were not exercised in this stage. The performance claims above are structural bounds, not measured TPS results.
