package dev.reimagined.gametest;

import dev.reimagined.research.Discoveries;
import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("reimagined")
@PrefixGameTestTemplate(false)
public final class DiscoveryBlockGameTest {
    private DiscoveryBlockGameTest() {}
    @GameTest(template = "empty")
    public static void blockDiscoveryTracks(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        Discoveries d = new Discoveries(List.of(), List.of(), List.of());
        d = d.withBlock(ResourceLocation.withDefaultNamespace("amethyst_cluster"));
        d.save(player);
        Discoveries after = Discoveries.read(player);
        if (!after.blocks().contains(ResourceLocation.withDefaultNamespace("amethyst_cluster"))) {
            helper.fail("block not tracked");
            return;
        }
        helper.succeed();
    }
}
