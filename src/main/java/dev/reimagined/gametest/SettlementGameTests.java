package dev.reimagined.gametest;

import dev.reimagined.settlements.SettlementContent;
import dev.reimagined.settlements.SettlementData;
import dev.reimagined.settlements.Settler;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

/** SettlementData persistence: save/load settler home, work, profession, tradeTick, stock. */
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
}