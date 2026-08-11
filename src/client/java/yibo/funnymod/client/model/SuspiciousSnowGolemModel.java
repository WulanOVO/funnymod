package yibo.funnymod.client.model;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;
import yibo.funnymod.client.render.SuspiciousSnowGolemRenderState;

public class SuspiciousSnowGolemModel extends EntityModel<SuspiciousSnowGolemRenderState> {
    private final ModelPart creeperHead;
    private final ModelPart upperBody;
    private final ModelPart leftArm;
    private final ModelPart rightArm;

    public SuspiciousSnowGolemModel(ModelPart root) {
        super(root);
        creeperHead = root.getChild("creeper_head");
        upperBody = root.getChild("upper_body");
        leftArm = root.getChild("left_arm");
        rightArm = root.getChild("right_arm");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        CubeDeformation deformation = new CubeDeformation(-0.5f);

        // 苦力怕头：8x8x8, 南瓜盖住时不可见
        root.addOrReplaceChild("creeper_head",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4.0f, -8.0f, -4.0f, 8.0f, 8.0f, 8.0f),
                PartPose.offset(0.0f, 4.0f, 0.0f));

        // 雪傀儡上身：10x10x10
        root.addOrReplaceChild("upper_body",
                CubeListBuilder.create().texOffs(0, 16).addBox(-5.0f, -10.0f, -5.0f, 10.0f, 10.0f, 10.0f, deformation),
                PartPose.offset(0.0f, 13.0f, 0.0f));

        // 雪傀儡下身：12x12x12
        root.addOrReplaceChild("lower_body",
                CubeListBuilder.create().texOffs(0, 36).addBox(-6.0f, -12.0f, -6.0f, 12.0f, 12.0f, 12.0f, deformation),
                PartPose.offset(0.0f, 24.0f, 0.0f));

        // 雪傀儡手臂：12x2x2
        CubeListBuilder arm = CubeListBuilder.create().texOffs(32, 0)
                .addBox(-1.0f, 0.0f, -1.0f, 12.0f, 2.0f, 2.0f, deformation);
        root.addOrReplaceChild("left_arm", arm,
                PartPose.offsetAndRotation(5.0f, 6.0f, 1.0f, 0.0f, 0.0f, 1.0f));
        root.addOrReplaceChild("right_arm", arm,
                PartPose.offsetAndRotation(-5.0f, 6.0f, -1.0f, 0.0f, (float) Math.PI, -1.0f));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(SuspiciousSnowGolemRenderState state) {
        super.setupAnim(state);

        // 南瓜盖住时隐藏苦力怕头，防止穿模露馅
        creeperHead.visible = !state.hasPumpkin;
        creeperHead.yRot = state.yRot * ((float) Math.PI / 180);
        creeperHead.xRot = state.xRot * ((float) Math.PI / 180);

        // 上半身随头轻微旋转
        upperBody.yRot = state.yRot * ((float) Math.PI / 180) * 0.25f;
        float sin = Mth.sin(upperBody.yRot);
        float cos = Mth.cos(upperBody.yRot);

        // 手臂跟随身体旋转
        leftArm.yRot = upperBody.yRot;
        rightArm.yRot = upperBody.yRot + Mth.PI;
        leftArm.x = cos * 5.0f;
        leftArm.z = -sin * 5.0f;
        rightArm.x = -cos * 5.0f;
        rightArm.z = sin * 5.0f;
    }

    public ModelPart getHead() {
        return creeperHead;
    }
}
