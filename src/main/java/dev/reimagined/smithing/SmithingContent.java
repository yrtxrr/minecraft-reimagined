package dev.reimagined.smithing;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.*;

public final class SmithingContent {
    private static final DeferredRegister.Blocks BLOCKS=DeferredRegister.createBlocks("reimagined");
    private static final DeferredRegister.Items ITEMS=DeferredRegister.createItems("reimagined");
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES=DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE,"reimagined");
    private static final DeferredRegister<MenuType<?>> MENUS=DeferredRegister.create(Registries.MENU,"reimagined");
    private static final DeferredRegister<RecipeType<?>> TYPES=DeferredRegister.create(Registries.RECIPE_TYPE,"reimagined");
    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS=DeferredRegister.create(Registries.RECIPE_SERIALIZER,"reimagined");
    public static final TagKey<Item> HAMMERS=TagKey.create(Registries.ITEM,ResourceLocation.parse("reimagined:smithing_hammers"));
    public static final DeferredBlock<ForgeBlock> FORGE=BLOCKS.register("smithing_forge",()->new ForgeBlock(BlockBehaviour.Properties.of().strength(3).sound(SoundType.METAL).noOcclusion()));
    public static final DeferredItem<BlockItem> FORGE_ITEM=ITEMS.register("smithing_forge",()->new BlockItem(FORGE.get(),new Item.Properties()) {
        @Override public void appendHoverText(ItemStack stack,Item.TooltipContext context,java.util.List<net.minecraft.network.chat.Component> tooltip,TooltipFlag flag) {
            tooltip.add(net.minecraft.network.chat.Component.translatable("reimagined.forge.guide"));
        }
    });
    public static final DeferredItem<Item> CARBON_IRON=ITEMS.registerSimpleItem("carbon_iron_blend");
    public static final DeferredItem<Item> STEEL=ITEMS.registerSimpleItem("steel_ingot");
    public static final DeferredItem<Item> BELLOWS=ITEMS.registerSimpleItem("smithing_bellows",new Item.Properties().stacksTo(1));
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<ForgeBlockEntity>> ENTITY=ENTITIES.register("smithing_forge",()->BlockEntityType.Builder.of(ForgeBlockEntity::new,FORGE.get()).build(null));
    public static final DeferredHolder<MenuType<?>,MenuType<ForgeMenu>> MENU=MENUS.register("smithing_forge",()->new MenuType<>(ForgeMenu::new,FeatureFlags.VANILLA_SET));
    public static final DeferredHolder<RecipeType<?>,RecipeType<ForgingRecipe>> FORGING=TYPES.register("forging",()->new RecipeType<>() {@Override public String toString(){return "reimagined:forging";}});
    public static final DeferredHolder<RecipeSerializer<?>,ForgingRecipe.Serializer> SERIALIZER=SERIALIZERS.register("forging",ForgingRecipe.Serializer::new);
    private SmithingContent() {}
    public static void register(IEventBus bus) {
        BLOCKS.register(bus);ITEMS.register(bus);ENTITIES.register(bus);MENUS.register(bus);TYPES.register(bus);SERIALIZERS.register(bus);
        bus.addListener((net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent event)-> {
            if(event.getTabKey()==CreativeModeTabs.FUNCTIONAL_BLOCKS){event.accept(FORGE_ITEM);event.accept(BELLOWS);}
            if(event.getTabKey()==CreativeModeTabs.INGREDIENTS){event.accept(CARBON_IRON);event.accept(STEEL);}
        });
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.entity.player.ItemTooltipEvent event)-> {
            if(!event.getItemStack().getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.EMPTY).contains(ForgedEquipment.KEY))return;
            var data=ForgedEquipment.data(event.getItemStack());int quality=data.getInt("Quality");
            if(data.getInt("Version")==1 && quality>=1 && quality<=3)event.getToolTip().add(net.minecraft.network.chat.Component.translatable("reimagined.forge.forged",
                    net.minecraft.network.chat.Component.translatable("reimagined.forge.quality."+quality),
                    net.minecraft.network.chat.Component.translatable("reimagined.forge.finish."+data.getString("Finish"))));
        });
    }
}
