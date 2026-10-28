package dev.reimagined.smithing;

import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Seven persisted slots; no block/entity ticker, recipe queue, heat simulation or offline catch-up. */
public final class ForgeBlockEntity extends BaseContainerBlockEntity {
    private NonNullList<ItemStack> items=NonNullList.withSize(7,ItemStack.EMPTY);
    public ForgeBlockEntity(BlockPos pos,BlockState state){super(SmithingContent.ENTITY.get(),pos,state);}
    @Override protected NonNullList<ItemStack> getItems(){return items;}
    @Override protected void setItems(NonNullList<ItemStack> value){items=value;}
    @Override public int getContainerSize(){return 7;}
    @Override protected Component getDefaultName(){return Component.translatable("block.reimagined.smithing_forge");}
    @Override protected AbstractContainerMenu createMenu(int id,Inventory inventory){return new ForgeMenu(id,inventory,this);}
    @Override public boolean stillValid(Player player){return player.level()==level && ForgeBlock.loaded(player.level(),worldPosition)==this && player.distanceToSqr(worldPosition.getX()+.5,worldPosition.getY()+.5,worldPosition.getZ()+.5)<=64;}
    @Override public boolean canPlaceItem(int slot,ItemStack stack){return switch(slot){case 0,1,2->true;case 3->stack.is(net.minecraft.tags.ItemTags.COALS);case 4->stack.is(SmithingContent.HAMMERS) && stack.isDamageableItem();case 5->stack.is(SmithingContent.BELLOWS.get());default->false;};}
    @Override public void clearContent(){super.clearContent();setChanged();}
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider registries){super.saveAdditional(tag,registries);ContainerHelper.saveAllItems(tag,items,registries);}
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider registries){super.loadAdditional(tag,registries);items=NonNullList.withSize(7,ItemStack.EMPTY);ContainerHelper.loadAllItems(tag,items,registries);}
}
