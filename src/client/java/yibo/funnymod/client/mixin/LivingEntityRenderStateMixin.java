package yibo.funnymod.client.mixin;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import yibo.funnymod.client.render.GlidePitchRenderState;

/**
 * 给所有生物渲染状态基类附加滑翔俯仰数据（{@link GlidePitchRenderState}），
 * 由 {@code LivingEntityRendererMixin} 填充，使整体俯仰对鞍鞘坐骑及其骑手通用。
 */
@Mixin(LivingEntityRenderState.class)
public abstract class LivingEntityRenderStateMixin implements GlidePitchRenderState {
    @Unique
    private float funnymod$glidePitch = 0.0f;

    @Unique
    private float funnymod$glidePivotY = 0.0f;

    @Override
    public float funnymod$getGlidePitch() {
        return this.funnymod$glidePitch;
    }

    @Override
    public void funnymod$setGlidePitch(float pitch) {
        this.funnymod$glidePitch = pitch;
    }

    @Override
    public float funnymod$getGlidePivotY() {
        return this.funnymod$glidePivotY;
    }

    @Override
    public void funnymod$setGlidePivotY(float pivotY) {
        this.funnymod$glidePivotY = pivotY;
    }
}
