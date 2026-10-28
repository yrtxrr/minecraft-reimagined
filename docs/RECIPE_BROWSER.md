> Historical design/acceptance record. The bespoke screen, inventory panel, search/navigation and client cache were removed in9b554d2. This document does not describe the current UI; see [JEI_MIGRATION.md](JEI_MIGRATION.md). Real recipe projections remain, but a JEI custom-category plugin is pending.

# Built-in recipe browser — display and brewing stage

This extends the existing inventory panel/full-screen browser; it does not replace crafting or grant client crafting authority. No JEI/EMI dependency is added.

## Requirements audit and delivered changes

The old browser already had left-click recipes, right-click uses, inventory R/U and a short history. Its generic getIngredients/getResultItem scan missed brewing and input-dependent recipes, conflated potion variants, reset the recipe page on back/resize and rescanned all recipes every second.

- Search now requires every whitespace-separated term, matches localized names/registry IDs case-insensitively, supports @namespace, and includes potion registry IDs (for example long_swiftness) to distinguish identically named duration/strength variants.
- Shared item-category logic drives both the inventory panel and full screen. A Potions category exposes actual drinkable/splash/lingering variants, not empty default bottle stacks.
- Existing recipe/usage navigation now runs over one common RecipeDisplay projection. Ingredients retain shaped slots, alternatives cycle through copied stacks, quantities and station icons are retained. R/U also work over the item panel and full-screen ingredient/result icons. Backspace/history restore recipe pages; filters/resizes/reloads clamp pages. Returning from the browser preserves the parent panel's search/category/page.
- Vanilla brewing is obtained through PotionBrewing.isIngredient/hasMix/mix. Each display represents one bottle plus reagent into one output, with exact potion contents, a brewing stand, time/fuel/batch information. Glowstone, redstone, gunpowder and dragon-breath transitions use real game rules. Public simple NeoForge BrewingRecipe descriptors are also projected.
- Vanilla smithing transforms need special handling: Minecraft1.21.1 does not expose their ingredients through getIngredients(). Public recipe-codec encoding supplies template/base/addition, retaining exact Ingredient semantics. The UI explicitly calls the output a base preview: transferred custom components are not simulated by this generic display.
- Recipe sync invokes cache invalidation; immutable RecipeManager collection replacement is also detected. Brewing projections are retained across recipe-only refreshes. Language/world changes refresh views; logout clears world-bound cache references. No full scan runs every second/frame with unchanged data.

## Architecture

`browser/RecipeDisplay` is common-side metadata (ID, ingredients, result, station, grid columns, localized note). Results/animated alternative stacks are copied. Ingredient predicates drive uses; potion contents distinguish output variants. Ordinary damaged equipment can still locate its base manufacturing recipe.

`BrowserRecipes.recipeDisplays` projects RecipeManager data. `brewingDisplays` separately builds brewing transitions. `BrowserSnapshotCache` tracks world/recipe/brewing identities and invalidation without any Minecraft client imports. `BrowserItems` supplies the actual item/search/category projection used by the UI. `browser/navigation` contains shared search/pagination/history rules.

`RecipeDisplayAdapter` provides explicit, context-aware previews for recipes with input-dependent outputs. Common callers pass adapters to the snapshot/projector; client modules may configure `RecipeCatalog.setDisplayAdapters` on the client thread. Adapters receive the selected stack and must produce truthful concrete previews. This is an extension point, not an implementation of all vanilla special recipes. The test adapter tests dispatch/context only and is not shipped as a real armor-dye recipe adapter.

The client keeps its existing screens/widgets. It consumes common displays and routes clicks/keys through recipe/usage modes. A display ID preserves the selected recipe across refreshes when still available. Every crafting/brewing action remains owned by Minecraft's server systems.

## Executed checks

Full Gradle build:16unit tests passed. Fresh official headless harness:41/41 required GameTests passed, with zero failures/skips. Nine new browser integration tests exercise real recipes (crafting, furnace, smithing, milling), usages, potion transforms/container conversion/simple brewing API, alternative stacks/counts, actual registry items/categories/variant search, immutable snapshots and cache replacement/invalidation/clear. Four new unit tests cover multi-term/localized search, host-locale independence, pagination overflow/shrinking and bounded history with full selection restoration.

## Remaining limitations and manual client checks

No graphical client was launched. Verify actual rendering at several GUI scales, long RU labels, mouse hit boxes, native search focus, returning to inventory and R/U/Backspace in a real client. Verify a server /reload updates an already-open browser without a reconnect; the shared replacement/invalidation path is tested headlessly, not the full network-to-screen path.

Arbitrary vanilla special recipes (armor dye/trim, fireworks, maps and other component-dependent results) still need real adapters. Generic IBrewingRecipe implementations with opaque custom predicates and arbitrary custom potion components cannot all be enumerated. Brewing information reflects the public brewing rules available on the client; arbitrary server-only custom brewing-rule synchronization is not implemented. A complete magic/forge/cooking-machine catalog depends on those gameplay systems existing; current ordinary recipe types work through the shared contract. Recipe transfer/auto-fill is not implemented.

The earlier browser checkpoint did not include agriculture. Ordinary barley has since been implemented and tested by the main developer; see docs/AGRICULTURE.md.

## Concrete shulker coloring increment

The client now includes vanilla shulker-box coloring without installing an adapter manually. `ShulkerColorDisplays` delegates concrete results to Minecraft's ShulkerBoxColoring. Selecting a box exposes16 outgoing dye usages; a colored box also has17 incoming base-box variants, deduplicating their shared case. Selecting a dye shows17 input boxes. Contents/custom name/components are preserved and source ingredients use strict component matching; no empty-box input is substituted for a cargo-filled example. Output is a read-only preview, never a client crafting action.

The default path indexes the canonical vanilla coloring recipe once per snapshot/recipe replacement (identical base-class duplicates have no ingredient configuration); arbitrary subclasses are excluded because their semantics may differ. At most33 displays are built on a selection/refresh, with no registry-wide expansion or per-frame projection. Existing optional adapters can override this built-in display and retain the previous extension API. Logout clears the holder with the snapshot; recipe-sync invalidation removes/restores it normally. No server commands, transfer packets or new dependencies are introduced.

Reviewed JEI1.21.1 commit [7d70c027c9a03901afb74bf3e4d40638a7a7a383](https://github.com/mezz/JustEnoughItems/tree/7d70c027c9a03901afb74bf3e4d40638a7a7a383), specifically its `ShulkerBoxColoringRecipeMaker` finite replacement approach and MIT license (mezz). No code/assets copied; this implementation uses actual vanilla assembly and selected components instead of invented static crafting outputs. See THIRD_PARTY_NOTICES.md.

Other special crafting (armor dyes/trims, fireworks, maps, repairs), recipe transfer/auto-fill and end-to-end graphical reload/navigation remain unfinished. This increment is not full dynamic-recipe coverage. Actual client performance and display layout are not measured; the finite selection-only budget and cache lifecycle are checked headlessly.
