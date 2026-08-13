package yibo.funnymod.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.world.entity.Entity;
import yibo.funnymod.entity.FireworkDashHorse;

/**
 * 网络注册：接收客户端发来的骷髅马突进请求。
 */
public class ModNetworking {
    public static void initialize() {
        // 注册 serverbound（客户端 → 服务器）payload 类型
        PayloadTypeRegistry.serverboundPlay().register(SkeletonHorseDashPayload.TYPE, SkeletonHorseDashPayload.CODEC);

        // 服务器接收器：定位骷髅马并触发突进（回调运行在服务器主线程）
        ServerPlayNetworking.registerGlobalReceiver(SkeletonHorseDashPayload.TYPE, (payload, context) -> {
            Entity entity = context.player().level().getEntity(payload.horseId());
            // 校验：目标必须是玩家当前骑乘的骷髅马
            if (entity instanceof FireworkDashHorse horse && context.player().getVehicle() == entity) {
                horse.funnymod$startDash();
            }
        });
    }
}
