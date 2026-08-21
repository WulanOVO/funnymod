package yibo.funnymod.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import yibo.funnymod.Funnymod;

/**
 * 客户端 → 服务器：骑乘骷髅马时按中键请求发射弩箭。
 * 携带骷髅马的实体 id，服务器据此定位目标并执行射击。
 */
public record SkeletonHorseShootPayload(int horseId) implements CustomPacketPayload {
    public static final Identifier ID = Funnymod.id("skeleton_horse_shoot");

    public static final CustomPacketPayload.Type<SkeletonHorseShootPayload> TYPE =
            new CustomPacketPayload.Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, SkeletonHorseShootPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT,
                    SkeletonHorseShootPayload::horseId,
                    SkeletonHorseShootPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
