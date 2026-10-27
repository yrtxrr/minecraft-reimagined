package dev.reimagined.cooking;

import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;

public final class CookingPotMenu extends AbstractContainerMenu {
    private final Container pot;
    private final ContainerData data;
    public CookingPotMenu(int id, Inventory inventory) { this(id,inventory,new SimpleContainer(5),new SimpleContainerData(3)); }
    public CookingPotMenu(int id, Inventory inventory, Container pot, ContainerData data) {
        super(CookingContent.MENU.get(), id); checkContainerSize(pot,5); checkContainerDataCount(data,3);
        this.pot = pot; this.data = data; pot.startOpen(inventory.player);
        for (int i = 0; i < 3; i++) addSlot(new Slot(pot,i,26+i*18,30) {
            @Override public boolean mayPlace(ItemStack stack) { return !stack.is(Items.BOWL) && pot.canPlaceItem(getContainerSlot(),stack); }
        });
        addSlot(new Slot(pot,3,98,30) { @Override public boolean mayPlace(ItemStack stack) { return stack.is(Items.BOWL); } });
        addSlot(new Slot(pot,4,134,30) { @Override public boolean mayPlace(ItemStack stack) { return false; } });
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) addSlot(new Slot(inventory,col+row*9+9,8+col*18,76+row*18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory,col,8+col*18,134));
        addDataSlots(data);
    }
    public int progress() { return data.get(0); }
    public int total() { return data.get(1); }
    public boolean heated() { return data.get(2) != 0; }
    @Override public boolean stillValid(Player player) { return pot.stillValid(player); }
    @Override public void removed(Player player) { super.removed(player); pot.stopOpen(player); }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index); if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem(), original = stack.copy();
        if (index < 5) {
            if (!moveItemStackTo(stack,5,slots.size(),true)) return ItemStack.EMPTY;
        } else if (stack.is(Items.BOWL)) {
            if (!moveItemStackTo(stack,3,4,false)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack,0,3,false)) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player,stack); return original;
    }
}
