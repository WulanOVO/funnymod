package yibo.funnymod.client.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yibo.funnymod.client.sound.SaddleElytraSoundInstance;

/**
 * 客户端：非玩家 LivingEntity 进入滑翔（fall flying）时启动坐骑滑翔音效。
 * 复刻原版 {@code LocalPlayer} 在数据同步回调里启动 {@code ElytraOnPlayerSoundInstance} 的模式。
 * 玩家自身由原版处理，此处跳过。
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityElytraSoundMixin {

    @Unique
    private boolean funnymod$wasFallFlying = false;

    @Inject(method = "onSyncedDataUpdated", at = @At("TAIL"))
    private void funnymod$startSaddleElytraSound(EntityDataAccessor<?> accessor, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!self.level().isClientSide() || self instanceof Player) {
            return;
        }
        boolean nowFlying = self.isFallFlying();
        if (nowFlying && !this.funnymod$wasFallFlying) {
            Minecraft.getInstance().getSoundManager().play(new SaddleElytraSoundInstance(self));
        }
        this.funnymod$wasFallFlying = nowFlying;
    }
}
