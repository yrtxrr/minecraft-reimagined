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
    @GameTest(template="empty")
    public static void boulderRarityIsOneInEighteenWithNoDensityMultiplier(GameTestHelper h) {
        var feature=h.getLevel().registryAccess().registryOrThrow(Registries.PLACED_FEATURE).getHolderOrThrow(BOULDER).value();
        var ops=h.getLevel().registryAccess().createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE);
        var json=PlacedFeature.DIRECT_CODEC.encodeStart(ops,feature).getOrThrow().getAsJsonObject();
        var modifiers=json.getAsJsonArray("placement");
        h.assertTrue(modifiers.size()==4 && modifiers.get(0).getAsJsonObject().get("chance").getAsInt()==18,
                "Single rarity18 attempt, not a per-chunk count multiplier");
        h.assertTrue(modifiers.get(0).getAsJsonObject().get("type").getAsString().equals("minecraft:rarity_filter"),"Rarity filter must precede placement");
        var context=new net.minecraft.world.level.levelgen.placement.PlacementContext(h.getLevel(),h.getLevel().getChunkSource().getGenerator(),java.util.Optional.of(feature));
        var random=net.minecraft.util.RandomSource.create(1818);int accepted=0;
        for(int n=0;n<4096;n++) {
            long count=feature.placement().getFirst().getPositions(context,random,h.absolutePos(new net.minecraft.core.BlockPos(7,3,7))).count();
            h.assertTrue(count<=1,"One candidate maximum per eligible chunk");accepted+=count;
        }
        h.assertTrue(accepted>=150 && accepted<=320,"Rarity sample must remain sparse (nominal1/18)");
        var desert=h.getLevel().registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(Biomes.DESERT);
        h.assertTrue(!desert.value().getGenerationSettings().hasFeature(feature),"No boulders injected into deserts");h.succeed();
    }
    @GameTest(template="empty")
    public static void actualBoulderFeatureCreatesBoundedMossyRockOnSoil(GameTestHelper h) {
        for(int x=1;x<=13;x++)for(int z=1;z<=13;z++) h.setBlock(new net.minecraft.core.BlockPos(x,2,z),net.minecraft.world.level.block.Blocks.DIRT);
        var key=ResourceKey.create(Registries.CONFIGURED_FEATURE,BOULDER_ID);
        var feature=h.getLevel().registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE).getHolderOrThrow(key).value();
        h.assertTrue(feature.place(h.getLevel(),h.getLevel().getChunkSource().getGenerator(),net.minecraft.util.RandomSource.create(18),h.absolutePos(new net.minecraft.core.BlockPos(7,3,7))),"Real vanilla forest_rock feature must place on soil");
        int blocks=0;
        for(int x=1;x<=13;x++)for(int z=1;z<=13;z++)for(int y=0;y<=7;y++)
            if(h.getBlockState(new net.minecraft.core.BlockPos(x,y,z)).is(net.minecraft.world.level.block.Blocks.MOSSY_COBBLESTONE))blocks++;
        h.assertTrue(blocks>0 && blocks<=375,"Three small vanilla rock blobs, no huge/generated structure");h.succeed();
    }
}
