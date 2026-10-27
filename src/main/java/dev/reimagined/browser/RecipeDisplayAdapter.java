package dev.reimagined.browser;

import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;

/**
 * Explicit extension point for recipes whose output depends on components or actual inputs.
 * An adapter must provide truthful concrete previews; an empty result is not invented by this API.
 * Adapters are passed per snapshot, avoiding globally retained worlds or mutable registration order.
 */
public interface RecipeDisplayAdapter {
    boolean supports(Recipe<?> recipe);
    List<RecipeDisplay> displays(RecipeHolder<?> recipe, HolderLookup.Provider registries, ItemStack selected);
}
