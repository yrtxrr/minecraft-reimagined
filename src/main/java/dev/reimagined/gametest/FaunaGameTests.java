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
}
