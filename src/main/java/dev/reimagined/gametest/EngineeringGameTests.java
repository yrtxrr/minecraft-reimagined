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
