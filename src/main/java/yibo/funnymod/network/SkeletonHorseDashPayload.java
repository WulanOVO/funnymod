package yibo.funnymod.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import yibo.funnymod.Funnymod;

/**
 * 客户端 → 服务器：玩家双击空格键请求骷髅马突进。
 * 携带骷髅马的实体 id，服务器据此定位目标并触发突进。
 */
public record SkeletonHorseDashPayload(int horseId) implements CustomPacketPayload {
    public static final Identifier ID = Funnymod.id("skeleton_horse_dash");

    public static final CustomPacketPayload.Type<SkeletonHorseDashPayload> TYPE =
            new CustomPacketPayload.Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, SkeletonHorseDashPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT,
                    SkeletonHorseDashPayload::horseId,
                    SkeletonHorseDashPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
