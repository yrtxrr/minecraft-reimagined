package dev.reimagined.settlements;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * World-owned settlement registry. Persists settler home/work positions and
 * trade data across world reloads. Stored in the overworld SavedData storage.
 */
public final class SettlementData extends SavedData {
    private static final String VERSION = "1";

    private final List<SettlerRecord> settlers = new ArrayList<>();
    private int nextSettlerId = 1;

    public static SettlementData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
                new Factory<>(SettlementData::new, SettlementData::load), "reimagined_settlements");
    }

    public static SettlementData load(CompoundTag tag, HolderLookup.Provider registries) {
        SettlementData data = new SettlementData();
        data.nextSettlerId = tag.getInt("NextSettlerId");
        ListTag list = tag.getList("Settlers", 10);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            SettlerRecord record = new SettlerRecord(
                    entry.getUUID("Uuid"),
                    new BlockPos(entry.getInt("HomeX"), entry.getInt("HomeY"), entry.getInt("HomeZ")),
                    new BlockPos(entry.getInt("WorkX"), entry.getInt("WorkY"), entry.getInt("WorkZ")),
                    entry.getString("Profession"),
                    entry.getInt("TradeTick"),
                    entry.getInt("Stock")
            );
            data.settlers.add(record);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putString("Version", VERSION);
        tag.putInt("NextSettlerId", nextSettlerId);
        ListTag list = new ListTag();
        for (SettlerRecord r : settlers) {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("Uuid", r.uuid);
            entry.putInt("HomeX", r.home.getX());
            entry.putInt("HomeY", r.home.getY());
            entry.putInt("HomeZ", r.home.getZ());
            entry.putInt("WorkX", r.work.getX());
            entry.putInt("WorkY", r.work.getY());
            entry.putInt("WorkZ", r.work.getZ());
            entry.putString("Profession", r.profession);
            entry.putInt("TradeTick", r.tradeTick);
            entry.putInt("Stock", r.stock);
            list.add(entry);
        }
        tag.put("Settlers", list);
        return tag;
    }

    public void registerSettler(UUID uuid, BlockPos home, BlockPos work, String profession) {
        settlers.add(new SettlerRecord(uuid, home, work, profession, 0, 0));
        setDirty();
    }

    public void removeSettler(UUID uuid) {
        settlers.removeIf(r -> r.uuid.equals(uuid));
        setDirty();
    }

    public SettlerRecord findSettler(UUID uuid) {
        for (SettlerRecord r : settlers) {
            if (r.uuid.equals(uuid)) return r;
        }
        return null;
    }

    public List<SettlerRecord> settlers() { return settlers; }

    public void clear() {
        settlers.clear();
        nextSettlerId = 1;
        setDirty();
    }

    public static final class SettlerRecord {
        public final UUID uuid;
        public final BlockPos home;
        public final BlockPos work;
        public final String profession;
        public int tradeTick;
        public int stock;

        public SettlerRecord(UUID uuid, BlockPos home, BlockPos work, String profession, int tradeTick, int stock) {
            this.uuid = uuid;
            this.home = home;
            this.work = work;
            this.profession = profession;
            this.tradeTick = tradeTick;
            this.stock = stock;
        }
    }
}
