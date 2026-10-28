package dev.reimagined.creature.client;

import dev.reimagined.creature.Boar;
import net.minecraft.client.model.QuadrupedModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;

/** Original compact quadruped mesh: heavy shoulders, long snout, ears, mane and adult tusks. */
public final class BoarModel extends QuadrupedModel<Boar> {
    public static final ModelLayerLocation LAYER=new ModelLayerLocation(ResourceLocation.parse("reimagined:boar"),"main");
    private final ModelPart tusks;
    public BoarModel(ModelPart root) {super(root,false,8,4,2,2,24);tusks=root.getChild("head").getChild("tusks");}
    public static LayerDefinition layer() {
        var mesh=new MeshDefinition();var root=mesh.getRoot();
        var head=root.addOrReplaceChild("head",CubeListBuilder.create().texOffs(0,0).addBox(-3,-3,-6,6,6,7)
                .texOffs(28,0).addBox(-2,-1,-9,4,3,3),PartPose.offset(0,13,-7));
        head.addOrReplaceChild("ears",CubeListBuilder.create().texOffs(42,0).addBox(-4,-6,-1,2,4,1)
                .addBox(2,-6,-1,2,4,1),PartPose.ZERO);
        head.addOrReplaceChild("tusks",CubeListBuilder.create().texOffs(54,0).addBox(-3,-2,-8,1,4,1)
                .addBox(2,-2,-8,1,4,1),PartPose.ZERO);
        var body=root.addOrReplaceChild("body",CubeListBuilder.create().texOffs(0,20).addBox(-4,-5,-8,8,10,16),PartPose.offset(0,13,1));
        body.addOrReplaceChild("mane",CubeListBuilder.create().texOffs(48,16).addBox(-.5f,-7,-7,1,2,7),PartPose.ZERO);
        body.addOrReplaceChild("tail",CubeListBuilder.create().texOffs(42,7).addBox(-.5f,-1,0,1,1,4),PartPose.offset(0,-2,8));
        root.addOrReplaceChild("right_hind_leg",leg(),PartPose.offset(-2.5f,17,6));
        root.addOrReplaceChild("left_hind_leg",leg(),PartPose.offset(2.5f,17,6));
        root.addOrReplaceChild("right_front_leg",leg(),PartPose.offset(-2.5f,17,-5));
        root.addOrReplaceChild("left_front_leg",leg(),PartPose.offset(2.5f,17,-5));
        return LayerDefinition.create(mesh,64,64);
    }
    private static CubeListBuilder leg() {return CubeListBuilder.create().texOffs(48,36).addBox(-1.5f,0,-1.5f,3,7,3);}
    @Override public void setupAnim(Boar boar,float swing,float amount,float age,float yaw,float pitch) {
        super.setupAnim(boar,swing,amount,age,yaw,pitch);tusks.visible=!boar.isBaby();
    }
}
