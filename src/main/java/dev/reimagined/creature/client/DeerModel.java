package dev.reimagined.creature.client;

import dev.reimagined.creature.Deer;
import net.minecraft.client.model.QuadrupedModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;

/** Original block model, including a long neck, ears, tail and branching antlers. */
public final class DeerModel extends QuadrupedModel<Deer> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath("reimagined", "deer"), "main");

    public DeerModel(ModelPart root) { super(root, false, 8, 4, 2, 2, 24); }

    public static LayerDefinition layer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-2, -4, -6, 4, 4, 7)
                .texOffs(22, 0).addBox(-1.5f, -1, -2, 3, 7, 3), PartPose.offset(0, 2, -5));
        head.addOrReplaceChild("left_ear", CubeListBuilder.create().texOffs(0, 12)
                .addBox(0, -3, -1, 4, 1, 2), PartPose.offsetAndRotation(1, -3, 0, 0, 0, -0.35f));
        head.addOrReplaceChild("right_ear", CubeListBuilder.create().texOffs(0, 12)
                .addBox(-4, -3, -1, 4, 1, 2), PartPose.offsetAndRotation(-1, -3, 0, 0, 0, 0.35f));
        head.addOrReplaceChild("left_antler", CubeListBuilder.create().texOffs(12, 12)
                .addBox(1, -10, 0, 1, 7, 1).addBox(1, -9, -2, 3, 1, 3)
                .addBox(3, -12, -2, 1, 4, 1), PartPose.ZERO);
        head.addOrReplaceChild("right_antler", CubeListBuilder.create().texOffs(12, 12)
                .addBox(-2, -10, 0, 1, 7, 1).addBox(-4, -9, -2, 3, 1, 3)
                .addBox(-4, -12, -2, 1, 4, 1), PartPose.ZERO);
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 18)
                .addBox(-3, -4, -7, 6, 8, 14), PartPose.offset(0, 10, 1));
        body.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(24, 12)
                .addBox(-1, -1, 0, 2, 3, 1), PartPose.offset(0, -2, 7));
        root.addOrReplaceChild("right_hind_leg", leg(), PartPose.offset(-2, 13, 6));
        root.addOrReplaceChild("left_hind_leg", leg(), PartPose.offset(2, 13, 6));
        root.addOrReplaceChild("right_front_leg", leg(), PartPose.offset(-2, 13, -4));
        root.addOrReplaceChild("left_front_leg", leg(), PartPose.offset(2, 13, -4));
        return LayerDefinition.create(mesh, 64, 64);
    }

    private static CubeListBuilder leg() {
        return CubeListBuilder.create().texOffs(42, 0).addBox(-1, 0, -1, 2, 11, 2);
    }
}
