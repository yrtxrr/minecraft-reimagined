package dev.reimagined.creature.client;

import dev.reimagined.creature.Boar;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;

public final class BoarRenderer extends MobRenderer<Boar,BoarModel> {
    private static final ResourceLocation TEXTURE=ResourceLocation.parse("reimagined:textures/entity/boar.png");
    public BoarRenderer(EntityRendererProvider.Context context) {super(context,new BoarModel(context.bakeLayer(BoarModel.LAYER)),0.55f);}
    @Override public ResourceLocation getTextureLocation(Boar boar) {return TEXTURE;}
}
