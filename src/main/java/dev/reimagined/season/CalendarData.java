package dev.reimagined.season;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/** Calendar offset is world-owned, saved in the overworld and shared by all dimensions. */
public final class CalendarData extends SavedData {
    private long offsetDays;
    public static CalendarData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
            new Factory<>(CalendarData::new, CalendarData::load), "reimagined_calendar");
    }
    public static CalendarData load(CompoundTag tag, HolderLookup.Provider registries) {
        CalendarData data = new CalendarData();
        data.offsetDays = tag.getLong("OffsetDays");
        return data;
    }
    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("Version", 1);
        tag.putLong("OffsetDays", offsetDays);
        return tag;
    }
    public long day(ServerLevel level) {
        return Math.floorDiv(level.getServer().overworld().getDayTime(), SeasonCalendar.TICKS_PER_DAY) + offsetDays;
    }
    public void setSeason(ServerLevel level, int season) {
        long now = day(level);
        offsetDays += season * SeasonCalendar.DAYS_PER_SEASON - Math.floorMod(now, SeasonCalendar.DAYS_PER_YEAR);
        setDirty();
    }
}
