package dev.reimagined.magic.client;

import dev.reimagined.magic.MagicStationMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public final class MagicStationScreen extends AbstractContainerScreen<MagicStationMenu> {
    private static final ResourceLocation TEXTURE=ResourceLocation.parse("reimagined:textures/gui/magic_station.png");
    private Button work;
    public MagicStationScreen(MagicStationMenu menu,Inventory inventory,Component title) {super(menu,inventory,title);imageHeight=226;inventoryLabelY=132;}
    @Override protected void init() {
        super.init();work=addRenderableWidget(Button.builder(Component.translatable(menu.research?"reimagined.magic.research":"reimagined.magic.infuse"),button-> {
            if(minecraft.gameMode!=null)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,0);
        }).bounds(leftPos+8,topPos+76,160,20).build());
    }
    @Override protected void containerTick() {super.containerTick();if(minecraft.level!=null && minecraft.level.getGameTime()%20==0)menu.refreshPreview();}
    @Override protected void renderBg(GuiGraphics graphics,float partial,int mouseX,int mouseY) {
        graphics.blit(TEXTURE,leftPos,topPos,0,0,imageWidth,imageHeight,176,226);
        var preview=menu.preview();if(!preview.isEmpty())graphics.renderItem(preview,leftPos+102,topPos+30);
    }
    @Override protected void renderLabels(GuiGraphics graphics,int mouseX,int mouseY) {
        super.renderLabels(graphics,mouseX,mouseY);
        graphics.drawWordWrap(font,Component.translatable("reimagined.magic.status."+menu.value(1)),8,51,160,0x353148);
        graphics.drawString(font,Component.translatable("reimagined.magic.mana",menu.value(0),menu.value(5)),8,100,0x403070,false);
        graphics.drawString(font,Component.translatable("reimagined.magic.observations",menu.value(2),menu.value(6),menu.value(3),menu.value(7)),8,111,0x403070,false);
        work.active=menu.value(1)==MagicStationMenu.READY;
    }
    @Override public void render(GuiGraphics graphics,int mouseX,int mouseY,float partial) {
        super.render(graphics,mouseX,mouseY,partial);renderTooltip(graphics,mouseX,mouseY);
        if(mouseX>=leftPos+102 && mouseX<leftPos+118 && mouseY>=topPos+30 && mouseY<topPos+46 && !menu.preview().isEmpty())
            graphics.renderTooltip(font,menu.preview(),mouseX,mouseY);
    }
}
