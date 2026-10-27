package dev.reimagined.progression;

import java.math.BigInteger;
import java.util.Objects;

/** Pure progression arithmetic. Levels have no gameplay cap; upgrades deliberately do. */
public record SkillProgress(BigInteger experience, int rank) {
    public static final int MAX_RANK = 3;
    private static final BigInteger FIFTY = BigInteger.valueOf(50);
    public static final SkillProgress EMPTY = new SkillProgress(BigInteger.ZERO, 0);

    public SkillProgress {
        Objects.requireNonNull(experience);
        if (experience.signum() < 0 || rank < 0 || rank > MAX_RANK) {
            throw new IllegalArgumentException("Invalid progression");
        }
        // Every rank costs one level-derived point. Prevent corrupt saves creating free ranks.
        if (levelFor(experience).compareTo(BigInteger.valueOf(rank)) < 0) {
            throw new IllegalArgumentException("Rank exceeds earned points");
        }
    }

    /** Level L requires 50 * L * (L + 1) total XP: 100, 300, 600, ... */
    public static BigInteger experienceForLevel(BigInteger level) {
        if (level.signum() < 0) throw new IllegalArgumentException("Negative level");
        return FIFTY.multiply(level).multiply(level.add(BigInteger.ONE));
    }

    public static BigInteger levelFor(BigInteger experience) {
        if (experience.signum() < 0) throw new IllegalArgumentException("Negative XP");
        return experience.divide(FIFTY).multiply(BigInteger.valueOf(4)).add(BigInteger.ONE)
                .sqrt().subtract(BigInteger.ONE).divide(BigInteger.TWO);
    }

    public BigInteger level() { return levelFor(experience); }
    public BigInteger availablePoints() { return level().subtract(BigInteger.valueOf(rank)); }
    public BigInteger toNextLevel() { return experienceForLevel(level().add(BigInteger.ONE)).subtract(experience); }

    public SkillProgress award(long amount) {
        if (amount < 0) throw new IllegalArgumentException("Negative XP award");
        // Mastery ranks improve training by a bounded 10% per rank, never exponential.
        BigInteger gain = BigInteger.valueOf(amount).multiply(BigInteger.valueOf(10L + rank)).divide(BigInteger.TEN);
        return new SkillProgress(experience.add(gain), rank);
    }

    public SkillProgress unlock() {
        if (rank == MAX_RANK) throw new IllegalStateException("All upgrades learned");
        if (availablePoints().signum() <= 0) throw new IllegalStateException("No skill point available");
        return new SkillProgress(experience, rank + 1);
    }

    public double bonusMultiplier() { return 1.0 + rank * 0.05; }
}
