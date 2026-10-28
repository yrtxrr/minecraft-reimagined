package dev.reimagined.gametest;

import dev.reimagined.creature.CreatureContent;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("reimagined")
@PrefixGameTestTemplate(false)
public final class FaunaBehaviorGameTests {
    private FaunaBehaviorGameTests() {}
    @GameTest(template = "empty", timeoutTicks = 60)
    public static void marmotFleesFromPlayer(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var marmot = helper.spawn(CreatureContent.MARMOT.get(), helper.absolutePos(new BlockPos(3, 3, 3)));
        player.setPos(marmot.getX(), marmot.getY(), marmot.getZ() + 5);
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(marmot.getX() < 3 || marmot.getZ() < 3, "Marmot should flee from nearby player");
            helper.succeed();
        });
    }
    @GameTest(template = "empty", timeoutTicks = 60)
    public static void otterSwimsInWater(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var otter = helper.spawn(CreatureContent.OTTER.get(), helper.absolutePos(new BlockPos(3, 1, 3)));
        helper.setBlock(new BlockPos(3, 0, 3), Blocks.WATER.defaultBlockState());
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(otter.isInWater(), "Otter should be in water");
            helper.succeed();
        });
    }
}
