package dev.reimagined.engineering.profile;

import com.google.gson.*;
import dev.reimagined.engineering.*;
import java.nio.file.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.TagParser;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

/** Opt-in ordinary-world save/restart check; uses only the already-loaded profiling fixture. */
final class MechanicalRestartProbe {
    private static final Path EXPECTED = Path.of("reports/restart-expected.json");
    private static List<BlockPos> positions;
    private static int ticks;
    private static final Map<BlockPos, Integer> initialInput = new HashMap<>();
    static void saveExpected(ServerLevel level, List<BlockPos> mills) throws Exception {
        JsonArray array = new JsonArray();
        for (BlockPos p : mills) {
            var object = new JsonObject();
            object.addProperty("position", p.asLong());
            object.addProperty("nbt", ((MachineBlockEntity) level.getBlockEntity(p)).saveWithFullMetadata(level.registryAccess()).toString());
            array.add(object);
        }
        Files.writeString(EXPECTED, array.toString());
    }
    static void start(MinecraftServer server) throws Exception {
        positions = new ArrayList<>(); initialInput.clear(); ticks = 0;
        Files.deleteIfExists(Path.of("reports/restart-result.json"));
        try {
            var level = server.overworld();
            var array = JsonParser.parseString(Files.readString(EXPECTED)).getAsJsonArray();
            if (array.size() != 64) throw new IllegalStateException("Expected 64 saved profiling mills");
            for (var entry : array) {
                var object = entry.getAsJsonObject();
                var p = BlockPos.of(object.get("position").getAsLong());
                if (!level.hasChunkAt(p) || !(level.getBlockEntity(p) instanceof MachineBlockEntity machine))
                    throw new IllegalStateException("Saved fixture is not already loaded");
                var expected = TagParser.parseTag(object.get("nbt").getAsString());
                var actual = machine.saveWithFullMetadata(level.registryAccess());
                for (String key : List.of("Input", "Output", "Progress", "Recipe"))
                    if (!Objects.equals(expected.get(key), actual.get(key))) throw new IllegalStateException("Restart changed " + key + " at " + p);
                if (machine.rotationSpeed() != 0) throw new IllegalStateException("Motion resumed before topology validation");
                initialInput.put(p, machine.getItem(0).getCount());
                positions.add(p);
            }
        } catch (Exception e) { result(false, e.toString()); positions = null; server.halt(false); throw e; }
    }
    static boolean tick(MinecraftServer server) {
        if (positions == null) return false;
        if (++ticks < 220) return true;
        try {
            var level = server.overworld();
            for (BlockPos p : positions) {
                if (!level.hasChunkAt(p) || !(level.getBlockEntity(p) instanceof MachineBlockEntity machine)
                        || machine.rotationSpeed() != 16 || !machine.getItem(1).is(EngineeringContent.FLOUR.get())
                        || machine.getItem(0).getCount() >= initialInput.get(p))
                    throw new IllegalStateException("Saved mill did not resume correctly at " + p);
                for (int dx = 1; dx <= 2; dx++) {
                    var drive = p.west(dx);
                    if (!level.hasChunkAt(drive) || !(level.getBlockEntity(drive) instanceof MachineBlockEntity part)
                            || part.rotationSpeed() != 16 || RotationAnimation.difference(part.rotationAngle(0), machine.rotationAngle(0)) > .02)
                        throw new IllegalStateException("Restarted transmission phase mismatch");
                }
            }
            result(true, null);
        } catch (Exception e) { try { result(false, e.toString()); } catch (Exception ignored) {} }
        positions = null; server.halt(false); return true;
    }
    private static void result(boolean complete, String error) throws Exception {
        JsonObject result = new JsonObject(); result.addProperty("complete", complete);
        result.addProperty("mills", 64); result.addProperty("observedTicks", ticks);
        if (error != null) result.addProperty("error", error);
        Files.writeString(Path.of("reports/restart-result.json"), result.toString());
    }
}
