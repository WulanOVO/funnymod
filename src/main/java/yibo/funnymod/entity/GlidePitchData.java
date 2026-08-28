package yibo.funnymod.entity;

/**
 * 滑翔俯仰平滑状态：存每 tick 平滑后的俯仰角及上一 tick 值，
 * 供渲染/相机用 partialTicks 插值，起飞触地时过渡平滑。
 */
public interface GlidePitchData {
    float funnymod$getGlidePitch();

    void funnymod$setGlidePitch(float pitch);

    float funnymod$getGlidePitchO();

    void funnymod$setGlidePitchO(float pitch);

    /** 落地开始衰减时的俯仰峰值，作为落地过渡的头俯仰混合基准 */
    float funnymod$getGlidePitchPeak();

    void funnymod$setGlidePitchPeak(float pitch);
}
