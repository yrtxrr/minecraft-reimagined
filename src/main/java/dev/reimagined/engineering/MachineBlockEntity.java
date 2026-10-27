package dev.reimagined.engineering;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class MachineBlockEntity extends BlockEntity {
    private ItemStack input = ItemStack.EMPTY;
    private ItemStack output = ItemStack.EMPTY;
    private int progress;
    private String currentRecipe = "";
    public MachineBlockEntity(BlockPos pos, BlockState state) { super(EngineeringContent.MACHINE.get(), pos, state); }
    public static void tick(Level level, BlockPos pos, BlockState state, MachineBlockEntity machine) {
        if (level.getGameTime() % 20 != Math.floorMod(pos.asLong(), 20)) return;
        boolean running = state.is(EngineeringContent.WATER_WHEEL.get())
                ? MechanicalNetwork.powered(level, pos, state)
                : !level.hasNeighborSignal(pos) && MechanicalNetwork.inspect(level, pos).running();
        if (state.getValue(MechanicalBlock.ACTIVE) != running)
            level.setBlock(pos, state.setValue(MechanicalBlock.ACTIVE, running), 3);
        if (!state.is(EngineeringContent.MILLSTONE.get()) || !running || machine.input.isEmpty()) return;
        var recipe = level.getRecipeManager().getRecipeFor(EngineeringContent.MILLING.get(), new SingleRecipeInput(machine.input), level);
        if (recipe.isEmpty()) { machine.progress = 0; machine.setChanged(); return; }
        var holder = recipe.get();
        ItemStack result = holder.value().getResultItem(level.registryAccess());
        if (!machine.output.isEmpty() && (!ItemStack.isSameItemSameComponents(machine.output, result)
                || machine.output.getCount() + result.getCount() > machine.output.getMaxStackSize())) return;
        String id = holder.id().toString();
        if (!machine.currentRecipe.equals(id)) { machine.progress = 0; machine.currentRecipe = id; }
        if (++machine.progress >= holder.value().seconds()) {
            if (machine.output.isEmpty()) machine.output = result.copy(); else machine.output.grow(result.getCount());
            machine.input.shrink(1);
            machine.progress = 0;
        }
        machine.setChanged();
    }
    public void interact(Player player, ItemStack held) {
        if (level == null) return;
        player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.reimagined.mill_status",
                input.getHoverName(), input.getCount(), output.getHoverName(), output.getCount(), progress), false);
        if (held.isEmpty()) {
            ItemStack removed = output.isEmpty() ? input : output;
            if (!removed.isEmpty()) {
                player.getInventory().placeItemBackInInventory(removed.copy());
                if (output.isEmpty()) { input = ItemStack.EMPTY; progress = 0; } else output = ItemStack.EMPTY;
                setChanged();
            }
            return;
        }
        if (level.getRecipeManager().getRecipeFor(EngineeringContent.MILLING.get(), new SingleRecipeInput(held), level).isEmpty()) return;
        if (!input.isEmpty() && !ItemStack.isSameItemSameComponents(input, held)) return;
        int room = held.getMaxStackSize() - input.getCount();
        int count = Math.min(room, held.getCount());
        if (count <= 0) return;
        if (input.isEmpty()) input = held.copyWithCount(count); else input.grow(count);
        held.shrink(count); setChanged();
    }
    public void dropContents() {
        if (level == null || level.isClientSide) return;
        Containers.dropItemStack(level, worldPosition.getX() + .5, worldPosition.getY() + .5, worldPosition.getZ() + .5, input);
        Containers.dropItemStack(level, worldPosition.getX() + .5, worldPosition.getY() + .5, worldPosition.getZ() + .5, output);
        input = ItemStack.EMPTY; output = ItemStack.EMPTY;
    }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!input.isEmpty()) tag.put("Input", input.save(registries));
        if (!output.isEmpty()) tag.put("Output", output.save(registries));
        tag.putInt("Progress", progress); tag.putString("Recipe", currentRecipe);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        input = ItemStack.parseOptional(registries, tag.getCompound("Input"));
        output = ItemStack.parseOptional(registries, tag.getCompound("Output"));
        progress = Math.max(0, tag.getInt("Progress")); currentRecipe = tag.getString("Recipe");
    }
}
