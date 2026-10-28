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
    public static final DeferredBlock<CloverBlock> CLOVER = BLOCKS.register("clover",
            () -> new CloverBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.DANDELION)));
    public static final DeferredBlock<Block> THATCH = BLOCKS.register("thatch",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.HAY_BLOCK)));
    public static final DeferredItem<BlockItem> CLOVER_ITEM = ITEMS.register("clover",
            () -> new BlockItem(CLOVER.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> THATCH_ITEM = ITEMS.register("thatch",
            () -> new BlockItem(THATCH.get(), new Item.Properties()));
    private AgricultureContent() {}
    public static void register(IEventBus bus) {
        BLOCKS.register(bus); ITEMS.register(bus);
        bus.addListener((BuildCreativeModeTabContentsEvent event) -> {
            if (event.getTabKey() == CreativeModeTabs.NATURAL_BLOCKS) { event.accept(BARLEY_SEEDS); event.accept(CLOVER_ITEM); }
            if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) event.accept(BARLEY);
            if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) event.accept(THATCH_ITEM);
        });
        bus.addListener((FMLCommonSetupEvent event) -> event.enqueueWork(() -> {
            ComposterBlock.COMPOSTABLES.put(BARLEY_SEEDS.get(), 0.3F);
            ComposterBlock.COMPOSTABLES.put(BARLEY.get(), 0.65F);
            ComposterBlock.COMPOSTABLES.put(THATCH.get(), 0.85F);
            ComposterBlock.COMPOSTABLES.put(CLOVER.get(), 0.3F);
        }));
    }
}
