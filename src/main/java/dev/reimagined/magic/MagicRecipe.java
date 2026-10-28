package dev.reimagined.magic;

import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

/** One shared recipe implementation, separately registered infusion/research processes. */
public record MagicRecipe(List<Ingredient> ingredients,ItemStack result,int mana,int biomes,int creatures,boolean research) implements Recipe<MagicRecipe.Input> {
    public MagicRecipe {
        ingredients=List.copyOf(ingredients);result=result.copy();
        if(ingredients.isEmpty() || ingredients.size()>3 || ingredients.stream().anyMatch(Ingredient::isEmpty) || result.isEmpty()
                || result.getCount()!=1 || mana<1 || mana>100 || biomes<0 || biomes>16 || creatures<0 || creatures>16)
            throw new IllegalArgumentException("Invalid magic process bounds");
    }
    @Override public ItemStack result() { return result.copy(); }
    @Override public boolean matches(Input input,Level level) {
        var stacks=new java.util.ArrayList<ItemStack>(3);
        for(int i=0;i<3;i++)if(!input.getItem(i).isEmpty())stacks.add(input.getItem(i));
        return stacks.size()==ingredients.size() && net.neoforged.neoforge.common.util.RecipeMatcher.findMatches(stacks,ingredients)!=null;
    }
    @Override public ItemStack assemble(Input input,HolderLookup.Provider registries) { return result.copy(); }
    @Override public ItemStack getResultItem(HolderLookup.Provider registries) { return result.copy(); }
    @Override public NonNullList<Ingredient> getIngredients() { var list=NonNullList.<Ingredient>create();list.addAll(ingredients);return list; }
    @Override public boolean canCraftInDimensions(int width,int height) { return width*height>=ingredients.size(); }
    @Override public boolean isSpecial() { return true; }
    @Override public RecipeType<?> getType() { return research?MagicContent.RESEARCH.get():MagicContent.INFUSION.get(); }
    @Override public RecipeSerializer<?> getSerializer() { return research?MagicContent.RESEARCH_SERIALIZER.get():MagicContent.INFUSION_SERIALIZER.get(); }
    @Override public ItemStack getToastSymbol() { return new ItemStack(research?MagicContent.RESEARCH_TABLE.get():MagicContent.ARCANE_ALTAR.get()); }
    public Component note() { return Component.translatable("reimagined.magic.recipe_note",mana,biomes,creatures); }
    public record Input(ItemStack first,ItemStack second,ItemStack third) implements RecipeInput {
        @Override public ItemStack getItem(int slot) { return switch(slot) { case 0->first;case 1->second;case 2->third;default->throw new IndexOutOfBoundsException(slot); }; }
        @Override public int size() { return 3; }
    }
    public static final class Serializer implements RecipeSerializer<MagicRecipe> {
        private final MapCodec<MagicRecipe> codec;
        private final StreamCodec<RegistryFriendlyByteBuf,MagicRecipe> stream;
        public Serializer(boolean research) {
            codec=RecordCodecBuilder.mapCodec(instance->instance.group(
                Ingredient.CODEC_NONEMPTY.listOf().validate(list->list.isEmpty() || list.size()>3?DataResult.error(()->"Needs1..3ingredients"):DataResult.success(list)).fieldOf("ingredients").forGetter(MagicRecipe::ingredients),
                ItemStack.SINGLE_ITEM_CODEC.fieldOf("result").forGetter(MagicRecipe::result),
                Codec.intRange(1,100).optionalFieldOf("mana",20).forGetter(MagicRecipe::mana),
                Codec.intRange(0,16).optionalFieldOf("biomes",0).forGetter(MagicRecipe::biomes),
                Codec.intRange(0,16).optionalFieldOf("creatures",0).forGetter(MagicRecipe::creatures)
            ).apply(instance,(ingredients,result,mana,biomes,creatures)->new MagicRecipe(ingredients,result,mana,biomes,creatures,research)));
            stream=StreamCodec.of((buffer,recipe)-> {
                buffer.writeVarInt(recipe.ingredients.size());for(var ingredient:recipe.ingredients)Ingredient.CONTENTS_STREAM_CODEC.encode(buffer,ingredient);
                ItemStack.STREAM_CODEC.encode(buffer,recipe.result);buffer.writeVarInt(recipe.mana);buffer.writeVarInt(recipe.biomes);buffer.writeVarInt(recipe.creatures);
            },buffer-> {
                int count=buffer.readVarInt();if(count<1 || count>3)throw new IllegalArgumentException("Invalid ingredient count");
                var ingredients=new java.util.ArrayList<Ingredient>(count);for(int i=0;i<count;i++)ingredients.add(Ingredient.CONTENTS_STREAM_CODEC.decode(buffer));
                return new MagicRecipe(ingredients,ItemStack.STREAM_CODEC.decode(buffer),buffer.readVarInt(),buffer.readVarInt(),buffer.readVarInt(),research);
            });
        }
        @Override public MapCodec<MagicRecipe> codec() { return codec; }
        @Override public StreamCodec<RegistryFriendlyByteBuf,MagicRecipe> streamCodec() { return stream; }
    }
}
