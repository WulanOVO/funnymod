package yibo.funnymod.client.mixin;

import net.minecraft.client.renderer.entity.state.EquineRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import yibo.funnymod.client.render.FireworkDashHorseRenderState;
import yibo.funnymod.client.render.SaddleElytraRenderState;

/**
 * 给坐骑渲染状态附加本模组额外的渲染数据：
 * 烟花火箭槽的 item 渲染状态（{@link FireworkDashHorseRenderState}），
 * 以及鞘翅旋转值（{@link SaddleElytraRenderState}，复刻玩家鞘翅动画机制）。
 * 由对应 renderer mixin 在 extractRenderState 时填充。
 */
@Mixin(EquineRenderState.class)
public abstract class EquineRenderStateMixin
    implements FireworkDashHorseRenderState, SaddleElytraRenderState {

    @Unique
    private final ItemStackRenderState funnymod$fireworkItem = new ItemStackRenderState();

    @Unique
    private int funnymod$fireworkCount = 0;

    @Unique
    private final ItemStackRenderState funnymod$crossbowItem = new ItemStackRenderState();

    @Unique
    private boolean funnymod$hasCrossbow = false;

    /**
     * 鞘翅当前旋转值，由 {@code AbstractHorseRendererMixin} 从 elytraAnimationState 提取并填充
     */
    @Unique
    private float funnymod$elytraRotX = 0.2617994f;

    @Unique
    private float funnymod$elytraRotY = 0.0f;

    @Unique
    private float funnymod$elytraRotZ = -0.2617994f;

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

    @Override
    public ItemStackRenderState funnymod$getCrossbowItem() {
        return this.funnymod$crossbowItem;
    }

    @Override
    public boolean funnymod$hasCrossbow() {
        return this.funnymod$hasCrossbow;
    }

    @Override
    public void funnymod$setHasCrossbow(boolean hasCrossbow) {
        this.funnymod$hasCrossbow = hasCrossbow;
    }

    @Override
    public float funnymod$getElytraRotX() {
        return this.funnymod$elytraRotX;
    }

    @Override
    public float funnymod$getElytraRotY() {
        return this.funnymod$elytraRotY;
    }

    @Override
    public float funnymod$getElytraRotZ() {
        return this.funnymod$elytraRotZ;
    }

    @Override
    public void funnymod$setElytraRotX(float value) {
        this.funnymod$elytraRotX = value;
    }

    @Override
    public void funnymod$setElytraRotY(float value) {
        this.funnymod$elytraRotY = value;
    }

    @Override
    public void funnymod$setElytraRotZ(float value) {
        this.funnymod$elytraRotZ = value;
    }
}
