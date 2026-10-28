package dev.reimagined.gametest;

import dev.reimagined.agriculture.OrchardContent;
import dev.reimagined.agriculture.PearLeavesBlock;
import dev.reimagined.browser.BrowserRecipes;
import dev.reimagined.season.CalendarData;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("reimagined")
@PrefixGameTestTemplate(false)
public final class OrchardGameTests {
    private static final BlockPos LEAVES = new BlockPos(3, 3, 3);

    private static BlockState bareLeaves() {
        return OrchardContent.PEAR_LEAVES.get().defaultBlockState()
                .setValue(LeavesBlock.DISTANCE, 1)
                .setValue(LeavesBlock.PERSISTENT, false)
                .setValue(PearLeavesBlock.FRUIT, false);
    }

    private static void restoreCalendar(ServerLevel level, CompoundTag previous) {
        CalendarData restored = CalendarData.load(previous, level.registryAccess());
        restored.setDirty();
        level.getServer().overworld().getDataStorage().set("reimagined_calendar", restored);
    }

    @GameTest(template = "empty")
    public static void orchardRegistersRealTreeContentRecipesAndTags(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        var pear = new ItemStack(OrchardContent.PEAR.get());
        var food = pear.get(DataComponents.FOOD);
        h.assertTrue(food != null && food.nutrition() == 4, "Pears must be a real four-point food item");
        h.assertTrue(OrchardContent.PEAR_LEAVES.get().defaultBlockState().hasProperty(PearLeavesBlock.FRUIT),
                "Pear leaves must expose the real fruit state");
        h.assertTrue(OrchardContent.PEAR_LEAVES.get().defaultBlockState().is(BlockTags.LEAVES)
                        && OrchardContent.PEAR_SAPLING.get().defaultBlockState().is(BlockTags.SAPLINGS)
                        && OrchardContent.PEAR_LOG.get().defaultBlockState().is(BlockTags.LOGS)
                        && OrchardContent.PEAR_WOOD.get().defaultBlockState().is(BlockTags.LOGS)
                        && OrchardContent.PEAR_PLANKS.get().defaultBlockState().is(BlockTags.PLANKS),
                "Pear content must participate in ordinary vanilla block tags");

        var graft = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING,
                CraftingInput.of(2, 1, List.of(pear.copy(), new ItemStack(Items.OAK_SAPLING))), level);
        h.assertTrue(graft.isPresent() && graft.get().value().getResultItem(level.registryAccess())
                        .is(OrchardContent.PEAR_SAPLING_ITEM.get()),
                "Pear + oak sapling must be a real grafting recipe");
        var planks = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING,
                CraftingInput.of(1, 1, List.of(new ItemStack(OrchardContent.PEAR_LOG_ITEM.get()))), level);
        h.assertTrue(planks.isPresent() && planks.get().value().getResultItem(level.registryAccess())
                        .getCount() == 4,
                "One pear log must yield four pear planks through the vanilla crafting manager");

        var displays = BrowserRecipes.recipeDisplays(level.getRecipeManager(), level.registryAccess(),
                ItemStack.EMPTY, List.of());
        h.assertTrue(displays.stream().anyMatch(d -> d.uses(pear)
                        && d.result().is(OrchardContent.PEAR_SAPLING_ITEM.get())),
                "The built-in browser must expose the grafting recipe");
        h.assertTrue(level.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE)
                        .containsKey(OrchardContent.PEAR_TREE),
                "The pear configured feature must be loaded by the real datapack registry");
        h.assertTrue(level.registryAccess().registryOrThrow(Registries.PLACED_FEATURE).containsKey(
                        ResourceKey.create(Registries.PLACED_FEATURE,
                                ResourceLocation.fromNamespaceAndPath("reimagined", "pear_trees"))),
                "The natural pear placement must be a loaded placed feature");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void pearLeavesKeepFruitWorkBoundedToTheInnerCanopy(GameTestHelper h) {
        BlockState bare = bareLeaves();
        BlockState ripe = bare.setValue(PearLeavesBlock.FRUIT, true);
        BlockState outer = bare.setValue(LeavesBlock.DISTANCE, 3);
        h.assertTrue(bare.isRandomlyTicking(), "Inner bare leaves need random ticks to ripen");
        h.assertTrue(!ripe.isRandomlyTicking(), "Ripe leaves must stop random work until harvested");
        h.assertTrue(!outer.isRandomlyTicking(), "Outer canopy must not add thousands of fruit tasks");
        h.assertTrue(!net.minecraft.world.level.block.EntityBlock.class.isInstance(OrchardContent.PEAR_LEAVES.get()),
                "Pear leaves must not own a block entity or ticker");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void fruitRipensInWarmSeasonsAndStopsInWinter(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        CalendarData calendar = CalendarData.get(level);
        CompoundTag previous = calendar.save(new CompoundTag(), level.registryAccess());
        BlockPos pos = h.absolutePos(LEAVES);
        BlockState bare = bareLeaves();
        try {
            calendar.setSeason(level, 1); // SUMMER
            RandomSource random = RandomSource.create(1597);
            boolean ripened = false;
            for (int i = 0; i < 256 && !ripened; i++) {
                h.setBlock(LEAVES, bare);
                bare.randomTick(level, pos, random);
                ripened = level.getBlockState(pos).getValue(PearLeavesBlock.FRUIT);
            }
            h.assertTrue(ripened, "Summer random ticks must ripen inner pear leaves");

            calendar.setSeason(level, 3); // WINTER
            for (int i = 0; i < 256; i++) {
                h.setBlock(LEAVES, bare);
                bare.randomTick(level, pos, RandomSource.create(i));
                h.assertTrue(!level.getBlockState(pos).getValue(PearLeavesBlock.FRUIT),
                        "Winter must not ripen pears");
            }
        } finally {
            restoreCalendar(level, previous);
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void ripeLeavesHarvestOnceThroughRealInteraction(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos pos = h.absolutePos(LEAVES);
        BlockState ripe = bareLeaves().setValue(PearLeavesBlock.FRUIT, true);
        h.setBlock(LEAVES, ripe);
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.NORTH, pos, false);
        var result = ripe.useItemOn(ItemStack.EMPTY, level, player, InteractionHand.MAIN_HAND, hit);
        h.assertTrue(result.consumesAction(), "Right-clicking ripe leaves must be a real block interaction");
        h.assertTrue(!level.getBlockState(pos).getValue(PearLeavesBlock.FRUIT),
                "Harvest must clear the fruit state");
        int pears = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2),
                        entity -> entity.getItem().is(OrchardContent.PEAR.get()))
                .stream().mapToInt(entity -> entity.getItem().getCount()).sum();
        h.assertTrue(pears >= 1 && pears <= 2, "Harvest must drop one or two real pears");
        h.assertTrue(!PearLeavesBlock.harvest(level, pos, level.getBlockState(pos), player),
                "Bare leaves must not be harvested twice");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void leafLootAndShearsFollowRealLootTables(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos pos = h.absolutePos(LEAVES);
        BlockState bare = bareLeaves();
        BlockState ripe = bare.setValue(PearLeavesBlock.FRUIT, true);
        int pears = 0;
        int saplings = 0;
        for (int i = 0; i < 256; i++) {
            for (ItemStack drop : Block.getDrops(ripe, level, pos, null)) {
                if (drop.is(OrchardContent.PEAR.get())) pears += drop.getCount();
                if (drop.is(OrchardContent.PEAR_SAPLING_ITEM.get())) saplings += drop.getCount();
            }
            for (ItemStack drop : Block.getDrops(bare, level, pos, null)) {
                h.assertTrue(!drop.is(OrchardContent.PEAR.get()), "Bare leaves must not drop pears");
            }
        }
        h.assertTrue(pears > 0 && saplings > 0, "Ripe leaf loot must contain pears and occasional saplings");
        var sheared = Block.getDrops(bare, level, pos, null, null, new ItemStack(Items.SHEARS));
        h.assertTrue(sheared.stream().anyMatch(drop -> drop.is(OrchardContent.PEAR_LEAVES_ITEM.get()))
                        && sheared.stream().noneMatch(drop -> drop.is(OrchardContent.PEAR_SAPLING_ITEM.get())),
                "Shears must preserve leaves without adding sapling loot");
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void pearSaplingGrowsIntoARealTreeFeature(GameTestHelper h) {
        BlockPos base = new BlockPos(3, 1, 3);
        h.setBlock(base.below(), Blocks.DIRT);
        BlockState sapling = OrchardContent.PEAR_SAPLING.get().defaultBlockState()
                .setValue(SaplingBlock.STAGE, 1);
        h.setBlock(base, sapling);
        ServerLevel level = h.getLevel();
        BlockPos pos = h.absolutePos(base);
        ((dev.reimagined.agriculture.PearSaplingBlock) sapling.getBlock())
                .advanceTree(level, pos, level.getBlockState(pos), RandomSource.create(4242));
        int logs = 0;
        int leaves = 0;
        for (BlockPos check : BlockPos.betweenClosed(pos.offset(-3, 0, -3), pos.offset(3, 7, 3))) {
            if (level.getBlockState(check).is(OrchardContent.PEAR_LOG.get())) logs++;
            if (level.getBlockState(check).is(OrchardContent.PEAR_LEAVES.get())) leaves++;
        }
        h.assertTrue(logs >= 4 && leaves >= 6,
                "Sapling growth must place the real pear tree feature, not a decorative stub");
        h.succeed();
    }
}
