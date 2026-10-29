package dev.reimagined.settlements;

import net.minecraft.core.HolderLookup;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class TradingMenu extends AbstractContainerMenu {
    private final Settler settler;
    private final ServerLevel level;
    private final ContainerData tradeData;
    private final Container inputSlot;
    private final Container outputSlot;
    private int selectedTradeIndex = -1;
    private int currentTradeCount = 1;
    private List<TradeOffer> clientOffers = List.of();

    public TradingMenu(int id, Inventory playerInventory) {
        this(id, playerInventory, null, null);
    }

    public TradingMenu(int id, Inventory playerInventory, FriendlyByteBuf buf) {
        this(id, playerInventory, null, null);
        if (buf != null) {
            int offerCount = buf.readVarInt();
            List<TradeOffer> offers = new ArrayList<>();
            HolderLookup.Provider registries = playerInventory.player.registryAccess();
            for (int i = 0; i < offerCount; i++) {
                String profession = buf.readUtf();
                ItemStack input = ItemStack.parseOptional(registries, buf.readNbt());
                ItemStack output = ItemStack.parseOptional(registries, buf.readNbt());
                int maxUses = buf.readVarInt();
                int uses = buf.readVarInt();
                int restockTick = buf.readVarInt();
                offers.add(new TradeOffer(profession, input, output, maxUses, uses, restockTick));
            }
            this.clientOffers = offers;
        }
    }

    public TradingMenu(int id, Inventory playerInventory, ServerLevel level, UUID settlerId) {
        super(SettlementContent.TRADING_MENU.get(), id);
        this.level = level;
        this.settler = level != null ? level.getEntity(settlerId) instanceof Settler s ? s : null : null;

        this.tradeData = new SimpleContainerData(4);
        this.inputSlot = new SimpleContainer(1);
        this.outputSlot = new SimpleContainer(1) {
            @Override
            public void setItem(int index, ItemStack stack) {
                super.setItem(index, stack);
                setChanged();
            }
        };

        addSlot(new Slot(inputSlot, 0, 26, 30) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return selectedTradeIndex >= 0 && getOffer().map((TradeOffer o) -> ItemStack.isSameItemSameComponents(o.input, stack)).orElse(false);
            }
            @Override
            public int getMaxStackSize() { return 1; }
        });

        addSlot(new Slot(outputSlot, 0, 134, 30) {
            @Override
            public boolean mayPlace(ItemStack stack) { return false; }
        });

        layoutPlayerInventorySlots(playerInventory, 76);
        addDataSlots(tradeData);
    }

    private void layoutPlayerInventorySlots(Inventory inventory, int yOffset) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, yOffset + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, yOffset + 58));
        }
    }

    public Settler getSettler() { return settler; }
    public boolean hasSettler() { return settler != null && settler.isAlive(); }

    public List<TradeOffer> getOffers() {
        if (!clientOffers.isEmpty()) return clientOffers;
        if (settler == null) return List.of();
        var tradeData = TradeData.get(level);
        return tradeData.offers().stream()
                .filter(o -> o.profession.equals(getProfession()))
                .toList();
    }

    public String getProfession() {
        var data = SettlementData.get(level);
        var record = data.findSettler(settler.getUUID());
        return record != null ? record.profession : "commoner";
    }

    public Optional<TradeOffer> getOffer() {
        List<TradeOffer> offers = getOffers();
        if (selectedTradeIndex < 0 || selectedTradeIndex >= offers.size()) return Optional.empty();
        return Optional.of(offers.get(selectedTradeIndex));
    }

    public void selectTrade(int index) {
        this.selectedTradeIndex = index;
        this.currentTradeCount = 1;
        updateOutput();
    }

    public void incrementTradeCount() {
        getOffer().ifPresent(o -> {
            int max = o.maxUses - o.uses();
            int availableInput = inputSlot.getItem(0).getCount();
            currentTradeCount = Math.min(max, Math.min(availableInput, currentTradeCount + 1));
            updateOutput();
        });
    }

    public void decrementTradeCount() {
        if (currentTradeCount > 1) {
            currentTradeCount--;
            updateOutput();
        }
    }

    public void setTradeCount(int count) {
        getOffer().ifPresent(o -> {
            int max = o.maxUses - o.uses();
            int availableInput = inputSlot.getItem(0).getCount();
            currentTradeCount = Math.clamp(count, 1, Math.min(max, availableInput));
            updateOutput();
        });
    }

    private void updateOutput() {
        getOffer().ifPresent(o -> {
            ItemStack output = o.output.copy();
            output.setCount(o.output.getCount() * currentTradeCount);
            outputSlot.setItem(0, output);
            tradeData.set(0, o.uses());
            tradeData.set(1, o.maxUses);
            tradeData.set(2, o.input.getCount() * currentTradeCount);
            tradeData.set(3, selectedTradeIndex);
        });
    }

    public int getSelectedTradeIndex() { return selectedTradeIndex; }
    public int getCurrentTradeCount() { return currentTradeCount; }
    public int getInputCount() { return tradeData.get(2); }
    public int getUses() { return tradeData.get(0); }
    public int getMaxUses() { return tradeData.get(1); }

    public boolean executeTrade(Player player) {
        return executeTrade(player, selectedTradeIndex, currentTradeCount);
    }

    public boolean executeTrade(Player player, int tradeIndex, int tradeCount) {
        if (tradeIndex < 0) return false;
        List<TradeOffer> offers = getOffers();
        if (tradeIndex >= offers.size()) return false;
        var offer = offers.get(tradeIndex);
        if (!offer.canTrade()) return false;
        if (tradeCount <= 0) return false;

        ItemStack input = inputSlot.getItem(0);
        if (input.isEmpty() || !ItemStack.isSameItemSameComponents(input, offer.input)) return false;
        if (input.getCount() < offer.input.getCount() * tradeCount) return false;

        ItemStack output = offer.output.copy();
        output.setCount(offer.output.getCount() * tradeCount);

        if (!player.getInventory().add(output)) return false;

        input.shrink(offer.input.getCount() * tradeCount);
        if (input.isEmpty()) inputSlot.setItem(0, ItemStack.EMPTY);

        offer.use();
        updateOutput();
        return true;
    }

    // Test-only execute that bypasses player inventory check
    public boolean executeTradeForTesting(int tradeIndex, int tradeCount) {
        if (tradeIndex < 0) return false;
        List<TradeOffer> offers = getOffers();
        if (tradeIndex >= offers.size()) return false;
        var offer = offers.get(tradeIndex);
        if (!offer.canTrade()) return false;
        if (tradeCount <= 0) return false;

        ItemStack input = inputSlot.getItem(0);
        if (input.isEmpty() || !ItemStack.isSameItemSameComponents(input, offer.input)) return false;
        if (input.getCount() < offer.input.getCount() * tradeCount) return false;

        ItemStack output = offer.output.copy();
        output.setCount(offer.output.getCount() * tradeCount);

        // Put output in output slot (slot 1) instead of player inventory
        outputSlot.setItem(0, output);

        input.shrink(offer.input.getCount() * tradeCount);
        if (input.isEmpty()) inputSlot.setItem(0, ItemStack.EMPTY);

        offer.use();
        updateOutput();
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        return hasSettler() && player.distanceToSqr(settler) <= 64.0;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!inputSlot.getItem(0).isEmpty()) {
            player.drop(inputSlot.getItem(0), true);
            inputSlot.setItem(0, ItemStack.EMPTY);
        }
    }
}