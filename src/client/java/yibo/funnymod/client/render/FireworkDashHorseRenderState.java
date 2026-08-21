package yibo.funnymod.client.render;

import net.minecraft.client.renderer.item.ItemStackRenderState;

/**
 * 暴露骷髅马渲染状态中烟花火箭渲染数据的接口。
 * 由 {@code EquineRenderStateMixin} 实现，供渲染器 mixin 与渲染层通过接口访问，
 * 避免直接引用 mixin 类。
 */
public interface FireworkDashHorseRenderState {
    /** 烟花火箭的 item 渲染状态（由 ItemModelResolver 填充） */
    ItemStackRenderState funnymod$getFireworkItem();

    /** 骷髅马烟花槽中的火箭数量（决定渲染几个火箭） */
    int funnymod$getFireworkCount();

    /** 设置火箭数量 */
    void funnymod$setFireworkCount(int count);

    /** 弩的 item 渲染状态（由 ItemModelResolver 填充） */
    ItemStackRenderState funnymod$getCrossbowItem();

    /** 弩槽是否放有弩（决定是否渲染） */
    boolean funnymod$hasCrossbow();

    /** 设置弩槽状态 */
    void funnymod$setHasCrossbow(boolean hasCrossbow);
}
