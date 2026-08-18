package yibo.funnymod.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 烟花火箭附着实体为非玩家（如骷髅马）时，跳过原版的加速逻辑。
 * 加速由 {@link SkeletonHorseMixin#tickRidden} 在客户端复刻，
 * 避免双重加速导致飞行卡顿。烟花火箭仍提供粒子、音效、爆炸等视觉效果。
 */
@Mixin(FireworkRocketEntity.class)
public abstract class FireworkRocketEntityMixin {
    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;isFallFlying()Z"))
    private boolean funnymod$skipBoostForNonPlayer(LivingEntity entity) {
        if (entity instanceof Player) {
            return entity.isFallFlying();
        }
        return false;
    }
}
