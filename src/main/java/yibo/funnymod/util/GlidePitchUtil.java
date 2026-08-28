package yibo.funnymod.util;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import yibo.funnymod.entity.GlidePitchData;

/**
 * 滑翔整体俯仰：鞍鞘坐骑滑翔时，按骑手视角俯仰把坐骑与骑手作为整体旋转，
 * 俯冲/爬升姿态更自然，也避免马头遮挡俯视视线。
 * 俯仰角 = -骑手视角俯仰 × 过渡系数（复刻玩家鞘翅 fallFlyingScale 的平方过渡），
 * 再经每 tick 平滑（起飞/触地几 tick 内过渡），渲染与相机用 partialTicks 插值。
 */
public final class GlidePitchUtil {
    /** 旋转轴心高度（相对坐骑碰撞箱高度比例），约在躯干中心 */
    private static final float PIVOT_HEIGHT_RATIO = 0.45f;

    /** 俯仰角小于该值（度）时跳过旋转 */
    private static final float MIN_PITCH = 0.01f;

    /** 每 tick 向目标俯仰靠近的比例，触地/起飞约 3~5 tick 完成过渡 */
    private static final float SMOOTH_FACTOR = 0.5f;

    private GlidePitchUtil() {
    }

    /** 每 tick 更新平滑俯仰：滑翔中逼近目标（含起飞平方过渡），停止滑翔后衰减回 0 */
    public static void tickGlidePitch(LivingEntity vehicle) {
        GlidePitchData data = (GlidePitchData) vehicle;
        data.funnymod$setGlidePitchO(data.funnymod$getGlidePitch());
        float target = shouldApplyGlidePitch(vehicle) ? computeRawPitch(vehicle) : 0.0f;
        data.funnymod$setGlidePitch(Mth.lerp(SMOOTH_FACTOR, data.funnymod$getGlidePitch(), target));
        // 落地开始衰减时记录峰值，衰减完毕后清零，供头俯仰落地混合使用
        if (!shouldApplyGlidePitch(vehicle) && !isNegligible(data.funnymod$getGlidePitch())
            && data.funnymod$getGlidePitchPeak() == 0.0f) {
            data.funnymod$setGlidePitchPeak(data.funnymod$getGlidePitch());
        } else if (isNegligible(data.funnymod$getGlidePitch())) {
            data.funnymod$setGlidePitchPeak(0.0f);
        }
    }

    /**
     * 落地过渡系数：1 = 刚落地（头俯仰仍按滑翔归零），0 = 过渡完成（完全原版头俯仰）。
     * 按 当前俯仰/落地峰值 线性衰减。
     */
    public static float landingBlend(LivingEntity vehicle) {
        float peak = ((GlidePitchData) vehicle).funnymod$getGlidePitchPeak();
        if (isNegligible(peak)) {
            return 0.0f;
        }
        float ratio = Math.abs(computeGlidePitch(vehicle, 0.0f)) / Math.abs(peak);
        return Mth.clamp(ratio, 0.0f, 1.0f);
    }

    /** 当前 tick 的目标俯仰角（未平滑）。负值 = 低头俯冲，正值 = 抬头爬升。 */
    private static float computeRawPitch(LivingEntity vehicle) {
        Entity controller = vehicle.getControllingPassenger();
        float viewXRot = controller != null ? controller.getXRot() : vehicle.getXRot();
        float ticks = vehicle.getFallFlyingTicks();
        float ramp = Mth.clamp(ticks * ticks / 100.0f, 0.0f, 1.0f);
        return -viewXRot * ramp;
    }

    /**
     * 平滑后的滑翔俯仰角（度），partialTicks 插值。负值 = 低头俯冲，正值 = 抬头爬升。
     */
    public static float computeGlidePitch(LivingEntity vehicle, float partialTicks) {
        GlidePitchData data = (GlidePitchData) vehicle;
        return Mth.lerp(partialTicks, data.funnymod$getGlidePitchO(), data.funnymod$getGlidePitch());
    }

    /**
     * 是否需要对该坐骑应用滑翔俯仰（排除玩家自身，玩家滑翔由 AvatarRenderer 处理）。
     */
    public static boolean shouldApplyGlidePitch(LivingEntity vehicle) {
        return vehicle.isFallFlying() && !(vehicle instanceof Player);
    }

    /**
     * 是否应用滑翔俯仰旋转：滑翔中，或触地/停止后俯仰尚未衰减完的过渡期。
     */
    public static boolean shouldApplyGlideRotation(LivingEntity vehicle) {
        return shouldApplyGlidePitch(vehicle)
            || !isNegligible(((GlidePitchData) vehicle).funnymod$getGlidePitch());
    }

    /**
     * 绕坐骑躯干中心沿右轴旋转骑手座位，与渲染端 setupRotations 的俯仰旋转一致。
     */
    public static Vec3 rotateSeat(Vec3 seat, LivingEntity vehicle, float pitchDegrees) {
        Vec3 pos = vehicle.position();
        Vec3 pivot = new Vec3(pos.x, pos.y + getPivotY(vehicle), pos.z);
        return rotateAroundPoint(seat, pivot, vehicle.getYRot(), pitchDegrees);
    }

    /**
     * 绕 pivot 点沿 yaw 朝向的水平右轴旋转任意点（度）。
     * 与渲染端 {@code poseStack.mulPose(Axis.XP.rotationDegrees(pitch))} 的旋转一致。
     */
    public static Vec3 rotateAroundPoint(Vec3 point, Vec3 pivot, float yawDegrees, float pitchDegrees) {
        Vec3 rel = point.subtract(pivot);
        // 转入本地坐标系（-Z 为前方，X 为右侧），绕 X 轴旋转后转回世界坐标
        float yawRad = yawDegrees * Mth.DEG_TO_RAD + Mth.PI;
        Vec3 local = rel.yRot(yawRad).xRot(-pitchDegrees * Mth.DEG_TO_RAD).yRot(-yawRad);
        return pivot.add(local);
    }

    /** 旋转轴心的世界高度（相对坐骑位置） */
    public static double getPivotY(LivingEntity vehicle) {
        return vehicle.getBbHeight() * PIVOT_HEIGHT_RATIO;
    }

    /** 俯仰角是否小到可以忽略 */
    public static boolean isNegligible(float pitch) {
        return Math.abs(pitch) < MIN_PITCH;
    }
}
