package dev.reimagined.gametest;

import dev.reimagined.engineering.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("reimagined")
@PrefixGameTestTemplate(false)
public final class MechanicalIntegrationGameTests {
    private static final BlockPos W = new BlockPos(3,3,3);
    private static MachineBlockEntity machine(GameTestHelper h, BlockPos p) {
        return (MachineBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(p));
    }
    private static void wheel(GameTestHelper h) {
        h.setBlock(W,EngineeringContent.WATER_WHEEL.get().defaultBlockState().setValue(MechanicalBlock.AXIS,Direction.Axis.X));
        h.setBlock(W.east(),EngineeringContent.SHAFT.get().defaultBlockState().setValue(MechanicalBlock.AXIS,Direction.Axis.X));
        for(int z=4;z<=5;z++) {
            h.setBlock(new BlockPos(3,2,z),Blocks.STONE);
            h.setBlock(new BlockPos(2,3,z),Blocks.STONE);
            h.setBlock(new BlockPos(4,3,z),Blocks.STONE);
        }
        h.setBlock(new BlockPos(3,3,6),Blocks.STONE);
        h.setBlock(new BlockPos(3,3,5),Blocks.WATER);
        h.setBlock(new BlockPos(3,3,4),Blocks.WATER.defaultBlockState().setValue(LiquidBlock.LEVEL,1));
    }
    @GameTest(template="empty",timeoutTicks=110)
    public static void wheelWithoutMillRetainsPeriodicRevalidationWithoutReaders(GameTestHelper h) {
        h.setBlock(W,EngineeringContent.WATER_WHEEL.get());
        h.setBlock(W.east(),EngineeringContent.SHAFT.get());
        h.runAtTickTime(80,()-> {
            long checked=MechanicalNetworks.lastCheckedAt(h.getLevel(),h.absolutePos(W));
            h.assertTrue(checked>=0 && h.getLevel().getGameTime()-checked<=40,
                    "Wheel-only network must revalidate without a mill, player click or rotation read");
            h.succeed();
        });
    }
    @GameTest(template="empty",timeoutTicks=210)
    public static void wheelAndShaftRespondToWaterRedstoneAndLifecycleWithoutMill(GameTestHelper h) {
        wheel(h);
        h.runAtTickTime(35,()-> {
            h.assertTrue(machine(h,W).rotationSpeed()>0 && machine(h,W.east()).rotationSpeed()>0,"Water drives wheel/shaft without mill");
            h.setBlock(W.above(),Blocks.REDSTONE_BLOCK);
        });
        h.runAtTickTime(65,()-> {
            h.assertTrue(machine(h,W).rotationSpeed()==0 && machine(h,W.east()).rotationSpeed()==0,"Redstone stops whole wheel-only network");
            h.setBlock(W.above(),Blocks.AIR);
        });
        h.runAtTickTime(95,()-> {
            h.assertTrue(machine(h,W).rotationSpeed()>0,"Removing signal resumes without interaction");
            var chunk=new ChunkPos(h.absolutePos(W));
            MechanicalNetworks.unloadChunk(h.getLevel(),chunk);
            h.assertTrue(!MechanicalNetworks.isCached(h.getLevel(),h.absolutePos(W)),"Unload retires cached component");
            MechanicalNetworks.loadedChunk(h.getLevel(),chunk);
            // Model BE onLoad after the real lifecycle callbacks, not a diagnostic rotation query.
            machine(h,W).onLoad(); machine(h,W.east()).onLoad();
        });
        h.runAtTickTime(125,()-> {
            h.assertTrue(machine(h,W).rotationSpeed()>0,"Load revalidates motion");
            for(int z=4;z<=5;z++) h.setBlock(new BlockPos(3,3,z),Blocks.STONE);
        });
        h.runAtTickTime(160,()-> {
            h.assertTrue(machine(h,W).rotationSpeed()==0 && machine(h,W.east()).rotationSpeed()==0,"Removed water stops both nodes");
            h.succeed();
        });
    }
    @GameTest(template="empty")
    public static void millingCacheReloadChangesResultAndHopperEligibility(GameTestHelper h) {
        var pos=W.east(2); h.setBlock(pos,EngineeringContent.MILLSTONE.get());
        var m=machine(h,pos); var manager=h.getLevel().getRecipeManager();var originals=List.copyOf(manager.getRecipes());
        var id=ResourceLocation.parse("reimagined:integration_milling");
        var first=new RecipeHolder<>(id,new MillingRecipe(Ingredient.of(Items.CARROT),new ItemStack(Items.EMERALD),1));
        var changed=new ArrayList<RecipeHolder<?>>(originals);
        try {
            changed.add(first); manager.replaceRecipes(changed);
            h.assertTrue(m.canPlaceItemThroughFace(0,new ItemStack(Items.CARROT),Direction.UP),"Added recipe invalidates cached miss for automation");
            var check=RecipeManager.createCheck(EngineeringContent.MILLING.get());
            h.assertTrue(check.getRecipeFor(new SingleRecipeInput(new ItemStack(Items.CARROT)),h.getLevel()).orElseThrow().value().result().is(Items.EMERALD),"Prime positive cached result");
            changed.remove(first);manager.replaceRecipes(changed);
            h.assertTrue(!m.canPlaceItemThroughFace(0,new ItemStack(Items.CARROT),Direction.UP)
                    && check.getRecipeFor(new SingleRecipeInput(new ItemStack(Items.CARROT)),h.getLevel()).isEmpty(),"Deleted recipe invalidates cached positive");
            var replacement=new RecipeHolder<>(id,new MillingRecipe(Ingredient.of(Items.CARROT),new ItemStack(Items.DIAMOND),1));
            changed.add(replacement);manager.replaceRecipes(changed);
            h.assertTrue(m.canPlaceItemThroughFace(0,new ItemStack(Items.CARROT),Direction.UP)
                    && check.getRecipeFor(new SingleRecipeInput(new ItemStack(Items.CARROT)),h.getLevel()).orElseThrow().value().result().is(Items.DIAMOND),"Reused recipe ID must resolve new result, not old holder");
            var overlap=new RecipeHolder<>(ResourceLocation.parse("reimagined:integration_overlap"),new MillingRecipe(Ingredient.of(Items.CARROT),new ItemStack(Items.GOLD_INGOT),2));
            changed.add(overlap);manager.replaceRecipes(changed);
            var selected=check.getRecipeFor(new SingleRecipeInput(new ItemStack(Items.CARROT)),h.getLevel()).orElseThrow();
            h.assertTrue((selected.id().equals(id)||selected.id().equals(overlap.id())) && selected.value().matches(new SingleRecipeInput(new ItemStack(Items.CARROT)),h.getLevel()),"Overlap selects a current matching recipe; datapack authors should avoid ambiguous priorities");
            h.assertTrue(!m.canPlaceItemThroughFace(0,new ItemStack(Items.CARROT),Direction.DOWN),"Reload preserves directional insertion restriction");
        } finally { manager.replaceRecipes(originals); }
        h.succeed();
    }
}
