package yibo.funnymod.client.mixin;

import net.minecraft.client.renderer.entity.state.EquineRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import yibo.funnymod.client.render.FireworkDashHorseRenderState;

/**
 * 给骷髅马的渲染状态附加烟花火箭渲染数据。
 * 烟花火箭的渲染用 ItemStackRenderState（与 item display 实体一致），
 * 由 {@link AbstractHorseRendererMixin} 在提取渲染状态时填充。
 * 通过 {@link FireworkDashHorseRenderState} 接口对外暴露，避免被直接引用。
 */
@Mixin(EquineRenderState.class)
public abstract class EquineRenderStateMixin implements FireworkDashHorseRenderState {
    /** 烟花火箭的 item 渲染状态（由 ItemModelResolver 填充） */
    @Unique
    private final ItemStackRenderState funnymod$fireworkItem = new ItemStackRenderState();

    /** 骷髅马烟花槽中的火箭数量（决定渲染几个火箭，最多 4 个） */
    @Unique
    private int funnymod$fireworkCount = 0;

    @Override
    public ItemStackRenderState funnymod$getFireworkItem() {
        return this.funnymod$fireworkItem;
    }

    @Override
    public int funnymod$getFireworkCount() {
        return this.funnymod$fireworkCount;
    }

    @Override
    public void funnymod$setFireworkCount(int count) {
        this.funnymod$fireworkCount = count;
    }
}
