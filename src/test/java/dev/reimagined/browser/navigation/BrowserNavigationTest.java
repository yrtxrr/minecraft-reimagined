package dev.reimagined.browser.navigation;

import java.util.Locale;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BrowserNavigationTest {
    @Test void everySearchTermMustMatchAcrossNameAndId() {
        assertTrue(BrowserSearch.matches("Iron Spear", "reimagined:iron_spear", "SPEAR @reimagined iron"));
        assertTrue(BrowserSearch.matches("Железное копьё", "reimagined:iron_spear", "КОПЬЁ желез"));
        assertFalse(BrowserSearch.matches("Iron Spear", "reimagined:iron_spear", "spear diamond"));
        assertFalse(BrowserSearch.matches("Iron Spear", "reimagined:iron_spear", "@minecraft"));
        assertFalse(BrowserSearch.matches("Iron Spear", "reimagined:iron_spear", "@"));
        assertTrue(BrowserSearch.matches("Iron Spear", "reimagined:iron_spear", " \t\n "));
    }
    @Test void searchDoesNotDependOnHostLocale() {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            assertTrue(BrowserSearch.matches("IRON", "minecraft:iron_ingot", "iron @MINECRAFT"));
        } finally { Locale.setDefault(previous); }
    }
    @Test void shrinkingPagesOrResizingKeepsValidSelection() {
        assertEquals(1, BrowserPages.count(0, 54));
        assertEquals(0, BrowserPages.clamp(9, 0, 54));
        assertEquals(2, BrowserPages.clamp(9, 120, 54));
        assertEquals(0, BrowserPages.clamp(2, 4, 54));
        assertEquals(0, BrowserPages.clamp(-1, 120, 54));
        assertEquals(Integer.MAX_VALUE, BrowserPages.count(Integer.MAX_VALUE, 1));
        assertThrows(IllegalArgumentException.class, () -> BrowserPages.count(1, 0));
    }
    @Test void backRestoresFullSelectionAndDropsOnlyOldestWhenBounded() {
        record Selection(String item, boolean usages, int page) {}
        var history = new BrowserHistory<Selection>(2);
        history.push(new Selection("wheat", true, 7));
        history.push(new Selection("bread", false, 3));
        assertEquals(new Selection("bread", false, 3), history.back().orElseThrow());
        assertEquals(new Selection("wheat", true, 7), history.back().orElseThrow());
        assertTrue(history.back().isEmpty());
        history.push(new Selection("old", false, 0));
        history.push(new Selection("middle", false, 1));
        history.push(new Selection("new", true, 2));
        assertEquals(2, history.size());
        assertEquals("new", history.back().orElseThrow().item());
        assertEquals("middle", history.back().orElseThrow().item());
        assertTrue(history.back().isEmpty());
    }
}
