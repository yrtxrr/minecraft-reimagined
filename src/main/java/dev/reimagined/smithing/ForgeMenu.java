package dev.reimagined.smithing;

import dev.reimagined.progression.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;

/** Server-only manual transaction. One fuel unit per finished item; hammer/bellows are equipment, not ingredients. */
public final class ForgeMenu extends AbstractContainerMenu {
    public static final int READY=0,NO_RECIPE=1,FUEL=2,HAMMER=3,FULL=4,WORKED=5;
    private final Container forge;
    private final Player owner;
    private final ContainerData data=new SimpleContainerData(3);
    private final RecipeManager.CachedCheck<ForgingRecipe.Input,ForgingRecipe> check=RecipeManager.createCheck(SmithingContent.FORGING.get());
    private ItemStack preview=ItemStack.EMPTY;
    private long lastAction=Long.MIN_VALUE;
    private int refreshTicks;
    public ForgeMenu(int id,Inventory inventory){this(id,inventory,new SimpleContainer(7));}
    public ForgeMenu(int id,Inventory inventory,Container forge){
        super(SmithingContent.MENU.get(),id);checkContainerSize(forge,7);this.forge=forge;owner=inventory.player;forge.startOpen(owner);data.set(0,NO_RECIPE);
        for(int i=0;i<7;i++){
            final int index=i;int x=switch(i){case 0,4->8;case 1->26;case 2,5->44;case 3->62;default->134;};int y=i==6?44:i<4?28:64;
            addSlot(new Slot(forge,i,x,y){
                @Override public boolean mayPlace(ItemStack stack){return index!=6 && switch(index){case 3->stack.is(net.minecraft.tags.ItemTags.COALS);case 4->stack.is(SmithingContent.HAMMERS) && stack.isDamageableItem();case 5->stack.is(SmithingContent.BELLOWS.get());default->forge.canPlaceItem(index,stack);};}
                @Override public int getMaxStackSize(){return index==0 || index==4 || index==5 || index==6?1:64;}
            });
        }
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(inventory,9+row*9+col,8+col*18,150+row*18));
        for(int col=0;col<9;col++)addSlot(new Slot(inventory,col,8+col*18,208));
        addDataSlots(data);refresh();
    }
    private ForgingRecipe.Input input(){return new ForgingRecipe.Input(forge.getItem(0),forge.getItem(1),forge.getItem(2));}
    public int value(int i){return data.get(i);}
    public ItemStack preview(){return preview.copy();}
    private void refresh(){
        int rank=owner instanceof ServerPlayer server?PlayerSkills.get(server,Skill.SMITH).rank():data.get(1);
        var recipe=check.getRecipeFor(input(),owner.level()).orElse(null);
        preview=recipe==null?ItemStack.EMPTY:recipe.value().assemble(input(),owner.registryAccess(),rank,forge.getItem(5).is(SmithingContent.BELLOWS.get()));
        if(!(owner instanceof ServerPlayer))return;
        data.set(1,rank);data.set(2,preview.isEmpty()?0:ForgedEquipment.data(preview).getInt("Quality"));
        int status=preview.isEmpty()?(forge.getItem(0).isDamageableItem() && !ForgedEquipment.eligible(forge.getItem(0))?WORKED:NO_RECIPE):!forge.getItem(3).is(net.minecraft.tags.ItemTags.COALS)?FUEL:
                !forge.getItem(4).is(SmithingContent.HAMMERS) || !forge.getItem(4).isDamageableItem()?HAMMER:!forge.getItem(6).isEmpty()?FULL:READY;
        data.set(0,status);
    }
    public void refreshPreview(){if(owner.level().isClientSide)refresh();}
    @Override public void broadcastChanges(){if(++refreshTicks>=20){refreshTicks=0;refresh();}super.broadcastChanges();}
    @Override public boolean clickMenuButton(Player player,int button){
        if(button!=0 || player!=owner || !(player instanceof ServerPlayer server) || player.isSpectator() || !(forge instanceof ForgeBlockEntity entity)
                || player.containerMenu!=this || !stillValid(player) || !entity.canOpen(player))return false;
        long now=server.serverLevel().getServer().overworld().getGameTime();if(lastAction!=Long.MIN_VALUE && now>=lastAction && now-lastAction<10)return false;
        lastAction=now;refresh();if(data.get(0)!=READY)return false;
        var result=preview.copy();var remainders=net.minecraft.core.NonNullList.withSize(3,ItemStack.EMPTY);
        for(int i=1;i<=3;i++)remainders.set(i-1,forge.getItem(i).copyWithCount(1).getCraftingRemainingItem());
        forge.setItem(0,ItemStack.EMPTY);
        for(int i=1;i<=3;i++) {
            forge.getItem(i).shrink(1);var remainder=remainders.get(i-1);
            if(!remainder.isEmpty()){if(forge.getItem(i).isEmpty())forge.setItem(i,remainder);else player.drop(remainder,false);}
        }
        forge.getItem(4).hurtAndBreak(4,server.serverLevel(),server,item->{});
        forge.setItem(6,result);forge.setChanged();PlayerSkills.award(server,Skill.SMITH,16);
        var pos=entity.getBlockPos();server.serverLevel().playSound(null,pos,SoundEvents.ANVIL_USE,SoundSource.BLOCKS,.5F,1.1F);
        server.serverLevel().sendParticles(ParticleTypes.CRIT,pos.getX()+.5,pos.getY()+1,pos.getZ()+.5,8,.2,.1,.2,0);
        refresh();broadcastChanges();return true;
    }
    @Override public boolean stillValid(Player player){return forge.stillValid(player);}
    @Override public void removed(Player player){super.removed(player);forge.stopOpen(player);}
    @Override public ItemStack quickMoveStack(Player player,int index){
        if(index<0 || index>=slots.size())return ItemStack.EMPTY;var slot=slots.get(index);if(!slot.hasItem())return ItemStack.EMPTY;
        var stack=slot.getItem();var original=stack.copy();boolean moved;
        if(index<7)moved=moveItemStackTo(stack,7,slots.size(),true);
        else if(stack.is(SmithingContent.BELLOWS.get()))moved=moveItemStackTo(stack,5,6,false);
        else if(stack.is(SmithingContent.HAMMERS))moved=moveItemStackTo(stack,4,5,false);
        else if(stack.is(net.minecraft.tags.ItemTags.COALS))moved=moveItemStackTo(stack,3,4,false);
        else if(stack.isDamageableItem())moved=moveItemStackTo(stack,0,1,false);
        else moved=moveItemStackTo(stack,1,3,false);
        if(!moved)return ItemStack.EMPTY;if(stack.isEmpty())slot.setByPlayer(ItemStack.EMPTY);else slot.setChanged();slot.onTake(player,stack);return original;
    }
}
