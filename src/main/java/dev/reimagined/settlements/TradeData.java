package dev.reimagined.settlements;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;

/**
 * Per-settler trade data: offered trades, stock limits and restock state.
 * Persists across world reloads so trading progress is not lost.
 */
public final class TradeData extends SavedData {
    private final List<TradeOffer> offers = new ArrayList<>();

    public static TradeData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
                new Factory<>(TradeData::new, TradeData::load), "reimagined_trades");
    }

    public static TradeData load(CompoundTag tag, HolderLookup.Provider registries) {
        TradeData data = new TradeData();
        ListTag list = tag.getList("Offers", 10);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            TradeOffer offer = data.new TradeOffer(
                    entry.getString("Profession"),
                    ItemStack.parseOptional(registries, entry.getCompound("Input")),
                    ItemStack.parseOptional(registries, entry.getCompound("Output")),
                    entry.getInt("MaxUses"),
                    entry.getInt("Uses"),
                    entry.getInt("RestockTick")
            );
            data.offers.add(offer);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (TradeOffer o : offers) {
            CompoundTag entry = new CompoundTag();
            entry.putString("Profession", o.profession);
            entry.put("Input", o.input.save(registries));
            entry.put("Output", o.output.save(registries));
            entry.putInt("MaxUses", o.maxUses);
            entry.putInt("Uses", o.uses);
            entry.putInt("RestockTick", o.restockTick);
            list.add(entry);
        }
        tag.put("Offers", list);
        return tag;
    }

    public List<TradeOffer> offers() { return offers; }

    public TradeOffer findOffer(String profession, ItemStack input) {
        for (TradeOffer o : offers) {
            if (o.profession.equals(profession) && ItemStack.isSameItemSameComponents(o.input, input)) return o;
        }
        return null;
    }

    public void addOffer(String profession, ItemStack input, ItemStack output, int maxUses) {
        offers.add(new TradeOffer(profession, input.copy(), output.copy(), maxUses, 0, 0));
        setDirty();
    }

    public final class TradeOffer {
        public final String profession;
        public final ItemStack input;
        public final ItemStack output;
        public final int maxUses;
        private int uses;
        private int restockTick;

        public TradeOffer(String profession, ItemStack input, ItemStack output, int maxUses, int uses, int restockTick) {
            this.profession = profession;
            this.input = input;
            this.output = output;
            this.maxUses = maxUses;
            this.uses = uses;
            this.restockTick = restockTick;
        }

        public boolean canTrade() { return uses < maxUses; }
        public int uses() { return uses; }
        public int restockTick() { return restockTick; }
        public void use() { uses++; TradeData.this.setDirty(); }
        public void restock() { uses = 0; TradeData.this.setDirty(); }
    }
}
