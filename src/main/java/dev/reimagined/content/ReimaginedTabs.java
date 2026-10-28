package dev.reimagined.content;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Ordered, data-driven catalogues. Future content joins an item tag, without new UI hooks. */
public final class ReimaginedTabs {
    private static final DeferredRegister<CreativeModeTab> TABS=DeferredRegister.create(Registries.CREATIVE_MODE_TAB,"reimagined");
    public static final java.util.List<String> CATEGORIES=java.util.List.of("nature","food","engineering","magic");
    static {
        String[] icons={"pear_sapling","barley","water_wheel","arcane_wand"};
        ResourceKey<CreativeModeTab> previous=CreativeModeTabs.SPAWN_EGGS;
        for(int i=0;i<CATEGORIES.size();i++) {
            String category=CATEGORIES.get(i), icon=icons[i];
            ResourceKey<CreativeModeTab> before=previous;
            TABS.register(category,()->CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.reimagined."+category))
                    .icon(()->new ItemStack(BuiltInRegistries.ITEM.get(id(icon))))
                    .withTabsBefore(before)
                    .displayItems((parameters,output)->BuiltInRegistries.ITEM.getTag(tag(category))
                            .ifPresent(items->items.forEach(item->output.accept(item.value())))).build());
            previous=ResourceKey.create(Registries.CREATIVE_MODE_TAB,id(category));
        }
    }
    public static TagKey<Item> tag(String category) { return TagKey.create(Registries.ITEM,id("creative/"+category)); }
    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("reimagined",path); }
    public static void register(IEventBus bus) { TABS.register(bus); }
    private ReimaginedTabs() {}
}
