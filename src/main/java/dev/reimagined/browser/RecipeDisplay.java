package dev.reimagined.browser;

import java.util.List;
import java.util.Objects;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

/** Read-only display metadata; no crafting or brewing authority is granted to the client. */
public record RecipeDisplay(ResourceLocation id, List<Ingredient> inputs, ItemStack result,
                            ItemStack station, int columns, Component note) {
    public RecipeDisplay {
        Objects.requireNonNull(id);
        inputs = List.copyOf(inputs);
        result = result.copy();
        station = station.copy();
        columns = Math.max(1, Math.min(9, columns));
        note = note.copy();
    }
    @Override public ItemStack result() { return result.copy(); }
    @Override public ItemStack station() { return station.copy(); }
    @Override public Component note() { return note.copy(); }
    public boolean uses(ItemStack item) {
        return !item.isEmpty() && inputs.stream().anyMatch(input -> input.test(item));
    }
    public boolean produces(ItemStack item) { return sameDisplayItem(result, item); }
    public ItemStack ingredient(int index, long tick) {
        if (index < 0 || index >= inputs.size()) return ItemStack.EMPTY;
        ItemStack[] alternatives = inputs.get(index).getItems();
        return alternatives.length == 0 ? ItemStack.EMPTY
                : alternatives[Math.floorMod(tick / 30, alternatives.length)].copy();
    }
    /** Ordinary equipment is searchable despite wear; distinct potion contents are never conflated. */
    public static boolean sameDisplayItem(ItemStack expected, ItemStack selected) {
        if (expected.isEmpty() || selected.isEmpty() || !ItemStack.isSameItem(expected, selected)) return false;
        var potion = selected.get(DataComponents.POTION_CONTENTS);
        if (potion == null) return true;
        return potion.equals(expected.get(DataComponents.POTION_CONTENTS));
    }
}
