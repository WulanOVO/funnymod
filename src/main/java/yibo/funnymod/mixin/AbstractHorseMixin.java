package yibo.funnymod.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 原版 {@link AbstractHorse#getRiddenRotation} 把马身俯仰角设为玩家 xRot 的 0.5 倍，
 * 导致滑翔物理（{@code updateFallFlyingMovement} 基于 {@code getXRot()} 计算 leanAngle）
 * 几乎不响应玩家的俯冲/爬升操作。
 * <p>
 * 此 mixin 在 {@code getRiddenRotation} 返回时拦截：滑翔中时返回玩家完整视角俯仰角，
 * 使鞘翅滑翔物理与玩家自己用鞘翅时的操控手感一致。
 */
@Mixin(AbstractHorse.class)
public abstract class AbstractHorseMixin {
    @Inject(method = "getRiddenRotation", at = @At("RETURN"), cancellable = true)
    private void funnymod$fullPitchWhenGliding(LivingEntity controller, CallbackInfoReturnable<Vec2> cir) {
        AbstractHorse self = (AbstractHorse) (Object) this;
        if (self.isFallFlying()) {
            cir.setReturnValue(new Vec2(controller.getXRot(), controller.getYRot()));
        }
    }
}
