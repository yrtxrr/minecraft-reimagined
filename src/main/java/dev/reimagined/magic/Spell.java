package dev.reimagined.magic;

import dev.reimagined.content.SpellWand.School;
import java.util.Locale;

/** Finite catalogue. The existing four spells stay available in all old/new worlds. */
public enum Spell {
    SPARK(School.ELEMENTAL,20,30,true,0), GROWTH(School.NATURE,25,100,true,0),
    BLINK(School.ARCANE,30,80,true,0), BLESSING(School.RITUAL,40,200,true,0),
    FROST_LANCE(School.ELEMENTAL,30,60,false,16), PURIFICATION(School.NATURE,35,160,false,16),
    MAGELIGHT(School.ARCANE,20,120,false,64), SANCTUARY(School.RITUAL,45,300,false,128);
    public final School school;
    public final int cost, cooldown, bit;
    public final boolean basic;
    Spell(School school,int cost,int cooldown,boolean basic,int bit) { this.school=school;this.cost=cost;this.cooldown=cooldown;this.basic=basic;this.bit=bit; }
    public String id() { return name().toLowerCase(Locale.ROOT); }
    public String descriptionKey() {return "tooltip.reimagined.spell."+id();}
    public String translationKey() { return "spell.reimagined."+id(); }
    public static Spell basic(School school) { for(var spell:values())if(spell.basic && spell.school==school)return spell;throw new IllegalArgumentException(); }
    public static Spell parse(String id) { for(var spell:values())if(spell.id().equals(id))return spell;return null; }
}
