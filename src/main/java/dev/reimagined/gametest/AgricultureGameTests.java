package dev.reimagined.gametest;

import dev.reimagined.agriculture.AgricultureContent;
import dev.reimagined.engineering.*;
import dev.reimagined.season.CalendarData;
import dev.reimagined.season.SeasonCalendar;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.block.CropGrowEvent;
import net.neoforged.neoforge.gametest.*;
import java.util.*;

@GameTestHolder("reimagined")
@PrefixGameTestTemplate(false)
public final class AgricultureGameTests {
    private static final BlockPos CROP = new BlockPos(3, 3, 3);
    private static void soil(GameTestHelper h) {
        h.setBlock(CROP.below(), Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, 7));
        h.setBlock(CROP, AgricultureContent.BARLEY_CROP.get());
    }
    @GameTest(template = "empty")
    public static void seedsPlantOnFarmlandAndBonemealAdvancesEightStages(GameTestHelper h) {
        h.setBlock(CROP.below(), Blocks.FARMLAND);
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(AgricultureContent.BARLEY_SEEDS.get(), 2));
        BlockPos soil = h.absolutePos(CROP.below());
        var result = player.getMainHandItem().useOn(new UseOnContext(player, net.minecraft.world.InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(soil).add(0, .5, 0), Direction.UP, soil, false)));
        var level = h.getLevel(); var pos = h.absolutePos(CROP); var crop = AgricultureContent.BARLEY_CROP.get();
        h.assertTrue(result.consumesAction() && level.getBlockState(pos).is(crop)
            && player.getMainHandItem().getCount() == 1, "Actual seed use must plant and consume one seed");
        h.assertTrue(crop.getMaxAge() == 7 && crop.getStateDefinition().getPossibleStates().size() == 8,
                "Barley must have eight ordinary vanilla growth stages");
        ItemStack meal = new ItemStack(Items.BONE_MEAL, 8);
        for (int i = 0; i < 4; i++) BoneMealItem.growCrop(meal, level, pos);
        h.assertTrue(crop.isMaxAge(level.getBlockState(pos)) && !level.getBlockState(pos).isRandomlyTicking(),
                "Bonemeal must mature barley and mature plants must stop random growth work");
        h.setBlock(CROP.below(), Blocks.STONE);
        h.assertTrue(!crop.defaultBlockState().canSurvive(level, pos), "Barley cannot survive without valid farmland");
        h.succeed();
    }
    @GameTest(template = "empty")
    public static void cropLootReturnsSeedsAndOnlyMaturePlantsYieldGrain(GameTestHelper h) {
        soil(h); var level = h.getLevel(); var pos = h.absolutePos(CROP); var crop = AgricultureContent.BARLEY_CROP.get();
        for (int age = 0; age <= 7; age++) {
            var drops = Block.getDrops(crop.getStateForAge(age), level, pos, null);
            int seeds = drops.stream().filter(s -> s.is(AgricultureContent.BARLEY_SEEDS.get())).mapToInt(ItemStack::getCount).sum();
            int grain = drops.stream().filter(s -> s.is(AgricultureContent.BARLEY.get())).mapToInt(ItemStack::getCount).sum();
            h.assertTrue(seeds >= 1 && seeds <= 4 && grain == (age == 7 ? 1 : 0), "Age-aware crop loot must preserve replanting and reject premature grain");
        }
        h.succeed();
    }
    @GameTest(template = "empty")
    public static void grassAcquisitionUsesAdditiveLootAndRespectsShears(GameTestHelper h) {
        var level = h.getLevel(); var pos = h.absolutePos(CROP);
        int seeds = 0;
        for (int i = 0; i < 256; i++) {
            seeds += Block.getDrops(Blocks.SHORT_GRASS.defaultBlockState(), level, pos, null).stream()
                    .filter(s -> s.is(AgricultureContent.BARLEY_SEEDS.get())).mapToInt(ItemStack::getCount).sum();
            var sheared = Block.getDrops(Blocks.SHORT_GRASS.defaultBlockState(), level, pos, null, null, new ItemStack(Items.SHEARS));
            h.assertTrue(sheared.stream().noneMatch(s -> s.is(AgricultureContent.BARLEY_SEEDS.get())), "Shears must not grant extra barley seeds");
        }
        h.assertTrue(seeds > 0 && seeds < 128, "Real global loot modifier must supply rare survival seeds");
        h.succeed();
    }
    @GameTest(template = "empty")
    public static void seasonalGrowthEventsShelterAndCompostingUseExistingSystems(GameTestHelper h) {
        soil(h); var level = h.getLevel(); var pos = h.absolutePos(CROP); var crop = AgricultureContent.BARLEY_CROP.get();
        var calendar = CalendarData.get(level);
        var previous = calendar.save(new net.minecraft.nbt.CompoundTag(), level.registryAccess());
        try {
            calendar.setSeason(level, 3);
            int[] counts = {0, 0};
            java.util.function.Consumer<CropGrowEvent.Pre> observer = event -> {
                if (event.getLevel() == level && event.getPos().equals(pos)) {
                    counts[0]++;
                    if (event.getResult() == CropGrowEvent.Pre.Result.DO_NOT_GROW) counts[1]++;
                }
            };
            NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.LOWEST, CropGrowEvent.Pre.class, observer);
            try {
                var random = net.minecraft.util.RandomSource.create(1597);
                for (int i = 0; i < 128; i++) {
                    level.setBlock(pos, crop.defaultBlockState(), Block.UPDATE_CLIENTS);
                    crop.defaultBlockState().randomTick(level, pos, random);
                }
            } finally { NeoForge.EVENT_BUS.unregister(observer); }
            h.assertTrue(counts[0] == 128, "Vanilla random ticks must fire the common growth hook for barley");
            // Keep this fixture in a temperate biome so the seasonal assertion actually executes.
            float temperature = level.getBiome(pos).value().getBaseTemperature();
            h.assertTrue(temperature >= .15F && temperature <= 1.5F, "Fixture requires a temperate biome");
            h.assertTrue(counts[1] > 0 && counts[1] < counts[0], "Winter must reduce ordinary barley growth through the shared event");
            h.setBlock(CROP.above(2), Blocks.GLASS);
            for (int i = 0; i < 16; i++) {
                var event = new CropGrowEvent.Pre(level, pos, crop.defaultBlockState()); NeoForge.EVENT_BUS.post(event);
                h.assertTrue(event.getResult() != CropGrowEvent.Pre.Result.DO_NOT_GROW, "Existing glass shelter must protect barley growth");
            }
        } finally {
            var restored = CalendarData.load(previous, level.registryAccess()); restored.setDirty();
            level.getServer().overworld().getDataStorage().set("reimagined_calendar", restored);
        }
        h.assertTrue(ComposterBlock.COMPOSTABLES.getFloat(AgricultureContent.BARLEY_SEEDS.get()) == .3F
            && ComposterBlock.COMPOSTABLES.getFloat(AgricultureContent.BARLEY.get()) == .65F, "Seeds and grain must compost");
        h.succeed();
    }
    @GameTest(template = "empty", timeoutTicks = 260)
    public static void barleyMillsThroughTheExistingPoweredAutomation(GameTestHelper h) {
        h.setBlock(new BlockPos(3,3,3), EngineeringContent.WATER_WHEEL.get().defaultBlockState().setValue(MechanicalBlock.AXIS, Direction.Axis.X));
        h.setBlock(new BlockPos(4,3,3), EngineeringContent.SHAFT.get().defaultBlockState().setValue(MechanicalBlock.AXIS, Direction.Axis.X));
        h.setBlock(new BlockPos(5,3,3), EngineeringContent.MILLSTONE.get());
        for (int z = 4; z <= 5; z++) {
            h.setBlock(new BlockPos(3,2,z), Blocks.STONE); h.setBlock(new BlockPos(2,3,z), Blocks.STONE); h.setBlock(new BlockPos(4,3,z), Blocks.STONE);
        }
        h.setBlock(new BlockPos(3,3,6), Blocks.STONE); h.setBlock(new BlockPos(3,3,5), Blocks.WATER);
        h.setBlock(new BlockPos(3,3,4), Blocks.WATER.defaultBlockState().setValue(LiquidBlock.LEVEL,1));
        var machine = (MachineBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(new BlockPos(5,3,3)));
        var input = net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK;
        var handler = h.getLevel().getCapability(input, machine.getBlockPos(), Direction.UP);
        h.assertTrue(handler != null && handler.insertItem(0,new ItemStack(AgricultureContent.BARLEY.get(),1),false).isEmpty(), "Existing top automation must accept barley recipe");
        h.succeedWhen(() -> h.assertTrue(machine.getItem(1).is(EngineeringContent.FLOUR.get()) && machine.getItem(0).isEmpty(),
                "Actual water-powered milling must consume barley into shared flour"));
    }
    @GameTest(template = "empty")
    public static void barleyCraftingAndBrowserUseRealRegisteredRecipes(GameTestHelper h) {
        var level = h.getLevel(); var grain = new ItemStack(AgricultureContent.BARLEY.get());
        var bread = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, CraftingInput.of(3,1,List.of(grain,grain.copy(),grain.copy())),level);
        h.assertTrue(bread.isPresent() && bread.get().value().assemble(CraftingInput.of(3,1,List.of(grain,grain.copy(),grain.copy())),level.registryAccess()).is(Items.BREAD), "Three barley must craft vanilla bread");
        var seeds = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING,CraftingInput.of(1,1,List.of(grain)),level);
        h.assertTrue(seeds.isPresent() && seeds.get().value().getResultItem(level.registryAccess()).getCount() == 2, "Grain must provide two replantable seeds");
        var displays = dev.reimagined.browser.BrowserRecipes.recipeDisplays(level.getRecipeManager(),level.registryAccess(),ItemStack.EMPTY,List.of());
        h.assertTrue(displays.stream().anyMatch(d -> d.uses(grain) && d.result().is(EngineeringContent.FLOUR.get()))
            && displays.stream().anyMatch(d -> d.uses(grain) && d.result().is(Items.BREAD)), "Existing browser must expose barley uses without a special crop adapter");
        h.succeed();
    }
    @GameTest(template = "empty")
    public static void newPlantsCompostCorrectly(GameTestHelper h) {
        h.assertTrue(ComposterBlock.COMPOSTABLES.getFloat(AgricultureContent.CLOVER.get()) == 0.3F,
                "Clover must compost at 0.3");
        h.assertTrue(ComposterBlock.COMPOSTABLES.getFloat(AgricultureContent.THATCH.get()) == 0.85F,
                "Thatch must compost at 0.85");
        h.assertTrue(ComposterBlock.COMPOSTABLES.getFloat(AgricultureContent.BARLEY_SEEDS.get()) == 0.3F,
                "Barley seeds must compost at 0.3");
        h.assertTrue(ComposterBlock.COMPOSTABLES.getFloat(AgricultureContent.BARLEY.get()) == 0.65F,
                "Barley grain must compost at 0.65");
        h.succeed();
    }
    @GameTest(template = "empty")
    public static void barleyGrowsOnMoistFarmland(GameTestHelper h) {
        h.setBlock(CROP.below(), Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, 7));
        h.setBlock(CROP, AgricultureContent.BARLEY_CROP.get());
        var level = h.getLevel();
        var pos = h.absolutePos(CROP);
        var state = level.getBlockState(pos);
        h.assertTrue(state.is(AgricultureContent.BARLEY_CROP.get()), "Barley must place on moist farmland");
        h.succeed();
    }
}
