package dev.reimagined.agriculture;

import dev.reimagined.Reimagined;
import java.util.Optional;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Pear orchard: a real tree feature, seasonal fruit, sapling propagation and a small wood set.
 * The tree uses the ordinary vanilla feature/leaf rules; no block entity or world scan is added.
 */
public final class OrchardContent {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Reimagined.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Reimagined.MOD_ID);

    public static final ResourceKey<ConfiguredFeature<?, ?>> PEAR_TREE = ResourceKey.create(
            Registries.CONFIGURED_FEATURE,
            ResourceLocation.fromNamespaceAndPath(Reimagined.MOD_ID, "pear_tree"));
    /** Registered before any sapling is constructed; TreeGrower's codec resolves by this stable name. */
    public static final TreeGrower PEAR_GROWER = new TreeGrower(
            "reimagined:pear", Optional.empty(), Optional.of(PEAR_TREE), Optional.empty());

    public static final DeferredBlock<PearLeavesBlock> PEAR_LEAVES = BLOCKS.register("pear_leaves",
            () -> new PearLeavesBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LEAVES)));
    public static final DeferredBlock<PearSaplingBlock> PEAR_SAPLING = BLOCKS.register("pear_sapling",
            () -> new PearSaplingBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SAPLING)));
    public static final DeferredBlock<RotatedPillarBlock> PEAR_LOG = BLOCKS.register("pear_log",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LOG)));
    public static final DeferredBlock<RotatedPillarBlock> PEAR_WOOD = BLOCKS.register("pear_wood",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_WOOD)));
    public static final DeferredBlock<Block> PEAR_PLANKS = BLOCKS.register("pear_planks",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS)));

    public static final DeferredItem<BlockItem> PEAR_LEAVES_ITEM = ITEMS.register("pear_leaves",
            () -> new BlockItem(PEAR_LEAVES.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> PEAR_SAPLING_ITEM = ITEMS.register("pear_sapling",
            () -> new BlockItem(PEAR_SAPLING.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> PEAR_LOG_ITEM = ITEMS.register("pear_log",
            () -> new BlockItem(PEAR_LOG.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> PEAR_WOOD_ITEM = ITEMS.register("pear_wood",
            () -> new BlockItem(PEAR_WOOD.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> PEAR_PLANKS_ITEM = ITEMS.register("pear_planks",
            () -> new BlockItem(PEAR_PLANKS.get(), new Item.Properties()));
    public static final DeferredItem<Item> PEAR = ITEMS.registerSimpleItem("pear",
            new Item.Properties().food(new FoodProperties.Builder().nutrition(4).saturationModifier(0.3F).build()));

    private OrchardContent() {}

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);

        bus.addListener((FMLCommonSetupEvent event) -> event.enqueueWork(() -> {
            net.minecraft.world.level.block.ComposterBlock.COMPOSTABLES.put(PEAR_SAPLING_ITEM.get(), 0.3F);
            net.minecraft.world.level.block.ComposterBlock.COMPOSTABLES.put(PEAR_LEAVES_ITEM.get(), 0.3F);
            net.minecraft.world.level.block.ComposterBlock.COMPOSTABLES.put(PEAR.get(), 0.65F);
        }));
    }
}
