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
    private static final org.slf4j.Logger LOGGER=org.slf4j.LoggerFactory.getLogger(SettlementData.class);
    private String migrationWarning="";
    private final java.util.Set<String> questSignals=new java.util.TreeSet<>();
    public java.util.Set<String> questSignals() { return java.util.Set.copyOf(questSignals); }
    public boolean recordQuestSignal(String id) {
        boolean known=false;for(var type:dev.reimagined.civilization.QuestSignalType.values())if(type.id.equals(id))known=true;
        if(!known || !questSignals.add(id))return false;
        setDirty();return true;
    }
    public String migrationWarning() { return migrationWarning; }

    private final List<SettlerRecord> settlers = new ArrayList<>();
    private int nextSettlerId = 1;
    private dev.reimagined.civilization.SettlementData civilizations = new dev.reimagined.civilization.SettlementData();
    public SettlementData() { civilizations.bindOwner(this::setDirty); }
    public dev.reimagined.civilization.SettlementData civilizations() { return civilizations; }

    public static SettlementData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
                new Factory<>(SettlementData::new, (tag,registries) -> {
                    if(version(tag)!=2) {
                        var directory=level.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve("data");
                        try {
                            for(String file:List.of("reimagined_settlements.dat","reimagined_settlements.dat_old")) {
                                var source=directory.resolve(file);
                                if(java.nio.file.Files.exists(source))LOGGER.info("Settlement migration backup: {}",SettlementSaveBackup.preserve(source));
                            }
                        } catch(java.io.IOException failure) { throw new IllegalStateException("Settlement backup failed; migration refused",failure); }
                    }
                    var loaded=load(tag,registries);
                    if(!loaded.migrationWarning.isEmpty())LOGGER.warn(loaded.migrationWarning);
                    return loaded;
                }), "reimagined_settlements");
    }

    public static SettlementData load(CompoundTag tag, HolderLookup.Provider registries) {
        SettlementData data = new SettlementData();
        int version=version(tag);
        if(version>2 || version<0)throw new IllegalStateException("Unsupported settlement version; refusing overwrite");
        data.nextSettlerId = Math.max(1,tag.getInt("NextSettlerId"));
        CompoundTag aggregates=new CompoundTag();
        if(tag.contains("Aggregates",9)) aggregates.put("Settlements",tag.getList("Aggregates",10).copy());
        else if(tag.contains("Civilizations",10)) aggregates=tag.getCompound("Civilizations");
        else aggregates=tag;
        data.civilizations=dev.reimagined.civilization.SettlementData.load(aggregates,registries);
        data.civilizations.bindOwner(data::setDirty);
        if(version!=2) {
            boolean npc=tag.contains("Settlers",9), remote=tag.contains("Aggregates",9)||tag.contains("Settlements",9)||tag.contains("Civilizations",10);
            if(!npc || !remote)data.migrationWarning="Settlement legacy migration: missing "+(!npc&&!remote?"both tiers":!npc?"NPC tier":"aggregate tier")+"; only present data preserved, no recovery invented";
        }
        var signals=tag.getList("QuestSignals",8);
        for(int i=0;i<Math.min(5,signals.size());i++) {
            String id=signals.getString(i);
            for(var type:dev.reimagined.civilization.QuestSignalType.values())if(type.id.equals(id))data.questSignals.add(id);
        }
        // Explicit announcement flags are historical facts; replay them without reposting events
        // or inventing milestones whose old payload did not prove the transition.
        var records=aggregates.getList("Settlements",10);
        for(int i=0;i<Math.min(64,records.size());i++) {
            var aggregate=records.getCompound(i);
            if(aggregate.getBoolean("FoundedAnnounced"))data.questSignals.add("settlement_founded");
            if(aggregate.getBoolean("FirstTradeAnnounced"))data.questSignals.add("settlement_first_trade");
            if((aggregate.getInt("EraAnnouncedMask") & ~1)!=0)data.questSignals.add("settlement_era_advanced");
        }
        java.util.Set<UUID> seen=new java.util.HashSet<>();
        ListTag list = tag.getList("Settlers", 10);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            if(!entry.hasUUID("Uuid") || !seen.add(entry.getUUID("Uuid"))) {
                data.migrationWarning += "; invalid/duplicate NPC UUID dropped (first wins)"; continue;
            }
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
        if(data.civilizations.duplicateCount()>0)data.migrationWarning += "; duplicate aggregate UUID dropped (first wins)";
        return data;
    }
    private static int version(CompoundTag tag) {
        if(!tag.contains("Version"))return 0;
        if(tag.contains("Version",8)) {
            try{return Integer.parseInt(tag.getString("Version"));}catch(NumberFormatException bad){throw new IllegalStateException("Unknown settlement version",bad);}
        }
        if(!tag.contains("Version",99))throw new IllegalStateException("Invalid settlement version type");
        return tag.getInt("Version");
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("Version", 2);
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
        tag.put("Aggregates", civilizations.save(new CompoundTag(),registries).getList("Settlements",10));
        var signals=new ListTag();questSignals.forEach(id->signals.add(net.minecraft.nbt.StringTag.valueOf(id)));tag.put("QuestSignals",signals);
        tag.remove("Settlements");tag.remove("Civilizations");
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
