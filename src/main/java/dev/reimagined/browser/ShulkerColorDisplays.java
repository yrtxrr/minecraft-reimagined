package dev.reimagined.browser;

import java.util.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.*;
import net.neoforged.neoforge.common.crafting.DataComponentIngredient;

/** Concrete, component-preserving previews; never modifies the selected stack or performs crafting. */
public final class ShulkerColorDisplays implements RecipeDisplayAdapter {
    public static final ShulkerColorDisplays INSTANCE = new ShulkerColorDisplays();
    public static final int MAX_DISPLAYS = 33; // 16 outgoing colors +17 incoming box colors, with one overlap.
    private ShulkerColorDisplays() {}
    @Override public boolean supports(Recipe<?> recipe) { return recipe.getClass() == ShulkerBoxColoring.class; }
    @Override public List<RecipeDisplay> displays(RecipeHolder<?> holder, HolderLookup.Provider registries, ItemStack selected) {
        if (!supports(holder.value()) || selected.isEmpty()) return List.of();
        var recipe = (ShulkerBoxColoring) holder.value();
        var result = new LinkedHashMap<ResourceLocation, RecipeDisplay>();
        if (Block.byItem(selected.getItem()) instanceof ShulkerBoxBlock block) {
            for (DyeColor color : DyeColor.values()) add(result, holder.id(), recipe, registries, selected, new ItemStack(DyeItem.byColor(color)));
            if (block.getColor() != null) {
                ItemStack dye = new ItemStack(DyeItem.byColor(block.getColor()));
                for (ItemStack source : boxes()) {
                    source.applyComponents(selected.getComponentsPatch());
                    add(result, holder.id(), recipe, registries, source, dye);
                }
            }
        } else if (selected.is(net.neoforged.neoforge.common.Tags.Items.DYES) && DyeColor.getColor(selected) != null) {
            for (ItemStack source : boxes()) add(result, holder.id(), recipe, registries, source, selected);
        }
        return List.copyOf(result.values());
    }
    private static List<ItemStack> boxes() {
        var result = new ArrayList<ItemStack>(17); result.add(new ItemStack(Blocks.SHULKER_BOX));
        for (DyeColor color : DyeColor.values()) result.add(new ItemStack(ShulkerBoxBlock.getBlockByColor(color)));
        return result;
    }
    private static void add(Map<ResourceLocation, RecipeDisplay> result, ResourceLocation original, ShulkerBoxColoring recipe,
                            HolderLookup.Provider registries, ItemStack source, ItemStack dye) {
        ItemStack input = source.copyWithCount(1), reagent = dye.copyWithCount(1);
        ItemStack output = recipe.assemble(CraftingInput.of(2, 1, List.of(input, reagent)), registries);
        if (output.isEmpty()) return;
        String from = BuiltInRegistries.ITEM.getKey(input.getItem()).toString().replace(':', '/');
        String color = BuiltInRegistries.ITEM.getKey(reagent.getItem()).toString().replace(':', '/');
        var id = ResourceLocation.fromNamespaceAndPath("reimagined", "browser/special/" + original.getNamespace() + "/" + original.getPath() + "/" + from + "/" + color);
        result.putIfAbsent(id, new RecipeDisplay(id, List.of(DataComponentIngredient.of(true, input), Ingredient.of(reagent)),
            output, new ItemStack(Blocks.CRAFTING_TABLE), 2, Component.translatable("reimagined.browser.shulker_color_note")));
        if (result.size() > MAX_DISPLAYS) throw new IllegalStateException("Shulker preview budget exceeded");
    }
}
