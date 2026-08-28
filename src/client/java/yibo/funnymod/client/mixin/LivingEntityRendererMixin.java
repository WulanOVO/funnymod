package yibo.funnymod.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yibo.funnymod.client.render.GlidePitchRenderState;
import yibo.funnymod.util.GlidePitchUtil;

/**
 * 滑翔整体俯仰渲染：鞍鞘坐骑滑翔时，按骑手视角俯仰旋转坐骑模型；
 * 骑手（含玩家）模型转同一角度并做头部补偿，视线始终指向瞄准方向。
 * 作用于所有生物渲染器基类，弩、鞍翅等图层在同一矩阵下随之旋转。
 */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void funnymod$extractGlidePitch(LivingEntity entity, LivingEntityRenderState state, float partialTicks, CallbackInfo ci) {
        GlidePitchRenderState glide = (GlidePitchRenderState) state;
        glide.funnymod$setGlidePitch(0.0f);
        glide.funnymod$setGlidePivotY(0.0f);

        if (entity.isPassenger() && entity.getVehicle() instanceof LivingEntity vehicle
            && GlidePitchUtil.shouldApplyGlidePitch(vehicle)) {
            // 骑手：与坐骑同俯仰，轴心换算到骑手渲染空间的局部高度
            float pitch = GlidePitchUtil.computeGlidePitch(vehicle, partialTicks);
            glide.funnymod$setGlidePitch(pitch);
            Vec3 pivotWorld = vehicle.getPosition(partialTicks).add(0.0, GlidePitchUtil.getPivotY(vehicle), 0.0);
            float pivotLocalY = (float) (pivotWorld.y - entity.getPosition(partialTicks).y) / state.scale;
            glide.funnymod$setGlidePivotY(pivotLocalY);
            // 头部补偿：身体随坐骑俯仰后，头部相对身体 = 视角俯仰 + 坐骑俯仰
            state.xRot += pitch;
        } else if (GlidePitchUtil.shouldApplyGlideRotation(entity)) {
            // 坐骑本体：绕自身躯干中心旋转
            glide.funnymod$setGlidePitch(GlidePitchUtil.computeGlidePitch(entity, partialTicks));
            glide.funnymod$setGlidePivotY((float) GlidePitchUtil.getPivotY(entity) / state.scale);
            // 马头相对身体固定：实体 xRot 会驱动头部俯仰，身体已随视角俯仰，
            // 头部再叠加会双重俯仰，滑翔时归零保持与身体平行。
            // 落地过渡期按混合系数平滑回到原版值，避免过渡结束瞬间跳变（抽动）
            if (GlidePitchUtil.shouldApplyGlidePitch(entity)) {
                state.xRot = 0.0f;
            } else {
                state.xRot *= 1.0f - GlidePitchUtil.landingBlend(entity);
            }
        }
    }

    @Inject(method = "setupRotations", at = @At("TAIL"))
    private void funnymod$applyGlidePitch(LivingEntityRenderState state, PoseStack poseStack, float bodyRot, float entityScale, CallbackInfo ci) {
        GlidePitchRenderState glide = (GlidePitchRenderState) state;
        float pitch = glide.funnymod$getGlidePitch();
        if (!GlidePitchUtil.isNegligible(pitch)) {
            // 绕轴心（躯干中心）俯仰，避免绕脚底旋转导致姿态漂移
            float pivotY = glide.funnymod$getGlidePivotY();
            poseStack.translate(0.0f, pivotY, 0.0f);
            poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
            poseStack.translate(0.0f, -pivotY, 0.0f);
        }
    }
}
