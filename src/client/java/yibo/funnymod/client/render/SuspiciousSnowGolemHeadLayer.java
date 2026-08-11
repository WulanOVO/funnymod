package yibo.funnymod.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import yibo.funnymod.client.model.SuspiciousSnowGolemModel;

public class SuspiciousSnowGolemHeadLayer
        extends RenderLayer<SuspiciousSnowGolemRenderState, SuspiciousSnowGolemModel> {

    public SuspiciousSnowGolemHeadLayer(
            RenderLayerParent<SuspiciousSnowGolemRenderState, SuspiciousSnowGolemModel> renderer) {
        super(renderer);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords,
                       SuspiciousSnowGolemRenderState state, float yRot, float xRot) {
        if (state.headBlock.isEmpty()) {
            return;
        }
        if (state.isInvisible && !state.appearsGlowing()) {
            return;
        }
        poseStack.pushPose();
        this.getParentModel().getHead().translateAndRotate(poseStack);
        poseStack.translate(0.0f, -0.34375f, 0.0f);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0f));
        poseStack.scale(0.625f, -0.625f, -0.625f);
        int overlayCoords = LivingEntityRenderer.getOverlayCoords(state, 0.0f);
        poseStack.translate(-0.5f, -0.5f, -0.5f);
        if (state.appearsGlowing() && state.isInvisible) {
            state.headBlock.submitOnlyOutline(poseStack, submitNodeCollector, lightCoords, overlayCoords,
                    state.outlineColor);
        } else {
            state.headBlock.submit(poseStack, submitNodeCollector, lightCoords, overlayCoords,
                    state.outlineColor);
        }
        poseStack.popPose();
    }
}
