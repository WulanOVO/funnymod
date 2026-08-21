package yibo.funnymod.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.animal.equine.AbstractEquineModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.EquineRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;

/**
 * 在骷髅马骨架前部渲染弩（平放）。
 * 渲染方式参考 {@link FireworkRocketLayer}：进入 body 骨骼局部空间，平移到前部空腔，
 * 应用旋转让弩横放贴在背上。仅当弩槽放有弩时渲染。
 */
public class CrossbowLayer extends RenderLayer<EquineRenderState, AbstractEquineModel<EquineRenderState>> {
    /** 弩中心在身体局部空间中的 Y 坐标（上负下正） */
    private static final float CROSSBOW_Y = -0.2f;

    /** 弩中心在身体局部空间中的 Z 坐标（前负后正） */
    private static final float CROSSBOW_Z = -1.0f;

    public CrossbowLayer(RenderLayerParent<EquineRenderState, AbstractEquineModel<EquineRenderState>> renderer) {
        super(renderer);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords,
                       EquineRenderState state, float yRot, float xRot) {
        FireworkDashHorseRenderState access = (FireworkDashHorseRenderState) (Object) state;
        if (!access.funnymod$hasCrossbow()) {
            return;
        }
        if (state.isInvisible && !state.appearsGlowing()) {
            return;
        }

        // 身体骨骼：应用其变换（含站立/行走动画），进入身体局部空间（原点 = 身体中心）
        ModelPart body = this.getParentModel().root().getChild("body");
        poseStack.pushPose();
        body.translateAndRotate(poseStack);

        // 平移到身体前部空腔
        poseStack.translate(0.0f, CROSSBOW_Y, CROSSBOW_Z);

        // 横放，由于物品纹理特性需偏航 45°
        poseStack.mulPose(Axis.XP.rotationDegrees(-90.0f));
        poseStack.mulPose(Axis.ZP.rotationDegrees(45.0f));

        access.funnymod$getCrossbowItem().submit(poseStack, submitNodeCollector, lightCoords,
                OverlayTexture.NO_OVERLAY, state.outlineColor);
        poseStack.popPose();
    }
}
