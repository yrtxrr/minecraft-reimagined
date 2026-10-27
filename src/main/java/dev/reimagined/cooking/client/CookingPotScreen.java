package dev.reimagined.cooking.client;

import dev.reimagined.cooking.CookingPotMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public final class CookingPotScreen extends AbstractContainerScreen<CookingPotMenu> {
    private static final ResourceLocation BACKGROUND = ResourceLocation.fromNamespaceAndPath("reimagined","textures/gui/cooking_pot.png");
    public CookingPotScreen(CookingPotMenu menu, Inventory inventory, Component title) {
        super(menu,inventory,title); imageHeight=158; inventoryLabelY=64;
    }
    @Override protected void renderBg(GuiGraphics graphics, float partial, int mouseX, int mouseY) {
        graphics.blit(BACKGROUND,leftPos,topPos,0,0,imageWidth,imageHeight,176,158);
        graphics.fill(leftPos+26,topPos+54,leftPos+150,topPos+58,0xFF37332C);
        int width = menu.total() > 0 ? Math.clamp(menu.progress()*124/menu.total(),0,124) : 0;
        graphics.fill(leftPos+26,topPos+54,leftPos+26+width,topPos+58,0xFFCF8B3B);
        graphics.fill(leftPos+10,topPos+30,leftPos+18,topPos+46,menu.heated()?0xFFEF882F:0xFF595959);
    }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        super.render(graphics,mouseX,mouseY,partial); renderTooltip(graphics,mouseX,mouseY);
        String hint = null;
        if (mouseX>=leftPos+8 && mouseX<leftPos+21 && mouseY>=topPos+27 && mouseY<topPos+49)
            hint = menu.heated()?"reimagined.cooking.hot":"reimagined.cooking.cold";
        else if (hoveredSlot != null && hoveredSlot.getItem().isEmpty())
            hint = hoveredSlot.index<3?"reimagined.cooking.ingredients":hoveredSlot.index==3?"reimagined.cooking.bowls":hoveredSlot.index==4?"reimagined.cooking.output":null;
        else if (mouseX>=leftPos+26 && mouseX<leftPos+150 && mouseY>=topPos+51 && mouseY<topPos+61)
            graphics.renderTooltip(font,Component.translatable("reimagined.cooking.progress",menu.progress()/20,menu.total()/20),mouseX,mouseY);
        if (hint != null) graphics.renderTooltip(font,Component.translatable(hint),mouseX,mouseY);
    }
}
