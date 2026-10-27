package dev.reimagined.agriculture;

import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.*;

public final class AgricultureContent {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks("reimagined");
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems("reimagined");
    public static final DeferredBlock<BarleyCropBlock> BARLEY_CROP = BLOCKS.register("barley_crop",
            () -> new BarleyCropBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.WHEAT)));
    public static final DeferredItem<Item> BARLEY = ITEMS.registerSimpleItem("barley");
    public static final DeferredItem<ItemNameBlockItem> BARLEY_SEEDS = ITEMS.register("barley_seeds",
            () -> new ItemNameBlockItem(BARLEY_CROP.get(), new Item.Properties()));
    private AgricultureContent() {}
    public static void register(IEventBus bus) {
        BLOCKS.register(bus); ITEMS.register(bus);
        bus.addListener((BuildCreativeModeTabContentsEvent event) -> {
            if (event.getTabKey() == CreativeModeTabs.NATURAL_BLOCKS) event.accept(BARLEY_SEEDS);
            if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) event.accept(BARLEY);
        });
        bus.addListener((FMLCommonSetupEvent event) -> event.enqueueWork(() -> {
            ComposterBlock.COMPOSTABLES.put(BARLEY_SEEDS.get(), 0.3F);
            ComposterBlock.COMPOSTABLES.put(BARLEY.get(), 0.65F);
        }));
    }
}
