package yibo.funnymod.client.mixin;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.AbstractMountInventoryScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yibo.funnymod.entity.FireworkDashHorse;

/**
 * 客户端：在骷髅马的库存界面，于马鞍槽位下方渲染烟花火箭槽位背景。
 * mixin 到 AbstractMountInventoryScreen（mount 与 extractBackground 均定义于此），
 * 对 HorseInventoryScreen 与其它坐骑库存界面同时生效，仅在坐骑为骷髅马时渲染。
 */
@Mixin(AbstractMountInventoryScreen.class)
public abstract class AbstractMountInventoryScreenMixin {
    @Shadow
    protected LivingEntity mount;

    /** 与 HorseInventoryScreen#getSlotSpriteLocation 相同的默认槽位贴图 */
    private static final Identifier SLOT_SPRITE = Identifier.withDefaultNamespace("container/slot");

    @Inject(method = "extractBackground", at = @At("TAIL"))
    private void renderExtraSlots(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (!(this.mount instanceof FireworkDashHorse)) return;

        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
        // 烟花槽背景 (leftPos+7, topPos+35)，与马甲槽位置一致（骷髅马马甲槽不渲染，故复用）
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_SPRITE, screen.leftPos + 7, screen.topPos + 35, 18, 18);
        // 弩槽背景 (leftPos+7, topPos+53)，烟花槽正下方
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_SPRITE, screen.leftPos + 7, screen.topPos + 53, 18, 18);
    }
}
