package yibo.funnymod.client.mixin;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yibo.funnymod.entity.FireworkDashHorse;
import yibo.funnymod.network.SkeletonHorseShootPayload;

/**
 * 客户端：骑乘骷髅马且装备弩时，拦截中键（拾取方块）改为发射弩箭。
 * 在 handleKeybinds 头部注入，先于原版 keyPickItem 消费中键事件，
 * 避免同时触发拾取方块。仅在满足射击条件时消费事件。
 */
@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    /** 客户端射击冷却（tick），与服务端 4 tick 间隔一致 */
    @Unique
    private int shootCooldown = 0;

    @Inject(method = "handleKeybinds", at = @At("HEAD"))
    private void funnymod$interceptCrossbowShoot(CallbackInfo ci) {
        if (this.shootCooldown > 0) this.shootCooldown--;

        Minecraft self = (Minecraft) (Object) this;
        LocalPlayer player = self.player;
        if (player == null) return;

        if (player.getVehicle() instanceof FireworkDashHorse horse && horse.funnymod$hasCrossbow()) {
            // 消费中键点击事件，防止触发原版拾取方块
            boolean clicked = false;
            while (self.options.keyPickItem.consumeClick()) {
                clicked = true;
            }
            // 按下或持续按住时发射（4 tick 冷却由 cooldown 控制）
            if ((clicked || self.options.keyPickItem.isDown()) && this.shootCooldown <= 0) {
                this.shootCooldown = 4;
                ClientPlayNetworking.send(new SkeletonHorseShootPayload(player.getVehicle().getId()));
            }
        }
    }
}
