package dev.reimagined.season;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class SeasonCalendarTest {
    @Test void seasonBoundariesAndYears() {
        assertEquals(SeasonCalendar.Season.SPRING, SeasonCalendar.season(0));
        assertEquals(SeasonCalendar.Season.SPRING, SeasonCalendar.season(23));
        assertEquals(SeasonCalendar.Season.SUMMER, SeasonCalendar.season(24));
        assertEquals(SeasonCalendar.Season.AUTUMN, SeasonCalendar.season(48));
        assertEquals(SeasonCalendar.Season.WINTER, SeasonCalendar.season(72));
        assertEquals(SeasonCalendar.Season.SPRING, SeasonCalendar.season(96));
        assertEquals(SeasonCalendar.Season.WINTER, SeasonCalendar.season(-1));
        assertEquals(24, SeasonCalendar.dayOfSeason(-1));
    }
    @Test void greenhouseProtectsTemperateWinterFarming() {
        assertEquals(25, SeasonCalendar.growthChance(80, .8f, false));
        assertEquals(100, SeasonCalendar.growthChance(80, .8f, true));
        assertEquals(100, SeasonCalendar.growthChance(80, 2f, false));
        assertEquals(100, SeasonCalendar.growthChance(80, 0f, false));
        assertEquals(75, SeasonCalendar.growthChance(24, .8f, false));
    }
}
