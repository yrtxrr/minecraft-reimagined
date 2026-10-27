package dev.reimagined.gametest;

import dev.reimagined.engineering.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("reimagined")
@PrefixGameTestTemplate(false)
public final class MechanicalMotionGameTests {
    @GameTest(template = "empty")
    public static void motionPacketsExcludeInventoryAndUnchangedSnapshotsAreSilent(GameTestHelper h) {
        BlockPos p = new BlockPos(3, 3, 3);
        h.setBlock(p, EngineeringContent.MILLSTONE.get());
        var machine = (MachineBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(p));
        machine.setItem(0, new ItemStack(Items.WHEAT, 9));
        long now = h.getLevel().getGameTime();
        h.assertTrue(machine.applyRotation(16, 42, now), "First motion update must be applied");
        long version = machine.motionVersion();
        for (int i = 0; i < 100; i++)
            h.assertTrue(!machine.applyRotation(16, 42, now), "Identical snapshots must not dirty/send again");
        h.assertTrue(machine.motionVersion() == version, "No extra motion notifications");
        CompoundTag tag = machine.getUpdateTag(h.getLevel().registryAccess());
        h.assertTrue(tag.getInt("Rpm") == 16 && !tag.contains("Input") && !tag.contains("Output")
            && !tag.contains("Progress"), "Tracking payload must contain motion only");
        var replica = new MachineBlockEntity(h.absolutePos(p), machine.getBlockState());
        replica.handleUpdateTag(tag, h.getLevel().registryAccess());
        h.assertTrue(replica.rotationSpeed() == 16, "Initial chunk synchronization must restore speed");
        tag.putDouble("Angle", Double.NaN); tag.putInt("Rpm", Integer.MAX_VALUE);
        replica.handleUpdateTag(tag, h.getLevel().registryAccess());
        h.assertTrue(replica.rotationSpeed() == 0 && Double.isFinite(replica.rotationAngle(0)), "Malformed motion must fail closed");
        h.succeed();
    }
    @GameTest(template = "empty")
    public static void savedMachineRetainsInventoryAndFreezesUntilTopologyRevalidation(GameTestHelper h) {
        BlockPos p = new BlockPos(3, 3, 3);
        h.setBlock(p, EngineeringContent.MILLSTONE.get());
        var machine = (MachineBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(p));
        machine.setItem(0, new ItemStack(Items.WHEAT, 7));
        machine.setItem(1, new ItemStack(EngineeringContent.FLOUR.get(), 3));
        machine.applyRotation(16, 73, h.getLevel().getGameTime());
        var saved = machine.saveWithFullMetadata(h.getLevel().registryAccess());
        var restored = (MachineBlockEntity) BlockEntity.loadStatic(h.absolutePos(p), machine.getBlockState(), saved, h.getLevel().registryAccess());
        h.assertTrue(restored != null && restored.rotationSpeed() == 0
            && RotationAnimation.difference(restored.rotationAngle(0), 73) < .01,
            "Persist angle but never resume an unvalidated saved running state");
        h.assertTrue(restored.getItem(0).is(Items.WHEAT) && restored.getItem(0).getCount() == 7
            && restored.getItem(1).getCount() == 3, "Existing inventory NBT must remain compatible");
        h.succeed();
    }
}
