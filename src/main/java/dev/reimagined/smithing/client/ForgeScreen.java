package dev.reimagined.smithing.client;

import dev.reimagined.smithing.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public final class ForgeScreen extends AbstractContainerScreen<ForgeMenu> {
    private Button work;
    public ForgeScreen(ForgeMenu menu,Inventory inventory,Component title){super(menu,inventory,title);imageHeight=232;inventoryLabelY=138;}
    @Override protected void init(){super.init();work=addRenderableWidget(Button.builder(Component.translatable("reimagined.forge.work"),b->{if(minecraft.gameMode!=null)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,0);}).bounds(leftPos+8,topPos+112,160,20).build());}
    @Override protected void containerTick(){super.containerTick();if(minecraft.level!=null && minecraft.level.getGameTime()%20==0)menu.refreshPreview();}
    @Override protected void renderBg(GuiGraphics graphics,float partial,int mx,int my){
        graphics.blit(ResourceLocation.parse("reimagined:textures/gui/smithing_forge.png"),leftPos,topPos,0,0,176,232,176,232);
        var preview=menu.preview();if(!preview.isEmpty())graphics.renderItem(preview,leftPos+106,topPos+44);
    }
    @Override protected void renderLabels(GuiGraphics graphics,int mx,int my){
        super.renderLabels(graphics,mx,my);graphics.drawString(font,Component.translatable("reimagined.forge.training",menu.value(1),menu.value(2)),8,90,0x493724,false);work.active=menu.value(0)==ForgeMenu.READY;
        graphics.drawString(font,Component.translatable("reimagined.forge.status."+menu.value(0)),8,101,0x493724,false);
    }
    @Override public void render(GuiGraphics graphics,int mx,int my,float partial){
        super.render(graphics,mx,my,partial);renderTooltip(graphics,mx,my);
        if(hoveredSlot!=null && hoveredSlot.index<7 && hoveredSlot.getItem().isEmpty())graphics.renderTooltip(font,Component.translatable("reimagined.forge.slot."+hoveredSlot.index),mx,my);
        if(mx>=leftPos+106 && mx<leftPos+122 && my>=topPos+44 && my<topPos+60 && !menu.preview().isEmpty())graphics.renderTooltip(font,menu.preview(),mx,my);
    }
}
