package dev.reimagined.fishing;

import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Simple edible fish obtained through the vanilla fishing loot table. */
public final class FishContent {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems("reimagined");

    public static final DeferredItem<Item> RIVER_PERCH = fish("river_perch", 2, 0.2F);
    public static final DeferredItem<Item> SILVER_TROUT = fish("silver_trout", 3, 0.3F);
    public static final DeferredItem<Item> BLUE_MACKEREL = fish("blue_mackerel", 2, 0.2F);

    private static DeferredItem<Item> fish(String id, int nutrition, float saturation) {
        return ITEMS.register(id, () -> new Item(new Item.Properties().food(new FoodProperties.Builder()
                .nutrition(nutrition).saturationModifier(saturation).build())));
    }

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }



    private FishContent() {}
}
