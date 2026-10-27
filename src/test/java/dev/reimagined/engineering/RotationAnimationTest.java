package dev.reimagined.engineering;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RotationAnimationTest {
    @Test void rpmAndPartialFramesHavePhysicalContinuousAngles() {
        assertEquals(96, RotationAnimation.angle(0, 100, 16, 120, 0), 0.00001);
        assertEquals(98.4, RotationAnimation.angle(0, 100, 16, 120, .5f), .00001);
        assertEquals(264, RotationAnimation.angle(0, 100, -16, 120, 0), .00001);
    }
    @Test void stopFreezesPhaseAndReanchorPreservesContinuity() {
        double phase = RotationAnimation.angle(23, 0, 16, 27, 0);
        assertEquals(phase, RotationAnimation.angle(phase, 27, 0, 10000, .8f));
        assertEquals(phase, RotationAnimation.angle(phase, 400, 16, 400, 0), .00001);
        assertEquals(0, RotationAnimation.difference(359, -1), .00001);
        assertEquals(2, RotationAnimation.difference(359, 1), .00001);
    }
    @Test void longWorldTimeRemainsPeriodicAndFinite() {
        long time = 1_000_000_000_000L;
        assertEquals(RotationAnimation.angle(7, 0, 16, time, .25f), RotationAnimation.angle(7, 0, 16, time + 12000, .25f), .00001);
        assertTrue(Double.isFinite(RotationAnimation.angle(7, 0, 16, Long.MAX_VALUE, .25f)));
    }
}
