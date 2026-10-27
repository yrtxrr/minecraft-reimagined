package dev.reimagined.client;

import dev.reimagined.browser.BrowserRecipes;
import dev.reimagined.browser.BrowserItems;
import dev.reimagined.browser.RecipeDisplayAdapter;
import dev.reimagined.browser.RecipeDisplay;
import dev.reimagined.browser.BrowserSnapshotCache;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RecipesUpdatedEvent;

/** Client bridge only. Recipe matching/display building lives in the server-testable browser API. */
@EventBusSubscriber(modid = "reimagined", value = Dist.CLIENT)
public final class RecipeCatalog {
    private static final BrowserSnapshotCache CACHE = new BrowserSnapshotCache();
    private static List<RecipeDisplayAdapter> adapters = List.of();
    private RecipeCatalog() {}

    @SubscribeEvent public static void recipesUpdated(RecipesUpdatedEvent event) { CACHE.invalidateRecipes(); }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { CACHE.clear(); }
    /** Client modules can opt into concrete input-dependent previews without replacing the UI. */
    public static void setDisplayAdapters(List<RecipeDisplayAdapter> values) {
        adapters = List.copyOf(values); CACHE.invalidateRecipes();
    }
    static long revision() { return snapshot().revision(); }
    private static BrowserSnapshotCache.Snapshot snapshot() {
        var level = Minecraft.getInstance().level;
        return level == null ? CACHE.clear() : CACHE.get(level, level.getRecipeManager(), level.registryAccess(), level.potionBrewing());
    }

    static List<ItemStack> items(String query, BrowserItems.Category category) {
        return BrowserItems.items(query, category);
    }
    static List<RecipeDisplay> recipes(ItemStack item, boolean usages) {
        var snapshot = snapshot();
        var level = Minecraft.getInstance().level;
        var recipes = level != null && !adapters.isEmpty()
                ? BrowserRecipes.recipeDisplays(level.getRecipeManager(), level.registryAccess(), item, adapters) : snapshot.recipes();
        return java.util.stream.Stream.concat(recipes.stream(), snapshot.brewing().stream())
                .filter(display -> usages ? display.uses(item) : display.produces(item))
                .sorted(java.util.Comparator.comparing(display -> display.id().toString())).toList();
    }
}
