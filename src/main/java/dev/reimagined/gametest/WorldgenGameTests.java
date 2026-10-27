package dev.reimagined.gametest;

import com.mojang.serialization.JsonOps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Exercises the live datapack codecs and noise/surface pipeline, without loading world chunks. */
@GameTestHolder("reimagined")
@PrefixGameTestTemplate(false)
public final class WorldgenGameTests {
    private static final ResourceLocation CONTINENTS = ResourceLocation.fromNamespaceAndPath("reimagined", "continents");
    private static final ResourceKey<WorldPreset> PRESET = ResourceKey.create(Registries.WORLD_PRESET, CONTINENTS);
    private static final ResourceKey<NoiseGeneratorSettings> SETTINGS = ResourceKey.create(Registries.NOISE_SETTINGS, CONTINENTS);
    private static final long SEED = 0x5265696d6167696eL;

    private WorldgenGameTests() {}

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void customPresetRoundTripsAndPreservesVanilla(GameTestHelper helper) {
        var access = helper.getLevel().registryAccess();
        var presets = access.registryOrThrow(Registries.WORLD_PRESET);
        var preset = presets.getOrThrow(PRESET);
        var ops = RegistryOps.create(JsonOps.INSTANCE, access);
        var encoded = WorldPreset.DIRECT_CODEC.encodeStart(ops, preset).getOrThrow();
        var decoded = WorldPreset.DIRECT_CODEC.parse(ops, encoded).getOrThrow();
        var custom = (NoiseBasedChunkGenerator) decoded.overworld().orElseThrow().generator();
        helper.assertTrue(custom.stable(SETTINGS), "Preset codec must preserve custom noise-settings binding");
        helper.assertTrue(custom.getMinY() == -64 && custom.getGenDepth() == 384,
                "Custom preset must retain the complete Overworld build range");
        helper.assertTrue(custom.getSeaLevel() == 63, "Custom preset sea level must be 63");
        var settings = access.registryOrThrow(Registries.NOISE_SETTINGS).getOrThrow(SETTINGS);
        helper.assertTrue(settings.isAquifersEnabled() && settings.oreVeinsEnabled(),
                "Custom preset must retain aquifers and large ore veins");
        var normal = (NoiseBasedChunkGenerator) presets.getOrThrow(WorldPresets.NORMAL)
                .overworld().orElseThrow().generator();
        helper.assertTrue(normal.stable(NoiseGeneratorSettings.OVERWORLD),
                "Registering our preset must not replace the vanilla normal generator");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void seededNoiseAndSurfacesProduceRepeatableSealedOceanChunk(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var generator = (NoiseBasedChunkGenerator) level.registryAccess().registryOrThrow(Registries.WORLD_PRESET)
                .getOrThrow(PRESET).overworld().orElseThrow().generator();
        RandomState firstState = randomState(level, generator, SEED);
        ChunkPos position = findOcean(firstState);
        helper.assertTrue(position != null, "Fixed seed must contain an ocean in the sampled continental region");
        if (position == null) return;

        // Both chunks are isolated ProtoChunks; no player-world chunk is requested or mutated.
        ProtoChunk first = generate(level, generator, firstState, position, SEED);
        ProtoChunk second = generate(level, generator, randomState(level, generator, SEED), position, SEED);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int water = 0;
        int solid = 0;
        int air = 0;
        for (int x = position.getMinBlockX(); x <= position.getMaxBlockX(); x++) {
            for (int z = position.getMinBlockZ(); z <= position.getMaxBlockZ(); z++) {
                helper.assertTrue(first.getBlockState(cursor.set(x, -64, z)).is(Blocks.BEDROCK),
                        "Surface rules must seal every bottom column with bedrock");
                helper.assertTrue(first.getBlockState(cursor.set(x, 319, z)).isAir(),
                        "Terrain must leave the top of the Overworld build range open");
                for (int y = -64; y < 320; y++) {
                    cursor.set(x, y, z);
                    var state = first.getBlockState(cursor);
                    helper.assertTrue(state.equals(second.getBlockState(cursor)),
                            "Identical seed and coordinates must reproduce every generated block");
                    if (state.is(Blocks.WATER)) water++;
                    if (state.is(Blocks.STONE) || state.is(Blocks.DEEPSLATE)) solid++;
                    if (state.isAir()) air++;
                }
            }
        }
        helper.assertTrue(water > 0, "Ocean chunk must contain actual generated water");
        helper.assertTrue(solid > 0 && air > 0, "Chunk generation must produce underground solids and open air");
        var alternate = randomState(level, generator, SEED + 1);
        var sample = new DensityFunction.SinglePointContext(position.getMiddleBlockX(), 64, position.getMiddleBlockZ());
        helper.assertTrue(Double.compare(firstState.router().continents().compute(sample),
                        alternate.router().continents().compute(sample)) != 0,
                "Changing the world seed must change the continental noise field");
        helper.succeed();
    }

    private static RandomState randomState(ServerLevel level, NoiseBasedChunkGenerator generator, long seed) {
        return RandomState.create(generator.generatorSettings().value(),
                level.registryAccess().registryOrThrow(Registries.NOISE).asLookup(), seed);
    }

    private static ChunkPos findOcean(RandomState state) {
        for (int x = -8192; x <= 8192; x += 1024) {
            for (int z = -8192; z <= 8192; z += 1024) {
                var context = new DensityFunction.SinglePointContext(x + 8, 63, z + 8);
                if (state.router().continents().compute(context) < -0.25
                        && state.router().depth().compute(context) < -0.15) {
                    return new ChunkPos(x >> 4, z >> 4);
                }
            }
        }
        return null;
    }

    private static ProtoChunk generate(ServerLevel level, NoiseBasedChunkGenerator generator,
                                       RandomState state, ChunkPos position, long seed) {
        var height = LevelHeightAccessor.create(generator.getMinY(), generator.getGenDepth());
        var biomes = level.registryAccess().registryOrThrow(Registries.BIOME);
        var chunk = new ProtoChunk(position, UpgradeData.EMPTY, height, biomes, null);
        // Vanilla StructureManager queries real chunk references. Supply an empty structure
        // provider so this noise/surface test cannot trigger actual world generation.
        var structures = new StructureManager(level, new WorldOptions(seed, false, false), null) {
            @Override
            public List<StructureStart> startsForStructure(ChunkPos pos, Predicate<Structure> predicate) {
                return List.of();
            }
        };
        generator.createBiomes(state, Blender.empty(), structures, chunk).join();
        // The normal ChunkStep scheduler commits each completed stage. We invoke
        // the generator directly, so reproduce those status transitions explicitly.
        chunk.setPersistedStatus(ChunkStatus.BIOMES);
        generator.fillFromNoise(Blender.empty(), state, structures, chunk).join();
        chunk.setPersistedStatus(ChunkStatus.NOISE);
        generator.buildSurface(chunk, new WorldGenerationContext(generator, height), state,
                structures, new BiomeManager(chunk, BiomeManager.obfuscateSeed(seed)),
                biomes, Blender.empty());
        chunk.setPersistedStatus(ChunkStatus.SURFACE);
        return chunk;
    }
}
