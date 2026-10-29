package dev.reimagined.settlements;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.flag.FeatureFlags;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class SettlementContent {
    private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, "reimagined");
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems("reimagined");
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, "reimagined");
    public static final DeferredHolder<EntityType<?>, EntityType<Settler>> SETTLER = ENTITIES.register("settler",
            () -> EntityType.Builder.of(Settler::new, MobCategory.MISC).sized(0.6f, 1.8f).clientTrackingRange(10).build("reimagined:settler"));
    public static final DeferredItem<DeferredSpawnEggItem> SETTLER_SPAWN_EGG = ITEMS.register("settler_spawn_egg",
            () -> new DeferredSpawnEggItem(SETTLER, 0x7F5F3F, 0xD4B88F, new Item.Properties()));
    public static final DeferredHolder<MenuType<?>, MenuType<TradingMenu>> TRADING_MENU = MENUS.register("trading",
            () -> new MenuType<>(TradingMenu::new, FeatureFlags.VANILLA_SET));

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
        ITEMS.register(bus);
        MENUS.register(bus);
        bus.addListener(SettlementContent::attributes);
        bus.addListener(SettlementContent::creativeTab);
        SettlementEvents.register();
    }

    private static void attributes(EntityAttributeCreationEvent event) {
        event.put(SETTLER.get(), Settler.attributes().build());
    }

    private static void creativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) { event.accept(SETTLER_SPAWN_EGG); }
    }

    private SettlementContent() {}
}
