package yibo.funnymod.mixin;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yibo.funnymod.effect.ModEffects;
import yibo.funnymod.entity.RiderJumpInput;

/**
 * 粘脚效果：模拟蜂蜜块，削减跳跃高度（跳一半）让玩家跳不了一格
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Shadow
    protected abstract boolean canGlide();

    @Shadow
    public abstract boolean isFallFlying();

    @Inject(method = "jumpFromGround", at = @At("TAIL"))
    private void reduceStickyFeetJump(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.hasEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModEffects.STICKY_FEET))) {
            Vec3 movement = self.getDeltaMovement();
            self.setDeltaMovement(movement.x, movement.y * 0.5, movement.z);
            self.playSound(SoundEvents.HONEY_BLOCK_SLIDE, 0.5F, 1.0F);
        }
    }

    /**
     * 坐骑滑翔：被玩家骑乘的坐骑装着鞍鞘（GLIDER 组件在鞍槽）时，
     * 在空中按住跳跃键即启动滑翔（对应共享标志位 7，与玩家鞘翅同一状态）。
     * 启动后滑翔物理由原版 {@code travelFallFlying} 自动接管，
     * 停止条件（触地/入水/耐久耗尽等）由原版 {@code updateFallFlying} 每 tick 检查。
     * 客户端（移动权威）与服务端（状态同步/伤害判定）各自独立触发，无需额外网络包。
     */
    @Inject(method = "tickRidden", at = @At("TAIL"))
    private void funnymod$tryStartGlideWhenRidden(Player controller, Vec3 riddenInput, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.isFallFlying() || self.onGround() || self.isInWater()) {
            return;
        }
        // 仅响应实现了按键状态接口的真实玩家（LocalPlayer / ServerPlayer）
        if (!(controller instanceof RiderJumpInput jumpInput) || !jumpInput.funnymod$isJumpHeld()) {
            return;
        }
        if (this.canGlide()) {
            // setSharedFlag 声明在 Entity（protected），已通过 access widener 开放，直接调用
            self.setSharedFlag(7, true);
        }
    }
}
