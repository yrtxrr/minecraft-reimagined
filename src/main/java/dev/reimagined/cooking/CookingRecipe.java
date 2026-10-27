package dev.reimagined.cooking;

import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

/** Three unordered food slots, one bowl per batch; no client crafting authority. */
public record CookingRecipe(List<Ingredient> ingredients, ItemStack result, int seconds) implements Recipe<CookingRecipe.Input> {
    public CookingRecipe {
        ingredients = List.copyOf(ingredients);
        if (ingredients.isEmpty() || ingredients.size() > 3 || ingredients.stream().anyMatch(Ingredient::isEmpty))
            throw new IllegalArgumentException("Cooking needs 1..3 nonempty ingredients");
        if (seconds < 1 || seconds > 1200 || result.isEmpty() || result.getCount() != 1)
            throw new IllegalArgumentException("Cooking needs one result and 1..1200 seconds");
        result = result.copy();
    }
    @Override public ItemStack result() { return result.copy(); }
    @Override public boolean matches(Input input, Level level) {
        var stacks = new java.util.ArrayList<ItemStack>(3);
        for (int i = 0; i < 3; i++) if (!input.getItem(i).isEmpty()) stacks.add(input.getItem(i));
        return stacks.size() == ingredients.size()
                && net.neoforged.neoforge.common.util.RecipeMatcher.findMatches(stacks, ingredients) != null;
    }
    @Override public ItemStack assemble(Input input, HolderLookup.Provider registries) { return result.copy(); }
    @Override public boolean canCraftInDimensions(int width, int height) { return width * height >= ingredients.size(); }
    @Override public ItemStack getResultItem(HolderLookup.Provider registries) { return result.copy(); }
    @Override public RecipeSerializer<?> getSerializer() { return CookingContent.SERIALIZER.get(); }
    @Override public RecipeType<?> getType() { return CookingContent.COOKING.get(); }
    @Override public boolean isSpecial() { return true; }
    @Override public ItemStack getToastSymbol() { return new ItemStack(CookingContent.POT.get()); }
    @Override public NonNullList<Ingredient> getIngredients() {
        var all = NonNullList.<Ingredient>create(); all.addAll(ingredients); all.add(Ingredient.of(Items.BOWL)); return all;
    }
    public record Input(ItemStack first, ItemStack second, ItemStack third) implements RecipeInput {
        @Override public ItemStack getItem(int slot) { return switch(slot) { case 0 -> first; case 1 -> second; case 2 -> third; default -> throw new IndexOutOfBoundsException(slot); }; }
        @Override public int size() { return 3; }
    }
    public static final class Serializer implements RecipeSerializer<CookingRecipe> {
        private static final MapCodec<CookingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Ingredient.CODEC_NONEMPTY.listOf().validate(list -> list.isEmpty() || list.size() > 3
                    ? DataResult.error(() -> "Cooking needs 1..3 ingredients") : DataResult.success(list)).fieldOf("ingredients").forGetter(CookingRecipe::ingredients),
            ItemStack.SINGLE_ITEM_CODEC.fieldOf("result").forGetter(CookingRecipe::result),
            Codec.intRange(1,1200).optionalFieldOf("seconds", 12).forGetter(CookingRecipe::seconds)
        ).apply(instance, CookingRecipe::new));
        private static final StreamCodec<RegistryFriendlyByteBuf, CookingRecipe> STREAM = StreamCodec.of((buffer, recipe) -> {
            buffer.writeVarInt(recipe.ingredients.size());
            for (var ingredient : recipe.ingredients) Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, ingredient);
            ItemStack.STREAM_CODEC.encode(buffer, recipe.result); buffer.writeVarInt(recipe.seconds);
        }, buffer -> {
            int count = buffer.readVarInt();
            if (count < 1 || count > 3) throw new IllegalArgumentException("Invalid cooking ingredient count");
            var ingredients = new java.util.ArrayList<Ingredient>(count);
            for (int i = 0; i < count; i++) ingredients.add(Ingredient.CONTENTS_STREAM_CODEC.decode(buffer));
            return new CookingRecipe(ingredients, ItemStack.STREAM_CODEC.decode(buffer), buffer.readVarInt());
        });
        @Override public MapCodec<CookingRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, CookingRecipe> streamCodec() { return STREAM; }
    }
}
