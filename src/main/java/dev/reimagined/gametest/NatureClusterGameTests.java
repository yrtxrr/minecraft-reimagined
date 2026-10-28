package dev.reimagined.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.feature.configurations.RandomPatchConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("reimagined")
@PrefixGameTestTemplate(false)
public final class NatureClusterGameTests {
    @GameTest(template="empty")
    public static void clustersDecodeRemainSparseAndUseSuitableDistinctBiomes(GameTestHelper h) {
        var regs=h.getLevel().registryAccess();var biomes=regs.registryOrThrow(Registries.BIOME);
        for(String name:new String[]{"grass","fern"}) {
            var feature=regs.registryOrThrow(Registries.PLACED_FEATURE).getHolderOrThrow(ResourceKey.create(
                    Registries.PLACED_FEATURE,ResourceLocation.parse("reimagined:"+name+"_clusters"))).value();
            var json=PlacedFeature.DIRECT_CODEC.encodeStart(regs.createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE),feature).getOrThrow().getAsJsonObject();
            int chance=name.equals("grass")?8:10;
            h.assertTrue(json.getAsJsonArray("placement").size()==4 && json.getAsJsonArray("placement").get(0).getAsJsonObject().get("chance").getAsInt()==chance,"One sparse candidate, no count multiplier");
            var config=(RandomPatchConfiguration)feature.feature().value().config();
            h.assertTrue(config.tries()==(name.equals("grass")?12:8) && config.xzSpread()==3 && config.ySpread()==1,"Bounded local vanilla patch attempts");
            h.assertTrue(biomes.getHolderOrThrow(Biomes.FOREST).value().getGenerationSettings().hasFeature(feature),"Forests receive both additions");
            h.assertTrue(!biomes.getHolderOrThrow(Biomes.DESERT).value().getGenerationSettings().hasFeature(feature)
                    && !biomes.getHolderOrThrow(Biomes.OCEAN).value().getGenerationSettings().hasFeature(feature),"No desert/ocean vegetation injection");
            h.assertTrue(biomes.getHolderOrThrow(Biomes.PLAINS).value().getGenerationSettings().hasFeature(feature)==name.equals("grass"),"Fern is not a plains patch");
            if(name.equals("fern")) h.assertTrue(biomes.getHolderOrThrow(Biomes.OLD_GROWTH_PINE_TAIGA).value().getGenerationSettings().hasFeature(feature)
                    && biomes.getHolderOrThrow(Biomes.OLD_GROWTH_SPRUCE_TAIGA).value().getGenerationSettings().hasFeature(feature),"Real valid old growth biome IDs");
            var context=new net.minecraft.world.level.levelgen.placement.PlacementContext(h.getLevel(),h.getLevel().getChunkSource().getGenerator(),java.util.Optional.of(feature));
            var random=RandomSource.create(820);int accepted=0;
            for(int n=0;n<4096;n++) {
                long count=feature.placement().getFirst().getPositions(context,random,h.absolutePos(new BlockPos(7,3,7))).count();
                h.assertTrue(count<=1,"At most one cluster candidate per chunk");accepted+=count;
            }
            h.assertTrue(accepted>300 && accepted<650,"Sample remains sparse at the declared rarity");
        }
        h.succeed();
    }
    @GameTest(template="empty")
    public static void grassPatchPlacesBothHalvesAndFernSurvives(GameTestHelper h) {
        for(int x=1;x<=13;x++) for(int z=1;z<=13;z++) h.setBlock(new BlockPos(x,2,z),Blocks.GRASS_BLOCK);
        for(String name:new String[]{"grass","fern"}) {
            for(int x=1;x<=13;x++) for(int z=1;z<=13;z++) for(int y=3;y<=4;y++) h.setBlock(new BlockPos(x,y,z),Blocks.AIR);
            var feature=h.getLevel().registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE).getHolderOrThrow(ResourceKey.create(
                    Registries.CONFIGURED_FEATURE,ResourceLocation.parse("reimagined:"+name+"_cluster"))).value();
            h.assertTrue(feature.place(h.getLevel(),h.getLevel().getChunkSource().getGenerator(),RandomSource.create(820),h.absolutePos(new BlockPos(7,3,7))),"Live feature must place real plants");
            int count=0;
            for(int x=1;x<=13;x++) for(int z=1;z<=13;z++) {
                var p=new BlockPos(x,3,z);var state=h.getBlockState(p);
                if(state.is(name.equals("grass")?Blocks.TALL_GRASS:Blocks.FERN)) {
                    count++;h.assertTrue(state.canSurvive(h.getLevel(),h.absolutePos(p)),"Plants survive on generated soil");
                    if(name.equals("grass")) h.assertTrue(state.getValue(DoublePlantBlock.HALF)==DoubleBlockHalf.LOWER
                            && h.getBlockState(p.above()).is(Blocks.TALL_GRASS)
                            && h.getBlockState(p.above()).getValue(DoublePlantBlock.HALF)==DoubleBlockHalf.UPPER,"Tall grass must not be an orphan lower half");
                }
            }
            h.assertTrue(count>0 && count<=(name.equals("grass")?12:8),"Actual placement obeys patch budget");
        }
        h.succeed();
    }
    @GameTest(template="empty")
    public static void combinedOverworldFeatureOrderHasNoBiomeModifierCycle(GameTestHelper h) {
        var generator=h.getLevel().registryAccess().registryOrThrow(Registries.WORLD_PRESET)
                .getOrThrow(net.minecraft.world.level.levelgen.presets.WorldPresets.NORMAL).overworld().orElseThrow().generator();
        var steps=net.minecraft.world.level.biome.FeatureSorter.buildFeaturesPerStep(
                java.util.List.copyOf(generator.getBiomeSource().possibleBiomes()),b->b.value().getGenerationSettings().features(),true);
        h.assertTrue(!steps.isEmpty(),"Combined vanilla, orchard, clover, boulder, grass and fern order must resolve without a cycle");h.succeed();
    }
}
