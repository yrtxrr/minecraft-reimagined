package dev.reimagined.gametest;

import com.mojang.serialization.JsonOps;
import io.netty.buffer.Unpooled;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Exercise the actual mod entry point, datapacks and both recipe serialization paths. */
@GameTestHolder("reimagined")
@PrefixGameTestTemplate(false)
public final class RegistryGameTests {
    private RegistryGameTests() {}
    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("reimagined", path); }

    @GameTest(template = "empty")
    public static void allGameplayModulesRegisterTheirContent(GameTestHelper helper) {
        for (String name : new String[]{"water_wheel", "shaft", "millstone"}) {
            helper.assertTrue(BuiltInRegistries.BLOCK.containsKey(id(name)), "Missing block registration: " + name);
            helper.assertTrue(BuiltInRegistries.BLOCK.get(id(name)).asItem() == BuiltInRegistries.ITEM.get(id(name)),
                    "Block item registration mismatch: " + name);
        }
        for (String name : new String[]{"water_wheel", "shaft", "millstone", "flour", "deer_spawn_egg",
                "elemental_wand", "nature_wand", "arcane_wand", "ritual_wand", "research_journal",
                "iron_spear", "iron_hammer", "iron_dagger", "iron_halberd"}) {
            helper.assertTrue(BuiltInRegistries.ITEM.containsKey(id(name)), "Missing item registration: " + name);
        }
        helper.assertTrue(BuiltInRegistries.ENTITY_TYPE.containsKey(id("deer")), "Missing deer EntityType");
        helper.assertTrue(BuiltInRegistries.BLOCK_ENTITY_TYPE.containsKey(id("machine")), "Missing machine BlockEntityType");
        helper.assertTrue(BuiltInRegistries.RECIPE_TYPE.containsKey(id("milling")), "Missing milling RecipeType");
        helper.assertTrue(BuiltInRegistries.RECIPE_SERIALIZER.containsKey(id("milling")), "Missing milling serializer");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void allModRecipesLoadAndRoundtripThroughDataAndNetworkCodecs(GameTestHelper helper) {
        var level = helper.getLevel();
        var manager = level.getRecipeManager();
        var ops = level.registryAccess().createSerializationContext(JsonOps.INSTANCE);
        int count = 0;
        for (String name : new String[]{"water_wheel", "shaft", "millstone", "flour_bread", "milling_wheat_flour",
                "milling_cobblestone_gravel", "milling_gravel_sand", "elemental_wand", "nature_wand", "arcane_wand",
                "ritual_wand", "research_journal", "iron_spear", "iron_hammer", "iron_dagger", "iron_halberd"}) {
            var holder = manager.byKey(id(name)).orElseThrow(() -> new AssertionError("Missing recipe: " + name));
            Recipe<?> original = holder.value();
            var json = Recipe.CODEC.encodeStart(ops, original).getOrThrow();
            Recipe<?> dataCopy = Recipe.CODEC.parse(ops, json).getOrThrow();
            var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), level.registryAccess());
            try {
                Recipe.STREAM_CODEC.encode(buffer, original);
                Recipe<?> networkCopy = Recipe.STREAM_CODEC.decode(buffer);
                helper.assertTrue(networkCopy.getType() == original.getType() && dataCopy.getType() == original.getType(),
                        "Recipe type changed in serialization: " + name);
                ItemStack result = original.getResultItem(level.registryAccess());
                for (Recipe<?> copy : new Recipe<?>[]{dataCopy, networkCopy}) {
                    ItemStack decoded = copy.getResultItem(level.registryAccess());
                    helper.assertTrue(ItemStack.isSameItemSameComponents(result, decoded) && result.getCount() == decoded.getCount(),
                            "Recipe result changed in serialization: " + name);
                    helper.assertTrue(copy.getIngredients().size() == original.getIngredients().size(),
                            "Recipe ingredients changed in serialization: " + name);
                }
            } finally { buffer.release(); }
            count++;
        }
        helper.assertTrue(count == 16, "All 16 mod recipes must be tested");
        helper.succeed();
    }
}
