package dev.reimagined.creature.client;

import net.minecraft.client.model.QuadrupedModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Animal;

/** Original compact meshes; vanilla quadruped head/leg animation, no per-tick packets. */
public final class SmallFaunaModel<T extends Animal> extends QuadrupedModel<T> {
    public static ModelLayerLocation layerId(String species) {
        return new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("reimagined",species),"main");
    }
    public SmallFaunaModel(ModelPart root) { super(root,false,5,2,2,2,24); }
    public static LayerDefinition layer(String species) {
        boolean otter=species.equals("otter");
        MeshDefinition mesh=new MeshDefinition();var root=mesh.getRoot();
        var head=root.addOrReplaceChild("head",CubeListBuilder.create().texOffs(0,0)
                .addBox(-2,-2,-4,4,4,4),PartPose.offset(0,18,-4));
        head.addOrReplaceChild("ears",CubeListBuilder.create().texOffs(20,0)
                .addBox(-2.5f,-3,-1,1,2,1).addBox(1.5f,-3,-1,1,2,1),PartPose.ZERO);
        var body=root.addOrReplaceChild("body",CubeListBuilder.create().texOffs(0,12)
                .addBox(-3,-3,-5,6,6,otter?12:9),PartPose.offset(0,19,0));
        body.addOrReplaceChild("tail",CubeListBuilder.create().texOffs(36,12)
                .addBox(-1,-1,0,2,2,otter?7:2),PartPose.offset(0,0,otter?7:4));
        for(String side:new String[]{"left","right"}) for(String end:new String[]{"front","hind"})
            root.addOrReplaceChild(side+"_"+end+"_leg",CubeListBuilder.create().texOffs(24,0)
                    .addBox(-1,0,-1,2,3,2),PartPose.offset(side.equals("left")?2:-2,21,end.equals("front")?-3:3));
        return LayerDefinition.create(mesh,64,64);
    }
}
