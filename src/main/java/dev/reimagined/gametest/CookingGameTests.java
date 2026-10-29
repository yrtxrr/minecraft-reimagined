package dev.reimagined.gametest;

import dev.reimagined.cooking.*;
import dev.reimagined.agriculture.AgricultureContent;
import dev.reimagined.engineering.EngineeringContent;
import dev.reimagined.browser.BrowserRecipes;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.*;
import net.neoforged.neoforge.capabilities.Capabilities;

@GameTestHolder("reimagined")
@PrefixGameTestTemplate(false)
public final class CookingGameTests {
    private static final BlockPos POS = new BlockPos(4,3,4);
    private static CookingPotBlockEntity pot(GameTestHelper h) {
        h.setBlock(POS.below(),Blocks.MAGMA_BLOCK); h.setBlock(POS,CookingContent.POT.get());
        return (CookingPotBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(POS));
    }
    private static void porridge(CookingPotBlockEntity pot, int count) {
        pot.setItem(0,new ItemStack(AgricultureContent.BARLEY.get(),count));
        pot.setItem(1,new ItemStack(Items.APPLE,count)); pot.setItem(3,new ItemStack(Items.BOWL,count));
    }
    @GameTest(template="empty")
    public static void cookingRecipesMatchUnorderedIngredientsAndRejectExtras(GameTestHelper h) {
        var level = h.getLevel();
        var recipe = level.getRecipeManager().byKey(ResourceLocation.parse("reimagined:cooking/mushroom_dumplings")).orElseThrow().value();
        h.assertTrue(recipe instanceof CookingRecipe && recipe.isSpecial(),"Cooking must be a real separate process, outside vanilla crafting book");
        var cooking = (CookingRecipe) recipe;
        var valid = new CookingRecipe.Input(new ItemStack(Items.RED_MUSHROOM),new ItemStack(EngineeringContent.FLOUR.get(),64),new ItemStack(Items.BROWN_MUSHROOM));
        h.assertTrue(cooking.matches(valid,level),"Unordered ingredients must accept all slots and counts");
        h.assertTrue(!cooking.matches(new CookingRecipe.Input(new ItemStack(Items.RED_MUSHROOM),new ItemStack(EngineeringContent.FLOUR.get()),new ItemStack(Items.RED_MUSHROOM)),level),"Duplicate mushroom cannot replace the other ingredient");
        var porridge = (CookingRecipe) level.getRecipeManager().byKey(ResourceLocation.parse("reimagined:cooking/barley_porridge")).orElseThrow().value();
        h.assertTrue(!porridge.matches(new CookingRecipe.Input(new ItemStack(AgricultureContent.BARLEY.get()),new ItemStack(Items.APPLE),new ItemStack(Items.DIRT)),level),"Extra items must block cooking");
        var output = cooking.assemble(valid,level.registryAccess()); output.setCount(16);
        h.assertTrue(cooking.getResultItem(level.registryAccess()).getCount()==1,"Recipe outputs must be independent copies");
        h.succeed();
    }
    @GameTest(template="empty")
    public static void allCookingRecipesRegistered(GameTestHelper h) {
        var manager=h.getLevel().getRecipeManager();
        for (String id : List.of("barley_porridge","vegetable_stew","hunters_stew","fish_chowder","mushroom_dumplings")) {
            var recipe=manager.byKey(ResourceLocation.parse("reimagined:cooking/"+id));
            h.assertTrue(recipe.isPresent() && recipe.get().value() instanceof CookingRecipe,
                    "Cooking recipe "+id+" must be registered");
        }
        h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=380)
    public static void bothReviewedMealsActuallyCookAndConsumeExactlyOneBatch(GameTestHelper h) {
        var positions=List.of(new BlockPos(4,3,4),new BlockPos(9,3,4));
        var inputs=List.of(List.of(EngineeringContent.FLOUR.get(),Items.BROWN_MUSHROOM,Items.RED_MUSHROOM),
                List.of(Items.CARROT,Items.POTATO,Items.BEETROOT));
        var expected=List.of(CookingContent.MUSHROOM_DUMPLINGS.get(),CookingContent.VEGETABLE_STEW.get());
        var pots=new ArrayList<CookingPotBlockEntity>();
        for(int n=0;n<2;n++) {
            var pos=positions.get(n);h.setBlock(pos.below(),Blocks.MAGMA_BLOCK);h.setBlock(pos,CookingContent.POT.get());
            var pot=(CookingPotBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(pos));pots.add(pot);
            for(int slot=0;slot<3;slot++)pot.setItem(slot,new ItemStack(inputs.get(n).get(slot)));
            pot.setItem(3,new ItemStack(Items.BOWL));
        }
        h.runAtTickTime(100,()-> {
            for(var pot:pots)h.assertTrue(pot.getItem(4).isEmpty() && pot.progress()>0,"Both16second recipes must do timed work before yielding a meal");
        });
        h.succeedWhen(()-> {
            for(int n=0;n<2;n++) {
                var pot=pots.get(n);
                h.assertTrue(pot.getItem(4).is(expected.get(n)) && pot.getItem(4).getCount()==1,"Actual heated scheduled cooking must yield the specified meal");
                for(int slot=0;slot<4;slot++)h.assertTrue(pot.getItem(slot).isEmpty(),"Exactly one of every ingredient and one bowl consumed");
            }
        });
    }
    @GameTest(template="empty",timeoutTicks=300)
    public static void actualScheduledCookingConsumesOneBatchAndOneBowl(GameTestHelper h) {
        var pot = pot(h); porridge(pot,1);
        h.runAfterDelay(100,()-> h.assertTrue(pot.getItem(4).isEmpty() && pot.progress()>0,"Cooking must take real time, not instant preview crafting"));
        h.succeedWhen(()-> {
            h.assertTrue(pot.getItem(4).is(CookingContent.BARLEY_PORRIDGE.get()),"Scheduled block ticks must cook with heat");
            h.assertTrue(pot.getItem(0).isEmpty() && pot.getItem(1).isEmpty() && pot.getItem(3).isEmpty() && pot.getItem(4).getCount()==1,"One meal consumes exactly one of each ingredient and one bowl");
        });
    }
    @GameTest(template="empty")
    public static void heatBowlsAndFullOutputPauseWithoutLosingIngredients(GameTestHelper h) {
        var pot=pot(h); porridge(pot,2);
        pot.cookStep(); int progress=pot.progress();
        h.setBlock(POS.below(),Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT,false));
        pot.cookStep(); h.assertTrue(pot.progress()==progress,"Unlit campfire cannot cook or discard work");
        h.setBlock(POS.below(),Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT,true).setValue(CampfireBlock.WATERLOGGED,true));
        pot.cookStep();h.assertTrue(pot.progress()==progress,"Waterlogged heat source cannot cook");
        h.setBlock(POS.below(),Blocks.MAGMA_BLOCK);
        pot.setItem(3,ItemStack.EMPTY);pot.cookStep();h.assertTrue(pot.progress()==progress,"Missing bowls must pause");
        pot.setItem(3,new ItemStack(Items.BOWL,2));pot.setItem(4,new ItemStack(CookingContent.BARLEY_PORRIDGE.get(),16));
        pot.cookStep();h.assertTrue(pot.progress()==progress && pot.getItem(0).getCount()==2,"Full output must pause without consuming ingredients");
        pot.setItem(4,ItemStack.EMPTY);
        for(int i=0;i<11;i++)pot.cookStep();
        h.assertTrue(pot.getItem(4).getCount()==1 && pot.getItem(0).getCount()==1 && pot.getItem(3).getCount()==1,"Resuming must finish only the saved batch");
        pot.setItem(0,new ItemStack(Items.DIRT));pot.cookStep();
        h.assertTrue(pot.progress()==0 && pot.total()==0,"Invalid replacement must discard previous recipe progress");
        h.succeed();
    }
    @GameTest(template="empty")
    public static void savedCookingProgressAndInventoryResumeWithoutOfflineCatchup(GameTestHelper h) {
        var pot=pot(h);porridge(pot,2);for(int i=0;i<4;i++)pot.cookStep();
        var tag=pot.saveWithFullMetadata(h.getLevel().registryAccess());
        var restored=(CookingPotBlockEntity)BlockEntity.loadStatic(pot.getBlockPos(),pot.getBlockState(),tag,h.getLevel().registryAccess());
        h.assertTrue(restored!=null && restored.progress()==80 && restored.getItem(0).getCount()==2,"NBT roundtrip preserves work and inventory");
        h.getLevel().setBlockEntity(restored);
        for(int i=0;i<8;i++)restored.cookStep();
        h.assertTrue(restored.getItem(4).is(CookingContent.BARLEY_PORRIDGE.get()) && restored.getItem(4).getCount()==1 && restored.getItem(0).getCount()==1,"Loaded pot must resume remaining8seconds, without duplication or elapsed-world catchup");
        h.succeed();
    }
    @GameTest(template="empty")
    public static void sidedCapabilitiesAndMenuProtectOutputAndRouteBowls(GameTestHelper h) {
        var pot=pot(h);var level=h.getLevel();var type=Capabilities.ItemHandler.BLOCK;
        var top=level.getCapability(type,pot.getBlockPos(),Direction.UP);
        var side=level.getCapability(type,pot.getBlockPos(),Direction.EAST);
        var bottom=level.getCapability(type,pot.getBlockPos(),Direction.DOWN);
        h.assertTrue(top!=null && side!=null && bottom!=null && top==pot.handler(Direction.UP),"Capabilities must use persistent sided wrappers");
        h.assertTrue(top.insertItem(0,new ItemStack(AgricultureContent.BARLEY.get()),false).isEmpty(),"Top accepts recipe ingredients");
        h.assertTrue(!top.insertItem(1,new ItemStack(Items.BOWL),false).isEmpty() && !side.insertItem(0,new ItemStack(Items.DIRT),false).isEmpty(),"Food slots reject bowls and side slot rejects non-bowls");
        h.assertTrue(side.insertItem(0,new ItemStack(Items.BOWL),false).isEmpty() && top.extractItem(0,1,false).isEmpty(),"Side feeds bowls, top cannot steal ingredients");
        h.assertTrue(!bottom.insertItem(0,new ItemStack(CookingContent.BARLEY_PORRIDGE.get()),false).isEmpty(),"No automation output insertion");
        pot.setItem(4,new ItemStack(CookingContent.BARLEY_PORRIDGE.get()));
        h.assertTrue(bottom.extractItem(0,1,false).is(CookingContent.BARLEY_PORRIDGE.get()),"Bottom extracts real meals");
        var player=h.makeMockPlayer(GameType.SURVIVAL);var menu=new CookingPotMenu(1,player.getInventory(),pot,new net.minecraft.world.inventory.SimpleContainerData(3));
        h.assertTrue(!menu.getSlot(4).mayPlace(new ItemStack(Items.APPLE)),"GUI output must also reject insertion");
        player.getInventory().setItem(9,new ItemStack(Items.BOWL,3));menu.quickMoveStack(player,5);
        h.assertTrue(pot.getItem(3).getCount()==4 && player.getInventory().getItem(9).isEmpty(),"Shift-click bowls must target only their own slot");
        h.succeed();
    }
    @GameTest(template="empty")
    public static void mealsRestoreHungerReturnBowlsAndAppearInBrowser(GameTestHelper h) {
        var player=h.makeMockPlayer(GameType.SURVIVAL); player.getFoodData().setFoodLevel(0);
        var meal=new ItemStack(CookingContent.BARLEY_PORRIDGE.get());
        var remaining=meal.finishUsingItem(h.getLevel(),player);
        h.assertTrue(remaining.is(Items.BOWL) && player.getFoodData().getFoodLevel()==7,"Actual food consumption must restore nutrition and return one bowl");
        var displays=BrowserRecipes.recipeDisplays(h.getLevel().getRecipeManager(),h.getLevel().registryAccess(),ItemStack.EMPTY,List.of());
        var cooking=displays.stream().filter(d->d.station().is(CookingContent.POT_ITEM.get())).toList();
        h.assertTrue(cooking.size()==7 && cooking.stream().allMatch(d->d.uses(new ItemStack(Items.BOWL)) && !d.note().getString().isEmpty()),"Every meal must expose ingredients, required bowl, station and time");
        h.assertTrue(cooking.stream().anyMatch(d->d.uses(new ItemStack(EngineeringContent.FLOUR.get())) && d.result().is(CookingContent.MUSHROOM_DUMPLINGS.get())),"Milled flour must link to cooking in uses mode");
        h.succeed();
    }
    @GameTest(template="empty")
    public static void cookingCodecAndNetworkRoundTripPreserveStationRecipe(GameTestHelper h) {
        var recipe=(CookingRecipe)h.getLevel().getRecipeManager().byKey(ResourceLocation.parse("reimagined:cooking/hunters_stew")).orElseThrow().value();
        var serializer=CookingContent.SERIALIZER.get();
        var buffer=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),h.getLevel().registryAccess());
        try {
            serializer.streamCodec().encode(buffer,recipe);var decoded=serializer.streamCodec().decode(buffer);
            h.assertTrue(decoded.seconds()==16 && decoded.ingredients().size()==3 && decoded.result().is(CookingContent.HUNTERS_STEW.get()),"Recipe sync must preserve every field");
        } finally { buffer.release(); }
        var ops=h.getLevel().registryAccess().createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE);
        var json=serializer.codec().codec().encodeStart(ops,recipe).getOrThrow();
        var decoded=serializer.codec().codec().parse(ops,json).getOrThrow();
        h.assertTrue(decoded.getIngredients().size()==4,"Reload codec must reconstruct required bowl display");
        json.getAsJsonObject().addProperty("seconds",0);
        h.assertTrue(serializer.codec().codec().parse(ops,json).error().isPresent(),"Invalid cooking time must be rejected at reload");
        h.succeed();
    }
    @GameTest(template="empty")
    public static void recipeReloadInvalidatesPositiveAndNegativeCookingCaches(GameTestHelper h) {
        var pot=pot(h);porridge(pot,3);pot.cookStep();
        var manager=h.getLevel().getRecipeManager();var originals=List.copyOf(manager.getRecipes());
        var id=ResourceLocation.parse("reimagined:cooking/barley_porridge");
        try {
            var changed=new ArrayList<RecipeHolder<?>>();
            for(var holder:originals) if(!holder.id().equals(id)) changed.add(holder);
            manager.replaceRecipes(changed);
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.OnDatapackSyncEvent(h.getLevel().getServer().getPlayerList(),null));
            pot.cookStep();
            h.assertTrue(pot.progress()==0 && pot.total()==0 && pot.getItem(0).getCount()==3,"Removed recipe must invalidate positive cache and unfinished work");
            var replacement=new CookingRecipe(List.of(Ingredient.of(AgricultureContent.BARLEY.get()),Ingredient.of(Items.APPLE)),new ItemStack(CookingContent.VEGETABLE_STEW.get()),1);
            changed.add(new RecipeHolder<>(id,replacement));manager.replaceRecipes(changed);
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.OnDatapackSyncEvent(h.getLevel().getServer().getPlayerList(),null));
            pot.cookStep();
            h.assertTrue(pot.getItem(4).is(CookingContent.VEGETABLE_STEW.get()) && pot.getItem(0).getCount()==2,"Added recipe must invalidate cached miss and use new result/time");
        } finally {
            manager.replaceRecipes(originals);
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.OnDatapackSyncEvent(h.getLevel().getServer().getPlayerList(),null));
        }
        h.succeed();
    }

    @GameTest(template="empty",timeoutTicks=340)
    public static void realHoppersFeedIngredientsAndExtractMealsThroughHeatConductor(GameTestHelper h) {
        var pot=pot(h);
        h.setBlock(POS.below(2),Blocks.MAGMA_BLOCK);
        h.setBlock(POS.below(),Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING,Direction.EAST));
        h.setBlock(POS.below().east(),Blocks.CHEST);
        h.setBlock(POS.above(),Blocks.HOPPER);
        var feeder=(net.minecraft.world.level.block.entity.HopperBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(POS.above()));
        feeder.setItem(0,new ItemStack(AgricultureContent.BARLEY.get()));feeder.setItem(1,new ItemStack(Items.APPLE));
        pot.setItem(3,new ItemStack(Items.BOWL));
        h.succeedWhen(()-> {
            var chest=(net.minecraft.world.level.block.entity.ChestBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(POS.below().east()));
            h.assertTrue(chest.getItem(0).is(CookingContent.BARLEY_PORRIDGE.get()) && chest.getItem(0).getCount()==1,"Real hopper input/output must cook above one conducting hopper");
            h.assertTrue(feeder.isEmpty() && pot.getItem(0).isEmpty() && pot.getItem(1).isEmpty() && pot.getItem(3).isEmpty(),"Physical automation must not duplicate food or bowls");
        });
    }

    @GameTest(template="empty",timeoutTicks=100)
    public static void emptyPotsSleepAndExternalInspectionNeverRequestsUnloadedChunks(GameTestHelper h) {
        var pot=pot(h);var level=h.getLevel();var state=CookingContent.POT.get().defaultBlockState();
        h.assertTrue(CookingContent.POT.get().getTicker(level,state,CookingContent.ENTITY.get())==null,"Pots must have no per-tick block-entity ticker");
        BlockPos absent=new BlockPos(16000000,80,16000000);
        h.assertTrue(level.getChunkSource().getChunkNow(absent.getX()>>4,absent.getZ()>>4)==null,"Fixture requires an unloaded distant chunk");
        int chunks=level.getChunkSource().getLoadedChunksCount();
        h.assertTrue(state.getMenuProvider(level,absent)==null && state.getAnalogOutputSignal(level,absent)==0,"Menus/comparators fail closed on unloaded positions");
        h.assertTrue(level.getChunkSource().getLoadedChunksCount()==chunks && level.getChunkSource().getChunkNow(absent.getX()>>4,absent.getZ()>>4)==null,"Inspection must not request or load a distant chunk");
        h.runAfterDelay(65,()-> {
            h.assertTrue(!level.getBlockTicks().hasScheduledTick(pot.getBlockPos(),CookingContent.POT.get()) && pot.progress()==0,"A settled empty pot must retain no periodic work");
            h.succeed();
        });
    }

}
