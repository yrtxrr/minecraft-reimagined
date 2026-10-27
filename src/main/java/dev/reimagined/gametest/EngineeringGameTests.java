package dev.reimagined.gametest;

import dev.reimagined.engineering.EngineeringContent;
import dev.reimagined.engineering.MachineBlockEntity;
import dev.reimagined.engineering.MechanicalBlock;
import dev.reimagined.engineering.MechanicalNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Integration tests use real registries, recipes, fluids, block entities and server ticks. */
@GameTestHolder("reimagined")
@PrefixGameTestTemplate(false)
public final class EngineeringGameTests {
    private static final BlockPos WHEEL = new BlockPos(3, 3, 3);
    private static final BlockPos SHAFT = new BlockPos(4, 3, 3);
    private static final BlockPos MILL = new BlockPos(5, 3, 3);

    private EngineeringGameTests() {}

    @GameTest(template = "empty")
    public static void millingRecipesProduceRegisteredItems(GameTestHelper helper) {
        var level = helper.getLevel();
        var wheat = level.getRecipeManager().getRecipeFor(EngineeringContent.MILLING.get(),
                new SingleRecipeInput(new ItemStack(Items.WHEAT)), level);
        helper.assertTrue(wheat.isPresent(), "Wheat milling recipe was not loaded");
        var recipe = wheat.orElseThrow().value();
        helper.assertTrue(recipe.isSpecial(), "Machine recipes must stay out of unsupported vanilla recipe-book categories");
        helper.assertTrue(recipe.getResultItem(level.registryAccess()).is(EngineeringContent.FLOUR.get()),
                "Wheat milling result must be registered flour");
        helper.assertTrue(recipe.getToastSymbol().is(EngineeringContent.MILLSTONE.get().asItem()),
                "Milling recipe must identify its workstation");
        helper.assertTrue(level.getRecipeManager().getRecipeFor(EngineeringContent.MILLING.get(),
                new SingleRecipeInput(new ItemStack(Items.DIAMOND)), level).isEmpty(),
                "Unsupported ingredients must not produce a milling recipe");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void shaftsTransmitOnlyAlongTheirAxis(GameTestHelper helper) {
        setupPoweredMill(helper);
        var level = helper.getLevel();
        var rotation = MechanicalNetwork.inspect(level, helper.absolutePos(MILL));
        helper.assertTrue(rotation.running() && rotation.wheels() == 1 && rotation.machines() == 1,
                "Flowing water must power the connected millstone");
        helper.assertTrue(rotation.speed() == 16 && rotation.capacity() == 16 && rotation.load() == 8,
                "Single wheel and mill must produce 16 RPM at 8/16 load");
        helper.setBlock(SHAFT, EngineeringContent.SHAFT.get().defaultBlockState().setValue(MechanicalBlock.AXIS, Direction.Axis.Y));
        helper.assertTrue(!MechanicalNetwork.inspect(level, helper.absolutePos(MILL)).running(),
                "Rotating the shaft perpendicular to the connection must stop transmission");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void overloadedNetworksStop(GameTestHelper helper) {
        setupPoweredMill(helper);
        helper.setBlock(MILL.south(), EngineeringContent.MILLSTONE.get().defaultBlockState());
        helper.setBlock(MILL.south(2), EngineeringContent.MILLSTONE.get().defaultBlockState());
        var rotation = MechanicalNetwork.inspect(helper.getLevel(), helper.absolutePos(MILL));
        helper.assertTrue(rotation.wheels() == 1 && rotation.machines() == 3,
                "All connected consumers must count against wheel capacity");
        helper.assertTrue(rotation.load() == 24 && rotation.capacity() == 16 && !rotation.running(),
                "Overloaded power networks must stop instead of producing unlimited power");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void redstoneDisablesWaterWheels(GameTestHelper helper) {
        setupPoweredMill(helper);
        helper.assertTrue(MechanicalNetwork.inspect(helper.getLevel(), helper.absolutePos(MILL)).running(),
                "Test fixture must be powered before redstone is added");
        helper.setBlock(WHEEL.above(), Blocks.REDSTONE_BLOCK);
        helper.assertTrue(!MechanicalNetwork.inspect(helper.getLevel(), helper.absolutePos(MILL)).running(),
                "Redstone must disable wheel power");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 260)
    public static void poweredMillProcessesAndPersistsInventory(GameTestHelper helper) {
        setupPoweredMill(helper);
        var level = helper.getLevel();
        var pos = helper.absolutePos(MILL);
        var machine = (MachineBlockEntity) level.getBlockEntity(pos);
        helper.assertTrue(machine != null, "Registered millstone must create its block entity");
        // Seed the same persistent input field used by manual insertion; processing itself runs through server ticks.
        CompoundTag initial = new CompoundTag();
        initial.put("Input", new ItemStack(Items.WHEAT, 3).save(level.registryAccess()));
        machine.loadWithComponents(initial, level.registryAccess());
        helper.succeedWhen(() -> {
            CompoundTag saved = machine.saveWithFullMetadata(level.registryAccess());
            ItemStack output = ItemStack.parseOptional(level.registryAccess(), saved.getCompound("Output"));
            helper.assertTrue(output.is(EngineeringContent.FLOUR.get()) && output.getCount() == 1,
                    "A running millstone must turn one wheat into one flour after its recipe duration");
            ItemStack input = ItemStack.parseOptional(level.registryAccess(), saved.getCompound("Input"));
            helper.assertTrue(input.is(Items.WHEAT) && input.getCount() == 2,
                    "Milling must consume exactly one input");
            helper.assertTrue(level.getBlockState(pos).getValue(MechanicalBlock.ACTIVE),
                    "Running state must be synchronized through the ACTIVE block state");
            BlockEntity restored = BlockEntity.loadStatic(pos, level.getBlockState(pos), saved, level.registryAccess());
            helper.assertTrue(restored instanceof MachineBlockEntity, "NBT must restore the registered machine type");
            CompoundTag roundTrip = restored.saveWithFullMetadata(level.registryAccess());
            helper.assertTrue(saved.getCompound("Input").equals(roundTrip.getCompound("Input"))
                            && saved.getCompound("Output").equals(roundTrip.getCompound("Output"))
                            && saved.getInt("Progress") == roundTrip.getInt("Progress")
                            && saved.getString("Recipe").equals(roundTrip.getString("Recipe")),
                    "Input, output, progress and recipe must survive a complete NBT round trip");
        });
    }

    @GameTest(template = "empty", timeoutTicks = 440)
    public static void hoppersFeedAndCollectOnlyValidMillingItems(GameTestHelper helper) {
        setupPoweredMill(helper);
        var level = helper.getLevel();
        helper.setBlock(MILL.above(), Blocks.HOPPER.defaultBlockState()
                .setValue(net.minecraft.world.level.block.HopperBlock.FACING, Direction.DOWN));
        helper.setBlock(MILL.below(), Blocks.HOPPER.defaultBlockState()
                .setValue(net.minecraft.world.level.block.HopperBlock.FACING, Direction.DOWN));
        var feeder = (net.minecraft.world.level.block.entity.HopperBlockEntity) level.getBlockEntity(helper.absolutePos(MILL.above()));
        var collector = (net.minecraft.world.level.block.entity.HopperBlockEntity) level.getBlockEntity(helper.absolutePos(MILL.below()));
        feeder.setItem(0, new ItemStack(Items.DIAMOND));
        feeder.setItem(1, new ItemStack(Items.WHEAT, 2));
        helper.succeedWhen(() -> {
            helper.assertTrue(collector.countItem(EngineeringContent.FLOUR.get()) == 2,
                    "A lower hopper must collect exactly the two processed flour outputs");
            helper.assertTrue(feeder.countItem(Items.DIAMOND) == 1 && feeder.countItem(Items.WHEAT) == 0,
                    "The upper hopper must retain unsupported diamonds and transfer all wheat");
            var machine = (MachineBlockEntity) level.getBlockEntity(helper.absolutePos(MILL));
            helper.assertTrue(machine.isEmpty(), "Input and output must be empty after automatic collection");
            helper.assertTrue(collector.countItem(Items.DIAMOND) == 0 && collector.countItem(Items.WHEAT) == 0,
                    "The lower hopper must never extract unprocessed input or unsupported items");
        });
    }

    @GameTest(template = "empty")
    public static void sidedItemHandlersRespectFiltersSimulationAndCapacity(GameTestHelper helper) {
        setupPoweredMill(helper);
        var level = helper.getLevel();
        var pos = helper.absolutePos(MILL);
        var top = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, pos, Direction.UP);
        var bottom = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, pos, Direction.DOWN);
        helper.assertTrue(top != null && bottom != null && top.getSlots() == 1 && bottom.getSlots() == 1,
                "Millstone must expose one input slot above and one output slot below");
        var machine = (MachineBlockEntity) level.getBlockEntity(pos);
        ItemStack offered = new ItemStack(Items.WHEAT, 70);
        ItemStack simulated = top.insertItem(0, offered, true);
        helper.assertTrue(simulated.getCount() == 6 && machine.isEmpty() && offered.getCount() == 70,
                "Simulation must report capacity64 without changing machine or caller stack");
        ItemStack remainder = top.insertItem(0, offered, false);
        helper.assertTrue(remainder.getCount() == 6 && machine.getItem(0).getCount() == 64 && offered.getCount() == 70,
                "Real insertion must cap at64 and return all six excess items without mutating caller stack");
        helper.assertTrue(top.extractItem(0, 64, false).isEmpty() && machine.getItem(0).getCount() == 64,
                "Input-side handlers must not extract unprocessed input");
        helper.assertTrue(top.insertItem(0, new ItemStack(Items.DIAMOND), false).is(Items.DIAMOND),
                "Unsupported ingredients must be rejected");
        helper.assertTrue(bottom.insertItem(0, new ItemStack(EngineeringContent.FLOUR.get()), false).getCount() == 1,
                "Output-side handlers must reject all insertion");
        machine.setItem(1, new ItemStack(EngineeringContent.FLOUR.get(), 3));
        helper.assertTrue(bottom.extractItem(0, 2, true).getCount() == 2 && machine.getItem(1).getCount() == 3,
                "Extraction simulation must leave output unchanged");
        helper.assertTrue(bottom.extractItem(0, 2, false).getCount() == 2 && machine.getItem(1).getCount() == 1,
                "Extraction must remove exactly the requested output count");
        helper.assertTrue(level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,
                helper.absolutePos(WHEEL), Direction.UP) == null,
                "Water wheels must not expose an item handler");
        var wheel = (MachineBlockEntity) level.getBlockEntity(helper.absolutePos(WHEEL));
        helper.assertTrue(wheel.getContainerSize() == 0 && wheel.getSlotsForFace(Direction.DOWN).length == 0,
                "Water wheels must not expose vanilla container slots either");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 230)
    public static void restoredPartialMillingResumesWithoutLosingProgress(GameTestHelper helper) {
        setupPoweredMill(helper);
        var level = helper.getLevel();
        var pos = helper.absolutePos(MILL);
        var original = (MachineBlockEntity) level.getBlockEntity(pos);
        original.setItem(0, new ItemStack(Items.WHEAT, 2));
        original.setItem(1, new ItemStack(EngineeringContent.FLOUR.get(), 3));
        boolean[] restored = {false};
        helper.runAtTickTime(85, () -> {
            CompoundTag saved = original.saveWithFullMetadata(level.registryAccess());
            helper.assertTrue(saved.getInt("Progress") >= 3 && saved.getInt("Progress") < 8,
                    "Fixture must save a genuinely unfinished recipe");
            var replacement = BlockEntity.loadStatic(pos, level.getBlockState(pos), saved, level.registryAccess());
            helper.assertTrue(replacement instanceof MachineBlockEntity, "Persistent machine must be recreatable");
            level.setBlockEntity(replacement);
            CompoundTag checked = replacement.saveWithFullMetadata(level.registryAccess());
            helper.assertTrue(saved.getInt("Progress") == checked.getInt("Progress")
                            && saved.getCompound("Input").equals(checked.getCompound("Input"))
                            && saved.getCompound("Output").equals(checked.getCompound("Output")),
                    "Replacement must preserve partial progress and both inventory slots");
            restored[0] = true;
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(restored[0], "Machine must be saved and restored during processing");
            var current = (MachineBlockEntity) level.getBlockEntity(pos);
            helper.assertTrue(current.getItem(1).is(EngineeringContent.FLOUR.get()) && current.getItem(1).getCount() == 4,
                    "Restored processing must finish before a restarted-from-zero recipe could finish");
            helper.assertTrue(current.getItem(0).is(Items.WHEAT) && current.getItem(0).getCount() == 1,
                    "Saving and loading must neither lose nor duplicate input");
        });
    }

    private static void setupPoweredMill(GameTestHelper helper) {
        helper.setBlock(WHEEL, EngineeringContent.WATER_WHEEL.get().defaultBlockState().setValue(MechanicalBlock.AXIS, Direction.Axis.X));
        helper.setBlock(SHAFT, EngineeringContent.SHAFT.get().defaultBlockState().setValue(MechanicalBlock.AXIS, Direction.Axis.X));
        helper.setBlock(MILL, EngineeringContent.MILLSTONE.get().defaultBlockState());
        // A short, enclosed horizontal channel keeps the adjacent water flowing across a supported floor.
        for (int z = 4; z <= 5; z++) {
            helper.setBlock(new BlockPos(3, 2, z), Blocks.STONE);
            helper.setBlock(new BlockPos(2, 3, z), Blocks.STONE);
            helper.setBlock(new BlockPos(4, 3, z), Blocks.STONE);
        }
        helper.setBlock(new BlockPos(3, 3, 6), Blocks.STONE);
        helper.setBlock(new BlockPos(3, 3, 5), Blocks.WATER);
        helper.setBlock(new BlockPos(3, 3, 4), Blocks.WATER.defaultBlockState().setValue(LiquidBlock.LEVEL, 1));
    }
}
