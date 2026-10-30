package dev.reimagined.civilization;

/**
 * Coarse technological era. Thresholds are cumulative knowledge plus a one-off material cost
 * paid when the era is entered; production scales with the era percent.
 */
public enum TechEra {
    FRONTIER("frontier", 100, 0, 0),
    SETTLED("settled", 125, 120, 80),
    CRAFTED("crafted", 160, 480, 260),
    ARCANE("arcane", 200, 1200, 650);

    public final String id;
    public final int productionPercent;
    public final int knowledgeRequired;
    public final int materialsRequired;

    TechEra(String id, int productionPercent, int knowledgeRequired, int materialsRequired) {
        this.id = id;
        this.productionPercent = productionPercent;
        this.knowledgeRequired = knowledgeRequired;
        this.materialsRequired = materialsRequired;
    }

    public String translationKey() {
        return "settlement.reimagined.era." + id;
    }

    public TechEra next() {
        TechEra[] values = values();
        int next = ordinal() + 1;
        return next < values.length ? values[next] : null;
    }
}
