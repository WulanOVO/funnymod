package yibo.funnymod.client.model;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.EquineRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import yibo.funnymod.client.render.SaddleElytraRenderState;

/**
 * 马背鞘翅模型，复刻原版 {@link net.minecraft.client.model.object.equipment.ElytraModel}：
 * 同样的左右翅膀模型结构与默认姿态，但接受 {@link EquineRenderState}，
 * 并从 {@link SaddleElytraRenderState} 读取旋转值。
 * 渲染类型用 {@link RenderTypes#armorCutoutNoCull}，与原版 WingsLayer 一致。
 */
public class HorseElytraModel extends EntityModel<EquineRenderState> {
    private final ModelPart rightWing;
    private final ModelPart leftWing;

    public HorseElytraModel(ModelPart root) {
        super(root, RenderTypes::armorCutoutNoCull);
        this.leftWing = root.getChild("left_wing");
        this.rightWing = root.getChild("right_wing");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        CubeDeformation windDeformation = new CubeDeformation(1.0f);
        root.addOrReplaceChild("left_wing",
                CubeListBuilder.create().texOffs(22, 0).addBox(-10.0f, 0.0f, 0.0f, 10.0f, 20.0f, 2.0f, windDeformation),
                PartPose.offsetAndRotation(5.0f, 0.0f, 0.0f, 0.2617994f, 0.0f, -0.2617994f));
        root.addOrReplaceChild("right_wing",
                CubeListBuilder.create().texOffs(22, 0).mirror().addBox(0.0f, 0.0f, 0.0f, 10.0f, 20.0f, 2.0f, windDeformation),
                PartPose.offsetAndRotation(-5.0f, 0.0f, 0.0f, 0.2617994f, 0.0f, 0.2617994f));
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(EquineRenderState state) {
        super.setupAnim(state);
        SaddleElytraRenderState access = (SaddleElytraRenderState) (Object) state;
        this.leftWing.xRot = access.funnymod$getElytraRotX();
        this.leftWing.zRot = access.funnymod$getElytraRotZ();
        this.leftWing.yRot = access.funnymod$getElytraRotY();
        this.rightWing.yRot = -this.leftWing.yRot;
        this.rightWing.xRot = this.leftWing.xRot;
        this.rightWing.zRot = -this.leftWing.zRot;
    }
}
