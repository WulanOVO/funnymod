package yibo.funnymod.client.render;

/**
 * 滑翔整体俯仰的渲染数据：俯仰角与旋转轴心（渲染空间局部 Y）。
 * 由 {@code LivingEntityRenderStateMixin} 实现到所有生物渲染状态基类，
 * {@code LivingEntityRendererMixin} 在 extractRenderState 时填充、setupRotations 时应用。
 * 所有鞍鞘坐骑（马、骆驼等）滑翔时通用。
 */
public interface GlidePitchRenderState {
    float funnymod$getGlidePitch();

    void funnymod$setGlidePitch(float pitch);

    float funnymod$getGlidePivotY();

    void funnymod$setGlidePivotY(float pivotY);
}
