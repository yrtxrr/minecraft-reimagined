package dev.reimagined.engineering;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
final class RotationTest {
    @Test void twoMachinesRunOnOneWheel() { assertTrue(new Rotation(1, 2, false).running()); }
    @Test void excessLoadStallsNetwork() { assertEquals(0, new Rotation(1, 3, false).speed()); }
    @Test void secondWheelAddsCapacityNotSpeed() { assertEquals(16, new Rotation(2, 4, false).speed()); }
    @Test void absentPowerAndTruncatedNetworksStop() {
        assertFalse(new Rotation(0, 0, false).running());
        assertFalse(new Rotation(100, 1, true).running());
    }
}
