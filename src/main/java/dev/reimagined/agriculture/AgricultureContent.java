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

    public static final DeferredBlock<RyeCropBlock> RYE_CROP = BLOCKS.register("rye_crop",
            () -> new RyeCropBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.WHEAT)));
    public static final DeferredItem<Item> RYE = ITEMS.registerSimpleItem("rye");
    public static final DeferredItem<ItemNameBlockItem> RYE_SEEDS = ITEMS.register("rye_seeds",
            () -> new ItemNameBlockItem(RYE_CROP.get(), new Item.Properties()));

    public static final DeferredBlock<OatsCropBlock> OATS_CROP = BLOCKS.register("oats_crop",
            () -> new OatsCropBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.WHEAT)));
    public static final DeferredItem<Item> OATS = ITEMS.registerSimpleItem("oats");
    public static final DeferredItem<ItemNameBlockItem> OATS_SEEDS = ITEMS.register("oats_seeds",
            () -> new ItemNameBlockItem(OATS_CROP.get(), new Item.Properties()));

    public static final DeferredBlock<CabbageCropBlock> CABBAGE_CROP = BLOCKS.register("cabbage_crop",
            () -> new CabbageCropBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.WHEAT)));
    public static final DeferredItem<Item> CABBAGE = ITEMS.registerSimpleItem("cabbage");
    public static final DeferredItem<ItemNameBlockItem> CABBAGE_SEEDS = ITEMS.register("cabbage_seeds",
            () -> new ItemNameBlockItem(CABBAGE_CROP.get(), new Item.Properties()));

    public static final DeferredBlock<OnionCropBlock> ONION_CROP = BLOCKS.register("onion_crop",
            () -> new OnionCropBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.WHEAT)));
    public static final DeferredItem<Item> ONION = ITEMS.registerSimpleItem("onion");
    public static final DeferredItem<ItemNameBlockItem> ONION_SEEDS = ITEMS.register("onion_seeds",
            () -> new ItemNameBlockItem(ONION_CROP.get(), new Item.Properties()));

    // Decorative farm items
    public static final DeferredItem<Item> HARVEST_BUNDLE = ITEMS.registerSimpleItem("harvest_bundle");
    public static final DeferredItem<Item> GRAIN_SACK = ITEMS.registerSimpleItem("grain_sack");
    public static final DeferredItem<Item> VEGETABLE_CRATE = ITEMS.registerSimpleItem("vegetable_crate");

    private AgricultureContent() {}
    public static void register(IEventBus bus) {
        BLOCKS.register(bus); ITEMS.register(bus);
        bus.addListener((BuildCreativeModeTabContentsEvent event) -> {
            if (event.getTabKey() == CreativeModeTabs.NATURAL_BLOCKS) {
                event.accept(BARLEY_SEEDS); event.accept(RYE_SEEDS); event.accept(OATS_SEEDS);
                event.accept(CABBAGE_SEEDS); event.accept(ONION_SEEDS);
            }
            if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
                event.accept(BARLEY); event.accept(RYE); event.accept(OATS); event.accept(CABBAGE); event.accept(ONION);
                event.accept(HARVEST_BUNDLE); event.accept(GRAIN_SACK); event.accept(VEGETABLE_CRATE);
            }
        });
        bus.addListener((FMLCommonSetupEvent event) -> event.enqueueWork(() -> {
            ComposterBlock.COMPOSTABLES.put(BARLEY_SEEDS.get(), 0.3F);
            ComposterBlock.COMPOSTABLES.put(BARLEY.get(), 0.65F);
            for (var s : new Item[]{RYE_SEEDS.get(), OATS_SEEDS.get(), CABBAGE_SEEDS.get(), ONION_SEEDS.get()}) ComposterBlock.COMPOSTABLES.put(s, 0.3F);
            for (var c : new Item[]{RYE.get(), OATS.get(), CABBAGE.get(), ONION.get()}) ComposterBlock.COMPOSTABLES.put(c, 0.65F);
        }));
    }
}
