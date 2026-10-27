package dev.reimagined.season;

/** Pure calendar rules: four 24-day seasons, stable also before day zero. */
public final class SeasonCalendar {
    public static final int DAYS_PER_SEASON = 24;
    public static final int DAYS_PER_YEAR = 96;
    public static final long TICKS_PER_DAY = 24000;
    public enum Season { SPRING, SUMMER, AUTUMN, WINTER }
    private SeasonCalendar() {}
    public static Season season(long day) {
        return Season.values()[Math.floorMod(day, DAYS_PER_YEAR) / DAYS_PER_SEASON];
    }
    public static int dayOfSeason(long day) { return Math.floorMod(day, DAYS_PER_SEASON) + 1; }
    public static int growthChance(long day, float temperature, boolean sheltered) {
        if (sheltered || temperature < 0.15f || temperature > 1.5f) return 100;
        return switch (season(day)) { case WINTER -> 25; case SUMMER -> 75; default -> 100; };
    }
}
