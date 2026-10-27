package dev.reimagined.gametest;

import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.GameTestServer;
import net.minecraft.gametest.framework.GlobalTestReporter;
import net.minecraft.gametest.framework.JUnitLikeTestReporter;
import net.minecraft.gametest.framework.LogTestReporter;
import net.minecraft.gametest.framework.TestReporter;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;

/** Development-only reporter, excluded from the player JAR alongside test classes. */
@EventBusSubscriber(modid = "reimagined")
public final class TestReportHooks {
    private TestReportHooks() {}
    @SubscribeEvent public static void starting(ServerAboutToStartEvent event) throws Exception {
        if (!(event.getServer() instanceof GameTestServer)) return;
        Path report = Path.of("reports", "gametest.xml");
        Files.createDirectories(report.getParent());
        Files.deleteIfExists(report); // Never present results from an earlier invocation as current.
        var xml = new JUnitLikeTestReporter(report.toFile());
        var log = new LogTestReporter();
        GlobalTestReporter.replaceWith(new TestReporter() {
            @Override public void onTestFailed(GameTestInfo info) {
                log.onTestFailed(info);
                info.getError().printStackTrace();
                xml.onTestFailed(info);
            }
            @Override public void onTestSuccess(GameTestInfo info) { log.onTestSuccess(info); xml.onTestSuccess(info); }
            @Override public void finish() { log.finish(); xml.finish(); }
        });
    }
}
