package yibo.funnymod.client.mixin;

import net.minecraft.client.Camera;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yibo.funnymod.util.GlidePitchUtil;

/**
 * 滑翔相机俯仰跟随：鞍鞘坐骑滑翔时，原版相机锚点固定为
 * 实体位置 + 垂直眼高，大俯仰角时马身遮挡视野。
 * 这里把相机位置绕坐骑躯干中心旋转同样的滑翔俯仰角，
 * 与座位旋转（LivingEntityMixin）和模型旋转（LivingEntityRendererMixin）一致。
 */
@Mixin(Camera.class)
public abstract class CameraMixin {

    /** 俯冲到 -90° 时相机沿视线方向的最大前推距离（格），用于越过马头 */
    private static final float DIVE_FORWARD_SHIFT = 0.75f;

    @Shadow
    protected abstract void setPosition(Vec3 position);

    @Shadow
    protected abstract void move(float forwards, float up, float right);

    @Inject(method = "alignWithEntity", at = @At("TAIL"))
    private void funnymod$rotateCameraWithGlidePitch(float partialTicks, CallbackInfo ci) {
        Camera self = (Camera) (Object) this;
        // 两种第三人称下相机先被原版拉远再反向观察，旋转/前推作用在拉远位置上
        // 语义完全不同（表现为移动怪异），保持原版行为，仅第一人称做滑翔相机跟随
        if (self.isDetached()) {
            return;
        }
        Entity cameraEntity = self.entity();
        if (cameraEntity == null) {
            return;
        }
        LivingEntity vehicle;
        if (cameraEntity instanceof LivingEntity living && GlidePitchUtil.shouldApplyGlideRotation(living)) {
            // 相机实体即滑翔坐骑本身（旁观视角）
            vehicle = living;
        } else if (cameraEntity.isPassenger() && cameraEntity.getVehicle() instanceof LivingEntity mount
            && GlidePitchUtil.shouldApplyGlideRotation(mount)) {
            // 常规情况：相机实体是骑手（本地玩家）
            vehicle = mount;
        } else {
            return;
        }
        float pitch = GlidePitchUtil.computeGlidePitch(vehicle, partialTicks);
        if (GlidePitchUtil.isNegligible(pitch)) {
            return;
        }
        // 旋转轴 yaw 必须用插值的马身旋转：tickRidden 里 yRotO 被设为当前值（不插值），
        // 直接用 getYRot(partialTicks) 会每 tick 跳变，水平转视角时相机相对马身抖动
        float bodyYaw = Mth.rotLerp(partialTicks, vehicle.yBodyRotO, vehicle.yBodyRot);
        Vec3 pivot = vehicle.getPosition(partialTicks).add(0.0, GlidePitchUtil.getPivotY(vehicle), 0.0);
        this.setPosition(GlidePitchUtil.rotateAroundPoint(
            self.position(), pivot, bodyYaw, pitch));
        // 俯冲时相机绕中心旋转后仍落在马头正上方，前下方视线被马头扫到；
        // 沿视线方向按俯冲深度比例前推，让镜头越过马头（爬升时无遮挡，不补偿）
        float dive = Mth.sin(-pitch * Mth.DEG_TO_RAD);
        if (dive > 0.0f) {
            this.move(DIVE_FORWARD_SHIFT * dive, 0.0f, 0.0f);
        }
    }
}
