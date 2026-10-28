package dev.reimagined.gametest;

import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("reimagined")
@PrefixGameTestTemplate(false)
public final class WorldFeatureGameTests {
    private static final ResourceLocation BOULDER_ID = ResourceLocation.fromNamespaceAndPath("reimagined", "mossy_forest_boulder");
    private static final ResourceKey<PlacedFeature> BOULDER = ResourceKey.create(Registries.PLACED_FEATURE, BOULDER_ID);

    private WorldFeatureGameTests() {}

    @GameTest(template = "empty")
    public static void forestBoulderFeatureIsRegisteredAndAddedByBiomeModifier(GameTestHelper helper) {
        var registries = helper.getLevel().registryAccess();
        var feature = registries.registryOrThrow(Registries.PLACED_FEATURE).getHolderOrThrow(BOULDER);
        var forest = registries.registryOrThrow(Registries.BIOME).getHolderOrThrow(Biomes.FOREST);
        var birchForest = registries.registryOrThrow(Registries.BIOME).getHolderOrThrow(Biomes.BIRCH_FOREST);
        helper.assertTrue(forest.value().getGenerationSettings().hasFeature(feature.value()),
                "Vanilla forest must contain the modded mossy boulder feature");
        helper.assertTrue(birchForest.value().getGenerationSettings().hasFeature(feature.value()),
                "Birch forests use the vanilla is_forest tag and must also contain the boulder");
        helper.assertTrue(feature.value().feature().value().config().getClass().getSimpleName().equals("BlockStateConfiguration"),
                "Boulder must be a codec-loaded configured block feature");
        helper.succeed();
    }
}
