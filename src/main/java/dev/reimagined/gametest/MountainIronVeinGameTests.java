package dev.reimagined.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.biome.Biomes;
import java.util.EnumSet;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.placement.HeightRangePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("reimagined")
@PrefixGameTestTemplate(false)
public final class MountainIronVeinGameTests {
    private static final ResourceLocation FEATURE_ID = ResourceLocation.fromNamespaceAndPath("reimagined", "mountain_iron_vein");
    private static final ResourceKey<PlacedFeature> FEATURE = ResourceKey.create(Registries.PLACED_FEATURE, FEATURE_ID);

    private MountainIronVeinGameTests() {}

    @GameTest(template = "empty")
    public static void ironVeinIsLimitedToMountainBiomes(GameTestHelper helper) {
        var registries = helper.getLevel().registryAccess();
        var placed = registries.registryOrThrow(Registries.PLACED_FEATURE).getHolderOrThrow(FEATURE).value();
        var mountains = registries.registryOrThrow(Registries.BIOME);
        var mountainTag = net.minecraft.tags.TagKey.create(Registries.BIOME,
                ResourceLocation.withDefaultNamespace("is_mountain"));
        for (var biome : mountains.holders().toList()) {
            boolean tagged = biome.is(mountainTag);
            helper.assertTrue(tagged == biome.value().getGenerationSettings().hasFeature(placed),
                    "Iron vein must be added to exactly minecraft:is_mountain biomes: " + biome);
        }
        helper.assertTrue(mountains.getHolderOrThrow(Biomes.STONY_PEAKS).value().getGenerationSettings().hasFeature(placed),
                "Stony Peaks must receive the mountain vein");
        helper.assertTrue(mountains.getHolderOrThrow(Biomes.FOREST).value().getGenerationSettings().hasFeature(placed) == false,
                "Ordinary forest must not receive a mountain vein");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void configuredVeinActuallyPlacesIronOreInStone(GameTestHelper helper) {
        var level = helper.getLevel();
        var registries = level.registryAccess();
        var key = ResourceKey.create(Registries.CONFIGURED_FEATURE, FEATURE_ID);
        var configured = registries.registryOrThrow(Registries.CONFIGURED_FEATURE).getHolderOrThrow(key).value();
        var structureOrigin = helper.absolutePos(BlockPos.ZERO);
        int startX = (structureOrigin.getX() & -16) + 2;
        int startZ = (structureOrigin.getZ() & -16) + 2;
        int bottomY = 20;
        for (int x = startX; x < startX + 12; x++) {
            for (int y = bottomY; y < bottomY + 15; y++) {
                for (int z = startZ; z < startZ + 12; z++) {
                    level.setBlock(new BlockPos(x, y, z), Blocks.STONE.defaultBlockState(), 3);
                }
            }
        }
        var chunk = level.getChunk(startX >> 4, startZ >> 4);
        Heightmap.primeHeightmaps(chunk, EnumSet.of(Heightmap.Types.OCEAN_FLOOR_WG));
        var generator = level.getChunkSource().getGenerator();
        var context = new PlacementContext(level, generator, java.util.Optional.empty());
        var random = RandomSource.create(0x1A0F7L);
        var candidate = new BlockPos(startX + 6, bottomY + 7, startZ + 6);
        var heightRange = HeightRangePlacement.uniform(VerticalAnchor.absolute(20), VerticalAnchor.absolute(30));
        var candidates = heightRange.getPositions(context, random, candidate).toList();
        boolean placed = false;
        for (var pos : candidates) placed |= configured.place(level, generator, random, pos);
        int ironOre = 0;
        for (int x = startX; x < startX + 12; x++) {
            for (int y = bottomY; y < bottomY + 15; y++) {
                for (int z = startZ; z < startZ + 12; z++) {
                    if (level.getBlockState(new BlockPos(x, y, z)).is(Blocks.IRON_ORE)) ironOre++;
                }
            }
        }
        helper.assertTrue(!candidates.isEmpty() && placed && ironOre > 0,
                "Placed-feature modifiers must produce a candidate and configured vanilla ore feature must replace stone with actual iron ore blocks");
        helper.succeed();
    }
}
