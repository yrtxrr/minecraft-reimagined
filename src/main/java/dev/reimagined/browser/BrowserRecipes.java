package dev.reimagined.browser;

import java.util.ArrayList;
import com.mojang.serialization.JsonOps;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SmithingTransformRecipe;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.neoforged.neoforge.common.brewing.BrewingRecipe;
import net.neoforged.neoforge.common.crafting.DataComponentIngredient;

/** Pure common-side projection of synchronized game recipes; suitable for headless integration tests. */
public final class BrowserRecipes {
    private BrowserRecipes() {}
    public static List<RecipeDisplay> snapshot(RecipeManager manager, HolderLookup.Provider registries, PotionBrewing brewing) {
        return snapshot(manager, registries, brewing, ItemStack.EMPTY, List.of());
    }
    public static List<RecipeDisplay> snapshot(RecipeManager manager, HolderLookup.Provider registries, PotionBrewing brewing,
                                              ItemStack selected, List<RecipeDisplayAdapter> adapters) {
        var result = new ArrayList<>(recipeDisplays(manager, registries, selected, adapters));
        result.addAll(brewingDisplays(brewing));
        result.sort(Comparator.comparing(display -> display.id().toString()));
        return List.copyOf(result);
    }
    /** RecipeManager-only projection, separated so callers can cache the brewing graph independently. */
    public static List<RecipeDisplay> recipeDisplays(RecipeManager manager, HolderLookup.Provider registries,
                                                    ItemStack selected, List<RecipeDisplayAdapter> adapters) {
        var result = new ArrayList<RecipeDisplay>();
        for (var holder : manager.getRecipes()) {
            var recipe = holder.value();
            boolean adapted = false;
            for (var adapter : adapters) {
                if (!adapter.supports(recipe)) continue;
                result.addAll(adapter.displays(holder, registries, selected.copy()));
                adapted = true;
                break;
            }
            if (adapted) continue;
            ItemStack output = recipe.getResultItem(registries);
            List<Ingredient> ingredients = recipe.getIngredients();
            Component note = Component.empty();
            if (recipe instanceof SmithingTransformRecipe && recipe.getSerializer() == RecipeSerializer.SMITHING_TRANSFORM) {
                // Vanilla smithing has no getIngredients override. Its supported public codec
                // exposes the exact component-aware ingredients, without private field access.
                var ops = registries.createSerializationContext(JsonOps.INSTANCE);
                var json = Recipe.CODEC.encodeStart(ops, recipe).getOrThrow().getAsJsonObject();
                ingredients = List.of(Ingredient.CODEC.parse(ops, json.get("template")).getOrThrow(),
                        Ingredient.CODEC.parse(ops, json.get("base")).getOrThrow(),
                        Ingredient.CODEC.parse(ops, json.get("addition")).getOrThrow());
                note = Component.translatable("reimagined.browser.smithing_note");
            }
            // Dynamic recipes with no concrete output/ingredients require an explicit adapter.
            if (output.isEmpty() || ingredients.isEmpty()) continue;
            result.add(new RecipeDisplay(holder.id(), ingredients, output, recipe.getToastSymbol(),
                    recipe instanceof ShapedRecipe shaped ? shaped.getWidth() : 3, note));
        }
        result.sort(Comparator.comparing(display -> display.id().toString()));
        return List.copyOf(result);
    }
    /** Includes exact potion variants rather than treating all bottles as the same recipe input. */
    public static List<ItemStack> potionVariants() {
        List<ItemStack> result = new ArrayList<>();
        for (var potion : BuiltInRegistries.POTION.holders().toList()) {
            for (Item container : List.of(Items.POTION, Items.SPLASH_POTION, Items.LINGERING_POTION))
                result.add(PotionContents.createItemStack(container, potion));
        }
        return List.copyOf(result);
    }
    public static List<RecipeDisplay> brewingDisplays(PotionBrewing brewing) {
        var result = new ArrayList<RecipeDisplay>();
        var reagents = new ArrayList<ItemStack>();
        for (Item item : BuiltInRegistries.ITEM) {
            ItemStack candidate = item.getDefaultInstance();
            if (!candidate.isEmpty() && brewing.isIngredient(candidate)) reagents.add(candidate);
        }
        Set<ResourceLocation> seen = new HashSet<>();
        for (ItemStack input : potionVariants()) {
            for (ItemStack reagent : reagents) {
                if (!brewing.hasMix(input, reagent)) continue;
                ItemStack output = brewing.mix(reagent, input);
                if (output.isEmpty() || ItemStack.isSameItemSameComponents(input, output)) continue;
                var potion = input.get(DataComponents.POTION_CONTENTS).potion().orElseThrow();
                String potionId = BuiltInRegistries.POTION.getKey(potion.value()).toString().replace(':', '/');
                String container = BuiltInRegistries.ITEM.getKey(input.getItem()).toString().replace(':', '/');
                String ingredient = BuiltInRegistries.ITEM.getKey(reagent.getItem()).toString().replace(':', '/');
                ResourceLocation id = ResourceLocation.fromNamespaceAndPath("reimagined", "browser/brewing/" + container + "/" + potionId + "/" + ingredient);
                if (!seen.add(id)) continue;
                result.add(new RecipeDisplay(id, List.of(
                        DataComponentIngredient.of(false, DataComponents.POTION_CONTENTS, input.get(DataComponents.POTION_CONTENTS), input.getItem()),
                        Ingredient.of(reagent)), output, new ItemStack(Items.BREWING_STAND), 2,
                        Component.translatable("reimagined.browser.brewing_note", PotionBrewing.BREWING_TIME_SECONDS)));
            }
        }
        // NeoForge's public simple brewing API also exposes concrete ingredient/output descriptors.
        // Arbitrary IBrewingRecipe predicates cannot be exhaustively enumerated without an adapter.
        int index = 0;
        for (var custom : brewing.getRecipes()) {
            if (custom instanceof BrewingRecipe simple && !simple.getOutput().isEmpty()) {
                result.add(new RecipeDisplay(ResourceLocation.fromNamespaceAndPath("reimagined", "browser/custom_brewing/" + index),
                        List.of(simple.getInput(), simple.getIngredient()), simple.getOutput(), new ItemStack(Items.BREWING_STAND), 2,
                        Component.translatable("reimagined.browser.brewing_note", PotionBrewing.BREWING_TIME_SECONDS)));
            }
            index++;
        }
        result.sort(Comparator.comparing(display -> display.id().toString()));
        return List.copyOf(result);
    }
}
