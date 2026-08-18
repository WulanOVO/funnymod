package yibo.funnymod.mixin;

import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import yibo.funnymod.entity.RiderJumpInput;

/**
 * 服务端：通过客户端同步的输入包（ServerboundPlayerInputPacket）读取骑手的跳跃按键状态。
 */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin implements RiderJumpInput {
    @Override
    public boolean funnymod$isJumpHeld() {
        return ((ServerPlayer) (Object) this).getLastClientInput().jump();
    }
}
