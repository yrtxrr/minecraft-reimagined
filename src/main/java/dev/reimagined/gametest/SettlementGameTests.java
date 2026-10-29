package dev.reimagined.gametest;

import dev.reimagined.settlements.SettlementContent;
import dev.reimagined.settlements.SettlementData;
import dev.reimagined.settlements.Settler;
import dev.reimagined.settlements.TradeData;
import dev.reimagined.settlements.TradeOffer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

/** SettlementData persistence and trading: save/load settler home, work, profession, tradeTick, stock. */
@GameTestHolder("reimagined")
@PrefixGameTestTemplate(false)
public final class SettlementGameTests {
    private SettlementGameTests() {}

    @GameTest(template = "empty", timeoutTicks = 60)
    public static void settlerDataPersistsAcrossSaveLoad(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var data = SettlementData.get(level);
        data.clear();

        UUID uuid = UUID.randomUUID();
        BlockPos home = new BlockPos(100, 64, 200);
        BlockPos work = new BlockPos(150, 64, 250);
        String profession = "blacksmith";
        int tradeTick = 42;
        int stock = 17;

        data.registerSettler(uuid, home, work, profession);
        SettlementData.SettlerRecord record = data.findSettler(uuid);
        assert record != null;
        record.tradeTick = tradeTick;
        record.stock = stock;

        // Save to NBT
        CompoundTag tag = new CompoundTag();
        data.save(tag, level.registryAccess());

        // Load into new SettlementData instance
        SettlementData loaded = SettlementData.load(tag, level.registryAccess());

        // Verify all fields preserved
        SettlementData.SettlerRecord loadedRecord = loaded.findSettler(uuid);
        helper.assertTrue(loadedRecord != null, "Settler must be found after save/load");
        helper.assertTrue(loadedRecord.uuid.equals(uuid), "UUID must survive save/load");
        helper.assertTrue(loadedRecord.home.equals(home), "Home position must survive save/load");
        helper.assertTrue(loadedRecord.work.equals(work), "Work position must survive save/load");
        helper.assertTrue(profession.equals(loadedRecord.profession), "Profession must survive save/load");
        helper.assertTrue(loadedRecord.tradeTick == tradeTick, "TradeTick must survive save/load");
        helper.assertTrue(loadedRecord.stock == stock, "Stock must survive save/load");

        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 60)
    public static void multipleSettlersPersistAcrossSaveLoad(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var data = SettlementData.get(level);
        data.clear();

        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();

        data.registerSettler(uuid1, new BlockPos(100, 64, 200), new BlockPos(150, 64, 250), "farmer");
        data.registerSettler(uuid2, new BlockPos(300, 64, 400), new BlockPos(350, 64, 450), "fisher");

        SettlementData.SettlerRecord r1 = data.findSettler(uuid1);
        SettlementData.SettlerRecord r2 = data.findSettler(uuid2);
        r1.tradeTick = 5;
        r1.stock = 10;
        r2.tradeTick = 12;
        r2.stock = 3;

        // Save to NBT
        CompoundTag tag = new CompoundTag();
        data.save(tag, level.registryAccess());

        // Load into new SettlementData instance
        SettlementData loaded = SettlementData.load(tag, level.registryAccess());

        // Verify both settlers
        helper.assertTrue(loaded.settlers().size() == 2, "Both settlers must survive save/load");

        SettlementData.SettlerRecord loaded1 = loaded.findSettler(uuid1);
        SettlementData.SettlerRecord loaded2 = loaded.findSettler(uuid2);

        helper.assertTrue(loaded1 != null && loaded2 != null, "Both settlers must be found");
        helper.assertTrue("farmer".equals(loaded1.profession), "First profession must survive");
        helper.assertTrue("fisher".equals(loaded2.profession), "Second profession must survive");
        helper.assertTrue(loaded1.tradeTick == 5 && loaded1.stock == 10, "First trade data must survive");
        helper.assertTrue(loaded2.tradeTick == 12 && loaded2.stock == 3, "Second trade data must survive");

        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 60)
    public static void removedSettlerDoesNotReappearAfterLoad(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var data = SettlementData.get(level);
        data.clear();

        UUID uuid = UUID.randomUUID();
        data.registerSettler(uuid, new BlockPos(0, 64, 0), new BlockPos(10, 64, 10), "commoner");

        data.removeSettler(uuid);

        // Save to NBT
        CompoundTag tag = new CompoundTag();
        data.save(tag, level.registryAccess());

        // Load into new SettlementData instance
        SettlementData loaded = SettlementData.load(tag, level.registryAccess());

        helper.assertTrue(loaded.settlers().isEmpty(), "No settlers should remain after removal");
        helper.assertTrue(loaded.findSettler(uuid) == null, "Removed settler must not reappear after load");

        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void settlerEntityHomeWorkPersistAcrossSaveLoad(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var data = SettlementData.get(level);
        data.clear();

        UUID uuid = UUID.randomUUID();
        BlockPos home = new BlockPos(10, 2, 10);
        BlockPos work = new BlockPos(20, 2, 20);

        // Spawn a settler and register it
        Settler settler = helper.spawn(SettlementContent.SETTLER.get(), home);
        settler.setHome(home);
        settler.setWork(work);
        data.registerSettler(uuid, home, work, "blacksmith");

        // Save settler entity NBT
        CompoundTag entityTag = settler.saveWithoutId(new CompoundTag());

        // Load into new entity
        Settler restored = SettlementContent.SETTLER.get().create(level);
        helper.assertTrue(restored != null, "Settler entity type must create an instance");
        restored.load(entityTag);

        // Verify home and work positions survived
        helper.assertTrue(restored.homePos().equals(home), "Settler homePos must survive save/load");
        helper.assertTrue(restored.workPos().equals(work), "Settler workPos must survive save/load");

        helper.succeed();
    }

    // ===== Trading Tests =====

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void tradeOffersInitializedForProfession(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var data = SettlementData.get(level);
        data.clear();
        TradeData.get(level).clear();

        UUID uuid = UUID.randomUUID();
        BlockPos home = new BlockPos(0, 64, 0);
        BlockPos work = new BlockPos(10, 64, 10);

        // Register settler as farmer
        data.registerSettler(uuid, home, work, "farmer");

        // Initialize trade offers
        TradeData.initializeOffersForProfession(level, "farmer");

        var tradeData = TradeData.get(level);
        var offers = tradeData.offers().stream().filter(o -> o.profession.equals("farmer")).toList();

        helper.assertTrue(offers.size() >= 6, "Farmer should have at least 6 trade offers");

        // Verify specific offers exist
        boolean hasWheat = offers.stream().anyMatch(o -> o.input.is(Items.WHEAT) && o.output.is(Items.EMERALD));
        boolean hasBread = offers.stream().anyMatch(o -> o.input.is(Items.EMERALD) && o.output.is(Items.BREAD));

        helper.assertTrue(hasWheat, "Farmer should buy wheat for emeralds");
        helper.assertTrue(hasBread, "Farmer should sell bread for emeralds");

        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void tradeOfferStockLimitedAndDecrements(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var data = SettlementData.get(level);
        data.clear();
        TradeData.get(level).clear();

        UUID uuid = UUID.randomUUID();
        BlockPos home = new BlockPos(0, 64, 0);
        data.registerSettler(uuid, home, home, "farmer");
        TradeData.initializeOffersForProfession(level, "farmer");

        var tradeData = TradeData.get(level);
        var offer = tradeData.findOffer("farmer", new ItemStack(Items.WHEAT, 20));

        helper.assertTrue(offer != null, "Wheat trade offer must exist");
        int initialUses = offer.uses();
        int maxUses = offer.maxUses;

        helper.assertTrue(initialUses == 0, "Initial uses should be 0");
        helper.assertTrue(maxUses > 0, "Max uses should be positive");

        // Use the trade once
        offer.use();
        helper.assertTrue(offer.uses() == 1, "Uses should increment after trade");
        helper.assertTrue(offer.canTrade(), "Should still be able to trade");

        // Exhaust the trade
        for (int i = 1; i < maxUses; i++) {
            offer.use();
        }
        helper.assertTrue(offer.uses() == maxUses, "Uses should reach max");
        helper.assertTrue(!offer.canTrade(), "Should not be able to trade when exhausted");

        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void tradeOfferRestocksAtNewDay(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var data = SettlementData.get(level);
        data.clear();
        TradeData.get(level).clear();

        UUID uuid = UUID.randomUUID();
        BlockPos home = new BlockPos(0, 64, 0);
        data.registerSettler(uuid, home, home, "farmer");
        TradeData.initializeOffersForProfession(level, "farmer");

        var tradeData = TradeData.get(level);
        var offer = tradeData.findOffer("farmer", new ItemStack(Items.WHEAT, 20));

        // Exhaust the trade
        for (int i = 0; i < offer.maxUses; i++) {
            offer.use();
        }
        helper.assertTrue(!offer.canTrade(), "Trade should be exhausted");

        // Manually restock (simulates the daily restock at 8:00)
        offer.restock();
        helper.assertTrue(offer.uses() == 0, "Uses should reset to 0 after restock");
        helper.assertTrue(offer.canTrade(), "Should be able to trade after restock");

        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void tradeDataPersistsAcrossSaveLoad(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var data = SettlementData.get(level);
        data.clear();
        TradeData.get(level).clear();

        UUID uuid = UUID.randomUUID();
        BlockPos home = new BlockPos(0, 64, 0);
        data.registerSettler(uuid, home, home, "blacksmith");
        TradeData.initializeOffersForProfession(level, "blacksmith");

        var tradeData = TradeData.get(level);
        var offer = tradeData.findOffer("blacksmith", new ItemStack(Items.IRON_INGOT, 4));
        helper.assertTrue(offer != null, "Iron ingot trade offer must exist");

        // Use the trade a few times
        offer.use();
        offer.use();
        int usesBeforeSave = offer.uses();

        // Save to NBT
        CompoundTag tag = new CompoundTag();
        tradeData.save(tag, level.registryAccess());

        // Load into new TradeData instance
        TradeData loaded = TradeData.load(tag, level.registryAccess());
        var loadedOffer = loaded.findOffer("blacksmith", new ItemStack(Items.IRON_INGOT, 4));

        helper.assertTrue(loadedOffer != null, "Offer must survive save/load");
        helper.assertTrue(loadedOffer.uses() == usesBeforeSave, "Uses must survive save/load");
        helper.assertTrue(loadedOffer.maxUses == offer.maxUses, "Max uses must survive save/load");
        helper.assertTrue(loadedOffer.profession.equals("blacksmith"), "Profession must survive save/load");
        helper.assertTrue(ItemStack.isSameItemSameComponents(loadedOffer.input, offer.input), "Input must survive save/load");
        helper.assertTrue(ItemStack.isSameItemSameComponents(loadedOffer.output, offer.output), "Output must survive save/load");

        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 120)
    public static void multipleSettlersHaveIndependentTradeData(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var data = SettlementData.get(level);
        data.clear();
        TradeData.get(level).clear();

        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();

        data.registerSettler(uuid1, new BlockPos(0, 64, 0), new BlockPos(10, 64, 10), "farmer");
        data.registerSettler(uuid2, new BlockPos(100, 64, 100), new BlockPos(110, 64, 110), "fisher");

        TradeData.initializeOffersForProfession(level, "farmer");
        TradeData.initializeOffersForProfession(level, "fisher");

        var tradeData = TradeData.get(level);

        // Verify farmer offers
        var farmerOffers = tradeData.offers().stream().filter(o -> o.profession.equals("farmer")).toList();
        var fisherOffers = tradeData.offers().stream().filter(o -> o.profession.equals("fisher")).toList();

        helper.assertTrue(farmerOffers.size() >= 6, "Farmer should have offers");
        helper.assertTrue(fisherOffers.size() >= 5, "Fisher should have offers");

        // Use a farmer trade
        var wheatOffer = tradeData.findOffer("farmer", new ItemStack(Items.WHEAT, 20));
        var codOffer = tradeData.findOffer("fisher", new ItemStack(Items.COD, 10));

        helper.assertTrue(wheatOffer != null, "Wheat offer must exist");
        helper.assertTrue(codOffer != null, "Cod offer must exist");

        wheatOffer.use();

        // Fisher trades should be unaffected
        helper.assertTrue(wheatOffer.uses() == 1, "Farmer trade should be used");
        helper.assertTrue(codOffer.uses() == 0, "Fisher trade should be untouched");

        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void commonerHasDefaultTrades(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var data = SettlementData.get(level);
        data.clear();
        TradeData.get(level).clear();

        UUID uuid = UUID.randomUUID();
        BlockPos home = new BlockPos(0, 64, 0);
        data.registerSettler(uuid, home, home, "commoner");
        TradeData.initializeOffersForProfession(level, "commoner");

        var tradeData = TradeData.get(level);
        var offers = tradeData.offers().stream().filter(o -> o.profession.equals("commoner")).toList();

        helper.assertTrue(offers.size() >= 3, "Commoner should have at least 3 trade offers");

        boolean hasStick = offers.stream().anyMatch(o -> o.input.is(Items.STICK) && o.output.is(Items.EMERALD));
        boolean hasCoal = offers.stream().anyMatch(o -> o.input.is(Items.COAL) && o.output.is(Items.EMERALD));
        boolean hasApple = offers.stream().anyMatch(o -> o.input.is(Items.EMERALD) && o.output.is(Items.APPLE));

        helper.assertTrue(hasStick, "Commoner should buy sticks for emeralds");
        helper.assertTrue(hasCoal, "Commoner should buy coal for emeralds");
        helper.assertTrue(hasApple, "Commoner should sell apples for emeralds");

        helper.succeed();
    }
}
