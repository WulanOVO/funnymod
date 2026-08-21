package yibo.funnymod.entity;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;

/**
 * 标记接口：让骷髅马支持烟花火箭槽位与突进能力。
 * 由 {@link yibo.funnymod.mixin.SkeletonHorseMixin} 实现，
 * 供库存菜单与网络包通过该接口访问骷髅马扩展出的能力。
 */
public interface FireworkDashHorse {
    /**
     * 获取骷髅马的烟花火箭槽位（单格，最多堆叠 64 个烟花火箭）。
     */
    SimpleContainer funnymod$getFireworkContainer();

    /**
     * 烟花槽位是否放有烟花火箭（通过实体数据同步，客户端可读）。
     */
    boolean funnymod$hasFirework();

    /**
     * 烟花槽位中烟花火箭的「数量」（通过实体数据同步，客户端可读）。
     * 客户端据此渲染骷髅马尾部骨架空腔中的烟花火箭（最多渲染 4 个）。
     */
    int funnymod$getFireworkCount();

    /**
     * 获取骷髅马的弩槽位（单格，不可堆叠）。
     */
    SimpleContainer funnymod$getCrossbowContainer();

    /**
     * 弩槽位是否放有弩（通过实体数据同步，客户端可读）。
     */
    boolean funnymod$hasCrossbow();

    /**
     * 弩槽中的实际 ItemStack（通过实体数据同步，客户端可读）。
     * 客户端据此渲染弩的装填状态、附魔光泽、耐久等所有实时状态。
     */
    ItemStack funnymod$getCrossbowStack();

    /**
     * 立即摆正马身、退出站立/跳跃蓄力状态。
     * 触发突进时调用，避免跳跃状态干扰突进。
     */
    void funnymod$resetJumpState();

    /**
     * 消耗一个烟花火箭，让骷髅马向前突进一段，并生成一个伴飞的烟花火箭实体。
     * 服务端：消耗烟花 + 生成伴飞实体 + 设置突进速度与持续加速度；
     * 客户端：跳过消耗与实体生成（由服务端负责），仅设置突进速度与持续加速度用于预测。
     */
    void funnymod$startDash();
}
