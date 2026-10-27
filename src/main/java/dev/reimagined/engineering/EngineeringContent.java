package dev.reimagined.engineering;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class EngineeringContent {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks("reimagined");
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems("reimagined");
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, "reimagined");
    private static final DeferredRegister<RecipeType<?>> TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, "reimagined");
    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, "reimagined");
    public static final DeferredBlock<MechanicalBlock> WATER_WHEEL = block("water_wheel", MechanicalBlock.Kind.WATER_WHEEL);
    public static final DeferredBlock<MechanicalBlock> SHAFT = block("shaft", MechanicalBlock.Kind.SHAFT);
    public static final DeferredBlock<MechanicalBlock> MILLSTONE = block("millstone", MechanicalBlock.Kind.MILLSTONE);
    public static final DeferredItem<Item> FLOUR = ITEMS.registerSimpleItem("flour");
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MachineBlockEntity>> MACHINE = ENTITIES.register("machine",
            () -> BlockEntityType.Builder.of(MachineBlockEntity::new, WATER_WHEEL.get(), MILLSTONE.get()).build(null));
    public static final DeferredHolder<RecipeType<?>, RecipeType<MillingRecipe>> MILLING = TYPES.register("milling", () -> new RecipeType<>() {
        @Override public String toString() { return "reimagined:milling"; }
    });
    public static final DeferredHolder<RecipeSerializer<?>, MillingRecipe.Serializer> MILLING_SERIALIZER = SERIALIZERS.register("milling", MillingRecipe.Serializer::new);
    private EngineeringContent() {}
    private static DeferredBlock<MechanicalBlock> block(String name, MechanicalBlock.Kind kind) {
        var block = BLOCKS.register(name, () -> new MechanicalBlock(BlockBehaviour.Properties.of().strength(2.5F).sound(SoundType.WOOD), kind));
        ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
        return block;
    }
    public static void register(IEventBus bus) {
        BLOCKS.register(bus); ITEMS.register(bus); ENTITIES.register(bus); TYPES.register(bus); SERIALIZERS.register(bus);
        bus.addListener((net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent event) -> {
            if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
                event.accept(WATER_WHEEL); event.accept(SHAFT); event.accept(MILLSTONE);
            }
            if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) event.accept(FLOUR);
        });
    }
}
