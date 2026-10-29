package dev.reimagined.beekeeping;

import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Small helper around the per-hive frame attachment. All access is server-side and O(1). */
public final class BeeFrames {
    public static final int MAX_FRAMES = 3;

    private BeeFrames() {}

    public static int get(BlockEntity hive) {
        if (!(hive instanceof BeehiveBlockEntity)) return 0;
        Integer frames = hive.getData(BeekeepingContent.HIVE_FRAMES.get());
        return frames == null ? 0 : Math.clamp(frames, 0, MAX_FRAMES);
    }

    /** Adds one frame if there is room. Returns false when the hive already holds the maximum. */
    public static boolean install(BlockEntity hive) {
        int frames = get(hive);
        if (frames >= MAX_FRAMES) return false;
        hive.setData(BeekeepingContent.HIVE_FRAMES.get(), frames + 1);
        hive.setChanged();
        return true;
    }

    /** Removes and returns every installed frame, used when the hive itself is broken. */
    public static int takeAll(BlockEntity hive) {
        if (hive == null) return 0;
        int frames = get(hive);
        if (frames > 0) hive.removeData(BeekeepingContent.HIVE_FRAMES.get());
        return frames;
    }
}
