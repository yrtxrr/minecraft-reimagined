package dev.reimagined.beekeeping;

import com.mojang.serialization.Codec;
import dev.reimagined.Reimagined;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Beekeeping builds on vanilla bees, bee nests and hives instead of adding a second hive block.
 * Hive frames are a small persisted per-hive upgrade; the smoker is a portable alternative to a
 * campfire. No block entity, ticker, world scan or saved-data index is added by this module.
 */
public final class BeekeepingContent {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Reimagined.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Reimagined.MOD_ID);
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Reimagined.MOD_ID);

    public static final DeferredBlock<Block> BEESWAX_BLOCK = BLOCKS.register("beeswax_block",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE)
                    .strength(1.0F).sound(SoundType.HONEY_BLOCK)));
    public static final DeferredItem<BlockItem> BEESWAX_BLOCK_ITEM = ITEMS.register("beeswax_block",
            () -> new BlockItem(BEESWAX_BLOCK.get(), new Item.Properties()));
    public static final DeferredItem<Item> BEESWAX = ITEMS.registerSimpleItem("beeswax");
    public static final DeferredItem<BeeFrameItem> BEE_FRAME = ITEMS.register("bee_frame",
            () -> new BeeFrameItem(new Item.Properties()));
    public static final DeferredItem<BeeSmokerItem> BEE_SMOKER = ITEMS.register("bee_smoker",
            () -> new BeeSmokerItem(new Item.Properties().durability(96).stacksTo(1)));

    /**
     * Frames live on the vanilla {@code BeehiveBlockEntity} through a NeoForge data attachment.
     * The attachment serializer is part of the block entity's normal NBT, so no extra file,
     * chunk index or block entity ticker is needed. Values above the cap are clamped on read.
     */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Integer>> HIVE_FRAMES =
            ATTACHMENTS.register("hive_frames", () -> AttachmentType.<Integer>builder(() -> 0)
                    .serialize(Codec.INT, value -> value > 0).build());

    private BeekeepingContent() {}

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        ATTACHMENTS.register(bus);
    }
}
