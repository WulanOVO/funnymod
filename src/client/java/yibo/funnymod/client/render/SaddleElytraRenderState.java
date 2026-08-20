package yibo.funnymod.client.render;

/**
 * 暴露坐骑渲染状态中鞘翅旋转值的接口。
 * 由 {@code EquineRenderStateMixin} 实现，供 {@link yibo.funnymod.client.render.HorseElytraLayer}
 * 在马背上渲染鞘翅时使用。旋转值从 {@code LivingEntity#elytraAnimationState} 提取，
 * 复刻玩家鞘翅动画机制（{@code HumanoidMobRenderer#extractRenderState}）。
 */
public interface SaddleElytraRenderState {
    float funnymod$getElytraRotX();

    float funnymod$getElytraRotY();

    float funnymod$getElytraRotZ();

    void funnymod$setElytraRotX(float value);

    void funnymod$setElytraRotY(float value);

    void funnymod$setElytraRotZ(float value);
}
