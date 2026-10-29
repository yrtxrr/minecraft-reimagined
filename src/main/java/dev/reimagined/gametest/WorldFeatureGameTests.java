package dev.reimagined.gametest;

import com.mojang.serialization.JsonOps;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.RarityFilter;
import net.minecraft.world.level.levelgen.placement.SurfaceWaterDepthFilter;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.HeightmapPlacement;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
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
        var biomes = registries.registryOrThrow(Registries.BIOME);
        for (var biome : biomes.holders().toList()) {
            boolean isForest = biome.is(net.minecraft.tags.TagKey.create(Registries.BIOME,
                    ResourceLocation.withDefaultNamespace("is_forest")));
            helper.assertTrue(isForest == biome.value().getGenerationSettings().hasFeature(feature.value()),
                    "Boulder must appear in exactly the vanilla forest biome tag; unexpected membership: " + biome);
        }
        helper.assertTrue(birchForest.value().getGenerationSettings().hasFeature(feature.value()),
                "Birch forests use the vanilla is_forest tag and must also contain the boulder");
        helper.assertTrue(feature.value().feature().value().config().getClass().getSimpleName().equals("BlockStateConfiguration"),
                "Boulder must be a codec-loaded configured block feature");
        var placements = feature.value().placement();
        helper.assertTrue(placements.size() == 5, "Boulder placement must remain a short bounded chain");
        helper.assertTrue(placements.get(0) instanceof RarityFilter,
                "Rare chance filter must run first, before terrain checks");
        helper.assertTrue(placements.get(1) instanceof InSquarePlacement,
                "Feature must choose positions inside the current chunk");
        helper.assertTrue(placements.get(2) instanceof SurfaceWaterDepthFilter,
                "Boulders must not be placed in water");
        helper.assertTrue(placements.get(3) instanceof HeightmapPlacement,
                "Boulder height must come from the local terrain heightmap");
        helper.assertTrue(placements.get(4) instanceof BiomeFilter,
                "Placed feature must retain the vanilla biome filter");
        var rarityJson = net.minecraft.world.level.levelgen.placement.PlacementModifier.CODEC
                .encodeStart(JsonOps.INSTANCE, placements.get(0)).getOrThrow();
        helper.assertTrue(rarityJson.getAsJsonObject().get("chance").getAsInt() == 18,
                "Boulder rarity must remain one attempt in eighteen");
        helper.succeed();
    }
}
