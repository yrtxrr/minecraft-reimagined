package dev.reimagined.gametest;

import dev.reimagined.agriculture.AgricultureContent;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.*;
import net.minecraft.tags.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("reimagined")
@PrefixGameTestTemplate(false)
public final class KiloContentGameTests {
    @GameTest(template="empty")
    public static void thatchCraftsDropsAndBothNewItemsActuallyCompost(GameTestHelper h) {
        var input=CraftingInput.of(2,2,java.util.List.of(new ItemStack(Items.WHEAT),new ItemStack(Items.WHEAT),new ItemStack(Items.WHEAT),new ItemStack(Items.WHEAT)));
        var recipe=h.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING,input,h.getLevel()).orElseThrow();
        var result=recipe.value().assemble(input,h.getLevel().registryAccess());
        h.assertTrue(result.is(AgricultureContent.THATCH_ITEM.get()) && result.getCount()==4,"Actual wheat recipe produces four thatch blocks");
        var p=new BlockPos(4,3,4);h.setBlock(p,AgricultureContent.THATCH.get());
        var drops=Block.getDrops(h.getBlockState(p),h.getLevel(),h.absolutePos(p),null);
        h.assertTrue(drops.size()==1 && drops.getFirst().is(AgricultureContent.THATCH_ITEM.get()),"Thatch has real self loot, not silent destruction");
        for(var item:java.util.List.of(AgricultureContent.THATCH_ITEM.get(),AgricultureContent.CLOVER_ITEM.get(),AgricultureContent.BARLEY.get(),dev.reimagined.agriculture.OrchardContent.PEAR.get())) {
            var state=Blocks.COMPOSTER.defaultBlockState();h.setBlock(p,state);var stack=new ItemStack(item);
            var next=ComposterBlock.insertItem(null,state,h.getLevel(),stack,h.absolutePos(p));
            h.assertTrue(stack.isEmpty() && next.getValue(ComposterBlock.LEVEL)==1,"Composter must recognize the Item key, not the Block object");
        }
        var displays=dev.reimagined.browser.BrowserRecipes.snapshot(h.getLevel().getRecipeManager(),h.getLevel().registryAccess(),h.getLevel().potionBrewing());
        h.assertTrue(displays.stream().anyMatch(d->d.result().is(AgricultureContent.THATCH_ITEM.get()) && d.result().getCount()==4 && d.uses(new ItemStack(Items.WHEAT))),"Shared browser exposes actual thatch recipe");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=30)
    public static void cloverUsesVanillaFlowerTagsSoilAndLoot(GameTestHelper h) {
        var p=new BlockPos(4,3,4);h.setBlock(p.below(),Blocks.DIRT);h.setBlock(p,AgricultureContent.CLOVER.get());
        var state=h.getBlockState(p);
        h.assertTrue(state.canSurvive(h.getLevel(),h.absolutePos(p)) && state.is(BlockTags.SMALL_FLOWERS) && state.is(BlockTags.FLOWERS)
                && new ItemStack(AgricultureContent.CLOVER_ITEM.get()).is(ItemTags.SMALL_FLOWERS),"Vanilla flower behavior/tags include clover");
        var drops=Block.getDrops(state,h.getLevel(),h.absolutePos(p),null);
        h.assertTrue(drops.size()==1 && drops.getFirst().is(AgricultureContent.CLOVER_ITEM.get()),"Clover has a real harvest drop");
        h.setBlock(p.below(),Blocks.STONE);
        h.runAtTickTime(3,()->{h.assertTrue(h.getBlockState(p).isAir(),"Plant cannot survive on stone after soil removal");h.succeed();});
    }
    @GameTest(template="empty")
    public static void realCloverPatchGrowsOnlyBoundedVanillaFlowerAttempts(GameTestHelper h) {
        for(int x=1;x<=13;x++)for(int z=1;z<=13;z++)h.setBlock(new BlockPos(x,2,z),Blocks.GRASS_BLOCK);
        var feature=h.getLevel().registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE)
                .getHolderOrThrow(ResourceKey.create(Registries.CONFIGURED_FEATURE,ResourceLocation.parse("reimagined:wild_clover"))).value();
        h.assertTrue(feature.place(h.getLevel(),h.getLevel().getChunkSource().getGenerator(),RandomSource.create(1835),h.absolutePos(new BlockPos(7,3,7))),"Real clover flower feature must place on grass");
        int count=0;for(int x=1;x<=13;x++)for(int z=1;z<=13;z++)if(h.getBlockState(new BlockPos(x,3,z)).is(AgricultureContent.CLOVER.get()))count++;
        h.assertTrue(count>0 && count<=12,"Single bounded patch, not a carpet or a world scan");h.succeed();
    }
    @GameTest(template="empty")
    public static void cloverGenerationIsSparseAndBiomeScoped(GameTestHelper h) {
        var regs=h.getLevel().registryAccess();var feature=regs.registryOrThrow(Registries.PLACED_FEATURE)
                .getHolderOrThrow(ResourceKey.create(Registries.PLACED_FEATURE,ResourceLocation.parse("reimagined:wild_clover"))).value();
        for(var biome:java.util.List.of(Biomes.FOREST,Biomes.BIRCH_FOREST,Biomes.MEADOW,Biomes.PLAINS))
            h.assertTrue(regs.registryOrThrow(Registries.BIOME).getHolderOrThrow(biome).value().getGenerationSettings().hasFeature(feature),"Expected temperate biome contains wild clover");
        h.assertTrue(!regs.registryOrThrow(Registries.BIOME).getHolderOrThrow(Biomes.DESERT).value().getGenerationSettings().hasFeature(feature),"Desert receives no clover");
        var json=PlacedFeature.DIRECT_CODEC.encodeStart(regs.createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE),feature).getOrThrow().getAsJsonObject();
        h.assertTrue(json.getAsJsonArray("placement").size()==4 && json.getAsJsonArray("placement").get(0).getAsJsonObject().get("chance").getAsInt()==8,"One rarity8patch candidate, no count multiplier");h.succeed();
    }
}
