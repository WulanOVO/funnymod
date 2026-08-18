package yibo.funnymod.entity;

/**
 * 骑手跳跃按键状态接口。
 * 客户端由 {@code LocalPlayerMixin} 实现（读取本地输入），
 * 服务端由 {@code ServerPlayerMixin} 实现（读取客户端同步的输入包），
 * 供 {@code LivingEntityMixin} 在骑乘 tick 中判断骑手是否按住跳跃键。
 */
public interface RiderJumpInput {
    /** 骑手当前是否按住跳跃键（空格） */
    boolean funnymod$isJumpHeld();
}
