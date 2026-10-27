package dev.reimagined.engineering;

/** Fixed-point rotational network summary. Independent of world and safe to unit test. */
public record Rotation(int wheels, int machines, boolean truncated) {
    public static final int MAX_NODES = 128;
    public int capacity() { return wheels * 16; }
    public int load() { return machines * 8; }
    public boolean running() { return !truncated && wheels > 0 && capacity() >= load(); }
    public int speed() { return running() ? 16 : 0; }
}
