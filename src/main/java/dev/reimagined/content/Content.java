package dev.reimagined.content;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.world.item.CreativeModeTabs;

public final class Content {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems("reimagined");
    public static final DeferredItem<Item> ELEMENTAL_WAND = wand("elemental_wand", SpellWand.School.ELEMENTAL);
    public static final DeferredItem<Item> NATURE_WAND = wand("nature_wand", SpellWand.School.NATURE);
    public static final DeferredItem<Item> ARCANE_WAND = wand("arcane_wand", SpellWand.School.ARCANE);
    public static final DeferredItem<Item> RITUAL_WAND = wand("ritual_wand", SpellWand.School.RITUAL);
    public static final DeferredItem<Item> RESEARCH_JOURNAL = ITEMS.register("research_journal", () -> new ResearchJournal(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<SwordItem> SPEAR = weapon("iron_spear", 4, -2.6F, SpecialWeapon.Technique.SPEAR);
    public static final DeferredItem<SwordItem> HAMMER = weapon("iron_hammer", 7, -3.4F, SpecialWeapon.Technique.HAMMER);
    public static final DeferredItem<SwordItem> DAGGER = weapon("iron_dagger", 1, -1.5F, SpecialWeapon.Technique.DAGGER);
    public static final DeferredItem<SwordItem> HALBERD = weapon("iron_halberd", 6, -3.0F, SpecialWeapon.Technique.HALBERD);
    private Content() {}
    private static DeferredItem<Item> wand(String id, SpellWand.School school) {
        return ITEMS.register(id, () -> new SpellWand(new Item.Properties().stacksTo(1), school));
    }
    private static DeferredItem<SwordItem> weapon(String id, int damage, float speed, SpecialWeapon.Technique technique) {
        return ITEMS.register(id, () -> new SpecialWeapon(
                new Item.Properties().attributes(SwordItem.createAttributes(Tiers.IRON, damage, speed)), technique));
    }
    public static void register(IEventBus bus) {
        ITEMS.register(bus);
        CombatEvents.register();
        bus.addListener(Content::creative);
        NeoForge.EVENT_BUS.addListener(Content::tick);
        NeoForge.EVENT_BUS.addListener(Content::clonePlayer);
    }
    private static void creative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            event.accept(RESEARCH_JOURNAL);
            event.accept(SPEAR); event.accept(HAMMER); event.accept(DAGGER); event.accept(HALBERD);
            event.accept(ELEMENTAL_WAND); event.accept(NATURE_WAND); event.accept(ARCANE_WAND); event.accept(RITUAL_WAND);
        }
    }
    private static void clonePlayer(net.neoforged.neoforge.event.entity.player.PlayerEvent.Clone event) {
        SpellWand.copyState(event.getOriginal(), event.getEntity());
        CombatState.copyState(event.getOriginal(), event.getEntity());
    }
    private static void tick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player && player.tickCount % 20 == 0)
            SpellWand.regenerate(player);
    }
}
