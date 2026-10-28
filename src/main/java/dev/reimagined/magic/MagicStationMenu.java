package dev.reimagined.magic;

import dev.reimagined.content.SpellWand;
import dev.reimagined.research.Discoveries;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;

public final class MagicStationMenu extends AbstractContainerMenu {
    public static final int READY=0,NO_RECIPE=1,LOW_MANA=2,EXPLORE=3,FULL=4;
    public final boolean research;
    private final Container station;
    private final Player owner;
    private final ContainerData data;
    private final RecipeManager.CachedCheck<MagicRecipe.Input,MagicRecipe> check;
    private MagicRecipe preview;
    private long lastAction=Long.MIN_VALUE;
    private int refreshTicks;
    public MagicStationMenu(int id,Inventory inventory,boolean research) { this(id,inventory,new SimpleContainer(4),research); }
    public MagicStationMenu(int id,Inventory inventory,Container station,boolean research) {
        super(research?MagicContent.RESEARCH_MENU.get():MagicContent.ALTAR_MENU.get(),id);
        checkContainerSize(station,4);this.station=station;this.research=research;owner=inventory.player;
        data=new SimpleContainerData(8);data.set(1,NO_RECIPE);
        check=RecipeManager.createCheck(research?MagicContent.RESEARCH.get():MagicContent.INFUSION.get());
        station.startOpen(owner);
        for(int i=0;i<3;i++)addSlot(new Slot(station,i,26+i*18,30));
        addSlot(new Slot(station,3,134,30) { @Override public boolean mayPlace(ItemStack stack) {return false;} });
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(inventory,9+row*9+col,8+col*18,144+row*18));
        for(int col=0;col<9;col++)addSlot(new Slot(inventory,col,8+col*18,202));
        addDataSlots(data);refresh();
    }
    private MagicRecipe.Input input() {return new MagicRecipe.Input(station.getItem(0),station.getItem(1),station.getItem(2));}
    public ItemStack preview() {return preview==null?ItemStack.EMPTY:preview.getResultItem(owner.registryAccess());}
    public void refreshPreview() {if(owner.level().isClientSide)refresh();}
    public int value(int index) {return data.get(index);}
    private void refresh() {
        preview=check.getRecipeFor(input(),owner.level()).map(RecipeHolder::value).orElse(null);
        if(!(owner instanceof ServerPlayer server))return;
        var discoveries=Discoveries.read(owner);
        data.set(0,SpellWand.mana(server));data.set(2,discoveries.biomes().size());data.set(3,discoveries.creatures().size());
        data.set(4,SpellKnowledge.mask(owner));data.set(5,preview==null?0:preview.mana());
        data.set(6,preview==null?0:preview.biomes());data.set(7,preview==null?0:preview.creatures());
        data.set(1,status(server,discoveries));
    }
    private int status(ServerPlayer player,Discoveries discoveries) {
        if(preview==null)return NO_RECIPE;
        if(!player.isCreative() && (discoveries.biomes().size()<preview.biomes() || discoveries.creatures().size()<preview.creatures()))return EXPLORE;
        if(!player.isCreative() && SpellWand.mana(player)<preview.mana())return LOW_MANA;
        var result=preview.getResultItem(player.registryAccess());var output=station.getItem(3);
        if(!output.isEmpty() && (!ItemStack.isSameItemSameComponents(result,output) || output.getCount()+1>Math.min(station.getMaxStackSize(),output.getMaxStackSize())))return FULL;
        return READY;
    }
    @Override public void broadcastChanges() {
        // Only open menus do any periodic work; no block ticker, sleeping station or global lookup loop.
        if(++refreshTicks>=20) {refreshTicks=0;refresh();}
        super.broadcastChanges();
    }
    @Override public boolean clickMenuButton(Player player,int button) {
        if(button!=0 || player!=owner || !(player instanceof ServerPlayer server) || player.isSpectator()
                || !(station instanceof MagicStationBlockEntity entity) || player.containerMenu!=this || !stillValid(player) || !entity.canOpen(player))return false;
        long now=server.serverLevel().getServer().overworld().getGameTime();
        if(lastAction!=Long.MIN_VALUE && now>=lastAction && now-lastAction<10)return false;
        lastAction=now;refresh();
        if(data.get(1)!=READY)return false;
        var result=preview.assemble(input(),player.registryAccess());
        var remainders=NonNullList.withSize(3,ItemStack.EMPTY);
        for(int i=0;i<3;i++)if(!station.getItem(i).isEmpty())remainders.set(i,station.getItem(i).copyWithCount(1).getCraftingRemainingItem());
        if(!SpellWand.spendMana(server,preview.mana()))return false;
        for(int i=0;i<3;i++) {
            var ingredient=station.getItem(i);if(ingredient.isEmpty())continue;
            ingredient.shrink(1);var remainder=remainders.get(i);
            if(!remainder.isEmpty()) {
                if(ingredient.isEmpty())station.setItem(i,remainder);else player.drop(remainder,false);
            }
        }
        if(station.getItem(3).isEmpty())station.setItem(3,result);else station.getItem(3).grow(1);
        station.setChanged();
        dev.reimagined.progression.PlayerSkills.award(server,dev.reimagined.progression.Skill.MAGE,research?8:4);
        refresh();broadcastChanges();return true;
    }
    @Override public boolean stillValid(Player player) {return station.stillValid(player);}
    @Override public void removed(Player player) {super.removed(player);station.stopOpen(player);}
    @Override public ItemStack quickMoveStack(Player player,int index) {
        if(index<0 || index>=slots.size())return ItemStack.EMPTY;
        var slot=slots.get(index);if(!slot.hasItem())return ItemStack.EMPTY;
        var stack=slot.getItem();var original=stack.copy();
        if(index<4) {if(!moveItemStackTo(stack,4,slots.size(),true))return ItemStack.EMPTY;}
        else if(!moveItemStackTo(stack,0,3,false))return ItemStack.EMPTY;
        if(stack.isEmpty())slot.setByPlayer(ItemStack.EMPTY);else slot.setChanged();slot.onTake(player,stack);return original;
    }
}
