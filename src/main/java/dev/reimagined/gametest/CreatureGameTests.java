package dev.reimagined.gametest;

import dev.reimagined.creature.CreatureContent;
import dev.reimagined.creature.Deer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biomes;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Actual server entity creation and persistent breeding data, beyond pure domain tests. */
@GameTestHolder("reimagined")
@PrefixGameTestTemplate(false)
public final class CreatureGameTests {
    private CreatureGameTests() {}

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void deerBreedingCreatesLiveFawn(GameTestHelper helper) {
        Deer first = helper.spawn(CreatureContent.DEER.get(), new BlockPos(3, 2, 3));
        Deer second = helper.spawn(CreatureContent.DEER.get(), new BlockPos(4, 2, 3));
        first.setNoAi(true);
        second.setNoAi(true);
        first.setAge(0);
        second.setAge(0);
        helper.assertTrue(first.getMaxHealth() == 16, "Registered deer attributes were not installed");
        helper.assertTrue(first.isFood(new ItemStack(Items.SWEET_BERRIES)), "Deer must accept sweet berries");
        helper.assertTrue(!first.isFood(new ItemStack(Items.WHEAT)), "Deer must retain its berry diet");
        first.setInLove(null);
        second.setInLove(null);
        first.spawnChildFromBreeding(helper.getLevel(), second);
        var fawns = helper.getLevel().getEntitiesOfClass(Deer.class,
                first.getBoundingBox().inflate(2), Deer::isBaby);
        helper.assertTrue(fawns.size() == 1, "Breeding must create exactly one live deer fawn");
        helper.assertTrue(first.getAge() == 6000 && second.getAge() == 6000,
                "Successful breeding must set both parents' breeding cooldown");
        helper.assertTrue(!first.isInLove() && !second.isInLove(), "Successful breeding must clear parents' love state");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void biomeModifierAddsNaturalDeerHerds(GameTestHelper helper) {
        var forest = helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME)
                .getHolderOrThrow(Biomes.FOREST).value();
        boolean hasHerd = forest.getMobSettings().getMobs(MobCategory.CREATURE).unwrap().stream()
                .anyMatch(spawn -> spawn.type == CreatureContent.DEER.get()
                        && spawn.minCount == 2 && spawn.maxCount == 4);
        helper.assertTrue(hasHerd, "Forest biome modifier must install natural deer herd spawning");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void deerAgeHealthAndNameSurviveSaveLoad(GameTestHelper helper) {
        Deer original = helper.spawn(CreatureContent.DEER.get(), new BlockPos(3, 2, 3));
        original.setNoAi(true);
        original.setAge(-6000);
        original.setHealth(7);
        original.setCustomName(Component.literal("Birch Fawn"));
        original.setPersistenceRequired();
        CompoundTag tag = original.saveWithoutId(new CompoundTag());
        Deer restored = CreatureContent.DEER.get().create(helper.getLevel());
        helper.assertTrue(restored != null, "Registered deer type must create an entity");
        restored.load(tag);
        helper.assertTrue(restored.getAge() == -6000 && restored.isBaby(), "Fawn age must survive save/load");
        helper.assertTrue(restored.getHealth() == 7, "Deer health must survive save/load");
        helper.assertTrue(Component.literal("Birch Fawn").equals(restored.getCustomName()), "Name must survive save/load");
        helper.assertTrue(restored.isPersistenceRequired(), "Persistence flag must survive save/load");
        helper.succeed();
    }
}
