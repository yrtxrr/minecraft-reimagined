package dev.reimagined.civilization;

import dev.reimagined.season.CalendarData;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Overworld-owned registry of abstract settlements. All state is in memory and saved with the
 * world; reading it never touches chunks, entities or block state.
 */
public final class SettlementData extends SavedData {
    public static final int MAX_SETTLEMENTS = 64;
    /** Minimum horizontal spacing between two settlements, in blocks. */
    public static final int MIN_SPACING = 192;
    private static final String DATA_NAME = "reimagined_settlements";

    public enum Reason { OK, LIMIT, TOO_CLOSE }
    public record FoundResult(Settlement settlement, Reason reason) {}

    private final Map<UUID, Settlement> settlements = new LinkedHashMap<>();

    public static SettlementData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(SettlementData::new, SettlementData::load), DATA_NAME);
    }

    public static SettlementData load(CompoundTag tag, HolderLookup.Provider registries) {
        SettlementData data = new SettlementData();
        ListTag list = tag.getList("Settlements", Tag.TAG_COMPOUND);
        int limit = Math.min(list.size(), MAX_SETTLEMENTS);
        for (int i = 0; i < limit; i++) {
            Settlement settlement = Settlement.load(list.getCompound(i));
            if (settlement != null) data.settlements.put(settlement.id(), settlement);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("Version", 1);
        ListTag list = new ListTag();
        for (Settlement settlement : settlements.values()) list.add(settlement.save());
        tag.put("Settlements", list);
        return tag;
    }

    public FoundResult found(ServerLevel level, SettlementType type, String requestedName, BlockPos position) {
        if (settlements.size() >= MAX_SETTLEMENTS) return new FoundResult(null, Reason.LIMIT);
        for (Settlement existing : settlements.values()) {
            long dx = (long) existing.position().getX() - position.getX();
            long dz = (long) existing.position().getZ() - position.getZ();
            if (dx * dx + dz * dz < (long) MIN_SPACING * MIN_SPACING) {
                return new FoundResult(null, Reason.TOO_CLOSE);
            }
        }
        String name = requestedName == null ? "" : requestedName.trim();
        if (name.isEmpty()) name = type.id + " " + (settlements.size() + 1);
        if (name.length() > Settlement.MAX_NAME_LENGTH) name = name.substring(0, Settlement.MAX_NAME_LENGTH);
        Settlement settlement = Settlement.create(name, type, position, CalendarData.get(level).day(level),
                level.getGameTime());
        settlements.put(settlement.id(), settlement);
        setDirty();
        return new FoundResult(settlement, Reason.OK);
    }

    public Collection<Settlement> all() {
        return List.copyOf(settlements.values());
    }

    public Optional<Settlement> byId(UUID id) {
        return Optional.ofNullable(settlements.get(id));
    }

    /** Resolves a full UUID or an unambiguous prefix, for command input. */
    public Optional<Settlement> find(String token) {
        if (token == null || token.isBlank()) return Optional.empty();
        String normalized = token.toLowerCase(java.util.Locale.ROOT);
        List<Settlement> matches = new ArrayList<>(1);
        for (Settlement settlement : settlements.values()) {
            String id = settlement.id().toString();
            if (id.equals(normalized) || id.startsWith(normalized)) matches.add(settlement);
        }
        return matches.size() == 1 ? Optional.of(matches.getFirst()) : Optional.empty();
    }

    public boolean remove(UUID id) {
        boolean removed = settlements.remove(id) != null;
        if (removed) setDirty();
        return removed;
    }

    public int size() {
        return settlements.size();
    }
}
