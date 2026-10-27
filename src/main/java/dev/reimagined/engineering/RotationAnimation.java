package dev.reimagined.engineering;

/** RPM -> degrees per tick, keeping long-running worlds numerically bounded. */
public final class RotationAnimation {
    private RotationAnimation() {}
    public static double normalize(double degrees) { return (degrees % 360.0 + 360.0) % 360.0; }
    public static double angle(double anchor, long anchorTick, int rpm, long tick, float partialTick) {
        if (rpm == 0) return normalize(anchor);
        // 12000 ticks gives an integral number of turns for every integral RPM.
        long elapsed = Math.floorMod(tick - anchorTick, 12000L);
        return normalize(anchor + (elapsed + partialTick) * rpm * 0.3);
    }
    public static double difference(double first, double second) {
        double delta = Math.abs(normalize(first) - normalize(second));
        return Math.min(delta, 360 - delta);
    }
}
