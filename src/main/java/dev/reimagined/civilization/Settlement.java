package dev.reimagined.civilization;

import dev.reimagined.season.SeasonCalendar;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

/**
 * Persistent abstract state of one settlement. Deliberately contains no entities, chunks or
 * block references: remote simulation only ever touches these scalars.
 */
public final class Settlement {
    public static final int MAX_NAME_LENGTH = 32;
    private static final int SAVE_VERSION = 1;

    private final UUID id;
    private final String name;
    private final SettlementType type;
    private final BlockPos position;
    private final long foundedDay;
    private TechEra era;
    private int population;
    private int housing;
    private int food;
    private int materials;
    private int knowledge;
    private int treasury;
    private long lastSimulatedGameTime;
    private boolean foundedAnnounced;
    private boolean firstTradeAnnounced;
    private int eraAnnouncedMask;
    private int growthMilestone;
    private int housingMilestone;

    private Settlement(UUID id, String name, SettlementType type, BlockPos position, long foundedDay,
                       long lastSimulatedGameTime) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.position = position.immutable();
        this.foundedDay = foundedDay;
        this.lastSimulatedGameTime = lastSimulatedGameTime;
        this.era = TechEra.FRONTIER;
        this.population = 24;
        this.housing = 32;
        this.food = 160;
        this.materials = 120;
        this.knowledge = 0;
        this.treasury = 40;
    }

    static Settlement create(String name, SettlementType type, BlockPos position, long foundedDay, long gameTime) {
        return new Settlement(UUID.randomUUID(), name, type, position, foundedDay, gameTime);
    }

    /**
     * Advances exactly one simulated hour. Returns a bit mask of {@link QuestSignalType} ordinals
     * for transitions that have not been reported before. The caller posts the events.
     */
    int advanceHour(SeasonCalendar.Season season) {
        int triggered = 0;
        int foodCap = SettlementEconomy.foodCapacity(population);
        int materialCap = SettlementEconomy.materialsCapacity(population);
        food = Math.min(foodCap, food + SettlementEconomy.foodProduced(type, era, population, season));
        materials = Math.min(materialCap, materials + SettlementEconomy.materialsProduced(type, era, population));
        knowledge = Math.min(SettlementEconomy.KNOWLEDGE_CAP,
                knowledge + SettlementEconomy.knowledgeProduced(type, era, population));

        int income = SettlementEconomy.tradeIncome(type, era, population);
        if (income > 0) {
            treasury = Math.min(SettlementEconomy.treasuryCapacity(population), treasury + income);
            if (!firstTradeAnnounced) {
                firstTradeAnnounced = true;
                triggered |= bit(QuestSignalType.SETTLEMENT_FIRST_TRADE);
            }
        }

        int consumed = SettlementEconomy.foodConsumed(population);
        if (SettlementEconomy.starving(food, consumed)) {
            // A settlement with coin buys grain before people leave.
            if (treasury >= SettlementEconomy.EMERGENCY_TRADE_COST) {
                treasury -= SettlementEconomy.EMERGENCY_TRADE_COST;
                food += SettlementEconomy.EMERGENCY_FOOD;
            }
            if (SettlementEconomy.starving(food, consumed)) {
                population = Math.max(1, population - 1);
                food = 0;
            } else {
                food -= consumed;
            }
        } else {
            food -= consumed;
        }

        if (SettlementEconomy.canBuildHousing(era, population, housing, materials)) {
            materials -= SettlementEconomy.HOUSE_MATERIAL_COST;
            housing = Math.min(SettlementEconomy.housingCap(era), housing + SettlementEconomy.HOUSE_SIZE);
            if (housing / 8 > housingMilestone) {
                housingMilestone = housing / 8;
                triggered |= bit(QuestSignalType.SETTLEMENT_EXPANDED);
            }
        }

        if (SettlementEconomy.canGrow(population, housing, food, materials)) {
            population++;
            food -= SettlementEconomy.GROWTH_FOOD_PER_CAPITA;
            materials -= SettlementEconomy.GROWTH_MATERIAL_COST;
            if (population / 8 > growthMilestone) {
                growthMilestone = population / 8;
                triggered |= bit(QuestSignalType.SETTLEMENT_GREW);
            }
        }

        TechEra next = era.next();
        if (next != null && knowledge >= next.knowledgeRequired && materials >= next.materialsRequired) {
            materials -= next.materialsRequired;
            era = next;
            if ((eraAnnouncedMask & (1 << era.ordinal())) == 0) {
                eraAnnouncedMask |= 1 << era.ordinal();
                triggered |= bit(QuestSignalType.SETTLEMENT_ERA_ADVANCED);
            }
        }
        return triggered;
    }

    private static int bit(QuestSignalType type) {
        return 1 << type.ordinal();
    }

    public UUID id() { return id; }
    public String name() { return name; }
    public SettlementType type() { return type; }
    public BlockPos position() { return position; }
    public long foundedDay() { return foundedDay; }
    public TechEra era() { return era; }
    public int population() { return population; }
    public int housing() { return housing; }
    public int food() { return food; }
    public int materials() { return materials; }
    public int knowledge() { return knowledge; }
    public int treasury() { return treasury; }
    public long lastSimulatedGameTime() { return lastSimulatedGameTime; }
    public int housingCap() { return SettlementEconomy.housingCap(era); }

    void markSimulated(long gameTime) { this.lastSimulatedGameTime = gameTime; }

    boolean markFoundedAnnounced() {
        if (foundedAnnounced) return false;
        foundedAnnounced = true;
        return true;
    }

    CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Version", SAVE_VERSION);
        tag.putUUID("Id", id);
        tag.putString("Name", name);
        tag.putString("Type", type.id);
        tag.putString("Era", era.name());
        tag.putInt("X", position.getX());
        tag.putInt("Y", position.getY());
        tag.putInt("Z", position.getZ());
        tag.putInt("Population", population);
        tag.putInt("Housing", housing);
        tag.putInt("Food", food);
        tag.putInt("Materials", materials);
        tag.putInt("Knowledge", knowledge);
        tag.putInt("Treasury", treasury);
        tag.putLong("FoundedDay", foundedDay);
        tag.putLong("LastGameTime", lastSimulatedGameTime);
        tag.putBoolean("FoundedAnnounced", foundedAnnounced);
        tag.putBoolean("FirstTradeAnnounced", firstTradeAnnounced);
        tag.putInt("EraAnnouncedMask", eraAnnouncedMask);
        tag.putInt("GrowthMilestone", growthMilestone);
        tag.putInt("HousingMilestone", housingMilestone);
        return tag;
    }

    static Settlement load(CompoundTag tag) {
        if (!tag.hasUUID("Id")) return null;
        SettlementType type = SettlementType.byId(tag.getString("Type"));
        if (type == null) return null;
        BlockPos position = new BlockPos(tag.getInt("X"), tag.getInt("Y"), tag.getInt("Z"));
        String name = tag.getString("Name");
        if (name.isBlank()) name = type.id + " settlement";
        Settlement settlement = new Settlement(tag.getUUID("Id"), name, type, position,
                tag.getLong("FoundedDay"), tag.getLong("LastGameTime"));
        try {
            settlement.era = TechEra.valueOf(tag.getString("Era"));
        } catch (IllegalArgumentException ignored) {
            settlement.era = TechEra.FRONTIER;
        }
        settlement.population = Math.max(0, tag.getInt("Population"));
        settlement.housing = Math.max(settlement.population, tag.getInt("Housing"));
        settlement.food = Math.max(0, tag.getInt("Food"));
        settlement.materials = Math.max(0, tag.getInt("Materials"));
        settlement.knowledge = Math.clamp(tag.getInt("Knowledge"), 0, SettlementEconomy.KNOWLEDGE_CAP);
        settlement.treasury = Math.max(0, tag.getInt("Treasury"));
        settlement.foundedAnnounced = tag.getBoolean("FoundedAnnounced");
        settlement.firstTradeAnnounced = tag.getBoolean("FirstTradeAnnounced");
        settlement.eraAnnouncedMask = tag.getInt("EraAnnouncedMask");
        settlement.growthMilestone = tag.getInt("GrowthMilestone");
        settlement.housingMilestone = tag.getInt("HousingMilestone");
        return settlement;
    }
}
