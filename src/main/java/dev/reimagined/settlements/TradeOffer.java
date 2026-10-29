package dev.reimagined.settlements;

import net.minecraft.world.item.ItemStack;

/**
 * Trade offer with input/output items, use limits and restock state.
 */
public final class TradeOffer {
    public final String profession;
    public final ItemStack input;
    public final ItemStack output;
    public final int maxUses;
    private int uses;
    private int restockTick;
    private TradeData owner;

    public TradeOffer(String profession, ItemStack input, ItemStack output, int maxUses, int uses, int restockTick) {
        this.profession = profession;
        this.input = input;
        this.output = output;
        this.maxUses = maxUses;
        this.uses = uses;
        this.restockTick = restockTick;
    }

    void setOwner(TradeData owner) {
        this.owner = owner;
    }

    public boolean canTrade() { return uses < maxUses; }
    public int uses() { return uses; }
    public int restockTick() { return restockTick; }
    public void use() { uses++; if (owner != null) owner.setDirty(); }
    public void restock() { uses = 0; if (owner != null) owner.setDirty(); }
}