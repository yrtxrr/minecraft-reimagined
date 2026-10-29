package dev.reimagined.gametest;

import com.google.gson.JsonParser;
import dev.reimagined.agriculture.AgricultureContent;
import dev.reimagined.agriculture.OrchardContent;
import dev.reimagined.beekeeping.BeekeepingContent;
import dev.reimagined.beekeeping.BeeFrames;
import dev.reimagined.browser.BrowserRecipes;
import dev.reimagined.cooking.CookingContent;
import dev.reimagined.cooking.CookingPotBlockEntity;
import dev.reimagined.engineering.EngineeringContent;
import io.netty.buffer.Unpooled;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Beekeeping extends vanilla hives instead of replacing them, so the tests focus on the two added
 * pieces of state (frames, smoker behaviour) plus the real recipe/cooking/browser pipeline.
 */
@GameTestHolder("reimagined")
@PrefixGameTestTemplate(false)
public final class BeekeepingGameTests {
    private static final BlockPos HIVE = new BlockPos(2, 3, 2);
    private static final BlockPos OTHER = new BlockPos(6, 3, 6);
    private static final BlockPos STONE = new BlockPos(1, 3, 6);
    private static final BlockPos POT = new BlockPos(5, 3, 2);

    private static BeehiveBlockEntity hive(GameTestHelper h) {
        h.setBlock(HIVE, Blocks.BEEHIVE);
        return (BeehiveBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(HIVE));
    }

    private static int count(Player player, Item item) {
        int total = 0;
        var inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(item)) total += stack.getCount();
        }
        return total;
    }

    private static int combs(ServerLevel level, BlockPos pos) {
        return items(level, pos, Items.HONEYCOMB);
    }

    private static int items(ServerLevel level, BlockPos pos, Item item) {
        return level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(4),
                entity -> entity.getItem().is(item)).stream()
                .mapToInt(entity -> entity.getItem().getCount()).sum();
    }

    private static Bee angryBee(GameTestHelper h, Player player, BlockPos near) {
        var level = h.getLevel();
        var bee = EntityType.BEE.create(level);
        bee.setPos(near.getX() + 0.5, near.getY() + 0.5, near.getZ() + 0.5);
        bee.setNoAi(true);
        bee.setPersistenceRequired();
        level.addFreshEntity(bee);
        bee.setPersistentAngerTarget(player.getUUID());
        bee.setRemainingPersistentAngerTime(400);
        bee.setTarget(player);
        return bee;
    }

    @GameTest(template = "empty")
    public static void contentIsRegisteredWithRealRecipesTagsAndAttachment(GameTestHelper h) {
        var level = h.getLevel();
        h.assertTrue(BuiltInRegistries.ITEM.get(ResourceLocation.parse("reimagined:beeswax")) == BeekeepingContent.BEESWAX.get()
                && BuiltInRegistries.ITEM.get(ResourceLocation.parse("reimagined:bee_frame")) == BeekeepingContent.BEE_FRAME.get()
                && BuiltInRegistries.ITEM.get(ResourceLocation.parse("reimagined:bee_smoker")) == BeekeepingContent.BEE_SMOKER.get(),
                "Every beekeeping item must be registered under its own id");
        h.assertTrue(BeekeepingContent.BEESWAX_BLOCK.get().asItem() == BeekeepingContent.BEESWAX_BLOCK_ITEM.get()
                && BeekeepingContent.BEESWAX_BLOCK.get().defaultBlockState().is(BlockTags.MINEABLE_WITH_HOE),
                "The wax block must have a block item and belong to the hoe mineable tag");
        h.assertTrue(NeoForgeRegistries.ATTACHMENT_TYPES.getKey(BeekeepingContent.HIVE_FRAMES.get())
                .equals(ResourceLocation.parse("reimagined:hive_frames")), "Frame attachment must be registered by id");

        for (String id : List.of("bee_frame", "bee_smoker", "beeswax_block", "beeswax_from_block", "milling_honeycomb_wax")) {
            h.assertTrue(level.getRecipeManager().byKey(ResourceLocation.parse("reimagined:" + id)).isPresent(),
                    "Beekeeping recipe must load from the real recipe manager: " + id);
        }
        var compost = BeekeepingContent.BEESWAX.get().builtInRegistryHolder().getData(
                net.neoforged.neoforge.registries.datamaps.builtin.NeoForgeDataMaps.COMPOSTABLES);
        h.assertTrue(compost != null && compost.chance() == 0.3F, "Wax must have reloadable compost data");
        h.setBlock(new BlockPos(2, 2, 2), Blocks.COMPOSTER);
        var waxStack = new ItemStack(BeekeepingContent.BEESWAX.get());
        var composted = net.minecraft.world.level.block.ComposterBlock.insertItem(null,
                h.getBlockState(new BlockPos(2, 2, 2)), level, waxStack, h.absolutePos(new BlockPos(2, 2, 2)));
        h.assertTrue(waxStack.isEmpty() && composted.getValue(net.minecraft.world.level.block.ComposterBlock.LEVEL) == 1,
                "Actual first compost layer consumes wax");
        var milling = level.getRecipeManager().byKey(ResourceLocation.parse("reimagined:milling_honeycomb_wax")).orElseThrow().value();
        h.assertTrue(milling.getResultItem(level.registryAccess()).is(BeekeepingContent.BEESWAX.get()),
                "Milling must turn vanilla honeycomb into wax through the existing millstone process");

        var grid = new java.util.ArrayList<ItemStack>(java.util.Collections.nCopies(9, ItemStack.EMPTY));
        grid.set(0, new ItemStack(Items.OAK_PLANKS));
        grid.set(1, new ItemStack(Items.OAK_PLANKS));
        grid.set(2, new ItemStack(BeekeepingContent.BEESWAX.get()));
        grid.set(3, new ItemStack(Items.STRING));
        var frameRecipe = (CraftingRecipe) level.getRecipeManager().byKey(ResourceLocation.parse("reimagined:bee_frame")).orElseThrow().value();
        var frameResult = frameRecipe.getResultItem(level.registryAccess());
        h.assertTrue(frameRecipe.matches(CraftingInput.of(3, 3, grid), level) && frameResult.is(BeekeepingContent.BEE_FRAME.get())
                        && frameResult.getCount() == 2,
                "A real crafting grid must produce two frames from planks, wax and string");

        grid.set(0, new ItemStack(Items.IRON_INGOT));
        grid.set(1, new ItemStack(BeekeepingContent.BEESWAX.get()));
        grid.set(2, new ItemStack(Items.STICK));
        grid.set(3, ItemStack.EMPTY);
        var smokerRecipe = (CraftingRecipe) level.getRecipeManager().byKey(ResourceLocation.parse("reimagined:bee_smoker")).orElseThrow().value();
        h.assertTrue(smokerRecipe.matches(CraftingInput.of(3, 3, grid), level)
                        && smokerRecipe.getResultItem(level.registryAccess()).is(BeekeepingContent.BEE_SMOKER.get()),
                "A real crafting grid must produce the smoker from iron, wax and a stick");

        for (int slot = 0; slot < 9; slot++) grid.set(slot, new ItemStack(BeekeepingContent.BEESWAX.get()));
        var blockRecipe = (CraftingRecipe) level.getRecipeManager().byKey(ResourceLocation.parse("reimagined:beeswax_block")).orElseThrow().value();
        h.assertTrue(blockRecipe.matches(CraftingInput.of(3, 3, grid), level)
                        && blockRecipe.getResultItem(level.registryAccess()).is(BeekeepingContent.BEESWAX_BLOCK_ITEM.get()),
                "Nine wax must compress into the wax block in a real crafting grid");

        for (String id : List.of("cooking/honeyed_pear", "cooking/honey_cake")) {
            h.assertTrue(level.getRecipeManager().byKey(ResourceLocation.parse("reimagined:" + id)).isPresent(),
                    "Honey meal must be a real cooking recipe: " + id);
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void framesInstallToCapAndPersistAcrossNbtReload(GameTestHelper h) {
        var level = h.getLevel();
        var abs = h.absolutePos(HIVE);
        var hive = hive(h);
        h.assertTrue(hive != null, "A placed vanilla hive must provide its block entity");
        h.assertTrue(BeeFrames.get(hive) == 0, "A new hive must start without frames");
        h.assertTrue(BeeFrames.install(hive) && BeeFrames.install(hive) && BeeFrames.install(hive),
                "Three frames must fit into one hive");
        h.assertTrue(!BeeFrames.install(hive) && BeeFrames.get(hive) == BeeFrames.MAX_FRAMES,
                "A fourth frame must be rejected at the cap");

        var tag = hive.saveWithFullMetadata(level.registryAccess());
        var restored = (BeehiveBlockEntity) BlockEntity.loadStatic(abs, hive.getBlockState(), tag, level.registryAccess());
        h.assertTrue(restored != null && BeeFrames.get(restored) == 3,
                "Frames must survive a real hive NBT save/load round trip");
        h.assertTrue(BeeFrames.takeAll(restored) == 3 && BeeFrames.get(restored) == 0,
                "Taking frames must clear the persisted attachment");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void framesInstallByInteractionReturnOnBreakAndIgnoreNonHives(GameTestHelper h) {
        var level = h.getLevel();
        var abs = h.absolutePos(HIVE);
        var hive = hive(h);
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        var hit = new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false);

        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(BeekeepingContent.BEE_FRAME.get(), 4));
        for (int attempt = 0; attempt < 3; attempt++) {
            var event = new PlayerInteractEvent.RightClickBlock(player, InteractionHand.MAIN_HAND, abs, hit);
            NeoForge.EVENT_BUS.post(event);
            h.assertTrue(event.isCanceled() && event.getCancellationResult() == InteractionResult.SUCCESS,
                    "Frame installation must be handled instead of falling through to vanilla");
        }
        h.assertTrue(BeeFrames.get(hive) == 3 && player.getMainHandItem().getCount() == 1,
                "Three interactions must install exactly three frames and consume three items");

        var full = new PlayerInteractEvent.RightClickBlock(player, InteractionHand.MAIN_HAND, abs, hit);
        NeoForge.EVENT_BUS.post(full);
        h.assertTrue(BeeFrames.get(hive) == 3 && player.getMainHandItem().getCount() == 1,
                "A full hive must keep the item instead of silently eating it");

        h.setBlock(STONE, Blocks.STONE);
        var stoneAbs = h.absolutePos(STONE);
        var stoneHit = new BlockHitResult(Vec3.atCenterOf(stoneAbs), Direction.UP, stoneAbs, false);
        var rejected = new PlayerInteractEvent.RightClickBlock(player, InteractionHand.MAIN_HAND, stoneAbs, stoneHit);
        NeoForge.EVENT_BUS.post(rejected);
        h.assertTrue(!rejected.isCanceled() && player.getMainHandItem().getCount() == 1,
                "Frames must not be installed into arbitrary blocks");

        NeoForge.EVENT_BUS.post(new BlockEvent.BreakEvent(level, abs, level.getBlockState(abs), player));
        h.assertTrue(BeeFrames.get(hive) == 0 && count(player, BeekeepingContent.BEE_FRAME.get()) == 1
                        && items(level, abs, BeekeepingContent.BEE_FRAME.get()) == 3,
                "Breaking a hive must recover every installed frame as an item");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void smokerHarvestsFullHiveSafelyAndCalmsBees(GameTestHelper h) {
        var level = h.getLevel();
        var abs = h.absolutePos(HIVE);
        h.setBlock(HIVE, Blocks.BEEHIVE.defaultBlockState().setValue(BeehiveBlock.HONEY_LEVEL, BeehiveBlock.MAX_HONEY_LEVELS));
        var hive = (BeehiveBlockEntity) level.getBlockEntity(abs);
        h.assertTrue(BeeFrames.install(hive) && BeeFrames.install(hive), "Two frames must be installed");

        var resident = EntityType.BEE.create(level);
        resident.setPos(abs.getX() + 0.5, abs.getY() + 0.5, abs.getZ() + 0.5);
        hive.addOccupant(resident);
        h.assertTrue(hive.getOccupantCount() == 1, "The colony must be stored inside the hive");

        var player = h.makeMockPlayer(GameType.SURVIVAL);
        var angry = angryBee(h, player, abs.offset(2, 0, 2));
        var smoker = new ItemStack(BeekeepingContent.BEE_SMOKER.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, smoker);
        var context = new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false));

        h.assertTrue(smoker.getItem().useOn(context) == InteractionResult.SUCCESS, "The smoker must handle a full hive");
        h.assertTrue(level.getBlockState(abs).getValue(BeehiveBlock.HONEY_LEVEL) == 0,
                "Smoked harvest must reset the hive honey level like a bottle harvest");
        h.assertTrue(count(player, Items.HONEY_BOTTLE) == 1, "A full hive must still yield one honey bottle");
        h.assertTrue(hive.getOccupantCount() == 1
                        && level.getEntitiesOfClass(Bee.class, new AABB(abs).inflate(3)).stream().noneMatch(bee -> bee != angry),
                "Smoking must not release the colony or spawn extra bees");
        h.assertTrue(combs(level, abs) == 2, "Each installed frame must add one honeycomb to the harvest");
        h.assertTrue(!angry.isAngry() && angry.getTarget() == null, "Smoke must calm angry bees around the hive");
        h.assertTrue(smoker.getDamageValue() == 1, "A smoked harvest must cost one durability");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void smokerOnEmptyHiveIsSafeAndFrameCountScalesTheHarvest(GameTestHelper h) {
        var level = h.getLevel();
        var abs = h.absolutePos(HIVE);
        var hive = hive(h);
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        var smoker = new ItemStack(BeekeepingContent.BEE_SMOKER.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, smoker);
        var context = new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false));

        h.assertTrue(smoker.getItem().useOn(context) == InteractionResult.SUCCESS, "Using a smoker on an empty hive must be harmless");
        h.assertTrue(count(player, Items.HONEY_BOTTLE) == 0 && count(player, Items.HONEYCOMB) == 0 && combs(level, abs) == 0,
                "An unripe hive must not produce honey or comb");
        h.assertTrue(level.getBlockState(abs).getValue(BeehiveBlock.HONEY_LEVEL) == 0 && BeeFrames.get(hive) == 0,
                "An unripe hive must keep its state");

        level.setBlock(abs, level.getBlockState(abs).setValue(BeehiveBlock.HONEY_LEVEL, BeehiveBlock.MAX_HONEY_LEVELS), 3);
        // Independent frameless-hive scenario uses a different player, not a cooldown bypass.
        player = h.makeMockPlayer(GameType.SURVIVAL);
        var second = new ItemStack(BeekeepingContent.BEE_SMOKER.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, second);
        h.assertTrue(second.getItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false))) == InteractionResult.SUCCESS,
                "A frameless full hive must still be harvestable");
        h.assertTrue(count(player, Items.HONEY_BOTTLE) == 1 && combs(level, abs) == 0,
                "Without frames a harvest must yield a bottle and no comb");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void smokerInAirCalmsBeesAndRespectsCooldown(GameTestHelper h) {
        var level = h.getLevel();
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        var bee = angryBee(h, player, new BlockPos((int) player.getX() + 2, (int) player.getY(), (int) player.getZ() + 2));
        var smoker = new ItemStack(BeekeepingContent.BEE_SMOKER.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, smoker);

        var first = smoker.getItem().use(level, player, InteractionHand.MAIN_HAND);
        h.assertTrue(!bee.isAngry() && bee.getTarget() == null, "Using the smoker in air must calm nearby angry bees");
        h.assertTrue(smoker.getDamageValue() == 1
                        && player.getCooldowns().isOnCooldown(BeekeepingContent.BEE_SMOKER.get()),
                "An air use must cost durability and start its cooldown");
        var second = smoker.getItem().use(level, player, InteractionHand.MAIN_HAND);
        h.assertTrue(second.getResult() == InteractionResult.FAIL && smoker.getDamageValue() == 1,
                "The cooldown must block repeated air uses without extra durability");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void smokerBoundsBeeUpdatesAndSharesHiveAirCooldown(GameTestHelper h) {
        var level = h.getLevel();
        var abs = h.absolutePos(HIVE);
        hive(h);
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        var bees = new java.util.ArrayList<Bee>();
        for (int n = 0; n < 72; n++) bees.add(angryBee(h, player, abs));
        var smoker = new ItemStack(BeekeepingContent.BEE_SMOKER.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, smoker);
        var context = new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false));
        h.assertTrue(smoker.getItem().useOn(context) == InteractionResult.SUCCESS, "First hive use succeeds");
        h.assertTrue(bees.stream().filter(bee -> !bee.isAngry()).count() == 64,
                "One use updates at most64bees, leaving the remainder for a later explicit use");
        h.assertTrue(smoker.getItem().useOn(context) == InteractionResult.FAIL && smoker.getDamageValue() == 1,
                "Repeated hive use must not query or damage again during cooldown");
        h.assertTrue(smoker.getItem().use(level, player, InteractionHand.MAIN_HAND).getResult() == InteractionResult.FAIL,
                "Hive cooldown also prevents switching to an air-use spam loop");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void honeyMealsCookWithContainerRemaindersAndFeedPlayers(GameTestHelper h) {
        var level = h.getLevel();
        h.setBlock(POT.below(), Blocks.MAGMA_BLOCK);
        h.setBlock(POT, CookingContent.POT.get());
        var pot = (CookingPotBlockEntity) level.getBlockEntity(h.absolutePos(POT));
        h.assertTrue(pot != null, "The real cooking pot block entity must exist");

        pot.setItem(0, new ItemStack(OrchardContent.PEAR.get()));
        pot.setItem(1, new ItemStack(Items.HONEY_BOTTLE));
        pot.setItem(2, new ItemStack(AgricultureContent.BARLEY.get()));
        pot.setItem(3, new ItemStack(Items.BOWL, 2));
        for (int step = 0; step < 14; step++) pot.cookStep();
        h.assertTrue(pot.getItem(4).is(CookingContent.HONEYED_PEAR.get()) && pot.getItem(4).getCount() == 1,
                "The real pot must cook honeyed pears from pear, honey and barley");
        h.assertTrue(pot.getItem(1).is(Items.GLASS_BOTTLE) && pot.getItem(3).getCount() == 1,
                "Cooking must return the honey bottle as a glass bottle and consume exactly one bowl");

        pot.setItem(4, ItemStack.EMPTY);
        pot.setItem(0, new ItemStack(EngineeringContent.FLOUR.get()));
        pot.setItem(1, new ItemStack(Items.HONEY_BOTTLE));
        pot.setItem(2, new ItemStack(Items.MILK_BUCKET));
        for (int step = 0; step < 18; step++) pot.cookStep();
        h.assertTrue(pot.getItem(4).is(CookingContent.HONEY_CAKE.get()), "The real pot must cook honey cake");
        h.assertTrue(pot.getItem(2).is(Items.BUCKET) && pot.getItem(1).is(Items.GLASS_BOTTLE),
                "Honey cake must return both the milk bucket and the glass bottle");

        var player = h.makeMockPlayer(GameType.SURVIVAL);
        player.getFoodData().setFoodLevel(0);
        var remaining = new ItemStack(CookingContent.HONEY_CAKE.get()).finishUsingItem(level, player);
        h.assertTrue(remaining.is(Items.BOWL) && player.getFoodData().getFoodLevel() == 10,
                "Eating a honey meal must restore its nutrition and return the bowl");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void recipesAppearInBrowserAndSurviveNetworkCodec(GameTestHelper h) {
        var level = h.getLevel();
        var displays = BrowserRecipes.recipeDisplays(level.getRecipeManager(), level.registryAccess(), ItemStack.EMPTY, List.of());
        var wax = displays.stream().filter(display -> display.id().toString().equals("reimagined:milling_honeycomb_wax"))
                .findFirst().orElse(null);
        h.assertTrue(wax != null && wax.uses(new ItemStack(Items.HONEYCOMB)) && wax.result().is(BeekeepingContent.BEESWAX.get())
                        && wax.station().is(EngineeringContent.MILLSTONE.get().asItem()),
                "The browser must link vanilla honeycomb to wax at the existing millstone");
        var honeyMeals = displays.stream().filter(display -> display.station().is(CookingContent.POT_ITEM.get())
                && (display.result().is(CookingContent.HONEYED_PEAR.get()) || display.result().is(CookingContent.HONEY_CAKE.get()))).toList();
        h.assertTrue(honeyMeals.size() == 2 && honeyMeals.stream().allMatch(display -> display.uses(new ItemStack(Items.BOWL))),
                "Both honey meals must appear as bowl cooking recipes");

        var stack = new ItemStack(BeekeepingContent.BEE_SMOKER.get());
        stack.setDamageValue(5);
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), level.registryAccess());
        try {
            ItemStack.STREAM_CODEC.encode(buffer, stack);
            var decoded = ItemStack.STREAM_CODEC.decode(buffer);
            h.assertTrue(decoded.is(BeekeepingContent.BEE_SMOKER.get()) && decoded.getDamageValue() == 5,
                    "The real item network codec must preserve beekeeping items and their damage");
        } finally {
            buffer.release();
        }

        for (String locale : List.of("en_us", "ru_ru")) {
            try (var stream = BeekeepingGameTests.class.getResourceAsStream("/assets/reimagined/lang/" + locale + ".json")) {
                h.assertTrue(stream != null, "Beekeeping language file must be packaged: " + locale);
                var language = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
                for (String key : List.of("block.reimagined.beeswax_block", "item.reimagined.beeswax", "item.reimagined.bee_frame",
                        "item.reimagined.bee_smoker", "item.reimagined.honeyed_pear", "item.reimagined.honey_cake",
                        "tooltip.reimagined.bee_frame", "tooltip.reimagined.bee_smoker", "message.reimagined.bee_frames_full",
                        "message.reimagined.bee_frames_installed", "message.reimagined.bee_smoker_idle",
                        "message.reimagined.bee_smoker_calm", "journal.reimagined.page.beekeeping")) {
                    h.assertTrue(language.has(key), "Beekeeping text must be localised: " + locale + " " + key);
                }
            } catch (java.io.IOException exception) {
                throw new RuntimeException(exception);
            }
        }
        h.succeed();
    }
}

