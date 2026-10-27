package dev.reimagined.gametest;

import dev.reimagined.browser.BrowserItems;
import dev.reimagined.browser.navigation.BrowserPages;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("reimagined")
@PrefixGameTestTemplate(false)
public final class BrowserViewGameTests {
    @GameTest(template = "empty")
    public static void actualRegistryItemsRespectSearchCategoriesAndEmptyPagination(GameTestHelper helper) {
        helper.assertTrue(BrowserItems.category(new ItemStack(Items.OAK_LOG)) == BrowserItems.Category.BLOCKS,
                "Real block items must appear in the block category");
        helper.assertTrue(BrowserItems.category(new ItemStack(Items.APPLE)) == BrowserItems.Category.FOOD,
                "Real food components must drive food category");
        helper.assertTrue(BrowserItems.category(new ItemStack(Items.IRON_SWORD)) == BrowserItems.Category.EQUIPMENT,
                "Damageable equipment must appear in equipment");
        var candidates = List.of(new ItemStack(Items.OAK_LOG), new ItemStack(Items.IRON_SWORD), new ItemStack(Items.APPLE));
        var match = BrowserItems.filter(candidates, "IRON @MINECRAFT sword", BrowserItems.Category.EQUIPMENT);
        helper.assertTrue(match.size() == 1 && match.getFirst().is(Items.IRON_SWORD), "Every search term and selected category must apply together");
        match.getFirst().setCount(7);
        helper.assertTrue(candidates.get(1).getCount() == 1, "Search results must not mutate registry/source stacks");
        var empty = BrowserItems.filter(candidates, "sword", BrowserItems.Category.FOOD);
        helper.assertTrue(empty.isEmpty() && BrowserPages.clamp(20, empty.size(), 54) == 0,
                "Filters with no results must safely return to page zero");
        helper.succeed();
    }
    @GameTest(template = "empty")
    public static void potionCategoryContainsDistinctNamedBottleVariants(GameTestHelper helper) {
        var entries = BrowserItems.items("@minecraft", BrowserItems.Category.BREWING);
        var healing = PotionContents.createItemStack(Items.POTION, Potions.HEALING);
        var awkward = PotionContents.createItemStack(Items.POTION, Potions.AWKWARD);
        var splash = PotionContents.createItemStack(Items.SPLASH_POTION, Potions.HEALING);
        for (var expected : List.of(healing, awkward, splash))
            helper.assertTrue(entries.stream().anyMatch(stack -> ItemStack.isSameItemSameComponents(stack, expected)),
                    "Named potion variants must be selectable from the real UI item projection");
        var longVariants = BrowserItems.items("long_swiftness @minecraft", BrowserItems.Category.BREWING);
        helper.assertTrue(longVariants.size() == 3 && longVariants.stream().allMatch(stack ->
                stack.get(DataComponents.POTION_CONTENTS).potion().orElseThrow().is(Potions.LONG_SWIFTNESS)),
                "Potion registry IDs must distinguish duration/strength variants even when names are identical");
        helper.assertTrue(entries.stream().allMatch(stack -> stack.get(DataComponents.POTION_CONTENTS).potion().isPresent()),
                "Do not expose empty-content defaults instead of actual potion variants");
        helper.succeed();
    }
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void uiCacheTracksReloadAndClearsWorldWithoutRebuildingEveryTick(GameTestHelper helper) {
        var level = helper.getLevel();
        var manager = new net.minecraft.world.item.crafting.RecipeManager(level.registryAccess());
        var bread = level.getRecipeManager().byKey(net.minecraft.resources.ResourceLocation.withDefaultNamespace("bread")).orElseThrow();
        manager.replaceRecipes(List.of(bread));
        var cache = new dev.reimagined.browser.BrowserSnapshotCache();
        Object context = new Object();
        var before = cache.get(context, manager, level.registryAccess(), level.potionBrewing());
        helper.assertTrue(before == cache.get(context, manager, level.registryAccess(), level.potionBrewing()),
                "Unchanged screen ticks must reuse the same snapshot rather than regenerate brewing displays");
        manager.replaceRecipes(List.of());
        var after = cache.get(context, manager, level.registryAccess(), level.potionBrewing());
        helper.assertTrue(after.revision() > before.revision() && after.recipes().isEmpty() && before.recipes().size() == 1,
                "Recipe replacement must invalidate the actual UI cache and preserve old immutable snapshots");
        helper.assertTrue(after.brewing() == before.brewing(), "Recipe-only reload must reuse unchanged brewing projections");
        cache.invalidateRecipes();
        helper.assertTrue(cache.get(context, manager, level.registryAccess(), level.potionBrewing()).revision() > after.revision(),
                "Explicit recipe-sync events must invalidate even an unchanged manager object");
        var cleared = cache.clear();
        helper.assertTrue(cleared.recipes().isEmpty() && cleared.brewing().isEmpty(), "Logout must discard world-bound cached data");
        helper.succeed();
    }
}
