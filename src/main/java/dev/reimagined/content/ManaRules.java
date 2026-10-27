package dev.reimagined.content;

/** Small, bounded resource model shared by all spell schools. */
public final class ManaRules {
    public static final int CAPACITY = 100;
    private ManaRules() {}
    public static int clamp(int value) { return Math.clamp(value, 0, CAPACITY); }
    public static int regenerate(int current, long elapsedSeconds) {
        return (int) Math.min(CAPACITY, clamp(current) + Math.max(0L, Math.min(CAPACITY, elapsedSeconds)));
    }
    public static boolean canCast(int mana, int cost) { return cost >= 0 && clamp(mana) >= cost; }
}
