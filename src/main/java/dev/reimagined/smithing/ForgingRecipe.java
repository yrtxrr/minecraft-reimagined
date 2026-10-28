package dev.reimagined.smithing;

import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

/** Fixed equipment identity, component-preserving assembly, player/equipment quality resolved by the server menu. */
public record ForgingRecipe(Item equipment,Ingredient metal,Ingredient treatment,int materialQuality,boolean sturdy) implements Recipe<ForgingRecipe.Input> {
    @Override public boolean matches(Input input,Level level) {
        return input.base().is(equipment) && ForgedEquipment.eligible(input.base()) && (sturdy || ForgedEquipment.supportsEdge(input.base()))
                && metal.test(input.metal()) && treatment.test(input.treatment());
    }
    public ItemStack assemble(Input input,HolderLookup.Provider registries,int rank,boolean bellows) {
        return ForgedEquipment.finish(input.base(),ForgeRules.quality(materialQuality,rank,bellows),sturdy);
    }
    @Override public ItemStack assemble(Input input,HolderLookup.Provider registries) {return assemble(input,registries,0,false);}
    @Override public ItemStack getResultItem(HolderLookup.Provider registries) {return ForgedEquipment.finish(equipment.getDefaultInstance(),materialQuality,sturdy);}
    @Override public NonNullList<Ingredient> getIngredients() {
        return NonNullList.of(Ingredient.EMPTY,Ingredient.of(equipment),metal,treatment,Ingredient.of(net.minecraft.tags.ItemTags.COALS),Ingredient.of(SmithingContent.HAMMERS));
    }
    @Override public boolean canCraftInDimensions(int width,int height) {return width*height>=3;}
    @Override public boolean isSpecial() {return true;}
    @Override public RecipeType<?> getType() {return SmithingContent.FORGING.get();}
    @Override public RecipeSerializer<?> getSerializer() {return SmithingContent.SERIALIZER.get();}
    @Override public ItemStack getToastSymbol() {return new ItemStack(SmithingContent.FORGE.get());}
    public record Input(ItemStack base,ItemStack metal,ItemStack treatment) implements RecipeInput {
        @Override public ItemStack getItem(int slot) {return switch(slot) {case 0->base;case 1->metal;case 2->treatment;default->throw new IndexOutOfBoundsException(slot);};}
        @Override public int size() {return 3;}
    }
    public static final class Serializer implements RecipeSerializer<ForgingRecipe> {
        private static final MapCodec<ForgingRecipe> CODEC=RecordCodecBuilder.mapCodec(instance->instance.group(
                BuiltInRegistries.ITEM.byNameCodec().fieldOf("equipment").forGetter(ForgingRecipe::equipment),
                Ingredient.CODEC_NONEMPTY.fieldOf("metal").forGetter(ForgingRecipe::metal),
                Ingredient.CODEC_NONEMPTY.fieldOf("treatment").forGetter(ForgingRecipe::treatment),
                Codec.intRange(1,2).fieldOf("material_quality").forGetter(ForgingRecipe::materialQuality),
                Codec.BOOL.fieldOf("sturdy").forGetter(ForgingRecipe::sturdy)
        ).apply(instance,ForgingRecipe::new));
        private static final StreamCodec<RegistryFriendlyByteBuf,ForgingRecipe> STREAM=StreamCodec.of((buf,recipe)-> {
            ItemStack.STREAM_CODEC.encode(buf,recipe.equipment.getDefaultInstance());Ingredient.CONTENTS_STREAM_CODEC.encode(buf,recipe.metal);
            Ingredient.CONTENTS_STREAM_CODEC.encode(buf,recipe.treatment);buf.writeVarInt(recipe.materialQuality);buf.writeBoolean(recipe.sturdy);
        },buf->new ForgingRecipe(ItemStack.STREAM_CODEC.decode(buf).getItem(),Ingredient.CONTENTS_STREAM_CODEC.decode(buf),Ingredient.CONTENTS_STREAM_CODEC.decode(buf),buf.readVarInt(),buf.readBoolean()));
        @Override public MapCodec<ForgingRecipe> codec() {return CODEC;}
        @Override public StreamCodec<RegistryFriendlyByteBuf,ForgingRecipe> streamCodec() {return STREAM;}
    }
    public ForgingRecipe {if(materialQuality<1 || materialQuality>2)throw new IllegalArgumentException("Invalid material quality");}
}
