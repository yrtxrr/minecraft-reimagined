package dev.reimagined.progression;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigInteger;
import org.junit.jupiter.api.Test;

class SkillProgressTest {
    @Test void thresholdsDoNotAwardLevelsEarly() {
        assertEquals(BigInteger.ZERO, SkillProgress.EMPTY.award(99).level());
        assertEquals(BigInteger.ONE, SkillProgress.EMPTY.award(100).level());
        assertEquals(BigInteger.ONE, SkillProgress.EMPTY.award(299).level());
        assertEquals(BigInteger.TWO, SkillProgress.EMPTY.award(300).level());
        assertEquals(BigInteger.valueOf(3), SkillProgress.EMPTY.award(600).level());
    }

    @Test void pointsAreSpentOnceAndAllRanksRemainLearnable() {
        assertThrows(IllegalStateException.class, SkillProgress.EMPTY::unlock);
        var progress = SkillProgress.EMPTY.award(600);
        for (int i = 0; i < SkillProgress.MAX_RANK; i++) progress = progress.unlock();
        assertEquals(3, progress.rank());
        assertEquals(BigInteger.ZERO, progress.availablePoints());
        assertEquals(1.15, progress.bonusMultiplier(), 0.000001);
        assertThrows(IllegalStateException.class, progress::unlock);
        assertEquals(BigInteger.valueOf(730), progress.award(100).experience());
    }

    @Test void unboundedLevelsDoNotOverflowOrIncreaseMaximumBonus() {
        BigInteger level = BigInteger.TEN.pow(30);
        BigInteger xp = SkillProgress.experienceForLevel(level);
        var progress = new SkillProgress(xp, 3);
        assertEquals(level, progress.level());
        assertEquals(level.subtract(BigInteger.ONE), new SkillProgress(xp.subtract(BigInteger.ONE), 3).level());
        assertEquals(3, progress.award(Long.MAX_VALUE).rank());
        assertEquals(1.15, progress.bonusMultiplier(), 0.000001);
    }

    @Test void invalidOrForgedProgressionIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new SkillProgress(BigInteger.valueOf(-1), 0));
        assertThrows(IllegalArgumentException.class, () -> new SkillProgress(BigInteger.ZERO, 1));
        assertThrows(IllegalArgumentException.class, () -> new SkillProgress(BigInteger.valueOf(5000), 4));
        assertThrows(IllegalArgumentException.class, () -> SkillProgress.EMPTY.award(-1));
    }
}
