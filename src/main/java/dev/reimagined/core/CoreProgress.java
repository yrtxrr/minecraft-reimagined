package dev.reimagined.core;

import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;

/** A small, bounded integration ledger, separate from legacy journal/skills/magic saves. */
public final class CoreProgress {
    public static final int LIMIT = 256;
    public enum Era { SURVIVAL, METALLURGY, MECHANICAL, INDUSTRY, ELECTRICITY_CHEMISTRY, COMPUTING, ADVANCED }
    private final Set<ResourceLocation> knowledge = new TreeSet<>();
    private final EnumSet<Era> eras = EnumSet.noneOf(Era.class);
    public Set<ResourceLocation> knowledge() { return Collections.unmodifiableSet(knowledge); }
    public Set<Era> eras() { return Collections.unmodifiableSet(eras); }
    public boolean learn(ResourceLocation id) { return knowledge.size() < LIMIT && knowledge.add(Objects.requireNonNull(id)); }
    public boolean unlock(Era era) { return eras.add(Objects.requireNonNull(era)); }
    public CompoundTag save() {
        var tag = new CompoundTag(); tag.putInt("Version", 1);
        var ids = new ListTag(); knowledge.forEach(id -> ids.add(StringTag.valueOf(id.toString())));
        tag.put("Knowledge", ids);
        var stages = new ListTag(); eras.forEach(era -> stages.add(StringTag.valueOf(era.name())));
        tag.put("Eras", stages); return tag;
    }
    public static CoreProgress load(CompoundTag tag) {
        if (!tag.isEmpty() && tag.getInt("Version") != 1) throw new IllegalStateException("Unsupported Core progress version; refusing to overwrite");
        var result = new CoreProgress(); var ids = tag.getList("Knowledge", Tag.TAG_STRING);
        for (int n=0; n<Math.min(LIMIT, ids.size()); n++) {
            var id = ResourceLocation.tryParse(ids.getString(n)); if (id != null) result.learn(id);
        }
        var stages = tag.getList("Eras", Tag.TAG_STRING);
        for (int n=0; n<Math.min(Era.values().length, stages.size()); n++) {
            try { result.unlock(Era.valueOf(stages.getString(n))); } catch (IllegalArgumentException ignored) { }
        }
        return result;
    }
}
