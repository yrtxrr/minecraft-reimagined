package dev.reimagined.civilization;

import dev.reimagined.season.SeasonCalendar;

/**
 * Deterministic hourly economy rules. No world access, randomness or clock reads: the same
 * inputs always produce the same outputs, and one call mutates nothing.
 */
public final class SettlementEconomy {
    /** Grain eaten per thousand inhabitants each simulated hour. */
    public static final int FOOD_CONSUMPTION_PERMILLE = 400;
    public static final int GROWTH_FOOD_PER_CAPITA = 8;
    public static final int GROWTH_MATERIAL_COST = 12;
    public static final int HOUSE_MATERIAL_COST = 60;
    public static final int HOUSE_SIZE = 2;
    public static final int HOUSE_BUILD_HEADROOM = 4;
    public static final int EMERGENCY_TRADE_COST = 20;
    public static final int EMERGENCY_FOOD = 10;
    public static final int KNOWLEDGE_CAP = 100_000;

    private SettlementEconomy() {}

    public static int foodProduced(SettlementType type, TechEra era, int population, SeasonCalendar.Season season) {
        if (population <= 0) return 0;
        long raw = (long) population * type.foodPermille * era.productionPercent * seasonFoodPercent(season);
        return (int) (raw / 10_000_000L);
    }

    public static int foodConsumed(int population) {
        return Math.max(0, population) * FOOD_CONSUMPTION_PERMILLE / 1_000;
    }

    public static int materialsProduced(SettlementType type, TechEra era, int population) {
        if (population <= 0) return 0;
        return (int) ((long) population * type.materialPermille * era.productionPercent / 100_000L);
    }

    public static int knowledgeProduced(SettlementType type, TechEra era, int population) {
        if (population <= 0) return 0;
        return (int) ((long) population * type.knowledgePermille * era.productionPercent / 100_000L);
    }

    public static int tradeIncome(SettlementType type, TechEra era, int population) {
        if (population <= 0) return 0;
        return (int) ((long) population * type.tradePermille * era.productionPercent / 50_000L);
    }

    public static int seasonFoodPercent(SeasonCalendar.Season season) {
        return switch (season) {
            case SPRING -> 100;
            case SUMMER -> 110;
            case AUTUMN -> 90;
            case WINTER -> 60;
        };
    }

    /** Housing is the hard population ceiling; each era unlocks a larger building stock. */
    public static int housingCap(TechEra era) {
        return 32 + era.ordinal() * 48;
    }

    public static int foodCapacity(int population) {
        return Math.max(120, Math.max(0, population) * 40);
    }

    public static int materialsCapacity(int population) {
        return Math.max(80, Math.max(0, population) * 20);
    }

    public static int treasuryCapacity(int population) {
        return Math.max(60, Math.max(0, population) * 100);
    }

    /** Growth needs spare housing, eight grain per inhabitant and enough material for a new home. */
    public static boolean canGrow(int population, int housing, int food, int materials) {
        return population > 0 && population < housing
                && food >= population * GROWTH_FOOD_PER_CAPITA
                && materials >= GROWTH_MATERIAL_COST;
    }

    /** Housing is expanded autonomously before the settlement runs out of beds. */
    public static boolean canBuildHousing(TechEra era, int population, int housing, int materials) {
        return housing < housingCap(era)
                && materials >= HOUSE_MATERIAL_COST
                && population >= housing - HOUSE_BUILD_HEADROOM;
    }

    public static boolean starving(int food, int consumption) {
        return food < consumption;
    }
}
