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
 * 在骷髅马尾部骨架空腔中渲染烟花火箭。
 * 渲染方式参考 item display 实体：火箭横放、尾部朝后，数量由实际装配数量决定（最多 4 个）。
 */
public class FireworkRocketLayer extends RenderLayer<EquineRenderState, AbstractEquineModel<EquineRenderState>> {
    /** 最多渲染的烟花火箭数量 */
    private static final int MAX_RENDER_COUNT = 4;

    /** 火箭沿身体宽度方向（X）并排的间距*/
    private static final float ROCKET_X_SPACING = 0.1f;

    /** 火箭中心在身体局部空间中的 Z 坐标（前负后正）*/
    private static final float ROCKET_Z = 0.1f;

    /** 尾部空腔在身体局部空间中的 Y 坐标（上负下正） */
    private static final float ROCKET_Y = -0.2f;

    public FireworkRocketLayer(RenderLayerParent<EquineRenderState, AbstractEquineModel<EquineRenderState>> renderer) {
        super(renderer);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords,
                       EquineRenderState state, float yRot, float xRot) {
        FireworkDashHorseRenderState access = (FireworkDashHorseRenderState) (Object) state;
        int count = Math.min(access.funnymod$getFireworkCount(), MAX_RENDER_COUNT);
        if (count <= 0 || access.funnymod$getFireworkItem().isEmpty()) {
            return;
        }
        if (state.isInvisible && !state.appearsGlowing()) {
            return;
        }

        // 身体骨骼：应用其变换（含站立/行走动画），进入身体局部空间（原点 = 身体中心）
        ModelPart body = this.getParentModel().root().getChild("body");
        for (int i = 0; i < count; i++) {
            poseStack.pushPose();
            body.translateAndRotate(poseStack);

            // 尾部空腔：身体局部空间中，头部在 -z，尾部在 +z，火箭并排横放
            float xOffset = (count == 1) ? 0.0f : (i - (count - 1) / 2.0f) * ROCKET_X_SPACING;
            poseStack.translate(xOffset, ROCKET_Y, ROCKET_Z);

            // 横放 + 滚转 90°
            poseStack.mulPose(Axis.ZP.rotationDegrees(90.0f));
            poseStack.mulPose(Axis.XP.rotationDegrees(-90.0f));

            access.funnymod$getFireworkItem().submit(poseStack, submitNodeCollector, lightCoords,
                    OverlayTexture.NO_OVERLAY, state.outlineColor);
            poseStack.popPose();
        }
    }
}
