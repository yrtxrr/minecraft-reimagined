package dev.reimagined.settlements;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;

/**
 * Per-settler trade data: offered trades, stock limits and restock state.
 * Persists across world reloads so trading progress is not lost.
 */
public final class TradeData extends SavedData {
    private final List<TradeOffer> offers = new ArrayList<>(); // Legacy profession catalogue, retained for v1 saves.
    private final java.util.Map<java.util.UUID, NpcStock> npcStocks = new java.util.HashMap<>();
    private record NpcStock(String profession, net.minecraft.world.item.trading.MerchantOffers offers, long[] restockDay) {}

    /** One lazy restock per advancing work day. No polling of unloaded NPC records. */
    public net.minecraft.world.item.trading.MerchantOffers offersFor(ServerLevel level, java.util.UUID id, String profession) {
        long day = Math.floorDiv(level.getDayTime() - 8000L, 24000L);
        NpcStock stock = npcStocks.get(id);
        if (stock == null) {
            initializeOffersForProfession(level, profession);
            var actual = new net.minecraft.world.item.trading.MerchantOffers();
            for (TradeOffer source : offers) if (source.profession.equals(profession) && !source.input.isEmpty() && !source.output.isEmpty()) {
                actual.add(new net.minecraft.world.item.trading.MerchantOffer(
                    new net.minecraft.world.item.trading.ItemCost(source.input.getItem(), source.input.getCount()),
                    java.util.Optional.empty(), source.output.copy(), Math.clamp(source.uses(),0,source.maxUses), source.maxUses, 0, 0F));
            }
            stock = new NpcStock(profession, actual, new long[]{day});
            npcStocks.put(id,stock); setDirty();
        } else if (day > stock.restockDay()[0]) {
            for (var offer : stock.offers()) offer.resetUses();
            stock.restockDay()[0] = day; setDirty();
        }
        return stock.offers();
    }

    public void removeStock(java.util.UUID id) { if (npcStocks.remove(id) != null) setDirty(); }


    public static TradeData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
                new Factory<>(TradeData::new, TradeData::load), "reimagined_trades");
    }

    public static TradeData load(CompoundTag tag, HolderLookup.Provider registries) {
        TradeData data = new TradeData();
        ListTag list = tag.getList("Offers", 10);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            TradeOffer offer = new TradeOffer(
                    entry.getString("Profession"),
                    ItemStack.parseOptional(registries, entry.getCompound("Input")),
                    ItemStack.parseOptional(registries, entry.getCompound("Output")),
                    entry.getInt("MaxUses"),
                    entry.getInt("Uses"),
                    entry.getInt("RestockTick")
            );
            offer.setOwner(data);
            data.offers.add(offer);
        }
        var ops = registries.createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE);
        for (var entryTag : tag.getList("NpcOffers",10)) {
            CompoundTag entry = (CompoundTag)entryTag;
            if (!entry.hasUUID("Uuid")) continue;
            var decoded = net.minecraft.world.item.trading.MerchantOffers.CODEC.parse(ops,entry.getCompound("Offers")).result();
            decoded.ifPresent(value -> data.npcStocks.put(entry.getUUID("Uuid"),
                new NpcStock(entry.getString("Profession"), value, new long[]{entry.getLong("RestockDay")})));
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
            entry.putInt("Uses", o.uses());
            entry.putInt("RestockTick", o.restockTick());
            list.add(entry);
        }
        tag.put("Offers", list);
        var stocks = new ListTag();
        var ops = registries.createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE);
        npcStocks.forEach((id,stock) -> {
            var entry = new CompoundTag(); entry.putUUID("Uuid",id); entry.putString("Profession",stock.profession());
            entry.putLong("RestockDay",stock.restockDay()[0]);
            entry.put("Offers",net.minecraft.world.item.trading.MerchantOffers.CODEC.encodeStart(ops,stock.offers()).getOrThrow());
            stocks.add(entry);
        });
        tag.put("NpcOffers",stocks);
        return tag;
    }

    public List<TradeOffer> offers() { return offers; }

    public void clear() {
        offers.clear(); npcStocks.clear();
        setDirty();
    }

    public TradeOffer findOffer(String profession, ItemStack input) {
        for (TradeOffer o : offers) {
            if (o.profession.equals(profession) && ItemStack.isSameItemSameComponents(o.input, input)) return o;
        }
        return null;
    }

    public void addOffer(String profession, ItemStack input, ItemStack output, int maxUses) {
        TradeOffer offer = new TradeOffer(profession, input.copy(), output.copy(), maxUses, 0, 0);
        offer.setOwner(this);
        offers.add(offer);
        setDirty();
    }

    public static void initializeOffersForProfession(ServerLevel level, String profession) {
        var data = TradeData.get(level);
        boolean hasOffers = data.offers().stream().anyMatch(o -> o.profession.equals(profession));
        if (hasOffers) return;

        switch (profession) {
            case "farmer" -> {
                data.addOffer("farmer", new ItemStack(Items.WHEAT, 20), new ItemStack(Items.EMERALD, 1), 16);
                data.addOffer("farmer", new ItemStack(Items.CARROT, 20), new ItemStack(Items.EMERALD, 1), 16);
                data.addOffer("farmer", new ItemStack(Items.POTATO, 20), new ItemStack(Items.EMERALD, 1), 16);
                data.addOffer("farmer", new ItemStack(Items.BEETROOT, 20), new ItemStack(Items.EMERALD, 1), 16);
                data.addOffer("farmer", new ItemStack(Items.EMERALD, 1), new ItemStack(Items.BREAD, 6), 12);
                data.addOffer("farmer", new ItemStack(Items.EMERALD, 1), new ItemStack(Items.COOKIE, 8), 12);
            }
            case "fisher" -> {
                data.addOffer("fisher", new ItemStack(Items.COD, 10), new ItemStack(Items.EMERALD, 1), 16);
                data.addOffer("fisher", new ItemStack(Items.SALMON, 6), new ItemStack(Items.EMERALD, 1), 16);
                data.addOffer("fisher", new ItemStack(Items.EMERALD, 2), new ItemStack(Items.FISHING_ROD, 1), 3);
                data.addOffer("fisher", new ItemStack(Items.EMERALD, 1), new ItemStack(Items.COOKED_COD, 6), 12);
                data.addOffer("fisher", new ItemStack(Items.EMERALD, 1), new ItemStack(Items.COOKED_SALMON, 6), 12);
            }
            case "blacksmith" -> {
                data.addOffer("blacksmith", new ItemStack(Items.IRON_INGOT, 4), new ItemStack(Items.EMERALD, 1), 12);
                data.addOffer("blacksmith", new ItemStack(Items.GOLD_INGOT, 4), new ItemStack(Items.EMERALD, 1), 12);
                data.addOffer("blacksmith", new ItemStack(Items.DIAMOND, 1), new ItemStack(Items.EMERALD, 3), 8);
                data.addOffer("blacksmith", new ItemStack(Items.EMERALD, 6), new ItemStack(Items.IRON_SWORD, 1), 3);
                data.addOffer("blacksmith", new ItemStack(Items.EMERALD, 10), new ItemStack(Items.DIAMOND_SWORD, 1), 2);
                data.addOffer("blacksmith", new ItemStack(Items.EMERALD, 4), new ItemStack(Items.IRON_PICKAXE, 1), 3);
            }
            case "librarian" -> {
                data.addOffer("librarian", new ItemStack(Items.PAPER, 24), new ItemStack(Items.EMERALD, 1), 16);
                data.addOffer("librarian", new ItemStack(Items.BOOK, 4), new ItemStack(Items.EMERALD, 1), 12);
                data.addOffer("librarian", new ItemStack(Items.EMERALD, 1), new ItemStack(Items.ENCHANTED_BOOK, 1), 8);
                data.addOffer("librarian", new ItemStack(Items.EMERALD, 5), new ItemStack(Items.BOOKSHELF, 1), 12);
                data.addOffer("librarian", new ItemStack(Items.EMERALD, 2), new ItemStack(Items.GLASS, 4), 12);
            }
            case "butcher" -> {
                data.addOffer("butcher", new ItemStack(Items.BEEF, 10), new ItemStack(Items.EMERALD, 1), 16);
                data.addOffer("butcher", new ItemStack(Items.PORKCHOP, 10), new ItemStack(Items.EMERALD, 1), 16);
                data.addOffer("butcher", new ItemStack(Items.CHICKEN, 14), new ItemStack(Items.EMERALD, 1), 16);
                data.addOffer("butcher", new ItemStack(Items.EMERALD, 1), new ItemStack(Items.COOKED_BEEF, 6), 12);
                data.addOffer("butcher", new ItemStack(Items.EMERALD, 1), new ItemStack(Items.COOKED_PORKCHOP, 6), 12);
            }
            default -> {
                data.addOffer("commoner", new ItemStack(Items.STICK, 32), new ItemStack(Items.EMERALD, 1), 16);
                data.addOffer("commoner", new ItemStack(Items.COAL, 16), new ItemStack(Items.EMERALD, 1), 16);
                data.addOffer("commoner", new ItemStack(Items.EMERALD, 1), new ItemStack(Items.APPLE, 4), 12);
            }
        }
    }
}