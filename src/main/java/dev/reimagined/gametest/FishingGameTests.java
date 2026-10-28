package dev.reimagined.gametest;

import dev.reimagined.fishing.FishContent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("reimagined")
@PrefixGameTestTemplate(false)
public final class FishingGameTests {
    private static final ResourceKey<LootTable> EXTRA_FISH = ResourceKey.create(
            net.minecraft.core.registries.Registries.LOOT_TABLE,
            ResourceLocation.fromNamespaceAndPath("reimagined", "gameplay/extra_fish_from_fishing"));

    private FishingGameTests() {}

    @GameTest(template = "empty")
    public static void threeFishAreRegisteredEdibleAndHaveCookRecipes(GameTestHelper helper) {
        var level = helper.getLevel();
        for (var fish : new ItemStack[]{new ItemStack(FishContent.RIVER_PERCH.get()),
                new ItemStack(FishContent.SILVER_TROUT.get()), new ItemStack(FishContent.BLUE_MACKEREL.get())}) {
            helper.assertTrue(BuiltInRegistries.ITEM.getKey(fish.getItem()) != null, "Every new fish must be registered");
            helper.assertTrue(fish.getFoodProperties(null) != null, "New fish must be edible");
            String path = BuiltInRegistries.ITEM.getKey(fish.getItem()).getPath();
            var recipeId = ResourceLocation.fromNamespaceAndPath("reimagined", path + "_cooked");
            var recipe = level.getRecipeManager().byKey(recipeId).orElseThrow(
                    () -> new AssertionError("Missing simple cooking recipe " + recipeId));
            helper.assertTrue(recipe.value().getResultItem(level.registryAccess()).is(Items.COOKED_COD),
                    "Simple fish cooking recipe should yield cooked cod");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void vanillaFishingLootIncludesTheThreeNewFishWithoutReplacingVanillaCatch(GameTestHelper helper) {
        var level = helper.getLevel();
        var extraFishTable = level.getServer().reloadableRegistries().getLootTable(EXTRA_FISH);
        helper.assertTrue(extraFishTable != LootTable.EMPTY, "Extra fish loot table must load from datapack");
        FishingHook hook = new FishingHook(EntityType.FISHING_BOBBER, level);
        var fishParams = new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, hook.position())
                .withParameter(LootContextParams.TOOL, new ItemStack(Items.FISHING_ROD))
                .withParameter(LootContextParams.THIS_ENTITY, hook)
                .create(LootContextParamSets.FISHING);
        var caughtTypes = new java.util.HashSet<Item>();
        var vanillaFishing = level.getServer().reloadableRegistries().getLootTable(BuiltInLootTables.FISHING);
        helper.assertTrue(vanillaFishing != LootTable.EMPTY, "Vanilla fishing loot table must remain loaded");
        for (long seed = 0; seed < 256 && caughtTypes.size() < 3; seed++) {
            for (ItemStack stack : vanillaFishing.getRandomItems(fishParams, seed)) {
                if (stack.is(FishContent.RIVER_PERCH.get()) || stack.is(FishContent.SILVER_TROUT.get())
                        || stack.is(FishContent.BLUE_MACKEREL.get())) caughtTypes.add(stack.getItem());
            }
        }
        helper.assertTrue(caughtTypes.size() == 3,
                "Vanilla fishing loot modifier must produce all three extra fish while retaining the vanilla table");
        helper.succeed();
    }
}
