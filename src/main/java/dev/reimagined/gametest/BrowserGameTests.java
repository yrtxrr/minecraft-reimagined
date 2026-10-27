package dev.reimagined.gametest;

import dev.reimagined.browser.BrowserRecipes;
import dev.reimagined.browser.RecipeDisplay;
import dev.reimagined.browser.RecipeDisplayAdapter;
import dev.reimagined.engineering.EngineeringContent;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Tests the same common display projection used by the client, against actual game recipes. */
@GameTestHolder("reimagined")
@PrefixGameTestTemplate(false)
public final class BrowserGameTests {
    private BrowserGameTests() {}
    private static ResourceLocation vanilla(String path) { return ResourceLocation.withDefaultNamespace(path); }
    private static List<RecipeDisplay> snapshot(GameTestHelper helper) {
        var level = helper.getLevel();
        return BrowserRecipes.snapshot(level.getRecipeManager(), level.registryAccess(), level.potionBrewing());
    }
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void craftingAndMillingExposeIngredientsResultsAndUsages(GameTestHelper helper) {
        var displays = snapshot(helper);
        var bread = displays.stream().filter(display -> display.id().equals(vanilla("bread"))).findFirst().orElseThrow();
        helper.assertTrue(bread.columns() == 3 && bread.inputs().size() == 3 && bread.result().is(Items.BREAD),
                "Shaped bread recipe must preserve all three ingredient positions and its output");
        helper.assertTrue(bread.uses(new ItemStack(Items.WHEAT)) && bread.produces(new ItemStack(Items.BREAD))
                && !bread.uses(new ItemStack(Items.DIAMOND)), "Usage lookup must inspect actual ingredient predicates");
        var milling = displays.stream().filter(display -> display.id().toString().equals("reimagined:milling_wheat_flour"))
                .findFirst().orElseThrow();
        helper.assertTrue(milling.uses(new ItemStack(Items.WHEAT)) && milling.result().is(EngineeringContent.FLOUR.get())
                && milling.station().is(EngineeringContent.MILLSTONE.get().asItem()),
                "Custom milling must share ingredient/result/workstation navigation with vanilla crafting");
        helper.assertTrue(displays.stream().anyMatch(display -> display.uses(new ItemStack(Items.RAW_IRON))
                        && display.produces(new ItemStack(Items.IRON_INGOT)) && display.station().is(Items.FURNACE)),
                "Furnace input/output and workstation must survive the generic recipe projection");
        helper.assertTrue(displays.stream().anyMatch(display -> display.uses(new ItemStack(Items.NETHERITE_INGOT))
                        && display.produces(new ItemStack(Items.NETHERITE_SWORD)) && display.station().is(Items.SMITHING_TABLE)),
                "Vanilla smithing transformation must expose its actual ingredients and station");
        ItemStack leaked = milling.result(); leaked.setCount(64);
        helper.assertTrue(milling.result().getCount() == 1, "Callers must not mutate the cached display output");
        ItemStack option = bread.ingredient(0, 0); option.setCount(64);
        helper.assertTrue(bread.ingredient(0, -60).getCount() == 1, "Ingredient animation must expose copied stacks and support wrapped time");
        helper.succeed();
    }
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void brewingUsesRealPotionContentsAndVanillaMixes(GameTestHelper helper) {
        var displays = BrowserRecipes.brewingDisplays(helper.getLevel().potionBrewing());
        ItemStack awkward = PotionContents.createItemStack(Items.POTION, Potions.AWKWARD);
        ItemStack healing = PotionContents.createItemStack(Items.POTION, Potions.HEALING);
        ItemStack swiftness = PotionContents.createItemStack(Items.POTION, Potions.SWIFTNESS);
        ItemStack strongHealing = PotionContents.createItemStack(Items.POTION, Potions.STRONG_HEALING);
        ItemStack longSwiftness = PotionContents.createItemStack(Items.POTION, Potions.LONG_SWIFTNESS);
        var healingRecipe = displays.stream().filter(display -> display.uses(awkward)
                && display.uses(new ItemStack(Items.GLISTERING_MELON_SLICE)) && display.produces(healing)).findFirst().orElseThrow();
        helper.assertTrue(healingRecipe.station().is(Items.BREWING_STAND) && !healingRecipe.uses(healing)
                && !healingRecipe.produces(swiftness), "Potion inputs and outputs must not conflate different contents on the same bottle item");
        helper.assertTrue(displays.stream().anyMatch(display -> display.uses(healing)
                        && display.uses(new ItemStack(Items.GLOWSTONE_DUST)) && display.produces(strongHealing)),
                "Glowstone strengthening must be obtained from the actual brewing API");
        helper.assertTrue(displays.stream().anyMatch(display -> display.uses(swiftness)
                        && display.uses(new ItemStack(Items.REDSTONE)) && display.produces(longSwiftness)),
                "Redstone duration extension must preserve its potion variant");
        helper.succeed();
    }
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void brewingContainerConversionsPreservePotionAndRejectFalseReagents(GameTestHelper helper) {
        var brewing = helper.getLevel().potionBrewing();
        var displays = BrowserRecipes.brewingDisplays(brewing);
        ItemStack drink = PotionContents.createItemStack(Items.POTION, Potions.HEALING);
        ItemStack splash = PotionContents.createItemStack(Items.SPLASH_POTION, Potions.HEALING);
        ItemStack lingering = PotionContents.createItemStack(Items.LINGERING_POTION, Potions.HEALING);
        helper.assertTrue(displays.stream().anyMatch(display -> display.uses(drink)
                && display.uses(new ItemStack(Items.GUNPOWDER)) && display.produces(splash)),
                "Gunpowder must convert the selected drinkable variant into the matching splash variant");
        helper.assertTrue(displays.stream().anyMatch(display -> display.uses(splash)
                && display.uses(new ItemStack(Items.DRAGON_BREATH)) && display.produces(lingering)),
                "Dragon breath must convert matching splash contents into lingering contents");
        helper.assertTrue(displays.stream().noneMatch(display -> display.uses(new ItemStack(Items.DIAMOND))),
                "Non-reagents must not acquire fabricated brewing usages");
        helper.succeed();
    }
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void simpleBrewingApiPreservesAlternativeInputsAndOutputCount(GameTestHelper helper) {
        var builder = new PotionBrewing.Builder(FeatureFlags.DEFAULT_FLAGS);
        builder.addRecipe(Ingredient.of(Items.COBBLESTONE, Items.STONE), Ingredient.of(Items.WHEAT), new ItemStack(Items.BREAD, 2));
        var displays = BrowserRecipes.brewingDisplays(builder.build());
        helper.assertTrue(displays.size() == 1, "Public simple brewing recipes must be displayed even with non-potion inputs");
        var display = displays.getFirst();
        helper.assertTrue(display.uses(new ItemStack(Items.COBBLESTONE)) && display.uses(new ItemStack(Items.STONE))
                && display.uses(new ItemStack(Items.WHEAT)) && display.result().getCount() == 2,
                "Alternative inputs and counted outputs must survive projection");
        var first = display.ingredient(0, 0);
        var second = display.ingredient(0, 30);
        helper.assertTrue(!ItemStack.isSameItem(first, second)
                && ItemStack.isSameItem(first, display.ingredient(0, 60))
                && ItemStack.isSameItem(second, display.ingredient(0, -30)),
                "Alternative animation must cycle, wrap and remain valid for negative game time");
        helper.succeed();
    }
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void replacedRecipeCollectionsProduceFreshDisplaySnapshots(GameTestHelper helper) {
        var level = helper.getLevel();
        var manager = new RecipeManager(level.registryAccess());
        RecipeHolder<?> bread = level.getRecipeManager().byKey(vanilla("bread")).orElseThrow();
        manager.replaceRecipes(List.of(bread));
        var before = BrowserRecipes.recipeDisplays(manager, level.registryAccess(), ItemStack.EMPTY, List.of());
        helper.assertTrue(before.stream().anyMatch(display -> display.id().equals(bread.id())), "First snapshot must include initial recipes");
        manager.replaceRecipes(List.of());
        var after = BrowserRecipes.recipeDisplays(manager, level.registryAccess(), ItemStack.EMPTY, List.of());
        helper.assertTrue(after.stream().noneMatch(display -> display.id().equals(bread.id()))
                && before.stream().anyMatch(display -> display.id().equals(bread.id())),
                "A new reload snapshot must remove deleted recipes while leaving a previously returned snapshot stable");
        helper.succeed();
    }
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void dynamicRecipesRequireExplicitContextAwareAdapters(GameTestHelper helper) {
        var level = helper.getLevel();
        var dynamic = level.getRecipeManager().getRecipes().stream()
                .filter(holder -> holder.value().getResultItem(level.registryAccess()).isEmpty()).findFirst().orElseThrow();
        var manager = new RecipeManager(level.registryAccess());
        manager.replaceRecipes(List.of(dynamic));
        var initial = BrowserRecipes.snapshot(manager, level.registryAccess(), level.potionBrewing());
        helper.assertTrue(initial.stream().noneMatch(display -> display.id().equals(dynamic.id())),
                "Unknown dynamic output must not be fabricated by the generic projection");
        RecipeDisplayAdapter adapter = new RecipeDisplayAdapter() {
            @Override public boolean supports(Recipe<?> recipe) { return recipe == dynamic.value(); }
            @Override public List<RecipeDisplay> displays(RecipeHolder<?> holder, HolderLookup.Provider registries, ItemStack selected) {
                return List.of(new RecipeDisplay(holder.id(), List.of(Ingredient.of(selected)), selected,
                        new ItemStack(Items.CRAFTING_TABLE), 1, Component.empty()));
            }
        };
        ItemStack requested = new ItemStack(Items.LEATHER_CHESTPLATE);
        var adapted = BrowserRecipes.snapshot(manager, level.registryAccess(), level.potionBrewing(), requested, List.of(adapter));
        helper.assertTrue(adapted.stream().anyMatch(display -> display.id().equals(dynamic.id()) && display.produces(requested)
                        && display.uses(requested)), "Explicit dynamic adapter must receive the selected concrete context");
        helper.succeed();
    }
}
