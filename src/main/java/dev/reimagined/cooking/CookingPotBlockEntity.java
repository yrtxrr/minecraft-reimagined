package dev.reimagined.cooking;

import java.util.Optional;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.wrapper.SidedInvWrapper;

/** No BE ticker. Sleeping pots wake on inventory/neighbor changes; loaded active pots work once/second. */
public final class CookingPotBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {
    public static final int OUTPUT = 4, BOWL = 3;
    public static final TagKey<Block> HEAT_SOURCES = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("reimagined", "cooking_heat_sources"));
    public static final TagKey<Block> HEAT_CONDUCTORS = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("reimagined", "cooking_heat_conductors"));
    private static final int[] INPUT_SLOTS = {0,1,2}, BOWL_SLOTS = {3}, OUTPUT_SLOTS = {4};
    private static long recipeEpoch;
    private long seenEpoch = -1;
    private NonNullList<ItemStack> items = NonNullList.withSize(5, ItemStack.EMPTY);
    private final RecipeManager.CachedCheck<CookingRecipe.Input, CookingRecipe> check = RecipeManager.createCheck(CookingContent.COOKING.get());
    private Optional<RecipeHolder<CookingRecipe>> cached = Optional.empty();
    private boolean recipeDirty = true;
    private boolean restoring = true;
    private CookingRecipe progressRecipe;
    private ResourceLocation savedRecipe;
    private int progress, total, heated;
    private final IItemHandler top = new SidedInvWrapper(this, Direction.UP);
    private final IItemHandler side = new SidedInvWrapper(this, Direction.NORTH);
    private final IItemHandler bottom = new SidedInvWrapper(this, Direction.DOWN);
    final ContainerData data = new ContainerData() {
        @Override public int get(int i) { return switch(i) { case 0 -> progress; case 1 -> total; case 2 -> heated; default -> 0; }; }
        @Override public void set(int i, int value) { switch(i) { case 0 -> progress = value; case 1 -> total = value; case 2 -> heated = value; default -> {} } }
        @Override public int getCount() { return 3; }
    };
    public CookingPotBlockEntity(BlockPos pos, BlockState state) { super(CookingContent.ENTITY.get(), pos, state); }
    static void recipesReloaded() { recipeEpoch++; }
    public IItemHandler handler(Direction side) { return side == Direction.DOWN ? bottom : side == Direction.UP ? top : this.side; }
    @Override protected NonNullList<ItemStack> getItems() { return items; }
    @Override protected void setItems(NonNullList<ItemStack> value) { items = value; }
    @Override public int getContainerSize() { return 5; }
    @Override protected Component getDefaultName() { return Component.translatable("block.reimagined.cooking_pot"); }
    @Override protected AbstractContainerMenu createMenu(int id, Inventory inventory) { return new CookingPotMenu(id, inventory, this, data); }
    @Override public int[] getSlotsForFace(Direction face) { return face == Direction.DOWN ? OUTPUT_SLOTS : face == Direction.UP ? INPUT_SLOTS : BOWL_SLOTS; }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) {
        // Food-slot filtering deliberately permits datapack ingredients, including non-food spices.
        return slot >= 0 && slot < 3 ? !stack.is(Items.BOWL) : slot == BOWL && stack.is(Items.BOWL);
    }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction face) { return canPlaceItem(slot, stack); }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction face) { return face == Direction.DOWN && slot == OUTPUT; }
    @Override public void setChanged() { super.setChanged(); recipeDirty = true; wake(); }
    @Override public void clearContent() { super.clearContent(); progress = 0; total = 0; savedRecipe = null; progressRecipe = null; setChanged(); }
    @Override public void onLoad() { super.onLoad(); wake(); }
    public void wake() {
        if (level instanceof ServerLevel server && !isRemoved())
            server.scheduleTick(worldPosition, getBlockState().getBlock(), 20 + Math.floorMod(worldPosition.asLong(),20));
    }
    public int progress() { return progress; }
    public int total() { return total; }
    private CookingRecipe.Input input() { return new CookingRecipe.Input(items.get(0),items.get(1),items.get(2)); }
    /** Testable production entry, also called by the vanilla scheduled-block-tick dispatcher. */
    public void cookStep() {
        if (!(level instanceof ServerLevel server) || isRemoved()) return;
        var chunk = server.getChunkSource().getChunkNow(worldPosition.getX() >> 4, worldPosition.getZ() >> 4);
        if (chunk == null || chunk.getBlockEntity(worldPosition) != this) return;
        BlockState heat = chunk.getBlockState(worldPosition.below());
        if (heat.is(HEAT_CONDUCTORS)) heat = chunk.getBlockState(worldPosition.below(2));
        boolean hot = heat.is(HEAT_SOURCES)
                && (!heat.hasProperty(BlockStateProperties.LIT) || heat.getValue(BlockStateProperties.LIT))
                && (!heat.hasProperty(BlockStateProperties.WATERLOGGED) || !heat.getValue(BlockStateProperties.WATERLOGGED));
        heated = hot ? 1 : 0;
        boolean hasInput = !items.get(0).isEmpty() || !items.get(1).isEmpty() || !items.get(2).isEmpty();
        if (recipeDirty || seenEpoch != recipeEpoch) {
            cached = hasInput ? check.getRecipeFor(input(), level) : Optional.empty();
            recipeDirty = false; seenEpoch = recipeEpoch;
        }
        var holder = cached.orElse(null);
        CookingRecipe recipe = holder == null ? null : holder.value();
        if (progressRecipe != recipe || restoring) {
            // Restore only against the same saved recipe ID on initial load. Reloaded recipe objects reset work.
            boolean restored = restoring && savedRecipe != null && holder != null && savedRecipe.equals(holder.id());
            restoring = false;
            if (!restored) progress = 0;
            progressRecipe = recipe; savedRecipe = holder == null ? null : holder.id();
            total = recipe == null ? 0 : recipe.seconds() * 20;
            progress = Math.min(progress, Math.max(0,total - 20));
            super.setChanged();
        }
        ItemStack output = recipe == null ? ItemStack.EMPTY : recipe.getResultItem(level.registryAccess());
        ItemStack current = items.get(OUTPUT);
        boolean fits = !output.isEmpty() && (current.isEmpty() || ItemStack.isSameItemSameComponents(current, output)
                && current.getCount() + output.getCount() <= Math.min(getMaxStackSize(), current.getMaxStackSize()));
        boolean working = hot && recipe != null && items.get(BOWL).is(Items.BOWL) && fits;
        if (getBlockState().getValue(CookingPotBlock.COOKING) != working)
            server.setBlock(worldPosition, getBlockState().setValue(CookingPotBlock.COOKING, working), Block.UPDATE_CLIENTS);
        if (working) {
            progress += 20;
            if (progress >= total) {
                for (int i = 0; i < 3; i++) {
                    ItemStack ingredient = items.get(i);
                    if (ingredient.isEmpty()) continue;
                    ItemStack remainder = ingredient.getCraftingRemainingItem();
                    ingredient.shrink(1);
                    if (!remainder.isEmpty()) {
                        if (ingredient.isEmpty()) items.set(i,remainder);
                        else Containers.dropItemStack(level,worldPosition.getX()+.5,worldPosition.getY()+.8,worldPosition.getZ()+.5,remainder);
                    }
                }
                items.get(BOWL).shrink(1);
                if (current.isEmpty()) items.set(OUTPUT,output); else current.grow(output.getCount());
                progress = 0; recipeDirty = true;
                server.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
            }
            super.setChanged();
        }
        // Nonempty invalid/cold/blocked pots retry every5s for recipe reloads and heat state changes.
        // Completely empty pots retain no periodic work. No offline catch-up is performed.
        if (hasInput) server.scheduleTick(worldPosition,getBlockState().getBlock(),working ? 20 : 100);
    }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag,registries); ContainerHelper.saveAllItems(tag,items,registries);
        tag.putInt("CookProgress",progress);
        if (savedRecipe != null) tag.putString("CookRecipe",savedRecipe.toString());
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag,registries); items = NonNullList.withSize(5,ItemStack.EMPTY); ContainerHelper.loadAllItems(tag,items,registries);
        progress = Math.clamp(tag.getInt("CookProgress"),0,24000);
        savedRecipe = ResourceLocation.tryParse(tag.getString("CookRecipe")); progressRecipe = null;
        recipeDirty = true; cached = Optional.empty(); seenEpoch = -1; restoring = true;
    }
}
