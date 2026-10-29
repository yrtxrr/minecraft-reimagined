package dev.reimagined.cooking;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.*;

public final class CookingContent {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks("reimagined");
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems("reimagined");
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, "reimagined");
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, "reimagined");
    private static final DeferredRegister<RecipeType<?>> TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, "reimagined");
    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, "reimagined");
    public static final DeferredBlock<CookingPotBlock> POT = BLOCKS.register("cooking_pot", () ->
            new CookingPotBlock(BlockBehaviour.Properties.of().strength(2.5F).sound(SoundType.METAL).noOcclusion()));
    public static final DeferredItem<BlockItem> POT_ITEM = ITEMS.register("cooking_pot", () -> new BlockItem(POT.get(), new Item.Properties()));
    public static final DeferredItem<Item> BARLEY_PORRIDGE = meal("barley_porridge", 7, .6F);
    public static final DeferredItem<Item> VEGETABLE_STEW = meal("vegetable_stew", 9, .7F);
    public static final DeferredItem<Item> HUNTERS_STEW = meal("hunters_stew", 11, .8F);
    public static final DeferredItem<Item> FISH_CHOWDER = meal("fish_chowder", 10, .8F);
    public static final DeferredItem<Item> MUSHROOM_DUMPLINGS = meal("mushroom_dumplings", 9, .75F);
    public static final DeferredItem<Item> HONEYED_PEAR = meal("honeyed_pear", 9, .85F);
    public static final DeferredItem<Item> HONEY_CAKE = meal("honey_cake", 10, .9F);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CookingPotBlockEntity>> ENTITY = ENTITIES.register("cooking_pot",
            () -> BlockEntityType.Builder.of(CookingPotBlockEntity::new, POT.get()).build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<CookingPotMenu>> MENU = MENUS.register("cooking_pot",
            () -> new MenuType<>(CookingPotMenu::new, FeatureFlags.VANILLA_SET));
    public static final DeferredHolder<RecipeType<?>, RecipeType<CookingRecipe>> COOKING = TYPES.register("cooking", () -> new RecipeType<>() {
        @Override public String toString() { return "reimagined:cooking"; }
    });
    public static final DeferredHolder<RecipeSerializer<?>, CookingRecipe.Serializer> SERIALIZER = SERIALIZERS.register("cooking", CookingRecipe.Serializer::new);
    private static DeferredItem<Item> meal(String id, int nutrition, float saturation) {
        return ITEMS.register(id, () -> new Item(new Item.Properties().stacksTo(16).craftRemainder(Items.BOWL)
                .food(new FoodProperties.Builder().nutrition(nutrition).saturationModifier(saturation).usingConvertsTo(Items.BOWL).build())));
    }
    private CookingContent() {}
    public static void register(IEventBus bus) {
        BLOCKS.register(bus); ITEMS.register(bus); ENTITIES.register(bus); MENUS.register(bus); TYPES.register(bus); SERIALIZERS.register(bus);
        bus.addListener((net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent event) ->
            event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, ENTITY.get(),
                (pot, side) -> side == null ? null : pot.handler(side)));

        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.OnDatapackSyncEvent event) -> CookingPotBlockEntity.recipesReloaded());
    }
}
