package dev.reimagined.gametest;

import com.mojang.authlib.GameProfile;
import dev.reimagined.magic.*;
import dev.reimagined.content.*;
import dev.reimagined.research.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("reimagined")
@PrefixGameTestTemplate(false)
public final class MagicGameTests {
    private static final BlockPos POS=new BlockPos(4,3,4);
    private static FakePlayer player(GameTestHelper h) {
        var p=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"MagicTest"));p.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        p.setPos(h.absolutePos(POS.south()).getCenter());return p;
    }
    private static MagicStationBlockEntity station(GameTestHelper h,boolean research) {
        h.setBlock(POS,research?MagicContent.RESEARCH_TABLE.get():MagicContent.ARCANE_ALTAR.get());
        return (MagicStationBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(POS));
    }
    private static MagicStationMenu open(FakePlayer player,MagicStationBlockEntity entity) {
        var menu=new MagicStationMenu(1,player.getInventory(),entity,entity.research());player.containerMenu=menu;return menu;
    }
    private static void observations(FakePlayer player) {
        new Discoveries(List.of(ResourceLocation.parse("minecraft:plains"),ResourceLocation.parse("minecraft:desert")),List.of(ResourceLocation.parse("minecraft:cow"))).save(player);
    }
    private static void inputs(MagicStationBlockEntity entity,Item first,Item second,Item third) {
        entity.setItem(0,new ItemStack(first));entity.setItem(1,new ItemStack(second));entity.setItem(2,new ItemStack(third));
    }
    @GameTest(template="empty")
    public static void researchRequiresDiscoveriesAndConsumesMaterialsAndManaExactlyOnce(GameTestHelper h) {
        var entity=station(h,true);var player=player(h);inputs(entity,MagicContent.SPATIAL_FOCUS.get(),Items.PAPER,Items.LAPIS_LAZULI);
        var menu=open(player,entity);
        h.assertTrue(!menu.clickMenuButton(player,0) && entity.getItem(0).getCount()==1 && SpellWand.mana(player)==100,"Unobserved researcher cannot spend resources or get a folio");
        observations(player);menu=open(player,entity);
        h.assertTrue(menu.clickMenuButton(player,0),"Journal discoveries must enable actual research");
        h.assertTrue(entity.getItem(3).is(MagicContent.MAGELIGHT_FOLIO.get()) && entity.getItem(0).isEmpty() && entity.getItem(1).isEmpty() && entity.getItem(2).isEmpty() && SpellWand.mana(player)==80,"One transaction consumes three ingredients and20mana");
        h.assertTrue(!menu.clickMenuButton(player,0) && entity.getItem(3).getCount()==1 && SpellWand.mana(player)==80,"Repeated request must not duplicate output or spend twice");h.succeed();
    }
    @GameTest(template="empty")
    public static void altarMakesRealFocusAndConcurrentResearchersCannotDoubleSpend(GameTestHelper h) {
        var entity=station(h,false);inputs(entity,Items.AMETHYST_SHARD,dev.reimagined.agriculture.AgricultureContent.BARLEY.get(),Items.BONE_MEAL);
        var a=player(h);var b=player(h);var ma=open(a,entity);var mb=open(b,entity);
        h.assertTrue(ma.clickMenuButton(a,0) && !mb.clickMenuButton(b,0),"Shared station must serialize actual inventory transactions");
        h.assertTrue(entity.getItem(3).is(MagicContent.VERDANT_FOCUS.get()) && entity.getItem(3).getCount()==1 && SpellWand.mana(a)==85 && SpellWand.mana(b)==100,"Only successful crafter pays and receives one focus");h.succeed();
    }
    @GameTest(template="empty")
    public static void menuRejectsRemoteSpoofedLowManaAndFullOutputRequests(GameTestHelper h) {
        var entity=station(h,false);inputs(entity,Items.AMETHYST_SHARD,Items.ENDER_PEARL,Items.LAPIS_LAZULI);var p=player(h);var menu=open(p,entity);
        p.getPersistentData().putInt("reimagined_mana",0);
        h.assertTrue(!menu.clickMenuButton(p,0) && entity.getItem(0).getCount()==1,"Insufficient mana cannot consume ingredients");
        p.getPersistentData().putInt("reimagined_mana",100);menu=open(p,entity);entity.setItem(3,new ItemStack(Items.STONE,64));
        h.assertTrue(!menu.clickMenuButton(p,0) && SpellWand.mana(p)==100,"Incompatible/full output cannot consume mana");
        entity.setItem(3,ItemStack.EMPTY);menu=open(p,entity);
        h.assertTrue(!menu.clickMenuButton(p,99) && !menu.getSlot(3).mayPlace(new ItemStack(Items.DIRT)),"Only supported button and protected output are accepted");
        p.setPos(h.absolutePos(POS.offset(20,0,0)).getCenter());
        h.assertTrue(!menu.clickMenuButton(p,0) && entity.getItem(0).getCount()==1,"Distant access must fail closed");h.succeed();
    }
    @GameTest(template="empty")
    public static void foliosTeachOnceAndKnowledgeSurvivesSaveDeathCloneAndIsolation(GameTestHelper h) {
        var p=player(h);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(MagicContent.MAGELIGHT_FOLIO.get(),2));
        var item=MagicContent.MAGELIGHT_FOLIO.get();
        h.assertTrue(item.use(h.getLevel(),p,InteractionHand.MAIN_HAND).getResult().consumesAction() && SpellKnowledge.knows(p,Spell.MAGELIGHT) && p.getMainHandItem().getCount()==1,"Actual folio use learns and consumes once");
        h.assertTrue(item.use(h.getLevel(),p,InteractionHand.MAIN_HAND).getResult()==InteractionResult.FAIL && p.getMainHandItem().getCount()==1,"Known folios cannot be wasted or farm repeated XP");
        var saved=p.saveWithoutId(new CompoundTag());var restored=player(h);restored.load(saved);
        var clone=player(h);NeoForge.EVENT_BUS.post(new PlayerEvent.Clone(clone,p,true));
        h.assertTrue(SpellKnowledge.knows(restored,Spell.MAGELIGHT) && SpellKnowledge.knows(clone,Spell.MAGELIGHT) && !SpellKnowledge.knows(player(h),Spell.MAGELIGHT),"Save/clone preserve personal knowledge without sharing it with other players");
        p.getPersistentData().getCompound(SpellKnowledge.KEY).putInt("Known",0);
        h.assertTrue(SpellKnowledge.knows(clone,Spell.MAGELIGHT),"Death clone must deep-copy knowledge");h.succeed();
    }
    @GameTest(template="empty")
    public static void advancedWandSelectionIsSavedAndUnknownSpellsCannotBeForged(GameTestHelper h) {
        var p=player(h);var item=(SpellWand)Content.ARCANE_WAND.get();var stack=new ItemStack(item);p.setItemInHand(InteractionHand.MAIN_HAND,stack);
        item.select(stack,Spell.MAGELIGHT);
        h.assertTrue(item.use(h.getLevel(),p,InteractionHand.MAIN_HAND).getResult()==InteractionResult.FAIL && SpellWand.mana(p)==100 && !p.hasEffect(MobEffects.NIGHT_VISION),"Forged selected spell cannot bypass personal unlocks");
        SpellKnowledge.learn(p,Spell.MAGELIGHT);p.setShiftKeyDown(true);
        item.use(h.getLevel(),p,InteractionHand.MAIN_HAND);h.assertTrue(item.selected(stack)==Spell.BLINK && SpellWand.mana(p)==100,"Sneak selection must cycle known spells without spending mana");
        item.use(h.getLevel(),p,InteractionHand.MAIN_HAND);p.setShiftKeyDown(false);
        h.assertTrue(item.selected(stack)==Spell.MAGELIGHT,"Second selection returns learned advanced spell");
        var encoded=stack.save(h.getLevel().registryAccess());var decoded=ItemStack.parse(h.getLevel().registryAccess(),encoded).orElseThrow();
        h.assertTrue(item.selected(decoded)==Spell.MAGELIGHT,"Selected spell must survive item NBT/component transport");
        h.assertTrue(item.use(h.getLevel(),p,InteractionHand.MAIN_HAND).getResult().consumesAction() && p.hasEffect(MobEffects.NIGHT_VISION) && p.hasEffect(MobEffects.SLOW_FALLING) && SpellWand.mana(p)==80,"Magelight must apply real bounded effects and cost");h.succeed();
    }
    @GameTest(template="empty")
    public static void purificationAndSanctuaryApplyRealEffectsAndRespectRitualResources(GameTestHelper h) {
        var p=player(h);SpellKnowledge.learn(p,Spell.PURIFICATION);var nature=(SpellWand)Content.NATURE_WAND.get();var stack=new ItemStack(nature);nature.select(stack,Spell.PURIFICATION);p.setItemInHand(InteractionHand.MAIN_HAND,stack);
        p.addEffect(new MobEffectInstance(MobEffects.POISON,200));p.addEffect(new MobEffectInstance(MobEffects.WITHER,200));
        h.assertTrue(nature.use(h.getLevel(),p,InteractionHand.MAIN_HAND).getResult().consumesAction() && !p.hasEffect(MobEffects.POISON) && !p.hasEffect(MobEffects.WITHER) && p.hasEffect(MobEffects.REGENERATION),"Purification must cleanse and regenerate");
        var ritual=(SpellWand)Content.RITUAL_WAND.get();var ward=new ItemStack(ritual);ritual.select(ward,Spell.SANCTUARY);SpellKnowledge.learn(p,Spell.SANCTUARY);p.setItemInHand(InteractionHand.MAIN_HAND,ward);
        h.assertTrue(ritual.use(h.getLevel(),p,InteractionHand.MAIN_HAND).getResult()==InteractionResult.FAIL && SpellWand.mana(p)==65,"Missing ritual shard must not consume mana");
        p.getInventory().setItem(1,new ItemStack(Items.AMETHYST_SHARD));
        h.assertTrue(ritual.use(h.getLevel(),p,InteractionHand.MAIN_HAND).getResult().consumesAction() && p.hasEffect(MobEffects.FIRE_RESISTANCE) && p.hasEffect(MobEffects.ABSORPTION) && p.getInventory().getItem(1).isEmpty() && SpellWand.mana(p)==20,"Sanctuary consumes one shard and45mana for actual wards");h.succeed();
    }
    @GameTest(template="empty")
    public static void frostLanceHitsAVisibleTargetButNotThroughWalls(GameTestHelper h) {
        var p=player(h);p.setPos(h.absolutePos(new BlockPos(4,3,1)).getCenter());p.setYRot(0);p.setXRot(0);
        var cow=EntityType.COW.create(h.getLevel());cow.setNoAi(true);cow.setPos(h.absolutePos(new BlockPos(4,3,5)).getCenter());h.getLevel().addFreshEntity(cow);
        var item=(SpellWand)Content.ELEMENTAL_WAND.get();var stack=new ItemStack(item);item.select(stack,Spell.FROST_LANCE);SpellKnowledge.learn(p,Spell.FROST_LANCE);p.setItemInHand(InteractionHand.MAIN_HAND,stack);
        item.use(h.getLevel(),p,InteractionHand.MAIN_HAND);
        h.assertTrue(cow.getHealth()==6 && cow.hasEffect(MobEffects.MOVEMENT_SLOWDOWN) && SpellWand.mana(p)==70,"Frost lance must damage and slow a visible target");
        var other=player(h);other.setPos(p.position());other.setYRot(0);other.setXRot(0);SpellKnowledge.learn(other,Spell.FROST_LANCE);other.setItemInHand(InteractionHand.MAIN_HAND,stack.copy());
        h.setBlock(new BlockPos(4,4,3),Blocks.STONE);h.setBlock(new BlockPos(4,5,3),Blocks.STONE);
        item.use(h.getLevel(),other,InteractionHand.MAIN_HAND);
        h.assertTrue(cow.getHealth()==6,"Opaque wall must block frost targeting");h.succeed();
    }
    @GameTest(template="empty")
    public static void stationInventoryRoundtripAndUnloadedQueriesHaveNoBackgroundWork(GameTestHelper h) {
        var entity=station(h,true);entity.setItem(0,new ItemStack(MagicContent.WARD_FOCUS.get(),2));entity.setItem(3,new ItemStack(MagicContent.SANCTUARY_FOLIO.get()));
        var loaded=BlockEntity.loadStatic(entity.getBlockPos(),entity.getBlockState(),entity.saveWithFullMetadata(h.getLevel().registryAccess()),h.getLevel().registryAccess());
        h.assertTrue(loaded instanceof MagicStationBlockEntity copy && copy.getItem(0).getCount()==2 && copy.getItem(3).is(MagicContent.SANCTUARY_FOLIO.get()),"Station inventory must survive real NBT codec");
        h.assertTrue(MagicContent.RESEARCH_TABLE.get().getTicker(h.getLevel(),entity.getBlockState(),MagicContent.STATION.get())==null,"Station must not have any BE ticker");
        var absent=new BlockPos(16000000,80,16000000);int chunks=h.getLevel().getChunkSource().getLoadedChunksCount();
        h.assertTrue(MagicStationBlock.loaded(h.getLevel(),absent)==null && new SpellWorldAccess(h.getLevel()).getBlockState(absent).is(Blocks.BARRIER),"Station/ray inspection must fail closed outside ready chunks");
        h.assertTrue(h.getLevel().getChunkSource().getLoadedChunksCount()==chunks,"Inspection must not generate/load chunks");h.succeed();
    }
    @GameTest(template="empty")
    public static void magicRecipesBrowserAndCodecsExposeBothStationsAndRequirements(GameTestHelper h) {
        var level=h.getLevel();var recipes=level.getRecipeManager().getRecipes().stream().filter(r->r.value() instanceof MagicRecipe).toList();
        h.assertTrue(recipes.size()==8,"Four infusions and four research recipes must actually load");
        for(var holder:recipes) {
            var recipe=(MagicRecipe)holder.value();var serializer=(MagicRecipe.Serializer)recipe.getSerializer();
            var buffer=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),level.registryAccess());
            try {serializer.streamCodec().encode(buffer,recipe);var decoded=serializer.streamCodec().decode(buffer);h.assertTrue(decoded.mana()==recipe.mana() && decoded.research()==recipe.research() && ItemStack.isSameItemSameComponents(decoded.result(),recipe.result()),"Recipe network transport preserves process and result");}finally{buffer.release();}
            var ops=level.registryAccess().createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE);
            var json=serializer.codec().codec().encodeStart(ops,recipe).getOrThrow();h.assertTrue(serializer.codec().codec().parse(ops,json).isSuccess(),"Registry codec must roundtrip actual magic recipes");
        }
        var displays=dev.reimagined.browser.BrowserRecipes.recipeDisplays(level.getRecipeManager(),level.registryAccess(),ItemStack.EMPTY,List.of());
        h.assertTrue(displays.stream().filter(d->d.station().is(MagicContent.RESEARCH_TABLE.get().asItem())).count()==4 && displays.stream().filter(d->d.station().is(MagicContent.ARCANE_ALTAR.get().asItem())).count()==4,"Browser must show both process stations");
        h.assertTrue(displays.stream().anyMatch(d->d.uses(new ItemStack(dev.reimagined.agriculture.AgricultureContent.BARLEY.get())) && d.result().is(MagicContent.VERDANT_FOCUS.get())),"Existing barley must connect to nature research uses");h.succeed();
    }
    @GameTest(template="empty")
    public static void journalAndProgressPayloadReflectOnlyTheReadersFiniteKnowledge(GameTestHelper h) {
        var p=player(h);SpellKnowledge.learn(p,Spell.SANCTUARY);
        var book=JournalPages.build(p);String json=book.pages().stream()
                .map(page->net.minecraft.network.chat.Component.Serializer.toJson(page.raw(),h.getLevel().registryAccess()))
                .filter(text->text.contains("spell_progress")).collect(java.util.stream.Collectors.joining());
        h.assertTrue(json.contains("spell_progress") && json.contains("learned"),"Journal must include personal study status");
        var buffer=io.netty.buffer.Unpooled.buffer();
        try {var snapshot=new MagicProgressPayload(SpellKnowledge.mask(p),2,1);MagicProgressPayload.CODEC.encode(buffer,snapshot);var decoded=MagicProgressPayload.CODEC.decode(buffer);MagicProgressPayload.receive(decoded);h.assertTrue(MagicProgressPayload.client().knows(Spell.SANCTUARY) && !MagicProgressPayload.client().knows(Spell.MAGELIGHT),"Client progress snapshot must keep individual finite unlocks");MagicProgressPayload.clear();h.assertTrue(MagicProgressPayload.client().known()==0,"Logout clear must not leak previous world's knowledge");}finally{buffer.release();MagicProgressPayload.clear();}
        h.succeed();
    }
}
