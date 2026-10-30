package dev.reimagined.civilization;

import java.util.Locale;

/**
 * Settlement specialisation. Values are per-mille of population per simulated hour.
 * Pure data: no Minecraft classes, so the economy rules stay independently testable.
 */
public enum SettlementType {
    AGRICULTURAL("agricultural", 700, 150, 80, 40),
    TRADING("trading", 350, 200, 120, 220),
    INDUSTRIAL("industrial", 250, 550, 140, 100),
    MILITARY("military", 400, 300, 100, 60),
    THAUMATURGIC("thaumaturgic", 300, 150, 400, 90),
    NATURE_MAGIC("nature_magic", 500, 200, 300, 70),
    MIXED("mixed", 450, 300, 180, 110);

    public final String id;
    public final int foodPermille;
    public final int materialPermille;
    public final int knowledgePermille;
    public final int tradePermille;

    SettlementType(String id, int foodPermille, int materialPermille, int knowledgePermille, int tradePermille) {
        this.id = id;
        this.foodPermille = foodPermille;
        this.materialPermille = materialPermille;
        this.knowledgePermille = knowledgePermille;
        this.tradePermille = tradePermille;
    }

    public String translationKey() {
        return "settlement.reimagined.type." + id;
    }

    public static SettlementType byId(String id) {
        String normalized = id == null ? "" : id.toLowerCase(Locale.ROOT);
        for (SettlementType type : values()) {
            if (type.id.equals(normalized) || type.name().toLowerCase(Locale.ROOT).equals(normalized)) return type;
        }
        return null;
    }
}
