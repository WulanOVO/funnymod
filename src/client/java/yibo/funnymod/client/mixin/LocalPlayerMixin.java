package yibo.funnymod.client.mixin;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.PlayerRideableJumping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yibo.funnymod.entity.FireworkDashHorse;
import yibo.funnymod.network.SkeletonHorseDashPayload;

/**
 * 客户端：骑乘骷髅马时，按住疾跑键的同时按下空格键，拦截原版跳跃逻辑，改为触发突进。
 */
@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {
    @Shadow
    public ClientInput input;

    /** 上一 tick 跳跃键是否处于按下状态，用于检测按下沿 */
    @Unique
    private boolean funnymod$jumpHeld = false;

    /**
     * 拦截 jumpableVehicle：骑乘骷髅马、按住疾跑键且烟花槽有烟花火箭时返回 null，
     * 使原版坐骑跳跃蓄力逻辑（aiStep 第 816-836 行）整体被跳过。
     * 没有烟花时保持原版跳跃逻辑。
     */
    @Redirect(method = "aiStep", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;jumpableVehicle()Lnet/minecraft/world/entity/PlayerRideableJumping;"))
    private PlayerRideableJumping redirectJumpableVehicle(LocalPlayer self) {
        PlayerRideableJumping vehicle = self.jumpableVehicle();
        if (vehicle instanceof FireworkDashHorse horse
                && this.input.keyPresses.sprint()
                && horse.funnymod$hasFirework()) {
            return null;
        }
        return vehicle;
    }

    /**
     * 在 aiStep 末尾检测「疾跑键 + 空格按下沿 + 骑乘骷髅马 + 烟花槽有烟花」，
     * 触发突进并通知服务器（服务器负责消耗烟花火箭并生成伴飞实体）。
     */
    @Inject(method = "aiStep", at = @At("TAIL"))
    private void detectSprintDash(CallbackInfo ci) {
        boolean jumpHeld = this.input.keyPresses.jump();
        if (jumpHeld && !this.funnymod$jumpHeld) {
            // 空格按下沿
            LocalPlayer self = (LocalPlayer) (Object) this;
            if (self.getVehicle() instanceof FireworkDashHorse horse
                    && this.input.keyPresses.sprint()
                    && horse.funnymod$hasFirework()) {
                horse.funnymod$startDash();
                ClientPlayNetworking.send(new SkeletonHorseDashPayload(self.getVehicle().getId()));
            }
        }
        this.funnymod$jumpHeld = jumpHeld;
    }
}
