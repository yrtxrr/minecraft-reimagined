package dev.reimagined.progression;

import java.util.Locale;

/** Stable save identifiers: never rename an enum constant without a data migration. */
public enum Skill {
    WARRIOR, ARCHER, MAGE, ALCHEMIST, ENGINEER, MINER, SMITH, FARMER, SAILOR, MERCHANT;

    public String id() { return name().toLowerCase(Locale.ROOT); }
    public String translationKey() { return "skill.reimagined." + id(); }
}
