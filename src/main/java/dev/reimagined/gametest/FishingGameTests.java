package dev.reimagined.gametest;

import dev.reimagined.fishing.FishContent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("reimagined")
@PrefixGameTestTemplate(false)
public final class FishingGameTests {
    private static final ResourceKey<LootTable> EXTRA_FISH = ResourceKey.create(
            net.minecraft.core.registries.Registries.LOOT_TABLE,
            ResourceLocation.fromNamespaceAndPath("reimagined", "gameplay/extra_fish_from_fishing"));

    private FishingGameTests() {}

    @GameTest(template = "empty")
    public static void threeFishAreRegisteredEdibleAndHaveCookRecipes(GameTestHelper helper) {
        var level = helper.getLevel();
        for (var fish : new ItemStack[]{new ItemStack(FishContent.RIVER_PERCH.get()),
                new ItemStack(FishContent.SILVER_TROUT.get()), new ItemStack(FishContent.BLUE_MACKEREL.get())}) {
            helper.assertTrue(BuiltInRegistries.ITEM.getKey(fish.getItem()) != null, "Every new fish must be registered");
            helper.assertTrue(fish.getFoodProperties(null) != null, "New fish must be edible");
            String path = BuiltInRegistries.ITEM.getKey(fish.getItem()).getPath();
            var recipeId = ResourceLocation.fromNamespaceAndPath("reimagined", path + "_cooked");
            var recipe = level.getRecipeManager().byKey(recipeId).orElseThrow(
                    () -> new AssertionError("Missing simple cooking recipe " + recipeId));
            helper.assertTrue(recipe.value().getResultItem(level.registryAccess()).is(Items.COOKED_COD),
                    "Simple fish cooking recipe should yield cooked cod");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void vanillaFishingLootIncludesTheThreeNewFishWithoutReplacingVanillaCatch(GameTestHelper helper) {
        var level = helper.getLevel();
        var extraFishTable = level.getServer().reloadableRegistries().getLootTable(EXTRA_FISH);
        helper.assertTrue(extraFishTable != LootTable.EMPTY, "Extra fish loot table must load from datapack");
        FishingHook hook = new FishingHook(EntityType.FISHING_BOBBER, level);
        var fishParams = new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, hook.position())
                .withParameter(LootContextParams.TOOL, new ItemStack(Items.FISHING_ROD))
                .withParameter(LootContextParams.THIS_ENTITY, hook)
                .create(LootContextParamSets.FISHING);
        var caughtTypes = new java.util.HashSet<Item>();
        var vanillaTypes = new java.util.HashSet<Item>();
        int bonusCatches=0;
        var vanillaFishing = level.getServer().reloadableRegistries().getLootTable(BuiltInLootTables.FISHING);
        helper.assertTrue(vanillaFishing != LootTable.EMPTY, "Vanilla fishing loot table must remain loaded");
        for (long seed = 1; seed <= 512; seed++) {
            var raw = new java.util.ArrayList<ItemStack>();
            // A raw roll is deliberate here ONLY as a control, never a gameplay path.
            var control = new net.minecraft.world.level.storage.loot.LootContext.Builder(fishParams)
                    .withOptionalRandomSeed(seed).create(java.util.Optional.empty());
            vanillaFishing.getRandomItemsRaw(control, raw::add);
            var modified = vanillaFishing.getRandomItems(fishParams, seed);
            helper.assertTrue(!raw.isEmpty() && modified.size() >= raw.size() && modified.size() <= raw.size()+1,
                    "Each fishing roll retains the vanilla result and adds at most one bonus");
            for(int i=0;i<raw.size();i++) {
                var before=raw.get(i); var after=modified.get(i);
                helper.assertTrue(before.getCount()==after.getCount() && ItemStack.isSameItemSameComponents(before,after),
                        "Vanilla catch, counts and treasure components must be unchanged for the same seed");
                vanillaTypes.add(before.getItem());
            }
            if(modified.size()>raw.size()) {
                var bonus=modified.getLast();
                helper.assertTrue(isNewFish(bonus) && bonus.getCount()==1,"Bonus is exactly one of the three fish");
                caughtTypes.add(bonus.getItem());bonusCatches++;
            }
        }
        helper.assertTrue(caughtTypes.size()==3 && bonusCatches>0 && bonusCatches<512,
                "All new fish occur, while bonus remains optional");
        helper.assertTrue(vanillaTypes.containsAll(java.util.List.of(Items.COD,Items.SALMON,Items.PUFFERFISH,Items.TROPICAL_FISH)),
                "All four vanilla fish still occur in the retained base catch");
        helper.succeed();
    }
    private static boolean isNewFish(ItemStack s) {
        return s.is(FishContent.RIVER_PERCH.get()) || s.is(FishContent.SILVER_TROUT.get()) || s.is(FishContent.BLUE_MACKEREL.get());
    }
    @GameTest(template="empty")
    public static void bonusDoesNotLeakToChestsOrRecurseThroughFishTables(GameTestHelper h) {
        var level=h.getLevel();var resolver=level.getServer().reloadableRegistries();
        var chest=resolver.getLootTable(BuiltInLootTables.SPAWN_BONUS_CHEST);
        var chestParams=new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN,h.absolutePos(new net.minecraft.core.BlockPos(3,3,3)).getCenter()).create(LootContextParamSets.CHEST);
        var params=new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN,net.minecraft.world.phys.Vec3.ZERO)
                .withParameter(LootContextParams.TOOL,new ItemStack(Items.FISHING_ROD)).create(LootContextParamSets.FISHING);
        var extra=resolver.getLootTable(EXTRA_FISH);
        var fishOnly=resolver.getLootTable(ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE,ResourceLocation.parse("minecraft:gameplay/fishing/fish")));
        for(long seed=1;seed<=32;seed++) {
            h.assertTrue(chest.getRandomItems(chestParams,seed).stream().noneMatch(FishingGameTests::isNewFish),"Unrelated chest loot receives no fish bonus");
            var sub=fishOnly.getRandomItems(params,seed);
            h.assertTrue(sub.size()==1 && !isNewFish(sub.getFirst()),"Vanilla nested fish table is not independently modified");
            var added=extra.getRandomItems(params,seed);
            h.assertTrue(added.size()==1 && isNewFish(added.getFirst()),"Extra table cannot recursively add more bonuses");
        }
        h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=270)
    public static void allThreeFishReallyCookInFurnaceAndSmokerAndAppearInBrowser(GameTestHelper h) {
        var fish=java.util.List.of(FishContent.RIVER_PERCH.get(),FishContent.SILVER_TROUT.get(),FishContent.BLUE_MACKEREL.get());
        var positions=new java.util.ArrayList<net.minecraft.core.BlockPos>();
        var displays=dev.reimagined.browser.BrowserRecipes.snapshot(h.getLevel().getRecipeManager(),h.getLevel().registryAccess(),h.getLevel().potionBrewing());
        h.assertTrue(new ItemStack(Items.COD).is(net.minecraft.tags.ItemTags.FISHES) && new ItemStack(Items.SALMON).is(net.minecraft.tags.ItemTags.FISHES),"Extending fishes never removes vanilla tag members");
        for(int i=0;i<fish.size();i++) {
            var input=new ItemStack(fish.get(i));h.assertTrue(input.is(net.minecraft.tags.ItemTags.FISHES),"Every new fish extends the vanilla fishes tag");
            for(int mode=0;mode<2;mode++) {
                var p=new net.minecraft.core.BlockPos(2+i*3,3,3+mode*4);positions.add(p);
                var block=mode==0?net.minecraft.world.level.block.Blocks.FURNACE:net.minecraft.world.level.block.Blocks.SMOKER;h.setBlock(p,block);
                var furnace=(net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(p));
                furnace.setItem(0,input.copy());furnace.setItem(1,new ItemStack(Items.COAL));
                var id=ResourceLocation.parse("reimagined:"+BuiltInRegistries.ITEM.getKey(fish.get(i)).getPath()+(mode==0?"_cooked":"_smoked"));
                var display=displays.stream().filter(d->d.id().equals(id)).findFirst().orElseThrow();
                h.assertTrue(display.uses(input) && display.result().is(Items.COOKED_COD) && display.station().is(block.asItem()),"Shared browser displays actual fish ingredient/result/station");
            }
        }
        h.runAtTickTime(235,()-> {
            for(var p:positions) {
                var furnace=(net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(p));
                h.assertTrue(furnace.getItem(0).isEmpty() && furnace.getItem(2).is(Items.COOKED_COD) && furnace.getItem(2).getCount()==1,"Real fueled furnace/smoker must process each new fish once");
            }
            h.succeed();
        });
    }
}
