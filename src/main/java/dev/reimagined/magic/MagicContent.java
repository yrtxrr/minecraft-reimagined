package dev.reimagined.magic;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.*;

public final class MagicContent {
    private static final DeferredRegister.Blocks BLOCKS=DeferredRegister.createBlocks("reimagined");
    private static final DeferredRegister.Items ITEMS=DeferredRegister.createItems("reimagined");
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES=DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE,"reimagined");
    private static final DeferredRegister<MenuType<?>> MENUS=DeferredRegister.create(Registries.MENU,"reimagined");
    private static final DeferredRegister<RecipeType<?>> TYPES=DeferredRegister.create(Registries.RECIPE_TYPE,"reimagined");
    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS=DeferredRegister.create(Registries.RECIPE_SERIALIZER,"reimagined");
    public static final DeferredBlock<MagicStationBlock> RESEARCH_TABLE=block("research_table",true);
    public static final DeferredBlock<MagicStationBlock> ARCANE_ALTAR=block("arcane_altar",false);
    public static final DeferredItem<Item> EMBER_FOCUS=ITEMS.registerSimpleItem("ember_focus");
    public static final DeferredItem<Item> VERDANT_FOCUS=ITEMS.registerSimpleItem("verdant_focus");
    public static final DeferredItem<Item> SPATIAL_FOCUS=ITEMS.registerSimpleItem("spatial_focus");
    public static final DeferredItem<Item> WARD_FOCUS=ITEMS.registerSimpleItem("ward_focus");
    public static final DeferredItem<SpellFolio> FROST_FOLIO=folio("frost_lance_folio",Spell.FROST_LANCE);
    public static final DeferredItem<SpellFolio> PURIFICATION_FOLIO=folio("purification_folio",Spell.PURIFICATION);
    public static final DeferredItem<SpellFolio> MAGELIGHT_FOLIO=folio("magelight_folio",Spell.MAGELIGHT);
    public static final DeferredItem<SpellFolio> SANCTUARY_FOLIO=folio("sanctuary_folio",Spell.SANCTUARY);
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<MagicStationBlockEntity>> STATION=ENTITIES.register("magic_station",
            ()->BlockEntityType.Builder.of(MagicStationBlockEntity::new,RESEARCH_TABLE.get(),ARCANE_ALTAR.get()).build(null));
    public static final DeferredHolder<MenuType<?>,MenuType<MagicStationMenu>> RESEARCH_MENU=MENUS.register("research_table",
            ()->new MenuType<>((id,inventory)->new MagicStationMenu(id,inventory,true),FeatureFlags.VANILLA_SET));
    public static final DeferredHolder<MenuType<?>,MenuType<MagicStationMenu>> ALTAR_MENU=MENUS.register("arcane_altar",
            ()->new MenuType<>((id,inventory)->new MagicStationMenu(id,inventory,false),FeatureFlags.VANILLA_SET));
    public static final DeferredHolder<RecipeType<?>,RecipeType<MagicRecipe>> RESEARCH=type("spell_research");
    public static final DeferredHolder<RecipeType<?>,RecipeType<MagicRecipe>> INFUSION=type("arcane_infusion");
    public static final DeferredHolder<RecipeSerializer<?>,MagicRecipe.Serializer> RESEARCH_SERIALIZER=SERIALIZERS.register("spell_research",()->new MagicRecipe.Serializer(true));
    public static final DeferredHolder<RecipeSerializer<?>,MagicRecipe.Serializer> INFUSION_SERIALIZER=SERIALIZERS.register("arcane_infusion",()->new MagicRecipe.Serializer(false));
    private MagicContent() {}
    private static DeferredBlock<MagicStationBlock> block(String id,boolean research) {
        var block=BLOCKS.register(id,()->new MagicStationBlock(BlockBehaviour.Properties.of().strength(2.5F).sound(research?SoundType.WOOD:SoundType.STONE).noOcclusion(),research));
        ITEMS.register(id,()->new BlockItem(block.get(),new Item.Properties()));return block;
    }
    private static DeferredItem<SpellFolio> folio(String id,Spell spell) { return ITEMS.register(id,()->new SpellFolio(new Item.Properties().stacksTo(16),spell)); }
    private static DeferredHolder<RecipeType<?>,RecipeType<MagicRecipe>> type(String id) { return TYPES.register(id,()->new RecipeType<>() { @Override public String toString() {return "reimagined:"+id;} }); }
    public static void register(IEventBus bus) {
        BLOCKS.register(bus);ITEMS.register(bus);ENTITIES.register(bus);MENUS.register(bus);TYPES.register(bus);SERIALIZERS.register(bus);

        bus.addListener((net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent event)->event.registrar("1").playToClient(
                MagicProgressPayload.TYPE,MagicProgressPayload.CODEC,(payload,context)->MagicProgressPayload.receive(payload)));
        var game=net.neoforged.neoforge.common.NeoForge.EVENT_BUS;
        game.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event)-> {if(event.getEntity() instanceof net.minecraft.server.level.ServerPlayer p)MagicProgressPayload.sync(p);});
        game.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerRespawnEvent event)-> {if(event.getEntity() instanceof net.minecraft.server.level.ServerPlayer p)MagicProgressPayload.sync(p);});
        game.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.Clone event)->SpellKnowledge.copy(event.getOriginal(),event.getEntity()));
    }
}
