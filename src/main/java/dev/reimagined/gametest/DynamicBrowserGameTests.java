package dev.reimagined.gametest;

import dev.reimagined.browser.*;
import java.util.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.crafting.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("reimagined")
@PrefixGameTestTemplate(false)
public final class DynamicBrowserGameTests {
    private static RecipeHolder<?> recipe(GameTestHelper h) {
        return h.getLevel().getRecipeManager().getRecipes().stream()
            .filter(r -> r.value() instanceof ShulkerBoxColoring).findFirst().orElseThrow();
    }
    @GameTest(template = "empty")
    public static void shulkerPreviewsAssembleRealRecipesAndPreserveContents(GameTestHelper h) {
        var level = h.getLevel(); var holder = recipe(h);
        var recipe = (ShulkerBoxColoring) holder.value();
        ItemStack selected = new ItemStack(Items.RED_SHULKER_BOX);
        selected.set(DataComponents.CUSTOM_NAME, Component.literal("Test cargo"));
        selected.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(new ItemStack(Items.DIAMOND, 3))));
        ItemStack before = selected.copy();
        var displays = ShulkerColorDisplays.INSTANCE.displays(holder, level.registryAccess(), selected);
        h.assertTrue(displays.size() == 32 && displays.size() <= ShulkerColorDisplays.MAX_DISPLAYS,
                "Incoming17 and outgoing16 colors must deduplicate their overlap and stay bounded");
        h.assertTrue(displays.stream().map(RecipeDisplay::id).distinct().count() == displays.size(), "Display IDs must be unique");
        for (var display : displays) {
            var input = CraftingInput.of(2, 1, List.of(display.ingredient(0, 0), display.ingredient(1, 0)));
            h.assertTrue(recipe.matches(input, level), "Each preview must be a real server-matchable coloring recipe");
            var actual = recipe.assemble(input, level.registryAccess());
            h.assertTrue(ItemStack.isSameItemSameComponents(actual, display.result()) && actual.getCount() == 1,
                    "Preview output must come from real vanilla assembly");
            h.assertTrue(Objects.equals(actual.get(DataComponents.CONTAINER), before.get(DataComponents.CONTAINER))
                && Objects.equals(actual.get(DataComponents.CUSTOM_NAME), before.get(DataComponents.CUSTOM_NAME)),
                    "Recoloring must preserve the selected cargo and name");
            h.assertTrue(!display.inputs().getFirst().test(new ItemStack(display.ingredient(0,0).getItem())),
                    "Concrete cargo previews must not pretend that an empty box is the same ingredient");
        }
        h.assertTrue(displays.stream().filter(d -> d.produces(selected)).count() == 17
                && displays.stream().filter(d -> d.uses(selected)).count() == 16, "Recipe and usage navigation must cover both directions");
        h.assertTrue(ItemStack.matches(selected, before), "Projection must not mutate the player's selected item");
        h.succeed();
    }
    @GameTest(template = "empty")
    public static void dyeUsageCoversAllBaseBoxesAndRejectsUnrelatedSelections(GameTestHelper h) {
        var holder = recipe(h); var registry = h.getLevel().registryAccess();
        var red = new ItemStack(Items.RED_DYE);
        var displays = ShulkerColorDisplays.INSTANCE.displays(holder, registry, red);
        h.assertTrue(displays.size() == 17 && displays.stream().allMatch(d -> d.uses(red) && d.result().is(Items.RED_SHULKER_BOX)),
                "Dye usage must cover plain and all16 colored inputs");
        h.assertTrue(ShulkerColorDisplays.INSTANCE.displays(holder, registry, ItemStack.EMPTY).isEmpty()
            && ShulkerColorDisplays.INSTANCE.displays(holder, registry, new ItemStack(Items.DIAMOND)).isEmpty(), "No invented unrelated special recipe");
        var custom = new ShulkerBoxColoring(CraftingBookCategory.MISC) {};
        h.assertTrue(!ShulkerColorDisplays.INSTANCE.supports(custom)
            && ShulkerColorDisplays.INSTANCE.displays(new RecipeHolder<>(ResourceLocation.fromNamespaceAndPath("reimagined", "custom_coloring"), custom), registry, red).isEmpty(),
            "An arbitrary subclass must not be assigned invented vanilla semantics");
        h.succeed();
    }
}
