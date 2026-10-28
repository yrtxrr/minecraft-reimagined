package dev.reimagined.magic;

import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Four saved slots, zero tickers/queues. Actual work is authorized by a nearby player's menu action. */
public final class MagicStationBlockEntity extends BaseContainerBlockEntity {
    private NonNullList<ItemStack> items=NonNullList.withSize(4,ItemStack.EMPTY);
    public MagicStationBlockEntity(BlockPos pos,BlockState state) { super(MagicContent.STATION.get(),pos,state); }
    public boolean research() { return getBlockState().getBlock() instanceof MagicStationBlock block && block.research; }
    @Override protected NonNullList<ItemStack> getItems() { return items; }
    @Override protected void setItems(NonNullList<ItemStack> items) { this.items=items; }
    @Override public boolean stillValid(net.minecraft.world.entity.player.Player player) {
        return player.level()==level && MagicStationBlock.loaded(player.level(),worldPosition)==this
                && player.distanceToSqr(worldPosition.getX()+.5,worldPosition.getY()+.5,worldPosition.getZ()+.5)<=64;
    }
    @Override public int getContainerSize() { return 4; }
    @Override public boolean canPlaceItem(int slot,ItemStack stack) { return slot>=0 && slot<3; }
    @Override protected Component getDefaultName() { return Component.translatable(research()?"block.reimagined.research_table":"block.reimagined.arcane_altar"); }
    @Override protected AbstractContainerMenu createMenu(int id,Inventory inventory) { return new MagicStationMenu(id,inventory,this,research()); }
    @Override public void clearContent() { super.clearContent();setChanged(); }
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider registries) { super.saveAdditional(tag,registries);ContainerHelper.saveAllItems(tag,items,registries); }
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider registries) { super.loadAdditional(tag,registries);items=NonNullList.withSize(4,ItemStack.EMPTY);ContainerHelper.loadAllItems(tag,items,registries); }
}
