package dev.reimagined.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;

/** Read-only view of the synchronized recipe manager: rebuilt on demand, never stale after /reload. */
final class RecipeCatalog {
    enum Category { ALL, BLOCKS, EQUIPMENT, FOOD, OTHER }

    private RecipeCatalog() {}

    static List<ItemStack> items(String query, Category category) {
        String needle = query.toLowerCase(Locale.ROOT).strip();
        List<ItemStack> result = new ArrayList<>();
        for (var item : BuiltInRegistries.ITEM) {
            ItemStack stack = item.getDefaultInstance();
            if (stack.isEmpty()) continue;
            Category actual = item instanceof BlockItem ? Category.BLOCKS
                    : stack.isDamageableItem() ? Category.EQUIPMENT
                    : stack.has(net.minecraft.core.component.DataComponents.FOOD) ? Category.FOOD : Category.OTHER;
            if (category != Category.ALL && actual != category) continue;
            String id = BuiltInRegistries.ITEM.getKey(item).toString();
            if (!needle.isEmpty() && !id.contains(needle)
                    && !stack.getHoverName().getString().toLowerCase(Locale.ROOT).contains(needle)) continue;
            result.add(stack);
        }
        result.sort(Comparator.comparing(s -> BuiltInRegistries.ITEM.getKey(s.getItem()).toString()));
        return result;
    }

    static List<RecipeHolder<?>> recipes(ItemStack item, boolean usages) {
        var level = Minecraft.getInstance().level;
        if (level == null) return List.of();
        List<RecipeHolder<?>> result = new ArrayList<>();
        for (RecipeHolder<?> holder : level.getRecipeManager().getRecipes()) {
            var recipe = holder.value();
            if (usages) {
                if (recipe.getIngredients().stream().anyMatch(i -> i.test(item))) result.add(holder);
            } else if (ItemStack.isSameItem(recipe.getResultItem(level.registryAccess()), item)) {
                result.add(holder);
            }
        }
        result.sort(Comparator.comparing(h -> h.id().toString()));
        return result;
    }

    static ItemStack ingredient(Ingredient ingredient, long tick) {
        ItemStack[] options = ingredient.getItems();
        return options.length == 0 ? ItemStack.EMPTY : options[(int) ((tick / 30) % options.length)];
    }

}
