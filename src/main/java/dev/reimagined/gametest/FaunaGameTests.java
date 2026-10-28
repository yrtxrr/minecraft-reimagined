package dev.reimagined.gametest;

import dev.reimagined.creature.CreatureContent;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("reimagined")
@PrefixGameTestTemplate(false)
public final class FaunaGameTests {
    private FaunaGameTests() {}
    @GameTest(template = "empty")
    public static void newFaunaRegistered(GameTestHelper helper) {
        if (CreatureContent.OTTER.get() == null || CreatureContent.BADGER.get() == null || CreatureContent.MARMOT.get() == null) {
            helper.fail("missing fauna");
            return;
        }
        if (CreatureContent.OTTER_SPAWN_EGG.get() == null || CreatureContent.BADGER_SPAWN_EGG.get() == null || CreatureContent.MARMOT_SPAWN_EGG.get() == null) {
            helper.fail("missing fauna eggs");
            return;
        }
        helper.succeed();
    }
    @GameTest(template="empty")
    public static void habitatsOffspringAndPersistentVanillaState(GameTestHelper h) {
        var biomes=h.getLevel().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.BIOME);
        var types=java.util.List.of(CreatureContent.OTTER.get(),CreatureContent.BADGER.get(),CreatureContent.MARMOT.get());
        var habitats=java.util.List.of(net.minecraft.world.level.biome.Biomes.RIVER,net.minecraft.world.level.biome.Biomes.FOREST,net.minecraft.world.level.biome.Biomes.MEADOW);
        for(int i=0;i<types.size();i++) {
            var type=types.get(i);var a=type.create(h.getLevel());
            h.assertTrue(biomes.getHolderOrThrow(habitats.get(i)).value().getMobSettings().getMobs(net.minecraft.world.entity.MobCategory.CREATURE).unwrap().stream()
                    .anyMatch(e->e.type==type && e.getWeight().asInt()<=6 && e.maxCount<=3),"Natural spawning uses ordinary creature cap and small groups");
            h.assertTrue(biomes.getHolderOrThrow(net.minecraft.world.level.biome.Biomes.DESERT).value().getMobSettings().getMobs(net.minecraft.world.entity.MobCategory.CREATURE).unwrap().stream()
                    .noneMatch(e->e.type==type),"No inappropriate desert spawn");
            var baby=a.getBreedOffspring(h.getLevel(),a);h.assertTrue(baby!=null && baby.getType()==type,"Breeding retains species");
            baby.setAge(-24000);baby.setHealth(5);baby.setPersistenceRequired();baby.setCustomName(net.minecraft.network.chat.Component.literal("Fauna"));
            var restored=type.create(h.getLevel());restored.load(baby.saveWithoutId(new net.minecraft.nbt.CompoundTag()));
            h.assertTrue(restored.isBaby() && restored.getHealth()==5 && restored.isPersistenceRequired()
                    && baby.getCustomName().equals(restored.getCustomName()),"Vanilla age, health, persistence and name survive world NBT round trip");
        }
        h.succeed();
    }
    @GameTest(template="empty")
    public static void riverOtterSpawnsInSurfaceWaterWithoutGrassRequirement(GameTestHelper h) {
        var p=new net.minecraft.core.BlockPos(6,3,6);var level=h.getLevel();var abs=h.absolutePos(p);
        h.setBlock(p.below(),net.minecraft.world.level.block.Blocks.WATER);h.setBlock(p,net.minecraft.world.level.block.Blocks.WATER);
        h.setBlock(p.above(),net.minecraft.world.level.block.Blocks.AIR);
        h.assertTrue(net.minecraft.world.entity.SpawnPlacements.getPlacementType(CreatureContent.OTTER.get())==net.minecraft.world.entity.SpawnPlacementTypes.IN_WATER
                && net.minecraft.world.entity.SpawnPlacements.checkSpawnRules(CreatureContent.OTTER.get(),level,net.minecraft.world.entity.MobSpawnType.NATURAL,abs,level.random),"Registered natural spawn must accept a river surface over water, not demand grass");
        h.setBlock(p,net.minecraft.world.level.block.Blocks.STONE);
        h.assertTrue(!net.minecraft.world.entity.SpawnPlacements.checkSpawnRules(CreatureContent.OTTER.get(),level,net.minecraft.world.entity.MobSpawnType.NATURAL,abs,level.random),"Dry rock cannot spawn a water otter");h.succeed();
    }
}
