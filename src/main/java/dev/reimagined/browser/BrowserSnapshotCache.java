package dev.reimagined.browser;

import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.crafting.RecipeManager;

/** Identity-based cache shared by the UI and server tests; invalidate on recipe sync, clear on logout. */
public final class BrowserSnapshotCache {
    public record Snapshot(long revision, List<RecipeDisplay> recipes, List<RecipeDisplay> brewing) {
        public Snapshot { recipes = List.copyOf(recipes); brewing = List.copyOf(brewing); }
    }
    private Object context;
    private Object recipesIdentity;
    private Object brewingIdentity;
    private Snapshot snapshot = new Snapshot(0, List.of(), List.of());
    public Snapshot get(Object currentContext, RecipeManager manager, HolderLookup.Provider registries, PotionBrewing brewing) {
        if (currentContext == null) return clear();
        Object recipes = manager.getRecipes();
        if (context != currentContext || recipesIdentity != recipes || brewingIdentity != brewing) {
            var brewingViews = context != currentContext || brewingIdentity != brewing
                    ? BrowserRecipes.brewingDisplays(brewing) : snapshot.brewing();
            snapshot = new Snapshot(snapshot.revision() + 1,
                    BrowserRecipes.recipeDisplays(manager, registries, ItemStack.EMPTY, List.of()), brewingViews);
            context = currentContext; recipesIdentity = recipes; brewingIdentity = brewing;
        }
        return snapshot;
    }
    public void invalidateRecipes() { recipesIdentity = null; }
    public Snapshot clear() {
        if (context != null || !snapshot.recipes().isEmpty() || !snapshot.brewing().isEmpty())
            snapshot = new Snapshot(snapshot.revision() + 1, List.of(), List.of());
        context = null; recipesIdentity = null; brewingIdentity = null;
        return snapshot;
    }
}
