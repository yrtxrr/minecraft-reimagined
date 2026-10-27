package dev.reimagined.content;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ManaRulesTest {
    @Test void regenerationIsBoundedEvenForHugeElapsedTime() {
        assertEquals(100, ManaRules.regenerate(99, Long.MAX_VALUE));
        assertEquals(100, ManaRules.regenerate(Integer.MAX_VALUE, 10));
        assertEquals(0, ManaRules.regenerate(-20, -1));
    }
    @Test void costsNeverAllowNegativeOrInsufficientResources() {
        assertFalse(ManaRules.canCast(20, 21));
        assertTrue(ManaRules.canCast(20, 20));
        assertFalse(ManaRules.canCast(100, -1));
        assertFalse(ManaRules.canCast(Integer.MAX_VALUE, 101));
    }
}
