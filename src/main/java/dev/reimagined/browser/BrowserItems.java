package dev.reimagined.browser;

import dev.reimagined.browser.navigation.BrowserSearch;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Item search/category logic shared by the real UI and headless tests. */
public final class BrowserItems {
    public enum Category { ALL, BLOCKS, EQUIPMENT, FOOD, BREWING, OTHER }
    private BrowserItems() {}
    public static Category category(ItemStack stack) {
        if (potionContainer(stack)) return Category.BREWING;
        if (stack.getItem() instanceof BlockItem) return Category.BLOCKS;
        if (stack.isDamageableItem()) return Category.EQUIPMENT;
        if (stack.has(DataComponents.FOOD)) return Category.FOOD;
        return Category.OTHER;
    }
    private static boolean potionContainer(ItemStack stack) {
        return stack.is(Items.POTION) || stack.is(Items.SPLASH_POTION) || stack.is(Items.LINGERING_POTION);
    }
    public static List<ItemStack> items(String query, Category category) {
        var candidates = new ArrayList<ItemStack>();
        for (var item : BuiltInRegistries.ITEM) {
            ItemStack stack = item.getDefaultInstance();
            if (!stack.isEmpty() && !potionContainer(stack)) candidates.add(stack);
        }
        candidates.addAll(BrowserRecipes.potionVariants());
        return filter(candidates, query, category);
    }
    public static List<ItemStack> filter(List<ItemStack> candidates, String query, Category category) {
        var result = new ArrayList<ItemStack>();
        for (var stack : candidates) {
            if (stack.isEmpty() || category != Category.ALL && category(stack) != category) continue;
            String searchableName = stack.getHoverName().getString();
            var potion = stack.get(DataComponents.POTION_CONTENTS);
            if (potion != null && potion.potion().isPresent()) {
                var key = potion.potion().get().unwrapKey();
                if (key.isPresent()) searchableName += " " + key.get().location();
            }
            if (BrowserSearch.matches(searchableName, BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(), query))
                result.add(stack.copy());
        }
        result.sort(Comparator.comparing((ItemStack s) -> BuiltInRegistries.ITEM.getKey(s.getItem()).toString())
                .thenComparing(s -> s.getHoverName().getString()));
        return List.copyOf(result);
    }
}
