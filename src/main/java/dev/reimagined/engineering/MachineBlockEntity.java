package dev.reimagined.engineering;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class MachineBlockEntity extends BlockEntity implements WorldlyContainer {
    private static final int[] NO_SLOTS = {};
    private static final int[] INPUT_SLOTS = {0};
    private static final int[] OUTPUT_SLOTS = {1};
    private ItemStack input = ItemStack.EMPTY;
    private ItemStack output = ItemStack.EMPTY;
    private int progress;
    private String currentRecipe = "";
    private double rotationAnchor;
    private long rotationTick;
    private int rotationRpm;
    private long motionVersion;
    public MachineBlockEntity(BlockPos pos, BlockState state) { super(EngineeringContent.MACHINE.get(), pos, state); }
    public static void tick(Level level, BlockPos pos, BlockState state, MachineBlockEntity machine) {
        if (level.getGameTime() % 20 != Math.floorMod(pos.asLong(), 20)) return;
        if (!(level instanceof ServerLevel server)) return;
        Rotation network = MechanicalNetworks.rotation(server, pos);
        boolean running = network.running() && !MechanicalNetwork.redstonePowered(level, pos);
        if (!state.is(EngineeringContent.MILLSTONE.get()) || !running || machine.input.isEmpty()) return;
        var recipe = level.getRecipeManager().getRecipeFor(EngineeringContent.MILLING.get(), new SingleRecipeInput(machine.input), level);
        if (recipe.isEmpty()) {
            if (machine.progress != 0 || !machine.currentRecipe.isEmpty()) { machine.resetProgress(); machine.setChanged(); }
            return;
        }
        var holder = recipe.get();
        ItemStack result = holder.value().getResultItem(level.registryAccess());
        if (result.getCount() > machine.getMaxStackSize(result)) return;
        if (!machine.output.isEmpty() && (!ItemStack.isSameItemSameComponents(machine.output, result)
                || machine.output.getCount() + result.getCount() > machine.getMaxStackSize(machine.output))) return;
        String id = holder.id().toString();
        if (!machine.currentRecipe.equals(id)) { machine.progress = 0; machine.currentRecipe = id; }
        if (++machine.progress >= holder.value().seconds()) {
            if (machine.output.isEmpty()) machine.output = result.copy(); else machine.output.grow(result.getCount());
            machine.input.shrink(1);
            machine.progress = 0;
        }
        machine.setChanged();
    }
    @Override public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel server) {
            // Saved motion is frozen until the currently loaded topology is evaluated.
            if (rotationRpm == 0) rotationTick = level.getGameTime();
            MechanicalNetworks.request(server, worldPosition);
        }
    }
    @Override public void setRemoved() {
        if (level instanceof ServerLevel server) MechanicalNetworks.invalidate(server, worldPosition);
        super.setRemoved();
    }
    public int rotationSpeed() { return rotationRpm; }
    public long motionVersion() { return motionVersion; }
    public double rotationAngle(float partialTick) {
        return RotationAnimation.angle(rotationAnchor, rotationTick, rotationRpm,
                level == null ? rotationTick : level.getGameTime(), partialTick);
    }
    /** One common snapshot for connected nodes; unchanged motion sends/saves nothing. */
    public boolean applyRotation(int rpm, double angle, long tick) {
        if (!(level instanceof ServerLevel) || isRemoved() || !level.hasChunkAt(worldPosition)
                || level.getBlockState(worldPosition).getBlock() != getBlockState().getBlock()) return false;
        rpm = rpm == 0 ? 0 : 16; // Current mechanical contract is a fixed positive 16 RPM, 1:1 drive.
        if (rotationRpm == rpm && getBlockState().getValue(MechanicalBlock.ACTIVE) == (rpm != 0)
                && RotationAnimation.difference(rotationAngle(0), angle) < 0.01) return false;
        rotationAnchor = RotationAnimation.normalize(angle);
        rotationTick = tick;
        rotationRpm = rpm;
        motionVersion++;
        setChanged();
        BlockState state = getBlockState();
        BlockState updated = state.setValue(MechanicalBlock.ACTIVE, rpm != 0);
        if (state != updated) level.setBlock(worldPosition, updated, Block.UPDATE_CLIENTS);
        // Only tracking clients receive a small motion-only tag, never the mill inventory.
        level.sendBlockUpdated(worldPosition, updated, updated, Block.UPDATE_CLIENTS);
        return true;
    }
    private CompoundTag motionTag() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("MotionVersion", 1);
        tag.putDouble("Angle", rotationAnchor);
        tag.putLong("MotionTick", rotationTick);
        tag.putInt("Rpm", rotationRpm);
        return tag;
    }
    private void receiveMotion(CompoundTag tag) {
        double angle = tag.getDouble("Angle");
        rotationAnchor = Double.isFinite(angle) ? RotationAnimation.normalize(angle) : 0;
        rotationTick = tag.getLong("MotionTick");
        rotationRpm = tag.getInt("Rpm") == 16 ? 16 : 0;
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return motionTag(); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) { receiveMotion(tag); }
    @Override public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet, HolderLookup.Provider registries) {
        receiveMotion(packet.getTag());
    }
    public boolean isMillstone() { return getBlockState().is(EngineeringContent.MILLSTONE.get()); }
    @Override public int getContainerSize() { return isMillstone() ? 2 : 0; }
    @Override public int getMaxStackSize() { return 64; }
    @Override public boolean isEmpty() { return input.isEmpty() && output.isEmpty(); }
    @Override public ItemStack getItem(int slot) {
        return !isMillstone() ? ItemStack.EMPTY : switch (slot) { case 0 -> input; case 1 -> output; default -> ItemStack.EMPTY; };
    }
    @Override public ItemStack removeItem(int slot, int amount) {
        if (amount <= 0) return ItemStack.EMPTY;
        ItemStack stack = getItem(slot);
        if (stack.isEmpty()) return ItemStack.EMPTY;
        ItemStack removed = stack.split(amount);
        if (slot == 0 && input.isEmpty()) resetProgress();
        setChanged();
        return removed;
    }
    @Override public ItemStack removeItemNoUpdate(int slot) {
        ItemStack removed = getItem(slot);
        if (slot == 0) { input = ItemStack.EMPTY; resetProgress(); }
        else if (slot == 1) output = ItemStack.EMPTY;
        return removed;
    }
    @Override public void setItem(int slot, ItemStack stack) {
        if (!isMillstone() || slot < 0 || slot > 1) return;
        ItemStack bounded = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(Math.min(stack.getCount(), getMaxStackSize(stack)));
        if (getItem(slot).getCount() == bounded.getCount() && ItemStack.isSameItemSameComponents(getItem(slot), bounded)) return;
        if (slot == 0) {
            if (bounded.isEmpty() || !ItemStack.isSameItemSameComponents(input, bounded)) resetProgress();
            input = bounded;
        } else output = bounded;
        setChanged();
    }
    @Override public boolean stillValid(Player player) { return isMillstone() && Container.stillValidBlockEntity(this, player); }
    @Override public void clearContent() {
        if (input.isEmpty() && output.isEmpty() && progress == 0 && currentRecipe.isEmpty()) return;
        input = ItemStack.EMPTY; output = ItemStack.EMPTY; resetProgress(); setChanged();
    }
    private void resetProgress() { progress = 0; currentRecipe = ""; }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) {
        return isMillstone() && slot == 0 && !stack.isEmpty() && level != null
                && level.getRecipeManager().getRecipeFor(EngineeringContent.MILLING.get(), new SingleRecipeInput(stack), level).isPresent();
    }
    @Override public int[] getSlotsForFace(Direction side) {
        return !isMillstone() ? NO_SLOTS : side == Direction.DOWN ? OUTPUT_SLOTS : INPUT_SLOTS;
    }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        return side != Direction.DOWN && canPlaceItem(slot, stack);
    }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return isMillstone() && side == Direction.DOWN && slot == 1;
    }
    public void interact(Player player, ItemStack held) {
        if (level == null || !isMillstone()) return;
        player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.reimagined.mill_status",
                input.getHoverName(), input.getCount(), output.getHoverName(), output.getCount(), progress), false);
        if (held.isEmpty()) {
            int slot = output.isEmpty() ? 0 : 1;
            ItemStack removed = removeItem(slot, getMaxStackSize());
            if (!removed.isEmpty()) player.getInventory().placeItemBackInInventory(removed);
            return;
        }
        if (!canPlaceItem(0, held) || !input.isEmpty() && !ItemStack.isSameItemSameComponents(input, held)) return;
        int count = Math.min(getMaxStackSize(held) - input.getCount(), held.getCount());
        if (count <= 0) return;
        ItemStack updated = held.copyWithCount(input.getCount() + count);
        setItem(0, updated);
        held.shrink(count);
    }
    public void dropContents() {
        if (level == null || level.isClientSide) return;
        Containers.dropItemStack(level, worldPosition.getX() + .5, worldPosition.getY() + .5, worldPosition.getZ() + .5, input);
        Containers.dropItemStack(level, worldPosition.getX() + .5, worldPosition.getY() + .5, worldPosition.getZ() + .5, output);
        clearContent();
    }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!input.isEmpty()) tag.put("Input", input.save(registries));
        if (!output.isEmpty()) tag.put("Output", output.save(registries));
        tag.putInt("Progress", progress); tag.putString("Recipe", currentRecipe);
        tag.putInt("MotionVersion", 1);
        tag.putDouble("SavedAngle", rotationAngle(0));
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        double savedAngle = tag.getDouble("SavedAngle");
        rotationAnchor = Double.isFinite(savedAngle) ? RotationAnimation.normalize(savedAngle) : 0;
        rotationRpm = 0; rotationTick = level == null ? 0 : level.getGameTime();
        input = ItemStack.parseOptional(registries, tag.getCompound("Input"));
        if (!input.isEmpty()) input.setCount(Math.min(input.getCount(), getMaxStackSize(input)));
        output = ItemStack.parseOptional(registries, tag.getCompound("Output"));
        if (!output.isEmpty()) output.setCount(Math.min(output.getCount(), getMaxStackSize(output)));
        progress = Math.max(0, tag.getInt("Progress")); currentRecipe = tag.getString("Recipe");
    }
}
