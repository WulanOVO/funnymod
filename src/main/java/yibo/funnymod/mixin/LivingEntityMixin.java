package yibo.funnymod.mixin;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yibo.funnymod.effect.ModEffects;

/**
 * 粘脚效果：模拟蜂蜜块，削减跳跃高度（跳一半）让玩家跳不了一格
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Inject(method = "jumpFromGround", at = @At("TAIL"))
    private void reduceStickyFeetJump(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.hasEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModEffects.STICKY_FEET))) {
            Vec3 movement = self.getDeltaMovement();
            self.setDeltaMovement(movement.x, movement.y * 0.5, movement.z);
            self.playSound(SoundEvents.HONEY_BLOCK_SLIDE, 0.5F, 1.0F);
        }
    }
}
