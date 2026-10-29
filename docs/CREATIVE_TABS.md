# Reimagined creative inventory

Four standard NeoForge CreativeModeTabs, not a custom screen. Inventory at6e5a343:51registered items, including block items and5spawn eggs. Non-item crop/derived block states are not separate obtainable items.

|Category/tag suffix|Count|Order|Icon|
|---|---:|---|---|
|nature|12|Pear sapling/leaves/log/wood/planks, thatch, clover, deer/boar/otter/badger/marmot eggs|Pear sapling|
|food|13|Barley seeds/grain/flour, pear, three fish, pot, five prepared meals|Barley|
|engineering|11|Wheel/shaft/millstone, blend/steel, forge/bellows, hammer/spear/dagger/halberd|Water wheel|
|magic|15|Journal, four school wands, table/altar, four focuses, four spell folios|Arcane wand|

Small wildlife/equipment groups share tabs rather than creating nearly empty categories. RU/EN labels are under itemGroup.reimagined.*. Standard NeoForge tab pagination/order is retained. Removed module-specific insertion into vanilla category tabs; vanilla search remains available, and other mods may add content through normal APIs. No global item removal/filter affecting third parties.

Extension:append a registered item ID to data/reimagined/tags/item/creative/<category>.json. Its position defines logical display order. Tags merge with replace:false. New categories need one central registration/icon/title, not per-item event subscribers. The complete registry-coverage GameTest fails when future mod items are omitted or duplicated and verifies tag order, nonempty icons/categories and absence from vanilla category tabs. No per-tick scanning/caching or gameplay changes.

Executed full build25JUnit and137/137GameTests, no failures/skips. Actual RU NeoForge client/Mesa software rendering opened page2and displayed category icons, titles and contents; original recipe-panel overlay still present at this checkpoint and belongs to the separate JEI-removal stage. Native EN switching, third-party datapack edits and physical-GPU checks are not claimed. Source resource titles were shortened to avoid wide headers. Existing item/recipe/mechanic identifiers and the Kilo settlements branch remain untouched.


## Beekeeping integration candidate

Six new entries (frame, smoker, wax, wax block and two honey meals) extend the existing food category. Current candidate totals:57items,12nature/19food/11engineering/15magic. Coverage/nonduplication/order GameTest and native RU page2food icons passed; vanilla-category injection from the author branch is removed. Main publication remains blocked on Kilo; see AGENT_INTEGRATION_REVIEW.md.
