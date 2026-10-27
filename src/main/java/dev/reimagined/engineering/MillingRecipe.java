package dev.reimagined.engineering;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

/** Shared vanilla recipe contract: reloadable, automatically synchronized and browser-compatible. */
public record MillingRecipe(Ingredient ingredient, ItemStack result, int seconds) implements Recipe<SingleRecipeInput> {
    @Override public boolean matches(SingleRecipeInput input, Level level) { return ingredient.test(input.item()); }
    @Override public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registries) { return result.copy(); }
    @Override public boolean canCraftInDimensions(int width, int height) { return width * height > 0; }
    @Override public ItemStack getResultItem(HolderLookup.Provider registries) { return result; }
    @Override public RecipeSerializer<?> getSerializer() { return EngineeringContent.MILLING_SERIALIZER.get(); }
    @Override public RecipeType<?> getType() { return EngineeringContent.MILLING.get(); }
    @Override public NonNullList<Ingredient> getIngredients() { return NonNullList.of(Ingredient.EMPTY, ingredient); }
    @Override public ItemStack getToastSymbol() { return new ItemStack(EngineeringContent.MILLSTONE.get()); }
    public static final class Serializer implements RecipeSerializer<MillingRecipe> {
        private static final MapCodec<MillingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(MillingRecipe::ingredient),
                ItemStack.CODEC.fieldOf("result").forGetter(MillingRecipe::result),
                com.mojang.serialization.Codec.intRange(1, 3600).optionalFieldOf("seconds", 8).forGetter(MillingRecipe::seconds)
        ).apply(instance, MillingRecipe::new));
        private static final StreamCodec<RegistryFriendlyByteBuf, MillingRecipe> STREAM = StreamCodec.of(
                (buffer, recipe) -> {
                    Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.ingredient());
                    ItemStack.STREAM_CODEC.encode(buffer, recipe.result());
                    buffer.writeVarInt(recipe.seconds());
                }, buffer -> new MillingRecipe(Ingredient.CONTENTS_STREAM_CODEC.decode(buffer), ItemStack.STREAM_CODEC.decode(buffer), buffer.readVarInt()));
        @Override public MapCodec<MillingRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, MillingRecipe> streamCodec() { return STREAM; }
    }
}
