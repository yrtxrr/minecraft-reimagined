package dev.reimagined.civilization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.reimagined.season.SeasonCalendar;
import org.junit.jupiter.api.Test;

class SettlementEconomyTest {
    @Test
    void seasonsChangeFoodOutputWithoutChangingConsumption() {
        int spring = SettlementEconomy.foodProduced(SettlementType.AGRICULTURAL, TechEra.FRONTIER, 24,
                SeasonCalendar.Season.SPRING);
        int summer = SettlementEconomy.foodProduced(SettlementType.AGRICULTURAL, TechEra.FRONTIER, 24,
                SeasonCalendar.Season.SUMMER);
        int autumn = SettlementEconomy.foodProduced(SettlementType.AGRICULTURAL, TechEra.FRONTIER, 24,
                SeasonCalendar.Season.AUTUMN);
        int winter = SettlementEconomy.foodProduced(SettlementType.AGRICULTURAL, TechEra.FRONTIER, 24,
                SeasonCalendar.Season.WINTER);
        assertEquals(16, spring);
        assertEquals(18, summer);
        assertEquals(15, autumn);
        assertEquals(10, winter);
        assertTrue(winter < autumn && autumn < spring && spring < summer);
        assertEquals(9, SettlementEconomy.foodConsumed(24));
        assertEquals(SettlementEconomy.foodConsumed(24),
                SettlementEconomy.foodConsumed(24), "Consumption must not depend on the season");
    }

    @Test
    void laterErasProduceMoreFromTheSamePopulation() {
        int frontier = SettlementEconomy.materialsProduced(SettlementType.INDUSTRIAL, TechEra.FRONTIER, 24);
        int settled = SettlementEconomy.materialsProduced(SettlementType.INDUSTRIAL, TechEra.SETTLED, 24);
        int crafted = SettlementEconomy.materialsProduced(SettlementType.INDUSTRIAL, TechEra.CRAFTED, 24);
        assertTrue(frontier > 0 && frontier < settled && settled < crafted);
        assertTrue(SettlementEconomy.knowledgeProduced(SettlementType.THAUMATURGIC, TechEra.FRONTIER, 24) > 0);
        assertTrue(SettlementEconomy.knowledgeProduced(SettlementType.THAUMATURGIC, TechEra.FRONTIER, 24)
                > SettlementEconomy.knowledgeProduced(SettlementType.AGRICULTURAL, TechEra.FRONTIER, 24));
    }

    @Test
    void specialisationDeterminesTradeIncome() {
        int agricultural = SettlementEconomy.tradeIncome(SettlementType.AGRICULTURAL, TechEra.FRONTIER, 24);
        int trading = SettlementEconomy.tradeIncome(SettlementType.TRADING, TechEra.FRONTIER, 24);
        assertTrue(agricultural > 0, "Even a farming village sells a little surplus");
        assertTrue(trading > agricultural);
        assertTrue(SettlementEconomy.tradeIncome(SettlementType.TRADING, TechEra.ARCANE, 24) > trading);
        assertEquals(0, SettlementEconomy.tradeIncome(SettlementType.TRADING, TechEra.FRONTIER, 0));
    }

    @Test
    void growthNeedsFoodMaterialsAndSpareHousing() {
        assertTrue(SettlementEconomy.canGrow(24, 32, 192, 12));
        assertFalse(SettlementEconomy.canGrow(24, 32, 191, 12), "Food below eight per inhabitant blocks growth");
        assertFalse(SettlementEconomy.canGrow(24, 32, 192, 11), "Material is required for a new home");
        assertFalse(SettlementEconomy.canGrow(32, 32, 999, 999), "Population cannot exceed housing");
        assertFalse(SettlementEconomy.canGrow(0, 32, 999, 999));
    }

    @Test
    void housingExpandsBeforeBedsRunOutAndIsCappedByEra() {
        assertTrue(SettlementEconomy.canBuildHousing(TechEra.SETTLED, 28, 32, 60));
        assertFalse(SettlementEconomy.canBuildHousing(TechEra.SETTLED, 20, 32, 60), "No premature sprawl");
        assertFalse(SettlementEconomy.canBuildHousing(TechEra.SETTLED, 28, 32, 59));
        assertEquals(32, SettlementEconomy.housingCap(TechEra.FRONTIER));
        assertEquals(80, SettlementEconomy.housingCap(TechEra.SETTLED));
        assertEquals(128, SettlementEconomy.housingCap(TechEra.CRAFTED));
        assertEquals(176, SettlementEconomy.housingCap(TechEra.ARCANE));
        assertFalse(SettlementEconomy.canBuildHousing(TechEra.FRONTIER, 32, 32, 999),
                "Frontier housing is capped at 32 even with materials");
    }

    @Test
    void starvationIsDetectedBeforePopulationLoss() {
        assertTrue(SettlementEconomy.starving(8, 9));
        assertFalse(SettlementEconomy.starving(9, 9));
        assertFalse(SettlementEconomy.starving(10, 9));
    }

    @Test
    void eraThresholdsAndProductionRiseMonotonically() {
        int previousKnowledge = -1;
        int previousMaterials = -1;
        int previousProduction = -1;
        for (TechEra era : TechEra.values()) {
            assertTrue(era.knowledgeRequired > previousKnowledge);
            assertTrue(era.materialsRequired > previousMaterials);
            assertTrue(era.productionPercent > previousProduction);
            previousKnowledge = era.knowledgeRequired;
            previousMaterials = era.materialsRequired;
            previousProduction = era.productionPercent;
        }
        assertEquals(TechEra.SETTLED, TechEra.FRONTIER.next());
        assertEquals(null, TechEra.ARCANE.next());
    }

    @Test
    void stockCapacityScalesWithPopulationAndNeverGoesNegative() {
        assertEquals(960, SettlementEconomy.foodCapacity(24));
        assertEquals(480, SettlementEconomy.materialsCapacity(24));
        assertEquals(2400, SettlementEconomy.treasuryCapacity(24));
        assertTrue(SettlementEconomy.foodCapacity(0) > 0);
        assertEquals(0, SettlementEconomy.foodProduced(SettlementType.MIXED, TechEra.FRONTIER, 0,
                SeasonCalendar.Season.SPRING));
        assertEquals(0, SettlementEconomy.foodConsumed(0));
    }

    @Test
    void rulesAreDeterministic() {
        for (SeasonCalendar.Season season : SeasonCalendar.Season.values()) {
            assertEquals(SettlementEconomy.foodProduced(SettlementType.NATURE_MAGIC, TechEra.CRAFTED, 47, season),
                    SettlementEconomy.foodProduced(SettlementType.NATURE_MAGIC, TechEra.CRAFTED, 47, season));
        }
        assertEquals(SettlementType.TRADING, SettlementType.byId("trading"));
        assertEquals(SettlementType.NATURE_MAGIC, SettlementType.byId("NATURE_MAGIC"));
        assertEquals(null, SettlementType.byId("unknown"));
    }
}
