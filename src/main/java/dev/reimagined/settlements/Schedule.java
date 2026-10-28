package dev.reimagined.settlements;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;

/**
 * Daily schedule for settlers. States:
 * IDLE, MORNING_EXIT, WORK, BREAK, EVENING_RETURN, NIGHT_REST, SHELTER.
 *
 * Times are in world ticks (20 ticks/second). Day starts at tick 0 (dawn).
 */
public final class Schedule {
    public enum State { IDLE, MORNING_EXIT, WORK, BREAK, EVENING_RETURN, NIGHT_REST, SHELTER }

    private static final int DAY_TICKS = 24000;
    private static final int MORNING_START = 6000;   // 6:00
    private static final int WORK_START = 8000;      // 8:00
    private static final int WORK_END = 18000;       // 18:00
    private static final int EVENING_END = 20000;    // 20:00
    private static final int NIGHT_START = 22000;    // 22:00

    private Schedule() {}

    /**
     * Compute the schedule state for a settler based on the current world time.
     * Returns the target state; callers apply transitions.
     */
    public static State nextState(Mob settler, State current, int scheduleTick) {
        long time = settler.level().getDayTime() % DAY_TICKS;
        boolean raining = settler.level().isRaining();

        if (raining && current != State.SHELTER) {
            return State.SHELTER;
        }
        if (!raining && current == State.SHELTER) {
            return State.IDLE;
        }

        if (time >= MORNING_START && time < WORK_START) return State.MORNING_EXIT;
        if (time >= WORK_START && time < WORK_END) return State.WORK;
        if (time >= WORK_END && time < EVENING_END) return State.EVENING_RETURN;
        if (time >= EVENING_END || time < MORNING_START) return State.NIGHT_REST;
        return State.IDLE;
    }

    public static boolean isWorkTime(ServerLevel level) {
        long time = level.getDayTime() % DAY_TICKS;
        return time >= WORK_START && time < WORK_END;
    }

    public static boolean isNightTime(ServerLevel level) {
        long time = level.getDayTime() % DAY_TICKS;
        return time >= NIGHT_START || time < MORNING_START;
    }
}
