package dev.reimagined.settlements.client;
import dev.reimagined.settlements.Settler;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
public final class SettlerRenderer extends MobRenderer<Settler,SettlerModel> {
    // Referenced from Minecraft, never copied or redistributed as a project PNG.
    private static final ResourceLocation TEXTURE=ResourceLocation.withDefaultNamespace("textures/entity/villager/villager.png");
    public SettlerRenderer(EntityRendererProvider.Context context) { super(context,new SettlerModel(context.bakeLayer(ModelLayers.VILLAGER)),0.5F); }
    @Override public ResourceLocation getTextureLocation(Settler settler) { return TEXTURE; }
}
