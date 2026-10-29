package dev.reimagined.settlements;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;

/** Vanilla server-authoritative offers, selection packets and payment/result slots. */
public final class TradingMenu extends MerchantMenu {
    private final Settler settler;
    public TradingMenu(int id, Inventory inventory, Settler settler) {
        super(id,inventory,settler); this.settler=settler;
    }
    @Override public boolean stillValid(Player player) {
        return super.stillValid(player) && settler.isAlive() && player.level()==settler.level()
            && player.distanceToSqr(settler)<=64 && !player.isSpectator();
    }
    @Override public ItemStack quickMoveStack(Player player,int index) {
        if (!stillValid(player) || index<0 || index>=slots.size()) return ItemStack.EMPTY;
        if (index==2) {
            var result=getSlot(2).getItem(); int capacity=0;
            for (int i=3;i<39;i++) {
                var existing=getSlot(i).getItem();
                if (existing.isEmpty()) capacity+=result.getMaxStackSize();
                else if (ItemStack.isSameItemSameComponents(existing,result)) capacity+=existing.getMaxStackSize()-existing.getCount();
            }
            if (result.isEmpty() || capacity<result.getCount()) return ItemStack.EMPTY;
        }
        return super.quickMoveStack(player,index);
    }
}
