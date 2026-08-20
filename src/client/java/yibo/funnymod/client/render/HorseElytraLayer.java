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
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import yibo.funnymod.client.model.HorseElytraModel;

/**
 * 在马背上渲染鞘翅，机制全部参考玩家 {@link net.minecraft.client.renderer.entity.layers.WingsLayer}：
 * 鞍槽物品含 {@link DataComponents#GLIDER} 组件（即鞍鞘）时显示鞘翅，
 * 翅膀旋转由 {@link SaddleElytraRenderState} 提供（复刻玩家 elytraAnimationState）。
 * 纹理使用原版鞘翅装备纹理，由资源包可覆盖。
 *
 * <p>所有几何参数（偏移量、基础俯仰角）均以块为单位提取为常量，方便微调。
 * 参考项目内 {@link FireworkRocketLayer} 的小数值风格。</p>
 */
public class HorseElytraLayer
    extends RenderLayer<EquineRenderState, AbstractEquineModel<EquineRenderState>> {

    /**
     * 原版鞘翅装备纹理路径（{@code EquipmentAssets.ELYTRA} + {@code LayerType.WINGS}）
     */
    private static final Identifier ELYTRA_TEXTURE =
        Identifier.withDefaultNamespace("textures/entity/equipment/wings/elytra.png");

    /**
     * 鞘翅根在身体局部空间中的上下偏移（上负下正）
     */
    private static final float WING_OFFSET_Y = -0.38f;

    /**
     * 鞘翅根在身体局部空间中的前后偏移（前负后正）
     */
    private static final float WING_OFFSET_Z = -0.45f;

    /**
     * 基础俯仰角（度）：原版鞘翅模型默认从根向下延伸（玩家身上合理），
     * 马背上需绕 X 轴旋转此角度让翅膀水平展开贴在背上。
     */
    private static final float WING_BASE_PITCH_DEG = 80.0f;

    private final HorseElytraModel elytraModel;

    public HorseElytraLayer(
        RenderLayerParent<EquineRenderState, AbstractEquineModel<EquineRenderState>> renderer,
        HorseElytraModel elytraModel
    ) {
        super(renderer);
        this.elytraModel = elytraModel;
    }

    @Override
    public void submit(
        PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords,
        EquineRenderState state, float yRot, float xRot
    ) {
        ItemStack saddle = state.saddle;
        if (saddle.isEmpty() || !saddle.has(DataComponents.GLIDER)) {
            return;
        }
        if (state.isInvisible && !state.appearsGlowing()) {
            return;
        }
        // 进入身体骨骼局部空间（含站立/行走动画），原点 = 身体中心
        ModelPart body = this.getParentModel().root().getChild("body");
        poseStack.pushPose();
        body.translateAndRotate(poseStack);

        poseStack.translate(0.0f, WING_OFFSET_Y, WING_OFFSET_Z);
        // 基础俯仰：让鞘翅水平贴在背上，玩家 WingsLayer 不需要此旋转（人体竖直，翅膀默认向下延伸合理）
        poseStack.mulPose(Axis.XP.rotationDegrees(WING_BASE_PITCH_DEG));

        this.elytraModel.setupAnim(state);
        submitNodeCollector.submitModel(
            this.elytraModel, state, poseStack,
            this.elytraModel.renderType(ELYTRA_TEXTURE),
            lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor, null
        );

        poseStack.popPose();
    }
}
